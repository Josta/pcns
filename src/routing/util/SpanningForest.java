package routing.util;

import java.util.ArrayList;

import utility.lib.Lists;

/**
 * A simple forest interface that allows to get paths between nodes and their roots.
 * The "forest" may or may not be a single tree.
 */
public class SpanningForest {
	
	protected int[] parent;
	
	public SpanningForest(int[] parents) {
		this.parent = parents;
	}
	
	public int[] getPathFrom(int node, boolean includeRoot) {
		ArrayList<Integer> path = new ArrayList<>();
		int n = node;
		while (n >= 0) {
			path.add(n);
			n = parent[n];
		}
		if (!includeRoot) {
			path.remove(path.size() - 1);
		}
		return Lists.array(path);
	}
	
	public int[] getPathTo(int node, boolean includeRoot) {
		int[] path = getPathFrom(node, includeRoot);
		return (path == null) ? null : Lists.revert(path);
	}
	
	public int getNextHop(int node) {
		return parent[node];
	}
	
	public int[] getForest() {
		return parent;
	}
	
}
