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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import orca.flukes.GUI;
import orca.flukes.GUIRequestState;
import orca.flukes.OrcaCrossconnect;
import orca.flukes.OrcaImage;
import orca.flukes.OrcaLink;
import orca.flukes.OrcaNode;
import orca.flukes.OrcaNodeGroup;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlGenerator;

import org.apache.commons.lang.StringUtils;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hyperrealm.kiwi.ui.dialog.ExceptionDialog;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.Pair;

public class RequestSaver {
	private static final String EUCALYPTUS_NS = "eucalyptus";
	public static final String DOT_FORMAT = "DOT";
	public static final String N3_FORMAT = "N3";
	public static final String RDF_XML_FORMAT = "RDF-XML";

	private static RequestSaver instance = null;
	public static final String defaultFormat = RDF_XML_FORMAT;
	private NdlGenerator ngen = null;
	private Individual reservation = null;
	private String outputFormat = null;
	
	// converting to netmask
	private static final String[] netmaskConverter = {
		"128.0.0.0", "192.0.0.0", "224.0.0.0", "240.0.0.0", "248.0.0.0", "252.0.0.0", "254.0.0.0", "255.0.0.0",
		"255.128.0.0", "255.192.0.0", "255.224.0.0", "255.240.0.0", "255.248.0.0", "255.252.0.0", "255.254.0.0", "255.255.0.0",
		"255.255.128.0", "255.255.192.0", "255.255.224.0", "255.255.240.0", "255.255.248.0", "255.255.252.0", "255.255.254.0", "255.255.255.0",
		"255.255.255.128", "255.255.255.192", "255.255.255.224", "255.255.255.240", "255.255.255.248", "255.255.255.252", "255.255.255.254", "255.255.255.255"
	};
		
	// helper
	public static final Map<String, String> domainMap;
	static {
		Map<String, String> dm = new HashMap<String, String>();
		dm.put("RENCI XO Rack", "rcivmsite.rdf#rcivmsite");
		dm.put("BBN/GPO XO Rack", "bbnvmsite.rdf#bbnvmsite");
		dm.put("RENCI ACIS","acisrencivmsite.rdf#acisrencivmsite");
		dm.put("Duke CS", "dukevmsite.rdf#dukevmsite");
		dm.put("UNC BEN", "uncvmsite.rdf#uncvmsite");
		dm.put("UH minirack", "uhoustonvmsite.rdf#uhoustonvmsite");
		dm.put("RENCI BEN (not a GENI resource)", "rencivmsite.rdf#rencivmsite");
		dm.put("NERSC (not a GENI resource)", "nerscvmsite.rdf#nerscvmsite");
		dm.put("UMass Amherst (vlan 533)", "mass.rdf#mass");
		dm.put("RENCI OSG (not a GENI resource)", "osgvmsite.rdf#osgvmsite");

		domainMap = Collections.unmodifiableMap(dm);
	}
	
	// various node types
	public static final Map<String, Pair<String>> nodeTypes;
	static {
		Map<String, Pair<String>> nt = new HashMap<String, Pair<String>>();
		nt.put("Euca m1.small", new Pair<String>(EUCALYPTUS_NS, "EucaM1Small"));
		nt.put("Euca c1.medium", new Pair<String>(EUCALYPTUS_NS, "EucaC1Medium"));
		nt.put("Euca m1.large", new Pair<String>(EUCALYPTUS_NS, "EucaM1Large"));
		nt.put("Euca m1.xlarge", new Pair<String>(EUCALYPTUS_NS, "EucaM1XLarge"));
		nt.put("Euca c1.xlarge", new Pair<String>(EUCALYPTUS_NS, "EucaC1XLarge"));
		nodeTypes = Collections.unmodifiableMap(nt);
	}
	
	private RequestSaver() {
		
	}
	
	public static RequestSaver getInstance() {
		if (instance == null)
			instance = new RequestSaver();
		return instance;
	}
	
	private String getFormattedOutput(NdlGenerator ng, String oFormat) {
		if (oFormat == null)
			return getFormattedOutput(ng, defaultFormat);
		if (oFormat.equals(RDF_XML_FORMAT)) 
			return ng.toXMLString();
		else if (oFormat.equals(N3_FORMAT))
			return ng.toN3String();
		else if (oFormat.equals(DOT_FORMAT)) {
			return ng.getGVOutput();
		}
		else
			return getFormattedOutput(ng, defaultFormat);
	}
	
	public void setOutputFormat(String of) {
		outputFormat = of;
	}
	
