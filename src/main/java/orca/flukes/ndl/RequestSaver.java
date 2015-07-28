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
import java.util.List;
import java.util.Map;

import orca.flukes.GUI;
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
	private static final String EXOGENI_NS = "exogeni";
	public static final String BAREMETAL = "ExoGENI Bare-metal";
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
		dm.put("RENCI (Chapel Hill, NC USA) XO Rack", "rcivmsite.rdf#rcivmsite");
		dm.put("BBN/GPO (Boston, MA USA) XO Rack", "bbnvmsite.rdf#bbnvmsite");
		dm.put("Duke CS (Durham, NC USA) XO Rack", "dukevmsite.rdf#dukevmsite");
		dm.put("UNC BEN (Chapel Hill, NC USA)", "uncvmsite.rdf#uncvmsite");
		dm.put("RENCI BEN (Chapel Hill, NC USA)", "rencivmsite.rdf#rencivmsite");
		dm.put("NICTA (Sydney, Australia) XO Rack", "nictavmsite.rdf#nictavmsite");
		dm.put("FIU (Miami, FL USA) XO Rack", "fiuvmsite.rdf#fiuvmsite");
		dm.put("UH (Houston, TX USA) XO Rack", "uhvmsite.rdf#uhvmsite");
		dm.put("UvA (Amsterdam, The Netherlands) XO Rack", "uvanlvmsite.rdf#uvanlvmsite");
		dm.put("UFL (Gainesville, FL USA) XO Rack", "uflvmsite.rdf#uflvmsite");
		dm.put("UCD (Davis, CA USA) XO Rack", "ucdvmsite.rdf#ucdvmsite");
		dm.put("OSF (Oakland, CA USA) XO Rack", "osfvmsite.rdf#osfvmsite");
		dm.put("SL (Chicago, IL USA) XO Rack", "slvmsite.rdf#slvmsite");
		dm.put("WVN (UCS-B series rack in Morgantown, WV, USA)", "wvnvmsite.rdf#wvnvmsite");
		dm.put("NCSU (UCS-B series rack at NCSU)", "ncsuvmsite.rdf#ncsuvmsite");
		dm.put("NCSU2 (UCS-C series rack at NCSU)", "ncsu2vmsite.rdf#ncsu2vmsite");
		dm.put("TAMU (College Station, TX, USA) XO Rack", "tamuvmsite.rdf#tamuvmsite");
		dm.put("UMass (UMass Amherst, MA, USA) XO Rack", "umassvmsite.rdf#umassvmsite");
		dm.put("WSU (Detroit, MI, USA) XO Rack", "wsuvmsite.rdf#wsuvmsite");
		dm.put("UAF (Fairbanks, AK, USA) XO Rack", "uafvmsite.rdf#uafvmsite");
		dm.put("PSC (Pittsburgh, PA, USA) XO Rack", "pscvmsite.rdf#pscvmsite");
		dm.put("GWU (Washington DC,  USA) XO Rack", "gwuvmsite.rdf#gwuvmsite");
		dm.put("CIENA (Ottawa,  CA) XO Rack", "cienavmsite.rdf#cienavmsite");
		dm.put(OrcaStitchPort.STITCHING_DOMAIN_SHORT_NAME, "orca.rdf#Stitching");

		domainMap = Collections.unmodifiableMap(dm);
	}
	
	public static final Map<String, String> netDomainMap;
	static {
		Map<String, String> ndm = new HashMap<String, String>();

		ndm.put("RENCI XO Rack Net", "rciNet.rdf#rciNet");
		ndm.put("BBN/GPO XO Rack Net", "bbnNet.rdf#bbnNet");
		ndm.put("Duke CS Rack Net", "dukeNet.rdf#dukeNet");
		ndm.put("UNC BEN XO Rack Net", "uncNet.rdf#UNCNet");
		ndm.put("NICTA XO Rack Net", "nictaNet.rdf#nictaNet");
		ndm.put("FIU XO Rack Net", "fiuNet.rdf#fiuNet");
		ndm.put("UH XO Rack Net", "uhNet.rdf#UHNet");
		ndm.put("NCSU XO Rack Net", "ncsuNet.rdf#ncsuNet");
		ndm.put("UvA XO Rack Net", "uvanlNet.rdf#uvanlNet");
		ndm.put("UFL XO Rack Net", "uflNet.rdf#uflNet");
		ndm.put("UCD XO Rack Net", "ucdNet.rdf#ucdNet");
		ndm.put("OSF XO Rack Net", "osfNet.rdf#osfNet");
		ndm.put("SL XO Rack Net", "slNet.rdf#slNet");
		ndm.put("WVN XO Rack Net", "wvnNet.rdf#wvnNet");
		ndm.put("NCSU XO Rack Net", "ncsuNet.rdf#ncsuNet");
		ndm.put("NCSU2 XO Rack Net", "ncs2Net.rdf#ncsuNet");
		ndm.put("TAMU XO Rack Net",  "tamuNet.rdf#tamuNet");
		ndm.put("UMass XO Rack Net",  "umassNet.rdf#umassNet");
		ndm.put("WSU XO Rack Net",  "wsuNet.rdf#wsuNet");
		ndm.put("UAF XO Rack Net",  "uafNet.rdf#uafNet");
		ndm.put("PSC XO Rack Net",  "pscNet.rdf#pscNet");
		ndm.put("GWU XO Rack Net",  "gwuNet.rdf#gwuNet");
		ndm.put("CIENA XO Rack Net",  "cienaNet.rdf#cienaNet");
		
		
		ndm.put("I2 ION/AL2S", "ion.rdf#ion");
		ndm.put("NLR Net", "nlr.rdf#nlr");
		ndm.put("BEN Net", "ben.rdf#ben");
	
		netDomainMap = Collections.unmodifiableMap(ndm);
	}
	
	// various node types
	public static final Map<String, Pair<String>> nodeTypes;
	static {
		Map<String, Pair<String>> nt = new HashMap<String, Pair<String>>();
		nt.put(BAREMETAL, new Pair<String>(EXOGENI_NS, "ExoGENI-M4"));
		//nt.put("Euca m1.small", new Pair<String>(EUCALYPTUS_NS, "EucaM1Small"));
		//nt.put("Euca c1.medium", new Pair<String>(EUCALYPTUS_NS, "EucaC1Medium"));
		//nt.put("Euca m1.large", new Pair<String>(EUCALYPTUS_NS, "EucaM1Large"));
		//nt.put("Euca m1.xlarge", new Pair<String>(EUCALYPTUS_NS, "EucaM1XLarge"));
		//nt.put("Euca c1.xlarge", new Pair<String>(EUCALYPTUS_NS, "EucaC1XLarge"));
		nt.put("XO Small", new Pair<String>(EXOGENI_NS, "XOSmall"));
		nt.put("XO Medium", new Pair<String>(EXOGENI_NS, "XOMedium"));
		nt.put("XO Large", new Pair<String>(EXOGENI_NS, "XOLarge"));
		nt.put("XO Extra large", new Pair<String>(EXOGENI_NS, "XOXlarge"));
		//nodeTypes = Collections.unmodifiableMap(nt);
		nodeTypes = nt;
	}
	
	private RequestSaver() {
		
	}
	
	public static void addCustomType(String type) {
		nodeTypes.put(type, new Pair<String>(EUCALYPTUS_NS, type));
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
	
	private void addLinkStorageDependency(OrcaNode n, OrcaLink e) throws NdlException {
		
		// if the other end is storage, need to add dependency
		if (e.linkToSharedStorage() && !(n instanceof OrcaStorageNode)) {
			Pair<OrcaNode> pn = GUIRequestState.getInstance().getGraph().getEndpoints(e);
			OrcaStorageNode osn = null;
			try {
				if (pn.getFirst() instanceof OrcaStorageNode)
					osn = (OrcaStorageNode) pn.getFirst();
				else
					osn = (OrcaStorageNode) pn.getSecond();
			} catch (Exception ce) {
				;
			}
			if (osn == null)
				throw new NdlException("Link " + e.getName() + " marked as storage, but neither endpoint is storage");
			Individual storInd = ngen.getRequestIndividual(osn.getName());
			Individual nodeInd = ngen.getRequestIndividual(n.getName());
			if ((storInd == null) || (nodeInd == null))
				throw new NdlException("Unable to find individual for node " + osn + " or " + n);
			ngen.addDependOnToIndividual(storInd, nodeInd);
		}
	}
	
	/**
	 * Link node to edge, create interface and process IP address 
	 * @param n
	 * @param e
	 * @param edgeI
	 * @throws NdlException
	 */
	private void processNodeAndLink(OrcaNode n, OrcaLink e, Individual edgeI) throws NdlException {

		Individual intI;
		
		addLinkStorageDependency(n, e);
		
		if (n instanceof OrcaStitchPort) {
			OrcaStitchPort sp = (OrcaStitchPort)n;
			if ((sp.getPort() == null) || (sp.getPort().length() == 0) || 
					(sp.getLabel() == null) || (sp.getLabel().length() == 0))
				throw new NdlException("URL and label must be specified in StitchPort");
			//intI = ngen.declareExistingInterface(sp.getPort());
			intI = ngen.declareExistingInterface(generateStitchPortInterfaceUrl(sp));
			ngen.addLabelToIndividual(intI, sp.getLabel());
			// declare adaptation
			Individual intM = ngen.declareExistingInterface(sp.getPort());
			ngen.addEthernetAdaptation(intM, intI);
		} else 
			intI = ngen.declareInterface(e.getName()+"-"+n.getName());
		// add to link
		ngen.addInterfaceToIndividual(intI, edgeI);

		
		// add to previously added node
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
		
		if ((pn.getFirst() instanceof OrcaStorageNode) &&
				(pn.getSecond() instanceof OrcaStorageNode)) 
			throw new NdlException("Link " + l.getName() + " in invalid: it connects two storage nodes together");
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
	
	private boolean colorLink(OrcaLink e) {
		if (e instanceof OrcaColorLink) 
			return true;
		return false;
	}
	
	/**
	 * Unique name generator for stitchport interfaces: interface URL + "/" + label
	 * @param osp
	 * @return
	 * @throws NdlException
	 */
	private String generateStitchPortInterfaceUrl(OrcaStitchPort osp) throws NdlException {
		if ((osp.getPort() == null) || (osp.getLabel() == null))
			throw new NdlException("Stitchport " + osp + " does not specify URL or label");
		
		if (osp.getPort().endsWith("/"))
			return osp.getPort() + osp.getLabel();
		else
			return osp.getPort() + "/" + osp.getLabel();
	}
	
	/**
	 * Check the sanity of a crossconnect
	 * @param n
	 * @throws NdlException
	 */
	private void checkCrossconnectSanity(OrcaCrossconnect n) throws NdlException {
		// sanity checks
		// 1) nodes can't be from different domains (obsoleted 08/28/13 /ib)
	}
	
	private void addCrossConnectStorageDependency(OrcaCrossconnect oc) throws NdlException {
		Collection<OrcaLink> iLinks = GUIRequestState.getInstance().getGraph().getIncidentEdges(oc);
		boolean sharedStorage = oc.linkToSharedStorage();
		
		if (!sharedStorage)
			return;
		
		List<OrcaStorageNode> snodes = new ArrayList<OrcaStorageNode>();
		List<OrcaNode> otherNodes = new ArrayList<OrcaNode>();
		
		for(OrcaLink l: iLinks) {
			Pair<OrcaNode> pn = GUIRequestState.getInstance().getGraph().getEndpoints(l);
			OrcaNode n = null;
			// find the non-crossconnect side
			if (!(pn.getFirst() instanceof OrcaCrossconnect))
				n = pn.getFirst();
			else if (!(pn.getSecond() instanceof OrcaCrossconnect))
				n = pn.getSecond();
			
			if (n instanceof OrcaStorageNode) 
				snodes.add((OrcaStorageNode)n);
			else
				otherNodes.add(n);
		}
		
		for(OrcaNode n: otherNodes) {
			for (OrcaStorageNode s: snodes) {
				Individual storInd = ngen.getRequestIndividual(s.getName());
				Individual nodeInd = ngen.getRequestIndividual(n.getName());
				if ((storInd == null) || (nodeInd == null))
					throw new NdlException("Unable to find individual for node " + s + " or " + n);
				ngen.addDependOnToIndividual(storInd, nodeInd);
			}
		}
	}
	
	private void processCrossconnect(OrcaCrossconnect oc, Individual blI) throws NdlException {
		
		addCrossConnectStorageDependency(oc);
		
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
			
			Individual intI;
			if (n instanceof OrcaStitchPort) {
				OrcaStitchPort sp = (OrcaStitchPort)n;
				if ((sp.getLabel() == null) || (sp.getLabel().length() == 0))
					throw new NdlException("URL and label must be specified in StitchPort");
				// declare adaptations to this port
				intI = ngen.declareExistingInterface(generateStitchPortInterfaceUrl(sp));
				ngen.addLabelToIndividual(intI, sp.getLabel());
				// add adaptation
				Individual intM = ngen.declareExistingInterface(sp.getPort());
				ngen.addEthernetAdaptation(intM, intI);
			} else
				intI = ngen.declareInterface(oc.getName()+"-"+n.getName());
			
			ngen.addInterfaceToIndividual(intI, blI);
			
			// find the individual matching this node
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
	
	private void setNodeTypeOnInstance(String type, Individual ni) throws NdlException {
		if (BAREMETAL.equals(type))
			ngen.addBareMetalDomainProperty(ni);
		else
			ngen.addVMDomainProperty(ni);
		if (nodeTypes.get(type) != null) {
			Pair<String> nt = nodeTypes.get(type);
			ngen.addNodeTypeToCE(nt.getFirst(), nt.getSecond(), ni);
		}
	}
	
	/**
	 * Save graph using NDL
	 * @param f
	 * @param requestGraph
	 */
	public String convertGraphToNdl(SparseMultigraph<OrcaNode, OrcaLink> g, String nsGuid) {
		String res = null;
		
		assert(g != null);
		// this should never run in parallel anyway
		synchronized(instance) {
			try {
				ngen = new NdlGenerator(nsGuid, GUI.logger());
			
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
				
				// decide whether we have a global domain
				boolean globalDomain = false;
				
				// is domain specified in the reservation?
				if (GUIRequestState.getInstance().getDomainInReservation() != null) {
					if (!GUIRequestState.getInstance().isAKnownDomain(GUIRequestState.getInstance().getDomainInReservation()))
						throw new NdlException("Domain " + GUIRequestState.getInstance().getDomainInReservation() + " is not visible from this SM!");
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
					} else if (n instanceof OrcaStitchPort) {
						OrcaStitchPort sp = (OrcaStitchPort)n;
						ni = ngen.declareStitchingNode(sp.getName());
						ngen.addResourceToReservation(reservation, ni);
					} else if (n instanceof OrcaStorageNode) {
						OrcaStorageNode snode = (OrcaStorageNode)n;
						ni = ngen.declareISCSIStorageNode(snode.getName(), 
								snode.getCapacity(),
								snode.getFSType(), snode.getFSParam(), snode.getMntPoint(), 
								snode.getDoFormat());
						if (!globalDomain && (n.getDomain() != null)) {
							if (!GUIRequestState.getInstance().isAKnownDomain(n.getDomain()))
								throw new NdlException("Domain " + n.getDomain() + " of node " + n + " is not visible from this SM!");
							Individual domI = ngen.declareDomain(domainMap.get(n.getDomain()));
							ngen.addNodeToDomain(domI, ni);
						}
						ngen.addResourceToReservation(reservation, ni);
					} else {
						// nodes and nodegroups
						if (n instanceof OrcaNodeGroup) {
							OrcaNodeGroup ong = (OrcaNodeGroup) n;
							if (ong.getSplittable())
								ni = ngen.declareServerCloud(ong.getName(), ong.getSplittable());
							else
								ni = ngen.declareServerCloud(ong.getName());
						}
						else {
							ni = ngen.declareComputeElement(n.getName());
							//ngen.addVMDomainProperty(ni);
						}

						ngen.addResourceToReservation(reservation, ni);
						
						// for clusters, add number of nodes, declare as cluster (VM domain)
						if (n instanceof OrcaNodeGroup) {
							OrcaNodeGroup ong = (OrcaNodeGroup)n;
							ngen.addNumCEsToCluster(ong.getNodeCount(), ni);
							//ngen.addVMDomainProperty(ni);
						}

						// node type 
						setNodeTypeOnInstance(n.getNodeType(), ni);
						
						// check if node has its own image
						if (n.getImage() != null) {
							// check if image is set in this node
							OrcaImage im = GUIRequestState.getInstance().getImageByName(n.getImage());
							if (im != null) {
								Individual imI = ngen.declareDiskImage(im.getUrl().toString(), im.getHash(), im.getShortName());
								ngen.addDiskImageToIndividual(imI, ni);
							}
						} else {
							// only bare-metal can specify no image
							if (!NdlCommons.isBareMetal(ni))
								throw new NdlException("Node " + n.getName() + " is not bare-metal and does not specify an image");
								
						}

						// if no global domain domain is set, declare a domain and add inDomain property
						if (!globalDomain && (n.getDomain() != null)) {
							if (!GUIRequestState.getInstance().isAKnownDomain(n.getDomain()))
								throw new NdlException("Domain " + n.getDomain() + " of node " + n + " is not visible from this SM!");
							Individual domI = ngen.declareDomain(domainMap.get(n.getDomain()));
							ngen.addNodeToDomain(domI, ni);
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
				
				// node dependencies and color extensions (done afterwards to be sure all nodes are declared)
				for (OrcaNode n: GUIRequestState.getInstance().getGraph().getVertices()) {
					Individual ni = ngen.getRequestIndividual(n.getName());
					for(OrcaNode dep: n.getDependencies()) {
						Individual depI = ngen.getRequestIndividual(dep.getName());
						if (depI != null) {
							ngen.addDependOnToIndividual(depI, ni);
						}
					}
					// see if any color extensions have been added
					processColorOnNE(n);
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
						
						if (colorLink(e)) {
							processColorLink((OrcaColorLink)e);
							continue;
						}
						
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
						
						// link color extensions
						processColorOnNE(e);
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
			} finally {
				if (ngen != null)
					ngen.done();
			}
		}
		return res;
	}
	
	/**
	 * Save to file
	 * @param f
	 * @param g
	 * @param nsGuid
	 * @return
	 */
	public boolean saveGraph(File f, final SparseMultigraph<OrcaNode, OrcaLink> g, final String nsGuid) {
		assert(f != null);

		String ndl = convertGraphToNdl(g, nsGuid);
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

	/**
	 * Save to string
	 * @param f
	 * @param g
	 * @param nsGuid
	 * @return
	 */
	public boolean saveGraph(String f, final SparseMultigraph<OrcaNode, OrcaLink> g, final String nsGuid) {
		
		f = convertGraphToNdl(g, nsGuid);
		if (f == null)
			return false;
		
		return true;
	}
	
	public SparseMultigraph<OrcaNode, OrcaLink> loadGraph(File f) {
		return null;
	}
	
	
	// use different maps to try to do a reverse lookup
	private static String reverseLookupDomain_(Resource dom, Map<String, String> m, String suffix) {
		String domainName = StringUtils.removeStart(dom.getURI(), NdlCommons.ORCA_NS);
		if (domainName == null)
			return null;
		
		// remove one or the other
		domainName = StringUtils.removeEnd(domainName, suffix);
		for (Iterator<Map.Entry<String, String>> domName = m.entrySet().iterator(); domName.hasNext();) {
			Map.Entry<String, String> e = domName.next();
			if (domainName.equals(e.getValue()))
				return e.getKey();
		}
		return null;
	}
	
	// use different maps to try to do a reverse lookup
	private static String reverseLookupDomain_(String dom, Map<String, String> m, String suffix) {
		String domainName = StringUtils.removeStart(dom, NdlCommons.ORCA_NS);
		if (domainName == null)
			return null;
		
		// remove one or the other
		domainName = StringUtils.removeEnd(domainName, suffix);
		for (Iterator<Map.Entry<String, String>> domName = m.entrySet().iterator(); domName.hasNext();) {
			Map.Entry<String, String> e = domName.next();
			if (domainName.equals(e.getValue()))
				return e.getKey();
		}
		return null;
	}
	
	/**
	 * Do a reverse lookup on domain (NDL -> short name)
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
		
		// try vm domain, then net domain
		String mapping = reverseLookupDomain_(dom, domainMap, "/Domain");
		if (mapping == null)
			mapping = reverseLookupDomain_(dom, domainMap, "/Domain/vm");
		if (mapping == null) 
			mapping = reverseLookupDomain_(dom, netDomainMap, "/Domain/vlan");
		if (mapping == null)
			mapping = reverseLookupDomain_(dom, domainMap, "/Domain/lun");
		
		return mapping;
	}
	
	public static String reverseLookupDomain(String dom) {
		if (dom == null)
			return null;
		// strip off name space and "/Domain"
		String domainName = StringUtils.removeStart(dom, NdlCommons.ORCA_NS);
		if (domainName == null)
			return null;
		
		// try vm domain, then net domain
		String mapping = reverseLookupDomain_(dom, domainMap, "/Domain");
		if (mapping == null)
			mapping = reverseLookupDomain_(dom, domainMap, "/Domain/vm");
		if (mapping == null) 
			mapping = reverseLookupDomain_(dom, netDomainMap, "/Domain/vlan");
		
		return mapping;
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
	
	// FIXME: order of nodes is random here and color links must
	// be directional
	private void processColorLink(OrcaColorLink e) throws NdlException {
		// Declare a new color and save its key map and blob
		
		Individual colorI = ngen.declareColor(e.getColor().getLabel(), 
				e.getColor().getKeys(), e.getColor().getBlob(), e.getColor().getXMLBlobState());
		
		Pair<OrcaNode> pn = GUIRequestState.getInstance().getGraph().getEndpoints(e);
		
		Individual fromI = ngen.getRequestIndividual(pn.getFirst().getName());
		Individual toI = ngen.getRequestIndividual(pn.getSecond().getName());
		
		if ((fromI != null) && (toI != null)) {
			ngen.encodeColorDependency(fromI, toI, colorI);
		} else
			throw new NdlException("Null resource as head or tail of color link " + e.getLabel());
	}
	
	/**
	 * good for nodes and links
	 * @param or
	 * @throws NdlException
	 */
	private void processColorOnNE(OrcaResource or) throws NdlException {
		for(OrcaColor color: or.getColors()) {
			Individual colorI = ngen.declareColor(color.getLabel(), 
				color.getKeys(), color.getBlob(), color.getXMLBlobState());
			Individual neI = ngen.getRequestIndividual(or.getName());
			if (neI != null) { 
				ngen.addColorToIndividual(neI, colorI);
			} else
				throw new NdlException("Null resource" + or.getName() + " with color extension");
				
		}
	}
}
