package core.event;

/** An event repeatetely occurring at a specific node **/
public abstract class RepeatedLocalEvent extends LocalEvent {
	private float interval;
	
	public RepeatedLocalEvent interval(float interval) {
		this.interval = interval;
		return this;
	}
	
	@Override
	public void prepareAndRun() {
		after(interval, sim);
		run();
	}
	
}
