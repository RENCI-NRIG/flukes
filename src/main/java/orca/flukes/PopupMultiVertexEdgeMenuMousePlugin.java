package orca.flukes;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.JPopupMenu;

import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractPopupGraphMousePlugin;

public class PopupMultiVertexEdgeMenuMousePlugin extends AbstractPopupGraphMousePlugin {
	private JPopupMenu modePopup = null;
	
    /** Creates a new instance of PopupVertexEdgeMenuMousePlugin */
    public PopupMultiVertexEdgeMenuMousePlugin() {
        super();
    }
    
    /**
     * Creates a new instance of PopupVertexEdgeMenuMousePlugin
     * @param modifiers mouse event modifiers see the jung visualization Event class.
     */
    public PopupMultiVertexEdgeMenuMousePlugin(int modifiers) {
        super(modifiers);
    }
    
	@Override
	protected void handlePopup(MouseEvent e) {
		
    	if (e.getButton() != MouseEvent.BUTTON3)
    		return;
        final VisualizationViewer<OrcaNode, OrcaLink> vv =
                (VisualizationViewer<OrcaNode, OrcaLink>)e.getSource();
        Point2D p = e.getPoint();
        
        GraphElementAccessor<OrcaNode, OrcaLink> pickSupport = vv.getPickSupport();
        if(pickSupport != null) {
            final OrcaNode v = pickSupport.getVertex(vv.getGraphLayout(), p.getX(), p.getY());
            if (v != null) {
            	JPopupMenu m = v.contextMenu();

            	if (m != null) {
            		updateVertexMenu(v, m, vv, p); 
            		m.show(vv, e.getX(), e.getY());
            	}
            } else {
                final OrcaLink edge = pickSupport.getEdge(vv.getGraphLayout(), p.getX(), p.getY());
                if (edge != null) {
                	JPopupMenu m = edge.contextMenu();

                	if (m != null) {
                		updateEdgeMenu(edge, m, vv, p);
                		m.show(vv, e.getX(), e.getY());
                	}
                }  else  {
                	// pop up the mode menu with common node properties
                	updateModeMenu(vv,p);
                	if (modePopup != null)
                		modePopup.show(vv, e.getX(), e.getY());
                }
            }
        }
	}
	
    
    public JPopupMenu getModePopup() {
    	return modePopup;
    }
    
    public void setModePopup(JPopupMenu modePopup) {
    	this.modePopup = modePopup;
    }

    private void updateVertexMenu(OrcaNode v, JPopupMenu vertexPopup, VisualizationViewer<OrcaNode, OrcaLink> vv, Point2D point) {
        if (vertexPopup == null) return;
        Component[] menuComps = vertexPopup.getComponents();
        for (Component comp: menuComps) {
            if (comp instanceof NodeMenuListener<?, ?>) {
                ((NodeMenuListener<OrcaNode, OrcaLink>)comp).setNodeAndView(v, vv);
            }
            if (comp instanceof MenuPointListener) {
                ((MenuPointListener)comp).setPoint(point);
            }
        }
    }
    
    private void updateEdgeMenu(OrcaLink edge, JPopupMenu edgePopup, VisualizationViewer<OrcaNode, OrcaLink> vv, Point2D point) {
        if (edgePopup == null) return;
        Component[] menuComps = edgePopup.getComponents();
        for (Component comp: menuComps) {
            if (comp instanceof EdgeMenuListener<?,?>) {
                ((EdgeMenuListener<OrcaNode, OrcaLink>)comp).setEdgeAndView(edge, vv);
            }
            if (comp instanceof MenuPointListener) {
                ((MenuPointListener)comp).setPoint(point);
            }
        }
    }
    
    private void updateModeMenu(VisualizationViewer<OrcaNode, OrcaLink> vv, Point2D point) {
        if (modePopup == null) return;
        Component[] menuComps = modePopup.getComponents();
        for (Component comp: menuComps) {
            if (comp instanceof SelectListener<?>) {
            	((SelectListener<OrcaNode>)comp).setSelectedNodes(vv.getPickedVertexState().getPicked());
            }
        }
    }
    
    
	
}
