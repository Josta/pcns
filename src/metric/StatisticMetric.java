package metric;

/** A statistics metric is a metric which produces no dedicated output file **/
public abstract class StatisticMetric extends Metric {
	
	public StatisticMetric() {
		super();
		this.storeToFile = false;
	}
	
}
