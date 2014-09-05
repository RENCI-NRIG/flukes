package orca.flukes.ndl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import orca.ndl.INdlAbstractDelegationModelListener;
import orca.ndl.elements.LabelSet;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Basic parser of Ads provided by an SM
 * @author ibaldin
 *
 */
public class AdLoader implements INdlAbstractDelegationModelListener {
	List<String> domains = new ArrayList<String>();
	
	@Override
	public void ndlNetworkDomain(Resource dom, OntModel m,
			List<Resource> netServices, List<Resource> interfaces,
			List<LabelSet> labelSets, Map<Resource, List<LabelSet>> netLabelSets) {
		domains.add(dom.toString());
	}

	@Override
	public void ndlInterface(Resource l, OntModel om, Resource conn,
			Resource node, String ip, String mask) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ndlNetworkConnection(Resource l, OntModel om, long bandwidth,
			long latency, List<Resource> interfaces) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ndlNode(Resource ce, OntModel om, Resource ceClass,
			List<Resource> interfaces) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ndlParseComplete() {
		// TODO Auto-generated method stub

	}
	
	public List<String> getDomains() {
		return domains;
	}

}
