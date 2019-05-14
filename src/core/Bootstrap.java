package core;

import static utility.lib.Unit.*;

import java.util.Arrays;

import graph.Node;
import graph.gen.*;
import graph.transform.*;
import metric.Metric;
import metric.StatisticMetric;
import metric.graph.*;
import metric.load.*;
import metric.payments.*;
import metric.routing.*;
import payment.fees.*;
import payment.traffic.*;
import plot.*;
import routing.algorithm.*;
import routing.costs.*;
import utility.RunCombiner;
import utility.global.Config;

@SuppressWarnings("unused")
public class Bootstrap {
	private static final double CONF_95 = 1.96;

	public static void main(String[] args) {
		configureDefaults();
		runSimLCR();
		//analyseNetwork();
		//runCombiners();
		System.exit(0);
	}
	
	public static void runCombiners() {
		Config.set("RUNS", 1);
		Config.set("BASE_DIR", "output/lcr-100k-ztt");
		
		new RunCombiner("Imbalance", CONF_95).combine();
		new RunCombiner("TouchedChannels", CONF_95).combine();
		new RunCombiner("ActiveChannels", CONF_95).combine();
		new RunCombiner("DepletedChannels", CONF_95).combine();
		new RunCombiner("ActiveConsumers", CONF_95).combine();
		new RunCombiner("Fee", CONF_95).combine();
		new RunCombiner("OpenPayments", CONF_95).combine();
		new RunCombiner("PaymentDelay", CONF_95).combine();
		new RunCombiner("PaymentResults", CONF_95).combine();
		new RunCombiner("RouteLength", CONF_95).combine();
		new RunCombiner("RouteSplit", CONF_95).combine();
		new RunCombiner("RoutingManagementTraffic", CONF_95).combine();
		
		Config.set("OUTPUT_DIR", Config.get("BASE_DIR") + "/combined");		
		new SharedPlot("Channels")
			.addWithConfidence("Imbalance")
			.addWithConfidence("TouchedChannels")
			.addWithConfidence("ActiveChannels")
			.addWithConfidence("DepletedChannels")
			.addWithConfidence("ActiveConsumers")
			.run();
		new MultiPlot("Fees", "SAT", "%")
			.addWithConfidence("Fee", 1, 3, 1)
			.addWithConfidence("Fee", 2, 4, 2)
			.run();
		new Plot("OpenPayments").withConfidence().run();
		new Plot("PaymentDelay").withConfidence().run();
		new SharedPlot("PaymentResults")
			.addWithConfidence("PaymentResults", 1, 5)
			.addWithConfidence("PaymentResults", 2, 6)
			.addWithConfidence("PaymentResults", 4, 8)
			.run();
		new Plot("RouteLength").withConfidence().run();
		new Plot("RouteSplit").withConfidence().run();
		new Plot("RoutingManagementTraffic").withConfidence().run();
	}
	
	public static void analyseNetwork() {
		Config.set("RANDOM_SEED", 10085);
		Config.set("MAX_RUN_TIME", 0);
		Config.set("RUNS", 1); 
		Config.set("BASE_DIR", "output/net");
		
		new Simulation(
			new JsonImporter("input/lightning-network-graph.json"),
			//new BianconiBarabasi(10000, 19, 0.6f, 0.4f),
			//new KlemmEguiluz(10000, 9, 1, 1, 0.23),
			//new DebugTraffic(1001, 1002, btc(0.0005), 100),
			new ConstantTraffic(1*PER_SECOND, euro(0.5)),
			new OptimalRouting()
		).transform(
			new PruneSecondaryPartitions(),
			//new CapacitiesSetUniformly(euro(50)),
			new CapacitiesSetByDegree(0, 2500 * BIT),
			new NodeRolesSetUniformly(Node.ROLE_CONSUMER)
		).measure(
			new BasicGraphProperties(),
			new CapacityDistribution(30),
			new DegreeDistribution(),
			new GraphDiameter()
			//new ClusteringCoefficient()
		).plot(
			new Plot("CapacityDistribution").withBoxes(),
			new Plot("DegreeDistribution").withLogScale("x")
			//new GraphPlot("graph")
		).start();
	}

