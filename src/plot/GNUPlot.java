package plot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import utility.global.Config;

/** Plots metrics with GNUPlot **/
public abstract class GNUPlot extends AbstractPlot {

	protected List<String[]> sources;
	protected LinkedList<String> colors;
	protected List<String> commands;
    
	public GNUPlot(String name) {
		super(name, "plot");
		sources = new ArrayList<>();	
		colors = new LinkedList<>(Arrays.asList("#ff0000", "#00ff00", "#0000ff", "#ffff00", "#ff00ff"));
		commands = new LinkedList<>();
	}

	/** Implements the plotfile creation **/
	protected void create() throws IOException {
		write("set term png");
		write("set output \"" + name + ".png\"");
		for (String command : commands) {
			write(command);
		}
	}
	
	/** Creates the plot using gnuplot **/
	protected void plot() throws IOException {
		String ext = Config.getBoolean("WINDOWS") ? ".exe" : "";
		String binary = Config.get("GNUPLOT_PATH") + "gnuplot" + ext;
		String command = binary + " \"" + plotfile + "\"";
		System.out.println(command);
		Process p = Runtime.getRuntime().exec(command, null, new File(Config.get("OUTPUT_DIR")));
		if (Config.getBoolean("GNUPLOT_PRINT_ERRORS")) {
			InputStream stderr = p.getErrorStream();
			InputStreamReader isr = new InputStreamReader(stderr);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
		}
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (!Config.getBoolean("GNUPLOT_SAVE_PLOTFILES")) {
			new File(Config.get("OUTPUT_DIR") + "/" + plotfile).delete();
		}
	}
	
	
	// SOURCE ADDING METHODS
	
	public GNUPlot add(String file) {
		return add(file, 1);
	}
	
	public GNUPlot add(String file, int col) {
		return add(file, col, "lines");
	}
	
	public GNUPlot add(String file, int col, String style) {
		sources.add(new String[] {file, String.format("1:%d with %s", col + 1, style), colors.pop()});
		return this;
	}

	public GNUPlot addWithConfidence(String file, int col, int confCol) {
		String color = colors.pop();
		sources.add(new String[] {file, String.format(
			"1:($%1$d-$%2$d):($%1$d+$%2$d) with filledcurves", col + 1, confCol + 1), color});
		sources.add(new String[] {file, String.format("1:%d with %s", col + 1, "lines"), color});
		return this;
	}

	public GNUPlot addWithConfidence(String file) {
		return addWithConfidence(file, 1, 2);
	}
	
	public GNUPlot withLogScale(String axes) {
		commands.add("set logscale " + axes);
		return this;
	}
	
	/*private int convertColumn(String file, String col) {
		if (col == null) return 2;
		if (col.matches("\\d+")) return Integer.parseInt(col);
		List<String> labels = getFirstDataLine(file + ".dat");
		int index = 2;
		for (int i = 0; i < labels.size(); i++)
			if (labels.get(i).contains(col)) index = i + 1;
		return index;
	}*/
	
	// STATIC HELPERS
	
	/**
	 * Returns the first line of a plot input data file, which usually contains axis labels
	 * @param filename name of the file in the output dir (without .txt extension)
	 **/
	protected static List<String> getFirstDataLine(String filename) {
		List<String> list = new ArrayList<String>();    
		String filepath = Config.get("OUTPUT_DIR") + "/" + filename + ".dat";
		try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {		
			String line = br.readLine(); 
			Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(line);
			while (m.find()) {
				list.add(m.group(1).replace("\"", ""));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	    return list;
	}
	
	protected void writeLineStylesAndPlotline() throws IOException {
		write("set style fill transparent solid 0.2 noborder");
		List<String> linestyles = IntStream.range(0, sources.size())
			.mapToObj(i -> "set style line " + (i+1) + " lc rgb '"
				+ sources.get(i)[2] + "' lt 1 lw 1.5")
			.collect(Collectors.toList());		
		for (String linestyle : linestyles)
			write(linestyle);
		write("plot " + IntStream.range(0, sources.size())
			.mapToObj(i -> "'" + sources.get(i)[0] + ".dat' u "
				+ sources.get(i)[1] + " ls " + (i+1))
			.collect(Collectors.joining(", ")));
	}
	
}
