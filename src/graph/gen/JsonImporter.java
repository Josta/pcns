package graph.gen;

import graph.Channel;
import graph.Graph;

import java.io.FileReader;
import java.util.HashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonImporter extends GraphGenerator {
	
	private String filename;
	
	public JsonImporter(String filename) {
		this.filename = filename;
	}

	@Override
	public Graph generate() {
		Graph g = new Graph();
		HashMap<String, Integer> pubkeymap = new HashMap<>();
		JsonObject root;
		try {
			root = new JsonParser().parse(new FileReader(filename)).getAsJsonObject();
		} catch (Exception e) {
			e.printStackTrace();
			return g;
		}
		
		JsonArray nodes = root.getAsJsonArray("nodes");
		g.setupNodes(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
        	JsonObject jnode = nodes.get(i).getAsJsonObject();
        	pubkeymap.put(jnode.get("pub_key").getAsString(), i);
        	//Node node = g.node(i);
        }
        
        JsonObject nullPolicy = new JsonParser().parse(
        		"{\"time_lock_delta\": 14,\"min_htlc\": \"0\",\"fee_base_msat\": \"1000\","
        		+ "\"fee_rate_milli_msat\": \"10\",\"disabled\": false,"
        		+ "\"max_htlc_msat\": \"297000000\"}").getAsJsonObject();
        
        JsonArray edges = root.getAsJsonArray("edges");
        for (int i = 0; i < edges.size(); i++) {
        	JsonObject jedge = edges.get(i).getAsJsonObject();
        	int capacity = jedge.get("capacity").getAsInt();
        	int node1 = pubkeymap.get(jedge.get("node1_pub").getAsString()),
        		node2 = pubkeymap.get(jedge.get("node2_pub").getAsString());
        	JsonElement p1e = jedge.get("node1_policy"),
        			p2e = jedge.get("node2_policy");
        	JsonObject p1 = (p1e.isJsonNull()) ? nullPolicy : p1e.getAsJsonObject(),
        			   p2 = (p2e.isJsonNull()) ? nullPolicy : p2e.getAsJsonObject();
        	Channel ch = g.newChannel(node1, node2, capacity / 2, capacity / 2);
        	ch.setFees(p1.get("fee_base_msat").getAsInt(),
        			p2.get("fee_base_msat").getAsInt(),
        			p1.get("fee_rate_milli_msat").getAsInt(),
        			p2.get("fee_rate_milli_msat").getAsInt());
        	ch.setTimelockDeltas(p1.get("time_lock_delta").getAsInt(),
        			p2.get("time_lock_delta").getAsInt());
        	ch.setHTLCAmountLimits(p1.get("min_htlc").getAsInt(),
        			p2.get("min_htlc").getAsInt(),
        			new Long((p1.get("max_htlc_msat").getAsLong())).intValue(),
        			new Long((p2.get("max_htlc_msat").getAsLong())).intValue());
        	ch.setDisabled(p1.get("disabled").getAsBoolean(),
        			p2.get("disabled").getAsBoolean());
        }
        
		return g;
	}
	
	@Override
	public String toString() {
		return String.format("JsonImporter(\"%s\")", filename);
	}

}
