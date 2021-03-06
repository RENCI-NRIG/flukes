package orca.flukes.xmlrpc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;

import orca.flukes.GUI;

import org.apache.commons.lang.Validate;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;

import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

public class OrcaSMXMLRPCProxy extends OrcaXMLRPCBase {
	private static final String RET_RET_FIELD = "ret";
	private static final String MSG_RET_FIELD = "msg";
	private static final String ERR_RET_FIELD = "err";
	private static final String TERM_END_FIELD = "termEnd";
	private static final String GET_VERSION = "orca.getVersion";
	private static final String SLICE_STATUS = "orca.sliceStatus";
	private static final String CREATE_SLICE = "orca.createSlice";
	private static final String DELETE_SLICE = "orca.deleteSlice";
	private static final String MODIFY_SLICE = "orca.modifySlice";
	private static final String MODIFY_SLIVER = "orca.modifySliver";
	private static final String RENEW_SLICE = "orca.renewSlice";
	private static final String LIST_SLICES = "orca.listSlices";
	private static final String LIST_RESOURCES = "orca.listResources";
	private static final String GET_SLIVER_PROPERTIES = "orca.getSliverProperties";
	private static final String GET_RESERVATION_STATES = "orca.getReservationStates";
	private static final String GET_RESERVATION_SLICE_STITCH_INFO = "orca.getReservationSliceStitchInfo";
	private static final String PERMIT_SLICE_STITCH = "orca.permitSliceStitch";
	private static final String REVOKE_SLICE_STITCH = "orca.revokeSliceStitch";
	private static final String PERFORM_SLICE_STITCH = "orca.performSliceStitch";
	private static final String UNDO_SLICE_STITCH = "orca.undoSliceStitch";
	private static final String SSH_DSA_PUBKEY_FILE = "id_dsa.pub";
	private static final String SSH_RSA_PUBKEY_FILE = "id_rsa.pub";

	OrcaSMXMLRPCProxy() {
		;
	}

	private static OrcaSMXMLRPCProxy instance = new OrcaSMXMLRPCProxy();

