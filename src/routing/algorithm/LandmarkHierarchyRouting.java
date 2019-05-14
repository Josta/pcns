package routing.algorithm;

import payment.Payment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import core.event.Message;
import core.event.RepeatedLocalEvent;
import utility.lib.Lists;
import utility.lib.Network;

/**
 * The landmark property has one Landmark object per node. Each landmark
 * - has a periodically run LandmarkManage event
 * - broadcasts/digests/forwards LandmarkUpdate messages
 * - has a LandmarkEntry for each landmark it knows
 * @author Josua
 */
public class LandmarkHierarchyRouting extends RoutingAlgorithm {

	private final int HYSTERESIS = 1;
	private final float ADOPTION_COOLDOWN = 5;
	private final int MAX_CHILDREN = 5;
	private float interval;
	private ArrayList<Landmark> landmarks;
	

	public LandmarkHierarchyRouting(float updateInterval) {
		this.interval = updateInterval;
	}
	
	@Override
	public void prepare() {
		landmarks = Lists.initArray(sim.graph().size(), id -> new Landmark(id));
		for (Landmark lm : landmarks) {
			new LandmarkManage().interval(interval).at(lm.id).after(0.1f * random.getFloat(), sim);
		}
	}
	
	@Override
	public void findPaths(Payment p) {
		String address = landmarks.get(p.getTarget()).address;
		List<Integer> path = new ArrayList<>();
		int node = p.getSource();
		while (node >= 0) {
			path.add(node);
			node = landmarks.get(node).getNextHop(address);
		}
		p.addPath(Lists.array(path));
		p.selectRoutes();
	}
	
	/** Returns a landmark **/
	public Landmark getLandmark(int node) {
		return landmarks.get(node);
	}
	
	/** A landmark as used in the landmark hierarchy. Note that every node is a landmark. **/
	public class Landmark {
		public String address, oldAddress;
		public int id, level, radius, parent;
		HashMap<String, LandmarkEntry> routingTable;
		HashMap<Integer, LandmarkEntry> mgmtTable;
		HashSet<LandmarkEntry> changedEntries;
		float lastParentChange, lastHeartbeat;
		int[] children;
		boolean changed;

		/** Initializes a level 0 landmark **/
		public Landmark(int id) {
			this.id = id;
			parent = -1;
			level = 0;
			children = Lists.initIntArray(MAX_CHILDREN, -1);
			address = null;
			oldAddress = null;
			radius = getInitialRadius();
			routingTable = new HashMap<>();
			mgmtTable = new HashMap<>();
			changedEntries = new HashSet<>();
			changed = true;
			lastParentChange = lastHeartbeat = 0;
		}
		
		private void log(String msg) {
			System.out.println(String.format("[%.2fs] %03d %s", sim.getTime(), id, msg));
		}
		
		/**
		 * Returns the ID of the next hop towards a given landmark address.
		 * @return ID of next node (-1 if unknown or arrived)
		 */
		public int getNextHop(String address) {
			String prefix = address;
			while (!routingTable.containsKey(prefix)) {
				prefix = prefix.substring(0, prefix.length() - 2);
				if (prefix.length() == 0)  return -1;
			}
			return (prefix.length() > 0) ? routingTable.get(prefix).nextHop : -1;
		}

		/** Called periodically to do tasks like elections **/
		public void manage() {
			
			/*if (id == 3 && sim.getTime() > 55f) {
				log("test");
			}*/
			
			// forward foreign updates
			for (LandmarkEntry e : changedEntries) {
				if (e.ttl > 0) {
					Network.flood(id, e.senders, sim, (n) ->
						new LandmarkUpdate(e.id, e.address,
							e.level, e.ttl - 1, e.satisfied,
							e.distance + 1, e.freePlaces, e.timestamp));
				}
			}
			changedEntries.clear();

			// childless demotion
			if (parentChangeAllowed() && level > 0 && childCount() == 0) {
				demoteSelf();
			}
			
			// too close peers demotion
			// TODO
			
			// promotion
			if (parentChangeAllowed() && parent < 0) {
				if (electable(level, level).count() < 2
						&& mgmtTable.values().stream().noneMatch(e -> (e.level > level))) {
					// we're at the global level, no more promotion
					sendUpdate();
				} else if (electable(level, level).filter(e -> !e.satisfied)
						.mapToInt(e -> e.id).min().orElse(id) == id) {
					promoteSelf();
				}
			}
			
			if (changed || lastHeartbeat > 100f) {
				sendUpdate();
				// remove outdated entries
				List<LandmarkEntry> oldEntries = mgmtTable.values().stream()
					.filter(e -> e.timestamp < sim.getTime() - 100f)
					.collect(Collectors.toList());
				oldEntries.forEach(e -> mgmtTable.remove(e.id));
			}
		}
		
