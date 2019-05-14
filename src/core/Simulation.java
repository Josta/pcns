package core;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import core.event.Event;
import graph.Graph;
import graph.gen.GraphGenerator;
import graph.transform.Transformation;
import metric.Metric;
import payment.fees.ExponentialFees;
import payment.fees.FeePolicy;
import payment.selector.AMPRouteSelector;
import payment.selector.RouteSelector;
import payment.traffic.Traffic;
import plot.AbstractPlot;
import routing.algorithm.RoutingAlgorithm;
import routing.costs.AdvancedRoutingCosts;
import routing.costs.RoutingCosts;
import utility.NonBlockingBufferedReader;
import utility.global.Config;
import utility.global.StopWatch;

/** Framework bundling the various simulation components together and running the event queue. **/
public class Simulation {
	
	private Random masterRandom;
	private GraphGenerator generator;
	private Graph graph;
	private RoutingAlgorithm routing;
	private Traffic traffic;
	private RoutingCosts costs, costsInverse;
	private FeePolicy feePolicy;
	private RouteSelector routeSelector;
	private List<Metric> metrics;
	private List<Transformation> transformations;
	private List<AbstractPlot> plots;
	private HashMap<Class<?>, List<Consumer<Event>>> beforeEventListeners;
	private HashMap<Class<?>, List<Consumer<Event>>> afterEventListeners;
	private PriorityQueue<Event> eventQueue;
	private float time, maxRunTime;
	private NonBlockingBufferedReader input;

	public Simulation() {	
		masterRandom = new Random(Config.getLong("RANDOM_SEED"));
		maxRunTime = Config.getFloat("MAX_RUN_TIME");
		input = new NonBlockingBufferedReader(System.in);
		metrics = new LinkedList<>();
		transformations = new LinkedList<>();
		plots = new LinkedList<>();
		// set defaults
		generate(null).routing(null).traffic(null);
		fees(new ExponentialFees(1000, 1, 10, 2));
		routingCosts(new AdvancedRoutingCosts(0, 100, 0));
		routeSelector(new AMPRouteSelector());
	}
	
	public Simulation(GraphGenerator gen, Traffic tr, RoutingAlgorithm r) {
		this();
		generate(gen).traffic(tr).routing(r);
	}
	
	// COMPONENT ADDITION

	public Simulation generate(GraphGenerator g) {
		this.generator = g;
		return this;
	}
	
	public Simulation routing(RoutingAlgorithm r) {
		this.routing = r;
		return this;
	}
	
	public Simulation routingCosts(RoutingCosts c) {
		this.costs = c;					
		this.costsInverse = c.clone();
		this.costsInverse.setInvert(true);
		return this;
	}
	
	public Simulation traffic(Traffic t) {
		this.traffic = t;
		return this;
	}
	
	public Simulation fees(FeePolicy p) {
		this.feePolicy = p;
		return this;
	}

	public Simulation routeSelector(RouteSelector s) {
		this.routeSelector = s;
		return this;
	}
	
	public Simulation transform(Transformation... ts) {
		for (Transformation t : ts) transformations.add(t);
		return this;
	}
	
	public Simulation measure(Metric... ms) {
		for (Metric m : ms) metrics.add(m);
		return this;
	}
	
	public Simulation plot(AbstractPlot... ps) {
		for (AbstractPlot p : ps) plots.add(p);
		return this;
	}
	
	
	// GETTERS

	public Graph graph() {
		return graph;
	}
	
	public RoutingAlgorithm routing() {
		return routing;
	}
	
	public RoutingCosts costs() {
		return costs;
	}
	
	public RoutingCosts inverseCosts() {
		return costsInverse;
	}
	
	public RouteSelector routeSelector() {
		return routeSelector;
	}
	
	public FeePolicy feePolicy() {
		return feePolicy;
	}
	
	// EVENT SIMULATION
	
	public void start() {
		int runCount = Config.getInt("RUNS");
		storeConfiguration();
		IntStream.range(0, runCount).forEach(i -> startRun(i, masterRandom.nextLong()));
	}
	
	public void startRun(int index, long seed) {
		Config.set("OUTPUT_DIR", Config.get("BASE_DIR") + "/run" + index);
		new File(Config.get("OUTPUT_DIR")).mkdirs();
		time = 0;
		beforeEventListeners = new HashMap<>();
		afterEventListeners = new HashMap<>();
		eventQueue = new PriorityQueue<Event>();
		
		Random runRandom = new Random(seed);
		List<Component> comp = new LinkedList<>(Arrays.asList(
			generator, traffic, routing, costs,
			costsInverse, feePolicy, routeSelector));
		comp.addAll(transformations);
		comp.addAll(metrics);
		comp.addAll(plots);
		comp.forEach(c -> c.initComponent(Simulation.this, runRandom.nextLong()));
		
		startGeneration();
		
		StopWatch.start("[Init Fee Policy]");
		feePolicy.prepare();
		StopWatch.measure();	
		
		StopWatch.start("[Init Routing]");	
		routing.prepare();
		StopWatch.measure();
		
		StopWatch.start("[Init Traffic]");
		traffic.prepare();
		StopWatch.measure();
		
		StopWatch.start("[Init Metrics]");
		metrics.stream().forEach(m -> m.prepare());
		StopWatch.measure();

		StopWatch.start("[Run Simulation]");
		while (advance());
		StopWatch.measure();
		
		StopWatch.start("[Finish Metrics]");
		metrics.stream().forEach(m -> m.finish());
		StopWatch.measure();
		
		startPlotting();
	}
	
