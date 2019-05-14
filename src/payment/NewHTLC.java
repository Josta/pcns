package payment;

import core.Simulation;
import core.event.Event;
import core.event.Message;
import graph.Channel;

public class NewHTLC {
	private Simulation sim;
	private Route route;
	private int idx;
	private Channel channel;
	private int id;
	private State state;
	private enum State {INITIAL, COMMITTED, FULFILLED, FAILED};
	private NewHTLC previous;
	
	public NewHTLC(Simulation sim, Route route, int index, NewHTLC previous) {
		this.sim = sim;
		this.route = route;
		this.idx = index;
		this.channel = sim.graph().channel(route.getNode(idx), route.getNode(idx + 1));
		this.state = State.INITIAL;
		this.previous = previous;
	}
	
	
	/** Proposes this HTLC to the receiver **/
	public boolean send() {
		int fee = previous.getAmount() - getAmount();
		int delta = previous.getCLTVExpiry() - getCLTVExpiry();
		if (channel.canPay(getSender(), getAmount())
				&& channel.getFee(getSender(), getAmount()) == fee
				&& delta >= channel.getMinTimelockDelta()
				// && htlc_count + 1 <= next.getMaxAcceptedHTLCs(nextHop)
				// && htlc_sum + amount <= next.getMaxHTLCValueInFlight(nextHop)
				&& getCLTVExpiry() < 500000000) {
			new AddHTLC().send(getReceiver(), getSender(), sim);
			return true;
		}
		return false;
	}
	
	/** Enacts this HTLC **/
	public void commit() {
		//channel.addHTLC(this, route.getNode(idx));
		Event.make(() -> fail()).after(route.getTimelock(idx) + 1, sim);
		this.state = State.COMMITTED;
	}
	
	
	public void fulfill() {
		new FulfillHTLC().send(getReceiver(), getSender(), sim);
		// TODO? fulfillment deadline... cltv_expiry - 7
	}
	
	public void fail() {
		new FailHTLC().send(getReceiver(), getSender(), sim);
	}
	
	private void timeout() {
		fail();
	}
	
	class AddHTLC extends Message {		
		@Override
		public void run() {
			// final receiver response
			if (idx == route.size() - 1) {
				route.setReady(() -> fulfill());
			}
			if (!new NewHTLC(sim, route, idx + 1, NewHTLC.this).send()) fail();
		}
	}
	
	class FulfillHTLC extends Message {
		@Override
		public void run() {
			if (state != State.COMMITTED) return;
			//channel.completeHTLC(true);
			state = State.FULFILLED;
			if (previous == null) {
				
			} else {
				previous.fulfill();
			}
		}
	}
	
	class FailHTLC extends Message {
		@Override
		public void run() {
			if (state != State.COMMITTED) return;
			//channel.completeHTLC(false);
			state = State.FULFILLED;
			if (previous == null) {
				
			} else {
				previous.fail();
			}
		}
	}
	
	private int getSender() {
		return route.getNode(idx);
	}
	
	private int getReceiver() {
		return route.getNode(idx + 1);
	}
	
	private int getAmount() {
		return route.getAmount(idx);
	}
	
	private int getCLTVExpiry() {
		return route.getTimelock(idx);
	}
}
