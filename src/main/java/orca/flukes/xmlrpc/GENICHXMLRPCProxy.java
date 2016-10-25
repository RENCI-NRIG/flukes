package orca.flukes.xmlrpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import orca.flukes.GUI;
import orca.flukes.GUI.PrefsEnum;

import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.NullParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.NullSerializer;

/**
 * GENI CH API support (SA and others)
 * @author ibaldin
 *
 */
public class GENICHXMLRPCProxy extends OrcaXMLRPCBase {

	public static final String SSH_KEY_PRIVATE = "KEY_PRIVATE";
	public static final String SSH_KEY_PUBLIC = "KEY_PUBLIC";
	public static final String SSH_KEY_ID = "KEY_ID";
	public static final String SSH_KEY_MEMBER_GUID = "_GENI_KEY_MEMBER_UID";

	private static final String FED_VERSION = "2";

	private static boolean saVersionMatch = false, maVersionMatch = false;

	public enum FedField {
		VERSION, 
		code, 
		value,
		output,
		// used in get_version return
		FIELDS, 
		// used in create/lookup/update
		fields, 
		match,
		filter,
		SLICE_NAME, 
		SLICE_PROJECT_URN,
		SLICE_URN,
		SLICE_EXPIRATION,
		KEY_MEMBER;
	}

	public enum FedAgent {
		SA, MA
	}
	
	public enum FedCall {
		get_version, create, 
		lookup, update;
	}

	public enum FedObjectType {
		SLICE, PROJECT, SLIVER, KEY;
	}

	private static GENICHXMLRPCProxy instance = new GENICHXMLRPCProxy();

	/**
	 * To deal with 'nil' returned by CH
	 * @author ibaldin
	 *
	 */
	public class MyTypeFactory extends TypeFactoryImpl {

		public MyTypeFactory(XmlRpcController pController) {
			super(pController);
		}

		@Override
		public TypeParser getParser(XmlRpcStreamConfig pConfig,
				NamespaceContextImpl pContext, String pURI, String pLocalName) {

			if ("".equals(pURI) && NullSerializer.NIL_TAG.equals(pLocalName)) {
				return new NullParser();
			} else {
				return super.getParser(pConfig, pContext, pURI, pLocalName);
			}
		}
	}

	GENICHXMLRPCProxy() {

	}

	public static GENICHXMLRPCProxy getInstance() {
		return instance;
	}

