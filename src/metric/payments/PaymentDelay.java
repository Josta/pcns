package metric.payments;

import metric.SamplingMetric;
import payment.Payment;

/** Measures the average time it takes to successfully complete a payment **/
public class PaymentDelay extends SamplingMetric {
	
	double delay, totalDelay;
	int payments, totalPayments;
	
	@Override
	public void beforeSimulation() {
		labels("Time (s)", "Payment Delay (s)");
		totalDelay = delay = totalPayments = payments = 0;
		sim.afterEvent(Payment.class, p -> {
			if (p.hasSucceeded()) {
				delay += sim.getTime() - p.getTime();
				totalDelay += sim.getTime() - p.getTime();
				payments++;
				totalPayments++;
			}
		});
	}
	
	@Override
	public void sample(float time) {
		write(time, delay / payments);
		delay = payments = 0;
	}
	
	@Override
	public void afterSimulation() {
		stat("  Payment Delay (avg)", totalDelay / totalPayments + "s");
	}
	
}
