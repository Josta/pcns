package graph.gen;

import graph.Channel;
import graph.Graph;

/** Creates a Watts-Strogatz graph, with small-world properties and high clustering. **/
public class WattsStrogatz extends GraphGenerator {

	private int size, links;
	private double beta;
	
	/**
	 * Creates a Watts-Strogatz graph
	 * @param size number of nodes
	 * @param links new links per node (half of average degree)
	 * @param beta probability of link rewiring
	 */
	public WattsStrogatz(int size, int links, double beta) {
		this.size = size;
		this.links = links;
		this.beta = beta;
	}
	
	public Graph generate() {
		Graph g = new Graph();
		g.setupNodes(size);
		// generate lattice
		for (int i = 0; i < size; i++)
			for (int j = 1; j <= links; j++)
				g.newChannel(i, (i + j) % size);
		// rewire links
		for (int i = 0; i < size; i++)
			for (int j = 1; j <= links; j++)
				if (random.getDouble() <= beta) {
					int oldNode = (i + j) % size;
					Channel ch = g.channel(i, oldNode);
					g.node(oldNode).removeChannel(ch);
					ch.setNode1(i);
					ch.setNode2(-1);
					int dest = -1;
					do {
						dest = random.getInt(size);
					} while (g.channel(i, dest) != null);
					g.node(dest).addChannel(ch);
					ch.setNode2(dest);
				}
		return g;
	}
	
	@Override
	public String toString() {
		return String.format("WattsStrogatz(%d, %d, %.3f)", size, links, beta);
	}
	
}
