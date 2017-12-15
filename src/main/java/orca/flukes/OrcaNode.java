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

import java.awt.Color;
import java.awt.Shape;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.FourPassImageShaper;
import edu.uci.ics.jung.visualization.LayeredIcon;
import edu.uci.ics.jung.visualization.renderers.Checkmark;
import orca.flukes.GUIUnifiedState.GUIState;
import orca.flukes.MouseMenus.DomainDisplay;
import orca.flukes.MouseMenus.ImageDisplay;
import orca.flukes.MouseMenus.IncreaseByNodeGroupItem;
import orca.flukes.MouseMenus.MultiDomainDisplay;
import orca.flukes.MouseMenus.NodeColorItem;
import orca.flukes.MouseMenus.NodeInsertSSHKeyItem;
import orca.flukes.MouseMenus.NodeLoginItem;
import orca.flukes.MouseMenus.NodePropItem;
import orca.flukes.MouseMenus.NodePropertiesItem;
import orca.flukes.MouseMenus.NodeTypeDisplay;
import orca.flukes.MouseMenus.NodeViewItem;
import orca.flukes.MouseMenus.PerformStitchingItem;
import orca.flukes.MouseMenus.PermitStitchingItem;
import orca.flukes.MouseMenus.RevokeStitchingItem;
import orca.flukes.MouseMenus.StitchPropertiesItem;
import orca.flukes.MouseMenus.UndoStitchingItem;
import orca.flukes.ui.Colors;
import orca.flukes.ui.IconOutline;

public class OrcaNode extends OrcaResource {

	protected static final String NOT_SPECIFIED = "Not specified";
	public static final String NODE_NETMASK="32";
	
	protected String image = null;
	protected String domain = null;
	protected String group = null;
	// Pair<String> first is IP, second is Netmask
	protected HashMap<OrcaLink, Pair<String>> addresses;
	protected HashMap<OrcaLink, String> macAddresses;
	
	protected List<String> managementAccess = null;
	
	protected final LayeredIcon icon;
	protected final Shape outline;

	protected Map<String, String> substrateInfo = new HashMap<String, String>();

	// specific node type 
	protected String nodeType = null;
	// post-boot script
	protected String postBootScript = null;
	// list of open ports
	protected String openPorts = null;
	
	protected Set<OrcaNode> dependencies = new HashSet<OrcaNode>();
	
	// mapping from links to interfaces on those links (used for manifests)
	protected Map<OrcaLink, String> interfaces = new HashMap<OrcaLink, String>();
	
	interface INodeCreator {
		public OrcaNode create(ResourceType rt);
		public void reset();
	}

	public String toStringLong() {
		String ret =  name;
		if (domain != null) 
			ret += " in domain " + domain;
		if (image != null)
			ret += " with image " + image;
		return ret;
	}
	
	// Icon transformer for GUI
	public static class OrcaNodeIconTransformer implements Transformer<OrcaNode, Icon> {

		public Icon transform(OrcaNode node) {
			switch(node.getResourceType()) {
			case REQUEST:
				node.icon.add(new IconOutline(node.outline, Colors.REQUEST.getColor()));
				break;
			case MANIFEST:
				if (node.isActive())
					node.icon.add(new IconOutline(node.outline, Colors.ACTIVE.getColor()));
				else if (node.isFailed())
					node.icon.add(new IconOutline(node.outline, Colors.FAILED.getColor()));
				else
					node.icon.add(new IconOutline(node.outline, Colors.TICKETED.getColor()));
				break;
			case RESOURCE:
				node.icon.add(new IconOutline(node.outline, Colors.RESOURCE.getColor()));
				break;
			default:
			}
			return node.icon;
		}
	}
	
	// Icon shape transformer for GUI (to make sure icon clickable shape roughly matches the icon)
	public static class OrcaNodeIconShapeTransformer implements Transformer<OrcaNode, Shape> {
		//private static final int ICON_HEIGHT = 30;
		//private static final int ICON_WIDTH = 50;
		        public Shape transform(OrcaNode i) {
		        	return i.outline;
		            //return new Ellipse2D.Float(-ICON_WIDTH/2, -ICON_HEIGHT/2, ICON_WIDTH, ICON_HEIGHT);
		        }
		    }

	
	// check mark for selected nodes
	// boosted from JUNG Lens example
    public static class PickWithIconListener implements ItemListener {
        OrcaNodeIconTransformer imager;
        Icon checked;
        
