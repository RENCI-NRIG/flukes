package orca.flukes;

import org.apache.commons.collections15.Factory;

public class OrcaNode {

	private String name;
	private boolean packetSwitchCapable;
	private boolean tdmSwitchCapable;
	
	public OrcaNode(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isPacketSwitchCapable() {
		return packetSwitchCapable;
	}

	public void setPacketSwitchCapable(boolean packetSwitchCapable) {
		this.packetSwitchCapable = packetSwitchCapable;
	}

	public boolean isTdmSwitchCapable() {
		return tdmSwitchCapable;
	}

	public void setTdmSwitchCapable(boolean tdmSwitchCapable) {
		this.tdmSwitchCapable = tdmSwitchCapable;
	}

	@Override
	public String toString() {
		return name;
	}
	
    public static class OrcaNodeFactory implements Factory<OrcaNode> {
        private static int nodeCount = 0;
        private static boolean defaultPSC = false;
        private static boolean defaultTDM = true;
        private static OrcaNodeFactory instance = new OrcaNodeFactory();
        
        private OrcaNodeFactory() {            
        }
        
        public static OrcaNodeFactory getInstance() {
            return instance;
        }
        
        public OrcaNode create() {
            String name = "Node" + nodeCount++;
            OrcaNode v = new OrcaNode(name);
            v.setPacketSwitchCapable(defaultPSC);
            v.setTdmSwitchCapable(defaultTDM);
            return v;
        }        

        public static boolean isDefaultPSC() {
            return defaultPSC;
        }

        public static void setDefaultPSC(boolean aDefaultPSC) {
            defaultPSC = aDefaultPSC;
        }

        public static boolean isDefaultTDM() {
            return defaultTDM;
        }

        public static void setDefaultTDM(boolean aDefaultTDM) {
            defaultTDM = aDefaultTDM;
        }
    }
    
}