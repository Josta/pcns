package payment.traffic;

public class DebugTraffic extends Traffic {
	
	private int srcNode, dstNode;
	private int amount;
	private float time;
	
	public DebugTraffic(int srcNode, int dstNode, int amount, float time) {
		this.srcNode = srcNode;
		this.dstNode = dstNode;
		this.amount = amount;
		this.time = time;
	}
	
	@Override
	public void prepare() {
		addPayment(time, srcNode, dstNode, amount);
	}

	@Override
	public String toString() {
		return String.format("DebugTraffic(%d, %d, %d, %f)", srcNode, dstNode, amount, time);
	}
}
