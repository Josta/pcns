package core.event;

/**
 * An event occurring at a specific node.
 * @author Josua
 */
public abstract class LocalEvent extends Event {

	protected int node;
	
	/** Sets the node where this event should occurr. **/
	public LocalEvent at(int node) {
		this.node = node;
		return this;
	}
}
