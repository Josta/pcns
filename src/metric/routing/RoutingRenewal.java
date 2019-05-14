package metric.routing;

import metric.Metric;
import routing.algorithm.LandmarkCentricRouting;
import routing.algorithm.LandmarkUniverseRouting;

/**
 * Records the times of routing renewal events.
 * Currently works with LandmarkCentricRouting and LandmarkUniverseRouting.
 **/
public class RoutingRenewal extends Metric {
	
	@Override
	public void beforeSimulation() {
		labels("Time (s)", "Routing Renewal");
		sim.afterEvent(LandmarkCentricRouting.RenewLandmark.class,
			e -> write(e.getTime(), 1));
		sim.afterEvent(LandmarkUniverseRouting.RenewUniverse.class,
			e -> write(e.getTime(), 1));
	}

}
