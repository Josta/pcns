package graph.gen;

import graph.Graph;

/**
 * Creates a Barabási–Albert graph, which is scale-free and has
 * a better than random clustering coefficient.
 * see https://en.wikipedia.org/wiki/Barab%C3%A1si%E2%80%93Albert_model
 * Complexity: O(V * newEdgesPerNode)
 */
public class BarabasiAlbert extends GraphGenerator {

	protected int initialNetworkSize = 5;
	protected int size;
	protected int newEdgesPerNode;
	
	/**
	 * Creates a Barabási–Albert graph, which is scale-free and has
	 * a better than random clustering coefficient.
	 * 
	 * @param nodes network size
	 * @param newEdgesPerNode number of old nodes each new node tries to attach to
	 */
	public BarabasiAlbert(int nodes, int newEdgesPerNode) {
		this.size = nodes;
		this.newEdgesPerNode = newEdgesPerNode;
		if (initialNetworkSize < newEdgesPerNode) {
			initialNetworkSize = newEdgesPerNode;
		}
	}

	@Override
	public Graph generate() {
		// start with some initial network
		GraphGenerator gen = new ErdosRenyi(initialNetworkSize, 3 * initialNetworkSize);
		gen.initComponent(this);
		Graph g = gen.generate();
		
		
		// add rest of nodes
		for (int i = initialNetworkSize; i < this.size; i++) {		
			g.newNode();
			
			// add 'newEdgesPerNode' channels
			float totalDegrees = g.channels().size() * 2;
			for (int n = 0; n < newEdgesPerNode; n++) {
				float rand = random.getFloat();
				float degrees = 0;
				for (int j = 0; j < i; j++) {
					degrees += g.node(j).getDegree();
					if (rand * totalDegrees <= degrees) {
						g.newChannel(i, j);
						break;
					}
				}
			}
		}
		
		return g;
	}
	
	@Override
	public String toString() {
		return String.format("BarabasiAlbert(%d, %d)", size, newEdgesPerNode);
	}

}
