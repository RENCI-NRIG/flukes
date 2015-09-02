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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;

import javax.swing.JPopupMenu;

import orca.flukes.MouseMenus.BandwidthDisplay;
import orca.flukes.MouseMenus.EdgeColorItem;
import orca.flukes.MouseMenus.EdgePropItem;
import orca.flukes.MouseMenus.EdgeViewerItem;
import orca.flukes.MouseMenus.LabelDisplay;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.graph.util.Pair;

public class OrcaLink extends OrcaResource {
    protected long bandwidth;
    protected long latency;
    protected String label = null;
    protected String realName = null;
	
	// standard  transformers for edges
	protected static class LinkPaint implements Transformer<OrcaLink, Paint> {
	    public Paint transform(OrcaLink l) {
	    	if (l instanceof OrcaColorLink)
	    		return Color.BLUE;
	    	if ((l.getResourceType() == ResourceType.MANIFEST) && (l.isResource())) {
	    		if (OrcaResource.ORCA_ACTIVE.equalsIgnoreCase(l.getState())) {
	    			return Color.green;
	    		}
	    		if (OrcaResource.ORCA_FAILED.equalsIgnoreCase(l.getState())) {
	    			return Color.red;
	    		}
	    		return Color.gray;
	    	} else 
	    		return Color.black;
	    }
	};
	
	protected static class LinkStroke implements Transformer<OrcaLink, Stroke> {
		float dash[] = { 10.0f };
		public Stroke transform(OrcaLink l) {
			if (l instanceof OrcaColorLink) {
				return new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
						BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
			}
			return new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
		}
	}
    
    public OrcaLink(String name) {
        super(name);
    }

    public OrcaLink(OrcaLink ol) {
    	super(ol.name, ol.isResource());
    	bandwidth = ol.bandwidth;
    	latency = ol.latency;
    	label = ol.label;
    	realName = ol.realName;
    	state = ol.state;
    	resNotice = ol.resNotice;
    }
    
    
    interface ILinkCreator {
    	public OrcaLink create(String prefix, ResourceType rt);
    	public OrcaLink create(String nm, long bw, ResourceType rt);
    	public void reset();
    }
    
    public void setBandwidth(long bw) {
    	bandwidth = bw;
    }

    public void setLatency(long l) {
    	latency = l;
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
    
    public long getBandwidth() {
    	return bandwidth;
    }
    
    public long getLatency() {
    	return latency;
    }
    
    public void setRealName(String n) {
    	this.realName = n;
    }
    
    /**
     * Get text for GUI viewer
     * @return
     */
    public String getViewerText() {
    	String viewText = "Link name: " + (realName != null ? realName : name);
    	if (bandwidth == 0)
    		viewText += "\nBandwidth: unspecified";
    	else 
    		viewText += "\nBandwidth: " + bandwidth;
    	
    	if (latency == 0) 
    		viewText += "\nLatency: unspecified";
    	else
    		viewText += "\nLatency: " + latency;
    	
    	if (label == null) 
    		viewText += "\nLabel: unspecified";
    	else
    		viewText += "\nLabel: " + label;
    	
    	if (reservationGuid == null) 
    		viewText += "\nLink reservation ID: unspecified";
    	else
    		viewText += "\nLink reservation ID: " + reservationGuid;
    	
    	if (state == null)
    		viewText += "\nLink reservation state: unspecified";
    	else
    		viewText += "\nLink reservation state: " + state;
    		
    	if (resNotice == null)
    		viewText += "\nReservation notice: unspecified";
    	else
    		viewText += "\nReservation notice: " + resNotice;
    	
    	return viewText;
    }
    
    public static class OrcaLinkFactory implements Factory<OrcaLink> {
       private ILinkCreator inc = null;
        
        public OrcaLinkFactory(ILinkCreator i) {
        	inc = i;
        }
        
        public OrcaLink create() {
        	if (inc == null)
        		return null;
        	synchronized(inc) {
        		return inc.create(null, ResourceType.REQUEST);
        	}
        }    
    }
    
    // link to broadcast?
    public boolean linkToBroadcast() {
    	// if it is a link to broadcastlink, no editable properties
    	Pair<OrcaNode> pn = GUIUnifiedState.getInstance().getGraph().getEndpoints(this);
    	
    	if (pn == null)
    		return false;
    	
    	if ((pn.getFirst() instanceof OrcaCrossconnect) || 
    			(pn.getSecond() instanceof OrcaCrossconnect))
    		return true;
    	return false;
    }
    
    // link to shared storage?
    public boolean linkToSharedStorage() {
    	// if it is a link to broadcastlink, no editable properties
    	Pair<OrcaNode> pn = GUIUnifiedState.getInstance().getGraph().getEndpoints(this);
    	
    	if (pn == null)
    		return false;
    	
    	if (pn.getFirst() instanceof OrcaStorageNode) {
    		OrcaStorageNode snode = (OrcaStorageNode)pn.getFirst();
    		if (snode.getSharedNetwork())
    			return true;
    	}
    	
    	if (pn.getSecond() instanceof OrcaStorageNode) {
    		OrcaStorageNode snode = (OrcaStorageNode)pn.getSecond();
    		if (snode.getSharedNetwork())
    			return true;
    	}
    	return false;
    }
    
    
    public void setSubstrateInfo(String t, String o) {
    	// FIXME:
    }
    
    public String getSubstrateInfo(String t) {
    	return null;
    }
    
    //
    // Menus
    //
	public static class RequestMenu extends JPopupMenu {        
		// private JFrame frame; 
		public RequestMenu() {
			super("Edge Menu");
			// this.frame = frame;
			this.add(new DeleteEdgeMenuItem<OrcaNode, OrcaLink>(GUIUnifiedState.getInstance()));
			this.addSeparator();
			//this.add(new LatencyDisplay());
			this.add(new BandwidthDisplay());
			this.add(new LabelDisplay());
			this.addSeparator();
			this.add(new EdgePropItem(GUI.getInstance().getFrame()));     
			this.addSeparator();
			this.add(new EdgeColorItem(GUI.getInstance().getFrame(), true));
		}

	}

	public static class ManifestMenu extends JPopupMenu {        
		// private JFrame frame; 
		public ManifestMenu() {
			super("Edge Menu");
			//this.add(new LatencyDisplay());
			this.add(new BandwidthDisplay());
			this.add(new LabelDisplay());
			this.addSeparator();
			this.add(new EdgeViewerItem(GUI.getInstance().getFrame()));     
			this.addSeparator();
			this.add(new EdgeColorItem(GUI.getInstance().getFrame(), false));
		}
	}
	
	public static class ResourceMenu extends JPopupMenu {
		public ResourceMenu() {
			super("Link Menu");
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
