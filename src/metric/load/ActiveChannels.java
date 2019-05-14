package metric.load;

import java.util.Arrays;

import metric.SamplingMetric;
import utility.lib.Lists;

/** Counts channels which have routed a payment in the interval time frame **/
public class ActiveChannels extends SamplingMetric {
	
	private boolean[] active;

	@Override 
	protected void beforeSimulation() {
		labels("Time (s)", "Active Channels (%)");	
		active = Lists.initBoolArray(graph().channels().size(), false);
		graph().onChannelUpdate(ch -> active[ch.getID()] = true);
	}

	@Override
	public void sample(float time) {		
		write(time, percentage(Lists.countTrue(active), active.length));
		Arrays.fill(active, false);
	}
	
}
