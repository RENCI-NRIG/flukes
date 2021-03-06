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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import com.hyperrealm.kiwi.ui.dialog.KQuestionDialog;

import edu.uci.ics.jung.visualization.VisualizationViewer;

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
            	KQuestionDialog kqd = new KQuestionDialog(GUI.getInstance().getFrame(), "Exit", true);
        		kqd.setMessage("Are you sure you want to delete node " + vertex.toString() + "?");
        		kqd.setLocationRelativeTo(GUI.getInstance().getFrame());
        		kqd.setVisible(true);
        		if (kqd.getStatus()) {
	                visComp.getPickedVertexState().pick(vertex, false);
	            	ic.deleteNodeCallBack(vertex);
	                //visComp.getGraphLayout().getGraph().removeVertex(vertex);
	                visComp.repaint();
        		}
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