	public static OrcaSMXMLRPCProxy getInstance() {
		return instance;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getVersion() throws Exception {
		Map<String, Object> versionMap = null;
		setSSLIdentity(null, GUI.getInstance().getSelectedController());
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// get verbose list of the AMs
			versionMap = (Map<String, Object>)client.execute(GET_VERSION, new Object[]{});
		} catch (MalformedURLException e) {
			throw new Exception("Please check the controller URL " + GUI.getInstance().getSelectedController());
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController() + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());
		}
		return versionMap;
	}

	/** submit an ndl request to create a slice, using explicitly specified users array
	 * 
	 * @param sliceId
	 * @param resReq
	 * @param users
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String createSlice(String sliceId, String resReq, List<Map<String, ?>> users) throws Exception {
	    Validate.notNull(sliceId);
	    Validate.notNull(resReq);
	    Validate.notNull(users);
	    Validate.notEmpty(users);

		String result = null;
		setSSLIdentity(null, GUI.getInstance().getSelectedController());
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");
		
		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// create sliver
			rr = (Map<String, Object>)client.execute(CREATE_SLICE, new Object[]{ sliceId, new Object[]{}, resReq, users});
		} catch (MalformedURLException e) {
			throw new Exception("Please check the controller URL " + GUI.getInstance().getSelectedController());
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController() + " due to " + e);
		} catch (Exception e) {
			return "Unable to submit slice to controller:  " + GUI.getInstance().getSelectedController() + " due to " + e;
		}

		if (rr == null)
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception("Unable to create slice: " + (String)rr.get(MSG_RET_FIELD));

		result = (String)rr.get(RET_RET_FIELD);
		return result;
	}

	/** submit an ndl request to create a slice, using explicitly specified users array
	 * 
	 * @param sliceId
	 * @param resReq
	 * @param users
	 * @return - string representing the date to which the slice was renewed or empty string
	 * if renew successful, but date not provided
	 */
	@SuppressWarnings("unchecked")
	public String renewSlice(String sliceId, Date newDate) throws Exception {
		Validate.notNull(sliceId);
		Validate.notNull(newDate);

		String result = "";
		setSSLIdentity(null, GUI.getInstance().getSelectedController());
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");
		
		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// create sliver
			Calendar ecal = Calendar.getInstance();
			ecal.setTime(newDate);
			String endDateString = DatatypeConverter.printDateTime(ecal); // RFC3339/ISO8601
			rr = (Map<String, Object>)client.execute(RENEW_SLICE, new Object[]{ sliceId, new Object[]{}, endDateString});
		} catch (MalformedURLException e) {
			throw new Exception("Please check the controller URL " + GUI.getInstance().getSelectedController());
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController() + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());
		}

		if (rr == null)
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception("Unable to renew slice: " + (String)rr.get(MSG_RET_FIELD));

		if (rr.containsKey(TERM_END_FIELD))
			result = (String)rr.get(TERM_END_FIELD);
		return result;
	}

	/**
	 * submit an ndl request to create a slice using this user's credentials
	 * @param sliceId
	 * @param resReq
	 * @param users
	 * @return
	 */
	public String createSlice(String sliceId, String resReq) throws Exception {
		setSSLIdentity(null, GUI.getInstance().getSelectedController());
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");

		// collect user credentials from $HOME/.ssh or load from portal

		// create an array
		List<Map<String, ?>> users = new ArrayList<Map<String, ?>>();
		String keyPathStr = null;
		String userKey = null;
		File keyPath;
		
		if ("file".equals(GUI.getInstance().getPreference(GUI.PrefsEnum.SSH_KEY_SOURCE))) {
			keyPathStr = GUI.getInstance().getPreference(GUI.PrefsEnum.SSH_PUBKEY);
			if (keyPathStr.startsWith("~/")) {
				keyPathStr = keyPathStr.replaceAll("~/", "/");
				keyPath = new File(System.getProperty("user.home"), keyPathStr);
			}
			else {
				keyPath = new File(keyPathStr);
			}

			userKey = getUserKeyFile(keyPath);

			if (userKey == null) 
				throw new Exception("Unable to load user public ssh key " + keyPath);
		} else if ((GUI.getInstance().getPreference(GUI.PrefsEnum.ENABLE_GENIMA).equalsIgnoreCase("true") ||
				GUI.getInstance().getPreference(GUI.PrefsEnum.ENABLE_GENIMA).equalsIgnoreCase("yes")) &&
				("portal".equals(GUI.getInstance().getPreference(GUI.PrefsEnum.SSH_KEY_SOURCE)))) {
			// get our urn based on the established cert
			String urn = getAltNameUrn();
			if (urn == null) {
				throw new Exception("Unable to obtain user GENI URN from the certificate file, cannot query GENI portal for the SSH keys, please change the '" + 
						GUI.PrefsEnum.SSH_KEY_SOURCE.name() + "' property to  'file'");
			}
			
			GUI.logger().info("Querying CH MA for SSH keys");
			GENICHXMLRPCProxy chp = GENICHXMLRPCProxy.getInstance();
			// this is a map that has a bunch of entries, including KEY_PRIVATE and KEY_PUBLIC
			// Map looks like this: SSH Keys: {_GENI_KEY_MEMBER_UID=c7578309-b14c-4b4a-b555-1eff10f1b092, 
			// _GENI_KEY_FILENAME=id_dsa.pub, 
			// KEY_PUBLIC=ssh-dss <key material>= user@hostname, 
			// KEY_TYPE=<key material> or null, KEY_DESCRIPTION=Kobol, KEY_MEMBER=urn:publicid:IDN+ch.geni.net+user+ibaldin, KEY_PRIVATE=null, KEY_ID=13}
			// we should not use it unless both entries are present
			Map<String, Object> keys = chp.maLookupLatestSSHKeys(urn);
			
			if ((keys == null) || 
					(keys.get(GENICHXMLRPCProxy.SSH_KEY_PUBLIC) == null)) {
				throw new Exception("Unable to obtain public SSH key from the portal for user " + urn + ", please change the '" +
						GUI.PrefsEnum.SSH_KEY_SOURCE.name() + "' property to  'file' or set proper SSL identity");
			}
			GUI.logger().info("Using public SSH key obtained from the portal");
			userKey = (String)keys.get(GENICHXMLRPCProxy.SSH_KEY_PUBLIC);
			
			// if private key is available, save it and change the ssh.key preference property to point to it
			if (keys.get(GENICHXMLRPCProxy.SSH_KEY_PRIVATE) != null) {
				String portalPrivateKey = (String)keys.get(GENICHXMLRPCProxy.SSH_KEY_PRIVATE);
				String keyFileName = "portal-" + (String)keys.get(GENICHXMLRPCProxy.SSH_KEY_ID) + "-key";
				File portalKeyFile = File.createTempFile(keyFileName, "");
				GUI.logger().info("Saving private key from the portal to " + portalKeyFile.getAbsolutePath());
				portalKeyFile.deleteOnExit();
				PrintWriter out = new PrintWriter(portalKeyFile.getAbsolutePath());
				GUI.getInstance().setPreference(GUI.PrefsEnum.SSH_KEY, portalKeyFile.getAbsolutePath());
				out.println(portalPrivateKey);
				out.close();
			}
		}

		Map<String, Object> userEntry = new HashMap<String, Object>();

		userEntry.put("login", "root");
		List<String> keys = new ArrayList<String>();
		keys.add(userKey);
		userEntry.put("keys", keys);
		users.add(userEntry);
		
		// any additional keys?
		keyPathStr = GUI.getInstance().getPreference(GUI.PrefsEnum.SSH_OTHER_PUBKEY);
		if (keyPathStr.startsWith("~/")) {
			keyPathStr = keyPathStr.replaceAll("~/", "/");
			keyPath = new File(System.getProperty("user.home"), keyPathStr);
		}
		else {
			keyPath = new File(keyPathStr);
		}
		String otherUserKey = getUserKeyFile(keyPath);

		if (otherUserKey != null) {
			if (GUI.getInstance().getPreference(GUI.PrefsEnum.SSH_OTHER_LOGIN).equals("root")) {
				keys.add(otherUserKey);
			} else {
				Map<String, Object> otherUserEntry = new HashMap<String, Object>();
				otherUserEntry.put("login", GUI.getInstance().getPreference(GUI.PrefsEnum.SSH_OTHER_LOGIN));
				otherUserEntry.put("keys", Collections.singletonList(otherUserKey));
				otherUserEntry.put("sudo", GUI.getInstance().getPreference(GUI.PrefsEnum.SSH_OTHER_SUDO));
				users.add(otherUserEntry);
			}
		}

		// submit the request
		return createSlice(sliceId, resReq, users);
	}

	@SuppressWarnings("unchecked")
	public boolean deleteSlice(String sliceId)  throws Exception {
		boolean res = false;
		setSSLIdentity(null, GUI.getInstance().getSelectedController());
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");

		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// delete sliver
			rr = (Map<String, Object>)client.execute(DELETE_SLICE, new Object[]{ sliceId, new Object[]{}});
		} catch (MalformedURLException e) {
			throw new Exception("Please check the controller URL " + GUI.getInstance().getSelectedController());
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController() + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());
		}

		if (rr == null)
                        throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception("Unable to delete slice: " + (String)rr.get(MSG_RET_FIELD));
		else
			res = (Boolean)rr.get(RET_RET_FIELD);

		return res;
	}

	@SuppressWarnings("unchecked")
	public String sliceStatus(String sliceId)  throws Exception {
		Validate.notNull(sliceId);

		String result = null;
		setSSLIdentity(null, GUI.getInstance().getSelectedController());
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");
		
		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// sliver status
			rr = (Map<String, Object>)client.execute(SLICE_STATUS, new Object[]{ sliceId, new Object[]{}});

		} catch (MalformedURLException e) {
			throw new Exception("Please check the controller URL " + GUI.getInstance().getSelectedController());
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController() + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());
		}

		if (rr == null)
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception("Unable to get slice status: " + rr.get(MSG_RET_FIELD));

		result = (String)rr.get(RET_RET_FIELD);

		return result;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Map<String, String>> getReservationStates(String sliceId, List<String> reservationIds)  throws Exception {
	    Validate.notNull(sliceId);
	    Validate.notNull(reservationIds);
	    Validate.notEmpty(reservationIds);

		setSSLIdentity(null, GUI.getInstance().getSelectedController());
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");

		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// sliver status
			rr = (Map<String, Object>)client.execute(GET_RESERVATION_STATES, new Object[]{ sliceId, reservationIds, new Object[]{}});

		} catch (MalformedURLException e) {
			throw new Exception("Please check the controller URL " + GUI.getInstance().getSelectedController());
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController() + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());
		}

		if (rr == null)
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception("Unable to get reservation states: " + rr.get(MSG_RET_FIELD));

		return (Map<String, Map<String, String>>) rr.get(RET_RET_FIELD);
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> getReservationSliceStitchInfo(String sliceId, List<String> reservationIds)  throws Exception {
	    Validate.notNull(sliceId);
	    Validate.notNull(reservationIds);
	    Validate.notEmpty(reservationIds);

		setSSLIdentity(null, GUI.getInstance().getSelectedController());
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");

		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// sliver status
			rr = (Map<String, Object>)client.execute(GET_RESERVATION_SLICE_STITCH_INFO, new Object[]{ sliceId, reservationIds, new Object[]{}});

		} catch (MalformedURLException e) {
			throw new Exception("Please check the controller URL " + GUI.getInstance().getSelectedController());
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController() + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());
		}

		if (rr == null)
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception("Unable to get reservation slice stitching info: " + rr.get(MSG_RET_FIELD));

		return (Map<String, Object>) rr.get(RET_RET_FIELD);
	}
	
	@SuppressWarnings("unchecked")
	public List<Map<String, String>> getSliverProperties(String sliceId, String reservationId)  throws Exception {
		Validate.notNull(sliceId);
		Validate.notNull(reservationId);

		setSSLIdentity(null, GUI.getInstance().getSelectedController());
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");

		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// sliver status
			rr = (Map<String, Object>)client.execute(GET_SLIVER_PROPERTIES, new Object[]{ sliceId, reservationId, new Object[]{}});

		} catch (MalformedURLException e) {
			throw new Exception("Please check the controller URL " + GUI.getInstance().getSelectedController());
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController() + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());
		}

		if (rr == null)
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception("Unable to get sliver properties: " + rr.get(MSG_RET_FIELD));

		Object[] tmpL = (Object[]) rr.get(RET_RET_FIELD);
		List<Map<String, String>> t1 = new ArrayList<Map<String, String>>();
		for(Object o: tmpL) {
			t1.add((Map<String, String>)o);
		}

		return t1;
	}
	
	@SuppressWarnings("unchecked")
	public String[] listMySlices() throws Exception {
		String[] result = null;
		setSSLIdentity(null, GUI.getInstance().getSelectedController());
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");

		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// sliver status
			rr = (Map<String, Object>)client.execute(LIST_SLICES, new Object[]{ new Object[]{}});
		} catch (MalformedURLException e) {
			throw new Exception("Please check the controller URL " + GUI.getInstance().getSelectedController());
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController() + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());
		}

		if (rr == null)
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception ("Unable to list active slices: " + rr.get(MSG_RET_FIELD));

		Object[] ll = (Object[])rr.get(RET_RET_FIELD);
		if (ll.length == 0)
			return new String[0];
		else {
			result = new String[ll.length];
			for (int i = 0; i < ll.length; i++)
				result[i] = (String)((Object[])rr.get(RET_RET_FIELD))[i];
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public Boolean modifySliverSSH(String sliceId, String resId, String user, boolean sudo, List<String> keys) throws Exception {
	    Validate.notNull(sliceId);
	    Validate.notNull(resId);
	    Validate.notNull(user);
	    Validate.notNull(keys);
	    Validate.notEmpty(keys);

	    Boolean result = null;
	    setSSLIdentity(null, GUI.getInstance().getSelectedController());

	    if (!isSSLIdentitySet())
	        throw new Exception("SSL Identity not set, unable to proceed");

	    Map<String, Object> rr = null;
	    try {
	        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
	        config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
	        XmlRpcClient client = new XmlRpcClient();
	        client.setConfig(config);

	        // set this transport factory for host-specific SSLContexts to work
	        XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
	        client.setTransportFactory(f);

	        List<Map<String, ?>> keyList = new ArrayList<>();
	        Map<String, Object> userEntry = new HashMap<>();
	        userEntry.put("login", user);
	        userEntry.put("keys", keys);
	        if (sudo)
	            userEntry.put("sudo", "yes");
	        else
	            userEntry.put("sudo", "no");
	        keyList.add(userEntry);
	        // modify sliver String slice_urn, String sliver_guid, Object[] credentials, 
            // String modifySubcommand, List<Map<String, ?>> modifyProperties
	        rr = (Map<String, Object>)client.execute(MODIFY_SLIVER, new Object[]{ sliceId, resId, new Object[]{}, "ssh", keyList});
	    } catch (MalformedURLException e) {
	        throw new Exception("Please check the controller URL " + GUI.getInstance().getSelectedController());
	    } catch (XmlRpcException e) {
	        throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController() + " due to " + e);
	    } catch (Exception e) {
	        throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());
	    }

	    if (rr == null)
	        throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());

	    if ((Boolean)rr.get(ERR_RET_FIELD))
	        throw new Exception("Unable to insert SSH key into sliver: " + (String)rr.get(MSG_RET_FIELD));

	    result = (Boolean)rr.get(RET_RET_FIELD);
	    return result;
	}
	
	@SuppressWarnings("unchecked")
	public String modifySlice(String sliceId, String modReq) throws Exception {
	    Validate.notNull(sliceId);
	    Validate.notNull(modReq);

		String result = null;
		setSSLIdentity(null, GUI.getInstance().getSelectedController());
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");
		
		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// modify slice
			rr = (Map<String, Object>)client.execute(MODIFY_SLICE, new Object[]{ sliceId, new Object[]{}, modReq});
		} catch (MalformedURLException e) {
			throw new Exception("Please check the controller URL " + GUI.getInstance().getSelectedController());
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController() + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());
		}

		if (rr == null)
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception("Unable to modify slice: " + (String)rr.get(MSG_RET_FIELD));

		result = (String)rr.get(RET_RET_FIELD);
		return result;
	}

	@SuppressWarnings("unchecked")
	public String listResources() throws Exception {

		String result = null;
		setSSLIdentity(null, GUI.getInstance().getSelectedController());
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");
		
		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// modify slice
			rr = (Map<String, Object>)client.execute(LIST_RESOURCES, new Object[]{ new Object[]{}, new HashMap<String, String>()});
		} catch (MalformedURLException e) {
			throw new Exception("Please check the controller URL " + GUI.getInstance().getSelectedController());
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController() + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());
		}

		if (rr == null)
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception("Unable to list resources: " + (String)rr.get(MSG_RET_FIELD));

		result = (String)rr.get(RET_RET_FIELD);
		return result;
	}

	@SuppressWarnings("unchecked")
	public boolean permitSliceStitch(String sliceId, String reservationId, String secret) throws Exception {
	    Validate.notNull(sliceId);
	    Validate.notNull(reservationId);
	    Validate.notNull(secret);
		
		Boolean result = null;
		setSSLIdentity(null, GUI.getInstance().getSelectedController());
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");
		
		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// modify slice
			rr = (Map<String, Object>)client.execute(PERMIT_SLICE_STITCH, new Object[]{ sliceId, reservationId, secret, new Object[]{}});
		} catch (MalformedURLException e) {
			throw new Exception("Please check the controller URL " + GUI.getInstance().getSelectedController());
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController() + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());
		}

		if (rr == null)
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception("Unable to permit slice stitching: " + (String)rr.get(MSG_RET_FIELD));

		result = (Boolean)rr.get(RET_RET_FIELD);
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public boolean revokeSliceStitch(String sliceId, String reservationId) throws Exception {
	    Validate.notNull(sliceId);
	    Validate.notNull(reservationId);
		
		Boolean result = null;
		setSSLIdentity(null, GUI.getInstance().getSelectedController());
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");
		
		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// modify slice
			rr = (Map<String, Object>)client.execute(REVOKE_SLICE_STITCH, new Object[]{ sliceId, reservationId, new Object[]{}});
		} catch (MalformedURLException e) {
			throw new Exception("Please check the controller URL " + GUI.getInstance().getSelectedController());
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController() + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());
		}

		if (rr == null)
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception("Unable to revoke slice stitch permission: " + (String)rr.get(MSG_RET_FIELD));

		result = (Boolean)rr.get(RET_RET_FIELD);
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public boolean performSliceStitch(String fromSliceId, String fromReservationId, 
			String toSliceId, String toReservationId, String secret, Properties p) throws Exception {
	    Validate.notNull(fromSliceId);
	    Validate.notNull(fromReservationId);
	    Validate.notNull(toSliceId);
	    Validate.notNull(toReservationId);
	    Validate.notNull(secret);
	    Validate.notNull(p);
	    
		Boolean result = null;
		setSSLIdentity(null, GUI.getInstance().getSelectedController());
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");
		
		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// modify slice
			rr = (Map<String, Object>)client.execute(PERFORM_SLICE_STITCH, new Object[]{ fromSliceId, fromReservationId, toSliceId, toReservationId, secret, p,  new Object[]{}});
		} catch (MalformedURLException e) {
			throw new Exception("Please check the controller URL " + GUI.getInstance().getSelectedController());
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController() + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());
		}

		if (rr == null)
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception("Unable to perform slice stitch: " + (String)rr.get(MSG_RET_FIELD));

		result = (Boolean)rr.get(RET_RET_FIELD);
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public boolean undoSliceStitch(String fromSliceId, String fromReservationId, String toSliceId, String toReservationId) throws Exception {
	    Validate.notNull(fromSliceId);
	    Validate.notNull(fromReservationId);
	    Validate.notNull(toSliceId);
	    Validate.notNull(toReservationId);
		
		Boolean result = null;
		setSSLIdentity(null, GUI.getInstance().getSelectedController());
		
		if (!isSSLIdentitySet())
			throw new Exception("SSL Identity not set, unable to proceed");
		
		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// modify slice
			rr = (Map<String, Object>)client.execute(UNDO_SLICE_STITCH, new Object[]{ fromSliceId, fromReservationId, toSliceId, toReservationId, new Object[]{}});
		} catch (MalformedURLException e) {
			throw new Exception("Please check the controller URL " + GUI.getInstance().getSelectedController());
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController() + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());
		}

		if (rr == null)
			throw new Exception("Unable to contact controller " + GUI.getInstance().getSelectedController());

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception("Unable to undo slice stitch: " + (String)rr.get(MSG_RET_FIELD));

		result = (Boolean)rr.get(RET_RET_FIELD);
		return result;
	}
	/**
	 * Try to get a public key file, first DSA, then RSA
	 * @return
	 */
	private String getAnyUserPubKey() {
		Properties p = System.getProperties();

		String keyFilePathStr = "" + p.getProperty("user.home") + p.getProperty("file.separator") + ".ssh" +
		p.getProperty("file.separator") + SSH_DSA_PUBKEY_FILE;
		File keyFilePath = new File(keyFilePathStr);

		String userKey = getUserKeyFile(keyFilePath);
		if (userKey == null) {
			keyFilePathStr = "" + p.getProperty("user.home") + p.getProperty("file.separator") + ".ssh" + 
			p.getProperty("file.separator") + SSH_RSA_PUBKEY_FILE;
			keyFilePath = new File(keyFilePathStr);
			userKey = getUserKeyFile(keyFilePath);
			if (userKey == null) {
				KMessageDialog md = new KMessageDialog(GUI.getInstance().getFrame(), "No user ssh keys found", true);
				md.setLocationRelativeTo(GUI.getInstance().getFrame());
				md.setMessage("Unable to locate ssh public keys, you will not be able to login to the resources!");
				md.setVisible(true);
				return null;
			}
		}
		return userKey;
	}

	private String getUserKeyFile(File path) {
		try {
			FileInputStream is = new FileInputStream(path);
			BufferedReader bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));

			StringBuilder sb = new StringBuilder();
			String line = null;
			while((line = bin.readLine()) != null) {
				sb.append(line);
				// re-add line separator
				sb.append(System.getProperty("line.separator"));
			}

			bin.close();

			return sb.toString();

		} catch (IOException e) {
			return null;
		}
	}


	/**
	 * Test harness
	 * @param args
	 */
	public static void main(String[] args) {
		OrcaSMXMLRPCProxy p = OrcaSMXMLRPCProxy.getInstance();

		if (args.length != 1) {
			System.err.println("You must specify the request filename");
			System.exit(1);
		}

		StringBuilder sb = null;
		try {
			BufferedReader bin = null;
			File f = new File(args[0]);
			FileInputStream is = new FileInputStream(f);
			bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));

			sb = new StringBuilder();
			String line = null;
			while((line = bin.readLine()) != null) {
				sb.append(line);
				// re-add line separator
				sb.append(System.getProperty("line.separator"));
			}

			bin.close();
		} catch (Exception e) {
			System.err.println("Error "  + e + " encountered while readling file " + args[0]);
			System.exit(1);
		} finally {
			;
		}

		try {
			System.out.println("Placing request against " + GUI.getInstance().getSelectedController());
			String sliceId = UUID.randomUUID().toString();
			System.out.println("Creating slice " + sliceId);
			String result = p.createSlice(sliceId, sb.toString());
			System.out.println("Result of create slice: " + result);

			System.out.println("Sleeping for 60sec");
			Thread.sleep(60000);

			System.out.println("Requesting sliver status");
			System.out.println("Status: " + p.sliceStatus(sliceId));

			//			System.out.println("Deleting slice " + sliceId);
			//			System.out.println("Result of delete slice: " + p.deleteSliver(sliceId));
		} catch (Exception e) {
			System.err.println("An exception has occurred in creating slice " + e);
		}

	}
}
