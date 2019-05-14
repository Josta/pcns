package core.event;

import java.util.Set;

import core.Simulation;

public class PMessage extends LocalEvent {

	protected int sender;
	protected Payload payload;
	protected int hops;
	private static float STANDARD_PROPAGATION_DELAY = 0.01f;
	
	private PMessage(int sender, int receiver, Payload payload, int hops) {	
		this.sender = sender;
		this.node = receiver;
		this.payload = payload;
		this.hops = hops;
	}
	
	/** Send payload immediately to one neighbor **/
	public static void send(Payload payload, int sender, int receiver, Simulation sim) {
		new PMessage(sender, receiver, payload, 0).after(STANDARD_PROPAGATION_DELAY, sim);
	}
	
	/** Flood payload immediately to all neighbors **/
	public static void flood(Payload payload, int sender, Set<Integer> blacklist, Simulation sim) {
		for (int neighbor : sim.graph().node(sender).neighbors()) {
			if (blacklist == null || !blacklist.contains(neighbor))
				new PMessage(sender, neighbor, payload, 0).after(STANDARD_PROPAGATION_DELAY, sim);
		}
	}
	
	/** Forward payload immediately to all but original sender **/
	public void forward() {
		for (int neighbor : sim.graph().node(sender).neighbors()) {
			if (neighbor != sender)
				new PMessage(node, neighbor, payload, hops + 1).after(STANDARD_PROPAGATION_DELAY, sim);
		}
	}

	@Override
	public void run() {
		payload.handle(node, sender, sim);
		completed();
	}
	
}