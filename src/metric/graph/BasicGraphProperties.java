package metric.graph;

import static utility.lib.Unit.*;

import graph.Node;
import metric.StatisticMetric;

/** Measures some simple attributes of the graph, like size, channels, average degree **/
public class BasicGraphProperties extends StatisticMetric {

	@Override
	protected void beforeSimulation() {
		stat("Nodes", graph().size());
		stat("  Consumers", graph().nodesWithRole(Node.ROLE_CONSUMER).size());
		stat("  Gateways", graph().nodesWithRole(Node.ROLE_GATEWAY).size());
		stat("  Degree (avg)", 2 * graph().channels().size() / (float) graph().size());
		stat("  Node Capacity (avg)",
				toBTC((int) graph().nodes().stream()
					.mapToInt(n -> n.channels().stream()
						.mapToInt(ch -> ch.getCapacity(n.getID())).sum())
					.average().orElse(0)) + " BTC");
		
		stat("Channels", graph().channels().size());
		stat("  Channel Capacity (avg)",
			toBTC((int) graph().channels().stream()
				.mapToInt(ch -> ch.getCapacity())
				.average().orElse(0)) + " BTC");
}

}
