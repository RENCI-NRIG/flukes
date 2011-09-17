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
        private static long defaultBandwidth;
        private static long defaultLatency;

        private static OrcaLinkFactory instance = new OrcaLinkFactory();
        
        private OrcaLinkFactory() {            
        }
        
        public static OrcaLinkFactory getInstance() {
            return instance;
        }
        
        public OrcaLink create() {
        	String name;
        	do {
        		name = "Link" + linkCount++;
        	} while (!GUIState.getInstance().checkUniqueLinkName(null, name));
            OrcaLink link = new OrcaLink(name);
            link.setBandwidth(defaultBandwidth);
            link.setLatency(defaultLatency);
            return link;
        }    

        public static long getDefaultLatency() {
            return defaultLatency;
        }

        public static void setDefaultLatency(long l) {
            defaultLatency = l;
        }

        public static long getDefaultBandwidth() {
            return defaultBandwidth;
        }

        public static void setDefaultBandwidth(long bw) {
            defaultBandwidth = bw;
        }   
    }  
}
