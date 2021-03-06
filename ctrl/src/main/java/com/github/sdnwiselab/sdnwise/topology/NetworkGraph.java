/*
 * Copyright (C) 2015 SDN-WISE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.sdnwiselab.sdnwise.topology;

import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;
import com.github.sdnwiselab.sdnwise.packet.ReportPacket;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;

import gnu.io.RXTXCommDriver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Set;

import org.graphstream.algorithm.BetweennessCentrality;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;

/**
 * Holder of the {@code org.graphstream.graph.Graph} object which represent the
 * topology of the wireless sensor network. The method updateMap is invoked when
 * a message with topology updates is sent to the controller.
 *
 * @author Sebastiano Milardo
 */
public class NetworkGraph extends Observable {

    /**
     * Byte constants.
     */
    private static final int MAX_BYTE = 255;
    /**
     * Milliseconds in a second.
     */
    private static final long MILLIS_IN_SECOND = 1000L;
    /**
     * Timers.
     */
    private long lastCheck, lastModification;
    /**
     * TTL of a node. If a node is not sending a message for timeout seconds it
     * is removed from the network.
     */
    private final int timeout;
    /**
     * Represents the topology of the network.
     */
    protected final Graph graph;
    /**
     * If the absolute value of the difference between two successive
     * measurements of the rssi of a link is greater than this value, an event
     * is sent to the controller.
     */
    protected final int rssiResolution;

    /**
     * Creates the NetworkGraph object. It requires a time to live for each node
     * in the network and a value representing the RSSI resolution in order to
     * consider a change of the RSSI value a change in the network.
     *
     * @param ttl the time to live for a node in seconds
     * @param rssiRes the RSSI resolution
     */
    public NetworkGraph(final int ttl, final int rssiRes) {
        graph = new MultiGraph("SDN-WISE Network");
        lastModification = Long.MIN_VALUE;
        rssiResolution = rssiRes;
        timeout = ttl;
        lastCheck = System.currentTimeMillis();
        graph.setAutoCreate(true);
        graph.setStrict(false);
    }

    /**
     * Adds a edge directed edge between the two given nodes. If directed, the
     * edge goes in the 'from' 'to' direction.
     *
     * @param <T> Extends an edge
     * @param id Unique and arbitrary string identifying the edge.
     * @param from The first node identifier.
     * @param to The second node identifier.
     * @param directed Is the edge directed?
     * @return The newly created edge, an existing edge or {@code null}
     */
    public final <T extends Edge> T addEdge(final String id, final String from,
            final String to,
            final boolean directed) {
        return graph.addEdge(id, from, to, directed);
    }

    /**
     * Add a node in the graph. This acts as a factory, creating the node
     * instance automatically (and eventually using the node factory provided).
     *
     * @param <T> returns something that extends node
     * @param id Arbitrary and unique string identifying the node.
     * @return The created node (or the already existing node).
     */
    public final <T extends Node> T addNode(final String id) {
        return graph.addNode(id);
    }

    /**
     * Gets an Edge of the Graph.
     *
     * @param <T> the type of edge in the graph.
     * @param id string id value to get an Edge.
     * @return the edge of the graph
     */
    public final <T extends Edge> T getEdge(final String id) {
        return graph.getEdge(id);
    }

    /**
     * Gets the Graph contained in the NetworkGraph.
     *
     * @return returns a Graph object
     */
    public final Graph getGraph() {
        return graph;
    }

    /**
     * Returns the last time instant when the NetworkGraph was updated.
     *
     * @return a long representing the last time instant when the NetworkGraph
     * was updated
     */
    public final synchronized long getLastModification() {
        return lastModification;
    }

    /**
     * Gets a Node of the Graph.
     *
     * @param <T> the type of node in the graph.
     * @param id string id value to get a Node.
     * @return the node of the graph
     */
    public final <T extends Node> T getNode(final String id) {
        return graph.getNode(id);
    }

    /**
     * Removes an edge.
     *
     * @param <T> This method is implicitly generic and returns something which
     * extends Edge.
     * @param edge The edge to be removed
     * @return The removed edge
     */
    public final <T extends Edge> T removeEdge(final Edge edge) {
        return graph.removeEdge(edge);
    }

