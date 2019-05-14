package routing.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import core.Simulation;
import core.event.Event;
import graph.Channel;
import graph.Graph;
import routing.costs.RoutingCosts;
import utility.lib.Lists;

/**
 * Creates a minimal spanning tree with the GHS distributed algorithm.
 * 
 * For details see "A distributed algorithm for minimum weight spanning trees"
 * by Gallager et al. (Guy Flysher and Amir Rubinshtein's version)
 * 
 * @author Josua
 */
public class GHS {

	private Simulation sim;
	private Graph g;
	private RoutingCosts costs;
	private LinkedList<Runnable> finishedListeners;
	
	/** Possible edge states **/
	private final byte BASIC = 0, BRANCH = 1, REJECTED = 2;
	
	/** State of each edge (as seen by node1/node2), one of BASIC, BRANCH, REJECTED **/
	private byte[] edgeStatus1, edgeStatus2;
	
	/** GHS algorithm instances (one per node). */
	private ArrayList<GHSInstance> nodes;
	private boolean finished;

	public GHS(Simulation sim) {
		this.sim = sim;
		this.g = sim.graph();
		this.finished = false;
		this.finishedListeners = new LinkedList<>();
	}
	
	/**
	 * Starts the GHS distributed MST algorithm.
	 */
	public void init(RoutingCosts costs) {
		this.costs = costs;
		this.edgeStatus1 = Lists.initByteArray(g.channels().size(), BASIC);
		this.edgeStatus2 = Lists.initByteArray(g.channels().size(), BASIC);	
		this.nodes = Lists.initArray(g.size(), (id) -> new GHSInstance(id));
		nodes.get(0).wakeup();
	}

	/**
	 * The given procedure is being run when the GHS algorithm has finished.
	 * @param listener
	 */
	public void addFinishedListener(Runnable listener) {
		finishedListeners.add(listener);
	}
	
	
	private void callFinishedListenersOnce() {
		if (!finished) {
			finished = true;
			finishedListeners.stream().forEach(listener -> listener.run());
		}	
	}
	
	/**
	 * Returns the algorithm result. Call this only after the algorithm has finished.
	 * @return
	 */
	public SpanningForest getMST() {
		int[] parents = new int[nodes.size()];
		for (GHSInstance node : nodes) {
			parents[node.id] = (node.parentEdge < 0) ? -1 :node.getTarget(node.parentEdge);
		}
		return new SpanningForest(parents);
	}
	
	
	/**
	 * Distributed algorithm instance (one per node)
	 */
	private class GHSInstance {		
		
		/** Possible node states **/
		private final byte SLEEPING = 0, FOUND = 1, FIND = 2;		
		/** Special value for the edge fields below. **/
		private final byte NONE = -1;	
		
		/** Node ID **/
		private int id;
		/** Level of this node's fragment **/
		protected int LN;
		/** Name of this node's fragment **/
	    protected int FN;
	    /** Status of this node (SLEEPING, FOUND or FIND) **/
	    protected byte SN;
	    /** Parent edge (in_branch) **/
	    protected int parentEdge;
	    /** Edge currently tested for "outgoing" property **/
	    protected int testEdge;
	    /** MWOE (min-weight outgoing edge) candidate **/
	    protected int bestEdge;
	    /** Number of report messages expected from children **/
	    protected int pendingReports;
	    
	    /** Local message queue **/
	    protected Queue<Event> message_queue;
	    
	    
	    
	    public GHSInstance(int id) {
	        this.id = id;
	        this.bestEdge = NONE;
	        this.SN = SLEEPING;
	        this.message_queue = new LinkedList<>();
	    }
	    
	    
	    
	    // CONVENIENCE ACCESS METHODS
	    
	    private byte getEdgeStatus(int edge) {
	    	Channel ch = g.channel(edge);
	    	return (id == ch.getNode1()) ? edgeStatus1[edge] : edgeStatus2[edge];
	    }
	    
	    private void setEdgeStatus(int edge, byte status) {
	    	Channel ch = g.channel(edge);
	    	if (id == ch.getNode1()) {
	    		edgeStatus1[edge] = status;
	    	} else {
	    		edgeStatus2[edge] = status;
	    	}
	    }
	    
	    /**
	     * Retrieves the channel destination node.
	     * @param edge
	     * @return
	     */
	    private int getTarget(int edge) {
	    	return g.channel(edge).getOtherNode(id);
	    }
	    
	    /**
		 * Retrieves the weight of an edge.
		 * Since weights have to be distinct for the GHS algorithm,
		 * it concatenates the channel standard costs and the channel ID.
		 * @param edge ID
		 * @return
		 */
		public int getWeight(int id) {
			if (id < 0) {
				return Integer.MAX_VALUE;
			}
			long cost = costs.getCosts(g.channel(id), id);
			int idspace = (int)(Math.log10(g.channels().size())+1);
			return Integer.parseInt(String.format("%d%0" + idspace + "d", cost, id));
		}
	    