	/**
	 * Convert netmask string to an integer (24-bit returned if no match)
	 * @param nm
	 * @return
	 */
	public static int netmaskStringToInt(String nm) {
		int i = 1;
		for(String s: netmaskConverter) {
			if (s.equals(nm))
				return i;
			i++;
		}
		return 24;
	}
	
	/**
	 * Convert netmask int to string (255.255.255.0 returned if nm > 32 or nm < 1)
	 * @param nm
	 * @return
	 */
	public static String netmaskIntToString(int nm) {
		if ((nm > 32) || (nm < 1)) 
			return "255.255.255.0";
		else
			return netmaskConverter[nm - 1];
	}
	
	/**
	 * Link node to edge, create interface and process IP address 
	 * @param n
	 * @param e
	 * @param edgeI
	 * @throws NdlException
	 */
	private void processNodeAndLink(OrcaNode n, OrcaLink e, Individual edgeI) throws NdlException {
		Individual intI = ngen.declareInterface(e.getName()+"-"+n.getName());
		ngen.addInterfaceToIndividual(intI, edgeI);
		
		Individual nodeI = ngen.getRequestIndividual(n.getName());
		ngen.addInterfaceToIndividual(intI, nodeI);
		
		// see if there is an IP address for this link on this node
		if (n.getIp(e) != null) {
			// create IP object, attach to interface
			Individual ipInd = ngen.addUniqueIPToIndividual(n.getIp(e), e.getName()+"-"+n.getName(), intI);
			if (n.getNm(e) != null)
				ngen.addNetmaskToIP(ipInd, netmaskIntToString(Integer.parseInt(n.getNm(e))));
		}
	}
	
	/**
	 * Special handling for node group internal vlan
	 * @param ong
	 * @throws NdlException
	 */
	private void processNodeGroupInternalVlan(Individual reservation, OrcaNodeGroup ong) throws NdlException {
		Individual netI = ngen.declareNetworkConnection("private-vlan-" + ong.getName());
		ngen.addLayerToConnection(netI, "ethernet", "EthernetNetworkElement");
		ngen.addResourceToReservation(reservation, netI);
		
		Individual intI = ngen.declareInterface("private-vlan-intf-" + ong.getName());
		ngen.addInterfaceToIndividual(intI, netI);
		
		Individual nodeI = ngen.getRequestIndividual(ong.getName());
		ngen.addInterfaceToIndividual(intI, nodeI);
		
		if (ong.getInternalVlanBw() > 0) 
			ngen.addBandwidthToConnection(netI, ong.getInternalVlanBw());
		
		if (ong.getInternalVlanLabel() != null)
			ngen.addLabelToIndividual(netI, ong.getInternalVlanLabel());
		
		if (ong.getInternalIp() != null) {
			Individual ipInd = ngen.addUniqueIPToIndividual(ong.getInternalIp(), "private-vlan-intf-" + ong.getName(), intI);
			if (ong.getInternalNm() != null) 
				ngen.addNetmaskToIP(ipInd, netmaskIntToString(Integer.parseInt(ong.getInternalNm())));
		}
	}
	
	private void checkLinkSanity(OrcaLink l) throws NdlException {
		// sanity checks
		// 1) if label is specified, nodes cannot be in different domains

		Pair<OrcaNode> pn = GUIRequestState.getInstance().getGraph().getEndpoints(l);
		
		if ((l.getLabel() != null) && 
				(((pn.getFirst().getDomain() != null) && 
				(!pn.getFirst().getDomain().equals(pn.getSecond().getDomain()))) ||
				(pn.getSecond().getDomain() != null) && 
				(!pn.getSecond().getDomain().equals(pn.getFirst().getDomain()))))
			throw new NdlException("Link " + l.getName() + " is invalid: it specifies a desired VLAN tag, but the nodes are bound to different domains");
	}
	
	/** 
	 * Links connecting nodes to crossconnects aren't real
	 * @param e
	 * @return
	 */
	private boolean fakeLink(OrcaLink e) {
		Pair<OrcaNode> pn = GUIRequestState.getInstance().getGraph().getEndpoints(e);
		if ((pn.getFirst() instanceof OrcaCrossconnect) ||
				(pn.getSecond() instanceof OrcaCrossconnect))
			return true;
		return false;
	}
	
