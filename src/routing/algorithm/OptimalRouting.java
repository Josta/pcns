package routing.algorithm;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.PriorityQueue;

import graph.Channel;
import graph.Node;
import payment.Payment;
import utility.global.Config;
import utility.lib.Lists;

/** Gets the optimal singular route (without respecting decentrality) **/
public class OptimalRouting extends RoutingAlgorithm {
	private double MAX_FEE_PERCENT;
	
	// path finding temporary data
	private boolean[] visited;
	private long[] distance;
	private int[] amount; // SAT
	private int[] prev;
	
	@Override
	public void prepare() {
		MAX_FEE_PERCENT = Config.getDouble("MAX_FEE_PERCENT");
		visited = new boolean[graph().size()];
		distance = new long[graph().size()];
		amount = new int[graph().size()];
		prev = new int[graph().size()];
	}

	@Override
	public void findPaths(Payment p) {	
		Arrays.fill(visited, false);
		Arrays.fill(distance, Long.MAX_VALUE);
		Arrays.fill(amount, -1);
		Arrays.fill(prev, -1);	
		PriorityQueue<Integer> heap = new PriorityQueue<>(
			(id1, id2) -> (int) (distance[id1] - distance[id2]));
		
		// start search at target node
		distance[p.getTarget()] = 0;
		amount[p.getTarget()] = p.getAmount();
		heap.add(p.getTarget());
		
		while (!heap.isEmpty()) {		
			// visit known node which is closest to target
			int n1 = heap.poll();
			int n1Amount = amount[n1];
			visited[n1] = true;
			
			// if it's the source we're finished
			if (n1 == p.getSource()) break;
			
			// consumers are always leaves
			if ((n1 != p.getTarget()) && graph().node(n1).hasRole(Node.ROLE_CONSUMER)) continue;
			
			// relaxate all edges
			for (Channel ch : graph().node(n1).channels()) {
				int n2 = ch.getOtherNode(n1);
	
				// TODO reject disabled, non-source channels
				if (visited[n2] || !ch.canPay(n2, n1Amount)) continue;
			
				// total fees too high / no timelock delta?
				int n2Amount = n1Amount + ch.getFee(n2, n1Amount);
				if ((n2Amount > (1 + MAX_FEE_PERCENT * 0.01) * p.getAmount())
					|| (ch.getMinTimelockDelta() == 0)) continue;
				
				// new node distance not better?	
				//long n2Dist = distance[node] + sim.costs().getCosts(ch, node, amount);
				long n2Dist = distance[n1] + ch.getFee(n2, n1Amount);
				if (n2Dist >= distance[n2]) continue;
				
				// accept edge
				distance[n2] = n2Dist;
				amount[n2] = n2Amount;
				prev[n2] = n1;
				heap.add(n2);
			}
		}
		
		// retrace found path
		if (prev[p.getSource()] >= 0) {
			LinkedList<Integer> path = new LinkedList<>();
			for (int node = p.getSource(); node != p.getTarget(); ) {
				path.add(node);
				node = prev[node];
			}
			path.add(p.getTarget());
			p.addPath(Lists.array(path));
		}
		p.selectRoutes();
	}
	
	@Override
	public long estimateStorage() {
		return graph().size() * (8 + 64 + 64 + 32);
	}
	
	@Override
	public String toString() {
		return "OptimalRouting()";
	}
	
}
