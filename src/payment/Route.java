package payment;

import graph.Channel;
import graph.Graph;

/**
 * A payment path together with precomputated fees, amounts and CLTV expirations.
 * Acts as a bridge between Payments and HTLCs.
 **/
public class Route {

	private final Payment payment;
	private final int[] nodes, timelocks;
	private final int[] amounts;
	
	private boolean sufficientCapacities;
	private State state;
	public enum State {PROPOSED, READY, PAID, FAILED};
	private static final int MIN_FINAL_CLTV_EXPIRATION = 9;
	
	public Route(Graph g, Payment p, int[] path, int amount) {
		payment = p;
		nodes = path;
		timelocks = new int[nodes.length];
		amounts = new int[nodes.length];
		sufficientCapacities = false;
		state = State.PROPOSED;
		computeFeesAndTimelocks(g, amount);
	}

	
	// GETTERS
	
	/** Returns the ID of node i **/
	public int getNode(int i) {
		return nodes[i];
	}
	
	public int[] getNodes() {
		return nodes;
	}
	
	public int size() {
		return nodes.length;
	}
	
	/** Returns the amount that should arrive at node i **/
	public int getAmount(int i) {
		// TODO: channel 0 should not demand fees
		return amounts[i];
	}
	
	public int getNetAmount() {
		return amounts[amounts.length - 1];
	}
	
	public int getGrossAmount() {
		return amounts[0];
	}
	
	public int getFee() {
		return getGrossAmount() - getNetAmount();
	}

	public int getTimelock(int i) {
		return timelocks[i];
	}
	
	public boolean hasSufficientCapacities() {
		return sufficientCapacities;
	}
	
	public boolean hasState(State state) {
		return this.state == state;
	}
	
	
	// PAYMENT/HTLC BRIDGE FUNCTIONALITY
	
	/**
	 * Called by last HTLC to indicate HTLC chain completeness.
	 * Fulfillment is only started if handler is called (this enables AMP payments)
	 */
	protected void setReady(Runnable handler) {
		state = State.READY;
		payment.routeReady(handler);
	}

	/** Called by first HTLC to indicate a completed route execution **/
	protected void setPaid() {
		state = State.PAID;
		payment.routePaid();
	}

	/** Called by first HTLC to indicate a failed route execution **/
	protected void setFailed() {
		state = State.FAILED;
		payment.routeFailed();
	}
	
	
	private void computeFeesAndTimelocks(Graph g, int amount) {
		sufficientCapacities = true;
		timelocks[nodes.length - 1] = MIN_FINAL_CLTV_EXPIRATION;
		amounts[nodes.length - 1] = amount;
		for (int i = nodes.length - 1; i > 0; i--) {
			Channel ch = g.channel(nodes[i-1], nodes[i]);
			if (ch.canPay(nodes[i-1], amounts[i])) {
				amounts[i-1] = amounts[i] + ch.getFee(nodes[i-1], amounts[i]);
				timelocks[i-1] = timelocks[i] + ch.getMinTimelockDelta();
			} else {
				sufficientCapacities = false;
				return;
			}
		}
	}
	
}
