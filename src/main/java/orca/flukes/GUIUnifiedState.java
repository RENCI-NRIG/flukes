package orca.flukes;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.swing.Icon;

import orca.flukes.GUI.GuiTabs;
import orca.flukes.GUI.PrefsEnum;
import orca.flukes.OrcaNode.OrcaNodeIconTransformer;
import orca.flukes.OrcaResource.ResourceType;
import orca.flukes.irods.IRodsException;
import orca.flukes.irods.IRodsICommands;
import orca.flukes.ndl.ManifestLoader;
import orca.flukes.ndl.ModifySaver;
import orca.flukes.ndl.RequestSaver;
import orca.flukes.ui.TextAreaDialog;
import orca.flukes.util.IP4Assign;
import orca.flukes.xmlrpc.GENICHXMLRPCProxy;
import orca.flukes.xmlrpc.GENICHXMLRPCProxy.FedField;
import orca.flukes.xmlrpc.NDLConverter;
import orca.flukes.xmlrpc.OrcaSMXMLRPCProxy;

import org.apache.commons.collections15.Transformer;

import com.hyperrealm.kiwi.ui.KTextArea;
import com.hyperrealm.kiwi.ui.dialog.ExceptionDialog;
import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;
import com.hyperrealm.kiwi.ui.dialog.KQuestionDialog;
import com.hyperrealm.kiwi.ui.dialog.ProgressDialog;
import com.hyperrealm.kiwi.util.Task;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.EditingModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.renderers.BasicVertexRenderer;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;

/**
 * For managing new and existing slices - unification of Request and Manifest states.
 * @author ibaldin
 *
 */
public class GUIUnifiedState extends GUICommonState implements IDeleteEdgeCallBack<OrcaLink>, IDeleteNodeCallBack<OrcaNode> {
	private static final String STATE_FAILED = "failed";
	private static final String STATE_ACTIVE = "active";
	private static final String STATE_TICKETED = "ticketed";
	private static final String STATE_NASCENT = "nascent";
	private static final String RESERVATION_STATE = "reservation.state";
	public static final String NODE_TYPE_SITE_DEFAULT = "Site default";
	public static final String NO_NODE_DEPS="No dependencies";

	private static GUIUnifiedState instance = null;
	
	public enum GUIState { REQUEST, MANIFEST, MANIFESTWITHMODIFY, SUBMITTED };
	
	protected GUIState guiState = GUIState.REQUEST;

	// copy of manifest graph to support a crude form of undo
	SparseMultigraph<OrcaNode, OrcaLink> gManifest = null;
	
	// is it openflow (and what version [null means non-of])
	private String ofNeededVersion = null;
	private String ofUserEmail = null;
	private String ofSlicePass = null;
	private String ofCtrlUrl = null;

	// File in which we save
	File saveFile = null;

	// Reservation details
	private OrcaReservationTerm term;
	private String resDomainName = null;

	// save the guid of the namespace of the request if it was loaded
	String nsGuid = null;

	// manifest-related things
	protected String manifestString;
	protected Date start = null, end = null, newEnd = null;

	// modify-related things (added links and nodes can be inferred
	// directly from the graph)
	protected List<OrcaResource> deleted = new ArrayList<>();
	protected Map<String, GroupModifyRecord> modifiedGroups = new HashMap<>();

	// copy of original request graph
	protected SparseMultigraph<OrcaNode, OrcaLink> gRequest = null;

	// map from reservations to nodes
	protected Map<String, OrcaResource> guidsToResources = new HashMap<>();
	
	// help stop progress threads
	private static boolean stopProgress = true;
	
	// collect information about modified groups in a bean
	public static class GroupModifyRecord {
		private Integer countChange;
		List<String> removeNodes;

		public GroupModifyRecord() {
			countChange = 0;
			removeNodes = new ArrayList<>();
		}

		public void setCountChange(Integer add) {
			countChange = add;
		}

		public Integer getCountChange() {
			return countChange;
		}

		public void addRemoveNode(String nUrl) {
			removeNodes.add(nUrl);
		}

		public List<String> getRemoveNodes() {
			return new ArrayList<>(removeNodes);
		}
	}

	private static void initialize() {
		;
	}

	private GUIUnifiedState() {
		super();
		term = new OrcaReservationTerm();
		// Set some defaults for the Edges...
		linkCreator.setDefaultBandwidth(10000000);
		linkCreator.setDefaultLatency(5000);
		sState = SliceState.NEW;
	}

	public static GUIUnifiedState getInstance() {
		if (instance == null) {
			initialize();
			instance = new GUIUnifiedState();
		}
		return instance;
	}

	@Override
	public void clear() {
		super.clear();

		resDomainName = null;
		term = new OrcaReservationTerm();
		ofNeededVersion = null;
		ofUserEmail = null;
		ofSlicePass = null;
		ofCtrlUrl = null;
		nsGuid = null;
		saveFile = null;
		clearModify();
		clearGraph(g);
		gManifest = null;
		
		guiState = GUIState.REQUEST;

		GUIImageList.getInstance().collectAllKnownImages();
	}
	
	/**
	 * Clear only modify-specific things
	 */
	public void clearModify() {
		deleted.clear();
		modifiedGroups.clear();
		resetManifest();

	}
	
	public void clearGuidMap() {
		guidsToResources.clear();
	}
	
	// add a mapping
	public void mapGuidToResource(String guid, OrcaResource or) {
		guidsToResources.put(guid, or);
	}

	//
	// graph-copy related functions
	//
	public void saveRequest() {
		gRequest = new SparseMultigraph<>();
		copyGraph(g, gRequest);
	}
	
	public void resetRequest() {
		guiState = GUIState.REQUEST;
		copyGraph(gRequest, g);
		gRequest = null;
	}
	
	public void saveUnmodifiedManifest() {
		gManifest = new SparseMultigraph<>();
		copyGraph(g, gManifest);
	}
	
	public void resetManifest() {
		copyGraph(gManifest, g);
		guiState = GUIState.MANIFEST;
	}
	
	public void setGUIState(GUIState s) {
		guiState = s;
	}
	
