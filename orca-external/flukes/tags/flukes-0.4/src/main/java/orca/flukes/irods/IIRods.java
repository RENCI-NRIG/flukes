package orca.flukes.irods;

// interface to iRods
public interface IIRods {

	public String loadFile(String name) throws IRodsException;
	
	public String saveFile(String name, String content) throws IRodsException;
}
