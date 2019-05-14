package routing.costs;

import graph.Channel;

public class AdvancedRoutingCosts extends RoutingCosts {

	private float baseFactor, rateFactor, capacityFactor;
	
	public AdvancedRoutingCosts(float baseFactor, float rateFactor, float capacityFactor) {
		this.baseFactor = baseFactor;
		this.rateFactor = rateFactor;
		this.capacityFactor = capacityFactor;
	}
	
	@Override
	public long getCosts(Channel ch, int sender) {
		if (invert)
			sender = ch.getOtherNode(sender);
		return (long) (baseFactor * (ch.getBaseFee(sender) / 1000f)
				+ rateFactor * ch.getFeeRate(sender)
				+ capacityFactor * (1 - ch.getCapacity(sender) / ch.getCapacity()));  
	}
	
	@Override
	public String toString() {
		return String.format("AdvancedRoutingCosts(%f, %f, %f)", baseFactor, rateFactor, capacityFactor);
	}

}
