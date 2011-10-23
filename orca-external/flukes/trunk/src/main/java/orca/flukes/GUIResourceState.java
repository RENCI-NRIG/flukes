package orca.flukes;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateException;

import orca.flukes.xmlrpc.RegistryXMLRPCProxy;

import org.apache.commons.collections15.Transformer;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import com.hyperrealm.kiwi.ui.dialog.ExceptionDialog;

public class GUIResourceState extends GUICommonState {
	
	public static final String WORLD_ICON="worldmap.jpg";
	
	Map<String,String[]> map = new HashMap<String,String[]>();
	private static GUIResourceState instance = null;
	Map<String, Map<String, String>> amData;

	
	/**
	 * Transform city name to coordinates
	 * @author ibaldin
	 *
	 */
    static class CityTransformer implements Transformer<OrcaNode,String[]> {

        Map<String,String[]> map;
        public CityTransformer(Map<String,String[]> map) {
                this.map = map;
        }

        /**
         * transform airport code to latlon string
         */
        public String[] transform(OrcaNode node) {
        	return map.get(node.getName());
        }
    }

	/**
	 * Transform coordinates into a point
	 * @author ibaldin
	 *
	 */
	static class LatLonPixelTransformer implements Transformer<String[],Point2D> {
		Dimension d;
		int startOffset;

		public LatLonPixelTransformer(Dimension d) {
			this.d = d;
		}
		
		/**
		 * transform a lat
		 */
		 public Point2D transform(String[] latlon) {
			 double latitude = 0;
			 double longitude = 0;
			 String[] lat = latlon[0].split(" ");
			 String[] lon = latlon[1].split(" ");
			 latitude = Integer.parseInt(lat[0]) + Integer.parseInt(lat[1])/60f;
			 latitude *= d.height/180f;
			 longitude = Integer.parseInt(lon[0]) + Integer.parseInt(lon[1])/60f;
			 longitude *= d.width/360f;
			 if(lat[2].equals("N")) {
				 latitude = d.height / 2 - latitude;

			 } else { // assume S
				 latitude = d.height / 2 + latitude;
			 }

			 if(lon[2].equals("W")) {
				 longitude = d.width / 2 - longitude;

			 } else { // assume E
				 longitude = d.width / 2 + longitude;
			 }

			 return new Point2D.Double(longitude,latitude);
		 }
	}
	
	@SuppressWarnings("unchecked")
	private GUIResourceState() {
        map.put("TYO", new String[] {"35 40 N", "139 45 E"});
        map.put("PEK", new String[] {"39 55 N", "116 26 E"});
        map.put("MOW", new String[] {"55 45 N", "37 42 E"});
        map.put("JRS", new String[] {"31 47 N", "35 13 E"});
        map.put("CAI", new String[] {"30 03 N", "31 15 E"});
        map.put("CPT", new String[] {"33 55 S", "18 22 E"});
        map.put("PAR", new String[] {"48 52 N", "2 20 E"});
        map.put("LHR", new String[] {"51 30 N", "0 10 W"});
        map.put("HNL", new String[] {"21 18 N", "157 51 W"});
        map.put("NYC", new String[] {"40 77 N", "73 98 W"});
        map.put("SFO", new String[] {"37 62 N", "122 38 W"});
        map.put("AKL", new String[] {"36 55 S", "174 47 E"});
        map.put("BNE", new String[] {"27 28 S", "153 02 E"});
        map.put("HKG", new String[] {"22 15 N", "114 10 E"});
        map.put("KTM", new String[] {"27 42 N", "85 19 E"});
        map.put("IST", new String[] {"41 01 N", "28 58 E"});
        map.put("STO", new String[] {"59 20 N", "18 03 E"});
        map.put("RIO", new String[] {"22 54 S", "43 14 W"});
        map.put("LIM", new String[] {"12 03 S", "77 03 W"});
        map.put("YTO", new String[] {"43 39 N", "79 23 W"});

	}
	
	public Map<String, String[]> getMap() {
		return map;
	}
	
	private static void initialize() {

	}
	
	static GUIResourceState getInstance() {
		if (instance == null) {
			initialize();
			instance = new GUIResourceState();
		}
		return instance;
	}
	
	/**
	 * Resource pane button actions
	 * @author ibaldin
	 *
	 */
	public class ResourceButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("query")) {
				// run XMLRPC query
				try {
					amData = RegistryXMLRPCProxy.getInstance().getAMs(true);
					System.out.println(amData);
				} catch (Exception ex) {
					ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
					ed.setLocationRelativeTo(GUI.getInstance().getFrame());
					ed.setException("Exception encountered while making XMLRPC query: ", ex);
					ed.setVisible(true);
				}
			} 
		}
	}
	
	private ActionListener al = new ResourceButtonListener();
	public ActionListener getActionListener() {
		return al;
	}
}
