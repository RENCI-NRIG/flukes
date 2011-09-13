/*
 * MouseMenus.java
 *
 * Created on March 21, 2007, 3:34 PM; Updated May 29, 2007
 *
 * Copyright March 21, 2007 Grotto Networking
 * 
 * Modified for Flukes. Additional code copyright 2011 RENCI/UNC Chapel Hill by Ilia Baldine
 *
 */

package orca.flukes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;

/**
 * A collection of classes used to assemble popup mouse menus for the custom
 * edges and vertices developed in this example.
 * @author Dr. Greg M. Bernstein
 */
public class MouseMenus {
    
	public static class ModeMenu extends JPopupMenu implements ActionListener {
		final ButtonGroup bg;
		
		public ModeMenu() {
			super("Mode Menu");
			bg = new ButtonGroup();
//			JMenu mm = GUI.getInstance().getMouse().getModeMenu();
//			for(int itemCnt = 0; itemCnt < mm.getItemCount(); itemCnt ++) {
//				if (mm.getItem(itemCnt) != null)
//					this.add(mm.getItem(itemCnt));
//			}
			JRadioButtonMenuItem mi = new JRadioButtonMenuItem("Edit");
			mi.setActionCommand("edit");
			mi.addActionListener(this);
			mi.setSelected(true);
			this.add(mi);
			bg.add(mi);
			mi = new JRadioButtonMenuItem("Pick");
			mi.setActionCommand("pick");
			mi.addActionListener(this);
			this.add(mi);
			bg.add(mi);
			mi = new JRadioButtonMenuItem("Pan");
			mi.setActionCommand("pan");
			mi.addActionListener(this);
			this.add(mi);
			bg.add(mi);
			this.addSeparator();
			// add button to create multi-node property window
			CommonPropItem rmi = new CommonPropItem(GUI.getInstance().getFrame());
			this.add(rmi);
		}
		
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("edit")) {
				GUI.getInstance().getMouse().setMode(ModalGraphMouse.Mode.EDITING);
			}
			else if (e.getActionCommand().equals("pick")) {
				GUI.getInstance().getMouse().setMode(ModalGraphMouse.Mode.PICKING);
			}
			else if (e.getActionCommand().equals("pan")) {
				GUI.getInstance().getMouse().setMode(ModalGraphMouse.Mode.TRANSFORMING);
			}
		}
	}
	
	public static class CommonPropItem extends JMenuItem implements SelectListener<OrcaNode> {
		Set<OrcaNode> nodes;
		
		public CommonPropItem(final JFrame parent) {
			super("Edit Selected Nodes...");
			this.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (nodes.size() > 0) {
						OrcaMultiNodePropertyDialog mnp = new OrcaMultiNodePropertyDialog(GUI.getInstance().getFrame(), nodes);
						mnp.pack();
						mnp.setVisible(true);
					} else {
						KMessageDialog kqd = new KMessageDialog(GUI.getInstance().getFrame(), "Empty selection", true);
						kqd.setMessage("You have not selected any nodes!");
						kqd.setLocationRelativeTo(GUI.getInstance().getFrame());
						kqd.setVisible(true);
					}
				}
			});
		}
		
		public void setSelectedNodes(Set<OrcaNode> nodes) {
			this.nodes = nodes;			
		}
		
	}
	
    public static class EdgeMenu extends JPopupMenu {        
        // private JFrame frame; 
        public EdgeMenu() {
            super("Edge Menu");
            // this.frame = frame;
            this.add(new DeleteEdgeMenuItem<OrcaNode, OrcaLink>(GUIState.getInstance()));
            this.addSeparator();
            this.add(new LatencyDisplay());
            this.add(new BandwidthDisplay());
            this.addSeparator();
            this.add(new EdgePropItem(GUI.getInstance().getFrame()));           
        }
        
    }
    
    public static class EdgePropItem extends JMenuItem implements EdgeMenuListener<OrcaNode, OrcaLink>,
            MenuPointListener {
        OrcaLink edge;
        VisualizationViewer<OrcaNode, OrcaLink> visComp;
        Point2D point;
        
        public void setEdgeAndView(OrcaLink edge, VisualizationViewer<OrcaNode, OrcaLink> visComp) {
            this.edge = edge;
            this.visComp = visComp;
        }

        public void setPoint(Point2D point) {
            this.point = point;
        }
        
        public  EdgePropItem(final JFrame frame) {            
            super("Edit Link Properties...");
            this.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    OrcaLinkPropertyDialog dialog = new OrcaLinkPropertyDialog(frame, edge);
                    dialog.pack();
                    dialog.setVisible(true);
                }
            });
        }
        
    }
    public static class LatencyDisplay extends JMenuItem implements EdgeMenuListener<OrcaNode, OrcaLink> {
        public void setEdgeAndView(OrcaLink e, VisualizationViewer<OrcaNode, OrcaLink> visComp) {
            this.setText("Latency: " + e.getLatency());
        }
    }
    
    public static class BandwidthDisplay extends JMenuItem implements EdgeMenuListener<OrcaNode, OrcaLink> {
        public void setEdgeAndView(OrcaLink e, VisualizationViewer<OrcaNode, OrcaLink> visComp) {
            this.setText("Capacity: " + e.getBandwidth());
        }
    }
    
    public static class NodeMenu extends JPopupMenu {
        public NodeMenu() {
            super("Node Menu");
            this.add(new DeleteVertexMenuItem<OrcaNode, OrcaLink>(GUIState.getInstance()));
            this.add(new ImageDisplay());
            this.add(new DomainDisplay());
            this.add(new NodeTypeDisplay());
            this.addSeparator();
            this.add(new NodePropItem(GUI.getInstance().getFrame()));
        }
    }
    
    public static class ImageDisplay extends JMenuItem implements NodeMenuListener<OrcaNode, OrcaLink> {
    	public void setNodeAndView(OrcaNode v,
				VisualizationViewer<OrcaNode, OrcaLink> visView) {
    		if ((v.getImage() != null) && (v.getImage().length() > 0))
    			this.setText("Image: " + v.getImage());
    		else
    			this.setText("Image: " + GUIState.NO_GLOBAL_IMAGE);
    	}
    }
    
    public static class DomainDisplay extends JMenuItem implements NodeMenuListener<OrcaNode, OrcaLink> {
    	public void setNodeAndView(OrcaNode v,
				VisualizationViewer<OrcaNode, OrcaLink> visView) {
    		if ((v.getDomain() != null) && (v.getDomain().length() > 0))
    			this.setText("Domain: " + v.getDomain());
    		else
    			this.setText("Domain: " + GUIState.NO_DOMAIN_SELECT);
    	}
    }
    
    public static class NodeTypeDisplay extends JMenuItem implements NodeMenuListener<OrcaNode, OrcaLink> {
    	public void setNodeAndView(OrcaNode v,
    			VisualizationViewer<OrcaNode, OrcaLink> visView) {
    		if ((v.getNodeType() != null) && (v.getNodeType().length() > 0))
    			this.setText("Node type: " + v.getNodeType());
    		else
    			this.setText("Node type: " + GUIState.NODE_TYPE_SITE_DEFAULT);
    	}
    }
    
    public static class NodePropItem extends JMenuItem implements NodeMenuListener<OrcaNode, OrcaLink>, MenuPointListener {
        OrcaNode node;
        VisualizationViewer<OrcaNode, OrcaLink> visComp;
        Point2D point;
        
        public  NodePropItem(final JFrame frame) {            
            super("Edit Node Properties...");
            this.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    OrcaNodePropertyDialog dialog = new OrcaNodePropertyDialog(frame, node);
                    dialog.pack();
                    dialog.setVisible(true);
                }
            });
        }
        
		public void setNodeAndView(OrcaNode v,
				VisualizationViewer<OrcaNode, OrcaLink> visView) {
			visComp = visView;
			node = v;
		}

		public void setPoint(Point2D point) {
			this.point = point;
		}	
    }
}
