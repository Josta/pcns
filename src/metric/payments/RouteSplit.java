package metric.payments;

import metric.SamplingMetric;
import payment.Payment;

/** Measures the average split count for payments **/
public class RouteSplit extends SamplingMetric {
	
	private long totalPayments, totalRoutes, payments, routes;

	@Override
	public void beforeSimulation() {
		labels("Time (s)", "Average Routes per Payment");
		totalPayments = totalRoutes = payments = routes = 0;
		sim.afterEvent(Payment.class, p -> {
			if (p.hasSucceeded()) {
				totalPayments++;
				payments++;
				totalRoutes += p.getRoutes().size();
				routes += p.getRoutes().size();
			}
		});
	}
	
	@Override
	public void sample(float time) {
		write(time, (payments > 0) ? (routes / (float) payments) : 0);
		payments = routes = 0;	
	}
	
	@Override
	protected void afterSimulation() {
		stat("Route Split (avg)", (totalPayments > 0)
			? "" + (totalRoutes / (float) totalPayments)
			: "No payments were routed successfully");
	}
	
}