    /**
     * Removes a node.
     *
     * @param <T> This method is implicitly generic and returns something which
     * extends Node.
     * @param node The node to be removed
     * @return The removed edge
     */
    public final <T extends Node> T removeNode(final Node node) {
        return graph.removeNode(node);
    }

    /**
     * Setups an Edge.
     *
     * @param edge the edge to setup
     * @param newLen the weight of the edge
     */
    public void setupEdge(final Edge edge, final int newLen) {
        updateEdge(edge, newLen);
    }

    /**
     * Setups a Node.
     *
     * @param node the node to setup
     * @param batt residual charge of the node
     * @param now last time time the node was alive
     * @param net Node network id
     * @param addr Node address
     */
    public void setupNode(final Node node, final int batt, 
    		final double temperature, final double humidity, final double light1, final double light2, final int rxCount, final int txCount,
    		final long now,
            final int net, final NodeAddress addr) {
        node.addAttribute("battery", batt);
        node.addAttribute("temperature", temperature);
        node.addAttribute("humidity", humidity);
        node.addAttribute("light1", light1);
        node.addAttribute("light2", light2);
        node.addAttribute("rxcount", rxCount);
        node.addAttribute("txcount", txCount);
        node.addAttribute("lastSeen", now);
        node.addAttribute("net", net);
        node.addAttribute("nodeAddress", addr);
    }

    /**
     * Updates an existing Edge.
     *
     * @param edge the edge to setup
     * @param newLen the weight of the edge
     */
    public void updateEdge(final Edge edge, final int newLen) {
    	
    		// TODO some magic here
    		double cost = 0;
        try {
        	//	System.out.println(edge.getNode0().getId() + " -----> " + edge.getNode1().getId());
        //	System.out.println(edge.getNode0().getDegree() + edge.getNode1().getDegree());
        	
        		int totalTraffic = 0;
        		for(Node node : graph.getEachNode()) {
        			totalTraffic += (int)node.getAttribute("rxcount") + (int)node.getAttribute("txcount");
        		}
        	
        		int rxCountNode0 = edge.getNode0().getAttribute("rxcount");
        		int txCountNode0 = edge.getNode0().getAttribute("txcount");
        		int rxCountNode1 = edge.getNode0().getAttribute("rxcount");
        		int txCountNode1 = edge.getNode0().getAttribute("txcount");
        		
        		double trafficFraction0 = (double) ((rxCountNode0+txCountNode0)*1.0 / totalTraffic);
        		double trafficFraction1 = (double) ((rxCountNode1+txCountNode1)*1.0 / totalTraffic);
        		        		
        		BetweennessCentrality bcb = new BetweennessCentrality();
        		bcb.setUnweighted();
    
        		bcb.init(graph);
        		bcb.compute();
        		
        		double centrality = 0;
        		for(Node node : graph.getEachNode()) {
        			centrality += (double)node.getAttribute("Cb");
        		}
        		double node0Centrality = (double)edge.getNode0().getAttribute("Cb")/centrality;
        		double node1Centrality = (double)edge.getNode1().getAttribute("Cb")/centrality;

        		cost = (trafficFraction0+trafficFraction1+node0Centrality+node1Centrality)/4;
        		
        		System.out.println("Cost: " + cost);
        		if(cost < 0) {
        			cost = 0;
        		}
        } catch(Exception e) {
        		System.out.println(e);
        }
    	
        edge.addAttribute("length", cost);
    }

