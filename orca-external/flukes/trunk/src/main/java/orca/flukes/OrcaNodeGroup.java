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

import javax.swing.ImageIcon;

import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.LayeredIcon;

public class OrcaNodeGroup extends OrcaNode {
	Pair<String> internalVlanAddress = null;
	protected int nodeCount = 1;

	public OrcaNodeGroup(String name) {
		super(name, 
				new LayeredIcon(new ImageIcon(GUIRequestState.class.getResource(GUIRequestState.CLOUD_ICON)).getImage()));
	}

	public int getNodeCount() {
		return nodeCount;
	}
	
	public void setNodeCount(int nc) {
		if (nc > 1)
			nodeCount = nc;
	}
	
	/**
	 * Node groups can have an internal bus with IP address
	 * @param addr
	 * @param nm
	 */
	public void setInternalIp(String addr, String nm) {
		if (addr == null)
			return;
		if (nm == null)
			nm = NODE_NETMASK;
		internalVlanAddress = new Pair<String>(addr, nm);
	}
	
	public String getInternalIp() {
		if (internalVlanAddress != null)
			return internalVlanAddress.getFirst();
		return null;
	}
	
	public String getInternalNm() {
		if (internalVlanAddress != null)
			return internalVlanAddress.getSecond();
		return null;
	}
	
	public void removeInternalIp() {
		internalVlanAddress = null;
	}
}
