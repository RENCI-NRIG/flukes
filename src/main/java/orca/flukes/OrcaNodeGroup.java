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

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import orca.flukes.GUI.PrefsEnum;
import orca.flukes.MouseMenus.DomainDisplay;
import orca.flukes.MouseMenus.ImageDisplay;
import orca.flukes.MouseMenus.IncreaseByNodeGroupItem;
import orca.flukes.MouseMenus.MultiDomainDisplay;
import orca.flukes.MouseMenus.NodeColorItem;
import orca.flukes.MouseMenus.NodeLoginItem;
import orca.flukes.MouseMenus.NodePropItem;
import orca.flukes.MouseMenus.NodeTypeDisplay;
import orca.flukes.MouseMenus.NodeViewItem;
import orca.flukes.OrcaNode.ManifestMenu;
import orca.flukes.OrcaNode.RequestMenu;
import orca.flukes.OrcaNode.ResourceMenu;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.LayeredIcon;

public class OrcaNodeGroup extends OrcaNode {
	Pair<String> internalVlanAddress = null;
	protected int nodeCount = 1;
	protected boolean splittable = false;
	// No more internal vlans - use broadcast links instead
	//protected boolean internalVlan = false;
	//protected long internalVlanBw = 0;
	//protected String internalVlanLabel = null;

	public OrcaNodeGroup(String name) {
		super(name, 
				new LayeredIcon(new ImageIcon(GUIRequestState.class.getResource(OrcaNodeEnum.NODEGROUP.getIconName())).getImage()));
	}

	public int getNodeCount() {
		return nodeCount;
	}
	
	public void setNodeCount(int nc) {
		if (nc >= 1)
			nodeCount = nc;
	}
	
	/**
	 * Node groups can have an internal bus with IP address
	 * @param addr
	 * @param nm
	 */
	public void setInternalIp(String addr, String nm) {
		if (addr == null)
			return;
		if (nm == null)
			nm = NODE_NETMASK;
		internalVlanAddress = new Pair<String>(addr, nm);
	}
	
	public String getInternalIp() {
		if (internalVlanAddress != null)
			return internalVlanAddress.getFirst();
		return null;
	}
	
	public String getInternalNm() {
		if (internalVlanAddress != null)
			return internalVlanAddress.getSecond();
		return null;
	}
	
	public void removeInternalIp() {
		internalVlanAddress = null;
	}
	
	public void setSplittable(boolean f) {
		splittable = f;
	}
	
	public boolean getSplittable() {
		return splittable;
	}
	
    //
    // Menus for the node groups
    //
	public static class RequestMenu extends JPopupMenu {
		public RequestMenu() {
			super("Node Group Menu");
			this.add(new DeleteVertexMenuItem<OrcaNode, OrcaLink>(GUIRequestState.getInstance()));
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
			super("Node Group Menu");
			this.add(new ImageDisplay());
			this.add(new DomainDisplay());
			this.add(new NodeTypeDisplay());
			this.addSeparator();
			this.add(new NodeViewItem(GUI.getInstance().getFrame()));
			this.addSeparator();
			this.add(new NodeColorItem(GUI.getInstance().getFrame(), false));
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
