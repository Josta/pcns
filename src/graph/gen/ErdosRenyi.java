package graph.gen;

import graph.Graph;

/**
 * Creates an adapted Erdos/Renyi graph, i.e a graph with a given number
 * of nodes where a given number of edges has been assigned randomly.
 * Complexity: O(V+E)
 * @author Josua
 */
public class ErdosRenyi extends GraphGenerator {

	private int totalNodes, totalEdges; 
	
	public ErdosRenyi(int nodes, int channels) {
		this.totalNodes = nodes;
		this.totalEdges = channels;
	}
	
	public Graph generate() {
		Graph g = new Graph();
		g.setupNodes(totalNodes);
		
		if (totalNodes * (totalNodes - 1) < 2 * totalEdges) {
			throw new IllegalArgumentException("Graph with " + totalNodes + " nodes cannot have " + totalEdges + " edges");
		}		
		
		int edgesToAdd = totalEdges;
		while (edgesToAdd > 0) {
			int sourceNode = random.getInt(totalNodes),
				targetNode = random.getInt(totalNodes);
			if ((sourceNode != targetNode) && (g.channel(sourceNode, targetNode) == null)) {
				g.newChannel(sourceNode, targetNode);
				edgesToAdd--;
			}
		}
		return g;
	}
	
	@Override
	public String toString() {
		return String.format("ErdosRenyi(%d, %d)", totalNodes, totalEdges);
	}
	
}
