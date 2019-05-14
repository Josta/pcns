package payment.traffic;

import java.util.ArrayList;
import core.Component;
import graph.Node;
import payment.Payment;
import utility.lib.Lists;

public abstract class Traffic extends Component {

	protected ArrayList<Node> consumers;

	public void prepare() {
		consumers = graph().nodesWithRole(Node.ROLE_CONSUMER);
		if (consumers.isEmpty()) {
			throw new IllegalStateException("No consumers found for traffic generation.");
		}
	}
	
	protected void addPayment(float time, int src, int dst, int amount) {
		new Payment(src, dst, amount).at(time, sim);
	}
	
	protected boolean isTrivialRoute(int src, int dst) {
		return Lists.intersect(
			graph().node(src).neighbors(),
			graph().node(dst).neighbors());
	}
	
}
