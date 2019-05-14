package routing.algorithm;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import core.event.Event;
import core.event.LocalEvent;
import core.event.Message;
import graph.Channel;
import graph.Node;
import payment.Payment;
import utility.Pair;
import utility.lib.Lists;

public class FlareRouting extends RoutingAlgorithm {
	
	private List<FlareNode> flareNodes;
	private int[] addresses;
	
	private static final float BEACON_INTERVAL = 10;
	private static final float RESET_INTERVAL = 60;
	private static final float SUBSCRIBE_INTERVAL = 60;
	private static final float CHANNEL_CHANGE_DELAY = 1;
	private static final float PROPAGATION_DELAY = 3;
	
	private static final int PROPAGATION_RADIUS = 2;
	
	private static final int MAX_BEACONS = 5;
	private static final int MAX_BEACON_DISTANCE = 100;
	private static final int BEACON_REACTIVATE_COUNT = 5;
	
	// temporary fields for routing
	private boolean[] visited;
	private int[] hops, prev;
	private int beaconResponses;
	
	public FlareRouting() {
	}	
	
	@Override
	public void prepare() {
		flareNodes = Lists.initArray(graph().size(), id -> new FlareNode(id));
		addresses = new int[graph().size()];
		Arrays.setAll(addresses, i -> random.get().nextInt());
		visited = new boolean[graph().size()];
		hops = new int[graph().size()];
		prev = new int[graph().size()];
		graph().onChannelUpdate(ch -> digestChannelUpdate(ch));
		graph().channels().forEach(ch -> digestChannelUpdate(ch));	
		new TickBeacons().now(sim);
		//new TickReset().now(sim);	
		new TickSubscribe().now(sim);	
	}
	
	@Override
	public void findPaths(Payment p) {
		FlareNode source = flareNodes.get(p.getSource()),
				  target = flareNodes.get(p.getTarget());
		Set<Integer> channels = new HashSet<>(source.channelStates.keySet());
		channels.addAll(target.channelStates.keySet());
		List<Integer> path = source.getPathTo(p.getTarget(), channels);
		if (path != null)
			p.addPath(Lists.array(path));
		Set<Beacon> beacons = new HashSet<Beacon>();
		beacons.addAll(source.beacons);
		beacons.addAll(target.beacons);
		beaconResponses = 0;
		if (beacons.isEmpty())
			p.selectRoutes();
		for (Beacon beacon : beacons) {
			new ChannelStatesRequest(chs -> {
				for (Channel ch : chs)
					channels.add(ch.getID());
				List<Integer> path2 = source.getPathTo(p.getTarget(), channels);
				if (path2 != null)
					p.addPath(Lists.array(path2));
				if (++beaconResponses == beacons.size())
					p.selectRoutes();
			}).send(source.id, beacon.id, sim);
		}
	}

	private void digestChannelUpdate(Channel channel) {
		//System.out.println("DIGEST CHANNEL " + channel.getNode1() + " <> " + channel.getNode2());
		// TODO: neighbor_hello()?
		Channel ch = channel.clone().setTimestamp(sim.getTime());
		
		flareNodes.get(ch.getNode1()).addUpdate(
			new ChannelStateUpdate(ch, PROPAGATION_RADIUS));
		flareNodes.get(ch.getNode2()).addUpdate(
			new ChannelStateUpdate(ch, PROPAGATION_RADIUS));
		new TickUpdates().at(ch.getNode1()).after(CHANNEL_CHANGE_DELAY, sim);
		new TickUpdates().at(ch.getNode2()).after(CHANNEL_CHANGE_DELAY, sim);
	}
	
	
	
	public int distance(int node1, int node2) {
		int a = addresses[node1];
		int b = addresses[node2];
		return Math.abs(a - b); 
	}
	
	public class FlareNode {
		int id; // Node ID
		String address; // random binary
		Set<Integer> nodes; // all the nodes we know through channel updates
		HashMap<Integer, Channel> channelStates; // all the channels we know
		Set<Beacon> beacons;
		List<Integer> subscribers, subscribed;
		List<ChannelStateUpdate> channelUpdates; // channels + TTL, to be propagated
		List<Object> routingUpdates; // ??

		public FlareNode(int id) {
			this.id = id;
			nodes = new HashSet<>();
			channelStates = new HashMap<>();
			beacons = new HashSet<>();
			subscribers = new LinkedList<>();
			subscribed = new LinkedList<>();
			channelUpdates = new LinkedList<>();
			routingUpdates = new LinkedList<>();
		}
		
