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

import java.lang.reflect.Constructor;
import java.util.Collection;

import orca.flukes.OrcaNode.INodeCreator;
import edu.uci.ics.jung.graph.SparseMultigraph;

public enum OrcaNodeEnum {
	CE(OrcaNode.class, "Node"), 
	ServerCloud(OrcaNodeGroup.class, "NodeGroup"), 
	CrossConnect(OrcaCrossconnect.class, "CrossConnect");
	private int nodeCount;
	private String namePrefix;
	private Class<?> clazz;
	
	OrcaNodeEnum(Class<?> c, String pf) {
		clazz = c;
		nodeCount = 0;
		namePrefix = pf;
	}
	
	public int getCount() {
		return nodeCount++;
	}
	
	public String getName() {
		return namePrefix;
	}
	
	public Class<?> getClazz() {
		return clazz;
	}
}
