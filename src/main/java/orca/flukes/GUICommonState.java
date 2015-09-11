/*
* Copyright (c) 2011 RENCI/UNC Chapel Hill 
*
* @author Ilia Baldine
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
* and/or hardware specification (the "Work") to deal in the Work without restriction, including 
* without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
* sell copies of the Work, and to permit persons to whom the Work is furnished to do so, subject to 
* the following conditions:  
* The above copyright notice and this permission notice shall be included in all copies or 
* substantial portions of the Work.  
*
* THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
* OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS 
* IN THE WORK.
*/
package orca.flukes;

import java.awt.Container;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hyperrealm.kiwi.ui.KTextField;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.EditingModalGraphMouse;

/**
 * holds common state for GUI panes
 * @author ibaldin
 *
 */
public abstract class GUICommonState {
	SparseMultigraph<OrcaNode, OrcaLink> g = new SparseMultigraph<>();
	OrcaNodeCreator nodeCreator = new OrcaNodeCreator(g);
	OrcaLinkCreator linkCreator = new OrcaLinkCreator(g);
	KTextField sliceIdField = null;
	enum SliceState { INVALID, NEW, EXISTING, CLOSED };
	SliceState sState;
	
	EditingModalGraphMouse<OrcaNode, OrcaLink> gm = null;
	
	// where are we saving
	String saveDirectory = null;
	
	// Vis viewer 
	VisualizationViewer<OrcaNode,OrcaLink> vv = null;
	
	public OrcaLinkCreator getLinkCreator() {
		return linkCreator;
	}
	
	public OrcaNodeCreator getNodeCreator() {
		return nodeCreator;
	}
	
	public SparseMultigraph<OrcaNode, OrcaLink> getGraph() {
		return g;
	}

	public void setSaveDir(String s) {
		saveDirectory = s;
	}
	
	public String getSaveDir() {
		return saveDirectory;
	}

	public void setSliceIdField(KTextField ktf) {
		sliceIdField = ktf;
	}
	
	public void setSliceIdFieldText(String t) {
		sliceIdField.setText(t);
	}
	
	public void clear() {
		// clear the graph
		if (g != null) {
			Set<OrcaNode> nodes = new HashSet<OrcaNode>(g.getVertices());
			for (OrcaNode n: nodes)
				g.removeVertex(n);
		}

		nodeCreator.reset();
		linkCreator.reset();
	}
	
	public String getSliceName() {
		return sliceIdField.getText();
	}
	
	public SliceState getSliceState() {
		return sState;
	}
	
	// a pane may have an action listener (e.g. for internal buttons)
	abstract public ActionListener getActionListener();
	
	abstract public void addPane(Container c);
	
	public static void clearGraph(SparseMultigraph<OrcaNode, OrcaLink> t) {
		List<OrcaNode> dNodes = new ArrayList<>(t.getVertices());
		List<OrcaLink> dEdges = new ArrayList<>(t.getEdges());
		for(OrcaLink e: dEdges)
			t.removeEdge(e);
		for(OrcaNode n: dNodes)
			t.removeVertex(n);
	}
	
	/**
	 * Shallow-copy graph (only structure, not nodes and edges) if both src and destional are non-null
	 * @param src
	 * @param dst
	 */
	public static void copyGraph(SparseMultigraph<OrcaNode, OrcaLink> src, SparseMultigraph<OrcaNode, OrcaLink> dst) {
		if ((src == null) || (dst == null))
			return;
		
		// empty dst graph
		clearGraph(dst);
		
	    for (OrcaNode v : src.getVertices())
	        dst.addVertex(v);

	    for (OrcaLink e : src.getEdges())
	        dst.addEdge(e, src.getIncidentVertices(e));
	}
}
