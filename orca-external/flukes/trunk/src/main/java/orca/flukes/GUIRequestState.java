/*
* Copyright (c) 2011 RENCI/UNC Chapel Hill 
*
* @author Ilia Baldine
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
* and/or hardware specification (the "Work") to deal in the Work without restriction, including 
* without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
* sell copies of the Work, and to permit persons to whom the Work is furnished to do so, subject to 
* the following conditions:  
* The above copyright notice and this permission notice shall be included in all copies or 
* substantial portions of the Work.  
*
* THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
* OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS 
* IN THE WORK.
*/

package orca.flukes;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import orca.flukes.ui.ChooserWithNewDialog;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.EditingModalGraphMouse;

/**
 * Singleton class that holds shared GUI request state. Since dialogs are all modal, no need for locking for now.
 * @author ibaldin
 *
 */
public class GUIRequestState implements IDeleteEdgeCallBack<OrcaLink>, IDeleteNodeCallBack<OrcaNode>, OrcaNode.INodeCreator {
	public static final String NO_GLOBAL_IMAGE = "None";
	public static final String NO_DOMAIN_SELECT = "System select";
	public static final String NODE_TYPE_SITE_DEFAULT = "Site default";
	public static final String NO_NODE_DEPS="No dependencies";
	public static final String NODE_ICON = "node-50.gif";
	public static final String CLOUD_ICON = "cloud-50.gif";
	public static final String XCON_ICON = "crossconnect-50.gif";
	
    private static int nodeCount = 0;
    private static int clusterCount = 0;
	
	private static GUIRequestState instance = null;
	
	// VM images defined by the user
	HashMap<String, OrcaImage> definedImages = new HashMap<String, OrcaImage>();
	
	ChooserWithNewDialog<String> icd = null;
	ReservationDetailsDialog rdd = null;
	
	// are we adding a new image definition or editing existing
	boolean addingNewImage = false;
	
	// The graph objects
	SparseMultigraph<OrcaNode, OrcaLink> requestGraph = null;
	// Vis viewer for request
	VisualizationViewer<OrcaNode,OrcaLink> vv = null;
	// File in which we save
	File saveFile = null;
	// Mouse 
	EditingModalGraphMouse<OrcaNode, OrcaLink> gm;
	
	// Reservation details
	private OrcaReservationTerm term;
	private String resImageName = null;
	private String resDomainName = null;
	
	// true for nodes, false for clusters
	boolean nodesOrGroups = true;
	
	private static void initialize() {
		;
	}
	
	private GUIRequestState() {
		term = new OrcaReservationTerm();
	}
	
	static GUIRequestState getInstance() {
		if (instance == null) {
			initialize();
			instance = new GUIRequestState();
		}
		return instance;
	}
	
	public void clear() {
		// clear the graph, reservation set else to defaults
		Set<OrcaNode> nodes = new HashSet<OrcaNode>(requestGraph.getVertices());
		for (OrcaNode n: nodes)
			requestGraph.removeVertex(n);
		resImageName = null;
		resDomainName = null;
		term = new OrcaReservationTerm();
		addingNewImage = false;
		definedImages = new HashMap<String, OrcaImage>();
	}
	
	public OrcaReservationTerm getTerm() {
		return term;
	}
	
	public void setTerm(OrcaReservationTerm t) {
		term = t;
	}
	
	public void setVMImageInReservation(String im) {
		// if the value is changing
		// set it for all nodes
		if ((resImageName == null) && (im == null))
			return;
		if ((resImageName != null) && (resImageName.equals(im)))
			return;
		for (OrcaNode n: requestGraph.getVertices()) 
			n.setImage(im);
		resImageName = im;
	}
	
	public String getVMImageInReservation() {
		return resImageName;
	}
	
	public void setDomainInReservation(String d) {
		// if the value is changing
		// set it for all nodes
		if ((resDomainName == null) && ( d == null))
			return;
		if ((resDomainName != null) && (resDomainName.equals(d)))
			return;
		for (OrcaNode n: requestGraph.getVertices()) 
			n.setDomain(d);
		resDomainName = d;
	}
	
	public String getDomainInReservation() {
		return resDomainName;
	}
	
	public OrcaImage getImageByName(String nm) {
		return definedImages.get(nm);
	}
	
	public void addImage(OrcaImage newIm, OrcaImage oldIm) {
		if (newIm == null)
			return;
		// if old image is not null, then we are replacing, so delete first
		if (oldIm != null)
			definedImages.remove(oldIm.getShortName());
		definedImages.put(newIm.getShortName(), newIm);
	}
	
	public Object[] getImageShortNames() {
		if (definedImages.size() > 0)
			return definedImages.keySet().toArray();
		else return new String[0];
	}
	
