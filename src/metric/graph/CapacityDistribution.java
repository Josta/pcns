package metric.graph;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;
import static utility.lib.Unit.*;

import graph.Channel;
import metric.Metric;

/** Creates a distribution of channel capacities **/
public class CapacityDistribution extends Metric {

	private final int steps;
	
	/** Creates a distribution of channel capacities **/
	public CapacityDistribution(int steps) {
		super();
		this.steps = steps;
	}
	
	@Override
	public void beforeSimulation() {
		labels("Capacity (BIT)", "Channels (%)");
		List<Channel> channels = graph().channels();
		
		int maxCapacity = channels.stream().mapToInt(ch -> ch.getCapacity()).max().orElse(100*BIT);
		float stepWidth =  maxCapacity / (float) steps;
		
		Map<Integer,Long> map = channels.stream().collect(
			groupingBy(ch -> ch.getCapacity() / (int) stepWidth, counting()));	
		
		for (int i = 0; i <= steps; i++) {
			int count = map.containsKey(i) ? map.get(i).intValue() : 0;
			write(toBIT((int) ((i + 0.5) * stepWidth)), percentage(count, channels.size()));
		}
	}

}
