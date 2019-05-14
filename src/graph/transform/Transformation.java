package graph.transform;

import core.Component;
import graph.Graph;

public abstract class Transformation extends Component {

	public abstract void transform(Graph g);
	
	public String getName() {
		return getClass().getSimpleName();
	}
}
