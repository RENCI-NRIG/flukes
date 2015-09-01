/*
* Copyright (c) 2011 RENCI/UNC Chapel Hill 
*
* @author Ilia Baldine
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
* and/or hardware specification (the "Work") to deal in the Work without restriction, including 
* without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
* sell copies of the Work, and to permit persons to whom the Work is furnished to do so, subject to 
* the following conditions:  
* The above copyright notice and this permission notice shall be included in all copies or 
* substantial portions of the Work.  
*
* THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
* OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS 
* IN THE WORK.
*/
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
		setResourceType(ResourceType.RESOURCE);
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
