package orca.flukes.xmlrpc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import orca.flukes.GUI;
import orca.util.ssl.ContextualSSLProtocolSocketFactory;
import orca.util.ssl.MultiKeyManager;
import orca.util.ssl.MultiKeySSLContextFactory;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;

import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

public class OrcaSMXMLRPCProxy {
	private static final String RET_RET_FIELD = "ret";
	private static final String MSG_RET_FIELD = "msg";
	private static final String ERR_RET_FIELD = "err";
	private static final String GET_VERSION = "orca.getVersion";
	private static final String SLICE_STATUS = "orca.sliceStatus";
	private static final String CREATE_SLICE = "orca.createSlice";
	private static final String DELETE_SLICE = "orca.deleteSlice";
	private static final String MODIFY_SLICE = "orca.modifySlice";
	private static final String RENEW_SLICE = "orca.renewSlice";
	private static final String LIST_SLICES = "orca.listSlices";
	private static final String LIST_RESOURCES = "orca.listResources";
	private static final String SSH_DSA_PUBKEY_FILE = "id_dsa.pub";
	private static final String SSH_RSA_PUBKEY_FILE = "id_rsa.pub";
	
	private MultiKeyManager mkm = null;
	private boolean sslIdentitySet = false;
	
	OrcaSMXMLRPCProxy() {
		;
	}
	
	private static OrcaSMXMLRPCProxy instance = new OrcaSMXMLRPCProxy();
	
	public static OrcaSMXMLRPCProxy getInstance() {
		return instance;
	}
	
	TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					// return 0 size array, not null, per spec
					return null;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
					// Trust always
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
					// Trust always
					// FIXME: should check the cert of controller we're talking to
				}

			}
	};
	
	public void resetSSLIdentity() {
		sslIdentitySet = false;
	}
	
	/**
	 * Set the identity for the communications to the XMLRPC controller. Eventually
	 * we may talk to several controller with different identities. For now only
	 * one is configured.
	 */
	private void setSSLIdentity() throws Exception {
		
		if (sslIdentitySet)
			return;
		
		try {
			// create multikeymanager
			mkm = new MultiKeyManager();
			URL ctrlrUrl = new URL(GUI.getInstance().getSelectedController());

			// register a new protocol
			ContextualSSLProtocolSocketFactory regSslFact = 
				new ContextualSSLProtocolSocketFactory();

			// add this multikey context factory for the controller host/port
			regSslFact.addHostContextFactory(new MultiKeySSLContextFactory(mkm, trustAllCerts), 
					ctrlrUrl.getHost(), ctrlrUrl.getPort());

			// load keystore and get the right cert from it
			String keyAlias = GUI.getInstance().getKeystoreAlias();
			String keyPassword = GUI.getInstance().getKeystorePassword();

			String keyStorePath = GUI.getInstance().getPreference(GUI.PrefsEnum.USER_KEYSTORE);
			Properties p = System.getProperties();
			keyStorePath = keyStorePath.replaceAll("~", p.getProperty("user.home"));
			
			FileInputStream fis = new FileInputStream(keyStorePath);

			KeyStore ks = KeyStore.getInstance("jks");

			ks.load(fis, keyPassword.toCharArray());
			fis.close();

			// check that the spelling of key alias is proper
			Enumeration<String> as = ks.aliases();
			while (as.hasMoreElements()) {
				String a = as.nextElement();
				if (keyAlias.toLowerCase().equals(a.toLowerCase())) {
					keyAlias = a;
					break;
				}
			}
			
			// alias has to exist and have a key and cert present
			if (!ks.containsAlias(keyAlias)) {
				throw new Exception("Alias " + keyAlias + " does not exist in keystore " + keyStorePath + ".");
			}
			
			if (ks.getKey(keyAlias, keyPassword.toCharArray()) == null)
				throw new Exception("Key with alias " + keyAlias + " does not exist in keystore " + keyStorePath + ".");
			
			if (ks.getCertificate(keyAlias) == null) {
				throw new Exception("Certificate with alias " + keyAlias + " does not exist in keystore " + keyStorePath + ".");
			}
			
			// add the identity into it
			mkm.addPrivateKey(keyAlias, 
					(PrivateKey)ks.getKey(keyAlias, keyPassword.toCharArray()), 
					ks.getCertificate(keyAlias));

			// before we do SSL to this controller, set our identity
			mkm.setCurrentGuid(keyAlias);

	    	// register the protocol (Note: All xmlrpc clients must use XmlRpcCommonsTransportFactory
	    	// for this to work). See ContextualSSLProtocolSocketFactory.
			Protocol reghhttps = new Protocol("https", (ProtocolSocketFactory)regSslFact, 443); 
			Protocol.registerProtocol("https", reghhttps);
			
			sslIdentitySet = true;
		} catch (Exception e) {
			GUI.getInstance().resetKeystoreAliasAndPassword();
			throw new Exception("Unable to load user private key and certificate from the keystore: " + e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> getVersion() throws Exception {
        Map<String, Object> versionMap = null;
    	setSSLIdentity();
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
        	throw new Exception("Please check the SM URL " + GUI.getInstance().getSelectedController());
        } catch (XmlRpcException e) {
        	throw new Exception("Unable to contact SM " + GUI.getInstance().getSelectedController() + " due to " + e);
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
		assert(sliceId != null);
		assert(resReq != null);
		
		String result = null;
    	setSSLIdentity();
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			
            // set this transport factory for host-specific SSLContexts to work
            XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);
			
			// create sliver
			Map<String, Object> rr = (Map<String, Object>)client.execute(CREATE_SLICE, new Object[]{ sliceId, new Object[]{}, resReq, users});
			if ((Boolean)rr.get(ERR_RET_FIELD))
				throw new Exception("Unable to create slice: " + (String)rr.get(MSG_RET_FIELD));
			result = (String)rr.get(RET_RET_FIELD);
        } catch (MalformedURLException e) {
        	throw new Exception("Please check the SM URL " + GUI.getInstance().getSelectedController());
        } catch (XmlRpcException e) {
        	throw new Exception("Unable to contact SM " + GUI.getInstance().getSelectedController() + " due to " + e);
        }
		
		return result;
	}
	
	/** submit an ndl request to create a slice, using explicitly specified users array
	 * 
	 * @param sliceId
	 * @param resReq
	 * @param users
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String renewSlice(String sliceId, Date newDate) throws Exception {
		assert(sliceId != null);
		
		String result = null;
    	setSSLIdentity();
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
			Map<String, Object> rr = (Map<String, Object>)client.execute(RENEW_SLICE, new Object[]{ sliceId, new Object[]{}, endDateString});
			if ((Boolean)rr.get(ERR_RET_FIELD))
				throw new Exception("Unable to renew slice: " + (String)rr.get(MSG_RET_FIELD));
			result = (String)rr.get(RET_RET_FIELD);
        } catch (MalformedURLException e) {
        	throw new Exception("Please check the SM URL " + GUI.getInstance().getSelectedController());
        } catch (XmlRpcException e) {
        	throw new Exception("Unable to contact SM " + GUI.getInstance().getSelectedController() + " due to " + e);
        }
		
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
    	setSSLIdentity();
    	
		// collect user credentials from $HOME/.ssh
		Properties p = System.getProperties();
		
		// create an array
		List<Map<String, ?>> users = new ArrayList<Map<String, ?>>();
		String keyPath = GUI.getInstance().getPreference(GUI.PrefsEnum.SSH_PUBKEY);
		keyPath = keyPath.replaceAll("~", p.getProperty("user.home"));
		
		String userKey = getUserKeyFile(keyPath);
		
		if (userKey == null) 
			throw new Exception("Unable to load user public ssh key " + keyPath);
		
		Map<String, Object> userEntry = new HashMap<String, Object>();
		String userName = System.getProperties().getProperty("user.name");
		userEntry.put("urn", (userName == null ? "authorized_user" : userName));
		List<String> keys = new ArrayList<String>();
		keys.add(userKey);
		userEntry.put("keys", keys);
		users.add(userEntry);

		// submit the request
		return createSlice(sliceId, resReq, users);
	}
	
	@SuppressWarnings("unchecked")
	public boolean deleteSlice(String sliceId)  throws Exception {
		boolean res = false;
    	setSSLIdentity();
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			
            // set this transport factory for host-specific SSLContexts to work
            XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);
			
			// delete sliver
			Map<String, Object> rr = (Map<String, Object>)client.execute(DELETE_SLICE, new Object[]{ sliceId, new Object[]{}});
			if ((Boolean)rr.get(ERR_RET_FIELD))
				throw new Exception("Unable to delete slice: " + (String)rr.get(MSG_RET_FIELD));
			else
				res = (Boolean)rr.get(RET_RET_FIELD);
        } catch (MalformedURLException e) {
        	throw new Exception("Please check the SM URL " + GUI.getInstance().getSelectedController());
        } catch (XmlRpcException e) {
        	throw new Exception("Unable to contact SM " + GUI.getInstance().getSelectedController() + " due to " + e);
        }
        
        return res;
	}
	
	@SuppressWarnings("unchecked")
	public String sliceStatus(String sliceId)  throws Exception {
		assert(sliceId != null);
		
		String result = null;
    	setSSLIdentity();
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			
            // set this transport factory for host-specific SSLContexts to work
            XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);
			
			// sliver status
			Map<String, Object> rr = (Map<String, Object>)client.execute(SLICE_STATUS, new Object[]{ sliceId, new Object[]{}});
			if ((Boolean)rr.get(ERR_RET_FIELD))
				throw new Exception("Unable to get sliver status: " + rr.get(MSG_RET_FIELD));
			result = (String)rr.get(RET_RET_FIELD);
			
        } catch (MalformedURLException e) {
        	throw new Exception("Please check the SM URL " + GUI.getInstance().getSelectedController());
        } catch (XmlRpcException e) {
        	throw new Exception("Unable to contact SM " + GUI.getInstance().getSelectedController() + " due to " + e);
        }
        
        return result;
	}
	
	@SuppressWarnings("unchecked")
	public String[] listMySlices() throws Exception {
		String[] result = null;
		setSSLIdentity();
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			
            // set this transport factory for host-specific SSLContexts to work
            XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);
			
			// sliver status
			Map<String, Object> rr = (Map<String, Object>)client.execute(LIST_SLICES, new Object[]{ new Object[]{}});
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
			
		} catch (MalformedURLException e) {
        	throw new Exception("Please check the SM URL " + GUI.getInstance().getSelectedController());
        } catch (XmlRpcException e) {
        	throw new Exception("Unable to contact SM " + GUI.getInstance().getSelectedController() + " due to " + e);
        }
        return result;
	}
	
	@SuppressWarnings("unchecked")
	public String modifySlice(String sliceId, String modReq) throws Exception {
		assert(sliceId != null);
		assert(modReq != null);
		
		String result = null;
    	setSSLIdentity();
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			
            // set this transport factory for host-specific SSLContexts to work
            XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);
			
			// modify slice
			Map<String, Object> rr = (Map<String, Object>)client.execute(MODIFY_SLICE, new Object[]{ sliceId, new Object[]{}, modReq});
			if ((Boolean)rr.get(ERR_RET_FIELD))
				throw new Exception("Unable to modify slice: " + (String)rr.get(MSG_RET_FIELD));
			result = (String)rr.get(RET_RET_FIELD);
        } catch (MalformedURLException e) {
        	throw new Exception("Please check the SM URL " + GUI.getInstance().getSelectedController());
        } catch (XmlRpcException e) {
        	throw new Exception("Unable to contact SM " + GUI.getInstance().getSelectedController() + " due to " + e);
        }
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public String listResources() throws Exception {
		
		String result = null;
    	setSSLIdentity();
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getSelectedController()));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			
            // set this transport factory for host-specific SSLContexts to work
            XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);
			
			// modify slice
			Map<String, Object> rr = (Map<String, Object>)client.execute(LIST_RESOURCES, new Object[]{ new Object[]{}, new HashMap<String, String>()});
			if ((Boolean)rr.get(ERR_RET_FIELD))
				throw new Exception("Unable to list resources: " + (String)rr.get(MSG_RET_FIELD));
			result = (String)rr.get(RET_RET_FIELD);
        } catch (MalformedURLException e) {
        	throw new Exception("Please check the SM URL " + GUI.getInstance().getSelectedController());
        } catch (XmlRpcException e) {
        	throw new Exception("Unable to contact SM " + GUI.getInstance().getSelectedController() + " due to " + e);
        }
		
		return result;
	}
	
	/**
	 * Try to get a public key file, first DSA, then RSA
	 * @return
	 */
	private String getAnyUserPubKey() {
		Properties p = System.getProperties();
		
		String keyFilePath = "" + p.getProperty("user.home") + p.getProperty("file.separator") + ".ssh" +
		p.getProperty("file.separator") + SSH_DSA_PUBKEY_FILE;

		String userKey = getUserKeyFile(keyFilePath);
		if (userKey == null) {
			keyFilePath = "" + p.getProperty("user.home") + p.getProperty("file.separator") + ".ssh" + 
			p.getProperty("file.separator") + SSH_RSA_PUBKEY_FILE;
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

	private String getUserKeyFile(String path) {
		try {
			File prefs = new File(path);
			FileInputStream is = new FileInputStream(prefs);
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