package graph.gen;

import graph.Graph;

/**
 * Creates a Bianconi–Barabasi graph, which is a variant of the
 * Barabasi-Albert model which also accounts for latecomer fitness.
 * @author Josua
 */
public class BianconiBarabasi extends GraphGenerator {
	
	protected int initialNetworkSize = 5;
	private int size;
	private double averageDegree, fitnessDistribution, degreeFactor;
	
	/**
	 * Creates a Bianconi-Barabasi graph.
	 * @param nodes network size
	 * @param averageDegree average degree of nodes
	 * @param fitness fitness distribution (higher => more equally distributed)
	 */
	public BianconiBarabasi(int size, double averageDegree, double fitnessDistribution, double degreeFactor) {
		this.size = size;
		this.averageDegree = averageDegree;
		this.fitnessDistribution = fitnessDistribution;
		this.degreeFactor = degreeFactor;
	}

	@Override
	public Graph generate() {
		// start with some initial network
		GraphGenerator gen = new ErdosRenyi(initialNetworkSize, 2 * initialNetworkSize);
		gen.initComponent(this);
		Graph g = gen.generate();
		
		// assign fitness to all nodes
		//float accFitness = 0;
		double sum_nk = 0;
		double[] fitness = new double[size];
		for (int i = 0; i < size; i++) {
			fitness[i] = randomFitness();
			if (i < initialNetworkSize) {
				//accFitness += fitness[i];
				sum_nk += adjustedDegree(g, i) * fitness[i];
			}
		}
		
		// add rest of nodes	
		for (int i = initialNetworkSize; i < this.size; i++) {
			g.newNode();

//			// add 'newEdgesPerNode' channels
//			float accChannels = g.channels().size() * 2;
//			for (int n = 0; n < newEdgesPerNode; n++) {
//				float rand = random.getFloat(), acc = 0;
//				for (int j = 0; j < i; j++) {
//					acc += degreeWeighting * (g.node(j).getDegree() / accChannels) 
//							+ (1 - degreeWeighting) * (fitness[n]  / accFitness);
//					if (rand <= acc) {
//						if (g.channel(i, j) != null) {
//							n--;
//							break;
//						}
//						g.newChannel(i, j);
//						break;
//					}
//				}
//			}
//			accFitness += fitness[i];
			
			double tmp_sum_nk = 0;
			while (g.node(i).getDegree() == 0) { // prevent unconnected nodes
				for (int j = 0; j < i; j++) {
					if (random.getFloat() <= 0.5 * averageDegree * fitness[j] * adjustedDegree(g, j) / sum_nk) {
						double oldDegree = adjustedDegree(g, i);
						g.newChannel(i, j);	
						tmp_sum_nk += (adjustedDegree(g, i) - oldDegree) * fitness[j];
					}
				}
			}
			sum_nk += tmp_sum_nk + adjustedDegree(g, i) * fitness[i];
		}
		
		return g;
	}

	
	private double randomFitness() {
		return Math.exp(-random.getDouble() * fitnessDistribution);
	}
	
	private double adjustedDegree(Graph g, int id) {
		return (g.node(id).getDegree() - 1) *  + 1;
	}
	
	@Override
	public String toString() {
		return String.format("BianconiBarabasi(%d, %d, %f, %f)",
			size, averageDegree, fitnessDistribution, degreeFactor);
	}
}
