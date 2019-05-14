package metric.payments;

import metric.SamplingMetric;
import payment.Payment;
import routing.algorithm.OptimalRouting;
import routing.algorithm.RoutingAlgorithm;

/** Compares fees to optimal (atomic) route fees **/
public class FeeQuality extends SamplingMetric {
	
	private RoutingAlgorithm optimalRouting;
	private long totalPayments, payments;
	private double totalQuality, quality;
	
	@Override
	public void beforeSimulation() {
		labels("Time (s)", "Average Fee Quality (%)");
		
		totalQuality = quality = totalPayments = payments = 0;
		optimalRouting = new OptimalRouting();
		optimalRouting.initComponent(sim, random.getLong());
		optimalRouting.prepare();
		
		sim.afterEvent(Payment.class, p -> {	
			if (p.hasSucceeded())  {
				payments++;
				Payment p2 = new Payment(p.getSource(), p.getTarget(), p.getAmount());
				p2.setDryRun(() -> quality += percentage(p2.getFee(), p.getFee()));
				optimalRouting.findPaths(p2);
			}
		});
		
	}
	
	@Override
	public void sample(float time) {
		write(time, (payments > 0) ? (quality / (double) payments) : 0);
		totalQuality += quality;
		totalPayments += payments;
		quality = payments = 0;
	}
	
	@Override
	protected void afterSimulation() {
		stat("Fee Quality (%)", (totalPayments > 0)
				?  String.valueOf(totalQuality / (float) totalPayments)
				: "No payments were routed");
	}
	
}
