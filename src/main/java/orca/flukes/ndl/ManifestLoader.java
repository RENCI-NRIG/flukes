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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hyperrealm.kiwi.ui.dialog.ExceptionDialog;

import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import orca.flukes.GUI;
import orca.flukes.GUIImageList;
import orca.flukes.GUIUnifiedState;
import orca.flukes.OrcaColor;
import orca.flukes.OrcaColorLink;
import orca.flukes.OrcaCrossconnect;
import orca.flukes.OrcaImage;
import orca.flukes.OrcaLink;
import orca.flukes.OrcaNode;
import orca.flukes.OrcaNodeGroup;
import orca.flukes.OrcaResource;
import orca.flukes.OrcaResource.ResourceType;
import orca.flukes.OrcaStitchPort;
import orca.flukes.OrcaStorageNode;
import orca.ndl.DomainResourceType;
import orca.ndl.INdlColorRequestListener;
import orca.ndl.INdlManifestModelListener;
import orca.ndl.INdlRequestModelListener;
import orca.ndl.NdlCommons;
import orca.ndl.NdlManifestParser;
import orca.ndl.NdlRequestParser;
import orca.ndl.NdlToRSpecHelper;

/**
 * Class for loading manifests
 * @author ibaldin
 *
 */
public class ManifestLoader implements INdlManifestModelListener, INdlRequestModelListener , INdlColorRequestListener {

	private static final String NOTICE_GUID_PATTERN = "^Reservation\\s+([a-zA-Z0-9-]+)\\s+.*(\\s+.*)*$";
	private Map<String, List<OrcaNode>> interfaceToNode = new HashMap<String, List<OrcaNode>>();
	private Set<Resource> sameAs = new HashSet<>();
	private Map<String, OrcaNode> nodes = new HashMap<String, OrcaNode>();
	private Map<String, OrcaLink> links = new HashMap<String, OrcaLink>();
	boolean requestPhase = true;
	protected Date creationTime = null;
	protected Date expirationTime = null;
	
	// these are to keep shared storage interfaces straight until the model gets better
	public static class SharedStorageInterfaces {
		// each LinkConnection for storage vlan has 4+ interfaces - storage, node(s) [with IP/generated by controller and without IP/from request] 
		// keep matching pairs of IPed and Non-IPed interfaces
		private Resource noIp, ip;
		
		public void set(final Resource i, final Resource n) {
			ip = i;
			noIp = n;
		}
		
		public void setIp(final Resource i) {
			ip = i;
		}
		
		public void setNoIp(final Resource n) {
			noIp = n;
		}
		
		public Resource getIp() {
			return ip;
		}
		
		public Resource getNoIp() {
			return noIp;
		}
	}
	private Map<Resource, Resource> sharedStorageLinkInterfaceEquivalence = new HashMap<Resource, Resource>();
	
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

			GUIUnifiedState.getInstance().clearGuidMap();
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
			GUIUnifiedState.getInstance().setManifestString(sb.toString());
			GUIUnifiedState.getInstance().setManifestTerm(creationTime, expirationTime);
			//GUIUnifiedState.getInstance().launchResourceStateViewer(creationTime, expirationTime);
			
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
			GUIUnifiedState.getInstance().clearGuidMap();
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
			
			GUIUnifiedState.getInstance().setManifestString(s);
			GUIUnifiedState.getInstance().setManifestTerm(creationTime, expirationTime);

