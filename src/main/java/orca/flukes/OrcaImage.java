package orca.flukes;

import java.net.URL;

/** 
 * Defines an ORCA VM Image
 * @author ibaldin
 *
 */
public class OrcaImage {
	private String shortName, hash;
	URL url;
	
	public OrcaImage(String shortName, URL url, String hash) {
		this.shortName = shortName;
		this.url = url;
		this.hash = hash;
	}

	public URL getUrl() {
		return url;
	}
	
	public String getHash() {
		return hash;
	}
	
	public String getShortName() {
		return shortName;
	}
}