	public static void runSimLCR() {
		Config.set("RANDOM_SEED", 1628);
		Config.set("MAX_RUN_TIME", 5000);
		Config.set("RUNS", 10);
		Config.set("BASE_DIR", "output/lcr-100k-zuz");

		new Simulation(
			new KlemmEguiluz(95000, 9, 1, 1, 0.23),
			//new ErdosRenyi(4500, 40000),
			//new BarabasiAlbert(4500, 9),
			new ConstantTraffic(500*PER_SECOND, euro(2)),
			new LandmarkCentricRouting(15*SECONDS, 48)
		).transform(
			new CapacitiesSetUniformly(euro(250)),
			new GatewaysSelectRandomly(5000),
			new ConsumersAdd(5000, 3, euro(100))
		).measure(
			new BasicGraphProperties(),
			new CapacityDistribution(80),
			new DegreeDistribution(),
			//new GraphDiameter(),
			new ClusteringCoefficient(),
			new Imbalance(),
			new ActiveChannels(),
			new ActiveConsumers(),
			new DepletedChannels(euro(1.99)),
			new TimeAnnouncement().interval(20),
			new TouchedChannels(),
			new Fee().interval(15*SECONDS),
			new OpenPayments().interval(15*SECONDS),
			new PaymentDelay().interval(15*SECONDS),
			new PaymentResults().interval(10*SECONDS),
			new RouteLength().interval(15*SECONDS),		
			new RouteSplit(),
			new RoutingManagementTraffic().interval(20)
		).start();
	}
	
	public static void runSimLUR() {
		Config.set("RANDOM_SEED", 72767);
		Config.set("MAX_RUN_TIME", 5000);
		Config.set("RUNS", 5);
		Config.set("BASE_DIR", "output/lur-100k-fff");

		new Simulation(
			new KlemmEguiluz(95000, 9, 1, 1, 0.23),
			//new BarabasiAlbert(4500, 9),
			//new ErdosRenyi(4500, 40000),
			new ConstantTraffic(500*PER_SECOND, euro(2)),
			new LandmarkUniverseRouting(20*SECONDS, 16, 3)
		).transform(
			new CapacitiesSetUniformly(euro(250)),
			new GatewaysSelectRandomly(5000),
			new ConsumersAdd(5000, 3, euro(100))
		).measure(
			new BasicGraphProperties(),
			new CapacityDistribution(80),
			new DegreeDistribution(),
			new ClusteringCoefficient(),
			new Imbalance(),
			new ActiveChannels(),
			new ActiveConsumers(),
			new DepletedChannels(euro(1.99)),
			new TimeAnnouncement().interval(20),
			new TouchedChannels(),
			new Fee(),
			new OpenPayments(),
			new PaymentDelay(),
			new PaymentResults().interval(10*SECONDS),
			new RouteLength().interval(15*SECONDS),	
			new RouteSplit(),
			new RoutingManagementTraffic().interval(20)
		).start();
	}
	
	public static void runSimSR() {
		Config.set("RANDOM_SEED", 10084);
		Config.set("MAX_RUN_TIME", 5000);
		Config.set("RUNS", 10);
		Config.set("BASE_DIR", "output/sr-5k-albert");
		
		new Simulation(
			//new KlemmEguiluz(4500, 9, 1, 1, 0.23),
			new BarabasiAlbert(4500, 9),
			//new ErdosRenyi(4500, 40000),
			new ConstantTraffic(100*PER_SECOND, euro(2)),
			new SourceGraphRouting(2 * SECONDS, 3)
		).transform(
			new CapacitiesSetUniformly(euro(50)),
			new GatewaysSelectRandomly(500),
			new ConsumersAdd(500, 3, euro(50))
		).measure(
			new BasicGraphProperties(),
			new CapacityDistribution(80),
			new DegreeDistribution(),
			new ClusteringCoefficient(),
			new Imbalance(),
			new ActiveChannels(),
			new ActiveConsumers(),
			new DepletedChannels(euro(1.99)),
			new TimeAnnouncement().interval(20),
			new TouchedChannels(),
			new Fee(),
			new OpenPayments(),
			new PaymentDelay(),
			new PaymentResults().interval(10*SECONDS),
			new RouteLength(),		
			new RouteSplit(),
			new RoutingManagementTraffic().interval(20)
		).startRun(4, -4678671089151032232L);
	}
		
