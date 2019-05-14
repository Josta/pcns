package metric.graph;

import graph.Channel;
import metric.SamplingMetric;

/** Measures the average channel imbalance. **/
public class Imbalance extends SamplingMetric {

	@Override
	protected void beforeSimulation() {
		labels("Time (s)", "Average Imbalance (%)");		
	}
	
	@Override
	protected void sample(float time) {	
		double imb = 0;
		for (Channel ch : graph().channels()) {
			imb += 2 * Math.abs(50.0 - percentage(ch.getCapacity1(), ch.getCapacity()));
		}
		write(time, imb / graph().channels().size());
	}
	
}
