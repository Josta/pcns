package routing.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.javamex.classmexer.MemoryUtil;

import core.event.Event;
import core.event.Message;
import graph.Channel;
import graph.Node;
import payment.Payment;
import utility.global.Config;
import utility.lib.Lists;

/**
 * Stores a complete view of the graph on each node and calculates routes with Dijkstra.
 * This implementation only returns a singular, unsplit payment route.
 */
public class SourceGraphRouting extends RoutingAlgorithm {
	
	private double MAX_FEE_PERCENT;
	private double BROADCAST_INTERVAL;
	private int pathCount;
	private ArrayList<LocalGraph> localGraphs;
	
	// path finding temporary data
	private boolean[] visited;
	private long[] distance;
	private int[] amount;
	private int[] prev;
	
	public SourceGraphRouting(double broadcastInterval, int pathCount) {
		this.BROADCAST_INTERVAL = broadcastInterval;
		this.pathCount = pathCount;
	}
	
	@Override
	public void prepare() {
		MAX_FEE_PERCENT = Config.getDouble("MAX_FEE_PERCENT");
		visited = new boolean[graph().size()];
		distance = new long[graph().size()];
		amount = new int[graph().size()];
		prev = new int[graph().size()];
		LocalGraph localGraph = new LocalGraph();
		localGraphs = Lists.initArray(graph().size(), i -> new LocalGraph(localGraph));
		
		// flood on fee change
		sim.feePolicy().onFeeUpdate(ch -> {
			Channel clone = ch.clone().setTimestamp(sim.getTime());
			Set<Channel> list = new HashSet<>();
			list.add(clone);
			new ChannelUpdate(list).send(ch.getNode1(), ch.getNode2(), sim);
			new ChannelUpdate(list).send(ch.getNode2(), ch.getNode1(), sim);
		});
		new ChannelBroadcastEpoch().now(sim);
	}
	
	@Override
	public void findPaths(Payment p) {
		Set<Integer> excludedChannels = new HashSet<>();
		for (int k = 0; k < pathCount; k++) {
			int[] path = Lists.array(findShortestPath(p.getSource(),
					p.getTarget(), p.getAmount(), excludedChannels));
			if (path == null) break;
			p.addPath(path);
			excludedChannels.addAll(IntStream.range(0, path.length - 1)
				.mapToObj(i -> graph().channel(path[i], path[i+1]).getID())
				.collect(Collectors.toList()));
		}
		p.selectRoutes();
	}
	
	
	
	private List<Integer> findShortestPath(int source, int target, int amt, Set<Integer> excludedChannels) {
		// source local view of the graph
		LocalGraph lg = localGraphs.get(source);
		
		Arrays.fill(visited, false);
		Arrays.fill(distance, Long.MAX_VALUE);
		//Arrays.fill(amount, -1); // not necessary to reset
		//Arrays.fill(prev, -1); // not necessary to reset
		
		PriorityQueue<Integer> heap = new PriorityQueue<>(
			(id1, id2) -> (int) (distance[id1] - distance[id2]));

		// start search at target node
		distance[target] = 0;
		amount[target] = amt;
		heap.add(target);

		while (!heap.isEmpty()) {
			// visit known node which is closest to target
			int n1 = heap.poll();
			int n1Amount = amount[n1];
			visited[n1] = true;
			
			// if it's the source we're finished
			if (n1 == source) break;
			
			// consumers are always leaves
			if ((n1 != target) && graph().node(n1).hasRole(Node.ROLE_CONSUMER)) continue;

			// relaxate all edges
			for (Channel ch : lg.nodeChannels[n1]) {
				if (excludedChannels.contains(ch.getID())) continue;
				
				int n2 = ch.getOtherNode(n1);
				
				// TODO reject disabled, non-source channels
				if (visited[n2] || !ch.canPay(n2, n1Amount)) continue;
				
				// total fees too high / no timelock delta?
				int n2Amount = n1Amount + ch.getFee(n2, n1Amount);
				if ((n2Amount > (1 + MAX_FEE_PERCENT * 0.01) * amt)
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
		if (distance[source] < Long.MAX_VALUE) {
			LinkedList<Integer> path = new LinkedList<>();
			for (int node = source; node != target; ) {
				path.add(node);
				node = prev[node];
			}
			path.add(target);
			return path;
		}
		return null;
	}

	/** Routing info data stored for each node **/
	public class LocalGraph {
		private Channel[][] nodeChannels;
		private HashSet<Channel> updates;
		/** New LocalGraph from real graph **/
		public LocalGraph() {		
			nodeChannels = graph().nodes().stream()
				.map(n -> n.channels().stream()
					.map(ch -> ch.clone().setTimestamp(0))
					.toArray(Channel[]::new))
				.toArray(Channel[][]::new);
			updates = new HashSet<>();
		}
		/** Deep copy, but shares Channel objects **/
		public LocalGraph(LocalGraph g) {		
			nodeChannels = IntStream.range(0, g.nodeChannels.length)
				.mapToObj(i -> g.nodeChannels[i].clone())
				.toArray(Channel[][]::new);
			updates = new HashSet<>();
		}
		public void replaceChannels(Set<Channel> channels, int sender) {
			for (Channel ch : channels) {
				int i1 = -1, i2 = -1;
				Channel[] c1 = nodeChannels[ch.getNode1()],
						  c2 = nodeChannels[ch.getNode2()];	
				for (int i = 0; i < c1.length; i++)
					if (c1[i].getID() == ch.getID()) {
						i1 = i;
						break;
					}
				for (int i = 0; i < c2.length; i++)
					if (c2[i].getID() == ch.getID()) {
						i2 = i;
						break;
					}
				Channel och = c1[i1];
				c1[i1] = c2[i2] = ch;
				if (ch.isNewerThan(och))
					updates.add(ch);
			}
		}
	}
	
	/** Informs about channel changes via flooding **/
	public class ChannelUpdate extends Message {
		private Set<Channel> channels;
		/** Expects effectively final channel objects **/
		public ChannelUpdate(Set<Channel> channels) {
			this.channels = channels;
		}
		@Override
		public void run() {
			localGraphs.get(node).replaceChannels(channels, sender);
		}
	}
	
	/** Regularly triggers forwarding of channel updates **/
	private class ChannelBroadcastEpoch extends Event {
		@Override
		public void run() {		
			// TODO maybe as local event? => more events but not synced
			graph().nodes().forEach(n -> {
				LocalGraph lg = localGraphs.get(n.getID());
				if (lg.updates.size() > 0) {
					n.neighborsList().forEach(receiver -> {
						new ChannelUpdate(lg.updates).send(n.getID(), receiver, sim);	
					});
					lg.updates = new HashSet<>();
				}
			});
			new ChannelBroadcastEpoch().after((float) BROADCAST_INTERVAL, sim);
		}	
	}
	
	@Override
	public long estimateStorage() {
		int REFSIZE = 32; 
		int channels = graph().channels().size();
		int nodes = graph().size();
		// array[E] of channels
		long channelRAM = channels * (REFSIZE + MemoryUtil.memoryUsageOf(graph().channel(0)));
		// array[V] of array, with each channel ID appearing twice in second layer
		long nodemapRAM =  nodes * 32 + channels * 2 * 32;
		return nodes * (channelRAM + nodemapRAM);
	}
	
	@Override
	public String toString() {
		return "SourceGraphRouting()";
	}
	
}
