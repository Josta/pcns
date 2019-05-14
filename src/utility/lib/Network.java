package utility.lib;

import java.util.Set;
import java.util.function.Function;

import core.Simulation;
import core.event.Message;

public class Network {

	private Network() {}
	
	/** Sends a new message to all neighbors **/
	public static void flood(int node,  Set<Integer> blacklist, Simulation sim, Function<Integer, Message> message) {
		for (int neighbor : sim.graph().node(node).neighbors()) {
			if (blacklist == null || !blacklist.contains(neighbor))
				message.apply(neighbor).send(node, neighbor, sim);
		}
	}
	
}
