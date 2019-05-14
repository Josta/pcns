package metric.routing;

import metric.SamplingMetric;
import routing.algorithm.FlareRouting;
import routing.algorithm.LandmarkCentricRouting;
import routing.algorithm.LandmarkUniverseRouting;
import routing.algorithm.SourceGraphRouting;
import routing.util.SpanningForestGenerator;
import routing.util.SpanningTreeGenerator;

/**
 * Records the global rate of routing messages not directly related to a concrete payment 
 **/
public class RoutingManagementTraffic extends SamplingMetric {
	
	private long updates;
	
	@Override
	public void beforeSimulation() {
		labels("Time (s)", "Channel Update Rate");
		updates = 0;
		if (sim.routing() instanceof SourceGraphRouting) {
			sim.beforeEvent(SourceGraphRouting.ChannelUpdate.class, e -> updates++);
		}
		if (sim.routing() instanceof FlareRouting) {		
			sim.beforeEvent(FlareRouting.NeighborReset.class, e -> updates++);
			sim.beforeEvent(FlareRouting.BeaconRequest.class, e -> updates++);
			sim.beforeEvent(FlareRouting.BeaconAck.class, e -> updates++);
			sim.beforeEvent(FlareRouting.Subscribe.class, e -> updates++);
			sim.beforeEvent(FlareRouting.Unsubscribe.class, e -> updates++);
			sim.beforeEvent(FlareRouting.DynamicInfo.class, e -> updates++);
		}
		if (sim.routing() instanceof LandmarkCentricRouting) {
			sim.beforeEvent(SpanningTreeGenerator.TreeUpdate.class, e -> updates++);
		}
		if (sim.routing() instanceof LandmarkUniverseRouting) {
			sim.beforeEvent(SpanningForestGenerator.TreeUpdate.class, e -> updates++);
		}
	}

	@Override
	protected void sample(float time) {
		write(time, updates / interval);
		updates = 0;
	}

}
