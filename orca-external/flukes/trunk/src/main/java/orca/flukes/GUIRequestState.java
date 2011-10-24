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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import orca.flukes.ndl.RequestSaver;
import orca.flukes.ui.ChooserWithNewDialog;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.EditingModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.picking.PickedState;

/**
 * Singleton class that holds shared GUI request state. Since dialogs are all modal, no need for locking for now.
 * @author ibaldin
 *
 */
public class GUIRequestState extends GUICommonState implements IDeleteEdgeCallBack<OrcaLink>, IDeleteNodeCallBack<OrcaNode> {
	public static final String NO_GLOBAL_IMAGE = "None";
	public static final String NO_DOMAIN_SELECT = "System select";
	public static final String NODE_TYPE_SITE_DEFAULT = "Site default";
	public static final String NO_NODE_DEPS="No dependencies";
	private static GUIRequestState instance = null;
	
	// VM images defined by the user
	HashMap<String, OrcaImage> definedImages; 
	
	ChooserWithNewDialog<String> icd = null;
	ReservationDetailsDialog rdd = null;
	
	// are we adding a new image definition or editing existing
	boolean addingNewImage = false;

	// File in which we save
	File saveFile = null;
	
	// Reservation details
	private OrcaReservationTerm term;
	private String resImageName = null;
	private String resDomainName = null;
	
	private static void initialize() {
		;
	}
	
	private GUIRequestState() {
		term = new OrcaReservationTerm();
		definedImages = new HashMap<String, OrcaImage>();
		// Set some defaults for the Edges...
		linkCreator.setDefaultBandwidth(10000000);
		linkCreator.setDefaultLatency(5000);
	}
	
	public static GUIRequestState getInstance() {
		if (instance == null) {
			initialize();
			instance = new GUIRequestState();
		}
		return instance;
	}
	
	public void clear() {
		// clear the graph, reservation set else to defaults
		Set<OrcaNode> nodes = new HashSet<OrcaNode>(g.getVertices());
		for (OrcaNode n: nodes)
			g.removeVertex(n);
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
		Pair<OrcaNode> p = g.getEndpoints(e);
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
		Collection<OrcaLink> edges = g.getIncidentEdges(n);
		for (OrcaLink e: edges) {
			deleteEdgeCallBack(e);
		}
	}
	
	/**
	 * Return available domains
	 * @return
	 */
	public String[] getAvailableDomains() {
		List<String> knownDomains = new ArrayList(RequestSaver.domainMap.keySet());
		Collections.sort(knownDomains);
		
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
		Collection<OrcaNode> knownNodes = g.getVertices();
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
		
		for (OrcaNode n: g.getVertices()) {
			if (nm.equals(n.getName()))
				return n;
		}
		return null;
	}
	
	/**
	 * set the saved file object
	 * @param f
	 */
	public void setSaveFile(File f) {
		saveFile = f;
	}
	
	/**
	 * retrieve saved file object
	 * @param f
	 * @return
	 */
	public File getSaveFile() {
		return saveFile;
	}
	
	/**
	 * Request pane button actions
	 * @author ibaldin
	 *
	 */
	public class RequestButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("images")) {
				icd = new ImageChooserDialog(GUI.getInstance().getFrame());
				icd.pack();
				icd.setVisible(true);
			} else if (e.getActionCommand().equals("reservation")) {
				rdd = new ReservationDetailsDialog(GUI.getInstance().getFrame());
				rdd.setFields(getVMImageInReservation(), 
						getDomainInReservation(),
						getTerm());
				rdd.pack();
				rdd.setVisible(true);
			} else if (e.getActionCommand().equals("nodes")) {
				nodeCreator.setCurrent(OrcaNodeEnum.CE);
			} else if (e.getActionCommand().equals("nodegroups")) {
				nodeCreator.setCurrent(OrcaNodeEnum.NODEGROUP);
			}
		}
	}
	
	// we just need one action listener
	ActionListener al = new RequestButtonListener();
	public ActionListener getActionListener() {
		return al;
	}
	
	/**
	 * Initialize request pane 
	 */
	@Override
	public void addPane(Container c) {

		// Layout<V, E>, VisualizationViewer<V,E>
		//	        Map<OrcaNode,Point2D> vertexLocations = new HashMap<OrcaNode, Point2D>();
		
		Layout<OrcaNode, OrcaLink> layout = new FRLayout<OrcaNode, OrcaLink>(g);
		
		//layout.setSize(new Dimension(1000,800));
		vv = 
			new VisualizationViewer<OrcaNode,OrcaLink>(layout);
		// Show vertex and edge labels
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<OrcaNode>());
		vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<OrcaLink>());
		
		// Create a graph mouse and add it to the visualization viewer
		OrcaNode.OrcaNodeFactory onf = new OrcaNode.OrcaNodeFactory(nodeCreator);
		OrcaLink.OrcaLinkFactory olf = new OrcaLink.OrcaLinkFactory(linkCreator);
		gm = new EditingModalGraphMouse<OrcaNode, OrcaLink>(vv.getRenderContext(), 
				onf, olf);
		
		// add the plugin
		PopupVertexEdgeMenuMousePlugin<OrcaNode, OrcaLink> myPlugin = new PopupVertexEdgeMenuMousePlugin<OrcaNode, OrcaLink>();
		
		// Add some popup menus for the edges and vertices to our mouse plugin.
		myPlugin.setEdgePopup(new MouseMenus.RequestEdgeMenu());
		myPlugin.setVertexPopup(new MouseMenus.RequestNodeMenu());
		myPlugin.setModePopup(new MouseMenus.ModeMenu());
		gm.remove(gm.getPopupEditingPlugin());  // Removes the existing popup editing plugin
		gm.add(myPlugin);

		// Add icon and shape (so pickable areal roughly matches the icon) transformer
		OrcaNode.OrcaNodeIconShapeTransformer st = new OrcaNode.OrcaNodeIconShapeTransformer();
		vv.getRenderContext().setVertexShapeTransformer(st);
		
		OrcaNode.OrcaNodeIconTransformer it = new OrcaNode.OrcaNodeIconTransformer();
		vv.getRenderContext().setVertexIconTransformer(it);
		
		// add listener to add/remove checkmarks on selected nodes
		PickedState<OrcaNode> ps = vv.getPickedVertexState();
        ps.addItemListener(new OrcaNode.PickWithIconListener(it));
		
		vv.setGraphMouse(gm);

		vv.setLayout(new BorderLayout(0,0));
		
		c.add(vv);

		gm.setMode(ModalGraphMouse.Mode.EDITING); // Start off in editing mode  
	}
	
	
}
