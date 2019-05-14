package metric.load;

import graph.Channel;
import metric.SamplingMetric;
import utility.lib.Lists;

/** Counts channels which were depleted at least once in the time frame **/
public class DepletedChannels extends SamplingMetric {

	private final int minAmount;
	private boolean[] depleted;
	
	/**
	 * Counts channels which were depleted at least once in the time frame
	 * @param minAmount used to determine depletion
	 */
	public DepletedChannels(int minAmount) {
		super();
		this.minAmount = minAmount;
	}
	
	@Override 
	protected void beforeSimulation() {
		labels("Time (s)", "Depleted Channels (%)");
		depleted = Lists.initBoolArray(graph().channels().size());	
		// remember depleted channels
		graph().onChannelUpdate(ch -> {
			if (isChannelDepleted(ch))
				depleted[ch.getID()] = true;
		});
	}

	@Override
	public void sample(float time) {
		write(time, percentage(Lists.countTrue(depleted), depleted.length));
		
		// get current depleted state for all channels
		for (int i = 0; i < depleted.length; i++) 
			depleted[i] = isChannelDepleted(graph().channel(i));
	}
	
	private boolean isChannelDepleted(Channel ch) {
		return !ch.canPay(ch.getNode1(), minAmount) || !ch.canPay(ch.getNode2(), minAmount);
	}
	
}
