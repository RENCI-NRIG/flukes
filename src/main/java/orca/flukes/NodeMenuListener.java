/*
 * NodeMenuListener.java
 *
 * Created on March 21, 2007, 1:50 PM; Updated May 29, 2007
 * 
 * Modified for Flukes. Additional code copyright 2011 RENCI/UNC Chapel Hill by Ilia Baldine
 * 
 */

package orca.flukes;

import edu.uci.ics.jung.visualization.VisualizationViewer;

/**
 * Used to indicate that this class wishes to be told of a selected vertex
 * along with its visualization component context. Note that the VisualizationViewer
 * has full access to the graph and layout.
 * @author Dr. Greg M. Bernstein
 */
public interface NodeMenuListener<V, E> {
    void setNodeAndView(V v, VisualizationViewer<V,E> visView);    
}
