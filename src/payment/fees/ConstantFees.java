package payment.fees;

import graph.Channel;

public class ConstantFees extends FeePolicy {
	
	private int base, rate;
	
	public ConstantFees(int base, int rate) {
		this.base = base;
		this.rate = rate;
	}
	
	@Override
	public void prepare() {
		for (Channel ch : graph().channels())
			ch.setFees(base, base, rate, rate);
	}

	@Override
	public boolean updateChannelFee(Channel ch) {
		return false;
	}

	@Override
	public String toString() {
		return String.format("ConstantFees(%d, %d)", base, rate);
	}
}
