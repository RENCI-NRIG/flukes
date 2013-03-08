package orca.flukes.irods;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import orca.flukes.GUI;
import orca.flukes.GUIManifestState;
import orca.flukes.GUI.PrefsEnum;
import orca.flukes.util.SystemExecutor;

// deal with irods using pre-installed icommands
public class IRodsICommands implements IIRods {
	private final String iput, iget, imkdir;
	
	public IRodsICommands() {
		imkdir = GUI.getInstance().getPreference(PrefsEnum.IRODS_ICOMMANDS_PATH) + System.getProperty("file.separator") + "imkdir";
		iput = GUI.getInstance().getPreference(PrefsEnum.IRODS_ICOMMANDS_PATH) + System.getProperty("file.separator") + "iput";
		iget = GUI.getInstance().getPreference(PrefsEnum.IRODS_ICOMMANDS_PATH) + System.getProperty("file.separator") + "iget";
	}
	

	@Override
	public String loadFile(String name) throws IRodsException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	/**
	 * Save file into irods
	 * @param name - name of irods file
	 * @param content - file contents
	 */
	public String saveFile(String name, String content)
			throws IRodsException {
		if (name == null)
			throw new IRodsException("Unable to create irods file with name (check irods.file.names property)" + name);
		
		if (content == null)
			throw new IRodsException("Unable to save empty manifest into iRods");
		
		// save manifest into temp file, then push to irods
		File tFile = null;
		try{
			tFile = File.createTempFile("manifest", null);
			FileOutputStream fsw = new FileOutputStream(tFile);
			OutputStreamWriter out = new OutputStreamWriter(fsw, "UTF-8");
			out.write(content);
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
		
		runIMkdir(name);
		
		// iput
		ArrayList<String> myCommand = new ArrayList<String>();

		myCommand.add(iput);
		myCommand.add(tFile.getPath());
		myCommand.add(name);
		
		String ret = executeIRodsCommand(myCommand, null);
	
		return ret;
	}
	
	private String executeIRodsCommand(List<String> cmd, Properties env) throws IRodsException {
		SystemExecutor se = new SystemExecutor();
		
		String response;
		try {
			response = se.execute(cmd, env, GUI.getInstance().getPreference(PrefsEnum.IRODS_ICOMMANDS_PATH), (Reader)null);
		} catch (RuntimeException re) {
			throw new IRodsException("Error executing icommand: " + re);
 		}
		return response;
	}
	
	private static String START_CONST = "${";
	private static String END_CONST = "}";
	
	public static String substituteManifestName() {
		return substituteName(GUI.getInstance().getPreference(PrefsEnum.IRODS_MANIFEST_TEMPLATE));
	}
	
	public static String substituteRequestName() {
		return substituteName(GUI.getInstance().getPreference(PrefsEnum.IRODS_REQUEST_TEMPLATE));
	}
	
	public static String substituteName(String template) {
		// collect the properties
		Properties p = new Properties();
		
		String sName = GUIManifestState.getInstance().getSliceName();
		if (sName == null)
			return null;
		p.setProperty("slice.name", GUIManifestState.getInstance().getSliceName());
		Date date = new Date();
		p.setProperty("date", date.toString().replace(" ", "-"));
		p.setProperty("irods.format", GUI.getInstance().getPreference(PrefsEnum.IRODS_FORMAT));
		
		return substituteManifestName_(template, p);
	}
	
	private void runIMkdir(String name) {
		// imkdir
		List<String> myCommand = new ArrayList<String>();
		
		myCommand.add(imkdir);
		myCommand.add("-p");
		
		int ind = name.indexOf('/');
		String subdir = null;
		while(ind > 0) {
			subdir = name.substring(0, ind);
			ind = name.indexOf('/', ind + 1);
		}
		myCommand.add(subdir);
		// need to mkdir for a series of subdirs
		
		try {
			executeIRodsCommand(myCommand, null);
		} catch(IRodsException ie1) {
			; // allow mkdir to fail
		}
	}
	
	// generate a name of the irods manifest based on pattern in a property
	private static String substituteManifestName_(String pattern, Properties props) {

		// Get the index of the first constant, if any
		int beginIndex = 0;
		int startName = pattern.indexOf(START_CONST, beginIndex);

		while (startName != -1) {
			int endName = pattern.indexOf(END_CONST, startName);
			if (endName == -1) {
				// Terminating symbol not found
				// Return the value as is
				return pattern;
			}

			String constName = pattern.substring(startName + 2, endName);
			String constValue = props.getProperty(constName);

			if (constValue == null) {
				// Property name not found
				// Return the value as is
				return pattern;
			}

			// Insert the constant value into the
			// original property value
			String newValue = (startName>0) ? pattern.substring(0, startName) : "";
			newValue += constValue;

			// Start checking for constants at this index
			beginIndex = newValue.length();

			// Append the remainder of the value
			newValue += pattern.substring(endName+1);

			pattern = newValue;

			// Look for the next constant
			startName = pattern.indexOf(START_CONST, beginIndex);
		}
		return pattern;
	}

	
	public static void main(String[] argv) {
		
		IRodsICommands irc = new IRodsICommands();
		Properties p = new Properties();
		
		p.setProperty("slice.name", "ilias-slice");
		Date date = new Date();
		p.setProperty("date", date.toString().replace(" ", "-"));
		p.setProperty("irods.format", "ndl");
		System.out.println(irc.substituteManifestName_(GUI.getInstance().getPreference(PrefsEnum.IRODS_MANIFEST_TEMPLATE), p));
		
	}
}
