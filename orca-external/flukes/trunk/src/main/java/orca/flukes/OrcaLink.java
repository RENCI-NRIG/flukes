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

import org.apache.commons.collections15.Factory;

import edu.uci.ics.jung.graph.util.Pair;

public class OrcaLink implements OrcaResource {
    protected long bandwidth;
    protected long latency;
    protected String label = null;
    
    protected String name;
	// reservation state
	protected String state = null;
	// reservation notice
	protected String resNotice = null;
	protected boolean isResource = false;
	
	public boolean isResource() {
		return isResource;
	}
    
	public void setIsResource() {
		isResource = true;
	}
	
    public OrcaLink(String name) {
        this.name = name;
    }

    interface ILinkCreator {
    	public OrcaLink create();
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
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }       
    
	public void setState(String s) {
		state = s;
	}
	
	public String getState() {
		return state;
	}
	
	public void setReservationNotice(String n) {
		resNotice = n;
	}
    
	public String getReservationNotice() {
		return resNotice;
	}
	
    @Override
    public String toString() {
        return name;
    }
    
    /**
     * Get text for GUI viewer
     * @return
     */
    public String getViewerText() {
    	String viewText = "Link name: " + name;
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
    		viewText += "\nLabel: " + latency;
    	
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
        		return inc.create();
        	}
        }    
    }
    
    // link to broadcast
    public boolean linkToBroadcast() {
    	// if it is a link to broadcastlink, no editable properties
    	Pair<OrcaNode> pn = GUIRequestState.getInstance().getGraph().getEndpoints(this);
    	
    	if (pn == null)
    		return false;
    	
    	if ((pn.getFirst() instanceof OrcaCrossconnect) || 
    			(pn.getSecond() instanceof OrcaCrossconnect))
    		return true;
    	return false;
    }
    
    public void setSubstrateInfo(String t, String o) {
    	// FIXME:
    }
    
    public String getSubstrateInfo(String t) {
    	return null;
    }
}
