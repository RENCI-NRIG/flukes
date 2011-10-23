package orca.flukes.xmlrpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateException;

import orca.flukes.GUI;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import com.hyperrealm.kiwi.ui.dialog.ExceptionDialog;

/**
 * XMLRPC Proxy singleton for ORCA Actor Registry (partial)
 * @author ibaldin
 *
 */
public class RegistryXMLRPCProxy {
	private static final String GET_AMS = "registryService.getAMs";
	
	private byte[] registryCertDigest;
	
	private RegistryXMLRPCProxy() {
		// singleton
        // get registry cert fingerprint
    	String[] fingerPrintBytes = GUI.getInstance().getPreference(GUI.PrefsEnum.ORCA_REGISTRY_CERT_FINGERPRINT).split(":");
    	
    	registryCertDigest = new byte[16];
    	
    	for (int i = 0; i < 16; i++ )
    		registryCertDigest[i] = (byte)(Integer.parseInt(fingerPrintBytes[i], 16) & 0xFF);
        
    	// Create a trust manager that does not validate certificate chains
        // so we can speak to the registry
    	TrustManager[] trustAllCerts = new TrustManager[] {
    			new X509TrustManager() {
    				public X509Certificate[] getAcceptedIssuers() {
    					return null;
    				}

    				public void checkClientTrusted(X509Certificate[] certs, String authType) {
    					// Trust always
    				}

    				public void checkServerTrusted(X509Certificate[] certs, String authType) {
    					// Trust always
    					MessageDigest md = null;
    					try {
    						md = MessageDigest.getInstance("MD5");

    						if (certs.length == 0) 
    							throw new CertificateException();

    						byte[] certDigest = md.digest(certs[0].getEncoded());
    						if (!Arrays.equals(certDigest, registryCertDigest)) {
    							throw new CertificateException();
    						}
    					} catch (NoSuchAlgorithmException e) {
    						;
    					} catch (Exception e) {
    						ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
    						ed.setLocationRelativeTo(GUI.getInstance().getFrame());
    						ed.setException("Exception encountered while contacting ORCA registry " + 
    								GUI.getInstance().getPreference(GUI.PrefsEnum.ORCA_REGISTRY), e);
    						ed.setVisible(true);
    					}
    				}
    			}
    	};
     
        // Install the all-trusting trust manager
        try {
        	SSLContext sc = SSLContext.getInstance("SSL");
        	// Create empty HostnameVerifier
        	HostnameVerifier hv = new HostnameVerifier() {
        		public boolean verify(String arg0, SSLSession arg1) {
        			return true;
        		}
        	};

        	sc.init(null, trustAllCerts, new java.security.SecureRandom());
        	HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        	HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (NoSuchAlgorithmException e1) {

        } catch (KeyManagementException e2) {

        }
	}
	
	private static RegistryXMLRPCProxy instance = new RegistryXMLRPCProxy();
	
	public static RegistryXMLRPCProxy getInstance() {
		return instance;
	}
	
	/**
	 * Get data on all known AMs
	 * @param verbose
	 * @return
	 */
	public Map<String, Map<String, String>> getAMs(boolean verbose) throws Exception {
        // call the actor registry
        Map<String, Map<String, String>> amData = null;
        try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(GUI.getInstance().getPreference(GUI.PrefsEnum.ORCA_REGISTRY)));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			
			// get verbose list of the AMs
			amData = (Map<String, Map<String, String>>)client.execute(GET_AMS, new Object[]{!verbose});
        } catch (MalformedURLException e) {
        	throw new Exception("Please check the registry URL " + GUI.getInstance().getPreference(GUI.PrefsEnum.ORCA_REGISTRY));
        } catch (XmlRpcException e) {
        	throw new Exception("Unable to contact registry " + GUI.getInstance().getPreference(GUI.PrefsEnum.ORCA_REGISTRY) + " due to " + e);
        }
		return amData;
	}
}
