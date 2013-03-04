package orca.flukes.irods;

// interface to iRods
public interface IIRods {

	public String loadRequest(String name) throws IRodsException;
	
	public String loadManifest(String name) throws IRodsException;
	
	public void saveRequest(String name, String request) throws IRodsException;
	
	public void saveManifest(String name, String manifest) throws IRodsException;
}
