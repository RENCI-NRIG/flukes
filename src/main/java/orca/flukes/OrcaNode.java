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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.LayeredIcon;
import edu.uci.ics.jung.visualization.renderers.Checkmark;

public class OrcaNode {

	public static final String NODE_NETMASK="32";
	private String name;
	private String image = null;
	private String domain = null;
	// Pair<String> first is IP, second is Netmask
	private HashMap<OrcaLink, Pair<String>> addresses;
	private final LayeredIcon icon;
	private final boolean amNode;
	// if cloud. 
	// TODO: probably need to have a class hierarchy here. 
	private int nodeCount = 1;
	
	// Icon transformer for GUI
	public static class OrcaNodeIconTransformer implements Transformer<OrcaNode, Icon> {

		public Icon transform(OrcaNode node) {
			return node.icon;
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
        		if (GUIState.getInstance().nodesOrClusters)
        			name = "Node" + nodeCount++;
        		else
        			name = "Cluster" + clusterCount++;
        		return new OrcaNode(name, GUIState.getInstance().nodesOrClusters);
        	}
        }       
    }
    
}