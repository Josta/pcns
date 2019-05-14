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

/** A distributed (minimal) spanning tree generator (every node stores next hop) **/
public class SpanningTreeGenerator {

	private Simulation sim;
	private RoutingCosts costs;
	private int size, root;
	private long[] distance;
	private int[] parent;
	private Consumer<SpanningForest> handler;

	/** A distributed  (minimal) spanning tree generator (every node stores next hop) **/
	public SpanningTreeGenerator(Simulation sim, RoutingCosts costs,
			int root, Consumer<SpanningForest> handler) {
		this.sim = sim;
		this.costs = costs;
		this.size = sim.graph().size();	
		this.root = root;
		this.parent = Lists.initIntArray(size, -1);
		this.distance = Lists.initLongArray(size, Integer.MAX_VALUE);
		this.handler = handler;
		//createInstantly(); // TODO: switch to traffic creation
		createByTraffic();
	}
	
	public SpanningForest getTree() {
		return new SpanningForest(parent);
	}
	
	/** Creates the spanning tree without using messaging **/
	public void createInstantly() {
		// Dijkstra algorithm
		boolean[] visited = Lists.initBoolArray(size, false);
		PriorityQueue<Integer> heap = new PriorityQueue<>(
			(id1, id2) -> (int) (distance[id1] - distance[id2]));
		distance[root] = 0;
		heap.add(root);
		while (!heap.isEmpty()) {
			int n1 = heap.poll();
			visited[n1] = true;
			Node node = sim.graph().node(n1);
			if (node.hasRole(Node.ROLE_CONSUMER)) continue;
			for (Channel ch : sim.graph().node(n1).channels()) {
				int n2 = ch.getOtherNode(n1);
				if (!visited[n2]) {
					long dist = distance[n1] + costs.getCosts(ch, n1);
					if (dist < distance[n2]) {
						parent[n2] = n1;
						distance[n2] = dist;
						heap.add(n2);
					}
				}
			}
		}
		Event.make(() -> handler.accept(getTree())).now(sim);	
	}
	
	/** Network traffic based creation of spanning tree with flooding **/
	public void createByTraffic() {
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
				propagate = true;
			} else {
				// relaxate edge
				long dist = distSender + costs.getCosts(nodeObj.getChannelTo(sender), node);
				if (dist < distance[node]) {
					parent[node] = sender;
					distance[node] = dist;
					propagate = true;
				}
			}
			// consumers are always leaves
			if (nodeObj.hasRole(Node.ROLE_CONSUMER))
				propagate = false;
			// update neighbors
			if (propagate)			
				IntStream.of(nodeObj.neighbors())
					.filter(id -> id != sender)
					.forEach(id -> new TreeUpdate(distance[node]).send(node, id, sim));
		}
	}

}
