package routing.costs;

import core.Component;
import graph.Channel;

/**
 * Calculates the routing costs (= edge weight) for a specific channel and direction.
 */
public abstract class RoutingCosts extends Component implements Cloneable {
	
	protected boolean invert;

	public abstract long getCosts(Channel c, int sender);
	
	public void setInvert(boolean invert) {
		this.invert = invert;
	}
	
	public RoutingCosts clone() {
		try {
			return (RoutingCosts) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}
}