		/** Triggered when an update message arrives at this landmark **/
		public void receiveUpdate(LandmarkUpdate m) {
			if (m.source == id) return;		
			// update entry
			LandmarkEntry entry = digest(m);		
			if (entry == null) return;
			changedEntries.add(entry);
			// we can't have a non-L+1 parent
			if (m.source == parent && m.level != level + 1) {
				parent = -1;
				lastParentChange = sim.getTime();
				radius = getInitialRadius();
				findParent();
				sendUpdate();
			}
			// accept as parent (we have none or this one is closer)
			if (m.source != parent && isAcceptableParent(entry) && setParent(m.source)) {
				//log("> " + m.source + " (ACCEPT) L" + m.level);
				sendUpdate();
			}
		}
		
		/** Creates a new LandmarkUpdate broadcast **/
		private void sendUpdate() {		
			int freePlaces = (int) IntStream.of(children).filter(c -> c < 0).count();
			digest(new LandmarkUpdate(id, address, level, radius,
				parent >= 0, 0, freePlaces, sim.getTime()));
			Network.flood(id, null, sim, (n) ->
				new LandmarkUpdate(id, address, level, radius,
					parent >= 0, 0, freePlaces, sim.getTime()));
			changed = false;
			lastHeartbeat = sim.getTime();
		}
		
		/** Sets a parent. May fail if new parent has too many children. **/
		private boolean setParent(int newParent) {
			if (newParent < 0) return false;
			// TODO: child application should be done via 3-way handshake
			String newAddress = landmarks.get(newParent).addChild(id);
			if (newAddress != null) {
				if (parent >= 0)
					landmarks.get(parent).removeChild(id);
				parent = newParent;
				lastParentChange = sim.getTime();
				oldAddress = address;
				address = newAddress;
				//radius = mgmtTable.get(parent).distance;
				changed = true;
				return true;
			}
			return false;
		}
		
		/** Promotes itself **/
		private void promoteSelf() {
			level++;
			radius = getInitialRadius();
			log("PROMO to L" + level);		
			// forget children, they'll know when they receive the update
			for (int i = 0; i < children.length; i++)
				children[i] = -1;
			findParent();
			sendUpdate();
		}
		
		/** Demotes itself (if it can find a new parent) **/
		private void demoteSelf() {
			if (setParent(electable(level - 1, level)
					.filter(e -> e.id != id && e.freePlaces > 0)
					.mapToInt(e -> e.id).min().orElse(-1))) {
				level--;
				radius = getInitialRadius();
				log("DEMO to L" + level);
				// forget children (if we even had any)
				for (int i = 0; i < children.length; i++)
					children[i] = -1;
				sendUpdate();
			}
		}
		
		/** Adopts the closest known potential parent **/
		private void findParent() {
			setParent(electable(level, level + 1)
				.filter(e -> e.freePlaces > 0)
				.sorted((e, f) -> f.distance - e.distance)
				.mapToInt(e -> e.id).findFirst().orElse(-1));
		}
		
		/** Adds a child, then returns assigned address (may fail by giving null) **/
		private String addChild(int child) {
			int slot = -1;
			for (int i = 0; i < children.length; i++) {
				if (children[i] < 0) slot = i;
			}
			if (slot < 0) return null;
			children[slot] = child;
			changed = true;
			return address + Integer.toHexString(slot);
		}
		
		/** Removes a child **/
		private void removeChild(int child) {
			for (int i = 0; i < children.length; i++) {
				if (children[i] == child) children[i] = -1;
			}
			changed = true;
		}
		
