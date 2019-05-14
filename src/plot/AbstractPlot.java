package plot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import core.Component;
import utility.global.Config;

public abstract class AbstractPlot extends Component {
	
	protected String name;
	protected String plotfile;
	private Writer writer;
	private static final String NEW_LINE = System.getProperty("line.separator");
	
	public AbstractPlot(String name, String ext) {
		this.name = name;
		this.plotfile = name + "." + ext;
	}

	public void run() {
		File file = new File(Config.get("OUTPUT_DIR") + "/" + plotfile);
		file.getParentFile().mkdirs();
        try {      	
        	writer = new BufferedWriter(new FileWriter(file));
        	create();
        	writer.close();
			plot();	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected abstract void create() throws IOException;
	
	protected abstract void plot() throws IOException;
	
	protected void write(String line) throws IOException {
		writer.write(line + NEW_LINE);
	}
	
}
