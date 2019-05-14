package core.event;

import core.Simulation;

/**
 * A basic event in the simulation. Extend this directly for
 * global events, or events not directly pertaining to the graph.
 */
public abstract class Event implements Comparable<Event> {

	/** When the event should be triggered (in simulation time) **/
	protected float time;	
	protected Simulation sim;
	
	/** Method that is executed when the event is triggered **/
	public void prepareAndRun() {
		run();
	}
	
	/**
	 * Method that is executed when the event is triggered
	 * @return false if the event is not yet finished, e.g. waiting for async. responses
	 **/
	public abstract void run();

	
	// SCHEDULING
	
	/** Schedules the event absolutely **/
	public void at(float time, Simulation sim) {
		this.sim = sim;
		this.time = time;
		sim.addEvent(this);
	}
	
	/** Schedules the event to be run immediately **/
	public void now(Simulation sim) {
		at(sim.getTime(), sim);
	}
	
	/** Schedules the event relatively **/
	public void after(float time, Simulation sim) {
		at(sim.getTime() + time, sim);
	}

	
	
	/**
	 * Call this to indicate that the event was completed.
	 * This is only necessary if the run() function returned false.
	 **/
	protected void completed() {
		sim.eventCompleted(this);
	}
	
	public float getTime() {
		return time;
	}
	
	public String getName() {
		//return MethodHandles.lookup().lookupClass().getName();
		return getClass().getName();
	}
	
	@Override
	public int compareTo(Event e) {
		return (time == e.time) ? 0 : (time > e.time) ? 1 : -1;
	}
	
	/** Creates a simple event from a lamda **/
	public static Event make(Runnable handler) {
		return new Event() {
			@Override
			public void run() {
				handler.run();
			}
		};
	}
}
