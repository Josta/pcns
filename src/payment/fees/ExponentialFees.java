package payment.fees;

import graph.Channel;

/** Maps channel depletion (0-100%) exponentially to fee rate **/
public class ExponentialFees extends FeePolicy {
	
	private double exponent;
	private int base, minRate, maxRate;
	
	/**
	 * Maps channel depletion (0-100%) exponentially to fee rate
	 * @param base fee base (msat)
	 * @param minRate minimum fee rate (milli msat)
	 * @param maxRate maximum fee rate (milli msat)
	 * @param exponent 0=constant, 1=linear, >1=superlinear
	 */
	public ExponentialFees(int base, int minRate, int maxRate, double exponent) {
		this.base = base;
		this.minRate = minRate;
		this.maxRate = maxRate;
		this.exponent = exponent;
	}

	@Override
	public boolean updateChannelFee(Channel ch) {
		int rate1 = (int) (minRate + (maxRate - minRate)
				* Math.pow(ch.getCapacity2() / (double) ch.getCapacity(), exponent)),
			rate2 = (int) (minRate + (maxRate - minRate)
				* Math.pow(ch.getCapacity1() / (double) ch.getCapacity(), exponent));
		return ch.setFees(base, base, rate1, rate2);
	}

	@Override
	public String toString() {
		return String.format("ExponentialFees(%d, %d, %d)", base, minRate, maxRate);
	}
	
}
