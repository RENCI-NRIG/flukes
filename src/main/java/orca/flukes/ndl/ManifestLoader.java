/*
* Copyright (c) 2011 RENCI/UNC Chapel Hill 
*
* @author Ilia Baldine
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
* and/or hardware specification (the "Work") to deal in the Work without restriction, including 
* without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
* sell copies of the Work, and to permit persons to whom the Work is furnished to do so, subject to 
* the following conditions:  
* The above copyright notice and this permission notice shall be included in all copies or 
* substantial portions of the Work.  
*
* THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
* OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS 
* IN THE WORK.
*/
package orca.flukes.ndl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import orca.flukes.GUI;
import orca.flukes.GUIManifestState;
import orca.flukes.GUIRequestState;
import orca.flukes.OrcaCrossconnect;
import orca.flukes.OrcaImage;
import orca.flukes.OrcaLink;
import orca.flukes.OrcaNode;
import orca.flukes.OrcaNodeGroup;
import orca.ndl.INdlManifestModelListener;
import orca.ndl.NdlCommons;
import orca.ndl.NdlManifestParser;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hyperrealm.kiwi.ui.dialog.ExceptionDialog;

import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * Class for loading manifests
 * @author ibaldin
 *
 */
public class ManifestLoader implements INdlManifestModelListener {

	private Map<String, OrcaNode> interfaceToNode = new HashMap<String, OrcaNode>();
	private Map<String, OrcaNode> nodes = new HashMap<String, OrcaNode>();
	private Map<String, OrcaLink> links = new HashMap<String, OrcaLink>();
	
	private static int lcount = 0;
	
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
			
