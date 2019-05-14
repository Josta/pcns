package metric.load;

import java.util.Arrays;
import java.util.List;

import graph.Node;
import metric.SamplingMetric;
import payment.Payment;
import utility.lib.Lists;

/** Counts consumers which send or receive successful payments **/
public class ActiveConsumers extends SamplingMetric {
	
	private List<Node> consumers;
	private boolean[] active;

	@Override 
	protected void beforeSimulation() {
		labels("Time (s)", "Active Consumers (%)");
		consumers = graph().nodesWithRole(Node.ROLE_CONSUMER);
		active = Lists.initBoolArray(graph().size(), false);		
		sim.afterEvent(Payment.class, p -> {
			if (p.hasSucceeded()) {
				setActive(p.getSource());
				setActive(p.getTarget());
			}
		});
	}
	
	private void setActive(int node) {
		active[node] = true;
	}

	@Override
	public void sample(float time) {
		write(time, percentage(Lists.countTrue(active), consumers.size()));
		Arrays.fill(active, false);
	}
	
}
