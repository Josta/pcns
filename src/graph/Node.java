package graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Node {

	public static final int ROLE_BRIDGE = 1, ROLE_GATEWAY = 2, ROLE_CONSUMER = 3;
	
	private int id, role;
	private ArrayList<Channel> channels;
	
	public Node(int id, int role) {
		this.id = id;
		this.role = role;
		this.channels = new ArrayList<Channel>(1);
	}
	
	public int getID() {
		return id;
	}

	public void addChannel(Channel channel) {
		channels.add(channel);
	}

	public List<Channel> channels() {
		return channels;
	}

	public int getDegree() {
		return channels.size();
	}
	
	public boolean hasRole(int role) {
		return (this.role == role);
	} 
	
	public int getRole() {
		return role;
	}
	
	public void setRole(int role) {
		this.role = role;
	}

	public Channel getChannelTo(int node) {
		for (Channel ch : channels) {
			if (ch.getOtherNode(id) == node)
				return ch;
		}
		return null;
	}

	public int[] neighbors() {
		return channels.stream().mapToInt(ch -> ch.getOtherNode(id)).toArray();
	}
	
	public List<Integer> neighborsList() {
		return channels.stream()
			.map(ch -> ch.getOtherNode(id))
			.collect(Collectors.toList());
	}

	public void setID(int id) {
		for (Channel ch : channels()) {
			if (ch.getNode1() == this.id) ch.setNode1(id);
			if (ch.getNode2() == this.id) ch.setNode2(id);
		}
		this.id = id;
	}

	public void removeChannel(Channel ch) {
		channels.remove(ch);
	}

	public void setChannels(Collection<Channel> channels) {
		this.channels = new ArrayList<>(channels);
	}
	
}
