package orca.flukes.xmlrpc;

import java.net.MalformedURLException;
import java.net.URL;
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

	private static final String SA_VERSION = "2";

	private static boolean saVersionMatch = false;

	public enum SaField {
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
		SLICE_EXPIRATION;
	}

	public enum SaCall {
		get_version, create, 
		lookup, update;
	}

	public enum SaObjectType {
		SLICE, PROJECT, SLIVER;
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

	private XmlRpcClient setupClient() throws MalformedURLException {
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(new URL(GUI.getInstance().getPreference(PrefsEnum.GENISA_URL)));
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
	public Map<String, Object> saGetVersion() throws Exception {
		setSSLIdentity(null, GUI.getInstance().getPreference(PrefsEnum.GENISA_URL));

		Map<String, Object> rr = null;
		try {
			XmlRpcClient client = setupClient();

			// create slice on SA
			rr = (Map<String, Object>)client.execute(SaCall.get_version.name(), new Object[]{});
			
			checkAPIError(rr);
			
			return (Map<String, Object>)rr.get(SaField.value.name());
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
	 * Check that we are talking to SA of the right version
	 * @return
	 */
	public void saCheckVersion() throws Exception {
		try {
			Map<String, Object> fields = saGetVersion();

			if (fields == null)
				throw new Exception("SA " + GUI.getInstance().getPreference(PrefsEnum.GENISA_URL) + " returned invalid values");
			if ((fields.get(SaField.VERSION.name()) != null) && ((String)fields.get(SaField.VERSION.name())).startsWith(SA_VERSION))
				return;
			else
				throw new Exception("SA " + GUI.getInstance().getPreference(PrefsEnum.GENISA_URL) + " version [" + fields.get(SaField.VERSION.name()) + "] is not compatible with this version of Flukes");
		} catch (Exception e) {
			throw new Exception("Unable to communicate with SA "+ GUI.getInstance().getPreference(PrefsEnum.GENISA_URL) + " due to " + e);
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

		saCompatible();

		Map<String, Object> rr = null;
		try {
			XmlRpcClient client = setupClient();

			// create slice on SA
			Map<String, Object> options = new HashMap<String, Object>();
			options.put(SaField.fields.name(), new HashMap<String, Object>());

			((Map<String, Object>)options.get(SaField.fields.name())).put(SaField.SLICE_NAME.name(), name);
			((Map<String, Object>)options.get(SaField.fields.name())).put(SaField.SLICE_PROJECT_URN.name(), projectUrn);

			rr = (Map<String, Object>)client.execute(SaCall.create.name(), new Object[]{SaObjectType.SLICE.name(), new Object[]{}, options});

			checkAPIError(rr);
			
			return (String)((Map<String, Object>)rr.get(SaField.value.name())).get(SaField.SLICE_URN.name());
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
	public void saUpdateSlice(String sliceUrn, SaField field, Object val) throws Exception {
		if ((sliceUrn == null) || (sliceUrn.length() == 0))
			throw new Exception("Invalid slice Urn: " + sliceUrn);

		setSSLIdentity(null, GUI.getInstance().getPreference(PrefsEnum.GENISA_URL));

		saCompatible();

		Map<String, Object> rr = null;
		try {
			XmlRpcClient client = setupClient();

			// create slice on SA
			Map<String, Object> options = new HashMap<String, Object>();
			options.put(SaField.fields.name(), new HashMap<String, Object>());

			((Map<String, Object>)options.get(SaField.fields.name())).put(field.name(), val);

			rr = (Map<String, Object>)client.execute(SaCall.update.name(), new Object[]{SaObjectType.SLICE.name(), sliceUrn, new Object[]{}, options});

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
	public Map<String, Object> saLookupSlice(String sliceUrn, SaField[] fields) throws Exception {
		if ((sliceUrn == null) || (sliceUrn.length() == 0))
			throw new Exception("Invalid slice Urn: " + sliceUrn);

		setSSLIdentity(null, GUI.getInstance().getPreference(PrefsEnum.GENISA_URL));

		saCompatible();

		Map<String, Object> rr = null;
		try {
			XmlRpcClient client = setupClient();

			// create slice on SA
			Map<String, Object> options = new HashMap<String, Object>();
			options.put(SaField.match.name(), new HashMap<String, Object>());

			((Map<String, Object>)options.get(SaField.match.name())).put(SaField.SLICE_URN.name(), new String[] {sliceUrn});

			if ((fields != null) && (fields.length > 0)) {
				String[] fieldNames = new String[fields.length];
				for(int i = 0; i < fields.length; i++)
					fieldNames[i] = fields[i].name();
				options.put(SaField.filter.name(), fieldNames);
			}

			rr = (Map<String, Object>)client.execute(SaCall.lookup.name(), new Object[]{SaObjectType.SLICE.name(), new Object[]{}, options});

			checkAPIError(rr);
			
			return (Map<String, Object>)rr.get(SaField.value.name());
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
	public Map<String, Object> saLookupSlice(String sliceUrn, List<SaField> fields) throws Exception {
		return saLookupSlice(sliceUrn, fields.toArray(new SaField[fields.size()]));
	}

	private void checkAPIError(Map<String, Object> r) throws Exception {
		if ((r.get(SaField.code.name()) != null) &&
				((Integer)r.get(SaField.code.name()) != 0)) 
			throw new Exception("SA API Error: [" + r.get(SaField.code.name()) + "]: " + r.get(SaField.output.name()));
	}

	/**
	 * Ensure we're compatible with this SA
	 * @throws Exception
	 */
	private synchronized void saCompatible() throws Exception {
		if (!saVersionMatch) { 
			saCheckVersion();
			saVersionMatch = true;
		}
	}
}
