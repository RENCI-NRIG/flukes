package orca.flukes;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import orca.flukes.OrcaNode.INodeCreator;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.EditingModalGraphMouse;

public class GUIManifestState implements INodeCreator {

	SparseMultigraph<OrcaNode, OrcaLink> manifestGraph;
	VisualizationViewer<OrcaNode, OrcaLink> vv;
	EditingModalGraphMouse<OrcaNode, OrcaLink> gm;
	
	private static GUIManifestState instance = new GUIManifestState();
	
	ManifestNodeClass nodeClass = ManifestNodeClass.CE;
	
	// various types of nodes we allow
	public enum ManifestNodeClass {
		CE(OrcaNode.class, "Node"), 
		ServerCloud(OrcaNodeGroup.class, "NodeGroup"), 
		CrossConnect(OrcaCrossconnect.class, "CrossConnect");
		private int nodeCount;
		private String namePrefix;
		private Class clazz;
		
		ManifestNodeClass(Class c, String pf) {
			clazz = c;
			nodeCount = 0;
			namePrefix = pf;
		}
		
		int getCount() {
			return nodeCount++;
		}
		
		String getName() {
			return namePrefix;
		}
		
		Class<?> getClazz() {
			return clazz;
		}
	}
	
	public static GUIManifestState getInstance() {
		return instance;
	}

	// FIXME - since there are no interactive additions, this isn't needed
	// create different types of nodes using reflection
	public OrcaNode create() {
		OrcaNode node = null;
		String name;

		try{ 
			do {
				name = nodeClass.getName() + nodeClass.getCount();
				Class<?> pars[] = new Class[1];
				pars[0] = String.class;
				Constructor<?> ct = nodeClass.getClazz().getConstructor(pars);
				Object args[] = new Object[1];
				args[0] = name;
				node = (OrcaNode)ct.newInstance(args);
			} while (!checkUniqueNodeName(null, name));
		} catch (Exception e) {
			;
		}
		return node;
	}

	/**
	 * clear the manifest
	 */
	public void clear() {
		// clear the graph, 
		Set<OrcaNode> nodes = new HashSet<OrcaNode>(manifestGraph.getVertices());
		for (OrcaNode n: nodes)
			manifestGraph.removeVertex(n);
	}
	
	
	/**
	 * Check if the link name is unique
	 * @param nm
	 * @return
	 */
	public boolean checkUniqueLinkName(OrcaLink edge, String nm) {
		// check all edges in graph
		Collection<OrcaLink> edges = manifestGraph.getEdges();
		for (OrcaLink e: edges) {
			// check that some other edge doesn't have this name
			if (edge != null) {
				if ((e != edge) &&(e.getName().equals(nm)))
					return false;
			} else
				if (e.getName().equals(nm))
					return false;
		}
		return true;
	}
	
	/**
	 * check if node name is unique. exclude a node if needed (or null)
	 * @param node
	 * @param nm
	 * @return
	 */
	public boolean checkUniqueNodeName(OrcaNode node, String nm) {
		// check all edges in graph
		Collection<OrcaNode> nodes = manifestGraph.getVertices();
		for (OrcaNode n: nodes) {
			// check that some other edge doesn't have this name
			if (node != null) {
				if ((n != node) &&(n.getName().equals(nm)))
					return false;
			} else
				if (n.getName().equals(nm))
					return false;
			
		}
		return true;
	}
}
