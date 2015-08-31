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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ActionListener;

import org.apache.commons.collections15.Transformer;

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
	SparseMultigraph<OrcaNode, OrcaLink> g = new SparseMultigraph<OrcaNode, OrcaLink>();
	OrcaNodeCreator nodeCreator = new OrcaNodeCreator(g);
	OrcaLinkCreator linkCreator = new OrcaLinkCreator(g);
	KTextField sliceIdField = null;
	enum SliceState { INVALID, NEW, EXISTING, CLOSED };
	SliceState sState;
	
	EditingModalGraphMouse<OrcaNode, OrcaLink> gm = null;
	
	// standard  transformers for edges
	protected class LinkPaint implements Transformer<OrcaLink, Paint> {
	    public Paint transform(OrcaLink l) {
	    	if (l instanceof OrcaColorLink)
	    		return Color.RED;
	    	return Color.BLACK;
	    }
	};
	
	protected class LinkStroke implements Transformer<OrcaLink, Stroke> {
		float dash[] = { 10.0f };
		public Stroke transform(OrcaLink l) {
			return new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
		}
	}

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
}
