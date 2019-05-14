package core.event;

import core.Simulation;

public abstract class Payload {

	public abstract void handle(int node, int sender, Simulation sim);
	
}
