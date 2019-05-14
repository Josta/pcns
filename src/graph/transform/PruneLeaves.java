package graph.transform;

import java.util.List;
import java.util.stream.Collectors;

import graph.Graph;
import graph.Node;

public class PruneLeaves extends Transformation {
	
	@Override
	public void transform(Graph g) {
		List<Node> leaves;
		do {
			leaves = g.nodes().stream()
				.filter(n -> n.getDegree() < 2)
				.collect(Collectors.toList());
			g.removeNodes(leaves);
		} while (leaves.size() > 0);
	}

	@Override
	public String toString() {
		return "PruneLeaves()";
	}
}
