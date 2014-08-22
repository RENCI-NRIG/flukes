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

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import orca.flukes.GUI;
import orca.flukes.GUIManifestState;
import orca.flukes.GUIRequestState;
import orca.flukes.OrcaColor;
import orca.flukes.OrcaColorLink;
import orca.flukes.OrcaCrossconnect;
import orca.flukes.OrcaImage;
import orca.flukes.OrcaLink;
import orca.flukes.OrcaNode;
import orca.flukes.OrcaNodeGroup;
import orca.flukes.OrcaResource;
import orca.flukes.OrcaStitchPort;
import orca.flukes.OrcaStorageNode;
import orca.ndl.INdlColorRequestListener;
import orca.ndl.INdlManifestModelListener;
import orca.ndl.INdlRequestModelListener;
import orca.ndl.NdlCommons;
import orca.ndl.NdlManifestParser;
import orca.ndl.NdlRequestParser;
import orca.ndl.NdlToRSpecHelper;

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
public class ManifestLoader implements INdlManifestModelListener, INdlRequestModelListener , INdlColorRequestListener {

	private static final String NOTICE_GUID_PATTERN = "^Reservation\\s+([a-zA-Z0-9-]+)\\s+.+$";
	private Map<String, List<OrcaNode>> interfaceToNode = new HashMap<String, List<OrcaNode>>();
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
			nrp.addColorListener(this);
			nrp.processRequest();
			nrp.freeModel();
			
			// parse as manifest
			requestPhase = false;
			NdlManifestParser nmp = new NdlManifestParser(sb.toString(), this);
			nmp.processManifest();
			nmp.freeModel();
			GUIManifestState.getInstance().setManifestString(sb.toString());
			GUIManifestState.getInstance().setManifestTerm(creationTime, expirationTime);
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
			nrp.addColorListener(this);
			nrp.processRequest();
			nrp.freeModel();
			
			// parse as manifest
			requestPhase = false;
			NdlManifestParser nmp = new NdlManifestParser(s, this);
			nmp.processManifest();	
			nmp.freeModel();
			
			GUIManifestState.getInstance().setManifestString(s);
			GUIManifestState.getInstance().setManifestTerm(creationTime, expirationTime);

			if (GraphicsEnvironment.isHeadless())
				GUIManifestState.getInstance().printResourceState(creationTime, expirationTime);
			else
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
	
	private String getPrettyName(Resource r) {
		String rname = getTrueName(r);
		int ind = rname.indexOf('#');
		if (ind > 0) {
			rname = rname.substring(ind + 1);
		}
		return rname;
	}
	