	    /**
	     * Retrieves the basic adjacent edge with the minimum weight.
	     * @return
	     */
	    private int getMinBasicEdge() {
	    	int minEdge = NONE;
	    	int minWeight = Integer.MAX_VALUE;
	    	for (Channel ch : g.node(id).channels()) {
	    		if ((getEdgeStatus(ch.getID()) == BASIC) && (getWeight(ch.getID()) < minWeight)) {
	    			minEdge = ch.getID();
	    			minWeight = getWeight(minEdge);
	    		}
	    	}
	    	return minEdge;
	    }
	    
	    
	    
	    // PROCEDURES (SEE PAPER)
	    
	    /** Create a L0 fragment and connect it to the nearest neighbor. **/
	    private void wakeup() {
	    	LN = 0;
	        SN = FOUND;
	        pendingReports = 0;
	        int minEdge = getMinBasicEdge();
    		setEdgeStatus(minEdge, BRANCH);
    		sendConnection(minEdge, 0);
	    }
	    
	    /** Look for the fragment's MWOE. **/
	    private void testNextEdge() {
	    	checkQueue();
	    	int minEdge = getMinBasicEdge();
	        if (minEdge != NONE) {
	            testEdge = minEdge;
	            sendTestQuery(testEdge, LN, FN);
	        } else {
	            testEdge = NONE;
	            report();
	        }
	    }
	   
	    /** Report MWOE towards core node (if all child reports arrived). **/
	    private void report() {
	    	if (pendingReports == 0 && testEdge == NONE) {
	            SN = FOUND;
	            sendReport(parentEdge, getWeight(bestEdge));
	            checkQueue();
	        }
	    }
	    
	    private void checkQueue() {
	    	int size = message_queue.size();
	    	for (int i = 0; i < size; i++) {
	    		message_queue.poll().run();
	    	}
	    }

	    
	    // MESSAGE HELPERS
	    
	    private void sendConnection(int edge, int level) {
	    	nodes.get(getTarget(edge)).new Connection(edge, level).after(0.01f, sim);
	    }
	    private void sendInitialization(int edge, int level, int name, byte state) {
	    	nodes.get(getTarget(edge)).new Initialization(edge, level, name, state).after(0.01f, sim);
	    }   
	    private void sendTestQuery(int edge, int level, int name) {
	    	nodes.get(getTarget(edge)).new TestQuery(edge, level, name).after(0.01f, sim);
	    }
	    private void sendTestAnswer(int edge, boolean accept) {
	    	nodes.get(getTarget(edge)).new TestAnswer(edge, accept).after(0.01f, sim);
	    }    
	    private void sendReport(int edge, int best_wt) {
	    	nodes.get(getTarget(edge)).new Report(edge, best_wt).after(0.01f, sim);   
	    }
	    private void sendCoreChange(int edge) {
	    	nodes.get(getTarget(edge)).new CoreChange().after(0.01f, sim);
	    }
	    private void sendTermination(int edge) {
	    	nodes.get(getTarget(edge)).new Termination().after(0.01f, sim);
	    }
	    
	    
	    // MESSAGES (SEE PAPER)
	    
	    /** Connects two fragments **/
	    private class Connection extends Event {
			private int edge, senderLevel;
			public Connection(int edge, int senderLevel) {
				this.edge = edge;
				this.senderLevel = senderLevel;
			}
			@Override
			public void run() {
				if (SN == SLEEPING) {
					wakeup();
		        }
		        if (senderLevel < LN) {
		        	//System.out.println("Absorb " + getTarget(edge) +"(F"+nodes.get(getTarget(edge)).FN+", L"+senderLevel+")" + " into " + id + "(F"+FN+", L"+LN+")");
		        	// absorb smaller fragment
		        	setEdgeStatus(edge, BRANCH);
		            sendInitialization(edge, LN, FN, SN);
		            if (SN == FIND) {
		                pendingReports++;
		            }
		        } else if (getEdgeStatus(edge) == BASIC) {
	            	// core must be a BRANCH => try again later
	            	message_queue.add(this);
	            } else {
	            	// same-level fragments => merge to level+1 fragment
	            	//System.out.println("L"+LN+" merge of " + getTarget(edge) + " and " + id + " => F" + getWeight(edge));
	            	sendInitialization(edge, LN + 1, getWeight(edge), FIND);
	            }
		        checkQueue();
			}
		}
		
