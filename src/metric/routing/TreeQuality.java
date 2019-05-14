package metric.routing;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import graph.Node;
import metric.SamplingMetric;
import payment.Route;
import routing.algorithm.LandmarkCentricRouting;
import routing.algorithm.LandmarkUniverseRouting;
import routing.util.SpanningForest;
import utility.lib.Lists;

/**
 * Finds the average fee to the tree root, which is
 * a measurement for the tree's quality.
 * Currently works with LandmarkCentricRouting only.
 */
public class TreeQuality extends SamplingMetric {
	
	LandmarkCentricRouting.BiLandmark[] lms;
	LandmarkUniverseRouting.LandmarkUniverse[] unis;
	
	@Override 
	protected void beforeSimulation() {
		lms = null;
		if (sim.routing() instanceof LandmarkCentricRouting) {
			lms = ((LandmarkCentricRouting) sim.routing()).getLandmarks();
			labels("Time (s)", Lists.strArray(IntStream.range(0, lms.length)
				.mapToObj(i -> "LM" + (i+1) + " Quality")));
		}
		if (sim.routing() instanceof LandmarkUniverseRouting) {
			unis = ((LandmarkUniverseRouting) sim.routing()).getUniverses();
			labels("Time (s)", Lists.strArray(IntStream.range(0, unis.length)
					.mapToObj(i -> "U" + (i+1) + " Quality")));
		}
	}

	@Override
	public void sample(float time) {
		Stream<Double> values = null;
		if (lms != null) {
			values = Arrays.stream(lms).map(lm -> getTreeFees(lm.toRoot, lm.fromRoot));
		} else if (unis != null) {
			values = Arrays.stream(unis).map(u -> IntStream.range(0, u.fromRoots.length)
				.mapToDouble(i -> getTreeFees(u.toRoots[i], u.fromRoots[i])).sum());
		}
		if (values != null)
			write(time, Lists.strArray(values.map(v -> String.valueOf(v))));
	}

	
	/** Get average fee for all paths to/from root **/
	private double getTreeFees(SpanningForest toRoot, SpanningForest fromRoot) {
		long toFees = 0, fromFees = 0;
		for (Node node : graph().nodes()) {
			toFees += new Route(graph(), null,
				toRoot.getPathFrom(node.getID(), true), 1000).getFee();
			fromFees += new Route(graph(), null,
				fromRoot.getPathTo(node.getID(), true), 1000).getFee();
		}
		return (toFees + fromFees) / (2d * graph().size());
	}
}
