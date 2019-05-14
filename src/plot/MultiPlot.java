package plot;

import java.io.IOException;

/**
 * Plots any number of metrics with the same x axis.
 * Metric 1 gets axis y1, all other metrics share axis y2.
 * Make sure the units match on axis y2.
 * @author Josua
 */
public class MultiPlot extends GNUPlot {
	
	private String axisy1, axisy2;
	
	/**
	 * Plots metrics with a shared x axis. You can define the plot style for each metric with a colon suffix.
	 * @param sources
	 */
	public MultiPlot(String name, String axisy1, String axisy2) {
		super(name);
		this.axisy1 = axisy1;
		this.axisy2 = axisy2;
	}
	
	@Override
	protected void create() throws IOException {
		super.create();
		String[] src1 = sources.get(0);
		write("set xlabel '" + getFirstDataLine(src1[0]).get(0) + "'");	
		write("set ylabel '" + axisy1 + "'");
		write("set y2label '" + axisy2 + "'");
		write("set ytics nomirror");
		write("set y2tics");
		write("set tics out");
		write("set autoscale y");
		write("set autoscale y2");
		write("set key autotitle columnhead");
		writeLineStylesAndPlotline();
	}
	
	public MultiPlot addWithConfidence(String file, int col, int confCol, int yAxis) {
		String color = colors.pop();
		sources.add(new String[] {file, String.format(
			"1:($%1$d-$%2$d):($%1$d+$%2$d) with filledcurves axes x1y%3$d",
			col + 1, confCol + 1, yAxis), color});
		sources.add(new String[] {file, String.format(
			"1:%d with lines axes x1y%d", col + 1, yAxis), color});
		return this;
	}
	
}