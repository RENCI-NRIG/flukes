package orca.flukes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import orca.ndl.INdlRequestModelListener;
import orca.ndl.NdlCommons;
import orca.ndl.NdlRequestParser;

import org.apache.commons.lang.StringUtils;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hyperrealm.kiwi.ui.dialog.ExceptionDialog;

import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

public class GraphLoader implements INdlRequestModelListener {

	private static GraphLoader instance;
	private OrcaReservationTerm term = new OrcaReservationTerm();
	private Map<String, OrcaNode> nodes = new HashMap<String, OrcaNode>();
	private Map<String, OrcaLink> links = new HashMap<String, OrcaLink>();
	private Map<String, OrcaNode> interfaceToNode = new HashMap<String, OrcaNode>();
	
	/**
	 * Return singleton
	 * @return
	 */
	public static GraphLoader getInstance() {
		if (instance == null)
			instance = new GraphLoader();
		return instance;
	}
	
	public boolean loadGraph(File f) {
		BufferedReader bin = null; 
		try {
			FileInputStream is = new FileInputStream(f);
			bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			
			StringBuilder sb = new StringBuilder();
			String line = null;
			while((line = bin.readLine()) != null) {
				sb.append(line);
				// re-add line separator
				sb.append(System.getProperty("line.separator"));
			}
			
			bin.close();
			
			NdlRequestParser nrp = new NdlRequestParser(sb.toString(), this);
			nrp.processRequest();
			
		} catch (Exception e) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while loading file " + f.getName() + ":", e);
			ed.setVisible(true);
			return false;
		} 
		
		return true;
	}
	
	public void ndlNodeDiskImage(Resource di, OntModel m, Resource node,
			String url, String hash) {
		if ((node == null) || (di == null))
			return;
		try {
			GUIState.getInstance().addImage(new OrcaImage(di.getLocalName(), new URL(url), hash), null);
			// get this node and assign image
			if (!nodes.containsKey(node.getLocalName())) 
				// node must exist
				return;
			nodes.get(node.getLocalName()).setImage(di.getLocalName());
		} catch (Exception e) {
			// FIXME: ?
			;
		}
		
	}

	public void ndlReservation(Resource i, final OntModel m) {
		// Nothing to do
		//System.out.println("Found reservation " + i.getLocalName() + " uri " + i.getURI());
	}

	public void ndlReservationDiskImage(Resource di, OntModel m,
			Resource res, String url, String hash) {
		//System.out.println("Disk image " + di.toString() + " " + url + " " + hash);
		if ((di == null) || (res == null))
			return;
		try {
			GUIState.getInstance().addImage(new OrcaImage(di.getLocalName(), new URL(url), hash), null);
			// assign image to reservation
			GUIState.getInstance().setVMImageInReservation(di.getLocalName());
		} catch (Exception e) {
			// FIXME: ?
			;
		}
	}

	public void ndlReservationEnd(Literal e, OntModel m, Date end) {
		// Nothing to do
	}

	public void ndlReservationStart(Literal s, OntModel m, Date start) {
		term.setStart(start);
	}

	public void ndlReservationTermDuration(Resource t, OntModel m, int years, int months, int days,
			int hours, int minutes, int seconds) {
		term.setDuration(days, hours, minutes);
	}

	public void ndlComputeElement(Resource ce, OntModel om, boolean isNode, Resource domain, 
			Resource ceType, int ceCount, List<Resource> interfaces) {

		if (ce == null)
			return;
		OrcaNode newNode = new OrcaNode(ce.getLocalName(), isNode);
		if (domain != null)
			newNode.setDomain(GraphSaver.reverseLookupDomain(domain));
		if (ceType != null)
			newNode.setNodeType(GraphSaver.reverseNodeTypeLookup(ceType));
		if (ceCount > 0)
			newNode.setNodeCount(ceCount);

		// process interfaces
		for (Iterator<Resource> it = interfaces.iterator(); it.hasNext();) {
			Resource intR = it.next();
			interfaceToNode.put(intR.getLocalName(), newNode);
		}
		
		// should we do something with interfaces here? 
		nodes.put(ce.getLocalName(), newNode);
		
		// add nodes to the graph
		GUIState.getInstance().g.addVertex(newNode);
	}

	/**
	 * For now deals only with p-to-p connections
	 */
	public void ndlConnection(Resource l, OntModel om, 
			long bandwidth, long latency, List<Resource> interfaces) {
		// System.out.println("Found connection " + l + " connecting " + interfaces + " with bandwidth " + bandwidth);
		if (l == null)
			return;
		OrcaLink ol = new OrcaLink(l.getLocalName());
		ol.setBandwidth(bandwidth);
		ol.setLatency(latency);
		
		// find what nodes it connects (should be two)
		Iterator<Resource> it = interfaces.iterator(); 
		
		if (interfaces.size() == 2) {
			// point-to-point link
			// the ends
			Resource if1 = it.next(), if2 = it.next();
			
			if ((if1 != null) && (if2 != null)) {
				OrcaNode if1Node = interfaceToNode.get(if1.getLocalName());
				OrcaNode if2Node = interfaceToNode.get(if2.getLocalName());
				
				// have to be there
				if ((if1Node != null) && (if2Node != null)) {
					GUIState.getInstance().g.addEdge(ol, new Pair<OrcaNode>(if1Node, if2Node), EdgeType.UNDIRECTED);
				}
			}
		} else {
			// multi-point link
			
		}
		links.put(l.getLocalName(), ol);
	}

	public void ndlInterface(Resource intf, OntModel om, Resource conn, Resource node, String ip, String mask) {
		// System.out.println("Interface " + l + " has IP/netmask" + ip + "/" + mask);
		if (intf == null)
			return;

		OrcaNode on = nodes.get(node.getLocalName());
		OrcaLink ol = links.get(conn.getLocalName());
		if ((on != null) && (ol != null))
			on.setIp(ol, ip, "" + GraphSaver.netmaskStringToInt(mask));
	}

	public void ndlReservationResources(List<Resource> res, OntModel m) {
		// nothing to do here in this case
	}
	
	public void ndlParseComplete() {
		// set term etc
		GUIState.getInstance().setTerm(term);
	}
	
	public void ndlReservationDomain(Resource d, OntModel m) {
		if (d == null)
			return;
		System.out.println("Setting reservation domain " + d + " " + GraphSaver.reverseLookupDomain(d));
		// do reverse lookup in GraphSaver domain map
		GUIState.getInstance().setDomainInReservation(GraphSaver.reverseLookupDomain(d));
	}

}