		/** Updates internal entry with update, then returns it **/
		private LandmarkEntry digest(LandmarkUpdate msg) {
			LandmarkEntry e = mgmtTable.get(msg.source);
			if (e == null) {
				e = new LandmarkEntry();
				e.distance = msg.distance;
				e.timestamp = -1f;
				mgmtTable.put(msg.source, e);
			}
			if (Float.compare(e.timestamp, msg.timestamp) == 0)
				e.senders.add(msg.getSender());
			if (Float.compare(e.timestamp, msg.timestamp) >= 0)
				return null;
			// new message: update general info
			e.senders.clear();
			if (msg.getSender() >= 0)
				e.senders.add(msg.getSender());
			e.id = msg.source;
			e.level = msg.level;
			e.freePlaces = msg.freePlaces;
			e.satisfied = msg.satisfied;
			if ((msg.address != null) && (e.address != msg.address)) {	
				routingTable.remove(e.address);
				routingTable.put(msg.address, e);
				e.address = msg.address;
			}
			// update next hop info?
			if (e.distance >= msg.distance) {
				e.ttl = msg.ttl;
				e.timestamp = msg.timestamp;
				e.distance = msg.distance;
				e.nextHop = msg.getSender();
			}
			return e;
		}
		
		/** Checks whether the given landmark should be chosen as new parent **/
		private boolean isAcceptableParent(LandmarkEntry entry) {
			return entry.distance <= getMaxDistance() // not too far
				&& entry.level == level + 1 // one level higher
				&& entry.freePlaces > 0 // has free places
				&& (parent < 0 // we have no parent, or new one is sufficiently better
					|| (entry.distance + HYSTERESIS < mgmtTable.get(parent).distance
							&& parentChangeAllowed()));
		}
		
		/** Returns all known landmarks of the given level within our max election distance **/
		private Stream<LandmarkEntry> electable(int mylevel, int level) {
			int dmax = (int) Math.pow(2, mylevel);
			return mgmtTable.values().stream()
				.filter(e -> e.distance <= dmax && e.level == level);
		}
		
		/** Returns the current number of children **/
		private int childCount() {
			return (int) IntStream.of(children).filter(i -> i >= 0).count();
		}

		/** Returns false if not enough time has passed since last parent change **/
		private boolean parentChangeAllowed() {
			return lastParentChange + ADOPTION_COOLDOWN < sim.getTime();
		}
		
		/** Returns the (initial) radius, i.e. the distance over which we broadcast our updates **/
		public int getInitialRadius() {
			return (int) Math.pow(2, level + 1);
		}
		
		/** Returns the max allowed distance of a parent **/
		public int getMaxDistance() {
			return (int) Math.pow(2, level);
		}
		
		/** Returns the min allowed distance of a parent **/
		public int getMinDistance() {
			return (int) Math.pow(2, level - 1);
		}
	}
	
	/** The view one landmark has of another **/
	public class LandmarkEntry {
		HashSet<Integer> senders;
		int ttl, id, distance, nextHop, level, freePlaces;
		float timestamp;
		String address;
		boolean satisfied;
		public LandmarkEntry() {
			senders = new HashSet<>();
		}
	}
	
	/** Realizes periodic management tasks for a landmark **/
	public class LandmarkManage extends RepeatedLocalEvent {
		@Override
		public void run() {
			landmarks.get(node).manage();
			completed();
		}
	}
	
	/** Self-announcement of a landmark within a radius. Used for peer and parent discovery. **/
	public class LandmarkUpdate extends Message {
		public int source, level, ttl, distance, freePlaces;
		public boolean satisfied;
		public float timestamp;
		public final String address;		
		public LandmarkUpdate(int source, final String address, int level, int ttl,
				boolean satisfied, int distance, int freePlaces, float timestamp) {
			this.source = source;
			this.address = address;
			this.level = level;
			this.ttl = ttl;
			this.satisfied = satisfied;
			this.distance = distance;
			this.sender = -1;
			this.freePlaces = freePlaces;
			this.timestamp = timestamp;
		}
		@Override
		public void run() {
			landmarks.get(node).receiveUpdate(this);
		}	
	}
	
	@Override
	public long estimateStorage() {
		return 0;
	}
	
	@Override
	public String toString() {
		return String.format("LandmarkHierarchyRouting(%f, %d)", interval);
	}
}
