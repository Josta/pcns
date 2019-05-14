package graph.transform;

import graph.Graph;
import graph.Node;

/**
 * Selects the nodes with the lowest degree and makes them gateway nodes.
 * Complexity: O(V)
 * @author Josua
 */
public class GatewaysSelectByLowDegree extends Transformation {

	private int count;
	
	/**
	 * Selects the nodes with the lowest degree and makes them gateway nodes.
	 */
	public GatewaysSelectByLowDegree(int count) {
		this.count = count;
	}
	
	@Override
	public void transform(Graph graph) {
		graph.nodes().stream()
			.sorted((n, m) -> n.getDegree() - m.getDegree()).limit(count)
			.forEach(n -> n.setRole(Node.ROLE_GATEWAY));
	}
	
	@Override
	public String toString() {
		return String.format("GatewaysSelectByLowDegree(%d)", count);
	}

}
