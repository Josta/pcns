package payment.selector;

import java.util.List;

import core.Component;
import payment.Payment;
import payment.Route;
import utility.global.Config;

public abstract class RouteSelector extends Component {

	protected final int MAX_PATH_LENGTH;
	protected final double MAX_FEE_PERCENT;
	
	public RouteSelector() {
		this.MAX_FEE_PERCENT = Config.getDouble("MAX_FEE_PERCENT");
		this.MAX_PATH_LENGTH = Config.getInt("MAX_PATH_LENGTH");
	}
	
	public abstract List<Route> selectRoutes(Payment payment);
	
}
