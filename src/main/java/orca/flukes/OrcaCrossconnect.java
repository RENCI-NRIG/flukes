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

import java.util.Collection;
import java.util.Map.Entry;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import orca.flukes.MouseMenus.DomainDisplay;
import orca.flukes.MouseMenus.NodeColorItem;
import orca.flukes.MouseMenus.NodePropItem;
import orca.flukes.MouseMenus.NodeTypeDisplay;
import orca.flukes.MouseMenus.NodeViewItem;
import orca.flukes.MouseMenus.PerformStitchingItem;
import orca.flukes.MouseMenus.PermitStitchingItem;
import orca.flukes.MouseMenus.RevokeStitchingItem;
import orca.flukes.MouseMenus.StitchPropertiesItem;
import orca.flukes.MouseMenus.UndoStitchingItem;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.LayeredIcon;

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
public class OrcaCrossconnect extends OrcaNode {
	private static final int DEFAULT_CROSSCONNECT_BANDWIDTH = 10000000;
	private static final String BROADCAST_LINK = "Broadcast link";
	// vlan or other path label
	protected String label = null;
	protected long bandwidth;
	
	
	public OrcaCrossconnect(String name) {
		super(name, 
				new LayeredIcon(new ImageIcon(GUIUnifiedState.class.getResource(OrcaNodeEnum.CROSSCONNECT.getIconName())).getImage()));
		bandwidth = DEFAULT_CROSSCONNECT_BANDWIDTH;
	}

	public void setLabel(String l) {
    	if ((l != null) && l.length() > 0)
    		label = l;
    	else
    		label = null;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setBandwidth(long b) {
		bandwidth = b;
	}
	
	public long getBandwidth() {
		return bandwidth;
	}
	
	/** 
	 * Create a detailed printout of properties
	 * @return
	 */
	@Override
	public String getViewerText() {
		String viewText = "";
		viewText += "Node name: " + name;
		viewText += "\nReservation ID: " + reservationGuid;
		viewText += "\nNode reservation state: " + state;
		viewText += "\nReservation notice: " + (resNotice != null ? resNotice : NOT_SPECIFIED);
		if (label != null)
			viewText += "\nLabel/Tag: " + label;
		viewText += "\nBandwidth: " + bandwidth;
		if (interfaces.size() > 0) {
			viewText += "\nInterfaces: ";
			for(Entry<OrcaLink, String> e: interfaces.entrySet()) {
				viewText += "\n    " + e.getKey().getName() + " : " + e.getValue();
			}
		}
		return viewText;
	}
	
	// is this crossconnect linked to shared storage?
    public boolean linkToSharedStorage() {
    	
    	Collection<OrcaLink> iLinks = GUIUnifiedState.getInstance().getGraph().getIncidentEdges(this);
		for(OrcaLink l: iLinks) {
			Pair<OrcaNode> pn = GUIUnifiedState.getInstance().getGraph().getEndpoints(l);
			OrcaResource n = null;
			// find the non-crossconnect side
			if (!(pn.getFirst() instanceof OrcaCrossconnect))
				n = pn.getFirst();
			else if (!(pn.getSecond() instanceof OrcaCrossconnect))
				n = pn.getSecond();
			
			if (n == null) 
				continue;
			
			if (n instanceof OrcaStorageNode) {
				OrcaStorageNode snode = (OrcaStorageNode)n;
				if (snode.getSharedNetwork())
					return true;
			}
		}
		return false;
    }
    
    //
    // Menus for the nodes
    //
	public static class RequestMenu extends JPopupMenu {
		public RequestMenu() {
			super("Broadcast Link Menu");
			this.add(new DeleteVertexMenuItem<OrcaNode, OrcaLink>(GUIUnifiedState.getInstance()));
			this.addSeparator();
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
			super("Broadcast Link Menu");
			this.add(new DeleteVertexMenuItem<OrcaNode, OrcaLink>(GUIUnifiedState.getInstance()));
			this.addSeparator();
			this.add(new DomainDisplay());
			this.add(new NodeTypeDisplay());
			this.addSeparator();
			this.add(new NodeViewItem(GUI.getInstance().getFrame()));
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
