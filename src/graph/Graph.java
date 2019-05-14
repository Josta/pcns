package graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A graph with nodes and channels (bidirectional edges).
 * @author Josua
 */
public class Graph {

	protected ArrayList<Node> nodesArr;
	protected ArrayList<Channel> channelsArr;
	//	private LinkedList<IntConsumer> nodeListeners;
	//	private LinkedList<IntConsumer> channelListeners;
	private LinkedList<Consumer<Channel>> channelUpdateListeners;
	
	
	/** Creates an empty graph. **/
	public Graph() {
		nodesArr = new ArrayList<Node>();
		channelsArr = new ArrayList<Channel>();
		channelUpdateListeners = new LinkedList<Consumer<Channel>>();
		//		nodeListeners = new LinkedList<IntConsumer>();
		//		channelListeners = new LinkedList<IntConsumer>();
		
	}
	
	/** Reduces memory overhead of the graph. **/
	public void optimize() {
		nodesArr.trimToSize();
		channelsArr.trimToSize();
	}

	
	
	// GETTERS
	
	public Node node(int id) {
		return nodesArr.get(id);
	}

	public ArrayList<Node> nodes() {
		return nodesArr;
	}
	
	public ArrayList<Node> nodesWithRole(int role) {
		return nodesArr.stream().filter(n -> n.getRole() == role)
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	public int size() {
		return nodesArr.size();
	}

	public Channel channel(int id) {
		return channelsArr.get(id);
	}
	
	public Channel channel(int node1, int node2) {
		return node(node1).getChannelTo(node2);
	}
	
	public ArrayList<Channel> channels() {
		return channelsArr;
	}
	
	
	// GRAPH CREATION / MODIFICATION
	
	/** Convenience method to generate a number of of Nodes. **/
	public void setupNodes(int count) {
		nodesArr = new ArrayList<Node>(count);
		for (int i = 0; i < count; i++) {
			newNode();
		}
	}
	
	/** Creates a new node. **/
	public Node newNode() {
		return newNode(Node.ROLE_BRIDGE);
	}
	
	/** Creates a new node. **/
	public Node newNode(int role) {
		Node node = new Node(nodesArr.size(), role);
		nodesArr.add(node);
		return node;
	}
	
	/** Creates and links a (zero-capacity) channel between two nodes. **/
	public Channel newChannel(int node1, int node2) {
		return newChannel(node1, node2, 0, 0);
	}
	
	/** Creates and links a channel between two nodes, also adding a capacity. **/
	public Channel newChannel(int node1, int node2, int capacity1, int capacity2) {
		Channel c = new Channel(channelsArr.size(), node1, node2, capacity1, capacity2);
		channelsArr.add(c);
		nodesArr.get(node1).addChannel(c);
		nodesArr.get(node2).addChannel(c);
		return c;
	}
	
	public void removeNodes(Collection<Node> nodes) {
		removeChannels(nodes.stream()
			.flatMap(n -> n.channels().stream())
			.distinct().collect(Collectors.toList()));
		nodesArr.removeAll(nodes);	
		for (int i = 0; i < size(); i++)
			node(i).setID(i);
	}
	
	public void removeChannels(Collection<Channel> channels) {
		channels.stream().forEach(ch -> {
			node(ch.getNode1()).removeChannel(ch);
			node(ch.getNode2()).removeChannel(ch);
		});
		channelsArr.removeAll(channels);
		for (int id = 0; id < channelsArr.size(); id++) {
			channel(id).setID(id);
		}
	}
	
	// GRAPH OBSERVATION
	
	public void onChannelUpdate(Consumer<Channel> listener) {
		channelUpdateListeners.add(listener);
	}
	
	public void channelUpdated(Channel channel) {
		channelUpdateListeners.forEach(l -> l.accept(channel));
	}
	
}
