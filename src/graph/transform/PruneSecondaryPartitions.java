package graph.transform;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import graph.Graph;
import utility.lib.Lists;

public class PruneSecondaryPartitions extends Transformation {
	
	private boolean turned[];
	
	@Override
	public void transform(Graph g) {
		turned = Lists.initBoolArray(g.size(), false);
		turnNode(0);
		g.removeNodes(IntStream.range(0, turned.length)
			.filter(i -> !turned[i])
			.mapToObj(i -> g.node(i))
			.collect(Collectors.toList()));
	}
	
	private void turnNode(int node) {
		if (!turned[node]) {
			turned[node] = true;
			for (int neighbor : sim.graph().node(node).neighbors()) {
				turnNode(neighbor);
			}
		}
	}

	@Override
	public String toString() {
		return "PruneSecondaryPartitions()";
	}
}