	private XmlRpcClient setupClient(FedAgent a) throws MalformedURLException {
		String agentUrl = null;
		switch(a) {
		case SA:
			agentUrl = GUI.getInstance().getPreference(PrefsEnum.GENISA_URL);
			break;
		case MA:
			agentUrl = GUI.getInstance().getPreference(PrefsEnum.GENIMA_URL);
			break;
		}
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(new URL(agentUrl));
		XmlRpcClient client = new XmlRpcClient();
		config.setEnabledForExtensions(true);
		client.setConfig(config);
		client.setTypeFactory(new MyTypeFactory(client));

		// set this transport factory for host-specific SSLContexts to work
		XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
		client.setTransportFactory(f);

		return client;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> fedGetVersion(FedAgent a) throws Exception {
		String agentUrl = null;
		
		switch (a) {
		case SA: 
			agentUrl = GUI.getInstance().getPreference(PrefsEnum.GENISA_URL);
			break;
		case MA:
			agentUrl = GUI.getInstance().getPreference(PrefsEnum.GENIMA_URL);
			break;
		}
		setSSLIdentity(null, agentUrl);	
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");

		Map<String, Object> rr = null;
		try {
			XmlRpcClient client = setupClient(a);

			// create slice on SA
			rr = (Map<String, Object>)client.execute(FedCall.get_version.name(), new Object[]{});
			
			checkAPIError(rr);
			
			return (Map<String, Object>)rr.get(FedField.value.name());
		} catch (MalformedURLException e) {
			throw new Exception("Please check the " + a + " URL " + agentUrl);
		} catch (XmlRpcException e) {
			e.printStackTrace();
			throw new Exception("Unable to contact " + a + " " + agentUrl + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to create slice on " + a + ":  " + agentUrl + " due to " + e);
		}
	}

	/**
	 * Check that we are talking to SA of the right version
	 * @return
	 */
	public void fedCheckVersion(FedAgent a) throws Exception {
		try {
			Map<String, Object> fields = fedGetVersion(a);

			if (fields == null)
				throw new Exception(a + " returned invalid values or SSL identity not set");
			if ((fields.get(FedField.VERSION.name()) != null) && ((String)fields.get(FedField.VERSION.name())).startsWith(FED_VERSION))
				return;
			else
				throw new Exception(a + " version [" + fields.get(FedField.VERSION.name()) + "] is not compatible with this version of Flukes");
		} catch (Exception e) {
			throw new Exception("Unable to communicate with " + a + " due to " + e);
		}
	}

	/**
	 * Create slice on the SA using slice name and project URN
	 * @param name
	 * @param projectUrn
	 * @return the URN of the created slice
	 */
	@SuppressWarnings("unchecked")
	public String saCreateSlice(String name, String projectUrn) throws Exception {
		if ((name == null) || (projectUrn == null) ||
				(name.length() == 0) || (projectUrn.length() == 0))
			throw new Exception("Invalid slice name or project urn: " + name + "/" + projectUrn);

		setSSLIdentity(null, GUI.getInstance().getPreference(PrefsEnum.GENISA_URL));

		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");
		
		saCompatible();

		Map<String, Object> rr = null;
		try {
			XmlRpcClient client = setupClient(FedAgent.SA);

			// create slice on SA
			Map<String, Object> options = new HashMap<String, Object>();
			options.put(FedField.fields.name(), new HashMap<String, Object>());

			((Map<String, Object>)options.get(FedField.fields.name())).put(FedField.SLICE_NAME.name(), name);
			((Map<String, Object>)options.get(FedField.fields.name())).put(FedField.SLICE_PROJECT_URN.name(), projectUrn);

			rr = (Map<String, Object>)client.execute(FedCall.create.name(), new Object[]{FedObjectType.SLICE.name(), new Object[]{}, options});

			checkAPIError(rr);
			
			return (String)((Map<String, Object>)rr.get(FedField.value.name())).get(FedField.SLICE_URN.name());
		} catch (MalformedURLException e) {
			throw new Exception("Please check the SA URL " + GUI.getInstance().getPreference(PrefsEnum.GENISA_URL));
		} catch (XmlRpcException e) {
			e.printStackTrace();
			throw new Exception("Unable to contact SA " + GUI.getInstance().getPreference(PrefsEnum.GENISA_URL) + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to create slice on SA:  " + GUI.getInstance().getPreference(PrefsEnum.GENISA_URL) + " due to " + e);
		}
	}

	/**
	 * Update slice field to a specific value
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public void saUpdateSlice(String sliceUrn, FedField field, Object val) throws Exception {
		if ((sliceUrn == null) || (sliceUrn.length() == 0))
			throw new Exception("Invalid slice Urn: " + sliceUrn);

		setSSLIdentity(null, GUI.getInstance().getPreference(PrefsEnum.GENISA_URL));
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");
		
		saCompatible();

		Map<String, Object> rr = null;
		try {
			XmlRpcClient client = setupClient(FedAgent.SA);

			// create slice on SA
			Map<String, Object> options = new HashMap<String, Object>();
			options.put(FedField.fields.name(), new HashMap<String, Object>());

			((Map<String, Object>)options.get(FedField.fields.name())).put(field.name(), val);

			rr = (Map<String, Object>)client.execute(FedCall.update.name(), new Object[]{FedObjectType.SLICE.name(), sliceUrn, new Object[]{}, options});

			checkAPIError(rr);
			
		} catch (MalformedURLException e) {
			throw new Exception("Please check the SA URL " + GUI.getInstance().getPreference(PrefsEnum.GENISA_URL));
		} catch (XmlRpcException e) {
			e.printStackTrace();
			throw new Exception("Unable to contact SA " + GUI.getInstance().getPreference(PrefsEnum.GENISA_URL) + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to update " + field.name() + " of slice " + sliceUrn + " on SA:  " + GUI.getInstance().getPreference(PrefsEnum.GENISA_URL) + " due to " + e);
		}
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> saLookupSlice(String sliceUrn, FedField[] fields) throws Exception {
		if ((sliceUrn == null) || (sliceUrn.length() == 0))
			throw new Exception("Invalid slice Urn: " + sliceUrn);

		setSSLIdentity(null, GUI.getInstance().getPreference(PrefsEnum.GENISA_URL));

		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");
		
		saCompatible();

		Map<String, Object> rr = null;
		try {
			XmlRpcClient client = setupClient(FedAgent.SA);

			// create slice on SA
			Map<String, Object> options = new HashMap<String, Object>();
			options.put(FedField.match.name(), new HashMap<String, Object>());

			((Map<String, Object>)options.get(FedField.match.name())).put(FedField.SLICE_URN.name(), new String[] {sliceUrn});

			if ((fields != null) && (fields.length > 0)) {
				String[] fieldNames = new String[fields.length];
				for(int i = 0; i < fields.length; i++)
					fieldNames[i] = fields[i].name();
				options.put(FedField.filter.name(), fieldNames);
			}

			rr = (Map<String, Object>)client.execute(FedCall.lookup.name(), new Object[]{FedObjectType.SLICE.name(), new Object[]{}, options});

			checkAPIError(rr);
			
			return (Map<String, Object>)rr.get(FedField.value.name());
		} catch (MalformedURLException e) {
			throw new Exception("Please check the SA URL " + GUI.getInstance().getPreference(PrefsEnum.GENISA_URL));
		} catch (XmlRpcException e) {
			e.printStackTrace();
			throw new Exception("Unable to contact SA " + GUI.getInstance().getPreference(PrefsEnum.GENISA_URL) + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to lookup slice " + sliceUrn + " on SA:  " + GUI.getInstance().getPreference(PrefsEnum.GENISA_URL) + " due to " + e);
		}
	}
	
	/**
	 * Lookup either a list of fields or all fields in a slice
	 * @param sliceUrn
	 * @param field
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> saLookupSlice(String sliceUrn, List<FedField> fields) throws Exception {
		return saLookupSlice(sliceUrn, fields.toArray(new FedField[fields.size()]));
	}

	/**
	 * MA Call to get all public and private SSH keys of a user
	 * @param userUrn
	 * @param fields
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> maLookupAllSSHKeys(String userUrn) throws Exception {
		if ((userUrn == null) || (userUrn.length() == 0))
			throw new Exception("Invalid user Urn: " + userUrn);

		setSSLIdentity(null, GUI.getInstance().getPreference(PrefsEnum.GENIMA_URL));
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");
		
		maCompatible();

		Map<String, Object> rr = null;
		try {
			XmlRpcClient client = setupClient(FedAgent.MA);

			Map<String, Object> options = new HashMap<String, Object>();
			options.put(FedField.match.name(), new HashMap<String, Object>());

			((Map<String, Object>)options.get(FedField.match.name())).put(FedField.KEY_MEMBER.name(), new String[] {userUrn});

			rr = (Map<String, Object>)client.execute(FedCall.lookup.name(), new Object[]{FedObjectType.KEY.name(), new Object[]{}, options});

			checkAPIError(rr);
			
			return (Map<String, Object>)rr.get(FedField.value.name());
		} catch (MalformedURLException e) {
			throw new Exception("Please check the MA URL " + GUI.getInstance().getPreference(PrefsEnum.GENIMA_URL));
		} catch (XmlRpcException e) {
			e.printStackTrace();
			throw new Exception("Unable to contact MA " + GUI.getInstance().getPreference(PrefsEnum.GENIMA_URL) + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to lookup SSH keys for " + userUrn + " on MA:  " + GUI.getInstance().getPreference(PrefsEnum.GENIMA_URL) + " due to " + e);
		}
	}
	
	/**
	 * Get the latest pair of private (if available) and public SSH keys. 
	 * @param userUrn
	 * @return a map with up to two keys: KEY_PUBLIC and KEY_PRIVATE. 
	 * @throws Exception
	 */
	public Map<String, Object> maLookupLatestSSHKeys(String userUrn) throws Exception {
		
		Map<String, Object> ret = maLookupAllSSHKeys(userUrn);
		
		List<String> keys = new ArrayList<String>(ret.keySet());
		java.util.Collections.sort(keys, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				try {
					// these are numbers
					return Integer.parseInt(o1) - Integer.parseInt(o2);
				} catch (NumberFormatException nfe) {
					// default to lexicographic comparison
					return o1.compareTo(o2);
				}
			}
		});
		if (keys.size() < 1) {
			throw new Exception("No SSH keys available for user " + userUrn + " from MA " + GUI.getInstance().getPreference(PrefsEnum.GENIMA_URL));
		}
		Map<String, Object> latestKeys = (Map<String, Object>)ret.get(keys.get(0));
		
		return latestKeys;
	}
	
	private void checkAPIError(Map<String, Object> r) throws Exception {
		if ((r.get(FedField.code.name()) != null) &&
				((Integer)r.get(FedField.code.name()) != 0)) 
			throw new Exception("FED API Error: [" + r.get(FedField.code.name()) + "]: " + r.get(FedField.output.name()));
	}

	/**
	 * Ensure we're compatible with this SA
	 * @throws Exception
	 */
	private synchronized void saCompatible() throws Exception {
		if (!saVersionMatch) { 
			fedCheckVersion(FedAgent.SA);
			saVersionMatch = true;
		}
	}
	
	/**
	 * Ensure we're compatible with this SA
	 * @throws Exception
	 */
	private synchronized void maCompatible() throws Exception {
		if (!maVersionMatch) { 
			fedCheckVersion(FedAgent.MA);
			maVersionMatch = true;
		}
	}
}
