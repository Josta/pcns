package core.event;

import core.Simulation;
import graph.Channel;

/**
 * A message transmitted over the network from one node
 * to one of its neighbors, with a propagation delay.
 * The run() method triggers upon message arrival.
 * @author Josua
 */
public abstract class Message extends LocalEvent {

	protected int sender;
	private static float STANDARD_PROPAGATION_DELAY = 0.01f;
	
	/** Sends a message by scheduling an arrival **/
	public void send(int sender, int receiver, Simulation sim) {
		from(sender).at(receiver).after(STANDARD_PROPAGATION_DELAY, sim);
	}
	
	/** Sends a message by scheduling an arrival **/
	public void send(int sender, Channel ch, Simulation sim) {
		send(sender, ch.getOtherNode(sender), sim);
	}
	
	public Message from(int sender) {
		this.sender = sender;
		return this;
	}
	
	public int getSender() {
		return sender;
	}
}
