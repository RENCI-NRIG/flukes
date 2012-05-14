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

import java.util.Map;

import orca.flukes.GUIResourceState;
import orca.flukes.OrcaResourceSite;
import orca.flukes.xmlrpc.RegistryXMLRPCProxy;
import orca.ndl.NdlCommons;
import orca.ndl.OntProcessor;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.ResultBinding;
import com.hyperrealm.kiwi.util.ProgressObserver;

/**
 * Process various queries to the registry
 * @author ibaldin
 *
 */
public class ResourceQueryProcessor {

	/**
	 * Process getAMs query from the registry by adding nodes to the resource graph
	 * @throws Exception
	 */
	public static synchronized void processAMQuery(ProgressObserver o) throws Exception {
		Map<String, Map<String, String>> amData;
		
		o.setProgress(0);
		amData = RegistryXMLRPCProxy.getInstance().getAMs(true);
		
		float progress=15;
		o.setProgress((int)progress);
		for (Map.Entry<String, Map<String, String>> am: amData.entrySet()) {
			progress += 85f/amData.size();
			o.setProgress((int)progress);
			
			String fullRdf = RegistryXMLRPCProxy.getField(am.getKey(), amData, RegistryXMLRPCProxy.Field.FULLRDF);
			if (fullRdf == null) {
				continue;
			}
			// create a model from the description
			OntModel amModel = NdlCommons.getModelFromString(fullRdf);
			// query
			String query = NdlCommons.createQueryStringDomainLocationDetails();
			ResultSet rs = OntProcessor.rdfQuery(amModel, query);
			
			if (rs.hasNext()) {
				ResultBinding result = (ResultBinding)rs.next();
				Resource domain = (Resource)result.get("domain");
				Resource pop = (Resource)result.get("popUri");
				Literal lat = (Literal)result.get("lat");
				Literal lon = (Literal)result.get("lon");
				String name = RegistryXMLRPCProxy.getField(am.getKey(), amData, RegistryXMLRPCProxy.Field.DESCRIPTION);
				if (name == null)
					name = domain.toString();
				OrcaResourceSite ors = GUIResourceState.getInstance().createSite(name, lat.getFloat(), lon.getFloat());
				
			}
		}
		o.setProgress(100);
	}
}
