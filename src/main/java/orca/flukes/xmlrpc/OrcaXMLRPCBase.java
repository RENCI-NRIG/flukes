package orca.flukes.xmlrpc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PrivilegedAction;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import orca.flukes.GUI;
import orca.util.ssl.ContextualSSLProtocolSocketFactory;
import orca.util.ssl.MultiKeyManager;
import orca.util.ssl.MultiKeySSLContextFactory;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;

public class OrcaXMLRPCBase {
	private static final int HTTPS_PORT = 443;
	private static MultiKeyManager mkm = null;
	private static ContextualSSLProtocolSocketFactory regSslFact = null;
	boolean sslIdentitySet = false;
	// alternative names set on the cert that is in use. Only valid when identity is set
	Collection<List<?>> altNames = null;

	static {
		mkm = new MultiKeyManager();
		regSslFact = new ContextualSSLProtocolSocketFactory();
		
		// register the protocol (Note: All xmlrpc clients must use XmlRpcCommonsTransportFactory
		// for this to work). See ContextualSSLProtocolSocketFactory.
		
		Protocol reghhttps = new Protocol("https", (ProtocolSocketFactory)regSslFact, HTTPS_PORT); 
		Protocol.registerProtocol("https", reghhttps);
	}
	
	TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					// return 0 size array, not null, per spec
					return new X509Certificate[0];
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
					// Trust always
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateExpiredException, CertificateNotYetValidException {
					// Trust always, unless expired
					// FIXME: should check the cert of controller we're talking to
					for(X509Certificate c: certs) {
						c.checkValidity();	
					}
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
	protected void setSSLIdentity(final String keyPass, String apiURL) throws Exception {
		
		if (sslIdentitySet)
			return;

		try {
			URL ctrlrUrl = new URL(apiURL);

			KeyStore ks = null;
			File keyStorePath = loadUserFile(GUI.getInstance().getPreference(GUI.PrefsEnum.USER_KEYSTORE));
			File certFilePath = loadUserFile(GUI.getInstance().getPreference(GUI.PrefsEnum.USER_CERTFILE));
			File certKeyFilePath = loadUserFile(GUI.getInstance().getPreference(GUI.PrefsEnum.USER_CERTKEYFILE));

			String keyAlias = null;
			String keyPassword = keyPass;
			if (keyStorePath.exists()) {
				// load keystore and get the right cert from it
				keyAlias = GUI.getInstance().getKeystoreAlias();
				if (keyPassword == null) 
					keyPassword = GUI.getInstance().getKeystorePassword();
				FileInputStream jksIS = new FileInputStream(keyStorePath);
				ks = loadJKSData(jksIS, keyAlias, keyPassword);
				
				jksIS.close();
			}
			else if (certFilePath.exists() && certKeyFilePath.exists()) {
				FileInputStream certIS = new FileInputStream(certFilePath);
				FileInputStream keyIS = new FileInputStream(certKeyFilePath);
				keyAlias = "x509convert";
				if (keyPassword == null) {
					keyPassword = GUI.getInstance().getKeystorePasswordOnly();
				}
				ks = loadX509Data(certIS, keyIS, keyAlias, keyPassword);
				certIS.close();
				keyIS.close();
			}

			if (ks == null)
				throw new Exception("Was unable to find either: " + keyStorePath.getCanonicalPath() +
						" or the pair of: " + certFilePath.getCanonicalPath() +
						" and " + certKeyFilePath.getCanonicalPath() + " as specified.");

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

			if (ks.getCertificate(keyAlias).getType().equals("X.509")) {
				X509Certificate x509Cert = (X509Certificate)ks.getCertificate(keyAlias);
				altNames = x509Cert.getSubjectAlternativeNames();
				try {
					x509Cert.checkValidity();
				} catch (Exception e) {
					throw new Exception("Certificate with alias " + keyAlias + " is not yet valid or has expired.");
				}
			}

			// add the identity into it
			mkm.addPrivateKey(keyAlias, 
					(PrivateKey)ks.getKey(keyAlias, keyPassword.toCharArray()), 
					ks.getCertificateChain(keyAlias));

			// before we do SSL to this controller, set our identity
			mkm.setCurrentGuid(keyAlias);

			// add this multikey context factory for the controller host/port
			int port = ctrlrUrl.getPort();
			if (port <= 0)
				port = HTTPS_PORT;
			regSslFact.addHostContextFactory(new MultiKeySSLContextFactory(mkm, trustAllCerts), 
					ctrlrUrl.getHost(), port);

			sslIdentitySet = true;
			
		} catch (Exception e) {
			GUI.getInstance().resetKeystoreAliasAndPassword();
			e.printStackTrace();

			throw new Exception("Unable to load user private key and certificate from the keystore: " + e);
		}
	}

	private File loadUserFile(String pathStr) {
		File f;

		if (pathStr.startsWith("~/")) {
			pathStr = pathStr.replaceAll("~/", "/");
			f = new File(System.getProperty("user.home"), pathStr);
		}
		else {
			f = new File(pathStr);
		}

		return f;
	}

	private KeyStore loadJKSData(FileInputStream jksIS, String keyAlias, String keyPassword)
			throws Exception {

		KeyStore ks = KeyStore.getInstance("jks");
		ks.load(jksIS, keyPassword.toCharArray());

		return ks;
	}

	private KeyStore loadX509Data(FileInputStream certIS, FileInputStream keyIS, String keyAlias,
			String keyPassword) throws Exception {

		AccessController.doPrivileged(new PrivilegedAction<Void>() {
			public Void run() {
				if (Security.getProvider("BC") == null) {
					Security.addProvider(new BouncyCastleProvider());
				}
				return null;
			}
		});

		JcaPEMKeyConverter keyConverter =
				new JcaPEMKeyConverter().setProvider("BC");
		JcaX509CertificateConverter certConverter =
				new JcaX509CertificateConverter().setProvider("BC");

		Object object;

		PEMParser pemParser = new PEMParser(new BufferedReader(new InputStreamReader(keyIS, "UTF-8")));

		PrivateKey privKey = null;

		while ((object = pemParser.readObject()) != null) {
			if (object instanceof PrivateKeyInfo) {
				PrivateKeyInfo pki = (PrivateKeyInfo)object;
				privKey = keyConverter.getPrivateKey(pki);
				break;
			}
			if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
				InputDecryptorProvider decProv =
						new JceOpenSSLPKCS8DecryptorProviderBuilder().build(keyPassword.toCharArray());
				privKey =
						keyConverter.getPrivateKey(((PKCS8EncryptedPrivateKeyInfo) object).decryptPrivateKeyInfo(decProv));
				break;
			}
			else if (object instanceof PEMEncryptedKeyPair) {
				PEMDecryptorProvider decProv =
						new JcePEMDecryptorProviderBuilder().build(keyPassword.toCharArray());
				privKey =
						keyConverter.getPrivateKey((((PEMEncryptedKeyPair) object).decryptKeyPair(decProv)).getPrivateKeyInfo());
				break;
			}
			else if (object instanceof PEMKeyPair) {
				privKey =
						keyConverter.getPrivateKey(((PEMKeyPair) object).getPrivateKeyInfo());
				break;
			}
		}

		if (privKey == null) 
			throw new Exception("Private key file did not contain a private key.");

		pemParser = new PEMParser(new BufferedReader(new InputStreamReader(certIS, "UTF-8")));

		ArrayList<Certificate> certs = new ArrayList<Certificate>();

		while ((object = pemParser.readObject()) != null) {
			if (object instanceof X509CertificateHolder) {
				certs.add(certConverter.getCertificate((X509CertificateHolder) object));
			}
		}

		if (certs.isEmpty())
			throw new Exception("Certificate file contained no certificates.");

		KeyStore ks = KeyStore.getInstance("jks");
		ks.load(null);
		ks.setKeyEntry(keyAlias, privKey,
				keyPassword.toCharArray(), certs.toArray(new Certificate[certs.size()]));

		return ks;
	}
	
	/**
	 * Get all alt names contained in a cert (only invocable after SSL identity is set)
	 * @return
	 * @throws Exception
	 */
	public Collection<List<?>> getAltNames() throws Exception {
		if (sslIdentitySet) 
			return altNames;
		else
			throw new Exception("SSL Identity is not set, alternative names are not known");
	}	
	
	/**
	 * Get the GENI URN in a cert, if available (only invocable after SSL identity is set)
	 * @return - the URN that matches "urn:publicid:IDN.+\\+user\\+.+" or null
	 * @throws Exception
	 */
	public String getAltNameUrn() throws Exception {
		Collection<List<?>> altNames = getAltNames();

		String urn = null;
		Iterator <List<?>> it = altNames.iterator();
		while(it.hasNext()) {
			List<?> altName = it.next();
			if ((Integer)altName.get(0) != 6)
				continue;
			Pattern pat = Pattern.compile("urn:publicid:IDN.+\\+user\\+.+");
			Matcher mat = pat.matcher((String)altName.get(1));
			if (mat.matches())  {
				urn = (String)altName.get(1);
			}
		}
		return urn;
	}

}
