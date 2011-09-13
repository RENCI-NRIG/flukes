/*
 * DeleteVertexMenuItem.java
 *
 * Created on March 21, 2007, 2:03 PM; Updated May 29, 2007
 *
 * Copyright March 21, 2007 Grotto Networking
 * 
 * Modified for Flukes. Additional code copyright 2011 RENCI/UNC Chapel Hill by Ilia Baldine
 *
 */

package orca.flukes;

import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.picking.PickedState;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;

/**
 * A class to implement the deletion of a vertex from within a 
 * PopupVertexEdgeMenuMousePlugin.
 * @author Dr. Greg M. Bernstein
 */
public class DeleteVertexMenuItem<V, E> extends JMenuItem implements NodeMenuListener<V, E> {
    private V vertex;
    private VisualizationViewer<V, E> visComp;
    private IDeleteNodeCallBack<V> ic;
    
    /** Creates a new instance of DeleteVertexMenuItem */
    public DeleteVertexMenuItem(IDeleteNodeCallBack<V> i) {
        super("Delete Node");
        ic = i;
        this.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
            	ic.deleteNodeCallBack(vertex);
                visComp.getPickedVertexState().pick(vertex, false);
                visComp.getGraphLayout().getGraph().removeVertex(vertex);
                visComp.repaint();
            }
        });
    }

    /**
     * Implements the NodeMenuListener interface.
     * @param v 
     * @param visComp 
     */
    public void setNodeAndView(V v, VisualizationViewer<V, E> visComp) {
        this.vertex = v;
        this.visComp = visComp;
        this.setText("Delete " + v.toString());
    }
    
}
