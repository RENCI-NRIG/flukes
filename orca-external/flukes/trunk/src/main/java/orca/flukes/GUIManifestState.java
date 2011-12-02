package orca.flukes;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import orca.flukes.GUI.GuiTabs;
import orca.flukes.ndl.ManifestLoader;
import orca.flukes.ui.TextAreaDialog;
import orca.flukes.xmlrpc.OrcaSMXMLRPCProxy;

import com.hyperrealm.kiwi.ui.KTextArea;
import com.hyperrealm.kiwi.ui.KTextField;
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
	public void clear() {
		// clear the graph, 
		if (g == null)
			return;
		Set<OrcaNode> nodes = new HashSet<OrcaNode>(g.getVertices());
		for (OrcaNode n: nodes)
			g.removeVertex(n);
	}
	
	public class ResourceButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			assert(sliceIdField != null);

			if (e.getActionCommand().equals("manifest")) {
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

						try {
							OrcaSMXMLRPCProxy.getInstance().deleteSlice(sliceIdField.getText());
						} catch (Exception ex) {
							ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
							ed.setLocationRelativeTo(GUI.getInstance().getFrame());
							ed.setException("Exception encountered while deleting slice manifest: ", ex);
							ed.setVisible(true);
						}

					}
		}
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
