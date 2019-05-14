package metric;

import metric.StatisticMetric;

/** Measures some simple attributes of the graph, like size, channels, average degree **/
public class SimulationProperties extends StatisticMetric {

	@Override
	protected void beforeSimulation() {
		stat("Simulation", sim.printProperties());
	}

}
