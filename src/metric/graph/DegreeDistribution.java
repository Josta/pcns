package metric.graph;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.counting;

import graph.Node;
import metric.Metric;

/** Creates a distribution of node degrees **/
public class DegreeDistribution extends Metric {

	private final int steps;
	
	/** Creates a distribution of channel capacities **/
	public DegreeDistribution(int steps) {
		super();
		this.steps = steps;
	}
	
	public DegreeDistribution() {
		super();
		this.steps = -1;
	}
	
	@Override
	public void beforeSimulation() {
		labels("Degree", "Nodes (%)");
		List<Node> nodes = graph().nodes();
		int maxDegree = nodes.stream().mapToInt(c -> c.getDegree()).max().getAsInt();
		if (steps < 0) {
			Map<Integer,Long> map = nodes.stream()
				.collect(groupingBy(c -> c.getDegree(), counting()));	
			for (int i = 0; i <= maxDegree; i++) {
				int count = map.containsKey(i) ? map.get(i).intValue() : 0;
				write(i, percentage(count, nodes.size()));
			}
		} else {
			float stepWidth =  maxDegree / (float) steps;	
			Map<Integer,Long> map = nodes.stream().collect(
				groupingBy(ch -> ch.getDegree() / (int) stepWidth, counting()));		
			for (int i = 0; i <= steps; i++) {
				int count = map.containsKey(i) ? map.get(i).intValue() : 0;
				write((i + 0.5) * stepWidth, percentage(count, nodes.size()));
			}
		}
		
	}

}
