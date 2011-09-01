package orca.flukes;

import java.io.File;
import java.io.FileWriter;

import orca.ndl.NdlException;
import orca.ndl.NdlGenerator;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.Individual;
import com.hyperrealm.kiwi.ui.dialog.ExceptionDialog;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.Pair;

public class GraphSaver {
	private static final String DOT_FORMAT = "DOT";
	private static final String N3_FORMAT = "N3";
	public static final String RDF_XML_FORMAT = "RDF-XML";

	private static GraphSaver instance = null;
	private NdlGenerator ngen = null;
	private Individual reservation = null;
	
	public static GraphSaver getInstance() {
		if (instance == null)
			instance = new GraphSaver();
		return instance;
	}
	
	private String getFormattedOutput(NdlGenerator ng, String oFormat) {
		if (oFormat.equals(RDF_XML_FORMAT)) 
			return ng.toXMLString();
		else if (oFormat.equals(N3_FORMAT))
			return ng.toN3String();
		else if (oFormat.equals(DOT_FORMAT)) {
			return ng.getGVOutput();
		}
		else
			return ng.toString();
	}
	
	/**
	 * Link node to edge, create interface and process IP address 
	 * @param n
	 * @param e
	 * @param edgeI
	 * @param gi - is global image set?
	 * @throws NdlException
	 */
	private void processNodeAndLink(OrcaNode n, OrcaLink e, Individual edgeI, boolean gi) throws NdlException {
		Individual intI = ngen.declareInterface(e.getName()+"-"+n.getName());
		ngen.addInterfaceToIndividual(intI, edgeI);
		Individual ni = ngen.getRequestIndividual(n.getName());
		ngen.addInterfaceToIndividual(intI, ni);
		
		// see if there is an IP address for this link on this node
		if (n.getIp(e) != null) {
			// create IP object, attach to interface
			ngen.addIPToIndividual(n.getIp(e), intI);
		}
		// if no global image is set and a local image is set, add it to node
		if (!gi && (n.getImage() != null)) {
			// check if image is set in this node
			OrcaImage im = GUIState.getInstance().definedImages.get(n.getImage());
			if (im != null) {
				Individual imI = ngen.declareVMImage(im.getUrl().toString(), im.getHash(), im.getShortName());
				ngen.addVMImageToIndividual(imI, ni);
			}
		}
	}
	
	/**
	 * Save graph using NDL
	 * @param f
	 * @param g
	 */
	public void saveGraph(File f, SparseMultigraph<OrcaNode, OrcaLink> g) {
		// this should never run in parallel anyway
		synchronized(instance) {
			try {
//				if (!f.canWrite()) {
//					KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame(), "File error", true);
//					kmd.setMessage("Unable to save to file " + f.getAbsolutePath());
//					kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
//					kmd.setVisible(true);
//					return;
//				}
//				// non-modal to allow saving to proceed
//				ProgressDialog pd = new ProgressDialog(GUI.getInstance().getFrame(), false);
//				pd.setMessage("Saving graph...");
//				pd.setLocationRelativeTo(GUI.getInstance().getFrame());
//				pd.setVisible(true);
//				pd.setProgress(10);
				
				ngen = new NdlGenerator(Logger.getLogger(this.getClass().getCanonicalName()));
			
				if ((GUIState.getInstance().resStart != null) && (GUIState.getInstance().resEnd != null))
					reservation = ngen.declareReservation(GUIState.getInstance().resStart, 
							GUIState.getInstance().resEnd);
				else
					reservation = ngen.declareReservation();
				
				// shove invidividual nodes onto the reservation
				for (OrcaNode n: GUIState.getInstance().g.getVertices()) {
					Individual ni = ngen.declareServer(n.getName());
					ngen.addResourceToReservation(reservation, ni);
				}
				
				// decide whether we a global image
				boolean globalImage = false;
				
				if (GUIState.getInstance().resImageName != null) {
					// there is a global image (maybe)
					OrcaImage im = GUIState.getInstance().definedImages.get(GUIState.getInstance().resImageName);
					if (im != null) {
						// definitely an global image - attach it to the reservation
						globalImage = true;
						// TODO: check for zero length
						Individual imI = ngen.declareVMImage(im.getUrl().toString(), im.getHash(), im.getShortName());
						ngen.addVMImageToIndividual(imI, reservation);
					}
				}
				
				if (GUIState.getInstance().g.getEdgeCount() == 0) {
					// a bunch of disconnected nodes, no IP addresses 
					
				} else {
					// edges, nodes, IP addresses oh my!
					for (OrcaLink e: GUIState.getInstance().g.getEdges()) {
						Individual ei = ngen.declareNetworkConnection(e.getName());
						ngen.addResourceToReservation(reservation, ei);

						ngen.addBandwidthToConnection(ei, e.getBandwidth());

						Pair<OrcaNode> pn = GUIState.getInstance().g.getEndpoints(e);
						processNodeAndLink(pn.getFirst(), e, ei, globalImage);
						processNodeAndLink(pn.getSecond(), e, ei, globalImage);
					}
				}
				
				// save the contents
				String res = getFormattedOutput(ngen, N3_FORMAT);
				FileWriter fsw = new FileWriter(f);
				fsw.write(res);
				fsw.close();
			} catch (Exception e) {
				ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
				ed.setLocationRelativeTo(GUI.getInstance().getFrame());
				ed.setException("Exception encountered while saving file", e);
				ed.setVisible(true);
			}
		}
	}

	public SparseMultigraph<OrcaNode, OrcaLink> loadGraph(File f) {
		return null;
	}
	
}
