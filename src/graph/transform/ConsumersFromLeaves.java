package graph.transform;

import graph.Graph;
import graph.Node;

/**
 * Turns all leaves (nodes with only one channel) into consumers,
 * and turns their neighbors into gateways.
 */
public class ConsumersFromLeaves extends Transformation {
	
	@Override
	public void transform(Graph g) {
		g.nodes().stream().filter(n -> n.getDegree() < 2).forEach(n -> {
			n.setRole(Node.ROLE_CONSUMER);
			for (int neighbor : n.neighbors()) {
				g.node(neighbor).setRole(Node.ROLE_GATEWAY);
			}
		});
	}

	@Override
	public String toString() {
		return "ConsumersFromLeaves()";
	}
}
