package metric.routing;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import core.event.Event;
import metric.StatisticMetric;
import routing.algorithm.LandmarkCentricRouting;

/**
 * Finds the average fee to the tree root, which is
 * a measurement for the tree's quality.
 * Currently works with LandmarkCentricRouting only.
 */
public class ValidateLandmarks extends StatisticMetric {
	
	LandmarkCentricRouting.BiLandmark[] lms;
	
	@Override 
	protected void beforeSimulation() {
		lms = null;
		if (sim.routing() instanceof LandmarkCentricRouting) {
			lms = ((LandmarkCentricRouting) sim.routing()).getLandmarks();
		}
		Event.make(() -> validate()).at(60, sim);
	}

	private void validate() {
		if (lms == null) return;
		Arrays.stream(lms).forEach(lm -> {
			Set<Integer> roots = new HashSet<>();
			graph().nodes().forEach(n -> {
				int[] path1 = lm.toRoot.getPathFrom(n.getID(), true);
				int[] path2 = lm.fromRoot.getPathTo(n.getID(), true);
				roots.add(path1[path1.length - 1]);
				roots.add(path2[0]);
			});
			if (roots.size() > 1) {
				System.out.println("Landmark invalid: roots=" + roots);
			}
		});
	}
	
}