		public void addUpdate(ChannelStateUpdate update) {
			if (setChannel(update.channel) && (update.ttl > 1))				
				channelUpdates.add(new ChannelStateUpdate(update.channel, update.ttl - 1));
		}

		public boolean setChannel(Channel ch) {
			Channel oldChannel = channelStates.get(ch.getID());
			if (oldChannel == null || ch.isNewerThan(oldChannel)) {
				channelStates.put(ch.getID(), ch);
				nodes.add(ch.getNode1());
				nodes.add(ch.getNode2());
				//System.out.println(id + " accepting update of channel " + ch.getNode1() +"<>"+ ch.getNode2());
				return true;
			}
			return false;
		}
		
		public List<Integer> neighbors() {
			return graph().node(id).neighborsList();
		}
		
		/** Finds a path and converts it into a list of channels **/
		public List<Channel> getChannelsTo(int node) {		
			if (node < 0) return null;
			List<Integer> path = getPathTo(node, channelStates.keySet());
			return IntStream.range(0, path.size() - 1)
				.mapToObj(i -> channelStates.get(graph()
					.channel(path.get(i), path.get(i+1)).getID()))
				.collect(Collectors.toList());
		}

		/** Finds a path to the target using only the local graph view **/
		public List<Integer> getPathTo(int target) {
			return getPathTo(target, channelStates.keySet());
		}
		
		/**
		 * Detects routes on the real graph, but restricts search
		 * to channels in the given channel set.
		 */
		public List<Integer> getPathTo(int target, Set<Integer> localChannels) {
			PriorityQueue<Integer> heap = new PriorityQueue<>(
					(e1, e2) -> (int) (hops[e1] - hops[e2]));
			Arrays.fill(visited, false);
			Arrays.fill(hops, Integer.MAX_VALUE);
			hops[target] = 0;
			prev[target] = -1;
			heap.add(target);
			while (!heap.isEmpty()) {
				int n1 = heap.poll();
				visited[n1] = true;
				if (n1 == id) break;
				/*if ((n1 != target) && graph().node(n1).hasRole(Node.ROLE_CONSUMER)) continue;*/
				for (Channel ch : graph().node(n1).channels()) {
					if (!localChannels.contains(ch.getID())) continue; // TODO maybe bloom filter?
					int n2 = ch.getOtherNode(n1);
					if (visited[n2]) continue;
					int n2Hops = hops[n1] + 1;
					if (n2Hops >= hops[n2]) continue;	
					hops[n2] = n2Hops;
					prev[n2] = n1;
					heap.add(n2);
				}
			}
			if (visited[id]) {
				LinkedList<Integer> path = new LinkedList<>();
				int nd = id;
				while (nd >= 0) {
					path.add(nd);
					nd = prev[nd];
				}
				return path;
			}
			return null;
		}
	}
	
	/** Information on a beacon **/
	public class Beacon {	
		private int id, distance;
		public Beacon (int node, int distance) {
			this.id = node;
			this.distance = distance;
		}
	}
	
	/** Propagatable update **/
	public class ChannelStateUpdate {
		Channel channel;
		int ttl;
		public ChannelStateUpdate(Channel channel, int ttl) {
			this.channel = channel;
			this.ttl = ttl;
		}
	}
	
	// UPDATE PROPAGATION (to neighbors and subscribers)
	
	/** Triggers topology forwarding **/
	public class TickUpdates extends LocalEvent {
		@Override
		public void run() {
			FlareNode lg = flareNodes.get(node);		
			if (lg.channelUpdates.size() == 0 && lg.routingUpdates.size() == 0) return;
			//System.out.println("PROP " + node + " > "+lg.neighbors()+": " + lg.channelUpdates.size() + " updates");
			lg.neighbors().stream().forEach(n -> {
				new NeighborUpdate(lg.routingUpdates, lg.channelUpdates) 
					.send(node, n, sim); // TODO routing updates w/o those sent by neighbor
			});
			lg.channelUpdates.forEach(u -> {
				lg.subscribers.forEach(s ->
					new DynamicInfo(u.channel).send(node, s, sim));
			});
			lg.channelUpdates.clear();
			lg.routingUpdates.clear();
		}
	}
	
