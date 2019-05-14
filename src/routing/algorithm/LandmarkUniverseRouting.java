package routing.algorithm;

import java.util.List;
import java.util.stream.IntStream;

import core.event.Event;
import graph.Node;
import payment.Payment;
import routing.util.SpanningForest;
import routing.util.SpanningForestGenerator;
import utility.lib.Lists;

/**
 * Creates a hierarchy of landmark forests, with 2^i trees on each level.
 * Tries to find paths on all levels, starting with the highest one.
 * @author Josua
 */
public class LandmarkUniverseRouting extends RoutingAlgorithm {

	private int levelCount, universeCount;
	private float interval;
	private LandmarkUniverse[] universes;
	private List<Node> bridges;
	
	/**
	 * Creates a hierarchy of landmark forests, with 2^i trees on each level.
	 * Tries to find paths on all levels, starting with the highest one.
	 */
	public LandmarkUniverseRouting(float updateInterval, int universeCount, int levelCount) {
		this.universeCount = universeCount;
		this.levelCount = levelCount;
		this.interval = updateInterval;
	}
	
	@Override
	public void prepare() {
		universes = new LandmarkUniverse[universeCount];
		bridges = graph().nodesWithRole(Node.ROLE_BRIDGE);
		if (bridges.size() < Math.pow(2, levelCount)) {
			throw new IllegalStateException("Not enough bridge nodes present");
		}
		IntStream.range(0, universes.length)
			.forEach(i -> universes[i] = new LandmarkUniverse(() -> {}));
		new RenewUniverse(0).after(interval, sim);
	}
	
	@Override
	public void findPaths(Payment p) {
		for (LandmarkUniverse u : universes) {
			for (int lvl = 0; lvl < u.toRoots.length; lvl++) {
				if (u.toRoots[lvl] == null || u.fromRoots[lvl] == null) continue;
				int[] path1 = u.toRoots[lvl].getPathFrom(p.getSource(), true),
					  path2 = u.fromRoots[lvl].getPathTo(p.getTarget(), true);
				if (path1[path1.length - 1] != path2[0]) continue;
				p.addPath(Lists.shortenPath(Lists.concat(path1, path2)));
			}
		}
		p.selectRoutes();
	}
	
	/** A landmark universe **/
	public class LandmarkUniverse {
		public SpanningForest[] toRoots, fromRoots;
		private int createdTrees;
		private Runnable doneHandler;
		private LandmarkUniverse(Runnable doneHandler) {
			toRoots = new SpanningForest[levelCount];
			fromRoots = new SpanningForest[levelCount];
			this.doneHandler = doneHandler;
			createdTrees = 0;
			// add forests of 2^i landmarks per level	
			int lmsAtLevel = 1;
			for (int i = 0; i < levelCount; i++) {
				int lvl = i;
				int[] roots = random.getDistinct(bridges, lmsAtLevel)
					.stream().mapToInt(b -> b.getID()).toArray();
				toRoots[lvl] = fromRoots[lvl] = null;
				new SpanningForestGenerator(sim, sim.costs(), roots, t -> {
					toRoots[lvl] = t;
					treeCreated();
				});
				new SpanningForestGenerator(sim, sim.inverseCosts(), roots, t -> {
					fromRoots[lvl] = t;
					treeCreated();
				});
				lmsAtLevel *= 2;
			}
		}
		private void treeCreated() {
			createdTrees++;
			if (createdTrees == levelCount * 2) {
				doneHandler.run();
			}
		}
	}
	
	/** Periodically exchanges one universe with a new one **/
	public class RenewUniverse extends Event {
		private int index;
		public RenewUniverse(int index) {
			this.index = index;
		}
		@Override
		public void run() {
			universes[index] = new LandmarkUniverse(() -> completed());
			new RenewUniverse((index + 1) % universes.length).after(interval, sim);
			completed();
		}		
	}

	@Override
	public long estimateStorage() {
		return universes.length * levelCount * 2 * graph().size() * 32;
	}

	@Override
	public String toString() {
		return String.format("LandmarkUniverseRouting(%f, %d, %d)", interval, universeCount, levelCount);
	}
	
	public LandmarkUniverse[] getUniverses() {
		return universes;
	}
}
