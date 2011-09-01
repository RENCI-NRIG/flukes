/*
 * DeleteEdgeMenuItem.java
 *
 * Created on March 21, 2007, 2:47 PM; Updated May 29, 2007
 *
 * Copyright March 21, 2007 Grotto Networking
 *
 */

package orca.flukes;

import edu.uci.ics.jung.visualization.VisualizationViewer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;

/**
 * A class to implement the deletion of an edge from within a 
 * PopupVertexEdgeMenuMousePlugin.
 * @author Dr. Greg M. Bernstein
 */
public class DeleteEdgeMenuItem<V, E> extends JMenuItem implements EdgeMenuListener<V, E> {
    private E edge;
    private VisualizationViewer<V, E> visComp;
    private IDeleteEdgeCallBack<E> ic;
    
    /** Creates a new instance of DeleteEdgeMenuItem */
    public DeleteEdgeMenuItem(final IDeleteEdgeCallBack<E> i) {
        super("Delete Edge");
    	ic = i;
        this.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
            	ic.deleteEdgeCallBack(edge);
                visComp.getPickedEdgeState().pick(edge, false);
                visComp.getGraphLayout().getGraph().removeEdge(edge);
                visComp.repaint();
            }
        });
    }

    /**
     * Implements the EdgeMenuListener interface to update the menu item with info
     * on the currently chosen edge.
     * @param edge 
     * @param visComp 
     */
    public void setEdgeAndView(E edge, VisualizationViewer<V, E> visComp) {
        this.edge = edge;
        this.visComp = visComp;
        this.setText("Delete Edge " + edge.toString());
    }
    
}