	/** Topology gossiping. Shares foreign static link state updates **/
	public class NeighborUpdate extends Message {
		//private List<Object> routingTableUpdates;
		private List<ChannelStateUpdate> channelStateUpdates;
		public NeighborUpdate(List<Object> routingTableUpdates, List<ChannelStateUpdate> channelStateUpdates) {
			//this.routingTableUpdates = new LinkedList<>(routingTableUpdates);
			this.channelStateUpdates = new LinkedList<>(channelStateUpdates);
		}
		@Override
		public void run() {
			FlareNode lg = flareNodes.get(node);
			// recreate routing table (include)
			// beacon_req to new nodes
			//lg.routingUpdates.addAll(routingTableUpdates);
			
			// digest and cache channel state updates
			for (ChannelStateUpdate update : channelStateUpdates)
				lg.addUpdate(update);
			
			// schedule forwarding of cached updates
			new TickUpdates().at(node).after(PROPAGATION_DELAY, sim);
		}
	}
	
	
	
	// RESETS (?)
	
	/** Asks neighbors to send us updates on all unknown channels. **/
	public class TickReset extends Event {
		@Override
		public void run() {
			for (Node node : graph().nodes()) {
				FlareNode lg = flareNodes.get(node.getID());
				Set<Integer> channelIds = lg.channelStates.values().stream()
					.map(ch -> ch.getID()).collect(Collectors.toSet());
				node.neighborsList().stream().forEach(neighbor -> {
					new NeighborReset(channelIds)
						.send(node.getID(), neighbor, sim);
				});
			}
			new TickReset().after(RESET_INTERVAL, sim);
		}
	}
	
	/** Requests updates for all channels except the given ones **/
	public class NeighborReset extends Message {
		private Set<Integer> channels;
		public NeighborReset(Set<Integer> channels) {
			this.channels = channels;
		}
		@Override
		public void run() {
			FlareNode lg = flareNodes.get(node);
			List<ChannelStateUpdate> updates = lg.channelStates.values().stream()
				.filter(ch -> !channels.contains(ch.getID()))
				.map(ch -> new ChannelStateUpdate(ch, 0))
				.collect(Collectors.toList());
			new NeighborUpdate(null, updates); // TODO routing table updates?
		}
	}
	
	
	
	// BEACON MANAGEMENT
	
	/** Asks random known nodes to become our beacons **/
	public class TickBeacons extends Event {
		@Override
		public void run() {
			for (Node node : graph().nodes()) {
				for (int i = 0; i < BEACON_REACTIVATE_COUNT; i++) {
					FlareNode lg = flareNodes.get(node.getID());
					int beacon = random.getOne(lg.nodes);
					if (beacon != node.getID())
						new BeaconRequest(lg.getPathTo(beacon).size(),
							new LinkedList<>(lg.beacons))
							.send(node.getID(), beacon, sim);
				}
			}
			new TickBeacons().after(BEACON_INTERVAL, sim);
		}
	}
	
	/** 
	 * A beacon request. In this implementation, the message is sent directly
	 * instead of via onion routing.
	 **/
	public class BeaconRequest extends Message {
		private int hops;
		private List<Beacon> excluded;
		public BeaconRequest(int hops, List<Beacon> excluded) {
			this.hops = hops;
			this.excluded = excluded;
		}
		@Override
		public void run() {
			FlareNode lg = flareNodes.get(node);
			int distance2me = distance(sender, node);
			int altBeacon = lg.nodes.stream()
				.map(id -> new Pair<Integer,Integer>(id, distance(sender, id)))
				.filter(p -> p.second < distance2me && p.first != sender
					&& !excluded.stream().anyMatch(e -> e.id == p.first))
				.sorted((p1, p2) -> p1.second - p2.second)
				.map(p -> p.first)
				.findFirst().orElse(-1);
			List<Channel> altPath = lg.getChannelsTo(altBeacon);
			if (altBeacon < 0 || hops + altPath.size() > MAX_BEACON_DISTANCE) {
				// accept request
				new BeaconAck(-1, null).send(node, sender, sim);	
			} else {
				// recommend alternative with closer address
				new BeaconAck(altBeacon, altPath).send(node, sender, sim);
			}
		}
	}
	
