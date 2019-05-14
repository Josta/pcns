package routing.algorithm;

import java.util.List;
import java.util.stream.IntStream;

import core.event.Event;
import core.event.Message;
import graph.Node;
import payment.Payment;
import routing.util.SpanningTreeGenerator;
import routing.util.SpanningForest;
import utility.lib.Lists;

/** Uses a set of global bidirectional landmarks for finding paths **/
public class LandmarkCentricRouting extends RoutingAlgorithm {

	private float interval;
	private int landmarkCount;
	private BiLandmark[] landmarks;
	private List<Node> bridges;
	
	/** Uses a set of global bidirectional landmarks for finding paths **/
	public LandmarkCentricRouting(float updateInterval, int landmarkCount) {
		this.interval = updateInterval;
		this.landmarkCount = landmarkCount;
	}	
	
	@Override
	public void prepare() {
		landmarks = new BiLandmark[landmarkCount];
		bridges = graph().nodesWithRole(Node.ROLE_BRIDGE);
		if (bridges.isEmpty())
			throw new IllegalStateException("No bridge node present");
		IntStream.range(0, landmarks.length)
			.forEach(i -> landmarks[i] = new BiLandmark(() -> {}));
		new RenewLandmark(0).after(interval, sim);
	}
	
	@Override
	public void findPaths(Payment p) {
		for (BiLandmark lm : landmarks) {
			if (lm.toRoot == null || lm.fromRoot == null) continue;
			int[] path1 = lm.toRoot.getPathFrom(p.getSource(), true),
				  path2 = lm.fromRoot.getPathTo(p.getTarget(), false);
			if (path1 == null || path2 == null) continue;
			p.addPath(Lists.shortenPath(Lists.concat(path1, path2)));
		}
		
		/*for (BiLandmark lm : landmarks) {
			if (lm.toRoot == null || lm.fromRoot == null) continue;
			List<Integer> path1 = new LinkedList<Integer>(),
					      path2 = new LinkedList<Integer>();
			new Probe(lm.toRoot, path1, () -> {})
				.send(p.getSource(), lm.toRoot.getNextHop(p.getSource()), sim);
			new Probe(lm.fromRoot, path2, () -> {})
				.send(p.getTarget(), lm.fromRoot.getNextHop(p.getTarget()), sim);
		}*/
		
		p.selectRoutes();
	}

	/** A bidirectional landmark **/
	public class BiLandmark {
		private final int root;
		public SpanningForest toRoot, fromRoot;
		private BiLandmark(Runnable doneHandler) {
			root = random.getOne(bridges).getID();
			toRoot = fromRoot = null;
			new SpanningTreeGenerator(sim, sim.inverseCosts(), root, t -> {
				toRoot = t;
				if (fromRoot != null)
					doneHandler.run();
			});
			new SpanningTreeGenerator(sim, sim.costs(), root, t -> {
				fromRoot = t;
				if (toRoot != null)
					doneHandler.run();
			});
		}
	}
	
	/** Periodically exchanges one landmark with a new one **/
	public class RenewLandmark extends Event {
		private int index;
		public RenewLandmark(int index) {
			this.index = index;
		}
		@Override
		public void run() {		
			landmarks[index] = new BiLandmark(() -> completed());
			new RenewLandmark((index + 1) % landmarks.length).after(interval, sim);
		}
	}
	
	public class Probe extends Message {
		private SpanningForest tree;
		private List<Integer> path;
		private Runnable handler;
		public Probe(SpanningForest tree, List<Integer> path, Runnable handler) {
			this.handler = handler;
		}
		@Override
		public void run() {		
			int nextHop = tree.getNextHop(node);
			path.add(node);
			if (nextHop >= 0) {
				new Probe(tree, path, handler).send(node, nextHop, sim);
			} else {
				handler.run();
			}
		}
	}

	@Override
	public long estimateStorage() {
		// two trees per landmark, each node stores: ID of parent (int) + distance to root (long)
		return landmarks.length * 2 * graph().size() * 32;	
	}

	@Override
	public String toString() {
		return String.format("LandmarkCentricRouting(%f, %d)", interval, landmarkCount);
	}
	
	public BiLandmark[] getLandmarks() {
		return landmarks;
	}
	
}