	public String[] getImageShortNamesWithNone() {
		String[] fa = new String[definedImages.size() + 1];
		fa[0] = NO_GLOBAL_IMAGE;
		System.arraycopy(getImageShortNames(), 0, fa, 1, definedImages.size());
		return fa;		
	}
	
	public Iterator<String> getImageShortNamesIterator() {
		return definedImages.keySet().iterator();
	}
	
	/**
	 * Cleanup before deleting an edge
	 * @param e
	 */
	public void deleteEdgeCallBack(OrcaLink e) {
		if (e == null)
			return;
		// remove edge from node IP maps
		Pair<OrcaNode> p = requestGraph.getEndpoints(e);
		p.getFirst().removeIp(e);
		p.getSecond().removeIp(e);
	}

	/**
	 * cleanup before deleting a node
	 */
	public void deleteNodeCallBack(OrcaNode n) {
		if (n == null)
			return;
		// remove incident edges
		Collection<OrcaLink> edges = requestGraph.getIncidentEdges(n);
		for (OrcaLink e: edges) {
			deleteEdgeCallBack(e);
		}
	}
	
	/**
	 * Check if the link name is unique
	 * @param nm
	 * @return
	 */
	public boolean checkUniqueLinkName(OrcaLink edge, String nm) {
		// check all edges in graph
		Collection<OrcaLink> edges = requestGraph.getEdges();
		for (OrcaLink e: edges) {
			// check that some other edge doesn't have this name
			if (edge != null) {
				if ((e != edge) &&(e.getName().equals(nm)))
					return false;
			} else
				if (e.getName().equals(nm))
					return false;
		}
		return true;
	}
	
	/**
	 * check if node name is unique. exclude a node if needed (or null)
	 * @param node
	 * @param nm
	 * @return
	 */
	public boolean checkUniqueNodeName(OrcaNode node, String nm) {
		// check all edges in graph
		Collection<OrcaNode> nodes = requestGraph.getVertices();
		for (OrcaNode n: nodes) {
			// check that some other edge doesn't have this name
			if (node != null) {
				if ((n != node) &&(n.getName().equals(nm)))
					return false;
			} else
				if (n.getName().equals(nm))
					return false;
			
		}
		return true;
	}
	
	/**
	 * Return available domains
	 * @return
	 */
	public String[] getAvailableDomains() {
		Set<String> knownDomains = RequestSaver.domainMap.keySet();
		
		String[] itemList = new String[knownDomains.size() + 1];
		
		int index = 0;
		itemList[index] = NO_DOMAIN_SELECT;
		
		for(String s: knownDomains) {
			itemList[++index] = s;
		}
		
		return itemList;
	}
	
	/**
	 * Return null if 'None' image is asked for
	 * @param n
	 * @param image
	 */
	public static String getNodeImageProper(String image) {
		if ((image == null) || image.equals(NO_GLOBAL_IMAGE))
			return null;
		else
			return image;
	}
	
	/**
	 * Return null if 'System select' domain is asked for
	 * 
	 */
	public static String getNodeDomainProper(String domain) {
		if ((domain == null) || domain.equals(NO_DOMAIN_SELECT))
			return null;
		else
			return domain;
	}
	
	public static String getNodeTypeProper(String nodeType) {
		if ((nodeType == null) || nodeType.equals(NODE_TYPE_SITE_DEFAULT))
			return null;
		else
			return nodeType;
	}
	
	public String[] getAvailableNodeTypes() {
		Set<String> knownTypes = RequestSaver.nodeTypes.keySet();
		
		String[] itemList = new String[knownTypes.size() + 1];
		
		int index = 0;
		itemList[index] = NODE_TYPE_SITE_DEFAULT;
		for (String s: knownTypes) {
			itemList[++index] = s;
		}
		
		return itemList;
	}
	
	public String[] getAvailableDependencies(OrcaNode subject) {
		Collection<OrcaNode> knownNodes = requestGraph.getVertices();
		String[] ret = new String[knownNodes.size() - 1];
		int i = 0;
		for (OrcaNode n: knownNodes) {
			if (!n.equals(subject)) {
				ret[i] = n.getName();
				i++;
			}
		}
		return ret;
	}
	
	public OrcaNode getNodeByName(String nm) {
		if (nm == null)
			return null;
		
		for (OrcaNode n: requestGraph.getVertices()) {
			if (nm.equals(n.getName()))
				return n;
		}
		return null;
	}

	public OrcaNode create() {
		OrcaNode node;
		String name;
		do {
			if (nodesOrGroups) {
				name = "Node" + nodeCount++;
				node = new OrcaNode(name);
			}
			else {
				name = "NodeGroup" + clusterCount++;
				node = new OrcaNodeGroup(name);
			}
		} while (!checkUniqueNodeName(null, name));
		return node;
	}
}
