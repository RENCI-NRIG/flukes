package orca.flukes.ndl;

import java.util.List;

import orca.flukes.GUI;
import orca.flukes.GUIUnifiedState;
import orca.flukes.OrcaLink;
import orca.flukes.OrcaNode;
import orca.flukes.OrcaResource;
import orca.ndl.NdlException;
import orca.ndl.NdlGenerator;

import com.hp.hpl.jena.ontology.Individual;

import edu.uci.ics.jung.graph.SparseMultigraph;

/**
 * Generate a modify request
 * @author ibaldin
 *
 */
public class ModifySaver {
	private NdlGenerator ngen = null;
	private Individual modRes = null;
	private String outputFormat = null;
	
	public  ModifySaver(String nsGuid) throws NdlException {
		ngen = new NdlGenerator(nsGuid, GUI.logger(), true);

		String nm = (nsGuid == null ? "my-modify" : nsGuid + "/my-modify");

		modRes = ngen.declareModifyReservation(nm);
	}
	
	public void setOutputFormat(String of) {
		outputFormat = of;
	}
	
	private String getFormattedOutput(String oFormat) {
		if (ngen == null)
			return null;
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
	 * Add a count of nodes to a group
	 * @param groupUrl
	 * @param count
	 */
	public void addNodesToGroup(String groupUrl, Integer count) throws NdlException {
		ngen.declareModifyElementNGIncreaseBy(modRes, groupUrl, count);
	}
	
	/**
	 * Remove a specific node from a specific group
	 * @param groupUrl
	 * @param nodeUrl
	 */
	public void removeNodeFromGroup(String groupUrl, String nodeUrl) throws NdlException {
		ngen.declareModifyElementNGDeleteNode(modRes, groupUrl, nodeUrl);
	}

	/**
	 * Return modify request in specified format
	 * @return
	 */
	public String createModifyRequest(SparseMultigraph<OrcaNode, OrcaLink> g, String nsGuid, List<OrcaResource> deleted, List<GUIUnifiedState.GroupModifyRecord> modifiedGroups) {
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