			if (GraphicsEnvironment.isHeadless())
				GUIUnifiedState.getInstance().printResourceState(creationTime, expirationTime);
			else {
				//GUIUnifiedState.getInstance().launchResourceStateViewer(creationTime, expirationTime);
			}
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
		// also cut off everything after first '/' if present
		ind = rname.indexOf('/');
		if (ind > 0) {
			rname = rname.substring(0, ind);
		}
		return rname;
	}
	
	private void addNodeToInterface(Resource iface, OrcaNode n) {

		String ifaceName = getTrueName(iface);
		GUI.logger().debug("Considering adding interface " + ifaceName + " to the list");
		
		Resource sameIface = NdlCommons.getSameAsResource(iface);
		if (sameIface != null) {
			GUI.logger().debug("Interface " + iface + " same as " + sameIface);
			// add to the set of duplicates and remove any previous copies
			sameAs.add(sameIface);
			interfaceToNode.remove(getTrueName(sameIface));
		} 
		// we don't need interfaces to which 'sameAs' points
		if (sameAs.contains(iface))
			return;
		
		GUI.logger().debug("Adding interface " + ifaceName + " of node " + n + " to the list");
		List<OrcaNode> others = interfaceToNode.get(ifaceName);
		if (others != null) 
			others.add(n);
		else
			interfaceToNode.put(ifaceName, new ArrayList<OrcaNode>(Arrays.asList(n)));
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
		
		// ignore links that are part of network connections
		if (parent != null) {
			GUI.logger().debug("    ignoring due to parent " + parent);
			return;
		}
		
		Iterator<Resource> it = interfaces.iterator(); 
		
		String label = NdlCommons.getResourceLabel(l);
		
		// limit to link connections not part of a network connection
		if (interfaces.size() == 2){
			GUI.logger().debug("  Adding p-to-p link");
			OrcaLink ol = GUIUnifiedState.getInstance().getLinkCreator().create(getPrettyName(l), NdlCommons.getResourceBandwidth(l), ResourceType.MANIFEST);
			ol.setLabel(label);
			ol.setUrl(l.getURI());
			// state
			ol.setState(NdlCommons.getResourceStateAsString(l));
			
			if (ol.getState() != null)
				ol.setIsResource();
			
			// reservation notice
			ol.setReservationNotice(NdlCommons.getResourceReservationNotice(l));
			ol.setReservationGuid(getGuidFromNotice(ol.getReservationNotice()));
			GUIUnifiedState.getInstance().mapGuidToResource(ol.getReservationGuid(), ol);
			links.put(getTrueName(l), ol); 

			// guid
			setRequestGuid(l, ol);
			
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
									oc.setUrl(l.getURI());
									oc.setDomain(RequestSaver.reverseLookupDomain(NdlCommons.getDomain(l)));
									nodes.put(getTrueName(l), oc);
									// save one interface
									//interfaceToNode.put(getTrueName(if1), oc);
									addNodeToInterface(if1, oc);
									GUIUnifiedState.getInstance().getGraph().addVertex(oc);
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
									GUIUnifiedState.getInstance().getGraph().addEdge(ol, new Pair<OrcaNode>(if1Node, if2Node), 
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
			ml.setUrl(l.getURI());
			ml.setReservationNotice(NdlCommons.getResourceReservationNotice(l));
			ml.setReservationGuid(getGuidFromNotice(ml.getReservationNotice()));
			GUIUnifiedState.getInstance().mapGuidToResource(ml.getReservationGuid(), ml);
			ml.setState(NdlCommons.getResourceStateAsString(l));
			ml.setDomain(RequestSaver.reverseLookupDomain(NdlCommons.getDomain(l)));

			if (ml.getState() != null)
				ml.setIsResource();
			
			setRequestGuid(l, ml);
			
			nodes.put(getTrueName(l), ml);
			
			// special case handling - if storage is one side of the link
			// (only for shared vlan storage), then we only remember
			// interfaces with no ip addresses on them (others are duplicates; 
			// we save the equivalence relationship to lookup IP later) /ib 09/18/14
			boolean sharedVlanStorageLink = false;
			Map<Resource, List<Resource>> interfacesByOwner = new HashMap<Resource, List<Resource>>();
			
			for(Resource ti: interfaces) {
				List<Resource> attached = NdlCommons.getWhoHasInterface(ti, m);
				if (attached == null)
					continue;
				for(Resource ta: attached) {
					// skip links (self included)
					if (NdlCommons.hasResourceType(ta, NdlCommons.topologyLinkConnectionClass))
						continue;
					if (NdlCommons.isISCSINetworkStorage(ta)) {
						sharedVlanStorageLink = true;
					}
					List<Resource> tmp = interfacesByOwner.get(ta);
					if (tmp == null)
						tmp = new ArrayList<Resource>();
					tmp.add(ti);
					interfacesByOwner.put(ta, tmp);
				}
			}

			// map ip/noip interfaces to each other
			for(Map.Entry<Resource, List<Resource>> ee: interfacesByOwner.entrySet()) {
				// for regular nodes and storage, not for node groups
				if (ee.getValue().size() == 2) {
					Resource noIp = null, ip = null;
					if (NdlCommons.getInterfaceIP(ee.getValue().get(0)) == null) {
						noIp = ee.getValue().get(0);
						ip = ee.getValue().get(1);
					} else {
						noIp = ee.getValue().get(1);
						ip = ee.getValue().get(0);
					}
					sharedStorageLinkInterfaceEquivalence.put(noIp, ip);
					sharedStorageLinkInterfaceEquivalence.put(ip, noIp);
				}
			}

			// remember the interfaces
			while(it.hasNext()) {
				Resource intR = it.next();
				//interfaceToNode.put(getTrueName(intR), ml);
				if (sharedVlanStorageLink) {
					// does it have an IP address? - then we use it (for node groups this ends up false)
					if ((NdlCommons.getInterfaceIP(intR) != null) && sharedStorageLinkInterfaceEquivalence.containsKey(intR)) {
						// if it has IP, it is shared and we don't need it, however we need
						// IP address from it
						GUI.logger().debug("  Skipping/deleting interface " + intR + " of " + ml + " that has IP address");
						interfaceToNode.remove(getTrueName(intR));
					} else {
						// if it doesn't have IP, we need it for proper topology visualization
						GUI.logger().debug("  Remembering interface " + intR + " of " + ml);
						addNodeToInterface(intR, ml);
					}
				} else {
					GUI.logger().debug("  Remembering interface " + intR + " of " + ml);					
					addNodeToInterface(intR, ml);
				}
			}
			
			// add crossconnect to the graph
			GUIUnifiedState.getInstance().getGraph().addVertex(ml);
			
			// link to this later from interface information
			
			// link nodes (we've already seen them) to it
//			for(Resource intf: interfaces) {
//				if (interfaceToNode.get(getTrueName(intf)) != null) {
//					GUI.logger().debug("  Creating a link " + lcount + " from " + ml + " to " + interfaceToNode.get(getTrueName(intf)));
//					OrcaLink ol = new OrcaLink("Link " + lcount++);
//					GUIUnifiedState.getInstance().getGraph().addEdge(ol, new Pair<OrcaNode>(ml, interfaceToNode.get(getTrueName(intf))), EdgeType.UNDIRECTED);
//				}
//			}
		}
	}

	@Override
	public void ndlManifest(Resource i, OntModel m) {
		
		// set the slice guid based on URL
		
		// nothing to do in this case
		
		// ignore request items
		if (requestPhase)
			return;
		
		GUI.logger().debug("Manifest: " + i);
	}

	private void setRequestGuid(Resource nr, OrcaResource or) {
		if (NdlCommons.getGuidProperty(nr) != null)
			or.setRequestGuid(NdlCommons.getGuidProperty(nr));
		else
			or.setRequestGuid("not available");
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
		
		// check mirrored storage interfaces
		if ((ip == null) && (sharedStorageLinkInterfaceEquivalence.containsKey(intf))){
			String ifIpLabel = NdlCommons.getLabelID(sharedStorageLinkInterfaceEquivalence.get(intf));
			// x.y.z.w/24
			if (ifIpLabel != null) {
				String[] ipnm = ifIpLabel.split("/");
				if (ipnm.length == 2) {
					ip = ipnm[0];
					nmInt = ipnm[1];
				}
			}
		}
		
		if (on != null) {
			if (ol != null) {
				on.setIp(ol, ip, nmInt);
				on.setInterfaceName(ol, getTrueName(intf));
				on.setMac(ol, NdlCommons.getAddressMAC(intf));
			} else if (crs != null) {
				// for individual nodes
				// create link from node to crossconnect and assign IP if it doesn't exist
				
				// check if the nodes are listed in the map
				
				if (interfaceToNode.get(getTrueName(intf)) != null) {
					ol = GUIUnifiedState.getInstance().getLinkCreator().create("Unnamed", ResourceType.MANIFEST);
					GUI.logger().debug("  Creating a link " + ol.getName() + " from " + on + " to " + crs);
					GUIUnifiedState.getInstance().getGraph().addEdge(ol, new Pair<OrcaNode>(on, crs), 
							EdgeType.UNDIRECTED);
				} else
					GUI.logger().debug("  Skipping a link from " + on + " to " + crs + " as interface isn't remembered");
				on.setIp(ol, ip, nmInt);
				on.setMac(ol, NdlCommons.getAddressMAC(intf));
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
		oc.setUrl(c.getURI());
		
		setCommonNodeProperties(oc, c);
		
		// later set bandwidth on adjacent links (crossconnects in NDL have
		// bandwidth but for users we'll show it on the links)
		oc.setBandwidth(bw);
		
		// process interfaces
		for (Iterator<Resource> it = interfaces.iterator(); it.hasNext();) {
			Resource intR = it.next();
			//interfaceToNode.put(getTrueName(intR), oc);
			addNodeToInterface(intR, oc);
		}
		
		nodes.put(getTrueName(c), oc);
		
		// add nodes to the graph
		GUIUnifiedState.getInstance().getGraph().addVertex(oc);
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
			GUI.logger().debug("Remembering interface " + intR + " of node " + ce);
			addNodeToInterface(intR, newNode);
		}
		
		// disk image
		Resource di = NdlCommons.getDiskImage(ce);
		if (di != null) {
			try {
				String imageURL = NdlCommons.getIndividualsImageURL(ce);
				String imageHash = NdlCommons.getIndividualsImageHash(ce);
				GUIImageList.getInstance().addImage(new OrcaImage(di.getLocalName(), 
						new URL(imageURL), imageHash), null);
				String imgName = di.getURI().replaceAll("http.+#", "").replace("+", " ");
				newNode.setImage(imgName);
			} catch (Exception e) {
				// FIXME: ?
				;
			}
		}
		
		nodes.put(getTrueName(ce), newNode);
		
		// add nodes to the graph
		GUIUnifiedState.getInstance().getGraph().addVertex(newNode);
		
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
			on.setUrl(tmpR.getURI());
			nodes.put(getTrueName(tmpR), on);
			GUIUnifiedState.getInstance().getGraph().addVertex(on);
			OrcaLink ol = GUIUnifiedState.getInstance().getLinkCreator().create("Unnamed", ResourceType.MANIFEST);
			GUI.logger().debug("  Creating a link  from " + parent + " to " + on);

			// link to parent (a visual HACK)
			links.put(ol.getName(), ol);
			GUIUnifiedState.getInstance().getGraph().addEdge(ol, new Pair<OrcaNode>(parent, on), 
					EdgeType.UNDIRECTED);
			
			// add various properties
			setCommonNodeProperties(on, tmpR);
			
			// process interfaces. if there is an interface that leads to
			// a link, this is an intra-domain case, so we can delete the parent later
			for (Resource intR: NdlCommons.getResourceInterfaces(tmpR)) {
				//interfaceToNode.put(getTrueName(intR), on);
				addNodeToInterface(intR, on);
				// HACK: for now check that this interface connects to something
				// and is not just hanging there with IP address
				List<Resource> hasI = NdlCommons.getWhoHasInterface(intR, om);
				if (hasI.size() > 1)
					innerNodeConnected = true;
			}
		}
		
		// Hack - remove parent if nodes are linked between themselves
		if (innerNodeConnected)
			GUIUnifiedState.getInstance().getGraph().removeVertex(parent);
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
		GUIUnifiedState.getInstance().mapGuidToResource(on.getReservationGuid(), on);
		
		// guid
		setRequestGuid(nr, on);
		
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
		if ((ceType == null) && (groupUrl != null)) {
			// try to lookup ce type on the group
			Resource gropuRes = nr.getModel().getResource(groupUrl);
			ceType = NdlCommons.getSpecificCE(gropuRes);
		}
		if (ceType != null) {
			// in some cases it can have multiple types (e.g. baremetal and 40G)
			List<DomainResourceType> drtl = NdlCommons.getDomainResourceTypes(nr);
			StringBuilder sb = new StringBuilder();
			for(DomainResourceType drti: drtl) {
				sb.append(RequestSaver.reverseNodeTypeLookup(ceType, drti));
				sb.append(", ");
			}
			if (sb.length() > 2)
				on.setNodeType(sb.toString().substring(0, sb.length() - 2));
		}
		
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
				sb.append("   Path (len: " + p.size() + ") ");
				for (Resource r: p) {
					sb.append(r + " ");
				}
				
				GUI.logger().debug(sb.toString());
				
				Iterator<Resource> pIter = p.iterator();
				
				Resource first = pIter.next();
				if (NdlCommons.isStitchingNodeInManifest(first)) {
					// skip one more because stitch node paths have node-node-link-node-link-node structure
					// rather than node-link-node-link-node structure /ib 12/7/17
					first = pIter.next();
				}
				if (first == null)
					continue;
				
				while(pIter.hasNext()) {
					// only take nodes, skip links on the path
					pIter.next();
					if (!pIter.hasNext()) {
						break;
					}
					Resource second = pIter.next();
					OrcaNode firstNode = nodes.get(getTrueName(first));
					OrcaNode secondNode = nodes.get(getTrueName(second));
					//System.out.println("first " + getTrueName(first) + " and second " + getTrueName(second) + " " + firstNode + "/" + secondNode);
					if ((firstNode == null) || (secondNode == null)) {
						break;
					}

					OrcaLink ol = GUIUnifiedState.getInstance().getLinkCreator().create("Unnamed", ResourceType.MANIFEST);
					
					GUI.logger().debug("  Creating a link " + ol.getName() + " from " + first + " to " + second);
					GUIUnifiedState.getInstance().getGraph().addEdge(ol, new Pair<OrcaNode>(firstNode, secondNode), 
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
			String blob = NdlCommons.getColorBlobXML(color, true);
			if (blob != null) {
				oc.setBlob(NdlToRSpecHelper.stripXmlNs(NdlToRSpecHelper.stripXmlHead(blob)));
				oc.setXMLBlobState(true);
			}
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
			GUIUnifiedState.getInstance().getGraph().addEdge(cd.ocl, new Pair<OrcaNode>(fromOr, toOr), EdgeType.UNDIRECTED);
		}
	}
	
	/**
	 * As a temporary measure we allow extracting guid from reservation notice /ib 08/20/14
	 */
	private static Pattern noticeGuidPattern = Pattern.compile(NOTICE_GUID_PATTERN);
	private static String getGuidFromNotice(String notice) {
		if (notice == null)
			return null;
		java.util.regex.Matcher m = noticeGuidPattern.matcher(notice.trim());
		if (m.matches()) {
			return m.group(1);
		}
		return null;
	}
	
	public static void main(String[] argv) {
		String msg = "Reservation e9a9602d-096c-45dc-a5ad-3794d1b334da (Slice test-slice) is in state [Active,None]\n";
		System.out.println(getGuidFromNotice(msg));
		msg = "Reservation 9d194ec5-a849-49a5-88c6-789bca669e9a (Slice test-4) is in state [Failed,None]\n\n" +
				"Last ticket update:\n java.lang.RuntimeException: Insufficient <memoryCapacity,0> to meet request:6000";
		System.out.println(getGuidFromNotice(msg));
	}
}
