package metric.graph;

import metric.SamplingMetric;
import plot.GraphPlot;

/** Periodically creates a graph plot. The plot name will be the output subfolder **/
public class PeriodicGraphPlotter extends SamplingMetric {

	/** Periodically creates a graph plot. The plot name will be the output subfolder **/
	public PeriodicGraphPlotter() {
		super();
		storeToFile = false;
	}
	
	@Override
	protected void sample(float time) {
		GraphPlot plot = new GraphPlot(name + "/" + time + "s-to");
		plot.initComponent(this);
		/*if (sim.routing() instanceof LandmarkCentricRouting) {
			plot.markTree(((LandmarkCentricRouting) sim.routing()).getLandmarks()[0].toRoot.getForest());
		}*/
		plot.run();
		
		/*GraphPlot plot2 = new GraphPlot(name + "/" + time + "s-from");
		plot2.initComponent(this);
		if (sim.routing() instanceof LandmarkCentricRouting) {
			plot2.markTree(((LandmarkCentricRouting) sim.routing()).getLandmarks()[0].fromRoot.getForest());
		}
		plot2.run();*/
	}
	
	

}
