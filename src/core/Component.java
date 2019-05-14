package core;

import graph.Graph;
import utility.Random;

/**
 * A module/component which can be attached to the Simulation object.
 * Components can access each other via the Simulation object.
 * Components have their own RNG to ensure reproducible behaviour.
 * @author Josua
 */
public abstract class Component {

	protected Simulation sim;
	protected Random random;
	
	/**
	 * Called by the simulation object when the component is registered.
	 * @param sim
	 */
	public void initComponent(Simulation sim, long seed) {
		this.sim = sim;
		this.random = new Random(seed);
	}
	
	public void initComponent(Component parent) {
		initComponent(parent.sim, parent.random.getLong());
	}
	
	/**
	 * The graph of this simulation.
	 * @return
	 */
	public Graph graph() {
		return sim.graph();
	}

}