    /**
     * Invoked when a message with topology updates is received by the
     * controller. It updates the network topology according to the message and
     * checks if all the nodes in the network are still alive.
     *
     * @param packet the NetworkPacket received
     */
    public final synchronized void updateMap(final ReportPacket packet) {

        long now = System.currentTimeMillis();
        boolean modified = checkConsistency(now);

        int net = packet.getNet();
        int batt = packet.getBattery();
        double temperature = packet.getTemperatureAsDouble();
        double humidity = packet.getHumidityAsDouble();
        double light1 = packet.getLight1AsDouble();
        double light2 = packet.getLight2AsDouble();
        
        int rxCount = 0;
        int txCount = 0;
        for(int i=0; i<packet.getNeigborsSize(); i++) {
        		rxCount += packet.getRxCount(i);
        		txCount += packet.getTxCount(i);
        }
       
        String nodeId = packet.getSrc().toString();
        String fullNodeId = net + "." + nodeId;
        NodeAddress addr = packet.getSrc();

        Node node = getNode(fullNodeId);

        if (node == null) {
            node = addNode(fullNodeId);
            setupNode(node, batt, temperature, humidity, light1, light2, rxCount, txCount, now, net, addr);

            for (int i = 0; i < packet.getNeigborsSize(); i++) {
                NodeAddress otheraddr = packet.getNeighborAddress(i);
                String other = net + "." + otheraddr.toString();
                if (getNode(other) == null) {
                    Node tmp = addNode(other);
                    setupNode(tmp, 0, 0, 0, 0, 0, packet.getTxCount(i), packet.getRxCount(i), now, net, otheraddr);
                }

                int newLen = MAX_BYTE - packet.getLinkQuality(i);
                String edgeId = other + "-" + fullNodeId;
                Edge edge = addEdge(edgeId, other, node.getId(), true);
                
                // TODO rxCount + txcount of both adjacent nodes
                setupEdge(edge, newLen);
            }
            modified = true;

        } else {
            updateNode(node, batt, temperature, humidity, light1, light2, rxCount, txCount, now);
            Set<Edge> oldEdges = new HashSet<>();
            oldEdges.addAll(node.getEnteringEdgeSet());

            for (int i = 0; i < packet.getNeigborsSize(); i++) {
                NodeAddress otheraddr = packet.getNeighborAddress(i);
                String other = net + "." + otheraddr.toString();
                if (getNode(other) == null) {
                    Node tmp = addNode(other);
                    setupNode(tmp, 0, 0, 0, 0, 0, packet.getTxCount(i), packet.getRxCount(i), now, net, otheraddr);
                }

                int newLen = MAX_BYTE - packet.getLinkQuality(i);

                String edgeId = other + "-" + fullNodeId;
                Edge edge = getEdge(edgeId);

                if (edge != null) {

                   
                    oldEdges.remove(edge);
                   // updateEdge(edge, 1);
//                    double oldLen = edge.getAttribute("length");
//                    if (Math.abs(oldLen - newLen) > rssiResolution) {
//                    	// TODO rxCount + txcount of both adjacent nodes
                        updateEdge(edge, newLen);
                        modified = true;
//                    }
                } else {
                    Edge tmp = addEdge(edgeId, other, node.getId(), true);
                    setupEdge(tmp, newLen);
                    modified = true;
                }
            }

            if (!oldEdges.isEmpty()) {
                oldEdges.stream().forEach((e) -> {
                    removeEdge(e);
                });
                modified = true;
            }
        }

        if (modified) {
            lastModification++;
            setChanged();
            notifyObservers();
        }
    }

    /**
     * Updates a existing Node.
     *
     * @param node the node to setup
     * @param batt residual charge of the node
     * @param now last time time the node was alive
     */
    public void updateNode(final Node node, final int batt, final double temperature, final double humidity, final double light1, final double light2, final int rxCount, final int txCount, final long now) {
        node.addAttribute("battery", batt);
        node.addAttribute("lastSeen", now);
        node.addAttribute("temperature", temperature);
        node.addAttribute("humidity", humidity);
        node.addAttribute("light1", light1);
        node.addAttribute("light2", light2);
        node.addAttribute("rxcount", rxCount);
        node.addAttribute("txcount", txCount);
    }

    /**
     * Checks if there were modifications in the graph. This method deletes dead
     * nodes.
     *
     * @param now actual time in milliseconds
     * @return true if the graph was changed, false otherwise
     */
    private boolean checkConsistency(final long now) {
        boolean modified = false;
        if (now - lastCheck > (timeout * MILLIS_IN_SECOND)) {
            lastCheck = now;
            for (Node n : graph) {
                if (n.getAttribute("net", Integer.class) < NetworkPacket.THRES
                        && n.getAttribute("lastSeen", Long.class) != null
                        && !isAlive(timeout, (long) n.getNumber("lastSeen"),
                                now)) {
                    removeNode(n);
                    modified = true;
                }

            }
        }
        return modified;
    }

    /**
     * Checks if a node is still alive.
     *
     * @param thrs if the node is not responding for more than thrs seconds is
     * deleted
     * @param last last time the graph was updated
     * @param now actual time in milliseconds
     * @return true if the node is still alive, false otherwise
     */
    private boolean isAlive(final long thrs, final long last, final long now) {
        return ((now - last) < thrs * MILLIS_IN_SECOND);
    }

}
