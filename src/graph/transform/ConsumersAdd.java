package graph.transform;

import java.util.ArrayList;
import graph.Graph;
import graph.Node;

/**
 * Adds consumer nodes which attach to gateway nodes.
 * Complexity: O(V) + O(CONSUMERS * CONNECTIONS)
 */
public class ConsumersAdd extends Transformation {

	private int consumers, connections, capacity;
	
	/**
	 * Add the given number of consumers connecting
	 * to a given numer of random gateway nodes each.
	 */
	public ConsumersAdd(int consumers, int connections, int capacity) {
		this.consumers = consumers;
		this.connections = connections;
		this.capacity = capacity;
	}
	
	@Override
	public void transform(Graph g) {
		ArrayList<Node> gateways = g.nodesWithRole(Node.ROLE_GATEWAY);
		if (gateways.isEmpty()) {
			throw new IllegalStateException("No gateway nodes exist to connect consumers with.");
		}
		for (int i = 0; i < consumers; i++) {
			Node consumer = g.newNode();
			consumer.setRole(Node.ROLE_CONSUMER);
			for (int k : random.getDistictInts(connections, gateways.size())) {
				g.newChannel(consumer.getID(), gateways.get(k).getID(), capacity / 2, capacity / 2);
			}
		}
		// reset consumer channels
		g.onChannelUpdate(ch -> {
			//int threshold = 10000*BIT, refill = 100000*BIT;
			/*if (g.node(ch.getNode1()).hasRole(Node.ROLE_CONSUMER) && !ch.canPay(ch.getNode1(), threshold)) {
				ch.setCapacities(ch.getCapacity1() + refill, ch.getCapacity2());
			} else if (g.node(ch.getNode2()).hasRole(Node.ROLE_CONSUMER)  && !ch.canPay(ch.getNode2(), threshold)) {
				ch.setCapacities(ch.getCapacity1(), ch.getCapacity2() + refill);
			}*/
			if ((g.node(ch.getNode1()).hasRole(Node.ROLE_CONSUMER) || g.node(ch.getNode2()).hasRole(Node.ROLE_CONSUMER)))
				ch.setCapacities(ch.getCapacity() / 2, ch.getCapacity() / 2);
		});
	}

	@Override
	public String toString() {
		return String.format("ConsumersAdd(%d, %d, %d)", consumers, connections, capacity);
	}
}
