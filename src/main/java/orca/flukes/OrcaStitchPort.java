package orca.flukes;

import java.util.Map.Entry;

import javax.swing.ImageIcon;

import edu.uci.ics.jung.visualization.LayeredIcon;

/*
* Copyright (c) 2013 RENCI/UNC Chapel Hill 
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
public class OrcaStitchPort extends OrcaNode {
	private static final String STITCHING_PORT = "Stitching port";
	public static final String STITCHING_DOMAIN_SHORT_NAME = "Stitching domain";
	protected String label;
	protected String port;
	
	public OrcaStitchPort(String name) {
		super(name, 
				new LayeredIcon(new ImageIcon(GUIRequestState.class.getResource(OrcaNodeEnum.STITCHPORT.getIconName())).getImage()));
		setDomain(STITCHING_DOMAIN_SHORT_NAME);
		setNodeType(STITCHING_PORT);
	}

	public void setLabel(String l) {
    	if ((l != null) && l.length() > 0)
    		label = l;
    	else
    		label = null;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setPort(String p) {
    	if ((p != null) && p.length() > 0)
    		port = p;
    	else
    		port = null;
	}
	
	public String getPort() {
		return port;
	}
	
	/** 
	 * Create a detailed printout of properties
	 * @return
	 */
	@Override
	public String getViewerText() {
		String viewText = "";
		viewText += "Stitch port: " + name;
		viewText += "\nPort id: " + port;
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
