package orca.flukes;

import org.apache.commons.collections15.Factory;

public class OrcaNode {

	private String name;
	private String image;
	
	public OrcaNode(String name) {
		this.name = name;
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
	
	@Override
	public String toString() {
		return name;
	}
	
    public static class OrcaNodeFactory implements Factory<OrcaNode> {
        private static int nodeCount = 0;
        private static OrcaNodeFactory instance = new OrcaNodeFactory();
        
        private OrcaNodeFactory() {            
        }
        
        public static OrcaNodeFactory getInstance() {
            return instance;
        }
        
        public OrcaNode create() {
            String name = "Node" + nodeCount++;
            OrcaNode v = new OrcaNode(name);
            return v;
        }       
    }
    
}