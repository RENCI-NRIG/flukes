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

import java.util.Map.Entry;

import javax.swing.ImageIcon;

import edu.uci.ics.jung.visualization.LayeredIcon;

/**
 * A node representing a crossconnect
 * @author ibaldin
 *
 */
public class OrcaCrossconnect extends OrcaNode {
	// vlan or other path label
	String label = null;
	long bandwidth;
	
	public OrcaCrossconnect(String name) {
		super(name, 
				new LayeredIcon(new ImageIcon(GUIRequestState.class.getResource(OrcaNodeEnum.CROSSCONNECT.getIconName())).getImage()));
	}

	public void setLabel(String s) {
		label = s;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setBandwidth(long b) {
		bandwidth = b;
	}
	
	public long getBandwidth() {
		return bandwidth;
	}
	
	/** 
	 * Create a detailed printout of properties
	 * @return
	 */
	@Override
	public String getViewerText() {
		String viewText = "";
		viewText += "Node name: " + name;
		viewText += "\nNode reservation state: " + state;
		if (label != null)
			viewText += "\nLabel/Tag: " + label;
		if (interfaces.size() > 0) {
			viewText += "\nInterfaces: ";
			for(Entry<OrcaLink, String> e: interfaces.entrySet()) {
				viewText += "\n    " + e.getKey().getName() + " : " + e.getValue();
			}
		}
		return viewText;
	}
}
