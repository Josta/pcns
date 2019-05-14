package metric.routing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import graph.Node;
import metric.StatisticMetric;
import routing.algorithm.LandmarkHierarchyRouting;
import utility.lib.Lists;

/**
 * Checks whether the landmark hierarchy is consistent.
 * - only one global landmark may exist
 */
public class ValidateLandmarkHierarchy extends StatisticMetric {
	
	@Override
	public void afterSimulation() {
		if (!(sim.routing() instanceof LandmarkHierarchyRouting)) {
			return;
		}
		LandmarkHierarchyRouting routing = (LandmarkHierarchyRouting) sim.routing();
		
		HashSet<Integer> roots = new HashSet<>();
		LinkedList<Integer> path = new LinkedList<>();
		for (Node subject : sim.graph().nodes()) {
			path.clear();
			int node = subject.getID();
			while (true) {
				path.add(node);
				int parent = routing.getLandmark(node).parent;
				if (parent < 0) {
					break;
				} else {
					if (path.contains(parent)) {
						stat("Landmark Hierarchy Error", "Cycle detected: " + path);
						break;
					}
					node = parent;
				}
			}
			roots.add(node);
		}
		stat("Landmark Hierarchy Roots", roots.toString());
		
		HashMap<Integer, Integer> landmarksPerLevel = new HashMap<>();
		for (Node subject : sim.graph().nodes()) {
			int level = routing.getLandmark(subject.getID()).level;
			landmarksPerLevel.put(level, landmarksPerLevel.getOrDefault(level, 0) + 1);
		}
		stat("Landmarks per level", Lists.strArray(landmarksPerLevel.keySet().stream()
			.sorted().map(lvl -> lvl + "=>" + landmarksPerLevel.get(lvl))));
	}
	
}
