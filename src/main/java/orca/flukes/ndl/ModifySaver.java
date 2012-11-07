package orca.flukes.ndl;

import orca.flukes.GUI;
import orca.ndl.NdlException;
import orca.ndl.NdlGenerator;

import com.hp.hpl.jena.ontology.Individual;
import com.hyperrealm.kiwi.ui.dialog.ExceptionDialog;

/**
 * Generate a modify request
 * @author ibaldin
 *
 */
public class ModifySaver {
	private NdlGenerator ngen = null;
	private Individual modRes = null;
	private String outputFormat = null;
	
	private ModifySaver() {
	}
	
	private static ModifySaver instance = null;
	
	public static ModifySaver getInstance() {
		if (instance == null)
			instance = new ModifySaver();
		return instance;
	}
	
	public void setOutputFormat(String of) {
		outputFormat = of;
	}
	
	private String getFormattedOutput(String oFormat) {
		if (oFormat == null)
			return getFormattedOutput(RequestSaver.defaultFormat);
		if (oFormat.equals(RequestSaver.RDF_XML_FORMAT)) 
			return ngen.toXMLString();
		else if (oFormat.equals(RequestSaver.N3_FORMAT))
			return ngen.toN3String();
		else if (oFormat.equals(RequestSaver.DOT_FORMAT)) {
			return ngen.getGVOutput();
		}
		else
			return getFormattedOutput(RequestSaver.defaultFormat);
	}

	/**
	 * Create a modify request in a specific namespace (null is allowed - GUID will be used)
	 * This call is optional. Calling addNodesToGroup and removeNodeFromGroup will automatically
	 * make this call if it has not been made.
	 * @param nsGuid
	 */
	public void createModifyRequest(String nsGuid) {
		// this should never run in parallel anyway
		synchronized(instance) {
			try {
				ngen = new NdlGenerator(nsGuid, GUI.logger(), true);
				
				String nm = (nsGuid == null ? "my-modify" : nsGuid + "/my-modify");
				
				modRes = ngen.declareModifyReservation(nm);
				
			} catch (Exception e) {
				ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
				ed.setLocationRelativeTo(GUI.getInstance().getFrame());
				ed.setException("Exception encountered while converting graph to NDL-OWL: ", e);
				ed.setVisible(true);
				return;
			} 
		}
	}
	
	/**
	 * Add a count of nodes to a group
	 * @param groupUrl
	 * @param count
	 */
	public void addNodesToGroup(String groupUrl, Integer count) {
		if (ngen == null)
			createModifyRequest(null);
		try {
			ngen.declareModifyElementNGIncreaseBy(modRes, groupUrl, count);
		} catch (NdlException e) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while converting graph to NDL-OWL: ", e);
			ed.setVisible(true);
			return;
		}
	}
	
	/**
	 * Remove a specific node from a specific group
	 * @param groupUrl
	 * @param nodeUrl
	 */
	public void removeNodeFromGroup(String groupUrl, String nodeUrl) {
		if (ngen == null)
			createModifyRequest(null);
		try {
			ngen.declareModifyElementNGDeleteNode(modRes, groupUrl, nodeUrl);
		} catch (NdlException e) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while converting graph to NDL-OWL: ", e);
			ed.setVisible(true);
			return;
		}
	}
	
	/**
	 * Return modify request in specified format
	 * @return
	 */
	public String getModifyRequest() {
		return getFormattedOutput(outputFormat);
	}
	
	/**
	 * clear up the modify saver
	 */
	public void clear() {
		if (ngen != null) {
			ngen.done();
			ngen = null;
		}
	}
	
}