	public void storeConfiguration() {
		Config.set("OUTPUT_DIR", Config.get("BASE_DIR"));
		new File(Config.get("OUTPUT_DIR")).mkdirs();
		Metric conf = new Metric(){};
		conf.setName("CONF");
		conf.prepare();
		conf.write("RANDOM_SEED: " + Config.get("RANDOM_SEED"));
		int runCount =  Config.getInt("RUNS");
		conf.write("RUNS: " + runCount);
		conf.write("MAX_RUN_TIME: " + Config.get("MAX_RUN_TIME"));
		masterRandom.setSeed(Config.getLong("RANDOM_SEED"));
		conf.write("RUN SEEDS: " + IntStream.range(1, runCount + 1)
			.mapToObj(r -> "" + r + ": " + masterRandom.nextLong())
			.collect(Collectors.joining(", ")));	
		masterRandom.setSeed(Config.getLong("RANDOM_SEED"));
		List<Component> comps = new LinkedList<>(Arrays.asList(generator,
			traffic, routing, costs, feePolicy, routeSelector));
		comps.addAll(transformations);		
		for (Component c : comps)
			conf.write(c.toString());
		conf.finish();		
	}

	/** Adds an event **/
	public void addEvent(Event event) {
		eventQueue.add(event);
	}
	
	/** Get notified before a certain event subtype has run **/
	@SuppressWarnings("unchecked")
	public <T extends Event> void beforeEvent(Class<T> klass, Consumer<T> listener) {
		List<Consumer<Event>> listeners = beforeEventListeners.get(klass);
		if (listeners == null) listeners = new LinkedList<>();
		listeners.add((Consumer<Event>) listener);
		beforeEventListeners.put(klass, listeners);
	}
	
	/** Get notified after a certain event subtype was completed **/
	@SuppressWarnings("unchecked")
	public <T extends Event> void afterEvent(Class<T> klass, Consumer<T> listener) {
		List<Consumer<Event>> listeners = afterEventListeners.get(klass);
		if (listeners == null) listeners = new LinkedList<>();
		listeners.add((Consumer<Event>) listener);
		afterEventListeners.put(klass, listeners);
	}

	
	
	// INTERNAL
	
	private boolean advance() {
		if ("q".equals(input.readLine())) {
			System.out.println("Quitting...");
			return false;
		}
		Event event = eventQueue.poll();
		if (event != null && event.getTime() < maxRunTime) {
			time = event.getTime();
			List<Consumer<Event>> listeners = beforeEventListeners.get(event.getClass());
			if (listeners != null)
				listeners.forEach(l -> l.accept(event));
			event.prepareAndRun();
			return true;
		}
		return false;
	}

	public void eventCompleted(Event event) {
		List<Consumer<Event>> listeners = afterEventListeners.get(event.getClass());
		if (listeners != null)
			listeners.forEach(l -> l.accept(event));
	}
	
	// START ALTERNATIVES (only generate, only plot)
	
	public void startGeneration() {
		StopWatch.start("[Generate graph]");
		graph = generator.generate();
		StopWatch.measure();
		for (Transformation t: transformations) {
			StopWatch.start("[Transformation " + t.getName() + "]");
			t.transform(graph);
			StopWatch.measure();
		}
		graph.optimize();
	}
	
	public void startPlotting() {
		StopWatch.start("[Create Plots]");
		plots.stream().forEach(p -> p.run());
		StopWatch.measure();
	}
	
	public float getTime() {
		return time;
	}

	public String printProperties() {
		String res = "-Generator: " + generator + "\n"
			+ "-Routing: " + routing + "\n"
			+ "-Traffic: " + traffic + "\n"
			+ "-Costs: " + costs + "\n"
			+ "-FeePolicy: " + feePolicy + "\n"
			+ "-RouteSelector: " + routeSelector + "\n";
		for (Transformation t : transformations) {
			res += "-Transformation: " + t + "\n";
		}
		res += "-MAX_RUN_TIME: " + maxRunTime;
		res += "-RUNS: " + Config.get("RUNS");
		return res;
	}
	
}
