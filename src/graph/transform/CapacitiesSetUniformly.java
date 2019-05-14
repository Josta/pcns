package graph.transform;

import graph.Graph;

/**
 * Sets all channel capacities (in either direction) to the given value;
 */
public class CapacitiesSetUniformly extends Transformation {

	private int capacity;
	
	/**
	 * Sets all channel capacities (in either direction) to the given value;
	 */
	public CapacitiesSetUniformly(int capacity) {
		this.capacity = capacity;
	}
	
	@Override
	public void transform(Graph graph) {
		graph.channels().stream().forEach(ch -> ch.setCapacities(capacity / 2, capacity / 2));
	}
	
	@Override
	public String toString() {
		return String.format("CapacitiesSetUniformly(%d)", capacity);
	}

}
