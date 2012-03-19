package orca.flukes;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import edu.uci.ics.jung.visualization.LayeredIcon;

public class OrcaResourceSite extends OrcaNode {
	float lat, lon;
	List<String> domains = new ArrayList<String>();
	
	public OrcaResourceSite(String name, float lat, float lon) {
		super(name, 
				new LayeredIcon(new ImageIcon(GUIRequestState.class.getResource(OrcaNodeEnum.RESOURCESITE.getIconName())).getImage()));
		domain = name;
		this.lat = lat;
		this.lon = lon;
	}
	
	public float getLat() {
		return lat;
	}
	
	public float getLon() {
		return lon;
	}
	
	public void addDomain(String d) {
		domains.add(d);
	}
	
	public List<String> getDomains() {
		return domains;
	}
}