        public PickWithIconListener(OrcaNodeIconTransformer imager) {
            this.imager = imager;
            checked = new Checkmark(Color.red);
        }

        public void itemStateChanged(ItemEvent e) {
            Icon icon = imager.transform((OrcaNode)e.getItem());
            if(icon != null && icon instanceof LayeredIcon) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    ((LayeredIcon)icon).add(checked);
                } else {
                    ((LayeredIcon)icon).remove(checked);
                }
            }
        }
    }
	
    private Shape getIconShape(LayeredIcon i) {
    	Shape s = FourPassImageShaper.getShape(i.getImage());
    	AffineTransform transform = 
                AffineTransform.getTranslateInstance(-i.getIconWidth()/2, -i.getIconHeight()/2);
    	s = transform.createTransformedShape(s);
    	return s;
    }
    
	public OrcaNode(String name) {
		super(name);
		this.addresses = new HashMap<OrcaLink, Pair<String>>();
		this.macAddresses = new HashMap<OrcaLink, String>();
		this.icon = new LayeredIcon(new ImageIcon(GUIUnifiedState.class.getResource(OrcaNodeEnum.CE.getIconName())).getImage());
		this.outline = getIconShape(this.icon);
		
	}

	// inherit some properties from parent
	public OrcaNode(String name, OrcaNode parent) {
		super(name);
		this.addresses = new HashMap<OrcaLink, Pair<String>>();
		this.macAddresses = new HashMap<OrcaLink, String>();
		this.icon = new LayeredIcon(new ImageIcon(GUIUnifiedState.class.getResource(OrcaNodeEnum.CE.getIconName())).getImage());
		this.outline = getIconShape(this.icon);
		this.domain = parent.getDomain();
		this.group = parent.getGroup();
		this.image = parent.getImage();
		this.url = parent.getUrl();
		this.nodeType = parent.getNodeType();
		this.dependencies = parent.getDependencies();
		this.state = parent.state;
	}
	
	/**
	 * only subclasses can set the icon
	 * @param name
	 * @param icon
	 */
	protected OrcaNode(String name, LayeredIcon icon) {
		super(name);
		this.addresses = new HashMap<OrcaLink, Pair<String>>();
		this.macAddresses = new HashMap<OrcaLink, String>();
		this.icon = icon;
		this.outline = getIconShape(icon);
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}
	
	public String getGroup() {
		return group;
	}
	
	public void setGroup(String d) {
		group = d;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public void setDomainWithGlobalReset(String d) {
		// reset reservation-level setting
		GUIUnifiedState.getInstance().resetDomainInReservation();
		domain = d;
	}
	
	public void setDomain(String d) {
		domain = d;
	}
	
	public void setNodeType(String t) {
		nodeType = t;
	}
	
	public String getNodeType() {
		return nodeType;
	}
	
	public void setMac(OrcaLink e, String mac) {
		if (e == null)
			return;
		if (mac == null) { 
			macAddresses.remove(e);
			return;
		}
		macAddresses.put(e, mac);
	}
	
	public String getMac(OrcaLink e) {
		if ((e == null) || (macAddresses.get(e) == null))
			return null;
		return macAddresses.get(e);
	}
	
	
	public void setIp(OrcaLink e, String addr, String nm) {
		if (e == null)
			return;
		if (addr == null) {
			addresses.remove(e);
			return;
		}
		if (nm == null)
			nm = NODE_NETMASK;
		addresses.put(e, new Pair<String>(addr, nm));
	}
	
	public String getIp(OrcaLink e) {
		if ((e == null) || (addresses.get(e) == null))
			return null;
		return addresses.get(e).getFirst();
	}
	
	public String getNm(OrcaLink e) {
		if ((e == null) || (addresses.get(e) == null))
			return null;
		return addresses.get(e).getSecond();
	}
	
	public void removeIp(OrcaLink e) {
		if (e == null)
			return;
		addresses.remove(e);
	}
	
	public void removeAllIps() {
		addresses.clear();
	}
	
	public void addDependency(OrcaNode n) {
		if (n != null) 
			dependencies.add(n);
	}
	
	public void removeDependency(OrcaResource n) {
		if (n != null)
			dependencies.remove(n);
	}
	
	public void clearDependencies() {
		dependencies = new HashSet<OrcaNode>();
	}
	
	public boolean isDependency(OrcaResource n) {
		if (n == null)
			return false;
		return dependencies.contains(n);
	}
	
	/**
	 * returns empty set if no dependencies
	 * @return
	 */
	public Set<String> getDependencyNames() { 
		Set<String> ret = new HashSet<String>();
		for(OrcaResource n: dependencies) 
			ret.add(n.getName());
		return ret;
	}
	
	public Set<OrcaNode> getDependencies() {
		return dependencies;
	}
	
	public void setPostBootScript(String s) {
		postBootScript = s;
	}
	
	public String getPostBootScript() {
		return postBootScript;
	}
	
	public String getInterfaceName(OrcaLink l) {
		if (l != null)
			return interfaces.get(l);
		return null;
	}
	
	public void setInterfaceName(OrcaLink l, String ifName) {
		if ((l == null) || (ifName == null))
			return;
		
		interfaces.put(l, ifName);
	}
	
	public void setManagementAccess(List<String> s) {
		managementAccess = s;
	}
	
	// all available access options
	public List<String> getManagementAccess() {
		return managementAccess;
	}
	
	// if ssh is available
	public String getSSHManagementAccess() {
		if (managementAccess == null)
			return null;
		for (String service: managementAccess) {
			if (service.startsWith("ssh://root")) {
				return service;
			}
		}
		return null;
	}
	
	public String getPortsList() {
		return openPorts;
	}
	
	public boolean setPortsList(String list) {
		
		if ((list == null) || (list.trim().length() == 0))
			return true;
		
		String chkRegex = "(\\s*\\d+\\s*)(,(\\s*\\d+\\s*))*";
		
		if (list.matches(chkRegex)) { 
			for(String port: list.split(",")) {
				int portI = Integer.decode(port.trim());
				if (portI > 65535)
					return false;
			}
			openPorts = list;
			return true;
		}
		return false;
	}
	
	/** 
	 * Create a detailed printout of properties
	 * @return
	 */
	public String getViewerText() {
		String viewText = "";
		viewText += "Node name: " + name;
		viewText += "\nReservation ID: " + reservationGuid;
		viewText += "\nNode reservation state: " + (state != null ? state : NOT_SPECIFIED);
		viewText += "\nReservation notice: " + (resNotice != null ? resNotice : NOT_SPECIFIED);
//		viewText += "\nNode Type: " + node.getNodeType();
//		viewText += "\nImage: " + node.getImage();
//		viewText += "\nDomain: " + domain;
		viewText += "\n\nPost Boot Script: \n" + (postBootScript == null ? NOT_SPECIFIED : postBootScript);
		viewText += "\n\nManagement access: \n";
		for (String service: getManagementAccess()) {
			if (service.startsWith("ssh")) {
				service = service.replaceAll("://", " " + " -i " +
						GUI.getInstance().getPreference(GUI.PrefsEnum.SSH_KEY) + " " +
						GUI.getInstance().getPreference(GUI.PrefsEnum.SSH_OPTIONS) + " ");
				service = service.replaceAll(":", " -p ");
			} 
			viewText += service + "\n";
		}
		if (getManagementAccess().size() == 0) {
			viewText += NOT_SPECIFIED + "\n";
		}
		viewText += "\n\nInterfaces: ";
		for(Map.Entry<OrcaLink, Pair<String>> e: addresses.entrySet()) {
			viewText += "\n\t" + e.getKey().getName() + ": " + e.getValue().getFirst() + "/" + e.getValue().getSecond() + " " + 
			(macAddresses.get(e.getKey()) != null ? macAddresses.get(e.getKey()) : "");
		}
		
		if (substrateInfo.size() > 0) {
			viewText += "\n\nSubstrate information: ";
			for(Map.Entry<String, String> e: substrateInfo.entrySet()) {
				viewText += "\n\t" + e.getKey() + ": " + e.getValue();
			}
		}
		return viewText;
	}
	
	/**
	 * Node factory for requests
	 * @author ibaldin
	 *
	 */
    public static class OrcaNodeFactory implements Factory<OrcaNode> {
        private INodeCreator inc = null;
        
        public OrcaNodeFactory(INodeCreator i) {
        	inc = i;
        }
        
        /**
         * Create a node or a cloud based on some setting
         */
        public OrcaNode create() {
        	if (inc == null)
        		return null;
        	synchronized(inc) {
        		if (GUIUnifiedState.getInstance().getGUIState() == GUIState.SUBMITTED) {
        			GUIUnifiedState.showAlreadySubmittedMessage();
        			throw new RuntimeException("Unable to create node");
        		}
        		if (GUIUnifiedState.getInstance().getGUIState() == GUIState.MANIFEST)
        			GUIUnifiedState.getInstance().setGUIState(GUIState.MANIFESTWITHMODIFY);
        		return inc.create(ResourceType.REQUEST);
        	}
        }       
    }

    /**
     * Substrate info is just an associative array. 
     * Describes some information about the substrate of the resource
     */
    public void setSubstrateInfo(String t, String o) {
    	substrateInfo.put(t, o);
    }
    
    public String getSubstrateInfo(String t) {
    	return substrateInfo.get(t);
    }
    
    //
    // Menus for the nodes
    //
	public static class RequestMenu extends JPopupMenu {
		public RequestMenu() {
			super("Node Menu");
			this.add(new DeleteVertexMenuItem<OrcaNode, OrcaLink>(GUIUnifiedState.getInstance()));
			this.addSeparator();
			this.add(new ImageDisplay());
			this.add(new DomainDisplay());
			this.add(new NodeTypeDisplay());
			this.addSeparator();
			this.add(new NodePropItem(GUI.getInstance().getFrame()));
			this.addSeparator();
			this.add(new NodeColorItem(GUI.getInstance().getFrame(), true));
		}
	}
	
	public static class ManifestMenu extends JPopupMenu {
		public ManifestMenu() {
			super("Node Menu");
			this.add(new ImageDisplay());
			this.add(new DomainDisplay());
			this.add(new NodeTypeDisplay());
			this.addSeparator();
			this.add(new DeleteVertexMenuItem<OrcaNode, OrcaLink>(GUIUnifiedState.getInstance()));
			this.add(new IncreaseByNodeGroupItem(GUI.getInstance().getFrame()));
			this.addSeparator();
			this.add(new NodeViewItem(GUI.getInstance().getFrame()));
			this.add(new NodePropertiesItem(GUI.getInstance().getFrame()));
	        this.add(new NodeInsertSSHKeyItem(GUI.getInstance().getFrame()));
			this.add(new NodeLoginItem(GUI.getInstance().getFrame()));
			this.addSeparator();
			this.add(new NodeColorItem(GUI.getInstance().getFrame(), false));
			if (GUI.getInstance().withSliceStitching()) {
				this.addSeparator();
				this.add(new PermitStitchingItem(GUI.getInstance().getFrame()));
				this.add(new RevokeStitchingItem(GUI.getInstance().getFrame()));
				this.add(new PerformStitchingItem(GUI.getInstance().getFrame()));
				this.add(new UndoStitchingItem(GUI.getInstance().getFrame()));
				this.add(new StitchPropertiesItem(GUI.getInstance().getFrame()));
			}
		}
	}
	
	
	public static class ResourceMenu extends JPopupMenu {
		public ResourceMenu() {
			super("Site Menu");
			this.add(new MultiDomainDisplay());
		}
	}
	
	private static JPopupMenu requestMenu, manifestMenu, resourceMenu;
	
	{
		requestMenu = new RequestMenu();
		manifestMenu = new ManifestMenu();
		resourceMenu = new ResourceMenu();
	}
	
	public JPopupMenu requestMenu() {
		return requestMenu;
	}
	
	public JPopupMenu manifestMenu() {
		return manifestMenu;
	}
	
	public JPopupMenu resourceMenu() {
		return resourceMenu;
	}
}