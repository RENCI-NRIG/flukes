package orca.flukes;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import orca.flukes.ndl.AdLoader;
import orca.flukes.ndl.RequestSaver;
import orca.flukes.xmlrpc.OrcaSMXMLRPCProxy;
import orca.ndl.NdlAbstractDelegationParser;
import orca.ndl.NdlException;

import com.hyperrealm.kiwi.ui.dialog.ExceptionDialog;

/**
 * Reflects what we know about a rack/domain/controller
 * @author ibaldin
 *
 */
public class GUIDomainState {
	private static final String RDF_START = "<rdf:RDF";
	private static final String RDF_END = "</rdf:RDF>";
	public static final String NO_DOMAIN_SELECT = "System select";
	
	// VM domains known to this controller
	private List<String> knownDomains = null;
	
	// Resource availability of the current SM
	private Map<String, Map<String, Integer>> resourceSlots = null;

	private static GUIDomainState instance = new GUIDomainState();
	
	public static GUIDomainState getInstance() {
		return instance;
	}
	
	private GUIDomainState() {
		
	}
	
	/**
	 * Return available domains
	 * @return
	 */
	public String[] getAvailableDomains() {
		if (knownDomains == null)
			listSMResources();
		
		Collections.sort(knownDomains);
		
		String[] itemList = new String[knownDomains.size() + 1];
		
		int index = 0;
		itemList[index] = NO_DOMAIN_SELECT;
		
		for(String s: knownDomains) {
			itemList[++index] = s;
		}
		
		return itemList;
	}
	
	private Calendar checkDate = null;
	
	/**
	 * Query for resource avialability on current controller (rate-controlled)
	 * @return
	 */
	public Map<String, Map<String, Integer>> updateResourceSlots() {
		// checkDate may well need to be reset every time controller
		// is changed, however since that also involves calling listSMResources(),
		// it's ok not to do that /ib
		if ((resourceSlots == null) || (checkDate == null)) { 
			checkDate = Calendar.getInstance();
			listSMResources();
		} else {
			checkDate.add(Calendar.MINUTE, 1);
			if (Calendar.getInstance().after(checkDate)) {
				checkDate = Calendar.getInstance();
				listSMResources();
			}
		}

		// Yes, I know it is shallow /ib
		return Collections.unmodifiableMap(resourceSlots);
	}
	
	/**
	 * Is this a known domain
	 * @return
	 */
	public boolean isAKnownDomain(String d) {
		if (knownDomains != null)
			return knownDomains.contains(d);
		return true;
	}
	
	/**
	 * Return null if 'System select' domain is asked for
	 * 
	 */
	public static String getNodeDomainProper(String domain) {
		if ((domain == null) || domain.equals(NO_DOMAIN_SELECT))
			return null;
		else
			return domain;
	}
	
	
	// sets the knownDomains instance variable based
	// on a query to the selected SM and populates resource slots
	public void listSMResources() {
		// query the selected controller for resources
		try {
			// re-initialize known domains and resource slots
			knownDomains = new ArrayList<String>();
			resourceSlots = new TreeMap<String, Map<String, Integer>>();
			
			String ads = OrcaSMXMLRPCProxy.getInstance().listResources();
			List<String> domains = new ArrayList<String>();

			try {
				
				boolean done = false;
				while (!done) {
					// find <rdf:RDF> and </rdf:RDF>
					int start = ads.indexOf(RDF_START);
					int end = ads.indexOf(RDF_END);
					if ((start == -1) || (end == -1)) {
						done = true;
						continue;
					}
					String ad = ads.substring(start, end + RDF_END.length());

					AdLoader adl = new AdLoader();
					// parse out
					NdlAbstractDelegationParser nadp = new NdlAbstractDelegationParser(ad, adl);
					
					// this will call the callbacks
					nadp.processDelegationModel();
					
					domains.add(adl.getDomain());
					
					String domShortName = RequestSaver.reverseLookupDomain(adl.getDomain());
					if (domShortName == null)
						domShortName = adl.getDomain();
					
					if (!resourceSlots.containsKey(domShortName))
						resourceSlots.put(domShortName, new TreeMap<String, Integer>());
					resourceSlots.get(domShortName).putAll(adl.getSlots());
					
					nadp.freeModel();
					
					// advance pointer
					ads = ads.substring(end + RDF_END.length());
				}
			} catch (NdlException e) {
				return;
			}
			for(String d: domains) {
				if (d.endsWith("Domain/vm")) {
					String domName = RequestSaver.reverseLookupDomain(d);
					if (domName != null)
						knownDomains.add(domName);
				}
			}
			
		} catch (Exception ex) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while querying the controller for available resources: ", ex);
			ed.setVisible(true);
		}
	}
	
}
