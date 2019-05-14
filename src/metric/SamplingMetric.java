package metric;

import core.event.Event;
import utility.global.Config;

/** A sampling metric is a metric which is run regularly in simulation time **/
public abstract class SamplingMetric extends Metric {

	protected float interval;
	
	/** A sampling metric is a metric which is run regularly in simulation time **/
	public SamplingMetric() {
		super();
		this.interval = Config.getFloat("DEFAULT_SAMPLING_INTERVAL");
	}
	
	/** Sets the sampling interval of a sampling metric **/
	public SamplingMetric interval(double interval) {
		this.interval = (float) interval;
		return this;
	}
	
	@Override
	public void prepare() {
		super.prepare();
		new Sampling().after(interval, sim);
	}
	
	/**
	 * Executes one measurement step at a specific time.
	 * @param time time of the measurement (simulation time)
	 */
	protected abstract void sample(float time);
	
	
	private class Sampling extends Event {
		@Override
		public void run() {
			sample(time);
			new Sampling().after(interval, sim);
		}
	}
	
}
