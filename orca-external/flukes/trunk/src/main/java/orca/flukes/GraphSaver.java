package orca.flukes;

import java.io.File;
import java.io.FileWriter;

import orca.ndl.NdlGenerator;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.Individual;
import com.hyperrealm.kiwi.ui.dialog.ExceptionDialog;
import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

import edu.uci.ics.jung.graph.SparseMultigraph;

public class GraphSaver {
	private static GraphSaver instance = null;
	private NdlGenerator ngen = null;
	private Individual reservation = null;
	
	public static GraphSaver getInstance() {
		if (instance == null)
			instance = new GraphSaver();
		return instance;
	}
	
	private String getFormattedOutput(NdlGenerator ng, String oFormat) {
		if (oFormat.equals("RDF-XML")) 
			return ng.toXMLString();
		else if (oFormat.equals("N3"))
			return ng.toN3String();
		else if (oFormat.equals("DOT")) {
			return ng.getGVOutput();
		}
		else
			return ng.toString();
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
				ngen = new NdlGenerator(Logger.getLogger(this.getClass().getCanonicalName()));
			
				if ((GUIState.getInstance().resStart != null) && (GUIState.getInstance().resEnd != null))
					reservation = ngen.declareReservation(GUIState.getInstance().resStart, 
							GUIState.getInstance().resEnd);
				else
					reservation = ngen.declareReservation();
			
				// need nodes, edges and the reservation
				String res = getFormattedOutput(ngen, "N3");
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
