package payment.fees;

import graph.Channel;

/** Maps channel depletion (0-100%) linearly to the fee rate (min to max) **/
public class LinearFees extends FeePolicy {
	
	private int base, minRate, maxRate;
	
	/** Maps channel depletion (0-100%) linearly to the fee rate (min to max) **/
	public LinearFees(int base, int minRate, int maxRate) {
		this.base = base;
		this.minRate = minRate;
		this.maxRate = maxRate;
	}

	@Override
	public boolean updateChannelFee(Channel ch) {
		int rate1 = minRate + (int) ((maxRate - minRate)
				* ch.getCapacity2() / (double) ch.getCapacity()),
			rate2 = minRate + (int) ((maxRate - minRate)
				* ch.getCapacity1() / (double) ch.getCapacity());
		return ch.setFees(base, base, rate1, rate2);
	}

	@Override
	public String toString() {
		return String.format("LinearFees(%d, %d, %d)", base, minRate, maxRate);
	}
	
}