	/**
	 * Response to a beacon request. Either communicates acceptance to be a beacon,
	 * or a beacon alternative. In this implementation, the message is sent directly
	 * instead of via onion routing.
	 */
	public class BeaconAck extends Message {
		private int altBeacon;
		private List<Channel> channels;
		public BeaconAck(int altBeacon, List<Channel> channels) {
			this.altBeacon = altBeacon;
			this.channels = channels;
		}
		@Override
		public void run() {
			FlareNode lg = flareNodes.get(node);
			// alternative => send new beacon request
			if (altBeacon >= 0) {
				for (Channel ch : channels)
					lg.setChannel(ch);
				if (lg.nodes.contains(altBeacon))
					new BeaconRequest(lg.getPathTo(altBeacon).size(),
						new LinkedList<>(lg.beacons)).send(node, altBeacon, sim);
				return;
			}
			// try to adopt beacon
			if (!lg.nodes.contains(sender)) {
				// duh, too far away now
			} else if (lg.beacons.stream().anyMatch(b -> b.id == sender)) {
				// we already have that beacon
			} else if (lg.beacons.size() < MAX_BEACONS) {
				// adding beacon
				lg.beacons.add(new Beacon(sender, distance(sender, node)));
			} else if (lg.beacons.stream()
					.anyMatch(b -> b.distance > distance(node, sender))) {
				// replacing inferior beacon
				Beacon old = lg.beacons.stream()
					.filter(b -> b.distance > distance(node, sender))
					.findFirst().get();
				lg.beacons.remove(old);
				lg.beacons.add(new Beacon(sender, distance(sender, node)));
			} else {
				// ignore beacon
			}
			//TODO: prune graph
		}	
	}
	
	
	
	// SUBSCRIPTIONS (to updates from beacons and paths thereto)
	
	/** Manages our subscriptions to other nodes' channel updates **/
	public class TickSubscribe extends Event {
		@Override
		public void run() {
			for (Node node : graph().nodes()) {
				// we want subscriptions to all beacons and paths to them
				FlareNode lg = flareNodes.get(node.getID());
				List<Integer> subscribedNew = lg.beacons.stream()
					.flatMap(beacon -> lg.getPathTo(beacon.id).stream())
					.collect(Collectors.toList());
				subscribedNew.remove(new Integer(node.getID()));
				// add new subscriptions, remove old ones
				lg.subscribed.stream()
					.filter(n -> !subscribedNew.contains(n))
					.forEach(id -> new Unsubscribe().send(lg.id, id, sim));
				subscribedNew.stream()
					.filter(n -> ! lg.subscribed.contains(n))
					.forEach(id -> new Subscribe().send(lg.id, id, sim));
				lg.subscribed = subscribedNew;
			}
			new TickSubscribe().after(SUBSCRIBE_INTERVAL, sim);
		}
	}
	
	/** Tells a node that we want to get regular dynamic info **/
	public class Subscribe extends Message {
		@Override
		public void run() {
			FlareNode lg = flareNodes.get(node);
			lg.subscribers.add(sender);
			// immediately inform about owned channels
			for (Channel ch : lg.channelStates.values()) {
				if (ch.getNode1() == node || ch.getNode2() == node)
					new DynamicInfo(ch).send(node, sender, sim);
			}
			
		}
	}
	
	/** Tells a node that we don't want regular channel updates anymore **/
	public class Unsubscribe extends Message {
		@Override
		public void run() {
			FlareNode lg = flareNodes.get(node);
			lg.subscribers.remove(new Integer(sender));
		}
	}
	
	/** Update sent to subscribers **/
	public class DynamicInfo extends Message {
		private Channel channel;
		public DynamicInfo(Channel channel) {
			this.channel = channel;
		}
		@Override
		public void run() {
			FlareNode lg = flareNodes.get(node);
			lg.setChannel(channel);
		}	
	}
	
	
	
	// PATH FINDING
	
	/** Requests topology from a beacon **/
	public class ChannelStatesRequest extends Message {
		private Consumer<Collection<Channel>> handler;
		public ChannelStatesRequest(Consumer<Collection<Channel>> handler) {
			this.handler = handler;
		}
		@Override
		public void run() {
			FlareNode lg = flareNodes.get(node);
			new ChannelStatesResponse(handler, lg.channelStates.values()).send(node, sender, sim);
		}
	}

	/** Topology response from a beacon **/
	public class ChannelStatesResponse extends Message {
		private Consumer<Collection<Channel>> handler;
		private Collection<Channel> channels;
		public ChannelStatesResponse(Consumer<Collection<Channel>> handler, Collection<Channel> channels) {
			this.handler = handler;
			this.channels = channels;
		}
		@Override
		public void run() {
			handler.accept(channels);
		}
	}
	
	
	
	@Override
	public long estimateStorage() {
		return 0;
	}

	@Override
	public String toString() {
		return String.format("FlareRouting()");
	}
}
