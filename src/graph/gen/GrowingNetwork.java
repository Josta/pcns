package graph.gen;

import graph.Graph;

/**
 * Creates a Growing Network graph, i.e. a graph where each
 * new node is randomly attached to the existing network.
 * Properties: contiguous, n-1 edges
 * Complexity: O(V)
 * @author Josua
 */
public class GrowingNetwork extends GraphGenerator {

	private int size;
	
	public GrowingNetwork(int nodes) {
		this.size = nodes;
	}

	@Override
	public Graph generate() {
		Graph g = new Graph();
		g.setupNodes(size);
		for (int i = 1; i < size; i++) {
			g.newChannel(i, random.getInt(i));
		}
		return g;
	}
	
	@Override
	public String toString() {
		return String.format("GrowingNetwork(%d)", size);
	}

}
