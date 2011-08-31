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
            String name = "Link" + linkCount++;
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
