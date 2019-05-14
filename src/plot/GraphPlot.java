package plot;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import graph.Channel;
import graph.Node;
import utility.global.Config;

public class GraphPlot extends AbstractPlot {

	private Set<Channel> markedChannels;
	private Set<Node> markedNodes;

	public GraphPlot(String name) {
		super(name, "gv");
		this.markedChannels = new HashSet<>();
		this.markedNodes = new HashSet<>();
	}

	@Override
	protected void create() throws IOException {
		write("graph G {");
		write("graph [pad=0.5, sep=\"+20\", esep=1, splines=false, overlap=false, overlap_scaling=6];");
		write("node [fontname=Arial, fontcolor=black, fontsize=12, margin=0, width=0.4, height=0.4, pad=5, style=filled];");
		write("edge [fontname=Helvetica, fontcolor=red, fontsize=8];");
		
		HashMap<String, String> props = new HashMap<>();
		
		for (Node n : graph().nodes()) {
			props.clear();
			switch (n.getRole()) {
				case Node.ROLE_BRIDGE: props.put("fillcolor", "red"); break; 
				case Node.ROLE_CONSUMER: props.put("fillcolor", "green"); break;
				case Node.ROLE_GATEWAY: props.put("fillcolor", "yellow"); break;
			}
			if (markedNodes.contains(n)) {
				props.put("shape", "doublecircle");
			}
			write(n.getID() + printProperties(props));
		}
		
		int minCapacity = Integer.MAX_VALUE;
		int maxCapacity = 0;
		for (Channel ch : sim.graph().channels()) {
			if (ch.getCapacity() < minCapacity)
				minCapacity = ch.getCapacity();
			if (ch.getCapacity() > maxCapacity)
				maxCapacity = ch.getCapacity();
		}
		
		for (Channel ch : sim.graph().channels()) {
			props.clear();		
			props.put("headlabel", "" + (ch.getCapacity2() / 1000));
			props.put("taillabel", "" + (ch.getCapacity1() / 1000));
			Color mix = mixColors(new Color(255, 0, 0), new Color(0, 255, 0), ch.getDisbalance());
			props.put("color", "#"+Integer.toHexString(mix.getRGB()).substring(2));
			double weight = 1 + 4 * (ch.getCapacity() - minCapacity) / (double) (maxCapacity - minCapacity);
			if (weight > 1.0001) {
				//props.put("penwidth", "" + weight);
			}
			if (markedChannels.contains(ch)) {
				props.put("color", props.get("color") + ":blue");
			}
			write(ch.getNode1() + " -- " + ch.getNode2() + printProperties(props));
		}
		write("}");
	}

	@Override
	protected void plot() throws IOException {
		String tool = (graph().size() > 100) ? "sfdp" : "neato";
		String ext = Config.getBoolean("WINDOWS") ? ".exe" : "";
		String binary = Config.get("GRAPHVIZ_PATH") + tool + ext;
		String args = "-Tpng";
		String command = binary + " " + args + " \"" + plotfile + "\" -o \"" + name + ".png\"";
		System.out.println(command);
		Process p = Runtime.getRuntime().exec(command, null, new File(Config.get("OUTPUT_DIR")));
		if (Config.getBoolean("GRAPHVIZ_PRINT_ERRORS")) {
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
		if (!Config.getBoolean("GRAPHVIZ_SAVE_PLOTFILES")) {
			new File(Config.get("OUTPUT_DIR") + "/" + plotfile).delete();
		}
	}

	public AbstractPlot markPath(int[] path) {	
		for (int x = 0; x < path.length - 1; x++) {
			markedChannels.add(sim.graph().channel(path[x], path[x+1]));
		}
		return this;
	}
	
	public AbstractPlot markTree(int[] tree) {
		for (int x = 0; x < tree.length; x++) {
			if (tree[x] >= 0) {
				markedChannels.add(sim.graph().channel(x, tree[x]));
			} else {
				markedNodes.add(sim.graph().node(x));
			}
		}
		return this;
	}
	
	private static Color mixColors(Color color1, Color color2, double percent){
	      double inverse_percent = 1.0 - percent;
	      int redPart = (int) (color1.getRed()*percent + color2.getRed()*inverse_percent);
	      int greenPart = (int) (color1.getGreen()*percent + color2.getGreen()*inverse_percent);
	      int bluePart = (int) (color1.getBlue()*percent + color2.getBlue()*inverse_percent);
	      return new Color(redPart, greenPart, bluePart);
	}
	
	private String printProperties(HashMap<String, String> props) {
		Stream<String> items = props.keySet().stream().map(key -> key + "=\"" + props.get(key) + "\"");
		return " [" + items.collect(Collectors.joining(", ")) + "];";
	}

}
