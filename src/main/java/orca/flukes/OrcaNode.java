package orca.flukes;

import java.util.HashMap;

import org.apache.commons.collections15.Factory;

public class OrcaNode {

	private String name;
	private String image = null;
	private String domain = null;
	private HashMap<OrcaLink, String> addresses;
	
	public OrcaNode(String name) {
		this.name = name;
		this.addresses = new HashMap<OrcaLink, String>();
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
	
	public String getDomain() {
		return domain;
	}
	
	public void setDomain(String d) {
		domain = d;
	}
	
	public void setIp(OrcaLink e, String addr) {
		if (e == null)
			return;
		addresses.put(e, addr);
	}
	
	public String getIp(OrcaLink e) {
		if (e == null)
			return null;
		return addresses.get(e);
	}
	
	public void removeIp(OrcaLink e) {
		if (e == null)
			return;
		addresses.remove(e);
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