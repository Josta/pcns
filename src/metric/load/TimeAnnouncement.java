package metric.load;

import metric.SamplingMetric;

/** Gives a regular time output on the console **/
public class TimeAnnouncement extends SamplingMetric {

	/** Gives a regular time output on the console **/
	public TimeAnnouncement() {
		super();
		storeToFile = false;
	}
	
	@Override
	protected void sample(float time) {
		System.out.println("[t=" + time + "s]");
	}

}
