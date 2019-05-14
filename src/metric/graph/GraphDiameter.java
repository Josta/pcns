package metric.graph;

import java.util.Arrays;

import graph.Channel;
import graph.Node;
import metric.StatisticMetric;
import utility.lib.Lists;

/**
 * Calculates the diameter of a graph (in hops), i.e. the longest "shortest path".
 * Complexity: O(V�)
 */
public class GraphDiameter extends StatisticMetric {
	
	@Override
	public void beforeSimulation() {
		// see https://en.wikipedia.org/wiki/Floyd%E2%80%93Warshall_algorithm
		
		int size = graph().size();
		int infinity = size + 1;
		
		// init distance array with infinity
		int dist[][] = new int[size][size];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				dist[i][j] = infinity;
			}
		}
		
		// add known distances: neighbors (1) and self (0)
		for (Channel c : graph().channels()) {
			dist[c.getNode1()][c.getNode2()] = 1;
			dist[c.getNode2()][c.getNode1()] = 1;
		}
		for (Node n : graph().nodes()) {
			dist[n.getID()][n.getID()] = 0;
		}
		
		// Floyd-Warshall relaxation
		for (int k = 0; k < size; k++) {
			for (int i = 0; i < size; i++) {
				for (int j = 0; j < size; j++) {
					int value = dist[i][k] + dist[k][j];
					if (value < infinity && dist[i][j] > value) {
						dist[i][j] = value;
					}
				}
			}
		}
		
		// find maximum distance (bonus: find sum for average path length)	
		int[] eccentricity = Lists.initIntArray(size, 0);
		double[] closeness = Lists.initDoubleArray(size, 0);
		double[] harmonicCloseness = Lists.initDoubleArray(size, 0);
		
		long pathLengthSum = 0;
		int pathCount = 0;
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				if (dist[i][j] > 0 && dist[i][j] < infinity) {
					pathLengthSum += dist[i][j];
					pathCount++;
					if (dist[i][j] > eccentricity[i])
						eccentricity[i] = dist[i][j];
					closeness[i] += dist[i][j];
					harmonicCloseness[i] += 1.0 / dist[i][j];
				}
			}
			closeness[i] = (closeness[i] > 0) ? (size - 1) / closeness[i] : 0;		
		}
		
		double diameter = Arrays.stream(eccentricity).max().orElse(-1);
		double radius = Arrays.stream(eccentricity).min().orElse(-1);
		double avgEcc = Arrays.stream(eccentricity).average().orElse(-1);
		double avgCloseness = Arrays.stream(closeness).average().orElse(-1);
		double avgHarmonicCloseness = Arrays.stream(harmonicCloseness).average().orElse(-1);
		double maxHarmClosness = Arrays.stream(harmonicCloseness).max().orElse(1);
		double avgHarmonicClosenessNorm = Arrays.stream(harmonicCloseness).map(c->c / maxHarmClosness).average().orElse(-1);
		double avgDistance = pathLengthSum / (double) pathCount;
		double fragmentation = 1.0 - (pathCount / ((double) size * (size - 1)));
		stat("Network Distance metrics", "");
		stat("  Diameter", diameter);
		stat("  Radius", radius);
		stat("  Average Eccentricity", avgEcc);
		stat("  Average Closeness", avgCloseness);
		stat("  Average Harmonic Closeness", avgHarmonicCloseness);
		stat("  Average Harmonic Closeness (normalized)", avgHarmonicClosenessNorm);
		stat("  Average Shortest Path Length", avgDistance);
		stat("  Fragmentation", fragmentation);
	}

}
