package orca.flukes.irods;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

import orca.flukes.GUI;
import orca.flukes.GUIManifestState;
import orca.flukes.GUI.PrefsEnum;

// deal with irods using pre-installed icommands
public class IRodsICommands implements IIRods {
	private final String iput, iget;
	
	public IRodsICommands() {
		iput = GUI.getInstance().getPreference(PrefsEnum.IRODS_ICOMMANDS_PATH) + System.getProperty("file.separator") + "iput";
		iget = GUI.getInstance().getPreference(PrefsEnum.IRODS_ICOMMANDS_PATH) + System.getProperty("file.separator") + "iget";
	}
	
	@Override
	public String loadManifest(String name) throws IRodsException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String loadRequest(String name) throws IRodsException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveManifest(String name, String manifest)
			throws IRodsException {
		if (name == null)
			throw new IRodsException("Unable to create irods file with name (check irods.file.names property)" + name);
		
		if (manifest == null)
			throw new IRodsException("Unable to save empty manifest into iRods");
		
		// save manifest into temp file, then push to irods
		File tFile = null;
		try{
			tFile = File.createTempFile("manifest", null);
			FileOutputStream fsw = new FileOutputStream(tFile);
			OutputStreamWriter out = new OutputStreamWriter(fsw, "UTF-8");
			out.write(manifest);
			out.close();
		} catch(FileNotFoundException e) {
			;
		} catch(UnsupportedEncodingException ex) {
			;
		} catch(IOException ey) {
			throw new IRodsException("Unable to save manifest temp file due to: " + ey.getMessage());
		} 

		if (tFile == null)
			throw new IRodsException("Unable to save manifest temp file");
		
		String command = iput + " " + tFile.getPath() + " " + name;
		Runtime rt = Runtime.getRuntime();   
		try {
			rt.exec(command);
		} catch (IOException e) {
			throw new IRodsException("Unable to save manifest to irods: " + e.getMessage());
		}
	}

	@Override
	public void saveRequest(String name, String request) throws IRodsException {
		// TODO Auto-generated method stub
		
	}
	
	private static String START_CONST = "${";
	private static String END_CONST = "}";
	
	public static String substituteManifestName() {
		// collect the properties
		Properties p = new Properties();
		
		String sName = GUIManifestState.getInstance().getSliceName();
		if (sName == null)
			return null;
		p.setProperty("slice.name", GUIManifestState.getInstance().getSliceName());
		Date date = new Date();
		p.setProperty("date", date.toString().replace(" ", "-"));
		p.setProperty("irods.format", GUI.getInstance().getPreference(PrefsEnum.IRODS_FORMAT));
		
		return substituteManifestName_(p);
	}
	
	// generate a name of the irods manifest based on pattern in a property
	private static String substituteManifestName_(Properties props) {
		String value = GUI.getInstance().getPreference(PrefsEnum.IRODS_FILE_NAMES);

		// Get the index of the first constant, if any
		int beginIndex = 0;
		int startName = value.indexOf(START_CONST, beginIndex);

		while (startName != -1) {
			int endName = value.indexOf(END_CONST, startName);
			if (endName == -1) {
				// Terminating symbol not found
				// Return the value as is
				return value;
			}

			String constName = value.substring(startName + 2, endName);
			String constValue = props.getProperty(constName);

			if (constValue == null) {
				// Property name not found
				// Return the value as is
				return value;
			}

			// Insert the constant value into the
			// original property value
			String newValue = (startName>0) ? value.substring(0, startName) : "";
			newValue += constValue;

			// Start checking for constants at this index
			beginIndex = newValue.length();

			// Append the remainder of the value
			newValue += value.substring(endName+1);

			value = newValue;

			// Look for the next constant
			startName = value.indexOf(START_CONST, beginIndex);
		}
		return value;
	}

	
	public static void main(String[] argv) {
		
		IRodsICommands irc = new IRodsICommands();
		Properties p = new Properties();
		
		p.setProperty("slice.name", "ilias-slice");
		Date date = new Date();
		p.setProperty("date", date.toString().replace(" ", "-"));
		p.setProperty("irods.format", "ndl");
		System.out.println(irc.substituteManifestName_(p));
		
	}
}
