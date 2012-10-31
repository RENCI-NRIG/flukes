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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import orca.flukes.GUI.GuiTabs;
import orca.flukes.ndl.ManifestLoader;
import orca.flukes.ui.TextAreaDialog;
import orca.flukes.xmlrpc.OrcaSMXMLRPCProxy;

import com.hyperrealm.kiwi.ui.KTextArea;
import com.hyperrealm.kiwi.ui.dialog.ExceptionDialog;
import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;
import com.hyperrealm.kiwi.ui.dialog.KQuestionDialog;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.EditingModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.picking.PickedState;

public class GUIManifestState extends GUICommonState {
	private static GUIManifestState instance = new GUIManifestState();
	protected String manifestString;
	
	public static GUIManifestState getInstance() {
		return instance;
	}

	public void setManifestString(String s) {
		manifestString = s;
	}
	
	/**
	 * clear the manifest
	 */
	@Override
	public void clear() {
		super.clear();
		
		// clear the graph, 
		if (g == null)
			return;
		Set<OrcaNode> nodes = new HashSet<OrcaNode>(g.getVertices());
		for (OrcaNode n: nodes)
			g.removeVertex(n);
	}

	void deleteSlice(String name) {
		if ((name == null) || 
				(name.length() == 0)) {
			KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
			kmd.setMessage("You must specify a slice id");
			kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
			kmd.setVisible(true);
			return;
		}
		
		try {
			OrcaSMXMLRPCProxy.getInstance().deleteSlice(name);
		} catch (Exception ex) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while deleting slice manifest: ", ex);
			ed.setVisible(true);
		}
	}
	
	void modifySlice(String name, String req) {
		if ((name == null) || 
				(name.length() == 0)) {
			KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
			kmd.setMessage("You must specify a slice id");
			kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
			kmd.setVisible(true);
			return;
		}
		try {
			String s = OrcaSMXMLRPCProxy.getInstance().modifySlice(name, req);
			TextAreaDialog tad = new TextAreaDialog(GUI.getInstance().getFrame(), "Modify Output", 
					"Modify Output", 
					30, 50);
			KTextArea ta = tad.getTextArea();

			if (s != null)
				ta.setText(s);
			tad.pack();
			tad.setVisible(true);
		} catch (Exception ex) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while modifying slice: ", ex);
			ed.setVisible(true);
		}
	}
	
	void queryManifest() {
		// run request manifest from controller
		if ((sliceIdField.getText() == null) || 
				(sliceIdField.getText().length() == 0)) {
			KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
			kmd.setMessage("You must specify a slice id");
			kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
			kmd.setVisible(true);
			return;
		}

		try {
			GUIManifestState.getInstance().clear();

			manifestString = OrcaSMXMLRPCProxy.getInstance().sliceStatus(sliceIdField.getText());

			ManifestLoader ml = new ManifestLoader();

			// get rid of crud before <rdf:RDF
			int ind = manifestString.indexOf("<rdf:RDF");
			if (ind > 0) {
				String realManifest = manifestString.substring(ind);
				if (ml.loadString(realManifest))
					GUI.getInstance().kickLayout(GuiTabs.MANIFEST_VIEW);
			} else {
				KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
				kmd.setMessage("Error has occurred, check raw controller response for details.");
				kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
				kmd.setVisible(true);
				return;
			}
		} catch (Exception ex) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while querying ORCA for slice manifest: ", ex);
			ed.setVisible(true);
		}
	}
	
	public class ResourceButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			assert(sliceIdField != null);

			if (e.getActionCommand().equals("manifest")) {
				// run request manifest from controller
				queryManifest();
			} else 
				if (e.getActionCommand().equals("raw")) {
					TextAreaDialog tad = new TextAreaDialog(GUI.getInstance().getFrame(), "Raw manifest", 
							"Raw manifest", 
							30, 50);
					KTextArea ta = tad.getTextArea();

					if (manifestString != null)
						ta.setText(manifestString);
					tad.pack();
					tad.setVisible(true);
				} else 
					if (e.getActionCommand().equals("delete")) {
						if ((sliceIdField.getText() == null) || 
								(sliceIdField.getText().length() == 0)) {
							KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
							kmd.setMessage("You must specify a slice id");
							kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
							kmd.setVisible(true);
							return;
						}

						KQuestionDialog kqd = new KQuestionDialog(GUI.getInstance().getFrame(), "Exit", true);
						kqd.setMessage("Are you sure you want to delete slice " + sliceIdField.getText());
						kqd.setLocationRelativeTo(GUI.getInstance().getFrame());
						kqd.setVisible(true);
						if (!kqd.getStatus()) 
							return;
						deleteSlice(sliceIdField.getText());

					} else 
						if (e.getActionCommand().equals("listSlices")) {
							try {
								String[] slices = OrcaSMXMLRPCProxy.getInstance().listMySlices();
								OrcaSliceList osl = new OrcaSliceList(GUI.getInstance().getFrame(), slices);
								osl.pack();
								osl.setVisible(true);
							} catch (Exception ex) {
								ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
								ed.setLocationRelativeTo(GUI.getInstance().getFrame());
								ed.setException("Exception encountered while listing user slices: ", ex);
								ed.setVisible(true);
							}
						} else 
							if (e.getActionCommand().equals("modify")) {
								try {
									if ((sliceIdField.getText() == null) || 
											(sliceIdField.getText().length() == 0)) {
										KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
										kmd.setMessage("You must specify a slice id");
										kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
										kmd.setVisible(true);
										return;
									}
									ModifyTextSetter mts = new ModifyTextSetter(sliceIdField.getText());
									TextAreaDialog tad = new TextAreaDialog(GUI.getInstance().getFrame(), mts, 
											"Modify Request", 
											"Cut and paste the modify request into the window", 30, 50);
									tad.pack();
									tad.setVisible(true);
								} catch(Exception ex) {
									ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
									ed.setLocationRelativeTo(GUI.getInstance().getFrame());
									ed.setException("Exception encountered while modifying slice: ", ex);
									ed.setVisible(true);
								}
							}
		}
	}
	
	public void launchResourceStateViewer(Date start, Date end) {
		// get a list of nodes and links
		List<OrcaResource> resources = new ArrayList<OrcaResource>();
		
		resources.addAll(g.getVertices());
		resources.addAll(g.getEdges());
		
		OrcaResourceStateViewer viewer = new OrcaResourceStateViewer(GUI.getInstance().getFrame(), resources, start, end);
		viewer.pack();
		viewer.setVisible(true);
	}
	
	ResourceButtonListener rbl = new ResourceButtonListener();
	@Override
	public ActionListener getActionListener() {
		return rbl;
	}

	@Override
	public void addPane(Container c) {
		
		Layout<OrcaNode, OrcaLink> layout = new FRLayout<OrcaNode, OrcaLink>(g);
		
		//layout.setSize(new Dimension(1000,800));
		vv = 
			new VisualizationViewer<OrcaNode,OrcaLink>(layout);
		// Show vertex and edge labels
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<OrcaNode>());
		vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<OrcaLink>());
		
		// Create a graph mouse and add it to the visualization viewer
		OrcaNode.OrcaNodeFactory onf = new OrcaNode.OrcaNodeFactory(nodeCreator);
		OrcaLink.OrcaLinkFactory olf = new OrcaLink.OrcaLinkFactory(linkCreator);
		gm = new EditingModalGraphMouse<OrcaNode, OrcaLink>(vv.getRenderContext(), 
				onf, olf);
		
		// add the plugin
		PopupVertexEdgeMenuMousePlugin<OrcaNode, OrcaLink> myPlugin = new PopupVertexEdgeMenuMousePlugin<OrcaNode, OrcaLink>();
		
		// Add some popup menus for the edges and vertices to our mouse plugin.
		// mode menu is not set for manifests
		myPlugin.setEdgePopup(new MouseMenus.ManifestEdgeMenu());
		myPlugin.setVertexPopup(new MouseMenus.ManifestNodeMenu());
		
		gm.remove(gm.getPopupEditingPlugin());  // Removes the existing popup editing plugin
		gm.add(myPlugin);

		// Add icon and shape (so pickable area roughly matches the icon) transformer
		OrcaNode.OrcaNodeIconShapeTransformer st = new OrcaNode.OrcaNodeIconShapeTransformer();
		vv.getRenderContext().setVertexShapeTransformer(st);
		
		OrcaNode.OrcaNodeIconTransformer it = new OrcaNode.OrcaNodeIconTransformer();
		vv.getRenderContext().setVertexIconTransformer(it);
		
		// add listener to add/remove checkmarks on selected nodes
		PickedState<OrcaNode> ps = vv.getPickedVertexState();
        ps.addItemListener(new OrcaNode.PickWithIconListener(it));
		
		vv.setGraphMouse(gm);

		vv.setLayout(new BorderLayout(0,0));
		
		c.add(vv);

		gm.setMode(ModalGraphMouse.Mode.TRANSFORMING); // Start off in panning mode  
	}
	

}
