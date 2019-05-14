package graph.transform;

import graph.Channel;
import graph.Graph;

/**
 * Sets all channel capacities (in either direction) to a value
 * proportional to the smaller of the two node degrees.
 * Complexity: O(E)
 */
public class CapacitiesSetByDegree extends Transformation {

	private int minCapacity, capacityPerDegree;
	
	/**
	 * Sets all channel capacities (in either direction) to a value
	 * proportional to the smaller of the two node degrees.
	 */
	public CapacitiesSetByDegree(int minCapacity, int capacityPerDegree) {
		this.minCapacity = minCapacity;
		this.capacityPerDegree = capacityPerDegree;
	}
	
	@Override
	public void transform(Graph graph) {
		for (Channel channel: graph.channels()) {
			int capacity = Math.max(minCapacity, capacityPerDegree * Math.min(
					graph.node(channel.getNode1()).getDegree(),
					graph.node(channel.getNode2()).getDegree()));
			channel.setCapacities(capacity / 2, capacity / 2);
		}
	}

	@Override
	public String toString() {
		return String.format("CapacitiesByDegree(%d, %d)", minCapacity, capacityPerDegree);
	}
}
