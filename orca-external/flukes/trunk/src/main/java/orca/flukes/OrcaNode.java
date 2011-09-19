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
import java.awt.geom.Ellipse2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.LayeredIcon;
import edu.uci.ics.jung.visualization.renderers.Checkmark;

public class OrcaNode {

	public static final String NODE_NETMASK="32";
	protected String name;
	protected String image = null;
	protected String domain = null;
	// Pair<String> first is IP, second is Netmask
	protected HashMap<OrcaLink, Pair<String>> addresses;
	protected final LayeredIcon icon;
	protected final boolean amNode;
	// specific node type 
	protected String nodeType = null;
	// post-boot script
	protected String postBootScript = null;
	
	protected Set<OrcaNode> dependencies = new HashSet<OrcaNode>();
	
	// if cloud. 
	// TODO: probably need to have a class hierarchy here. 
	protected int nodeCount = 1;
	
	// Icon transformer for GUI
	public static class OrcaNodeIconTransformer implements Transformer<OrcaNode, Icon> {

		public Icon transform(OrcaNode node) {
			return node.icon;
		}
	}
	
	// Icon shape transformer for GUI (to make sure icon clickable shape roughly matches the icon)
	public static class OrcaNodeIconShapeTransformer implements Transformer<OrcaNode, Shape> {
//		        private final Shape[] styles = {
//		            new Rectangle(-20, -10, 40, 20),
//		            new Ellipse2D.Double(-25, -10, 50, 20),
//		            new Arc2D.Double(-30, -15, 60, 30, 30, 30,
//		                Arc2D.PIE) };
		        public Shape transform(OrcaNode i) {
		            return new Ellipse2D.Double(-25, -15, 50, 30);
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
	
	public OrcaNode(String name) {
		this.name = name;
		this.addresses = new HashMap<OrcaLink, Pair<String>>();
		this.icon = new LayeredIcon(new ImageIcon(GUIState.class.getResource(GUIState.NODE_ICON)).getImage());
		this.amNode = true;
	}

	public OrcaNode(String name, boolean amNode) {
		this.name = name;
		this.addresses = new HashMap<OrcaLink, Pair<String>>();
		this.amNode = amNode;
		if (amNode) 
			this.icon = new LayeredIcon(new ImageIcon(GUIState.class.getResource(GUIState.NODE_ICON)).getImage());
		else
			this.icon = new LayeredIcon(new ImageIcon(GUIState.class.getResource(GUIState.CLOUD_ICON)).getImage());
	}
	
	// is this a node or a cloud
	public boolean isNode() {
		return amNode;
	}
	
	public int getNodeCount() {
		return nodeCount;
	}
	
	public void setNodeCount(int nc) {
		if (nc > 1)
			nodeCount = nc;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}
	
	public String getDomain() {
		return domain;
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
	
	public void setIp(OrcaLink e, String addr, String nm) {
		if (e == null)
			return;
		if (addr == null)
			return;
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
	
	public void addDependency(OrcaNode n) {
		if (n != null) 
			dependencies.add(n);
	}
	
	public void removeDependency(OrcaNode n) {
		if (n != null)
			dependencies.remove(n);
	}
	
	public void clearDependencies() {
		dependencies = new HashSet<OrcaNode>();
	}
	
	public boolean isDependency(OrcaNode n) {
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
		for(OrcaNode n: dependencies) 
			ret.add(n.getName());
		return ret;
	}
	
	Set<OrcaNode> getDependencies() {
		return dependencies;
	}
	
	public void setPostBootScript(String s) {
		postBootScript = s;
	}
	
	public String getPostBootScript() {
		return postBootScript;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
    public static class OrcaNodeFactory implements Factory<OrcaNode> {
        private static int nodeCount = 0;
        private static int clusterCount = 0;
        private static OrcaNodeFactory instance = new OrcaNodeFactory();
        
        private OrcaNodeFactory() {            
        }
        
        public static OrcaNodeFactory getInstance() {
            return instance;
        }
        
        /**
         * Create a node or a cloud based on global GUI setting
         */
        public OrcaNode create() {
        	synchronized(instance) {
        		String name;
        		do {
        			if (GUIState.getInstance().nodesOrGroups)
        				name = "Node" + nodeCount++;
        			else
        				name = "NodeGroup" + clusterCount++;
        		} while (!GUIState.getInstance().checkUniqueNodeName(null, name));
        		return new OrcaNode(name, GUIState.getInstance().nodesOrGroups);
        	}
        }       
    }
    
}