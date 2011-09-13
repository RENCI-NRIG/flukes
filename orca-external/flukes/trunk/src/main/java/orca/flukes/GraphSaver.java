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

package orca.flukes;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import orca.ndl.NdlException;
import orca.ndl.NdlGenerator;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.Individual;
import com.hyperrealm.kiwi.ui.dialog.ExceptionDialog;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.Pair;

public class GraphSaver {
	public static final String DOT_FORMAT = "DOT";
	public static final String N3_FORMAT = "N3";
	public static final String RDF_XML_FORMAT = "RDF-XML";

	private static GraphSaver instance = null;
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
	static Map<String, String> domainMap;
	static {
		domainMap = new HashMap<String, String>();
		domainMap.put("RENCI", "rencivmsite.rdf#rencivmsite");
		domainMap.put("UNC", "uncvmsite.rdf#uncvmsite");
		domainMap.put("Duke", "dukevmsite.rdf#dukevmsite");
	}
	
	public static GraphSaver getInstance() {
		if (instance == null)
			instance = new GraphSaver();
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
			ngen.addIPToIndividual(n.getIp(e), intI);
			if (n.getNm(e) != null)
				ngen.addNetmaskToIP(n.getIp(e), netmaskConverter[Integer.parseInt(n.getNm(e)) - 1]);
		}
	}
	
	/**
	 * Save graph using NDL
	 * @param f
	 * @param g
	 */
	public void saveGraph(File f, SparseMultigraph<OrcaNode, OrcaLink> g) {
		// this should never run in parallel anyway
		synchronized(instance) {
			try {
				if ((f == null) || (g == null))
					return;
				
				ngen = new NdlGenerator(Logger.getLogger(this.getClass().getCanonicalName()));
			
				reservation = ngen.declareReservation();
				Individual term = ngen.declareTerm();
				// not an immediate reservation? declare term beginning
				if (GUIState.getInstance().getTerm().getStart() != null) {
					Individual tStart = ngen.declareTermBeginning(GUIState.getInstance().getTerm().getStart());
					ngen.addBeginningToTerm(tStart, term);
				}
				// now duration
				GUIState.getInstance().getTerm().normalizeDuration();
				Individual duration = ngen.declareTermDuration(GUIState.getInstance().getTerm().getDurationDays(), 
						GUIState.getInstance().getTerm().getDurationHours(), GUIState.getInstance().getTerm().getDurationMins());
				ngen.addDurationToTerm(duration, term);
				ngen.addTermToReservation(term, reservation);
				
				// decide whether we have a global image
				boolean globalImage = false, globalDomain = false;
				
				// is image specified in the reservation?
				if (GUIState.getInstance().getVMImageInReservation() != null) {
					// there is a global image (maybe)
					OrcaImage im = GUIState.getInstance().definedImages.get(GUIState.getInstance().getVMImageInReservation());
					if (im != null) {
						// definitely an global image - attach it to the reservation
						globalImage = true;
						// TODO: check for zero length
						Individual imI = ngen.declareDiskImage(im.getUrl().toString(), im.getHash(), im.getShortName());
						ngen.addDiskImageToIndividual(imI, reservation);
					}
				}
				
				// is domain specified in the reservation?
				if (GUIState.getInstance().getDomainInReservation() != null) {
					globalDomain = true;
					Individual domI = ngen.declareDomain(GUIState.getInstance().getDomainInReservation());
					ngen.addDomainToIndividual(domI, reservation);
				}
				
				// shove invidividual nodes onto the reservation
				for (OrcaNode n: GUIState.getInstance().g.getVertices()) {
					Individual ni = ngen.declareComputeElement(n.getName());
					ngen.addResourceToReservation(reservation, ni);
					
					// for clusters, add number of nodes, declare as cluster (VM domain)
					if (!n.isNode()) {
						ngen.addNumCEsToCluster(n.getNodeCount(), ni);
						ngen.addVMDomainProperty(ni);
					}
					
					// if no global image is set and a local image is set, add it to node
					if (!globalImage && (n.getImage() != null)) {
						// check if image is set in this node
						OrcaImage im = GUIState.getInstance().definedImages.get(n.getImage());
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
					if ((n.getNodeType() != null) && (GUIState.nodeTypes.get(n.getNodeType()) != null)) {
						Pair<String> nt = GUIState.nodeTypes.get(n.getNodeType());
						ngen.addNodeTypeToCE(nt.getFirst(), nt.getSecond(), ni);
					}
				}
				
				if (GUIState.getInstance().g.getEdgeCount() == 0) {
					// a bunch of disconnected nodes, no IP addresses 
					
				} else {
					// edges, nodes, IP addresses oh my!
					for (OrcaLink e: GUIState.getInstance().g.getEdges()) {
						Individual ei = ngen.declareNetworkConnection(e.getName());
						ngen.addResourceToReservation(reservation, ei);

						if (e.getBandwidth() > 0)
							ngen.addBandwidthToConnection(ei, e.getBandwidth());
						
						// TODO: deal with layers later
						ngen.addLayerToConnection(ei, "ethernet", "EthernetNetworkElement");

						// TODO: latency
						
						Pair<OrcaNode> pn = GUIState.getInstance().g.getEndpoints(e);
						processNodeAndLink(pn.getFirst(), e, ei);
						processNodeAndLink(pn.getSecond(), e, ei);
					}
				}
				
				// save the contents
				String res = getFormattedOutput(ngen, outputFormat);
				FileWriter fsw = new FileWriter(f);
				fsw.write(res);
				fsw.close();
			} catch (Exception e) {
				ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
				ed.setLocationRelativeTo(GUI.getInstance().getFrame());
				ed.setException("Exception encountered while saving file", e);
				ed.setVisible(true);
			}
		}
	}

	public SparseMultigraph<OrcaNode, OrcaLink> loadGraph(File f) {
		return null;
	}
	
}