	private void addNodeToInterface(String iface, OrcaNode n) {
		List<OrcaNode> others = interfaceToNode.get(iface);
		if (others != null) 
			others.add(n);
		else
			interfaceToNode.put(iface, new ArrayList<OrcaNode>(Arrays.asList(n)));
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
		
		GUI.logger().debug("Link Connection: " + l + " with interfaces " + interfaces);
		
		if (parent != null) {
			GUI.logger().debug("    ignoring due to parent " + parent);
			return;
		}
		
		// find what nodes it connects (should be two)
		Iterator<Resource> it = interfaces.iterator(); 
		
		String label = NdlCommons.getResourceLabel(l);
		
		// limit to link connections not part of a network connection
		if (interfaces.size() == 2){
			GUI.logger().debug("  Adding p-to-p link");
			OrcaLink ol = GUIManifestState.getInstance().getLinkCreator().create(getPrettyName(l), NdlCommons.getResourceBandwidth(l));
			ol.setLabel(label);
			// state
			ol.setState(NdlCommons.getResourceStateAsString(l));
			
			if (ol.getState() != null)
				ol.setIsResource();
			
			// reservation notice
			ol.setReservationNotice(NdlCommons.getResourceReservationNotice(l));
			ol.setReservationGuid(getGuidFromNotice(ol.getReservationNotice()));
			links.put(getTrueName(l), ol);

			// maybe point-to-point link
			// the ends
			Resource if1 = it.next(), if2 = it.next();
			
			boolean usedOnce = false;
			if ((if1 != null) && (if2 != null)) {
				List<OrcaNode> if1List = interfaceToNode.get(getTrueName(if1));
				List<OrcaNode> if2List = interfaceToNode.get(getTrueName(if2));
				
				if (if1List != null) {
					for(OrcaNode if1Node: if1List) {
						if (if2List != null) {
							for (OrcaNode if2Node: if2List) {
								
								if ((if1Node != null) && if1Node.equals(if2Node)) {
									// degenerate case of a node on a shared vlan
									OrcaCrossconnect oc = new OrcaCrossconnect(getPrettyName(l));
									oc.setLabel(label);
									oc.setDomain(RequestSaver.reverseLookupDomain(NdlCommons.getDomain(l)));
									nodes.put(getTrueName(l), oc);
									// save one interface
									//interfaceToNode.put(getTrueName(if1), oc);
									addNodeToInterface(getTrueName(if1), oc);
									GUIManifestState.getInstance().getGraph().addVertex(oc);
									return;
								}
								
								if (!usedOnce) {
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
								} else
									ol = new OrcaLink(ol);
								
								// have to be there
								if ((if1Node != null) && (if2Node != null)) {
									GUI.logger().debug("  Creating a link " + ol.getName() + " from " + if1Node + " to " + if2Node);
									GUIManifestState.getInstance().getGraph().addEdge(ol, new Pair<OrcaNode>(if1Node, if2Node), 
											EdgeType.UNDIRECTED);
									usedOnce = true;
								}
							}
						}
					}
				}
			}

		} else {			
			GUI.logger().debug("  Adding multi-point crossconnect " + getTrueName(l) + " (has " + interfaces.size() + " interfaces)");
			// multi-point link
			// create a crossconnect then use interfaceToNode mapping to create links to it
			OrcaCrossconnect ml = new OrcaCrossconnect(getPrettyName(l));

			ml.setLabel(label);
			ml.setReservationNotice(NdlCommons.getResourceReservationNotice(l));
			ml.setReservationGuid(getGuidFromNotice(ml.getReservationNotice()));
			ml.setState(NdlCommons.getResourceStateAsString(l));
			ml.setDomain(RequestSaver.reverseLookupDomain(NdlCommons.getDomain(l)));

			if (ml.getState() != null)
				ml.setIsResource();
			
			nodes.put(getTrueName(l), ml);
			
			// remember the interfaces
			while(it.hasNext()) {
				Resource intR = it.next();
				GUI.logger().debug("  Remembering interface " + intR + " of " + ml);
				//interfaceToNode.put(getTrueName(intR), ml);
				addNodeToInterface(getTrueName(intR), ml);
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
				on.setMac(ol, NdlCommons.getAddressMAC(intf));
			} else if (crs != null) {
				// for individual nodes
				if (intf.toString().matches(node.toString() + "/IP/[0-9]+")) {
					// include only interfaces that have nodename/IP/<number> format - those
					// are generated by Yufeng. 

					// create link from node to crossconnect and assign IP if it doesn't exist
					GUI.logger().debug("  Creating a link  from " + on + " to " + crs);
					ol = GUIManifestState.getInstance().getLinkCreator().create("Unnamed");
					GUIManifestState.getInstance().getGraph().addEdge(ol, new Pair<OrcaNode>(on, crs), 
							EdgeType.UNDIRECTED);
					on.setIp(ol, ip, nmInt);
					on.setMac(ol, NdlCommons.getAddressMAC(intf));
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

		GUI.logger().debug("CrossConnect: " + c + " with label " + label);
		
		OrcaCrossconnect oc = new OrcaCrossconnect(getPrettyName(c));
		oc.setLabel(label);
		
		setCommonNodeProperties(oc, c);
		
		// later set bandwidth on adjacent links (crossconnects in NDL have
		// bandwidth but for users we'll show it on the links)
		oc.setBandwidth(bw);
		
		// process interfaces
		for (Iterator<Resource> it = interfaces.iterator(); it.hasNext();) {
			Resource intR = it.next();
			//interfaceToNode.put(getTrueName(intR), oc);
			addNodeToInterface(getTrueName(intR), oc);
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
		
		if (NdlCommons.isStitchingNodeInManifest(ce)) {
			GUI.logger().debug("  is a stitching port");
			OrcaStitchPort sp = new OrcaStitchPort(getPrettyName(ce));
			// get the interface (first)
			if (interfaces.size() == 1) {
				sp.setLabel(NdlCommons.getLayerLabelLiteral(interfaces.get(0)));
				if (NdlCommons.getLinkTo(interfaces.get(0)) != null)
					sp.setPort(NdlCommons.getLinkTo(interfaces.get(0)).toString());
			} 
			newNode = sp;
		} else if (NdlCommons.isNetworkStorage(ce)) {
			GUI.logger().debug("  is a storage node");
			newNode = new OrcaStorageNode(getPrettyName(ce));
			newNode.setIsResource();
		} else if (NdlCommons.isMulticastDevice(ce)) {
			GUI.logger().debug("  is a multicast root");
			newNode = new OrcaCrossconnect(getPrettyName(ce));
			newNode.setIsResource();
		} else {
			GUI.logger().debug("  is a regular node");
			newNode = new OrcaNode(getPrettyName(ce));
		}
		
		for (Resource ii: interfaces)
			GUI.logger().debug("  With interface " + ii);
		
		// set common properties
		setCommonNodeProperties(newNode, ce);
		
		// process interfaces
		for (Iterator<Resource> it = interfaces.iterator(); it.hasNext();) {
			Resource intR = it.next();
			//interfaceToNode.put(getTrueName(intR), newNode);
			addNodeToInterface(getTrueName(intR), newNode);
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
			OrcaLink ol = GUIManifestState.getInstance().getLinkCreator().create("Unnamed");
			
			// link to parent (a visual HACK)
			links.put(ol.getName(), ol);
			GUIManifestState.getInstance().getGraph().addEdge(ol, new Pair<OrcaNode>(parent, on), 
					EdgeType.UNDIRECTED);
			
			// add various properties
			setCommonNodeProperties(on, tmpR);
			
			// process interfaces. if there is an interface that leads to
			// a link, this is an intra-domain case, so we can delete the parent later
			for (Resource intR: NdlCommons.getResourceInterfaces(tmpR)) {
				//interfaceToNode.put(getTrueName(intR), on);
				addNodeToInterface(getTrueName(intR), on);
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
		
		if (on.getState() != null) {
			on.setIsResource();
		}
		
		// reservation notice
		on.setReservationNotice(NdlCommons.getResourceReservationNotice(nr));
		on.setReservationGuid(getGuidFromNotice(on.getReservationNotice()));
		
		// domain
		Resource domain = NdlCommons.getDomain(nr);
		if (domain != null)
			on.setDomain(RequestSaver.reverseLookupDomain(domain));
		
		// url
		on.setUrl(nr.getURI());
		
		// group (if any)
		String groupUrl = NdlCommons.getRequestGroupURLProperty(nr);
		// group URL same as my URL means I'm a single node
		if ((groupUrl != null) &&
				groupUrl.equals(on.getUrl()))
			groupUrl = null;
		on.setGroup(groupUrl);
		
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
		
		// process colors
		processColors();
		
		// nothing to do in this case
		GUI.logger().debug("Parse complete.");
	}

	@Override
	public void ndlNetworkConnectionPath(Resource c, OntModel m,
			List<List<Resource>> paths, List<Resource> roots) {

		// ignore request items
		if (requestPhase)
			return;

		GUI.logger().debug("Network Connection Path: " + c);
		if (roots != null) {
			GUI.logger().debug("Printing roots");
			for (Resource rr: roots) {
				GUI.logger().debug(rr);
			}
		}
		if (paths != null) {
			GUI.logger().debug("Printing paths");
			for (List<Resource> p: paths) {
				StringBuilder sb =  new StringBuilder();
				sb.append("   Path: ");
				for (Resource r: p) {
					sb.append(r + " ");
				}
				GUI.logger().debug(sb.toString());
				
				Iterator<Resource> pIter = p.iterator();
				Resource first = pIter.next();
				if (first == null) 
					continue;
				while(pIter.hasNext()) {
					// only take nodes, skip interfaces on the path
					pIter.next();
					if (!pIter.hasNext())
						break;
					Resource second = pIter.next();
					OrcaNode firstNode = nodes.get(getTrueName(first));
					OrcaNode secondNode = nodes.get(getTrueName(second));
					if (secondNode == null)
						break;

					GUI.logger().debug("  Adding p-to-p link");
					OrcaLink ol = GUIManifestState.getInstance().getLinkCreator().create("Unnamed");
					
					GUI.logger().debug("  Creating a link " + ol.getName() + " from " + first + " to " + second);
					GUIManifestState.getInstance().getGraph().addEdge(ol, new Pair<OrcaNode>(firstNode, secondNode), 
							EdgeType.UNDIRECTED);
					
					first = second;
				}
			}

		} else 
			GUI.logger().debug("   None");
		
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
		if ((years == 0) && (months == 0) && (days == 0) && (hours == 0) && (minutes == 0) && (seconds == 0))
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

	//
	// Dealing with color - early
	//
	
	private class NEColor {
		Resource ne;
		OrcaColor oc;
		
		NEColor(Resource n, OrcaColor o) {
			ne = n;
			oc = o;
		}
	}

	private class ColorDependency {
		Resource fromNe, toNe;
		OrcaColorLink ocl;
		
		ColorDependency(Resource f, Resource t, OrcaColorLink o) {
			fromNe = f;
			toNe = t;
			ocl = o;
		}
	}
	
	private List<NEColor> necolors = new ArrayList<NEColor>();
	private List<ColorDependency> colorDependencies = new ArrayList<ColorDependency>();
	
	@Override
	public void ndlResourceColor(Resource ne, Resource color, String label) {
			
		OrcaColor oc = new OrcaColor(label);
		oc.addKeys(NdlCommons.getColorKeys(color));
		if (NdlCommons.getColorBlob(color) != null)
			oc.setBlob(NdlCommons.getColorBlob(color));
		else { 
			oc.setBlob(NdlToRSpecHelper.stripXmlNs(NdlToRSpecHelper.stripXmlHead(NdlCommons.getColorBlobXML(color, true))));
			oc.setXMLBlobState(true);
		}

		necolors.add(new NEColor(ne, oc));

	}

	@Override
	public void ndlColorDependency(Resource fromNe, Resource toNe,
			Resource color, String label) {
		

		
		OrcaColorLink ocl = new OrcaColorLink(label);
		
		ocl.getColor().addKeys(NdlCommons.getColorKeys(color));
		if (NdlCommons.getColorBlob(color) != null)
			ocl.getColor().setBlob(NdlCommons.getColorBlob(color));
		else { 
			ocl.getColor().setBlob(NdlToRSpecHelper.stripXmlNs(NdlToRSpecHelper.stripXmlHead(NdlCommons.getColorBlobXML(color, true))));
			ocl.getColor().setXMLBlobState(true);
		}
	
		colorDependencies.add(new ColorDependency(fromNe, toNe, ocl));

	}
	
	/**
	 * Re-add colors collected previously in request parse phase
	 */
	private void processColors() {
		
		// attach colors to network elements
		for(NEColor nec: necolors) {
			OrcaResource or = null;
			if (nodes.get(getTrueName(nec.ne)) != null)
				or = nodes.get(getTrueName(nec.ne));
			else if (links.get(getTrueName(nec.ne)) != null)
				or = links.get(getTrueName(nec.ne));
			
			if (or != null) {
				or.addColor(nec.oc);
			} 
		}
		
		// add dependencies between elements
		for(ColorDependency cd: colorDependencies) {
			OrcaNode fromOr = null, toOr = null;
			
			fromOr = nodes.get(getTrueName(cd.fromNe));
			toOr = nodes.get(getTrueName(cd.toNe));
			
			if ((fromOr == null) || (toOr == null)) {
				return;
			}
			GUIManifestState.getInstance().getGraph().addEdge(cd.ocl, new Pair<OrcaNode>(fromOr, toOr), EdgeType.UNDIRECTED);
		}
	}
	
	/**
	 * As a temporary measure we allow extracting guid from reservation notice /ib 08/20/14
	 */
	private static Pattern noticeGuidPattern = Pattern.compile(NOTICE_GUID_PATTERN);
	private static String getGuidFromNotice(String notice) {
		java.util.regex.Matcher m = noticeGuidPattern.matcher(notice);
		if (m.matches()) 
			return m.group(1);
		return null;
	}
	
	public static void main(String[] argv) {
		String msg = "Reservation a6dae2e0-fe47-472c-87fe-875a3d841e7a (Slice geno-snapshot) is in state [Active,None]";
		System.out.println(getGuidFromNotice(msg));
	}
}