	/**
	 * Check the sanity of a crossconnect
	 * @param n
	 * @throws NdlException
	 */
	private void checkCrossconnectSanity(OrcaCrossconnect n) throws NdlException {
		// sanity checks
		// 1) nodes can't be from different domains
		
		Collection<OrcaLink> iLinks = GUIRequestState.getInstance().getGraph().getIncidentEdges(n);
		String domain = null;
		boolean flag = false;
		for(OrcaLink l: iLinks) {
			// count how many nodes or node groups we're connected to
			Pair<OrcaNode> pn = GUIRequestState.getInstance().getGraph().getEndpoints(l);
			if (!(pn.getFirst() instanceof OrcaCrossconnect)) {
				if (pn.getFirst().getDomain() != null) {
					if (!flag) {
						flag = true;
						domain = pn.getFirst().getDomain();
					} else if (!pn.getFirst().getDomain().equals(domain))
						throw new NdlException("You cannot split broadcast link " + n.getName() + " between domains.");
				} 
			} else 
				if (!(pn.getSecond() instanceof OrcaCrossconnect)) {
					if (pn.getSecond().getDomain() != null) {
						if (!flag) {
							flag = true;
							domain = pn.getSecond().getDomain();
						} else if (!pn.getSecond().getDomain().equals(domain))
							throw new NdlException("You cannot split broadcast link " + n.getName() + " between domains.");
					}
				}
		}
	}
	
	private void processCrossconnect(OrcaCrossconnect oc, Individual blI) throws NdlException {
		
		Collection<OrcaLink> iLinks = GUIRequestState.getInstance().getGraph().getIncidentEdges(oc);
		for(OrcaLink l: iLinks) {
			Pair<OrcaNode> pn = GUIRequestState.getInstance().getGraph().getEndpoints(l);
			OrcaNode n = null;
			// find the non-crossconnect side
			if (!(pn.getFirst() instanceof OrcaCrossconnect))
				n = pn.getFirst();
			else if (!(pn.getSecond() instanceof OrcaCrossconnect))
				n = pn.getSecond();
			
			if (n == null) 
				throw new NdlException("Two VLANs linked together is not a valid combination");
			// find the individual matching this node
			
			Individual intI = ngen.declareInterface(oc.getName()+"-"+n.getName());
			ngen.addInterfaceToIndividual(intI, blI);
			
			Individual nodeI = ngen.getRequestIndividual(n.getName());
			ngen.addInterfaceToIndividual(intI, nodeI);

			// see if there is an IP address for this link on this node
			if (n.getIp(l) != null) {
				// create IP object, attach to interface
				Individual ipInd = ngen.addUniqueIPToIndividual(n.getIp(l), oc.getName()+"-"+n.getName(), intI);
				if (n.getNm(l) != null)
					ngen.addNetmaskToIP(ipInd, netmaskIntToString(Integer.parseInt(n.getNm(l))));
			}
		}
	}
	