	    /** Propagates merger info (if state is FIND: also inits MWOE search) **/
	    private class Initialization extends Event {
		    private int newLevel, newName, edge;
		    private byte state;
		    public Initialization(int edge, int newLevel, int newName, byte state) {
		        this.edge = edge;
		        this.newLevel = newLevel;
		        this.newName = newName;
		        this.state = state;
		    }    
		    @Override
		    public void run() {  	
		    	// set node info (to new fragment)
		    	LN = newLevel;
		    	FN = newName;
		        SN = state;
		        parentEdge = edge;
		        checkQueue();
		        bestEdge = NONE;
		        // flood merging info along this fragment's MST
		        // (this also absorbs any same-level fragments that was asking to be connected to this fragment)
		        for (Channel ch : sim.graph().node(id).channels()) {
		        	int eid = ch.getID();
		            if (eid != edge && getEdgeStatus(eid) == BRANCH) {
		            	sendInitialization(eid, newLevel, newName, state);
		                if (state == FIND) {
		                    pendingReports++;
		                }
		            }
		        }  
		        if (state == FIND) {
		            testNextEdge();
		        }
		        checkQueue();
		    }
		}
		
	    /** Tests whether the edge is outgoing. Triggers MWOE reports towards core node. **/
	    private class TestQuery extends Event {
		    private int edge, fragLevel, fragName;    
		    public TestQuery(int edge, int fragLevel, int fragName) {
		        this.edge = edge;
		        this.fragLevel = fragLevel;
		        this.fragName = fragName;
		    }
		    @Override
		    public void run() {
		        if (SN == SLEEPING) {
		            wakeup();
		        }
		        if (fragLevel > LN) {
		        	// fragment level too low => try again later
		            message_queue.add(this);
		        } else if (fragName != FN) {
		        	// different fragments => outgoing edge
	                sendTestAnswer(edge, true);
	            } else {
	            	// same fragment => not an outgoing edge
	                if (getEdgeStatus(edge) == BASIC) {
	                	setEdgeStatus(edge, REJECTED);
	                }
	                // don't answer if we already sent a test over this very edge (redundant)
	                if (testEdge != edge) {
	                    sendTestAnswer(edge, false);
	                } else {
	                	// shortcut: test next edge (of our own test)
	                    testNextEdge();
	                }
	            }
		   }
		}
		
	    /** This adjacient edge is an outgoing fragment edge. **/
	    private class TestAnswer extends Event {
		    private int edge;   
		    private boolean accept;
		    public TestAnswer(int edge, boolean accept) {
		        this.edge = edge;
		        this.accept = accept;
		    } 
		    @Override
		    public void run() {
		    	if (accept) {
		    		testEdge = NONE;
			        // remember edge if it's lighter than previously found ones
			        if (getWeight(edge) < getWeight(bestEdge)) {
			            bestEdge = edge;
			        }
			        report();
		    	} else {
		    		// mark edge as non-MST, test next edge
			        if (getEdgeStatus(edge) == BASIC) {
			        	setEdgeStatus(edge, REJECTED);
			        }
			        testNextEdge();
		    	}
		    	checkQueue();
		    }
		}
	    
	    /** Integrates child report (and possibly triggers parent report).
	     *  At the core, new connections or termination can be triggered. **/
	    private class Report extends Event {		    
		    private int edge, reportedWeight; 
		    public Report(int edge, int reportedWeight) {
		        this.edge = edge;
		        this.reportedWeight = reportedWeight;
		    }
		    @Override
		    public void run() {		
		    	checkQueue();
		    	if (edge != parentEdge) {
		    		// integrate report (one per child)
		            pendingReports--;
		            if (reportedWeight < getWeight(bestEdge)) {
		            	bestEdge = edge;
		            }
		            // if complete, send aggregated report to parent
		            report();
		        } else if (SN == FIND) {
		        	// inter core node report only after FIND phase => try again later
		        	message_queue.add(this);
	            } else if (reportedWeight > getWeight(bestEdge)) {
	            	// FIND phase over, but core unbalanced => move core
	            	new CoreChange().run();
	            } else if (reportedWeight == getWeight(bestEdge) && reportedWeight == Integer.MAX_VALUE) {
	            	// FIND phase over, core balanced => we're done
	            	new Termination().run();
	            	parentEdge = -1;
	            	callFinishedListenersOnce();
	            }
		    }
		}
		
	    /** Connects to the fragment behind the MWOE (triggered at a core node) **/
	    private class CoreChange extends Event {
		    @Override
		    public void run() {		    	
				if(getEdgeStatus(bestEdge) == BRANCH) {
					// pass move-core message to MWOE
				    sendCoreChange(bestEdge);
				} else {
					// we reached the MWOE => connect to new fragment => new core
				    sendConnection(bestEdge, LN);
				    setEdgeStatus(bestEdge, BRANCH);
				}
				checkQueue();
		    }
		}
		
	    /** Propagates the terminate message (triggered at a core node). **/
	    private class Termination extends Event {
			 @Override
			 public void run() {
				for (Channel ch : sim.graph().node(id).channels()) {
					if (ch.getID() != parentEdge && getEdgeStatus(ch.getID()) == BRANCH) {
						sendTermination(ch.getID());
					}
				}
				checkQueue();
			}
		}
		
	}

}
