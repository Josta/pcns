package payment.selector;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import payment.Payment;
import payment.Route;

public class AMPRouteSelector extends RouteSelector {

	@Override
	public List<Route> selectRoutes(Payment payment) {
		List<Route> routes = Collections.emptyList();
		// select routes
		int routeCount;
		int fee = Integer.MAX_VALUE;
		for (routeCount = 1; routeCount < 8; routeCount++) {
			int splitAmount = (payment.getAmount() / routeCount) + 1;
			routes = payment.getPaths().stream()
				.filter(p -> p.length <= MAX_PATH_LENGTH)
				.map(p -> new Route(graph(), payment, p, splitAmount))
				.filter(r -> r.hasSufficientCapacities())
				.sorted((r1, r2) -> (int) (r1.getFee() - r2.getFee()))
				.limit(routeCount)
				.collect(Collectors.toList());
			fee = routes.stream().mapToInt(r -> r.getFee()).sum();
			if (routes.size() == routeCount && isValid(fee, payment)) break;
		}
		return (routes.size() == routeCount && isValid(fee, payment)) ? routes : null;
	}
	
	private boolean isValid(int fee, Payment p) {
		return fee <= p.getAmount() * MAX_FEE_PERCENT * 0.01;
	}

	@Override
	public String toString() {
		return "AMPRouteSelector()";
	}
}
