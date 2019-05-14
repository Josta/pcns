package graph.gen;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import graph.Graph;

/**
 * 
 */
public class KlemmEguiluz extends GraphGenerator {
	
	private int size, connections;
	private double fitnessDistribution, degreeFactor, mu;
	double[] fitness;
	
	/**
	 * Creates a Bianconi-Barabasi graph.
	 * @param nodes network size
	 * @param connections new links per added node
	 * @param fitness fitness distribution (smaller => more equally distributed)
	 */
	public KlemmEguiluz(int size, int connections, double fitnessDistribution, double degreeFactor, double mu) {
		this.size = size;
		this.connections = connections;
		this.fitnessDistribution = fitnessDistribution;
		this.degreeFactor = degreeFactor;
		this.mu = mu;
	}

	@Override
	public Graph generate() {
		// start with some initial network
		GraphGenerator gen = new ErdosRenyi(connections, 2 * connections);
		gen.initComponent(this);
		Graph g = gen.generate();
		
		// assign fitness to all nodes
		fitness = new double[size];
		for (int i = 0; i < size; i++)
			fitness[i] = randomFitness();
	
		List<Integer> active = g.nodes().stream()
			.map(n -> n.getID()).collect(Collectors.toList());
		double totalWeight = g.nodes().stream()
			.mapToDouble(nd -> weight(g, nd.getID())).sum();
		// add rest of nodes	
		for (int i = connections; i < this.size; i++) {
			g.newNode();
			/*int newEdges = random.getInt(active.size());
			List<Integer> activeCopy = new LinkedList<>(active);
			Collections.shuffle(activeCopy);
			for (int k = 0; k < newEdges; k++) {
				int node = activeCopy.get(k);*/
			for (int node : active) {
				// choose active or preferential random node
				if (random.getDouble() < mu)
					node = choosePreferentialRandom(g, null, totalWeight);
				// add link
				totalWeight -= weight(g, node);
				g.newChannel(i, node);
				totalWeight += weight(g, node);	
			}
			totalWeight += weight(g, i);
			// activate
			active.add(i);
			// deactivate preferential random active node
			double totalActiveWeight = active.stream().mapToDouble(k->weight(g,k)).sum();
			int rem = choosePreferentialRandom(g,
					active, totalActiveWeight);
			active.remove(new Integer(rem));
		}
		
		
		return g;
	}

	
	private double randomFitness() {
		return Math.exp(-random.getDouble() * fitnessDistribution);
	}
	
	private double weight(Graph g, int id) {
		return (g.node(id).getDegree() == 0) ? 0
			: ((g.node(id).getDegree() - 1) * degreeFactor + 1) * fitness[id];
	}
	
	private int choosePreferentialRandom(Graph g,
			List<Integer> inputs, double totalWeight) {
		double rand = random.getDouble(),
			   sum = 0;
		if (inputs != null) {
			for (int i : inputs) {
				sum += weight(g, i);
				if (rand * totalWeight <= sum)
					return i;
			}
		} else {
			for (int i = 0; i < g.size(); i++) {
				sum += weight(g, i);
				if (rand * totalWeight <= sum)
					return i;
			}
		}
		return -1;
	}
	
	@Override
	public String toString() {
		return String.format("BianconiBarabasi(%d, %d, %f, %f)",
			size, connections, fitnessDistribution, degreeFactor);
	}
}
