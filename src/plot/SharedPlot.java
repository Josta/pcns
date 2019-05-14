package plot;

import java.io.IOException;
import java.util.List;

/**
 * Plots any number of metrics with the same x and y axis.
 * @author Josua
 */
public class SharedPlot extends GNUPlot {

	/**
	 * Plots metrics with a shared x and y axis.
	 * You can define the plot style for each metric with a colon suffix.
	 * @param sources
	 */
	public SharedPlot(String name) {
		super(name);
	}
	
	@Override
	protected void create() throws IOException {
		super.create();
		List<String> labels = getFirstDataLine(sources.get(0)[0]);
		write("set xlabel '" + labels.get(0) + "'");		
		write("set ylabel '" + labels.get(1).split("[\\(\\)]")[1] + "'");
		write("set key autotitle columnhead");
		write("unset key");
		write("set autoscale y");
		write("set style fill transparent solid 0.2 noborder");
		write("set key autotitle columnhead");
		writeLineStylesAndPlotline();
	}

}