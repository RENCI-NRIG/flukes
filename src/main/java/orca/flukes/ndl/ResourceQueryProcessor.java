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