	public static void runSimFR() {
		//Config.set("RANDOM_SEED", 10085);
		Config.set("RANDOM_SEED", 4372);
		Config.set("MAX_RUN_TIME", 5000);
		Config.set("RUNS", 10);
		Config.set("BASE_DIR", "output/fr-5k-erdos");
		
		// MIN_HTLC = 1SAT, MAX_HTLC = 100000SAT, BASE=1SAT, RATE=1MMSAT
		new Simulation(
			//new KlemmEguiluz(4500, 9, 1, 1, 0.23),
				new ErdosRenyi(4500, 40000),
			new ConstantTraffic(100*PER_SECOND, euro(2)),
			new FlareRouting()
		).transform(
			//new PruneSecondaryPartitions(),
			new CapacitiesSetUniformly(euro(50)),
			new GatewaysSelectRandomly(500),
			new ConsumersAdd(500, 3, euro(50))
		).measure(
			new BasicGraphProperties(),
			new CapacityDistribution(40),
			new DegreeDistribution(),
			new ClusteringCoefficient(),
			new Imbalance(),
			new ActiveChannels(),
			new ActiveConsumers(),
			new DepletedChannels(euro(1.99)),
			new TimeAnnouncement().interval(20),
			new TouchedChannels(),
			new Fee(),
			new OpenPayments(),
			new PaymentDelay(),
			new PaymentResults().interval(10*SECONDS),
			new RouteLength(),		
			new RouteSplit(),
			new RoutingManagementTraffic().interval(20)
		).startRun(4, -29785796921951705L);
	}
	
	public static void runSimConf() {
		Config.set("RANDOM_SEED", 4372);
		Config.set("MAX_RUN_TIME", 500);
		Config.set("RUNS", 1);
		Config.set("BASE_DIR", "output/sim-conf-lcr-5k");
		
		Config.set("OUTPUT_DIR", "output/sim-conf-lcr-5k/run0");
		Metric m = new Metric() {}.setName("SUCCESS");
		m.prepare();
		
		for (int i = 1; i <= 40; i+=2) {
			for (int t = 1; t <= 32; t+=2) {		
				PaymentResults pr = new PaymentResults();
				new Simulation(
					new KlemmEguiluz(4500, 9, 1, 1, 0.23),
					new ConstantTraffic(100*PER_SECOND, euro(2)),
					new LandmarkCentricRouting(i, t)
				).transform(
					new CapacitiesSetUniformly(euro(50)),
					new GatewaysSelectRandomly(50),
					new ConsumersAdd(500, 3, euro(50))
				).measure(
					new TimeAnnouncement(),
					pr.interval(20*SECONDS).setName("success-" + i + "s-" + t + "t")
				).plot(
					new Plot("success-" + i + "s-" + t + "t").fromCol(2)
				).start();
				m.write(i + " " + t + " " + pr.getSuccessRate());
			}
		}
		m.finish();
	}
	
	public static void configureDefaults() {
		// environment
		Config.set("WINDOWS", true);
		Config.set("GNUPLOT_PATH", "C:/Tools/gnuplot/bin/");
		Config.set("GRAPHVIZ_PATH", "C:/Tools/graphviz/bin/");
		
		// reporting
		Config.set("GNUPLOT_PRINT_ERRORS", true);
		Config.set("GRAPHVIZ_PRINT_ERRORS", true);
		
		// PRNG
		Config.set("RANDOM_SEED", System.nanoTime());
		
		// simulation
		Config.set("DEFAULT_SAMPLING_INTERVAL", 5*SECONDS);
		Config.set("MAX_PATH_LENGTH", 15);
		Config.set("MAX_FEE_PERCENT", 30);
		Config.set("BITCOIN_EUROS", 4874f);
		
		// output
		Config.set("OUTPUT_DIR", "output/default");
		Config.set("GNUPLOT_SAVE_PLOTFILES", true);
		Config.set("GRAPHVIZ_SAVE_PLOTFILES", false);
		Config.set("METRIC_WRITE_BUFFER", 1024);
	}

}
