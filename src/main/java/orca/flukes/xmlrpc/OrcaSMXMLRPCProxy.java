package orca.flukes.xmlrpc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.security.auth.login.CredentialException;

import orca.flukes.GUI;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

public class OrcaSMXMLRPCProxy {
	private static final String GET_VERSION = "geni.GetVersion";
	private static final String SLIVER_STATUS = "geni.SliverStatus";
	private static final String CREATE_SLIVER = "geni.CreateSliver";
	private static final String DELETE_SLIVER = "geni.DeleteSliver";
	private static final String SSH_DSA_PUBKEY_FILE = "id_dsa.pub";
	private static final String SSH_RSA_PUBKEY_FILE = "id_rsa.pub";
	
	OrcaSMXMLRPCProxy() {
		;
	}
	
	private static OrcaSMXMLRPCProxy instance = new OrcaSMXMLRPCProxy();
	
	public static OrcaSMXMLRPCProxy getInstance() {
		return instance;
	}
	
	public Map<String, Object> getVersion() throws Exception {
        Map<String, Object> versionMap = null;
        try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getPreference(GUI.PrefsEnum.ORCA_XMLRPC_CONTROLLER)));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			
			// get verbose list of the AMs
			versionMap = (Map<String, Object>)client.execute(GET_VERSION, new Object[]{});
        } catch (MalformedURLException e) {
        	throw new Exception("Please check the SM URL " + GUI.getInstance().getPreference(GUI.PrefsEnum.ORCA_XMLRPC_CONTROLLER));
        } catch (XmlRpcException e) {
        	throw new Exception("Unable to contact SM " + GUI.getInstance().getPreference(GUI.PrefsEnum.ORCA_XMLRPC_CONTROLLER) + " due to " + e);
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
	public String createSliver(String sliceId, String resReq, List<Map<String, ?>> users) throws Exception {
		assert(sliceId != null);
		assert(resReq != null);
		
		String result = null;
		
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getPreference(GUI.PrefsEnum.ORCA_XMLRPC_CONTROLLER)));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			
			// create sliver
			result = (String)client.execute(CREATE_SLIVER, new Object[]{ sliceId, new Object[]{}, resReq, users});
        } catch (MalformedURLException e) {
        	throw new Exception("Please check the SM URL " + GUI.getInstance().getPreference(GUI.PrefsEnum.ORCA_XMLRPC_CONTROLLER));
        } catch (XmlRpcException e) {
        	throw new Exception("Unable to contact SM " + GUI.getInstance().getPreference(GUI.PrefsEnum.ORCA_XMLRPC_CONTROLLER) + " due to " + e);
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
	public String createSliver(String sliceId, String resReq) throws Exception {
		
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
		return createSliver(sliceId, resReq, users);
	}
	
	public boolean deleteSliver(String sliceId)  throws Exception {
		boolean res = false;
		
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getPreference(GUI.PrefsEnum.ORCA_XMLRPC_CONTROLLER)));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			
			// delete sliver
			res = (Boolean)client.execute(DELETE_SLIVER, new Object[]{ sliceId, new Object[]{}});
        } catch (MalformedURLException e) {
        	throw new Exception("Please check the SM URL " + GUI.getInstance().getPreference(GUI.PrefsEnum.ORCA_XMLRPC_CONTROLLER));
        } catch (XmlRpcException e) {
        	throw new Exception("Unable to contact SM " + GUI.getInstance().getPreference(GUI.PrefsEnum.ORCA_XMLRPC_CONTROLLER) + " due to " + e);
        }
        
        return res;
	}
	
	public String sliverStatus(String sliceId)  throws Exception {
		assert(sliceId != null);
		
		String result = null;
		
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getPreference(GUI.PrefsEnum.ORCA_XMLRPC_CONTROLLER)));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			
			// sliver status
			result = (String)client.execute(SLIVER_STATUS, new Object[]{ sliceId, new Object[]{}});
        } catch (MalformedURLException e) {
        	throw new Exception("Please check the SM URL " + GUI.getInstance().getPreference(GUI.PrefsEnum.ORCA_XMLRPC_CONTROLLER));
        } catch (XmlRpcException e) {
        	throw new Exception("Unable to contact SM " + GUI.getInstance().getPreference(GUI.PrefsEnum.ORCA_XMLRPC_CONTROLLER) + " due to " + e);
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
			System.out.println("Placing request against " + GUI.getInstance().getPreference(GUI.PrefsEnum.ORCA_XMLRPC_CONTROLLER));
			String sliceId = UUID.randomUUID().toString();
			System.out.println("Creating slice " + sliceId);
			String result = p.createSliver(sliceId, sb.toString());
			System.out.println("Result of create slice: " + result);
			
			System.out.println("Sleeping for 60sec");
			Thread.sleep(60000);
			
			System.out.println("Requesting sliver status");
			System.out.println("Status: " + p.sliverStatus(sliceId));
			
//			System.out.println("Deleting slice " + sliceId);
//			System.out.println("Result of delete slice: " + p.deleteSliver(sliceId));
		} catch (Exception e) {
			System.err.println("An exception has occurred in creating slice " + e);
		}

	}
}
