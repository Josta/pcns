package routing.util;

import java.util.PriorityQueue;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import core.Simulation;
import core.event.Event;
import core.event.Message;
import graph.Channel;
import graph.Node;
import routing.costs.RoutingCosts;
import utility.lib.Lists;

/**
 * A distributed (minimal) spanning tree forest generator (every node
 * stores next hop in the direction of the closest root node)
 */
public class SpanningForestGenerator {

	private Simulation sim;
	private RoutingCosts costs;
	private int size;
	private int[] roots, parent, partition;
	private long[] distance;
	private Consumer<SpanningForest> handler;

	/**
	 * A distributed (minimal) spanning tree forest generator (every node
	 * stores next hop in the direction of the closest root node)
	 */
	public SpanningForestGenerator(Simulation sim, RoutingCosts costs,
			int[] roots, Consumer<SpanningForest> handler) {
		this.sim = sim;
		this.costs = costs;
		this.size = sim.graph().size();	
		this.roots = roots;
		this.parent = Lists.initIntArray(size, -1);
		this.partition = Lists.initIntArray(size, -1);
		this.distance = Lists.initLongArray(size, Integer.MAX_VALUE);
		this.handler = handler;
		//createInstantly(); // TODO: switch to traffic creation
		createByTraffic();
	}
	
	public SpanningForest getTree() {
		return new SpanningForest(parent);
	}
	
	public long[] getDistances() {
		return distance;
	}
	
	public int getPartitionFor(int node) {
		return partition[node];
	}
	
	/** Creates the spanning tree without using messaging **/
	public void createInstantly() {
		// modified Dijkstra algorithm
		boolean[] visited = Lists.initBoolArray(size, false);
		PriorityQueue<Integer> heap = new PriorityQueue<>((id1, id2) -> (int) (distance[id1] - distance[id2]));	
		for (int root : roots) {
			distance[root] = 0;
			partition[root] = root;
			heap.add(root);
		}	
		while (!heap.isEmpty()) {
			int n1 = heap.poll();
			visited[n1] = true;
			Node node = sim.graph().node(n1);
			if (node.hasRole(Node.ROLE_CONSUMER)) continue;	
			for (Channel ch : node.channels()) {
				int n2 = ch.getOtherNode(n1);
				if (!visited[n2]) {
					long dist = distance[n1] + costs.getCosts(ch, n1);
					if (dist < distance[n2]) {
						parent[n2] = n1;
						distance[n2] = dist;
						partition[n2] = partition[n1];
						heap.add(n2);
					}
				}
			}
		}
		handler.accept(getTree());
	}
	
	/** Network traffic based creation of spanning tree with flooding **/
	public void createByTraffic() {
		for (int root : roots)
			new TreeUpdate(0).send(-1, root, sim);
		Event.make(() -> handler.accept(getTree())).after(10, sim);
	}
	
	/** A flooding message for MST generation **/
	public class TreeUpdate extends Message {
		private long distSender;
		public TreeUpdate(long distSender) {
			this.distSender = distSender;
		}
		@Override
		public void run() {
			Node nodeObj = sim.graph().node(node);		
			boolean propagate = false;
			if (sender < 0) {
				// root node special case
				distance[node] = 0;
				partition[node] = node;
				propagate = true;
			} else {
				// relaxate edge
				long dist = distSender + costs.getCosts(nodeObj.getChannelTo(sender), node);
				if (dist < distance[node]) {
					parent[node] = sender;
					distance[node] = dist;
					partition[node] = partition[sender];
					propagate = true;
				}
			}
			// consumers are always leaves
			if (nodeObj.hasRole(Node.ROLE_CONSUMER))
				propagate = false;
			// update neighbors
			if (propagate)
				IntStream.of(nodeObj.neighbors()).filter(id -> id != sender)
					.forEach(id -> new TreeUpdate(distance[node]).send(node, id, sim));
		}
	}

}
