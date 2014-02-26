package orca.flukes;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import orca.flukes.xmlrpc.GENICHXMLRPCProxy;
import orca.flukes.xmlrpc.GENICHXMLRPCProxy.SaField;
import orca.flukes.xmlrpc.OrcaSMXMLRPCProxy;

/**
 * Testing GENI CH API
 * @author ibaldin
 *
 */
public class TestCHApi {
	
	public static void main(String[] argv) {
		
		GUI.getInstance().processPreferences();
		GUI.getInstance().setSelectedController("https://geni.renci.org:11443/orca/xmlrpc");
		
		GENICHXMLRPCProxy p = GENICHXMLRPCProxy.getInstance();
		
		OrcaSMXMLRPCProxy pp = OrcaSMXMLRPCProxy.getInstance();
		
		try {
			Map<String, Object> ret = pp.getVersion();
			for(Map.Entry<String, Object> e: ret.entrySet()) {
				System.out.println(e.getKey() + " " + e.getValue());
			}
		} catch (Exception e) {
			System.err.println("Exception: " + e);
			e.printStackTrace();
		}
		
//		p.resetSSLIdentity();
		
		try {
			//Map<String, Object> r = p.saGetVersion();
			
//			Map<String, Object> r = p.saCreateSlice("testFlukes", "urn:publicid:IDN+ch.geni.net+project+ADAMANT");
			
//			String sUrn = p.saCreateSlice("testFlukesAgain", "urn:publicid:IDN+ch.geni.net+project+ADAMANT");
//			System.out.println("Slice URN is " + sUrn);
			
			Calendar cal = Calendar.getInstance();
			cal.set(2014, 3, 15);
			TimeZone tz = TimeZone.getTimeZone("UTC");
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
			df.setTimeZone(tz);
			String nowAsISO = df.format(cal.getTime());
			
			p.saUpdateSlice("urn:publicid:IDN+ch.geni.net:ADAMANT+slice+testFlukes", SaField.SLICE_EXPIRATION, nowAsISO);
			
//			String sliceUrn = "urn:publicid:IDN+ch.geni.net:ADAMANT+slice+testFlukes";
//			Map<String, Object> r = p.saLookupSlice(sliceUrn, 
//					new SaField[] {	GENICHXMLRPCProxy.SaField.SLICE_EXPIRATION});
//			
//			String dateString = (String)((Map<String, Object>)r.get(sliceUrn)).get(GENICHXMLRPCProxy.SaField.SLICE_EXPIRATION.name());
//			System.out.println("EXPIRATION IS " + dateString);
//		    DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.ENGLISH);
//		    df.setTimeZone(TimeZone.getTimeZone("UTC"));
//		    Date result =  df.parse(dateString);  
//		    System.out.println(result);
//		    
//			for(Map.Entry<String, Object> e: r.entrySet()) {
//				if (e.getValue() instanceof HashMap) {
//					System.out.println(e.getKey());
//					HashMap<String, Object> inner = (HashMap<String, Object>)e.getValue();
//					for (Map.Entry<String, Object> ee: inner.entrySet()) {
//						if (ee.getValue() instanceof Object[]) {
//							System.out.println("-- " + ee.getKey());
//							Object[] roles = (Object[])ee.getValue();
//							for (Object role: roles) {
//								System.out.println("++++++ " + role);
//							}
//						} else 
//							System.out.println("---- " + ee.getKey() + ": " +  ee.getValue());
//					}
// 				} else if (e.getValue() instanceof Object[]) {
// 					System.out.println(e.getKey());
// 					for(Object o: (Object[])e.getValue()) {
// 						System.out.println("--- " + o);
// 					}
// 				} else
// 					System.out.println(e.getKey() + ": "+ e.getValue());
//			}
		} catch (Exception e) {
			System.err.println("Exception: " + e);
			e.printStackTrace();
		}
	}
}