	public GUIState getGUIState() {
		return guiState;
	}
	
	public OrcaReservationTerm getTerm() {
		return term;
	}

	public void setTerm(OrcaReservationTerm t) {
		term = t;
	}

	public void setNsGuid(String g) {
		nsGuid = g;
	}

	/**
	 * Change domain reservation. Reset node domain reservations to system select.
	 * @param d
	 */
	public void setDomainInReservation(String d) {
		// if the value is changing
		// set it for all nodes
		if ((resDomainName == null) && ( d == null))
			return;
		if ((resDomainName != null) && (resDomainName.equals(d)))
			return;
		// reset all node domains
		for(OrcaNode n: g.getVertices()) {
			n.setDomain(null);
		}
		resDomainName = d;
	}

	/**
	 * Simply set domain reservation to null
	 */
	public void resetDomainInReservation() {
		resDomainName = null;
	}

	public String getDomainInReservation() {
		return resDomainName;
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

	public String[] getAvailableDependencies(OrcaResource subject) {
		Collection<OrcaNode> knownNodes = g.getVertices();
		String[] ret = new String[knownNodes.size() - 1];
		int i = 0;
		for (OrcaResource n: knownNodes) {
			if ((!n.equals(subject)) && !(n instanceof OrcaCrossconnect)) {
				ret[i] = n.getName();
				i++;
			}
		}
		return ret;
	}

	public String[] getAvailableDependenciesWithNone(OrcaResource subject) {
		Collection<OrcaNode> knownNodes = g.getVertices();
		String[] ret = new String[knownNodes.size()];
		ret[0] = NO_NODE_DEPS;
		int i = 1;
		for (OrcaResource n: knownNodes) {
			if ((!n.equals(subject)) && !(n instanceof OrcaCrossconnect)) {
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


	public void setOF1_0() {
		ofNeededVersion = "1.0";
	}

	public void setOF1_1() {
		ofNeededVersion = "1.1";
	}

	public void setOF1_2() {
		ofNeededVersion = "1.2";
	}

	public void setNoOF() {
		ofNeededVersion = null;
	}

	public void setOFVersion(String v) {
		if ("1.0".equals(v) || "1.1".equals(v) || "1.2".equals(v))
			ofNeededVersion = v;
	}

	public String getOfNeededVersion() {
		return ofNeededVersion;
	}

	public void setOfUserEmail(String ue) {
		ofUserEmail = ue;
	}

	public String getOfUserEmail() {
		return ofUserEmail;
	}

	public void setOfSlicePass(String up) {
		ofSlicePass = up;
	}

	public String getOfSlicePass() {
		return ofSlicePass;
	}

	public void setOfCtrlUrl(String cu) {
		ofCtrlUrl = cu;
	}

	public String getOfCtrlUrl() {
		return ofCtrlUrl;
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

	@Override
	public void deleteNodeCallBack(OrcaNode n) {
		if (n == null)
			return;
		switch(n.getResourceType()) {
		case REQUEST:
			// remove incident edges
			Collection<OrcaLink> edges = g.getIncidentEdges(n);
			for (OrcaLink e: edges) {
				deleteEdgeCallBack(e);
			}
			g.removeVertex(n);
			break;
		case MANIFEST:
			if (n.getGroup() != null)
				removeNodeFromGroup(n.getGroup(), n.getUrl());
			else {
				deleted.add(n);
				// also remove incident point-to-point links
				for(OrcaLink rl: g.getIncidentEdges(n)) {
					if (rl.isResource())
						deleted.add(rl);
				}
			}
			g.removeVertex(n);
			setGUIState(GUIState.MANIFESTWITHMODIFY);
			break;
		default:	
		}
	}

	@Override
	public void deleteEdgeCallBack(OrcaLink e) {
		if (e == null)
			return;
		switch(e.getResourceType()) {
		case REQUEST:
			// remove edge from node IP maps
			Pair<OrcaNode> p = g.getEndpoints(e);
			p.getFirst().removeIp(e);
			p.getSecond().removeIp(e);
			g.removeEdge(e);
			break;
		case MANIFEST:
			deleted.add(e);
			g.removeEdge(e);
			setGUIState(GUIState.MANIFESTWITHMODIFY);
			break;
		default:
		}
	}

	/**
	 * This renderer adds a circle around icons colored depending on their state. Only kept
	 * as an example - instead we use layered icons and add/remove colored outlines on nodes. 
	 * See IconOutline class
	 * @author ibaldin
	 *
	 * @param <V>
	 * @param <E>
	 */
	class OutlineRenderer<V,E> extends BasicVertexRenderer<V,E> {
		public void paintIconForVertex(RenderContext<V,E> rc, V v, Layout<V,E> layout) {

			// get the coordinates of the icon
			Point2D p = layout.transform(v);
			p = rc.getMultiLayerTransformer().transform(Layer.LAYOUT, p);
			float x = (float)p.getX();
			float y = (float)p.getY();

			GraphicsDecorator g = rc.getGraphicsContext();
			Transformer<V,Icon> vertexIconFunction = rc.getVertexIconTransformer();
			Icon icon = vertexIconFunction.transform(v);

			Ellipse2D ellipse = new Ellipse2D.Float(-30, -30, 60, 60);

			Shape s = AffineTransform.getTranslateInstance(x,y).createTransformedShape(ellipse);
			paintShapeForVertex(rc, v, s);

			if(icon != null) {
				int xLoc = (int) (x - icon.getIconWidth()/2);
				int yLoc = (int) (y - icon.getIconHeight()/2);
				icon.paintIcon(rc.getScreenDevice(), g.getDelegate(), xLoc, yLoc);
			}
		}
	}

	@Override
	public void addPane(Container c) {

		// Layout<V, E>, VisualizationViewer<V,E>
		//	        Map<OrcaNode,Point2D> vertexLocations = new HashMap<OrcaNode, Point2D>();

		Layout<OrcaNode, OrcaLink> layout = new FRLayout<OrcaNode, OrcaLink>(g);

		//layout.setSize(new Dimension(1000,800));
		vv = new VisualizationViewer<OrcaNode,OrcaLink>(layout);
		// Show vertex and edge labels
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<OrcaNode>());
		vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<OrcaLink>());
		vv.getRenderContext().setEdgeDrawPaintTransformer(new OrcaLink.LinkPaint());
		vv.getRenderContext().setEdgeStrokeTransformer(new OrcaLink.LinkStroke());

		// Add icon and shape (so pickable areal roughly matches the icon) transformer
		vv.getRenderContext().setVertexShapeTransformer(new OrcaNode.OrcaNodeIconShapeTransformer());
		OrcaNodeIconTransformer it = new OrcaNode.OrcaNodeIconTransformer();
		vv.getRenderContext().setVertexIconTransformer(it);
		// add shape transformer to detect clickable shapes
		vv.getRenderContext().setVertexShapeTransformer(new OrcaNode.OrcaNodeIconShapeTransformer());

		// add a renderer to draw circles around icons
		//vv.getRenderer().setVertexRenderer(new OutlineRenderer<OrcaNode, OrcaLink>());

		// Create a graph mouse and add it to the visualization viewer
		OrcaNode.OrcaNodeFactory onf = new OrcaNode.OrcaNodeFactory(nodeCreator);
		OrcaLink.OrcaLinkFactory olf = new OrcaLink.OrcaLinkFactory(linkCreator);

		// FIXME: this editingmodalgraphmosewithmodifiers is broken w.r.t. pick - picking on node
		// results in loopback links being added. For now use the usual editingmodelgraphmouse 10/30/12 /ib
		//gm = new EditingModalGraphMouseWithModifiers<OrcaNode, OrcaLink>(MouseEvent.BUTTON1_MASK, vv.getRenderContext(),
		gm = new EditingModalGraphMouse<OrcaNode, OrcaLink>(vv.getRenderContext(), onf, olf);

		// add the plugin
		//PopupVertexEdgeMenuMousePlugin<OrcaNode, OrcaLink> myPlugin = new PopupVertexEdgeMenuMousePlugin<OrcaNode, OrcaLink>();
		PopupMultiVertexEdgeMenuMousePlugin myPlugin = new PopupMultiVertexEdgeMenuMousePlugin();

		// Add some popup menus for the edges and vertices to our mouse plugin.
		//myPlugin.setEdgePopup(new MouseMenus.RequestEdgeMenu());
		//myPlugin.setVertexPopup(new MouseMenus.RequestNodeMenu());
		myPlugin.setModePopup(new MouseMenus.ModeMenu());
		gm.remove(gm.getPopupEditingPlugin());  // Removes the existing popup editing plugin
		gm.add(myPlugin);

		// add listener to add/remove checkmarks on selected nodes
		PickedState<OrcaNode> ps = vv.getPickedVertexState();
		ps.addItemListener(new OrcaNode.PickWithIconListener(it));

		vv.setGraphMouse(gm);

		vv.setLayout(new BorderLayout(0,0));

		c.add(vv);

		gm.setMode(ModalGraphMouse.Mode.EDITING); // Start off in editing mode  
	}

	public void saveRequestToIRods() {
		IRodsICommands irods = new IRodsICommands();
		String ndl = RequestSaver.getInstance().convertGraphToNdl(g, nsGuid);
		if ((ndl == null) ||
				(ndl.length() == 0)) {
			KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
			kmd.setMessage("Unable to convert graph to NDL.");
			kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
			kmd.setVisible(true);
			return;
		}
		try {
			// convert if needed
			if (GUI.getInstance().getPreference(PrefsEnum.IRODS_FORMAT).equalsIgnoreCase("rspec")) {
				String rspec = NDLConverter.callConverter(NDLConverter.RSPEC3_TO_NDL, new Object[]{ndl, sliceIdField.getText()});
				irods.saveFile(IRodsICommands.substituteRequestName(), rspec);
			} else if (GUI.getInstance().getPreference(PrefsEnum.IRODS_FORMAT).equalsIgnoreCase("ndl"))
				irods.saveFile(IRodsICommands.substituteRequestName(), ndl);
			else {
				ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
				ed.setLocationRelativeTo(GUI.getInstance().getFrame());
				ed.setException("Exception encountered while saving request to iRods: ", 
						new Exception("unknown format " + GUI.getInstance().getPreference(PrefsEnum.IRODS_FORMAT)));
				ed.setVisible(true);
			}
		} catch (IRodsException ie) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while saving request to iRods: ", ie);
			ed.setVisible(true);
		} catch (Exception e) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while saving request to iRods: ", e);
			ed.setVisible(true);
		}
	}
	
	private void removeAssignedRequestIPs() {
		// remove IPs associated with requested links
		for(OrcaLink ol: g.getEdges()) {
			if (requestResource(ol)) {
				Collection<OrcaNode> incs = g.getIncidentVertices(ol);
				for(OrcaNode inc: incs) {
					inc.removeIp(ol);
				}
			}
		}
		
		// remove all IPs from request nodes
		for(OrcaNode on: g.getVertices()) {
			if (requestResource(on))
				on.removeAllIps();
		}
	}
	
	private boolean assignableNode(OrcaNode on) {
		if (on instanceof OrcaCrossconnect) 
			return false;
		
		if (on instanceof OrcaStorageNode)
			return false;
		
		if (on instanceof OrcaStitchPort) 
			return false;

		
		return true;
	}
	
	private boolean assignableRequestNode(OrcaNode on) {
		if (on.getResourceType() != ResourceType.REQUEST)
			return false;
		
		return assignableNode(on);
	}
	
	private boolean requestResource(OrcaResource on) {
		return on.getResourceType() == ResourceType.REQUEST;
	}
	
	public boolean autoAssignIPAddresses() {
		// for each link and switch assign IP addresses
		// treat node groups as switches
		
		// collect all already used addresses
		List<IP4Assign.AssignedRange> allAddresses = new ArrayList<>();
		
		for(OrcaNode on: g.getVertices()) {
			int qty = 1;
			if (on instanceof OrcaNodeGroup) 
				qty = ((OrcaNodeGroup)on).getNodeCount();
			for(OrcaLink ol: g.getIncidentEdges(on)) {
				String ad = on.getIp(ol);
				if (ad != null) {
					allAddresses.add(new IP4Assign.AssignedRange(ad, qty));
				}
			}
		}
		
		int mpMask = Integer.parseInt(GUI.getInstance().getPreference(PrefsEnum.AUTOIP_MASK));
		IP4Assign ipa = new IP4Assign(mpMask);

		for(OrcaLink ol: g.getEdges()) {

			if (!requestResource(ol))
				continue;

			if (ol.linkToSharedStorage())
				continue;

			// if one end is a switch, ignore it for now
			Pair<OrcaNode> pn = g.getEndpoints(ol);
			
			if (!assignableNode(pn.getFirst()) || !assignableNode(pn.getSecond()))
				continue;

			// if already assigned, skip
			// this is a little careless, but not clear what to do if one is set
			// and the other one isn't /ib
			if ((pn.getFirst().getIp(ol) != null) || (pn.getSecond().getIp(ol) != null))
				continue;
			
			int nodeCt1, nodeCt2;
			if (pn.getFirst() instanceof OrcaNodeGroup) {
				OrcaNodeGroup ong = (OrcaNodeGroup)pn.getFirst();
				nodeCt1 = ong.getNodeCount();
			} else
				nodeCt1 = 1;
			if (pn.getSecond() instanceof OrcaNodeGroup) {
				OrcaNodeGroup ong = (OrcaNodeGroup)pn.getSecond();
				nodeCt2 = ong.getNodeCount();
			} else
				nodeCt2 = 1;

			if (nodeCt1 + nodeCt2 == 2) {
				String[] addrs = ipa.getPPAddresses(allAddresses);
				if (addrs != null) {
					pn.getFirst().setIp(ol, addrs[0], "" + ipa.getPPIntMask());
					pn.getSecond().setIp(ol, addrs[1], "" + ipa.getPPIntMask());
				} else {
					return false;
				}
			} else {
				String[] addrs = ipa.getMPAddresses(nodeCt1 + nodeCt2, null, allAddresses);
				if (addrs != null) {
					pn.getFirst().setIp(ol, addrs[0], "" + ipa.getMPIntMask());
					pn.getSecond().setIp(ol, addrs[nodeCt1], "" + ipa.getMPIntMask());
				} else
					return false;
			}
		}

		// now deal with crossconnects
		// each crossconnects may have nodes or groups attached to it
		for(OrcaNode csx: g.getVertices()) {
			if (!(csx instanceof OrcaCrossconnect))
				continue;

			OrcaCrossconnect csxI = (OrcaCrossconnect)csx;
			
			if (csxI.linkToSharedStorage())
				continue;
			
			// find neighbor nodes (they can't be crossconnects)
			int[] nodeCts = new int[g.getNeighborCount(csx)];
			int i = 0;
			Collection<OrcaLink> csxIncLinks = g.getIncidentEdges(csx);
			
			int sum = 0;
			List<IP4Assign.AssignedRange> alreadyAssigned = new ArrayList<>();
			for(OrcaLink incLink: csxIncLinks) {
				Collection<OrcaNode> neighborCandidates = g.getIncidentVertices(incLink);
				OrcaNode nb = null;
				
				for(OrcaNode nbCandidate: neighborCandidates) {
					if (g.getOpposite(nbCandidate, incLink).equals(csx))
						nb = nbCandidate;
				}
				
				if (!assignableNode(nb))
					continue;

				if (nb instanceof OrcaNodeGroup) {
					// populate used addresses, if already numbered
					if (nb.getIp(incLink) != null) {
						alreadyAssigned.add(new IP4Assign.AssignedRange(nb.getIp(incLink), ((OrcaNodeGroup)nb).getNodeCount()));
					} else {
						nodeCts[i] = ((OrcaNodeGroup)nb).getNodeCount();
					}
				} else {
					// could be new or modify
					if (nb.getIp(incLink) != null) {
						alreadyAssigned.add(new IP4Assign.AssignedRange(nb.getIp(incLink), 1));
					} else {
						nodeCts[i] = 1;
					}
				}
				sum += nodeCts[i++];
			}
			
			// everything already assigned
			if (sum == 0)
				continue;
			
			String[] addrs = ipa.getMPAddresses(sum, alreadyAssigned, allAddresses);
			
			Collection<OrcaNode> neighbors = g.getNeighbors(csx);
			if (addrs != null) {
				int ct = 0;
				i = 0;
				for(OrcaNode nb: neighbors) {
					if (!assignableNode(nb))
						continue;
					
					// find the link that goes back to the crossconnect
					for(OrcaLink nl: g.getIncidentEdges(nb)) {
						if (g.getOpposite(nb, nl).equals(csx)) {
							// assign addresses as needed
							if (nb.getIp(nl) == null) {
								nb.setIp(nl, addrs[ct], "" + ipa.getMPIntMask());
								if (nb instanceof OrcaNodeGroup) 
									ct += ((OrcaNodeGroup)nb).getNodeCount();
								else
									ct++;
							}

							break;
						}
					}
				}
			} else
				return false;
		}
		return true;
	}

	/**
	 * Poll the controller for manifest reservation states until all become non-ticketed
	 * @param o
	 * @throws Exception
	 */
	public static void processManifestPoll(ProgressDialog o) throws Exception {

		o.setProgress(0);
		boolean flag = false;
		// set thread SSL identity
		OrcaSMXMLRPCProxy.getInstance().setThreadCurrentAlias();
		while(!flag && !stopProgress) {
			Map<String, Map<String, String>> states = GUIUnifiedState.getInstance().queryManifestStates();
			GUI.getInstance().kickLayout(GuiTabs.UNIFIED_VIEW);
			flag = true;
			int ticketed = 0, failed = 0, active = 0; 
			for(Map.Entry<String, Map<String, String>> me: states.entrySet()) {
				String state = me.getValue().get(RESERVATION_STATE);
				// we count nascent as ticketed here for simplicity
				if (state.equalsIgnoreCase(STATE_TICKETED) || state.equalsIgnoreCase(STATE_NASCENT)) {
					flag = false;
					ticketed++;
				}
				if (state.equalsIgnoreCase(STATE_ACTIVE)) {
					active++;
				}
				if (state.equalsIgnoreCase(STATE_FAILED)) {
					failed++;
				}
			}
			float total = (float)states.entrySet().size();
			o.setMessage("Ticketed: " + ticketed + "/ Active: " + active + "/ Failed: " + failed);
			o.setProgress((int)((total-ticketed)/total*100.0));
			o.pack();
			Thread.sleep(Integer.parseInt(GUI.getInstance().getPreference(PrefsEnum.QUERY_POLL_INTERVAL))*1000);
		}
		if (!stopProgress) {
			o.setProgress(100);
			GUI.getInstance().kickLayout(GuiTabs.UNIFIED_VIEW);
		}
		stopProgress = true;
		//GUIUnifiedState.getInstance().queryManifest();
	}
	
	/**
	 * Button listener for the unified pane
	 * @author ibaldin
	 *
	 */
	public class UnifiedButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			GUI.getInstance().hideMenus();
			stopProgress = true;
			if (e.getActionCommand().equals("manifest")) {
				queryManifest();
			} else if (e.getActionCommand().equals("manifestpoll")) {
				boolean querySuccess = true;
				if (guiState != GUIState.MANIFEST)
					querySuccess = queryManifest();
				if (querySuccess) {
					final ProgressDialog pd = GUI.getProgressDialog("Polling for reservation states");
					stopProgress = false;
					try {
						pd.track(new Task (){
							@Override
							public void run() {
								try {
									processManifestPoll(pd);
								} catch (Exception e) {
									pd.destroy();
									ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
									ed.setLocationRelativeTo(GUI.getInstance().getFrame());
									ed.setException("Exception encountered while polling reservation states: ", e);
									ed.setVisible(true);
								}
							}
						});

					} catch (Exception ex) {
						pd.destroy();	
						ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
						ed.setLocationRelativeTo(GUI.getInstance().getFrame());
						ed.setException("Exception encountered while polling reservation states: ", ex);
						ed.setVisible(true);
					}
					// query one last time to get e.g. IP addresses and other labels with late binding
					queryManifest();
				}
			} else if (e.getActionCommand().equals("addsshkey")) {
			    try {
                    // display key dialog
                    NewUserDialog sshKeyDialog = new NewUserDialog(GUI.getInstance().getFrame(), "Account and SSH keys", 
                            "Paste your public SSH key here:", 20, 50);
                    sshKeyDialog.pack();
                    sshKeyDialog.setVisible(true);
                    String keys = sshKeyDialog.getSSHKeys();
                    boolean sudo = sshKeyDialog.getSudo();
                    String username = sshKeyDialog.getUsername();

                    for(OrcaNode on: g.getVertices()) {
                        if (on instanceof OrcaNode) {
                            // call XMLRPC proxy to insert the key into node
                            Boolean tmpRes = OrcaSMXMLRPCProxy.getInstance().modifySliverSSH(
                                    GUIUnifiedState.getInstance().getSliceName(), 
                                    on.getReservationGuid(), username, sudo, Arrays.asList(keys));
                            if (!tmpRes) 
                                throw new Exception("Unable to insert ssh key into node " + on.getName());
                        }
                    }

                    KMessageDialog kd = new KMessageDialog(GUI.getInstance().getFrame(), "Result", true);
                    kd.setMessage("SSH Keys inserted successfully");                            
                    kd.setLocationRelativeTo(GUI.getInstance().getFrame());
                    kd.setVisible(true);
                } catch (Exception ex) {
                    ExceptionDialog ked = new ExceptionDialog(GUI.getInstance().getFrame(),
                            "Unable to insert SSH key due to exception!");
                    ked.setException("Exception encountered: ", ex);
                    ked.setLocationRelativeTo(GUI.getInstance().getFrame());
                    ked.setVisible(true);
                }
			}  else if (e.getActionCommand().equals("clear")) {
				// distinguish modify clear and all clear
				switch(guiState) {
				case REQUEST:
					clear();
				case MANIFEST:
					// do nothing
					break;
				case SUBMITTED:
				case MANIFESTWITHMODIFY:
					removeAssignedRequestIPs();
					setGUIState(GUIState.MANIFEST);
					clearModify();
					break;
				}
				vv.repaint();
			} else if (e.getActionCommand().equals(GUI.Buttons.listSlices.getCommand())) {
				try {
					String[] slices = OrcaSMXMLRPCProxy.getInstance().listMySlices();
					OrcaSliceList osl = new OrcaSliceList(GUI.getInstance().getFrame(), slices);
					osl.pack();
					osl.setVisible(true);
				} catch (Exception ex) {
					ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
					ed.setLocationRelativeTo(GUI.getInstance().getFrame());
					ed.setException("Exception encountered while listing user slices: ", ex);
					ed.setVisible(true);
				}
			} else if (e.getActionCommand().equals("delete")) {
				if ((sliceIdField.getText() == null) || 
						(sliceIdField.getText().length() == 0)) {
					KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
					kmd.setMessage("You must specify a slice id");
					kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
					kmd.setVisible(true);
					return;
				}

				KQuestionDialog kqd = new KQuestionDialog(GUI.getInstance().getFrame(), "Exit", true);
				kqd.setMessage("Are you sure you want to delete slice " + sliceIdField.getText());
				kqd.setLocationRelativeTo(GUI.getInstance().getFrame());
				kqd.setVisible(true);
				if (!kqd.getStatus()) 
					return;
				deleteSlice(sliceIdField.getText());
				setGUIState(GUIState.REQUEST);
				clear();
				vv.repaint();
			} else if (e.getActionCommand().equals("reservation")) {
				ReservationDetailsDialog rdd = new ReservationDetailsDialog(GUI.getInstance().getFrame());
				rdd.setFields(getDomainInReservation(),
						getTerm(), ofNeededVersion);
				rdd.pack();
				rdd.setVisible(true);
			} else if (e.getActionCommand().equals(GUI.Buttons.nodes.getCommand())) {
				nodeCreator.setCurrent(OrcaNodeEnum.CE);
			} else if (e.getActionCommand().equals("nodegroups")) {
				nodeCreator.setCurrent(OrcaNodeEnum.NODEGROUP);
			} else if (e.getActionCommand().equals("bcastlinks")) {
				nodeCreator.setCurrent(OrcaNodeEnum.CROSSCONNECT);
			} else if (e.getActionCommand().equals("stitchport")) {
				nodeCreator.setCurrent(OrcaNodeEnum.STITCHPORT);
			} else if (e.getActionCommand().equals("storage")) {
				nodeCreator.setCurrent(OrcaNodeEnum.STORAGE);
			} else if (e.getActionCommand().equals(GUI.Buttons.links.getCommand())) {
				linkCreator.setLinkType(OrcaLinkCreator.OrcaLinkType.TOPO);
			} else if (e.getActionCommand().equals("topo")) {
				linkCreator.setLinkType(OrcaLinkCreator.OrcaLinkType.TOPO);
			} else if (e.getActionCommand().equals("color")) {
				linkCreator.setLinkType(OrcaLinkCreator.OrcaLinkType.COLOR);
			} else if (e.getActionCommand().equals("autoip")) {
				removeAssignedRequestIPs();
				if (!autoAssignIPAddresses()) {
					KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
					kmd.setMessage("Unable auto-assign IP addresses.");
					kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
					kmd.setVisible(true);
				}
			} else if (e.getActionCommand().equals("unip")) { 
				removeAssignedRequestIPs();
			} else if (e.getActionCommand().equals("submit")) {
				if ((sliceIdField.getText() == null) || 
						(sliceIdField.getText().length() == 0)) {
					KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
					kmd.setMessage("You must specify a slice id");
					kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
					kmd.setVisible(true);
					return;
				}
				String ndl = null;
				String sliceUrn = sliceIdField.getText();
				if (guiState == GUIState.REQUEST) {
					ndl = RequestSaver.getInstance().convertGraphToNdl(g, nsGuid);
					if ((ndl == null) ||
							(ndl.length() == 0)) {
						KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
						kmd.setMessage("Unable to convert graph to NDL.");
						kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
						kmd.setVisible(true);
						return;
					}
					try {
						// add slice to the SA
						boolean saError = false;
						if (GUI.getInstance().getPreference(GUI.PrefsEnum.ENABLE_GENISA).equalsIgnoreCase("true") ||
								GUI.getInstance().getPreference(GUI.PrefsEnum.ENABLE_GENISA).equalsIgnoreCase("yes")) {
							try {
								sliceUrn = GENICHXMLRPCProxy.getInstance().saCreateSlice(sliceUrn, 
										GUI.getInstance().getPreference(GUI.PrefsEnum.GENISA_PROJECT));	
							} catch (Exception ee) {
								ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
								ed.setLocationRelativeTo(GUI.getInstance().getFrame());
								ed.setException("Exception encountered while communicating with SA: ", ee);
								ed.setVisible(true);
								saError = true;
							}
						}
						if (!saError) {
							String status = OrcaSMXMLRPCProxy.getInstance().createSlice(sliceUrn, ndl);
							TextAreaDialog tad = new TextAreaDialog(GUI.getInstance().getFrame(), "ORCA Response", 
									"ORCA Controller response", 
									25, 50);
							KTextArea ta = tad.getTextArea();

							ta.setText(status);
							tad.pack();
							tad.setVisible(true);
							saveRequest();
							setGUIState(GUIState.SUBMITTED);
						}
					} catch (Exception ex) {
						ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
						ed.setLocationRelativeTo(GUI.getInstance().getFrame());
						ed.setException("Exception encountered while submitting slice request to ORCA: ", ex);
						ed.setVisible(true);
					}
				} else if (guiState == GUIState.MANIFESTWITHMODIFY) {
					ndl = ModifySaver.getInstance().convertModifyGraphToNdl(g, deleted, modifiedGroups);
					try {
						String status = OrcaSMXMLRPCProxy.getInstance().modifySlice(sliceUrn, ndl);
						TextAreaDialog tad = new TextAreaDialog(GUI.getInstance().getFrame(), "ORCA Response", 
								"ORCA Controller response", 
								25, 50);
						KTextArea ta = tad.getTextArea();

						ta.setText(status);
						tad.pack();
						tad.setVisible(true);
						setGUIState(GUIState.SUBMITTED);
					} catch (Exception ex) {
						ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
						ed.setLocationRelativeTo(GUI.getInstance().getFrame());
						ed.setException("Exception encountered while submitting slice modify request to ORCA: ", ex);
						ed.setVisible(true);
					}
				} else if (guiState == GUIState.SUBMITTED){
					KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
					kmd.setMessage("These modifications have already been submitted, please query for manifest");
					kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
					kmd.setVisible(true);
				} else {
					KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
					kmd.setMessage("No modifications to submit.");
					kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
					kmd.setVisible(true);
				}
			} else if (e.getActionCommand().equals("extend")) {
				ReservationExtensionDialog red = new ReservationExtensionDialog(GUI.getInstance().getFrame());
				red.setFields(new Date());
				red.pack();
				red.setVisible(true);

				String sliceUrn = sliceIdField.getText();

				if (newEnd != null) {
					boolean saException = false;
					// check slice expiration with SA and extend if necessary
					if (GUI.getInstance().getPreference(GUI.PrefsEnum.ENABLE_GENISA).equalsIgnoreCase("true") ||
							GUI.getInstance().getPreference(GUI.PrefsEnum.ENABLE_GENISA).equalsIgnoreCase("yes")) {
						try {
							Map<String, Object> r = GENICHXMLRPCProxy.getInstance().saLookupSlice(sliceUrn, 
									new FedField[] { GENICHXMLRPCProxy.FedField.SLICE_EXPIRATION});
							String dateString = (String)((Map<String, Object>)r.get(sliceUrn)).get(GENICHXMLRPCProxy.FedField.SLICE_EXPIRATION.name());
							DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss", Locale.ENGLISH);
							df.setTimeZone(TimeZone.getTimeZone("UTC"));
							Date result =  df.parse(dateString);
							if (result.before(newEnd)) {
								// update slice expiration on SA
								df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
								df.setTimeZone(TimeZone.getTimeZone("UTC"));
								String nowAsISO = df.format(newEnd);

								GENICHXMLRPCProxy.getInstance().saUpdateSlice(sliceUrn, FedField.SLICE_EXPIRATION, nowAsISO);
							}
						} catch (Exception sae) {
							ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
							ed.setLocationRelativeTo(GUI.getInstance().getFrame());
							ed.setException("Unable to extend slice on the SA: ", sae);
							ed.setVisible(true);
							saException = true;
						}
					}

					if (!saException) {
						try {
							String res = OrcaSMXMLRPCProxy.getInstance().renewSlice(sliceUrn, newEnd);
							KMessageDialog kd = new KMessageDialog(GUI.getInstance().getFrame(), "Result", true);
							if (res != null) {
								if (res.length() > 0)
									kd.setMessage("The extend operation returned new date: " + res);
								else
									kd.setMessage("Extend operation succeeded, please check actual date by requesting manifest as list.");
								
								kd.setLocationRelativeTo(GUI.getInstance().getFrame());
								kd.setVisible(true);
							}
							if (res != null)
								resetEndDate();
							else
								newEnd = null;
						} catch (Exception ee) {
							ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
							ed.setLocationRelativeTo(GUI.getInstance().getFrame());
							ed.setException("Exception encountered while extending slice: ", ee);
							ed.setVisible(true);
						}
					}
				} else {
					KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
					kmd.setMessage("Invalid new end date.");
					kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
					kmd.setVisible(true);
					return;
				}
			} else if (e.getActionCommand().equals("view")) {
				if (queryManifest())
					launchResourceStateViewer(start, end);
			} else if (e.getActionCommand().equals("resetrequset")) {
				GUIUnifiedState.getInstance().resetRequest();
				vv.repaint();
			}
		}
	}

	ActionListener al = new UnifiedButtonListener();
	public ActionListener getActionListener() {
		return al;
	}
	
	//
	// Manifest-related functions
	//

	public void setManifestString(String s) {
		manifestString = s;
	}

	public String getManifestString() {
		return manifestString;
	}

	public boolean emptyManifestString() {
		if ((manifestString == null) || (manifestString.length() == 0))
			return true;
		return false;
	}
	
	public void setManifestTerm(Date s, Date e) {
		start = s;
		end = e;
	}

	public void setNewEndDate(Date s) {

		if ((start == null) || (end == null))
			return;

		Long diff = s.getTime() - start.getTime();
		if (diff < 0)
			return;

		diff = s.getTime() - end.getTime();
		if (diff < 0)
			return;

		newEnd = s;	
	}

	public void resetEndDate() {
		end = newEnd;
		newEnd = null;
	}

	void deleteSlice(String name) {
		if ((name == null) || 
				(name.length() == 0)) {
			KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
			kmd.setMessage("You must specify a slice id");
			kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
			kmd.setVisible(true);
			return;
		}

		try {
			OrcaSMXMLRPCProxy.getInstance().deleteSlice(name);
		} catch (Exception ex) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while deleting slice manifest: ", ex);
			ed.setVisible(true);
		}
	}

	void modifySlice(String name, String req) {
		if ((name == null) || 
				(name.length() == 0)) {
			KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
			kmd.setMessage("You must specify a slice id");
			kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
			kmd.setVisible(true);
			return;
		}
		try {
			String s = OrcaSMXMLRPCProxy.getInstance().modifySlice(name, req);
			TextAreaDialog tad = new TextAreaDialog(GUI.getInstance().getFrame(), "Modify Output", 
					"Modify Output", 
					30, 50);
			KTextArea ta = tad.getTextArea();

			if (s != null)
				ta.setText(s);
			tad.pack();
			tad.setVisible(true);
		} catch (Exception ex) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while modifying slice: ", ex);
			ed.setVisible(true);
		}
	}

	private String stripManifest(String m) {
		if (m == null)
			return null;
		int ind = m.indexOf("<rdf:RDF");
		if (ind > 0)
			return m.substring(ind);
		else
			return null;
	}

	/**
	 * Find out the states of reservations in previously retrieved manifest
	 * @return
	 * @throws Exception
	 */
	synchronized Map<String, Map<String, String>> queryManifestStates() throws Exception {
		
		// find all resources and query their state directly
		List<String> resList = new ArrayList<>();
		
		if (g == null)
			return null;
		
		for(OrcaNode onn: g.getVertices()) {
			if (onn.isResource() && (onn.getReservationGuid() != null)) {
				resList.add(onn.getReservationGuid());
			}
		}
		for(OrcaLink oll: g.getEdges()) {
			if (oll.isResource() && (oll.getReservationGuid() != null)) {
				resList.add(oll.getReservationGuid());
			}
		}
		
		Map<String, Map<String, String>> ret = OrcaSMXMLRPCProxy.getInstance().getReservationStates(getSliceName(), resList);
		
		for(Map.Entry<String, Map<String, String>> me: ret.entrySet()) {
			OrcaResource or = guidsToResources.get(me.getKey());
			
			if (or == null)
				continue;
			
			or.setState(me.getValue().get(RESERVATION_STATE));
		}
		
		return ret;
	}
	
	/**
	 * Fill in manifest graph
	 */
	synchronized boolean queryManifest() {
		// run request manifest from controller
		if ((sliceIdField.getText() == null) || 
				(sliceIdField.getText().length() == 0)) {
			KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
			kmd.setMessage("You must specify a slice id");
			kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
			kmd.setVisible(true);
			return false;
		}

		try {
			clear();

			manifestString = OrcaSMXMLRPCProxy.getInstance().sliceStatus(sliceIdField.getText());

			ManifestLoader ml = new ManifestLoader();

			String realM = stripManifest(manifestString);
			if (realM != null) {
				if (ml.loadString(realM)) {
					GUI.getInstance().kickLayout(GuiTabs.UNIFIED_VIEW);
					// save unmodified manifest
					saveUnmodifiedManifest();
					setGUIState(GUIState.MANIFEST);
				}
			} else {
				KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
				kmd.setMessage("Error has occurred, check raw controller response for details.");
				kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
				kmd.setVisible(true);
				return false;
			}
		} catch (Exception ex) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while querying the controller for slice manifest: ", ex);
			ed.setVisible(true);
			return false;
		} 
		return true;
	}

	public void launchResourceStateViewer(Date start, Date end) {
		if (start == null)
			return;

		// get a list of nodes and links
		List<OrcaResource> resources = new ArrayList<OrcaResource>();

		resources.addAll(g.getVertices());
		resources.addAll(g.getEdges());

		//OrcaResourceStateViewer viewer = new OrcaTableResourceStateViewer(GUI.getInstance().getFrame(), resources, start, end);
		OrcaResourceStateViewer viewer = new OrcaHtmlResourceStateViewer(GUI.getInstance().getFrame(), resources, start, end);
		viewer.pack();
		viewer.setVisible(true);
	}

	public void printResourceState(Date start, Date end) {
		// get a list of nodes and links
		List<OrcaResource> resources = new ArrayList<OrcaResource>();

		resources.addAll(g.getVertices());
		resources.addAll(g.getEdges());

		System.out.println("Printing resource state:");
		for (OrcaResource r : resources) {
			String resourceInfo =
					"Name: " + r.getName() + ", " +
							"IsResource: " + Boolean.toString(r.isResource()) + ", " +
							"State: " + r.getState() + "\n";

			resourceInfo += "ResNotice: ";
			String resNotice = r.getReservationNotice();
			if (resNotice != null)
				resourceInfo += resNotice; // Contains "\n" on end.
			else
				resourceInfo += "None\n";

			resourceInfo +=	"Colors: ";
			List<OrcaColor> colors = r.getColors();
			if (colors.size() > 0) {
				resourceInfo += "|";
				for (OrcaColor c : colors) {
					resourceInfo += c.getLabel() + "|";
				}
			}
			else {
				resourceInfo += "None";
			}
			System.out.println(resourceInfo);
		}
	}

	public void saveManifestToIRods() {
		IRodsICommands irods = new IRodsICommands();
		if (manifestString == null) {
			KMessageDialog md = new KMessageDialog(GUI.getInstance().getFrame(), "Manifest Error", true);
			md.setMessage("Manifest is empty!");
			md.setLocationRelativeTo(GUI.getInstance().getFrame());
			md.setVisible(true);
			return;
		}
		try {
			String realM = stripManifest(manifestString);
			// convert if needed
			String iRodsName = IRodsICommands.substituteManifestName();
			if (GUI.getInstance().getPreference(PrefsEnum.IRODS_FORMAT).equalsIgnoreCase("rspec")) {
				String rspec = NDLConverter.callConverter(NDLConverter.MANIFEST_TO_RSPEC, new Object[]{realM, sliceIdField.getText()});
				irods.saveFile(iRodsName, rspec);
			} else if (GUI.getInstance().getPreference(PrefsEnum.IRODS_FORMAT).equalsIgnoreCase("ndl"))
				irods.saveFile(iRodsName, realM);
			else {
				ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
				ed.setLocationRelativeTo(GUI.getInstance().getFrame());
				ed.setException("Exception encountered while saving manifest to iRods: ", 
						new Exception("unknown format " + GUI.getInstance().getPreference(PrefsEnum.IRODS_FORMAT)));
				ed.setVisible(true);
			}
			KMessageDialog md = new KMessageDialog(GUI.getInstance().getFrame(), "Saving to iRods", true);
			md.setMessage("Manifest saved as " + iRodsName);
			md.setLocationRelativeTo(GUI.getInstance().getFrame());
			md.setVisible(true);
		} catch (IRodsException ie) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while saving manifest to iRods: ", ie);
			ed.setVisible(true);
		} catch (Exception e) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while saving manifest to iRods: ", e);
			ed.setVisible(true);
		}
	}

	public static void showAlreadySubmittedMessage() {
		KMessageDialog md = new KMessageDialog(GUI.getInstance().getFrame(), "Already submitted", true);
		md.setMessage("Modifications have been submitted, please query for manifest");
		md.setLocationRelativeTo(GUI.getInstance().getFrame());
		md.setVisible(true);
	}
	
	//
	// modify operations
	//

	public void addNodesToGroup(String url, Integer c) {
		GroupModifyRecord gmr = null;

		if ((c <= 0) || (url == null))
			return;

		if (guiState == GUIState.SUBMITTED) {
			showAlreadySubmittedMessage();
			return;
		}
		
		if (guiState == GUIState.MANIFEST)
			setGUIState(GUIState.MANIFESTWITHMODIFY);

		if (modifiedGroups.containsKey(url)) {
			gmr = modifiedGroups.get(url);
		} else {
			gmr = new GroupModifyRecord();
			modifiedGroups.put(url, gmr);
		}
		gmr.setCountChange(c);
	}

	public void removeNodeFromGroup(String url, String nUrl) {
		GroupModifyRecord gmr = null;

		if ((nUrl == null) || (url == null))
			return;

		if (guiState == GUIState.SUBMITTED) {
			showAlreadySubmittedMessage();
			return;
		}
		
		if (guiState == GUIState.MANIFEST)
			setGUIState(GUIState.MANIFESTWITHMODIFY);
		
		if (modifiedGroups.containsKey(url)) {
			gmr = modifiedGroups.get(url);
		} else {
			gmr = new GroupModifyRecord();
			modifiedGroups.put(url, gmr);
		}
		gmr.addRemoveNode(nUrl);
	}
	
	public String generateModifyNdl() {
		if (guiState == GUIState.MANIFESTWITHMODIFY) {
			return ModifySaver.getInstance().convertModifyGraphToNdl(g, deleted, modifiedGroups);
		}
		return null;
	}

}
