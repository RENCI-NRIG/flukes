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

import orca.flukes.OrcaLink.ILinkCreator;
import edu.uci.ics.jung.graph.SparseMultigraph;

public class OrcaLinkCreator implements ILinkCreator {
    private static int linkCount = 0;
    private long defaultBandwidth = 10000000;
    private long defaultLatency = 50;
    private final SparseMultigraph<OrcaNode, OrcaLink> g;
    // by default links are topology
    private OrcaLinkType currentLinkType = OrcaLinkType.COLOR;
    
    public enum OrcaLinkType {
    	TOPO("Link"), COLOR("ColorLink");
    	
    	private String prefix;
    	private int linkCount = 0;
    	
    	
    	private OrcaLinkType(String n) {
    		prefix = n;
    	}
    	
    	public String getPrefix() {
    		return prefix;
    	}
    	
    	public int getNextCount() {
    		return linkCount++;
    	}
    	
    	public void resetCount() {
    		linkCount = 0;
    	}
    }
    
    // change the link type used by the creator
    public void setLinkType(OrcaLinkType olt) {
    	currentLinkType = olt;
    }
    
	public OrcaLinkCreator(SparseMultigraph<OrcaNode, OrcaLink> g) {
		this.g = g;
	}
	
	/**
	 * Check if the link name is unique
	 * @param nm
	 * @return
	 */
	public boolean checkUniqueLinkName(OrcaLink edge, String nm) {
		// check all edges in graph
		Collection<OrcaLink> edges = g.getEdges();
		for (OrcaLink e: edges) {
			// check that some other edge doesn't have this name
			if (edge != null) {
				if ((e != edge) && (e.getName().equals(nm)))
					return false;
			} else
				if (e.getName().equals(nm))
					return false;
		}
		return true;
	}

	@Override
	public OrcaLink create(String prefix) {
       	synchronized(this) {
    		String name;
    		do {
    			if (prefix != null)
    				name = prefix; 
    			else
    				name = "";
    			name += currentLinkType.getPrefix() + linkCount++;
    		} while (!checkUniqueLinkName(null, name));
    		OrcaLink link = null;
    		switch(currentLinkType) {
    		case TOPO:
        		link = new OrcaLink(name);
        		link.setBandwidth(defaultBandwidth);
        		link.setLatency(defaultLatency);
    			break;
    		case COLOR:
    			link = new OrcaColorLink(name);
    			break;
    		}
    		return link;
    	}
	}
	
	// Creates *only* TOPO links
	@Override
	public OrcaLink create(String nm, long bw) {
		String dispName = nm;
		if ((nm != null) && (nm.length() > 20)) {
			dispName = nm.substring(0, 20) + "...";
		}
		OrcaLink link = new OrcaLink(dispName);
		link.setRealName(nm);
		link.setBandwidth(bw);
		link.setLatency(defaultLatency);
		return link;
	}
	
	// reset counters for all types
	public void reset() {
		for(OrcaLinkType olt: OrcaLinkType.values()) {
			olt.resetCount();
		}
	}
	
	// reset counter for specific type
	public void reset(OrcaLinkType olt) {
		olt.resetCount();
	}
	
    public long getDefaultLatency() {
        return defaultLatency;
    }

    public void setDefaultLatency(long l) {
        defaultLatency = l;
    }

    public long getDefaultBandwidth() {
        return defaultBandwidth;
    }

    public void setDefaultBandwidth(long bw) {
        defaultBandwidth = bw;
    }   
}
