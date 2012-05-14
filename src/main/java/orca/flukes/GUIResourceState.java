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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import javax.swing.ImageIcon;

import orca.flukes.ndl.ResourceQueryProcessor;

import org.apache.commons.collections15.Transformer;

import com.hyperrealm.kiwi.ui.dialog.ExceptionDialog;
import com.hyperrealm.kiwi.ui.dialog.ProgressDialog;
import com.hyperrealm.kiwi.util.Task;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.EditingModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.picking.PickedState;

public class GUIResourceState extends GUICommonState {
	
	public static final String WORLD_ICON="worldmap3.png";
	
	private static GUIResourceState instance = null;

	/**
	 * Transform coordinates into a point
	 * @author ibaldin
	 *
	 */
	static class LatLonPixelTransformer implements Transformer<OrcaNode,Point2D> {
		Dimension d;
		int startOffset;

		public LatLonPixelTransformer(Dimension d) {
			this.d = d;
		}
		
		/**
		 * transform a lat
		 */
		 public Point2D transform(OrcaNode s) {
			 if (!(s instanceof OrcaResourceSite))
				 return null;
			 OrcaResourceSite rs = (OrcaResourceSite)s;
			 double latitude = rs.getLat();
			 double longitude = rs.getLon();
			 latitude *= d.height/180f;
			 longitude *= d.width/360f;
			 latitude = d.height/2 - latitude;
			 longitude += d.width/2;

			 return new Point2D.Double(longitude,latitude);
		 }
	}
	
	private GUIResourceState() {
		;
	}
	
	private static void initialize() {

	}
	
	public static GUIResourceState getInstance() {
		if (instance == null) {
			initialize();
			instance = new GUIResourceState();
		}
		return instance;
	}
	
	/**
	 * Resource pane button actions
	 * @author ibaldin
	 *
	 */
	public class ResourceButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("query")) {
				// run XMLRPC query

				try {
					final ProgressDialog pd = GUI.getProgressDialog("Contacting registry");
					pd.track(new Task (){

						@Override
						public void run() {
							try {
								ResourceQueryProcessor.processAMQuery(pd);
							} catch (Exception e) {
								;
							}
						}
					});
				} catch (Exception ex) {
					ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
					ed.setLocationRelativeTo(GUI.getInstance().getFrame());
					ed.setException("Exception encountered while making XMLRPC query: ", ex);
					ed.setVisible(true);
				}
			} 
		}
	}
	
	private ActionListener al = new ResourceButtonListener();
	@Override
	public ActionListener getActionListener() {
		return al;
	}
	
	/**
	 * Create a new site or return existing ones with similar coordinates
	 * @param dom
	 * @return
	 */
	private double tolerance = 0.01;
	public synchronized OrcaResourceSite createSite(String dom, float lat, float lon) {
		for (OrcaNode node: g.getVertices()) {
			OrcaResourceSite ors = (OrcaResourceSite)node;
			if ((Math.abs(lat - ors.getLat()) < tolerance) &&
					(Math.abs(lon - ors.getLon()) < tolerance)) {
				ors.addDomain(dom);
				return ors;
			}
		}
		OrcaResourceSite newOrs = 
			new OrcaResourceSite(dom, lat, lon);
		newOrs.addDomain(dom);
		g.addVertex(newOrs);
		return newOrs;
	}

	@Override
	public void addPane(Container c) {

		//Layout<OrcaNode, OrcaLink> layout = new StaticLayout<OrcaNode, OrcaLink>(g);
		Layout<OrcaNode,OrcaLink> layout = 
			new StaticLayout<OrcaNode,OrcaLink>(g,
				new GUIResourceState.LatLonPixelTransformer(new Dimension(7000,3500)));

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
		//myPlugin.setEdgePopup(new MouseMenus.ManifestEdgeMenu());
		myPlugin.setVertexPopup(new MouseMenus.ResourceNodeMenu());
		
		// add map pre-renderer
		ImageIcon mapIcon = null;
        String imageLocation = GUIResourceState.WORLD_ICON;
        try {
            mapIcon = 
                    new ImageIcon(GUIResourceState.class.getResource(imageLocation));
        } catch(Exception ex) {
            System.err.println("Can't load \""+imageLocation+"\"");
        }
        final ImageIcon icon = mapIcon;

        vv.addPreRenderPaintable(new VisualizationViewer.Paintable(){
            public void paint(Graphics g) {
                    Graphics2D g2d = (Graphics2D)g;
                    AffineTransform oldXform = g2d.getTransform();
                AffineTransform lat = 
                    vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).getTransform();
                AffineTransform vat = 
                    vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).getTransform();
                AffineTransform at = new AffineTransform();
                at.concatenate(g2d.getTransform());
                at.concatenate(vat);
                at.concatenate(lat);
                g2d.setTransform(at);
                g.drawImage(icon.getImage(), 0, 0,
                            icon.getIconWidth(),icon.getIconHeight(), vv);
                g2d.setTransform(oldXform);
            }
            public boolean useTransform() { return false; }
        });
		
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
		
		// center over US
		vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).setTranslate(-1100, -700);
	}
	
}
