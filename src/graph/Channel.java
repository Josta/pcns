package graph;

/**
 * A bidirectional edge in the graph. Stores a capacity in either direction.
 */
public class Channel implements Cloneable {
	private static final int HTLC_MIN = 0, HTLC_MAX = Integer.MAX_VALUE;
	private int id, node1, node2;
	private int capacity1, capacity2;
	private int minHTLC1, minHTLC2, maxHTLC1, maxHTLC2;
	private int timelockDelta1, timelockDelta2;
	private int feeBase1, feeBase2; // MSAT
	private int feeRate1, feeRate2; // MMSAT
	private boolean disabled1, disabled2;
	private float timestamp;
	
	public Channel(int id, int node1, int node2, int capacity1, int capacity2) {
		this.id = id;
		this.node1 = node1;
		this.node2 = node2;
		this.capacity1 = capacity1;
		this.capacity2 = capacity2;
		this.minHTLC1 = HTLC_MIN;
		this.minHTLC2 = HTLC_MIN;
		this.maxHTLC1 = HTLC_MAX;
		this.maxHTLC2 = HTLC_MAX;
		this.timelockDelta1 = 1;
		this.timelockDelta2 = 1;
		this.feeBase1 = this.feeBase2 = 1000;
		this.feeRate1 = this.feeRate2 = 2;
		this.disabled1 = false;
		this.disabled2 = false;
	}
	
	public Channel(int id, int node1, int node2) {
		this(id, node1, node2, 0, 0);
	}
	
	
	// GETTERS

	public int getID() {
		return id;
	}
	
	public int getCapacity() {
		return capacity1 + capacity2;
	}
	
	public int getMinTimelockDelta() {
		return 12; // suggested by BOLT 2
	}

	public int getNode1() {
		return node1;
	}
	
	public int getNode2() {
		return node2;
	}
	
	public int getOtherNode(int node) {
		return ((node == node1) ? node2 : node1);
	}
	
	public int getCapacity1() {
		return capacity1;
	}
	
	public int getCapacity2() {
		return capacity2;
	}
	
	public int getCapacity(int node) {
		return ((node == node1) ? capacity1 : capacity2);
	}
	
	public double getDisbalance() {
		return 2 * Math.abs(0.5 - capacity1 / (double) getCapacity());
	}

	public void setCapacities(int capacity1, int capacity2) {
		this.capacity1 = capacity1;
		this.capacity2 = capacity2;
	}
	
	public boolean setFees(int base1, int base2, int rate1, int rate2) {
		boolean changed = feeBase1 != base1 || feeBase1 != base1 || feeRate1 != rate1 || feeRate2 != rate2;
		this.feeBase1 = base1;
		this.feeBase2 = base2;
		this.feeRate1 = rate1;
		this.feeRate2 = rate2;
		return changed;
	}
	
	public void setTimelockDeltas(int delta1, int delta2) {
		this.timelockDelta1 = delta1;
		this.timelockDelta2 = delta2;
	}
	
	public void setHTLCAmountLimits(int min1, int min2, int max1, int max2) {
		this.minHTLC1 = min1;
		this.maxHTLC1 = max1 > 0 ? max1 : Integer.MAX_VALUE;
		this.minHTLC2 = min2;
		this.maxHTLC2 = max2 > 0 ? max2 : Integer.MAX_VALUE;
	}
	
	public void setDisabled(boolean dir12, boolean dir21) {
		this.disabled1 = dir12;
		this.disabled2 = dir21;
	}
	
	/**
	 * Checks whether a payment can be routed.
	 * The given amount should be before fees for this channel.
	 */
	public boolean canPay(int sender, int amount) {
		return amount > 0
			&& amount >= getHTLCMinimum(sender)
			&& amount < getHTLCMaximum(sender)
			&& amount + getFee(sender, amount) <= getCapacity(sender);
	}

	/** Calculates the fee required to transfer a payment **/
	public int getFee(int sender, int amount) {
		int feeBase = (sender == node1) ? feeBase1 : feeBase2;
		double feeRate = (sender == node1) ? feeRate1 : feeRate2;
		return (int) Math.ceil(0.001 * feeBase + (amount * feeRate * 0.001));
	}
	
	@Override
	public Channel clone() {
        try {
			return (Channel) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean isNewerThan(Channel ch) {
		return this.timestamp - ch.timestamp > 0.005f;
	}

	/** Add/subtract from a channel side **/
	public void modify(int node, int amount) {
		if (node == node1) {
			capacity1 += amount;
		} else {
			capacity2 += amount;
		}
	}

	/** Returns the min transfer amount that this channel accepts **/
	public int getHTLCMinimum(int sender) {
		return (sender == node1) ? minHTLC2 : minHTLC1;
	}
	
	/** Returns the max transfer amount that this channel accept **/
	private int getHTLCMaximum(int sender) {
		return (sender == node1) ? maxHTLC2 : maxHTLC1;
	}

	public void setNode1(int id) {
		this.node1 = id;
	}
	
	public void setNode2(int id) {
		this.node2 = id;
	}

	public void setID(int id) {
		this.id = id;
	}
	
	public Channel setTimestamp(float time) {
		this.timestamp = time;
		return this;
	}

	/**
	 * Returns the base fee of this channel
	 * @return msat
	 */
	public int getBaseFee(int sender) {
		return (sender == node1) ? feeBase1 : feeBase2;
	}
	
	/**
	 * Return the fee rate of this channel
	 * @return milli msat
	 */
	public int getFeeRate(int sender) {
		return (sender == node1) ? feeRate1 : feeRate2;
	}

	/*
	public void addHTLC(NewHTLC htlc, int sender) {
		// TODO Auto-generated method stub
		
	}

	public void completeHTLC(boolean fulfilled) {
		// TODO Auto-generated method stub
		
	}*/

	@Override
	public String toString() {
		return "" + node1 + "<>" + node2;
	}
}
