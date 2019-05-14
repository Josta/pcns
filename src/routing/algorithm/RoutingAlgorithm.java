package routing.algorithm;

import core.Component;
import payment.Payment;

public abstract class RoutingAlgorithm extends Component {
	
	public abstract void prepare();
	
	public abstract void findPaths(Payment p);
	
	public abstract long estimateStorage();
	
}
