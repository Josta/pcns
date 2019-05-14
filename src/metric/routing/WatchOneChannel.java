package metric.routing;

import graph.Channel;
import metric.SamplingMetric;
import payment.Payment;

import static utility.lib.Unit.*;

import java.util.ArrayList;
import java.util.Arrays;

/** Watches one particular channel for debugging **/
public class WatchOneChannel extends SamplingMetric {

	private int id, payments;
	private ArrayList<String> routes;
	
	public WatchOneChannel(int id) {
		this.id = id;
		this.routes = new ArrayList<>();
	}
	
	@Override 
	protected void beforeSimulation() {
		labels("Time (s)", "Disbalance (%)", "Fee1 (msat)", "Fee2 (msat)", "Payments (#)");
		/*graph.addChannelUpdateListener((chid) -> {
			if (chid == id) {
				payments++;
			}
		});*/
		sim.afterEvent(Payment.class, p -> {
			if (p.hasSucceeded()) {
				for (int node : p.getRoutes().get(0).getNodes()) {
					if (node == id) {
						payments++;
						routes.add(Arrays.toString(p.getRoutes().get(0).getNodes()));
					}
				}
			}
		});
	}

	@Override
	public void sample(float time) {
		Channel ch = graph().channel(id);

		write(time,
			2 * (50.0 - percentage(ch.getCapacity1(), ch.getCapacity())),
			ch.getFee(ch.getNode1(), btc(0.001)),
			ch.getFee(ch.getNode2(), btc(0.001)),
			payments);
		
		payments = 0;
	}
	
	@Override
	public void afterSimulation() {
		stat
		("WOC Routes", String.join(", ", (String[]) routes.toArray()));
	}
	
}
