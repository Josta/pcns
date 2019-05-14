package graph.transform;

import graph.Graph;
import graph.Node;

/**
 * Assigns the same role to all nodes.
 * Complexity: O(V)
 * @author Josua
 */
public class NodeRolesSetUniformly extends Transformation {

	private int role;

	/**
	 * Assigns the given role to all nodes.
	 */
	public NodeRolesSetUniformly(int role) {
		this.role = role;
	}
	
	@Override
	public void transform(Graph graph) {	
		for (Node node : graph.nodes()) {
			node.setRole(role);
		}
	}
	
	@Override
	public String toString() {
		return String.format("GatewaysSelectRandomly(%d)", role);
	}

}
