package metric;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import core.Component;
import utility.global.Config;
import utility.lib.Lists;

/**
 * A metric measures some static or dynamic aspect of the graph, traffic or routing.
 * Metrics are initialized directly before the EventQueue is started,
 * but after all other initialization (graph, transformations, ...).
 * @author Josua
 */
public abstract class Metric extends Component {
	
	protected String name;
	protected boolean storeToFile;
	
	private static boolean statisticsInitiated = false;
    private BufferedWriter writer;	
    protected static final String NEW_LINE = System.getProperty("line.separator");
	
	public Metric() {
		this.name = this.getClass().getSimpleName();
		this.storeToFile = true;
	}
	
	public Metric setName(String name) {
		this.name = name;
		return this;
	}

	public String getName() {
		return name;
	}
	
	/**
	 * Called before the simulation starts. Creates the metrics file.
	 */
	public void prepare() {
		if (storeToFile) {
			File file = new File(Config.get("OUTPUT_DIR") + "/" + name + ".dat");
			file.getParentFile().mkdirs();
			try{
	    		writer = new BufferedWriter(new FileWriter(file),
	    			Config.getInt("METRIC_WRITE_BUFFER"));          
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		}
		beforeSimulation();
	}
	
	/** Called after the simulation finished. Saves the metrics file. **/
	public void finish() {
		afterSimulation();
		if (storeToFile) {
			try {
				writer.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		}
	}
	
	/** Use beforeSimulation() to calulate static metrics or attach listeners/events for dynamic metrics **/
	protected void beforeSimulation() {};
	
	/** Use afterSimulation() to write out aggregated metric results after the simulation **/
	protected void afterSimulation() {};
	
	/** Writes the data file column labels. Should be called in beforeSimulation() **/
	protected void labels(String labelX, String[] labels) {
		labels(labelX, String.join("\" \"", labels));
	}
	
	/** Writes the data file column labels. Should be called in beforeSimulation() **/
	protected void labels(String... labels) {
		write("\"" + String.join("\" \"", labels) + "\"");
	}

	/** Writes a line **/
	public void write(String line) {
		try {
			writer.write(line + NEW_LINE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** Writes a data point **/
	protected void write(float valueX, double valueY) {
		write(valueX + " " + valueY);
	}
	
	/** Writes a data point **/
	protected <T extends Object> void write(float valueX, T[] values) {
		write(valueX + " " + String.join(" ", Lists.strArray(Arrays.stream(values).map(v -> String.valueOf(v)))));
	}
	
	/** Writes a data point **/
	protected <T extends Object> void write(@SuppressWarnings("unchecked") T... values) {
		write(String.join(" ", Lists.strArray(Arrays.stream(values).map(v -> String.valueOf(v)))));
	}
	
	/** Writes a line to the "_STATS.txt" file **/
	protected void stat(String name, String... value) {
		try {
		    FileWriter fw = new FileWriter(Config.get("OUTPUT_DIR") + "/_STATS.txt", statisticsInitiated);
		    statisticsInitiated = true;
		    fw.write(name + ": " + String.join(", ", value) + NEW_LINE);
		    fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void stat(String name, double value) {
		stat(name, String.valueOf(value));
	}
	

	
	protected static double percentage(long value, long total) {
		return Math.max(0, Math.min(1, value / (double) total)) * 100;
	}
	
}
