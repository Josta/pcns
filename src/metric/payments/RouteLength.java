package metric.payments;

import metric.SamplingMetric;
import payment.Payment;
import payment.Route;

public class RouteLength extends SamplingMetric {
	
	private long totalRoutes, totalLength, routes, length;
		
	@Override
	public void beforeSimulation() {
		labels("Time (s)", "Average Route Length (nodes)");
		totalRoutes = totalLength = routes = length = 0;
		sim.afterEvent(Payment.class, p -> {
			if (p.hasSucceeded()) {
				for (Route route : p.getRoutes()) {
					totalRoutes++;
					routes++;	
					totalLength += route.size();
					length += route.size();
				}
			}
		});
	}
	
	@Override
	public void sample(float time) {
		write(time, (routes > 0) ? (length / (float) routes) : 0);
		routes = length = 0;	
	}
	
	@Override
	protected void afterSimulation() {
		stat("Route Length (avg)", (totalRoutes > 0)
			? "" + (totalLength / (float) totalRoutes)
			: "No payments were routed");
	}
	
}
