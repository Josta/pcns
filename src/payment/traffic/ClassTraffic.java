package payment.traffic;

import static utility.lib.Unit.*;

import java.util.List;

import core.event.Event;
import graph.Node;

/** Creates uniform, random traffic **/
public class ClassTraffic extends Traffic {
	
	private float interval, frequency;
	
	/**
	 * Creates constant traffic with random source and destination nodes.
	 * The generator creates 3 different classes of payments (micro, medium, large).
	 * @param frequency payments per second
	 */
	public ClassTraffic(float frequency) {
		this.interval = 5;
		this.frequency = frequency;
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

				// get uniformly random time within the interval
				float paymentTime = time + interval * random.getFloat();
				
				// 3 payment classes
				int amount = 0;
				switch (random.getInt(3)) {
					case 0: amount = random.getIntBetween(1000 * BIT, 5000 * BIT); break;
					case 1: amount = random.getIntBetween(10 * BIT, 1000 * BIT); break;
					case 2: amount = random.getIntBetween(1, 10 * BIT); break;
				}
				
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
		return "ClassTraffic()";
	}
	
}
