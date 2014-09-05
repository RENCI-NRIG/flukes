/*
 * PopupVertexEdgeMenuMousePlugin.java
 *
 * Created on March 21, 2007, 12:56 PM; Updated May 29, 2007
 *
 * Copyright March 21, 2007 Grotto Networking
 * 
 * Modified for Flukes. Additional code copyright 2011 RENCI/UNC Chapel Hill by Ilia Baldine
 *
 */

package orca.flukes;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.JPopupMenu;

import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractPopupGraphMousePlugin;

/**
 * A GraphMousePlugin that brings up distinct popup menus when an edge or vertex is
 * appropriately clicked in a graph.  If these menus contain components that implement
 * either the EdgeMenuListener or NodeMenuListener then the corresponding interface
 * methods will be called prior to the display of the menus (so that they can display
 * context sensitive information for the edge or vertex).
 * @author Dr. Greg M. Bernstein
 */
public class PopupVertexEdgeMenuMousePlugin<V, E> extends AbstractPopupGraphMousePlugin {
    private JPopupMenu edgePopup = null, vertexPopup = null, modePopup = null;
    
    /** Creates a new instance of PopupVertexEdgeMenuMousePlugin */
    public PopupVertexEdgeMenuMousePlugin() {
        super();
    }
    
    /**
     * Creates a new instance of PopupVertexEdgeMenuMousePlugin
     * @param modifiers mouse event modifiers see the jung visualization Event class.
     */
    public PopupVertexEdgeMenuMousePlugin(int modifiers) {
        super(modifiers);
    }
    
    /**
     * Implementation of the AbstractPopupGraphMousePlugin method. This is where the 
     * work gets done. You shouldn't have to modify unless you really want to...
     * @param e 
     */
    @Override
    protected void handlePopup(MouseEvent e) {
//    	if ((e.getButton() != MouseEvent.BUTTON3) &&
//    					((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK ) != MouseEvent.CTRL_DOWN_MASK))
//    		return;
    	if (e.getButton() != MouseEvent.BUTTON3)
    		return;
        final VisualizationViewer<V,E> vv =
                (VisualizationViewer<V,E>)e.getSource();
        Point2D p = e.getPoint();
        
        GraphElementAccessor<V,E> pickSupport = vv.getPickSupport();
        if(pickSupport != null) {
            final V v = pickSupport.getVertex(vv.getGraphLayout(), p.getX(), p.getY());
            if(v != null) {
                updateVertexMenu(v, vv, p);
                vertexPopup.show(vv, e.getX(), e.getY());
            } else {
                final E edge = pickSupport.getEdge(vv.getGraphLayout(), p.getX(), p.getY());
                if (edge != null) {
                    // System.out.println("Edge " + edge + " was right clicked");
                    updateEdgeMenu(edge, vv, p);
                    edgePopup.show(vv, e.getX(), e.getY());
                }  else  {
                	// pop up the mode menu with common node properties
                	updateModeMenu(vv,p);
                	if (modePopup != null)
                		modePopup.show(vv, e.getX(), e.getY());
                }
            }
        }

    }
    
    private void updateVertexMenu(V v, VisualizationViewer<V,E> vv, Point2D point) {
        if (vertexPopup == null) return;
        Component[] menuComps = vertexPopup.getComponents();
        for (Component comp: menuComps) {
            if (comp instanceof NodeMenuListener<?, ?>) {
                ((NodeMenuListener<V, E>)comp).setNodeAndView(v, vv);
            }
            if (comp instanceof MenuPointListener) {
                ((MenuPointListener)comp).setPoint(point);
            }
        }
        
    }
    
    /**
     * Getter for the edge popup.
     * @return 
     */
    public JPopupMenu getEdgePopup() {
        return edgePopup;
    }
    
    /**
     * Setter for the Edge popup.
     * @param edgePopup 
     */
    public void setEdgePopup(JPopupMenu edgePopup) {
        this.edgePopup = edgePopup;
    }
    
    /**
     * Getter for the vertex popup.
     * @return 
     */
    public JPopupMenu getVertexPopup() {
        return vertexPopup;
    }
    
    /**
     * Setter for the vertex popup.
     * @param vertexPopup 
     */
    public void setVertexPopup(JPopupMenu vertexPopup) {
        this.vertexPopup = vertexPopup;
    }
    
    public JPopupMenu getModePopup() {
    	return modePopup;
    }
    
    public void setModePopup(JPopupMenu modePopup) {
    	this.modePopup = modePopup;
    }
    
    private void updateEdgeMenu(E edge, VisualizationViewer<V, E> vv, Point2D point) {
        if (edgePopup == null) return;
        Component[] menuComps = edgePopup.getComponents();
        for (Component comp: menuComps) {
            if (comp instanceof EdgeMenuListener<?,?>) {
                ((EdgeMenuListener<V, E>)comp).setEdgeAndView(edge, vv);
            }
            if (comp instanceof MenuPointListener) {
                ((MenuPointListener)comp).setPoint(point);
            }
        }
    }
    
    private void updateModeMenu(VisualizationViewer<V, E> vv, Point2D point) {
        if (modePopup == null) return;
        Component[] menuComps = modePopup.getComponents();
        for (Component comp: menuComps) {
            if (comp instanceof SelectListener<?>) {
            	((SelectListener<V>)comp).setSelectedNodes(vv.getPickedVertexState().getPicked());
            }
        }
    }
    
}
