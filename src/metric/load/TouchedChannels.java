package metric.load;

import metric.SamplingMetric;
import utility.lib.Lists;

/** Counts channels which have been touched by at least one payment during the simulation **/
public class TouchedChannels extends SamplingMetric {
	
	private boolean[] touched;

	@Override 
	protected void beforeSimulation() {		
		labels("Time (s)", "Touched Channels (%)");
		touched = Lists.initBoolArray(graph().channels().size(), false);
		graph().onChannelUpdate(ch -> touched[ch.getID()] = true);
	}

	@Override
	public void sample(float time) {
		write(time, percentage(Lists.countTrue(touched), touched.length));
	}
	
}
