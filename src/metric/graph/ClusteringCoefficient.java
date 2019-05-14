package metric.graph;

import graph.Node;
import metric.StatisticMetric;

/**
 * Calculates the average clustering coefficient of a graph.
 */
public class ClusteringCoefficient extends StatisticMetric {
	
	@Override
	public void beforeSimulation() {		
        double total = 0;
        for (Node node : graph().nodes()) {
            int possible = node.getDegree() * (node.getDegree() - 1);
            int actual = 0;
            for (int a : node.neighborsList()) {
                for (int b : node.neighborsList()) {
                    if (graph().channel(a,b) != null)
                        actual++;
                }
            }
            if (possible > 0)
                total += actual / (double) possible;
        }
        double coefficient = total / graph().size();     
		stat("Clustering Coefficient (average)", coefficient);
	}

}
