package payment.traffic;

import java.util.List;

import core.event.Event;
import graph.Node;

/** Creates uniform, random traffic **/
public class ConstantTraffic extends Traffic {
	
	private float interval, frequency;
	private int amount; //SAT
	
	/**
	 * Creates constant traffic with random source and destination nodes.
	 * @param frequency payments per second
	 * @param amount payment size (sat)
	 */
	public ConstantTraffic(float frequency, int amount) {
		this.interval = 5;
		this.frequency = frequency;
		this.amount = amount;
	}
	
	/**
	 * Enqueues an initial traffic generation event.
	 */
	@Override
	public void prepare() {
		super.prepare();
		new CreateTraffic().now(sim);
	}
	
	
	private class CreateTraffic extends Event {

		@Override
		public void run() {
			
			// create (frequency * interval) payments
			for (int i = 0; i < frequency * interval; i++) {
				
				// get uniformly random time within the interval, and uniformly random payment amount
				float paymentTime = time + interval * random.getFloat();
				
				List<Node> nodes = random.getDistinct(consumers, 2);
				int srcNode = nodes.get(0).getID(),
					dstNode = nodes.get(1).getID();
				
				if (!isTrivialRoute(srcNode, dstNode)) {
					addPayment(paymentTime, srcNode, dstNode, amount);
				} else {
					i--;
				}
			}
			
			// register traffic generation for the next interval
			new CreateTraffic().after(interval, sim);
		}
		
	}
	
	@Override
	public String toString() {
		return String.format("ConstantTraffic(%f, %d)", frequency, amount);
	}
	
}
