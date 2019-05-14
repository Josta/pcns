package graph.gen;

import graph.Graph;

/**
 * Creates a fully connected graph.
 * Complexity: O(V²)
 * @author Josua
 */
public class Clique extends GraphGenerator {

	private int size;
	
	/**
	 * Creates a fully connected graph.
	 * @param nodes network size
	 */
	public Clique(int nodes) {
		this.size = nodes;
	}

	@Override
	public Graph generate() {
		Graph g = new Graph();
		
		g.setupNodes(size);
		
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < i; j++) {
				g.newChannel(i, j);
			}
		}
		
		return g;
	}

	@Override
	public String toString() {
		return String.format("Clique(%d)", size);
	}
}
