package routing.costs;

import graph.Channel;

/** Calculates the routing costs as 1 for every hop **/
public class HopRoutingCosts extends RoutingCosts {

	@Override
	public long getCosts(Channel c, int sender) {
		return 1;
	}

	@Override
	public String toString() {
		return "HopRoutingCosts()";
	}
}
