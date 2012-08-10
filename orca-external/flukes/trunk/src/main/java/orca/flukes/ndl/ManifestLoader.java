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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import orca.flukes.GUI;
import orca.flukes.GUIManifestState;
import orca.flukes.GUIRequestState;
import orca.flukes.OrcaCrossconnect;
import orca.flukes.OrcaImage;
import orca.flukes.OrcaLink;
import orca.flukes.OrcaNode;
import orca.flukes.OrcaNodeGroup;
import orca.ndl.INdlManifestModelListener;
import orca.ndl.INdlRequestModelListener;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlManifestParser;
import orca.ndl.NdlRequestParser;

import org.apache.commons.lang.StringUtils;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Literal;
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
public class ManifestLoader implements INdlManifestModelListener, INdlRequestModelListener {

	private Map<String, OrcaNode> interfaceToNode = new HashMap<String, OrcaNode>();
	private Map<String, OrcaNode> nodes = new HashMap<String, OrcaNode>();
	private Map<String, OrcaLink> links = new HashMap<String, OrcaLink>();
	boolean requestPhase = true;
	protected Date creationTime = null;
	protected Date expirationTime = null;
	
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

			// parse as request
			NdlRequestParser nrp = new NdlRequestParser(sb.toString(), this);
			// something wrong with request model that is part of manifest
			// some interfaces belong only to nodes, and no connections
			// for now do less strict checking so we can get IP info
			// 07/2012/ib
			nrp.doLessStrictChecking();
			nrp.processRequest();
			
