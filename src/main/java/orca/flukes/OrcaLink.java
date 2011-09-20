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

public class OrcaLink {
    private long bandwidth;
    private long latency;
    private String name;

    public OrcaLink(String name) {
        this.name = name;
    }
    
    public void setBandwidth(long bw) {
    	bandwidth = bw;
    }

    public void setLatency(long l) {
    	latency = l;
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
    
    @Override
    public String toString() {
        return name;
    }
    
    public static class OrcaLinkFactory implements Factory<OrcaLink> {
        private static int linkCount = 0;
        private long defaultBandwidth;
        private long defaultLatency;

        public OrcaLinkFactory() {            
        }
        
        public OrcaLink create() {
        	synchronized(this) {
        		String name;
        		do {
        			name = "Link" + linkCount++;
        		} while (!GUIRequestState.getInstance().checkUniqueLinkName(null, name));
        		OrcaLink link = new OrcaLink(name);
        		link.setBandwidth(defaultBandwidth);
        		link.setLatency(defaultLatency);
        		return link;
        	}
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
}
