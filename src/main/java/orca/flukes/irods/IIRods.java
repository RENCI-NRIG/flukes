package orca.flukes.irods;

// interface to iRods
public interface IIRods {

	public String loadFile(String name) throws IRodsException;
	
	public void saveFile(String name, String manifest) throws IRodsException;
}