	/**
	 * Save graph using NDL
	 * @param f
	 * @param requestGraph
	 */
	public String convertGraphToNdl(SparseMultigraph<OrcaNode, OrcaLink> g) {
		String res = null;
		
		assert(g != null);
		// this should never run in parallel anyway
		synchronized(instance) {
			try {
				ngen = new NdlGenerator(GUI.logger());
			
				reservation = ngen.declareReservation();
				Individual term = ngen.declareTerm();
				
				// not an immediate reservation? declare term beginning
				if (GUIRequestState.getInstance().getTerm().getStart() != null) {
					Individual tStart = ngen.declareTermBeginning(GUIRequestState.getInstance().getTerm().getStart());
					ngen.addBeginningToTerm(tStart, term);
				}
				// now duration
				GUIRequestState.getInstance().getTerm().normalizeDuration();
				Individual duration = ngen.declareTermDuration(GUIRequestState.getInstance().getTerm().getDurationDays(), 
						GUIRequestState.getInstance().getTerm().getDurationHours(), GUIRequestState.getInstance().getTerm().getDurationMins());
				ngen.addDurationToTerm(duration, term);
				ngen.addTermToReservation(term, reservation);
				
				// openflow
				ngen.addOpenFlowCapable(reservation, GUIRequestState.getInstance().getOfNeededVersion());
				
				// add openflow details
				if (GUIRequestState.getInstance().getOfNeededVersion() != null) {
					Individual ofSliceI = ngen.declareOfSlice("of-slice");
					ngen.addSliceToReservation(reservation, ofSliceI);
					ngen.addOfPropertiesToSlice(GUIRequestState.getInstance().getOfUserEmail(), 
							GUIRequestState.getInstance().getOfSlicePass(), 
							GUIRequestState.getInstance().getOfCtrlUrl(), 
							ofSliceI);
				}
				
				// decide whether we have a global image
				boolean globalImage = false, globalDomain = false;
				
				// is image specified in the reservation?
				if (GUIRequestState.getInstance().getVMImageInReservation() != null) {
					// there is a global image (maybe)
					OrcaImage im = GUIRequestState.getInstance().getImageByName(GUIRequestState.getInstance().getVMImageInReservation());
					if (im != null) {
						// definitely an global image - attach it to the reservation
						globalImage = true;
						// TODO: check for zero length
						Individual imI = ngen.declareDiskImage(im.getUrl().toString(), im.getHash(), im.getShortName());
						ngen.addDiskImageToIndividual(imI, reservation);
					}
				}
				
				// is domain specified in the reservation?
				if (GUIRequestState.getInstance().getDomainInReservation() != null) {
					globalDomain = true;
					Individual domI = ngen.declareDomain(domainMap.get(GUIRequestState.getInstance().getDomainInReservation()));
					ngen.addDomainToIndividual(domI, reservation);
				}
				
				// shove invidividual nodes onto the reservation/crossconnects are vertices, but not 'nodes'
				// so require special handling
				for (OrcaNode n: GUIRequestState.getInstance().getGraph().getVertices()) {
					Individual ni;
					if (n instanceof OrcaCrossconnect) {
						continue;
					} else {
						// nodes and nodegroups
						if (n instanceof OrcaNodeGroup) {
							OrcaNodeGroup ong = (OrcaNodeGroup) n;
							if (ong.getSplittable())
								ni = ngen.declareServerCloud(ong.getName(), ong.getSplittable());
							else
								ni = ngen.declareServerCloud(ong.getName());
							if (ong.getInternalVlan())
								processNodeGroupInternalVlan(reservation, ong);
						}
						else {
							ni = ngen.declareComputeElement(n.getName());
							ngen.addVMDomainProperty(ni);
						}

						ngen.addResourceToReservation(reservation, ni);

						// for clusters, add number of nodes, declare as cluster (VM domain)
						if (n instanceof OrcaNodeGroup) {
							OrcaNodeGroup ong = (OrcaNodeGroup)n;
							ngen.addNumCEsToCluster(ong.getNodeCount(), ni);
							ngen.addVMDomainProperty(ni);
						}

						// if no global image is set and a local image is set, add it to node
						if (!globalImage && (n.getImage() != null)) {
							// check if image is set in this node
							OrcaImage im = GUIRequestState.getInstance().getImageByName(n.getImage());
							if (im != null) {
								Individual imI = ngen.declareDiskImage(im.getUrl().toString(), im.getHash(), im.getShortName());
								ngen.addDiskImageToIndividual(imI, ni);
							}
						}

						// if no global domain domain is set, declare a domain and add inDomain property
						if (!globalDomain && (n.getDomain() != null)) {
							Individual domI = ngen.declareDomain(domainMap.get(n.getDomain()));
							ngen.addNodeToDomain(domI, ni);
						}

						// node type
						if ((n.getNodeType() != null) && (nodeTypes.get(n.getNodeType()) != null)) {
							Pair<String> nt = nodeTypes.get(n.getNodeType());
							ngen.addNodeTypeToCE(nt.getFirst(), nt.getSecond(), ni);
						}

						// open ports
						if (n.getPortsList() != null) {
							// Say it's a TCPProxy with proxied port
							String[] ports = n.getPortsList().split(",");
							int pi = 0;
							for (String port: ports) {
								Individual prx = ngen.declareTCPProxy("prx-" + n.getName().replaceAll("[ \t#:/]", "-") + "-" + pi++);
								ngen.addProxyToIndividual(prx, ni);
								ngen.addPortToProxy(port.trim(), prx);
							}
						}

						// post boot script
						if ((n.getPostBootScript() != null) && (n.getPostBootScript().length() > 0)) {
							ngen.addPostBootScriptToCE(n.getPostBootScript(), ni);
						}
					}
				}
				
				// node dependencies (done afterwards to be sure all nodes are declared)
				for (OrcaNode n: GUIRequestState.getInstance().getGraph().getVertices()) {
					Individual ni = ngen.getRequestIndividual(n.getName());
					for(OrcaNode dep: n.getDependencies()) {
						Individual depI = ngen.getRequestIndividual(dep.getName());
						if (depI != null) {
							ngen.addDependOnToIndividual(depI, ni);
						}
					}
				}
				
				// crossconnects are vertices in the graph, but are actually a kind of link
				for (OrcaNode n: GUIRequestState.getInstance().getGraph().getVertices()) {
					Individual bl;
					if (n instanceof OrcaCrossconnect) {
						// sanity check
						OrcaCrossconnect oc = (OrcaCrossconnect)n;
						checkCrossconnectSanity(oc);
						bl = ngen.declareBroadcastConnection(oc.getName());
						ngen.addResourceToReservation(reservation, bl);
						
						if (oc.getBandwidth() > 0)
							ngen.addBandwidthToConnection(bl, oc.getBandwidth());
						
						if (oc.getLabel() != null) 
							ngen.addLabelToIndividual(bl, oc.getLabel());
						
						ngen.addLayerToConnection(bl, "ethernet", "EthernetNetworkElement");
						
						// add incident nodes' interfaces
						processCrossconnect(oc, bl);
					}
				}
				

				if (GUIRequestState.getInstance().getGraph().getEdgeCount() == 0) {
					// a bunch of disconnected nodes, no IP addresses 
					
				} else {
					// edges, nodes, IP addresses oh my!
					for (OrcaLink e: GUIRequestState.getInstance().getGraph().getEdges()) {
						// skip links to crossconnects
						if (fakeLink(e))
							continue;
						
						checkLinkSanity(e);
						
						Individual ei = ngen.declareNetworkConnection(e.getName());
						ngen.addResourceToReservation(reservation, ei);

						if (e.getBandwidth() > 0)
							ngen.addBandwidthToConnection(ei, e.getBandwidth());
						
						if (e.getLabel() != null) 
							ngen.addLabelToIndividual(ei, e.getLabel());
						
						// TODO: deal with layers later
						ngen.addLayerToConnection(ei, "ethernet", "EthernetNetworkElement");

						// TODO: latency
						
						Pair<OrcaNode> pn = GUIRequestState.getInstance().getGraph().getEndpoints(e);
						processNodeAndLink(pn.getFirst(), e, ei);
						processNodeAndLink(pn.getSecond(), e, ei);
					}
				}
				
				// save the contents
				res = getFormattedOutput(ngen, outputFormat);

			} catch (Exception e) {
				ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
				ed.setLocationRelativeTo(GUI.getInstance().getFrame());
				ed.setException("Exception encountered while converting graph to NDL-OWL: ", e);
				ed.setVisible(true);
				return null;
			}
		}
		return res;
	}
	
