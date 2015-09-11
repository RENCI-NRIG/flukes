package orca.flukes.ndl;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import orca.flukes.GUI;
import orca.flukes.GUIUnifiedState;
import orca.flukes.GUIUnifiedState.GroupModifyRecord;
import orca.flukes.OrcaCrossconnect;
import orca.flukes.OrcaLink;
import orca.flukes.OrcaNode;
import orca.flukes.OrcaResource;
import orca.flukes.OrcaResource.ResourceType;
import orca.ndl.NdlGenerator;

import com.hp.hpl.jena.ontology.Individual;
import com.hyperrealm.kiwi.ui.dialog.ExceptionDialog;

import edu.uci.ics.jung.graph.SparseMultigraph;

/**
 * Generate a modify request
 * @author ibaldin
 *
 */
public class ModifySaver extends RequestSaver {
	
	private static ModifySaver msInstance = new ModifySaver();
	
	public static ModifySaver getInstance() {
		return msInstance;
	}
	
	private  ModifySaver() {
	}
	
	@Override
	public String convertGraphToNdl(SparseMultigraph<OrcaNode, OrcaLink> g, String nsGuid) {
		throw new RuntimeException("For ModifySaver call convertModifyGraphToNdl() instead");
	}
	
	/**
	 * Return modify request in specified format
	 * @return
	 */
	public String convertModifyGraphToNdl(SparseMultigraph<OrcaNode, OrcaLink> g, List<OrcaResource> deleted, Map<String, GUIUnifiedState.GroupModifyRecord> modifiedGroups) {

		synchronized(msInstance) {
			try {
				String nsGuid = UUID.randomUUID().toString();
				
				ngen = new NdlGenerator(nsGuid, GUI.logger(), true);
				
				String nm = nsGuid + "/modify";

				reservation = ngen.declareModifyReservation(nm);

//				System.out.println("Submitting modify");
//				System.out.println("Deleted: ");
//				for(OrcaResource or: deleted) {
//					System.out.print(or + " ");
//				}
//				System.out.println();
//				System.out.println("Modified groups:");
//				for(Map.Entry<String, GroupModifyRecord> gmre: modifiedGroups.entrySet()) {
//					System.out.print(gmre.getKey() + ": " + gmre.getValue() + " ");
//				}
//				System.out.println();
//				System.out.println("New: ");
//				for (OrcaNode n: g.getVertices()) {
//					if (n.getResourceType() == ResourceType.REQUEST) {
//						for(OrcaLink l: g.getIncidentEdges(n)) {
//							if (l.getResourceType() == ResourceType.REQUEST)
//								System.out.print(l + " ");
//						}
//						System.out.print(n + " ");
//					}
//				}
//				System.out.println();
				
				for(OrcaResource orr: deleted) {
					if (orr.getUrl() != null) {
						if ((orr instanceof OrcaLink) || (orr instanceof OrcaCrossconnect)) {
							ngen.declareModifyElementRemoveLink(reservation, orr.getUrl());
							// NOTE: no need to determine if node modifies (to remove interfaces) need to be triggered 
						} else
							ngen.declareModifyElementRemoveNode(reservation, orr.getUrl());
					} else
						throw new Exception("Deleted resource " + orr + " is missing a model URL");
				}
				
				// add nodes
				for(OrcaNode n: g.getVertices()) {
					if (n.getResourceType() == ResourceType.REQUEST) {
						Individual ni = processNode(n, null, false);
						if (ni != null)
							ngen.declareModifyElementAddElement(reservation, ni);
					}
				}
				
				// add crossconnects
				for(OrcaResource n: g.getVertices()) {
					if (n.getResourceType() == ResourceType.REQUEST) {
						Individual ci = processPossibleCrossconnect(n, null);
						if (ci != null) {
							ngen.declareModifyElementAddElement(reservation, ci);
						}
					}
				}
				
				// FIXME: skipping color links and dependencies for now
				
				// add links
				for(OrcaLink l: g.getEdges()) {
					if (l.getResourceType() == ResourceType.REQUEST) {
						Individual li = processLink(l, null);
						if (li != null) {
							ngen.declareModifyElementAddElement(reservation, li);
						}
					}
				}
				
				for(Map.Entry<String, GroupModifyRecord> gmre: modifiedGroups.entrySet()) {
					if (gmre.getValue().getCountChange() != 0) 
						ngen.declareModifyElementNGIncreaseBy(reservation, gmre.getKey(), gmre.getValue().getCountChange());
					
					if (gmre.getValue().getRemoveNodes().size() != 0) {
						for(String nUrl: gmre.getValue().getRemoveNodes()) {
							ngen.declareModifyElementNGDeleteNode(reservation, gmre.getKey(), nUrl);
						}
					}
				}

				return getFormattedOutput(outputFormat);
			} catch (Exception e) {
				ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
				ed.setLocationRelativeTo(GUI.getInstance().getFrame());
				ed.setException("Exception encountered while converting graph to NDL-OWL: ", e);
				ed.setVisible(true);
				return null;
			} finally {
				if (ngen != null)
					ngen.done();
			}
		}
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
