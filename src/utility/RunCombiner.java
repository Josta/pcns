package utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import utility.global.Config;

/** Merges output data from several runs by computing averages and confidence intervals **/
public class RunCombiner {

	private String metricName;
	private double zValue;
	protected static final String NEW_LINE = System.getProperty("line.separator");
	
	/** Merges output data from several runs by computing averages and confidence intervals **/
	public RunCombiner(String metricName, double zValue) {
		this.metricName = metricName;
		this.zValue = zValue;
	}
	
	public void combine() {
		int runCount = Config.getInt("RUNS");
		String labels = "";
		
		try {
			// get value count
			BufferedReader probe = new BufferedReader(new FileReader(
					Config.get("BASE_DIR") + "/run0/" + metricName + ".dat"));
			probe.readLine();
			int valueCount = probe.readLine().split(" ").length - 1;
			
			// get data files (and labels)
			BufferedReader[] runs = new BufferedReader[runCount];
			for (int r = 0; r < runCount; r++) {	
				runs[r] = new BufferedReader(new FileReader(
					Config.get("BASE_DIR") + "/run" + r + "/" + metricName + ".dat"));
				labels = runs[r].readLine();
			}
			
			// get output file
			File file = new File(Config.get("BASE_DIR") + "/combined/" + metricName + ".dat");
			file.getParentFile().mkdirs();
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));   
			writer.write(labels + NEW_LINE);
			
			do {
				// get values for line for all runs
				String time = "";
				double[][] values = new double[valueCount][runCount];
				for (int r = 0; r < runCount; r++) {
					String[] line  = runs[r].readLine().split(" ");
					time = line[0];
					for (int i = 0; i < valueCount; i++)
						values[i][r] += Double.parseDouble(line[i + 1]) /** Double.parseDouble(time) / 10.0*/;
				}
				
				// get sample means and standard deviations
				double[] means = new double[valueCount],
						 devs = new double[valueCount],
						 intervals = new double[valueCount];
				for (int i = 0; i < valueCount; i++) {
					int iFixed = i;
					means[i] = Arrays.stream(values[i]).sum() / runCount;
					devs[i] = Math.sqrt(Arrays.stream(values[i])
						.map(v -> Math.pow(v - means[iFixed], 2)).sum() / runCount);
					intervals[i] = zValue * devs[i] / Math.sqrt(runCount);
				}
				
				writer.write(time + " " + joinDoubleArray(means)
					+ " " + joinDoubleArray(intervals) + NEW_LINE);
			} while (probe.readLine() != null);
			
			probe.close();
			writer.close();
			for (int r = 0; r < runCount; r++) {	
				runs[r].close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	private String joinDoubleArray(double[] arr) {
		return Arrays.stream(arr).mapToObj(String::valueOf).collect(Collectors.joining(" "));
	}
	
	
}