	public boolean saveGraph(File f, SparseMultigraph<OrcaNode, OrcaLink> g) {
		assert(f != null);

		String ndl = convertGraphToNdl(g);
		if (ndl == null)
			return false;
		
		try{
			FileOutputStream fsw = new FileOutputStream(f);
			OutputStreamWriter out = new OutputStreamWriter(fsw, "UTF-8");
			out.write(ndl);
			out.close();
			return true;
		} catch(FileNotFoundException e) {
			;
		} catch(UnsupportedEncodingException ex) {
			;
		} catch(IOException ey) {
			;
		} 
		return false;
	}

	public SparseMultigraph<OrcaNode, OrcaLink> loadGraph(File f) {
		return null;
	}
	
	/**
	 * Do a reverse lookip on domain (NDL -> short name)
	 * @param dom
	 * @return
	 */
	public static String reverseLookupDomain(Resource dom) {
		if (dom == null)
			return null;
		// strip off name space and "/Domain"
		String domainName = StringUtils.removeStart(dom.getURI(), NdlCommons.ORCA_NS);
		if (domainName == null)
			return null;
		domainName = StringUtils.removeEnd(domainName, "/Domain");
		for (Iterator<Map.Entry<String, String>> domName = domainMap.entrySet().iterator(); domName.hasNext();) {
			Map.Entry<String, String> e = domName.next();
			if (domainName.equals(e.getValue()))
				return e.getKey();
		}
		return null;
	}
	
	/**
	 * Do a reverse lookup on node type (NDL -> shortname )
	 */
	public static String reverseNodeTypeLookup(Resource nt) {
		if (nt == null)
			return null;
		for (Iterator<Map.Entry<String, Pair<String>>> it = nodeTypes.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, Pair<String>> e = it.next();
			// convert to namespace and type in a pair
			// WARNING: this checks only the type, not the namespace.
			if (nt.getLocalName().equals(e.getValue().getSecond()))
				return e.getKey();
		}
		return null;
	}
	
	/**
	 * Post boot scripts need to be sanitized (deprecated)
	 * @param s
	 * @return
	 */
	public static String sanitizePostBootScript(String s) {
		// no longer needed
		return s;
	}
}
