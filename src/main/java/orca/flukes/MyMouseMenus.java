/*
 * MyMouseMenus.java
 *
 * Created on March 21, 2007, 3:34 PM; Updated May 29, 2007
 *
 * Copyright March 21, 2007 Grotto Networking
 *
 */

package orca.flukes;

import edu.uci.ics.jung.visualization.VisualizationViewer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * A collection of classes used to assemble popup mouse menus for the custom
 * edges and vertices developed in this example.
 * @author Dr. Greg M. Bernstein
 */
public class MyMouseMenus {
    
    public static class EdgeMenu extends JPopupMenu {        
        // private JFrame frame; 
        public EdgeMenu() {
            super("Edge Menu");
            // this.frame = frame;
            this.add(new DeleteEdgeMenuItem<OrcaNode, OrcaLink>());
            this.addSeparator();
            this.add(new WeightDisplay());
            this.add(new CapacityDisplay());
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
            super("Edit Edge Properties...");
            this.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    EdgePropertyDialog dialog = new EdgePropertyDialog(frame, edge);
                    dialog.setLocation((int)point.getX()+ frame.getX(), (int)point.getY()+ frame.getY());
                    dialog.setVisible(true);
                }
                
            });
        }
        
    }
    public static class WeightDisplay extends JMenuItem implements EdgeMenuListener<OrcaNode, OrcaLink> {
        public void setEdgeAndView(OrcaLink e, VisualizationViewer visComp) {
            this.setText("Weight " + e + " = " + e.getWeight());
        }
    }
    
    public static class CapacityDisplay extends JMenuItem implements EdgeMenuListener<OrcaNode, OrcaLink> {
        public void setEdgeAndView(OrcaLink e, VisualizationViewer visComp) {
            this.setText("Capacity " + e + " = " + e.getCapacity());
        }
    }
    
    public static class VertexMenu extends JPopupMenu {
        public VertexMenu() {
            super("Vertex Menu");
            this.add(new DeleteVertexMenuItem<OrcaNode>());
            this.addSeparator();
            this.add(new pscCheckBox());
            this.add(new tdmCheckBox());
        }
    }
    
    public static class pscCheckBox extends JCheckBoxMenuItem implements VertexMenuListener<OrcaNode> {
        OrcaNode v;
        
        public pscCheckBox() {
            super("PSC Capable");
            this.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    v.setPacketSwitchCapable(isSelected());
                }
                
            });
        }
        public void setVertexAndView(OrcaNode v, VisualizationViewer visComp) {
            this.v = v;
            this.setSelected(v.isPacketSwitchCapable());
        }
        
    }
    
        public static class tdmCheckBox extends JCheckBoxMenuItem implements VertexMenuListener<OrcaNode> {
        OrcaNode v;
        
        public tdmCheckBox() {
            super("TDM Capable");
            this.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    v.setTdmSwitchCapable(isSelected());
                }
                
            });
        }
        public void setVertexAndView(OrcaNode v, VisualizationViewer visComp) {
            this.v = v;
            this.setSelected(v.isTdmSwitchCapable());
        }
        
    }
    
}
