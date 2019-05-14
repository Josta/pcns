package payment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import core.event.Event;

public class Payment extends Event {

	private final int source, target;
	private final int amount;
	
	private List<int[]> paths;
	private List<Route> routes;
	private List<Runnable> executors;
	private Result result;
	
	// only for debug reasons
	private int id;
	private static int globalID = 0;
	private boolean dryRun;
	
	public enum Result {
		UNDETERMINED (0), SUCCESS (1),
		NO_PATH (2), PATH_TOO_LONG (3),
		HTLC_FAILURE (4);
		private final int code;
	    private Result(int code) {this.code = code;}
	    public int getCode() {return this.code;}
	}
	
	public Payment(int srcNode, int dstNode, int amount) {
		this.source = srcNode;
		this.target = dstNode;
		this.amount = amount;
		this.result = Result.UNDETERMINED;
		this.paths = new ArrayList<>();
		this.executors = new LinkedList<>();
		this.routes = null;
		this.dryRun = false;
	}
	
	
	// PAYMENT PROCESS STEPS
	
	/** Step 1 of payment process **/
	@Override
	public void run() {
		this.id = globalID++;
		sim.routing().findPaths(this);
	}
	
	/** Step 2 of payment process (called for each found path) **/
	public void addPath(int[] path) {
		for (int[] p : paths)
			if (Arrays.equals(p, path)) return;
		paths.add(path);
	}
	
	/** Step 3 of payment process (called when path finding completed) **/
	public void selectRoutes() {
		if (routes != null) return;
		routes = sim.routeSelector().selectRoutes(this);
		if (dryRun) {
			executors.forEach(e -> e.run());
		} else if (routes == null) {
			completeWith(Result.NO_PATH);
		} else for (Route route : routes) {
			new HTLC(route).send(source, route.getNode(1), sim);
		}
	}
	
	/** Step 4 of payment process (called for each ready route) **/
	public void routeReady(Runnable executor) {
		executors.add(executor);
		if (routes.stream().allMatch(r -> r.hasState(Route.State.READY))) {
			executors.forEach(e -> e.run());
			executors.clear();
		}
	}
	
	/** Step 4b of payment process (called for a failed route) **/
	public void routeFailed() {
		completeWith(Result.HTLC_FAILURE);
	}
	
	/** Step 5 of payment process (called for each paid route) **/
	public void routePaid() {
		if (routes.stream().allMatch(r -> r.hasState(Route.State.PAID)))
			completeWith(Result.SUCCESS);
	}
	
	/** Finishes the payment process **/
	private void completeWith(Result result) {
		if (this.result == Result.UNDETERMINED) {
			this.result = result;
			completed();
		}
	}
	
	
	// INTRUSIVE MODIFICATION
	
	/** Replaces the payment execution/completion with a custom runnable **/
	public void setDryRun(Runnable executor) {
		this.dryRun = true;
		executors.add(executor);
	}
	
	
	// GETTERS
	
	public int getID() {
		return id;
	}
	
	public int getSource() {
		return source;
	}

	public int getTarget() {
		return target;
	}

	public int getAmount() {
		return amount;
	}
	
	public Result getResult() {
		return result;
	}

	public int getFee() {
		return routes != null ? routes.stream().mapToInt(r -> r.getFee()).sum() : Integer.MAX_VALUE;
	}

	public List<Route> getRoutes() {
		return routes;
	}
	
	public List<int[]> getPaths() {
		return paths;
	}
	
	public boolean hasSucceeded() {
		return (result == Result.SUCCESS);
	}

}
