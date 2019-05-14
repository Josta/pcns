package metric.load;

import com.javamex.classmexer.MemoryUtil;

import metric.StatisticMetric;

/**
 * Estimates storage consumption of nodes, channels, routing, and overall.
 * Make sure routing implements estimateStorage() correctly before using this.
 **/
public class StorageConsumption extends StatisticMetric {

	private final float MILLION = (float) 1000000.0; 
	
	@Override
	public void beforeSimulation() {		
		long nodeStorage = graph().size() * MemoryUtil.memoryUsageOf(graph().node(0));	
		long channelStorage = graph().channels().size() * MemoryUtil.memoryUsageOf(graph().channel(0));
		long heapStorage = Runtime.getRuntime().totalMemory(); 
		long routingStorage =  sim.routing().estimateStorage();
		stat("RAM Heap", (heapStorage / MILLION) + " MB");
		stat("RAM Nodes", (nodeStorage / MILLION) + " MB");
		stat("RAM Channels", (channelStorage / MILLION) + " MB");
		stat("RAM Routing", (routingStorage / MILLION) + " MB");
	}

}
