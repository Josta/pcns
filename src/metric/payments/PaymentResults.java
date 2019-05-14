package metric.payments;

import java.util.Arrays;
import java.util.List;

import metric.SamplingMetric;
import payment.Payment;
import payment.Payment.Result;
import utility.lib.Lists;

/** Counts payments by result (success, no path, fees too high etc.) **/
public class PaymentResults extends SamplingMetric {
	
	private final List<Result> restypes;
	private long[] totalResults, results;
	private long totalPayments, payments;
	
	public PaymentResults() {
		super();
		restypes = Arrays.asList(Result.values());
	}
	
	@Override
	public void beforeSimulation() {	
		labels("Time (s)", Lists.strArray(restypes.stream().map(r -> r.toString() + " (%)")));	
		totalPayments = payments = 0;
		totalResults = Lists.initLongArray(Result.values().length, 0);
		results = Lists.initLongArray(Result.values().length, 0);
		sim.afterEvent(Payment.class, p -> {
			totalPayments++;
			payments++;
			totalResults[p.getResult().getCode()]++;
			results[p.getResult().getCode()]++;
		});
	}
	
	@Override
	public void sample(float time) {
		write(time, Lists.strArray(restypes.stream().map(r -> "" + ((payments > 0)
			? percentage(results[r.getCode()], payments) : 0))));
		payments = 0;
		for (int i = 0; i < results.length; i++) {
			results[i] = 0;
		}
	}
	
	@Override
	protected void afterSimulation() {
		stat("Payments", "" + totalPayments);
		stat("  Payment Results", Lists.strArray(
			restypes.stream().map(r -> r.toString() + ": " + totalResults[r.getCode()])));
	}
	
	public double getSuccessRate() {
		return totalResults[1] / (double) totalPayments;
	}
	
}
