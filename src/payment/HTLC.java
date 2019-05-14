package payment;

import core.Simulation;
import core.event.Event;
import core.event.Message;
import graph.Channel;

/**
 * The HTLC class includes the whole HTLC chain process.
 * 
 * In the simulation, an HTLC is the update_add_htlc message and the HTLC data object in one.
 * On arrival of the message
 * 	- the HTLC is assumed to be irrevocably committed
 *  - the fulfilling node creates a new HTLC with the next node
 * The HTLC will fail if:
 *  - the next HTLC could not be built or failed later
 *  - cltv_expiry was reached
 *  - cltv_expiry for next HTLC is less than min delta blocks in the future
 * The HTLC will be fulfilled if the next HTLC was fulfilled
 */
public class HTLC extends Message {
	
	private Route route;
	private HTLC prev;
	private int index;
	private boolean completed;
	private Channel channel;
	
	public HTLC(Route route) {
		this(route, 1, null);
	}
	
	/** Builds an HTLC with the receiver (triggering a chain) **/
	public HTLC(Route route, int index, HTLC prev) {
		this.index = index; // index indicates the HTLC *receiver* in the path
		this.route = route;
		this.prev = prev;
		this.completed = false;
	}
	
	
	// the following methods run at HTLC receiver side
	
	/** Reacts to (this) incoming HTLC **/
	@Override
	public void run() {
		if (index == route.size() - 1) {
			respondToFinalHTLC();
		} else if (!buildNextHTLC()){
			fail();
		}
	}
	
	/** Builds an HTLC to the next hop if fees and capacities pan out **/
	private boolean buildNextHTLC() {
		// see https://github.com/lightningnetwork/lightning-rfc/blob/master/02-peer-protocol.md#adding-an-htlc-update_add_htlc	
		int grossAmount = route.getAmount(index),
			amount = route.getAmount(index + 1);
		int nextHop = route.getNode(index + 1);
		Channel next = sim.graph().channel(node, nextHop);
		int cltvExpiryDelta = route.getTimelock(index) - route.getTimelock(index + 1);
		if (next.canPay(node, amount)
				//&& channel.getFee(getSender(), amount) == grossAmount - amount
				&& cltvExpiryDelta >= channel.getMinTimelockDelta()
				// && htlc_count + 1 <= next.getMaxAcceptedHTLCs(nextHop)
				// && htlc_sum + amount <= next.getMaxHTLCValueInFlight(nextHop)
				&& route.getTimelock(index + 1) < 500000000) {
			new HTLC(route, index + 1, this).send(node, nextHop, sim);
			return true;
		} else return false;
	}
	
	/** Returns a cancel message to the HTLC sender **/
	public void fail() {
		new FailHTLC().send(node, sender, sim);
	}
	
	/** Returns a fulfill message to the HTLC sender **/
	public void fulfill() {	
		new FullfillHTLC().send(node, sender, sim);
		// TODO? fullfillment deadline... cltv_expiry - 7
	}
	
	private void respondToFinalHTLC() {
		route.setReady(() -> fulfill());
	}
	
	// the following methods/events all run at HTLC sender side
	
	/** Sends an HTLC and reserves the necessary funds **/
	@Override
	public void send(int sender, int receiver, Simulation sim) {
		super.send(sender, receiver, sim);
		channel = sim.graph().channel(sender, receiver);
		channel.modify(sender, -route.getAmount(index));
		//sim.graph().channelUpdated(channel); // TODO should we update due to blockage?
		Event.make(() -> completeHTLC(false)).after(route.getTimelock(index - 1) + 1, sim);
	}
	
	/** Completes an HTLC with either success or failure **/
	private void completeHTLC(boolean success) {
		if (completed) return;
		channel.modify(success ? node : sender, route.getAmount(index));
		if (success)
			sim.graph().channelUpdated(channel);
		completed = true;
		completed();
		if (prev != null) { 
			if (success) prev.fulfill(); else prev.fail();
		} else {
			if (success) route.setPaid(); else route.setFailed();
		}
	}

	class FullfillHTLC extends Message {
		@Override
		public void run() {
			completeHTLC(true);
		}
	}
	
	class FailHTLC extends Message {
		@Override
		public void run() {
			completeHTLC(false);
		}
	}
	
}
