package orca.flukes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

public class GUIManifestState extends GUICommonState {
	
	private static GUIManifestState instance = new GUIManifestState();
	
	public static GUIManifestState getInstance() {
		return instance;
	}

	/**
	 * clear the manifest
	 */
	public void clear() {
		// clear the graph, 
		Set<OrcaNode> nodes = new HashSet<OrcaNode>(g.getVertices());
		for (OrcaNode n: nodes)
			g.removeVertex(n);
	}
	
	public ActionListener getActionListener() {
		return null;
	}
}
