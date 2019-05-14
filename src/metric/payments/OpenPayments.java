package metric.payments;

import metric.SamplingMetric;
import payment.Payment;

/** Periodically measures the number of started but unfinished payments **/
public class OpenPayments extends SamplingMetric {
	
	private int openPayments;

	@Override
	public void beforeSimulation() {	
		labels("Time (s)", "Open Payments (#)");
		openPayments = 0;
		sim.beforeEvent(Payment.class, p -> openPayments++);
		sim.afterEvent(Payment.class, p -> openPayments--);
	}
	
	@Override
	public void sample(float time) {
		write(time, openPayments);
	}
	
}
