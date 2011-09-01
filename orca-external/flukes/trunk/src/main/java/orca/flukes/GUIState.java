package orca.flukes;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import orca.flukes.ui.ChooserWithNewDialog;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * Singleton class that holds shared GUI state. Since dialogs are all modal, no need for locking for now.
 * @author ibaldin
 *
 */
public class GUIState implements IDeleteEdgeCallBack<OrcaLink>, IDeleteNodeCallBack<OrcaNode> {
	public static String NO_GLOBAL_IMAGE = "None";
	
	private static GUIState instance = null;
	
	HashMap<String, OrcaImage> definedImages = new HashMap<String, OrcaImage>();
	ChooserWithNewDialog<String> icd = null;
	ReservationDetailsDialog rdd = null;
	
	// are we adding a new image definition or editing existing
	boolean addingNewImage = false;
	
	// The graph object
	SparseMultigraph<OrcaNode, OrcaLink> g;
	
	// Reservation details
	Date resStart = null, resEnd = null;
	String resImageName = null;
	
	static GUIState getInstance() {
		if (instance == null)
			instance = new GUIState();
		return instance;
	}
	
	public Object[] getImageShortNames() {
		if (definedImages.size() > 0)
			return definedImages.keySet().toArray();
		else return new String[0];
	}
	
	public String[] getImageShortNamesWithNone() {
		String[] fa = new String[definedImages.size() + 1];
		fa[0] = NO_GLOBAL_IMAGE;
		System.arraycopy(getImageShortNames(), 0, fa, 1, definedImages.size());
		return fa;		
	}
	
	public Iterator<String> getImageShortNamesIterator() {
		return definedImages.keySet().iterator();
	}
	
	/**
	 * Cleanup before deleting an edge
	 * @param e
	 */
	public void deleteEdgeCallBack(OrcaLink e) {
		if (e == null)
			return;
		// remove edge from node IP maps
		Pair<OrcaNode> p = g.getEndpoints(e);
		p.getFirst().removeIp(e);
		p.getSecond().removeIp(e);
	}

	public void deleteNodeCallBack(OrcaNode n) {
		if (n == null)
			return;
		// remove incident edges
		Collection<OrcaLink> edges = g.getIncidentEdges(n);
		for (OrcaLink e: edges) {
			deleteEdgeCallBack(e);
		}
	}
}
