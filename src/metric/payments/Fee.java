package metric.payments;

import metric.SamplingMetric;
import payment.Payment;
import payment.Route;

/** Measures the average fee per time frame **/
public class Fee extends SamplingMetric {
	
	private long totalRoutes, totalFees, routes, fees;
	private double totalFeePercents, feePercents;

	@Override
	public void beforeSimulation() {
		labels("Time (s)", "Average Fee (sat)", "Average Fee (%)");
		totalFeePercents = feePercents = totalRoutes = totalFees = routes = fees = 0;
		sim.afterEvent(Payment.class, p -> {
			if (p.hasSucceeded()) {
				for (Route route : p.getRoutes()) {
					int fee = route.getFee();
					routes++;
					totalRoutes++;
					fees += fee;
					totalFees += fee;
					feePercents += percentage(fee, route.getNetAmount());
					totalFeePercents += percentage(fee, route.getNetAmount());
				}
			}
		});
	}
	
	@Override
	public void sample(float time) {
		if (routes > 0) {
			write(time, fees / (float) routes, feePercents / routes);
		} else write(time, 0, 0);
		feePercents = routes = fees = 0;	
	}
	
	@Override
	protected void afterSimulation() {
		stat("Route Fee (avg)", (totalRoutes > 0)
			? "" + totalFees / totalRoutes + "sat (" + totalFeePercents / totalRoutes + "%)"
			: "No payments were routed");
	}
	
}