			// parse as manifest
			requestPhase = false;
			NdlManifestParser nmp = new NdlManifestParser(sb.toString(), this);
			nmp.processManifest();
			GUIManifestState.getInstance().setManifestString(sb.toString());
			GUIManifestState.getInstance().launchResourceStateViewer(creationTime, expirationTime);
			
		} catch (Exception e) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while loading file " + f.getName() + ":", e);
			ed.setVisible(true);
			return false;
		} 
		
		return true;
	}
	
	public boolean loadString(String s) {
		
		try {
			// parse as request
			NdlRequestParser nrp = new NdlRequestParser(s, this);
			// something wrong with request model that is part of manifest
			// some interfaces belong only to nodes, and no connections
			// for now do less strict checking so we can get IP info
			// 07/2012/ib
			nrp.doLessStrictChecking();
			nrp.processRequest();
			
			// parse as manifest
			requestPhase = false;
			NdlManifestParser nmp = new NdlManifestParser(s, this);
			nmp.processManifest();	
			GUIManifestState.getInstance().launchResourceStateViewer(creationTime, expirationTime);
			
		} catch (Exception e) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while parsing manifest(m): ", e);
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
			List<Resource> interfaces, Resource parent) {
		//System.out.println("Found link connection " + l + " connecting " + interfaces);
		assert(l != null);
		
		// ignore request items
		if (requestPhase)
			return;
		
		GUI.logger().debug("Link Connection: " + l);
		
		// find what nodes it connects (should be two)
		Iterator<Resource> it = interfaces.iterator(); 
		
		String label = NdlCommons.getResourceLabel(l);
		
		if (interfaces.size() == 2) {
			GUI.logger().debug("  Adding p-to-p link");
			OrcaLink ol = GUIManifestState.getInstance().getLinkCreator().create();

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
					GUI.logger().debug("  Creating a link " + ol.getName() + " from " + if1Node + " to " + if2Node);
					GUIManifestState.getInstance().getGraph().addEdge(ol, new Pair<OrcaNode>(if1Node, if2Node), 
							EdgeType.UNDIRECTED);
				}
			}
			// state
			ol.setState(NdlCommons.getResourceStateAsString(l));
			
			if (ol.getState() != null)
				ol.setIsResource();
			
			// reservation notice
			ol.setReservationNotice(NdlCommons.getResourceReservationNotice(l));
			links.put(getTrueName(l), ol);
		} else {
			GUI.logger().debug("  Adding multi-point crossconnect " + getTrueName(l));
			// multi-point link
			// create a crossconnect then use interfaceToNode mapping to create links to it
			OrcaCrossconnect ml = new OrcaCrossconnect(getTrueName(l));

			ml.setLabel(label);
			ml.setReservationNotice(NdlCommons.getResourceReservationNotice(l));
			ml.setState(NdlCommons.getResourceStateAsString(l));
			
			if (ml.getState() != null)
				ml.setIsResource();
			
			nodes.put(getTrueName(l), ml);
			
			// remember the interfaces
			while(it.hasNext()) {
				Resource intR = it.next();
				interfaceToNode.put(getTrueName(intR), ml);
			}
			
			// add crossconnect to the graph
			GUIManifestState.getInstance().getGraph().addVertex(ml);
			
			// link to this later from interface information
			
			// link nodes (we've already seen them) to it
//			for(Resource intf: interfaces) {
//				if (interfaceToNode.get(getTrueName(intf)) != null) {
//					GUI.logger().debug("  Creating a link " + lcount + " from " + ml + " to " + interfaceToNode.get(getTrueName(intf)));
//					OrcaLink ol = new OrcaLink("Link " + lcount++);
//					GUIManifestState.getInstance().getGraph().addEdge(ol, new Pair<OrcaNode>(ml, interfaceToNode.get(getTrueName(intf))), EdgeType.UNDIRECTED);
//				}
//			}
		}
	}

	@Override
	public void ndlManifest(Resource i, OntModel m) {
		// nothing to do in this case
		
		// ignore request items
		if (requestPhase)
			return;
		
		GUI.logger().debug("Manifest: " + i);
	}

	@Override
	public void ndlInterface(Resource intf, OntModel om, Resource conn,
			Resource node, String ip, String mask) {
		
		// ignore request items
		if (requestPhase)
			return;
		
		// System.out.println("Interface " + l + " has IP/netmask" + ip + "/" + mask);
		GUI.logger().debug("Interface " + intf + " between " + node + " and " + conn + " has IP/netmask " + ip + "/" + mask);
		
		if (intf == null)
			return;
		OrcaNode on = null;
		OrcaLink ol = null;
		OrcaCrossconnect crs = null;
		if (node != null)
			on = nodes.get(getTrueName(node));
		
		if (conn != null) {
			ol = links.get(getTrueName(conn));
			if (ol == null) 
				// maybe it is a crossconnect and not a link connection
				crs = (OrcaCrossconnect)nodes.get(getTrueName(conn));
		}
		
		// extract the IP address from label, if it is not set on
		// the interface in the request (basically we favor manifest
		// setting over the request because in node groups that's the
		// correct one)
		String nmInt = null;
		if (ip == null) {
			String ifIpLabel = NdlCommons.getLabelID(intf);
			// x.y.z.w/24
			if (ifIpLabel != null) {
				String[] ipnm = ifIpLabel.split("/");
				if (ipnm.length == 2) {
					ip = ipnm[0];
					nmInt = ipnm[1];
				}
			}
		} else {
			if (mask != null)
				nmInt = "" + RequestSaver.netmaskStringToInt(mask);
		}
		
		if (on != null) {
			if (ol != null) {
				on.setIp(ol, ip, nmInt);
				on.setInterfaceName(ol, getTrueName(intf));
			} else if (crs != null) {
				if (intf.toString().matches(node.toString() + "/IP/[0-9]+")) {
					// include only interfaces that have nodename/IP/<number> format - those
					// are generated by Yufeng. 

					// create link from node to crossconnect and assign IP if it doesn't exist
					GUI.logger().debug("  Creating a link  from " + on + " to " + crs);
					ol = GUIManifestState.getInstance().getLinkCreator().create();
					GUIManifestState.getInstance().getGraph().addEdge(ol, new Pair<OrcaNode>(on, crs), 
							EdgeType.UNDIRECTED);
					on.setIp(ol, ip, nmInt);
				}
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
		
		// ignore request items
		if (requestPhase)
			return;
		
		// nothing to do in this case
		GUI.logger().debug("Network Connection: " + l);

	}

	@Override
	public void ndlCrossConnect(Resource c, OntModel m, 
			long bw, String label, List<Resource> interfaces, Resource parent) {
		
		// ignore request items
		if (requestPhase)
			return;
		
		if (c == null)
			return;

		GUI.logger().debug("CrossConnect: " + c);
		
		OrcaCrossconnect oc = new OrcaCrossconnect(getTrueName(c));
		oc.setLabel(label);
		
		setCommonNodeProperties(oc, c);
		
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
		
		// ignore request items
		if (requestPhase)
			return;
		
		if (ce == null)
			return;
		
		GUI.logger().debug("Node: " + ce);
		
		OrcaNode newNode;
		
		newNode = new OrcaNode(getTrueName(ce));
		
//		if (ceClass.equals(NdlCommons.computeElementClass))
//			// HACK! if it is a collection, it used to be NODEGROUP
//			if (ce.hasProperty(NdlCommons.collectionElementProperty))
//				newNode = new OrcaCrossconnect(getTrueName(ce));
//			else
//				newNode = new OrcaNode(getTrueName(ce));
//		else { 
//			if (ceClass.equals(NdlCommons.serverCloudClass)) {
//				OrcaNodeGroup newNodeGroup = new OrcaNodeGroup(getTrueName(ce));
//				int ceCount = NdlCommons.getNumCE(ce);
//				if (ceCount > 0)
//					newNodeGroup.setNodeCount(ceCount);
//				newNode = newNodeGroup;
//			} else // default just a node
//				newNode = new OrcaNode(getTrueName(ce));
//		}
		
		// set common properties
		setCommonNodeProperties(newNode, ce);
		
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
		
		// HACK - if we added real interfaces to inner nodes, we don't need link to parent
		boolean innerNodeConnected = false;
		
		for (StmtIterator vmEl = vm.listProperties(NdlCommons.collectionElementProperty); vmEl.hasNext();) {
			Resource tmpR = vmEl.next().getResource();
			OrcaNode on = new OrcaNode(getTrueName(tmpR), parent);
			nodes.put(getTrueName(tmpR), on);
			GUIManifestState.getInstance().getGraph().addVertex(on);
			OrcaLink ol = GUIManifestState.getInstance().getLinkCreator().create();
			
			// link to parent (a visual HACK)
			links.put(ol.getName(), ol);
			GUIManifestState.getInstance().getGraph().addEdge(ol, new Pair<OrcaNode>(parent, on), 
					EdgeType.UNDIRECTED);
			
			// add various properties
			setCommonNodeProperties(on, tmpR);
			
			// process interfaces. if there is an interface that leads to
			// a link, this is an intra-domain case, so we can delete the parent later
			for (Resource intR: NdlCommons.getResourceInterfaces(tmpR)) {
				interfaceToNode.put(getTrueName(intR), on);
				// HACK: for now check that this interface connects to something
				// and is not just hanging there with IP address
				List<Resource> hasI = NdlCommons.getWhoHasInterface(intR, om);
				if (hasI.size() > 1)
					innerNodeConnected = true;
			}
		}
		
		// Hack - remove parent if nodes are linked between themselves
		if (innerNodeConnected)
			GUIManifestState.getInstance().getGraph().removeVertex(parent);
	}
	
	// set common node properties from NDL
	private void setCommonNodeProperties(OrcaNode on, Resource nr) {
		// post boot script
		on.setPostBootScript(NdlCommons.getPostBootScript(nr));
		
		// management IP/port access
		on.setManagementAccess(NdlCommons.getNodeServices(nr));
		
		// state
		on.setState(NdlCommons.getResourceStateAsString(nr));
		
		if (on.getState() != null)
			on.setIsResource();
		
		// reservation notice
		on.setReservationNotice(NdlCommons.getResourceReservationNotice(nr));
		
		// domain
		Resource domain = NdlCommons.getDomain(nr);
		if (domain != null)
			on.setDomain(RequestSaver.reverseLookupDomain(domain));
		
		// specific ce type
		Resource ceType = NdlCommons.getSpecificCE(nr);
		if (ceType != null)
			on.setNodeType(RequestSaver.reverseNodeTypeLookup(ceType));
		
		// substrate info if present
		if (NdlCommons.getEC2WorkerNodeId(nr) != null)
			on.setSubstrateInfo("worker", NdlCommons.getEC2WorkerNodeId(nr));
		if (NdlCommons.getEC2InstanceId(nr) != null)
			on.setSubstrateInfo("instance", NdlCommons.getEC2InstanceId(nr));
		
	}
	
	@Override
	public void ndlParseComplete() {
		// ignore request items
		if (requestPhase)
			return;
		
		// nothing to do in this case
		GUI.logger().debug("Parse complete.");
	}

	@Override
	public void ndlNetworkConnectionPath(Resource c, OntModel m,
			List<Resource> path) {
		
		// ignore request items
		if (requestPhase)
			return;
		
		// nothing to do in this case
		GUI.logger().debug("Network Connection Path: " + c);
	}

	/**
	 * Request items - mostly ignored
	 * 
	 */
	
	
	@Override
	public void ndlBroadcastConnection(Resource bl, OntModel om,
			long bandwidth, List<Resource> interfaces) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void ndlNodeDependencies(Resource ni, OntModel m,
			Set<Resource> dependencies) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void ndlReservation(Resource i, OntModel m) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void ndlReservationEnd(Literal e, OntModel m, Date end) {
		expirationTime = end;
		
	}

	@Override
	public void ndlReservationResources(List<Resource> r, OntModel m) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void ndlReservationStart(Literal s, OntModel m, Date start) {
		creationTime = start;
		
	}

	@Override
	public void ndlReservationTermDuration(Resource d, OntModel m, int years,
			int months, int days, int hours, int minutes, int seconds) {
		if (creationTime == null)
			return;
		Calendar cal = Calendar.getInstance();
		cal.setTime(creationTime);
		cal.add(Calendar.YEAR, years);
		cal.add(Calendar.MONTH, months);
		cal.add(Calendar.DAY_OF_YEAR, days);
		cal.add(Calendar.HOUR, hours);
		cal.add(Calendar.MINUTE, minutes);
		cal.add(Calendar.SECOND, seconds);
		expirationTime = cal.getTime();
	}

	@Override
	public void ndlSlice(Resource sl, OntModel m) {
		// TODO Auto-generated method stub
		
	}

}
