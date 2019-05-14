package plot;

import java.io.IOException;
import java.util.List;

/** Plots one metric **/
public class Plot extends GNUPlot {
	
	public Plot(String name) {
		super(name);
		add(name);
	}
	
	public Plot fromCol(int col) {
		sources.clear();
		add(name, col);
		return this;
	}
	
	public Plot withConfidence() {
		sources.clear();
		addWithConfidence(name);
		return this;
	}
	
	public Plot withConfidence(int col, int confCol) {
		sources.clear();
		addWithConfidence(name, col, confCol);
		return this;
	}
	
	public Plot withBoxes() {
		sources.clear();
		add(name, 1, "boxes");
		return this;
	}
	
	@Override
	protected void create() throws IOException {
		super.create();
		String[] src = sources.get(0);
		List<String> labels = getFirstDataLine(src[0]);
		write("set xlabel '" + labels.get(0) + "'");		
		write("set ylabel '" + labels.get(1) + "'");
		write("set key autotitle columnhead");
		write("unset key");
		write("set autoscale y");
		writeLineStylesAndPlotline();
	}
	
}