			NdlManifestParser nrp = new NdlManifestParser(sb.toString(), this);
			nrp.processManifest();
			
		} catch (Exception e) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while loading file " + f.getName() + ":", e);
			ed.setVisible(true);
			return false;
		} 
		
		return true;
	}

	// sometimes getLocalName is not good enough
	// so we strip off orca name space and call it a day
	private String getTrueName(Resource r) {
		if (r == null)
			return null;
		
		return StringUtils.removeStart(r.getURI(), NdlCommons.ORCA_NS);
	}
	
	// get domain name from inter-domain resource name
	private String getInterDomainName(Resource r) {
		String trueName = getTrueName(r);
		
		if (r == null)
			return null;
		
		String[] split = trueName.split("#");
		if (split.length >= 2) {
			String rem = split[1];
			String[] split1 = rem.split("/");
			return split1[0];
		}	
		return null;
	}
	
	@Override
	public void ndlLinkConnection(Resource l, OntModel m,
			List<Resource> interfaces) {
		// System.out.println("Found connection " + l + " connecting " + interfaces + " with bandwidth " + bandwidth);
		if (l == null)
			return;
		OrcaLink ol = new OrcaLink("Link " + lcount++);
		
		// find what nodes it connects (should be two)
		Iterator<Resource> it = interfaces.iterator(); 
		
		if (interfaces.size() == 2) {
			// point-to-point link
			// the ends
			Resource if1 = it.next(), if2 = it.next();
			
			if ((if1 != null) && (if2 != null)) {
				OrcaNode if1Node = interfaceToNode.get(getTrueName(if1));
				OrcaNode if2Node = interfaceToNode.get(getTrueName(if2));
				
				// get the bandwidth of crossconnects if possible
				long bw1 = 0, bw2 = 0;
				if (if1Node instanceof OrcaCrossconnect) {
					OrcaCrossconnect oc = (OrcaCrossconnect)if1Node;
					bw1 = oc.getBandwidth();
				} 
				if (if2Node instanceof OrcaCrossconnect) {
					OrcaCrossconnect oc = (OrcaCrossconnect)if2Node;
					bw2 = oc.getBandwidth();
				}
				ol.setBandwidth(bw1 > bw2 ? bw1 : bw2);
				
				// have to be there
				if ((if1Node != null) && (if2Node != null)) {
					GUIManifestState.getInstance().getGraph().addEdge(ol, new Pair<OrcaNode>(if1Node, if2Node), 
							EdgeType.UNDIRECTED);
				}
			}
		} else {
			// multi-point link
			
		}
		links.put(getTrueName(l), ol);

	}

	@Override
	public void ndlReservation(Resource i, OntModel m) {
		// nothing to do in this case

	}

	@Override
	public void ndlInterface(Resource intf, OntModel om, Resource conn,
			Resource node, String ip, String mask) {
		// System.out.println("Interface " + l + " has IP/netmask" + ip + "/" + mask);
		if (intf == null)
			return;
		OrcaNode on = null;
		OrcaLink ol = null;
		if (node != null)
			on = nodes.get(getTrueName(node));
		if (conn != null)
			ol = links.get(getTrueName(conn));
		
		if (on != null) {
			if (ol != null) {
				on.setIp(ol, ip, "" + RequestSaver.netmaskStringToInt(mask));
				on.setInterfaceName(ol, getTrueName(intf));
			}
			else {
				// this could be a disconnected node group
				if (on instanceof OrcaNodeGroup) {
					OrcaNodeGroup ong = (OrcaNodeGroup)on;
					ong.setInternalIp(ip, "" + RequestSaver.netmaskStringToInt(mask));
				}
			}
				
		}
	}

	@Override
	public void ndlNetworkConnection(Resource l, OntModel om, long bandwidth,
			long latency, List<Resource> interfaces) {
		// nothing to do in this case

	}

	@Override
	public void ndlCrossConnect(Resource c, OntModel m, 
			long bw, String label, List<Resource> interfaces) {
		
		if (c == null)
			return;

		OrcaCrossconnect oc = new OrcaCrossconnect(getTrueName(c));
		
		// extract domain from the name: getTrueName gets us blah.rdf#domain-name/Domain/vlan
		oc.setDomain(getInterDomainName(c));
		
		oc.setLabel(label);
		
		oc.setState(NdlCommons.getResourceStateAsString(c));
		
		// later set bandwidth on adjacent links (crossconnects in NDL have
		// bandwidth but for users we'll show it on the links)
		oc.setBandwidth(bw);
		
		// process interfaces
		for (Iterator<Resource> it = interfaces.iterator(); it.hasNext();) {
			Resource intR = it.next();
			interfaceToNode.put(getTrueName(intR), oc);
		}
		
		nodes.put(getTrueName(c), oc);
		
		// add nodes to the graph
		GUIManifestState.getInstance().getGraph().addVertex(oc);
	}
	
	@Override
	public void ndlNode(Resource ce, OntModel om, Resource ceClass,
			List<Resource> interfaces) {
		if (ce == null)
			return;
		OrcaNode newNode;
		
		if (ceClass.equals(NdlCommons.computeElementClass))
			// HACK! if it is a collection, it used to be NODEGROUP
			if (ce.hasProperty(NdlCommons.collectionElementProperty))
				newNode = new OrcaCrossconnect(getTrueName(ce));
			else
				newNode = new OrcaNode(getTrueName(ce));
		else { 
			if (ceClass.equals(NdlCommons.serverCloudClass)) {
				OrcaNodeGroup newNodeGroup = new OrcaNodeGroup(getTrueName(ce));
				int ceCount = NdlCommons.getNumCE(ce);
				if (ceCount > 0)
					newNodeGroup.setNodeCount(ceCount);
				newNode = newNodeGroup;
			} else // default just a node
				newNode = new OrcaNode(getTrueName(ce));
		}
		
		// state
		newNode.setState(NdlCommons.getResourceStateAsString(ce));
		
		// domain
		Resource domain = NdlCommons.getDomain(ce);
		if (domain != null)
			newNode.setDomain(RequestSaver.reverseLookupDomain(domain));
		
		// specific ce type
		Resource ceType = NdlCommons.getSpecificCE(ce);
		if (ceType != null)
			newNode.setNodeType(RequestSaver.reverseNodeTypeLookup(ceType));
		
		// post boot script
		newNode.setPostBootScript(NdlCommons.getPostBootScript(ce));

		// management IP/port access
		newNode.setManagementAccess(NdlCommons.getNodeServices(ce));
		
		// process interfaces
		for (Iterator<Resource> it = interfaces.iterator(); it.hasNext();) {
			Resource intR = it.next();
			interfaceToNode.put(getTrueName(intR), newNode);
		}
		
		// disk image
		Resource di = NdlCommons.getDiskImage(ce);
		if (di != null) {
			try {
				String imageURL = NdlCommons.getIndividualsImageURL(ce);
				String imageHash = NdlCommons.getIndividualsImageHash(ce);
				GUIRequestState.getInstance().addImage(new OrcaImage(di.getLocalName(), 
						new URL(imageURL), imageHash), null);
				newNode.setImage(di.getLocalName());
			} catch (Exception e) {
				// FIXME: ?
				;
			}
		}
		
		nodes.put(getTrueName(ce), newNode);
		
		// add nodes to the graph
		GUIManifestState.getInstance().getGraph().addVertex(newNode);
		
		// are there nodes hanging off of it as elements? if so, link them in
		processDomainVmElements(ce, om, newNode);
	}

	// add collection elements
	private void processDomainVmElements(Resource vm, OntModel om, OrcaNode parent) {
		
		for (StmtIterator vmEl = vm.listProperties(NdlCommons.collectionElementProperty); vmEl.hasNext();) {
			Resource tmpR = vmEl.next().getResource();
			OrcaNode on = new OrcaNode(getTrueName(tmpR), parent);
			nodes.put(getTrueName(tmpR), on);
			GUIManifestState.getInstance().getGraph().addVertex(on);
			OrcaLink ol = GUIManifestState.getInstance().getLinkCreator().create();
			links.put(ol.getName(), ol);
			GUIManifestState.getInstance().getGraph().addEdge(ol, new Pair<OrcaNode>(parent, on), 
					EdgeType.UNDIRECTED);
			// add various properties
			// post boot script
			on.setPostBootScript(NdlCommons.getPostBootScript(tmpR));
			
			// management IP/port access
			on.setManagementAccess(NdlCommons.getNodeServices(tmpR));
		}
	}
	
	@Override
	public void ndlParseComplete() {
		// nothing to do in this case

	}

	@Override
	public void ndlNetworkConnectionPath(Resource c, OntModel m,
			List<Resource> path) {
		// nothing to do in this case
		
	}

}