package graph.transform;

import graph.Graph;
import graph.Node;

/**
 * Selects the nodes with the lowest degree and makes them gateway nodes.
 * Complexity: O(V)
 * @author Josua
 */
public class GatewaysSelectRandomly extends Transformation {

	private int count;
	
	/**
	 * Selects the nodes with the lowest degree and makes them gateway nodes.
	 */
	public GatewaysSelectRandomly(int count) {
		this.count = count;
	}
	
	@Override
	public void transform(Graph graph) {
		random.getDistinct(graph.nodes(), count).forEach(n -> n.setRole(Node.ROLE_GATEWAY));
	}
	
	@Override
	public String toString() {
		return String.format("GatewaysSelectRandomly(%d)", count);
	}

}
