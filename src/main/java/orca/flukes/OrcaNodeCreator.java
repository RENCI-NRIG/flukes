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
import orca.flukes.OrcaResource.ResourceType;
import edu.uci.ics.jung.graph.SparseMultigraph;

public class OrcaNodeCreator implements INodeCreator {
	private OrcaNodeEnum currentSetting = OrcaNodeEnum.CE;
	private final SparseMultigraph<OrcaNode, OrcaLink> g;

	
	public OrcaNodeCreator(SparseMultigraph<OrcaNode, OrcaLink> g) {
		this.g = g;
	}
	
	public void setCurrent(OrcaNodeEnum t) {
		currentSetting = t;
	}
	
	/**
	 * check if node name is unique. exclude a node if needed (or null)
	 * @param node
	 * @param nm
	 * @return
	 */
	public boolean checkUniqueNodeName(OrcaResource node, String nm) {
		// check all edges in graph
		Collection<OrcaNode> nodes = g.getVertices();
		for (OrcaResource n: nodes) {
			// check that some other edge doesn't have this name
			if (node != null) {
				if ((n != node) &&(n.getName().equals(nm)))
					return false;
			} else
				if (n.getName().equals(nm))
					return false;
			
		}
		return true;
	}
	
	@Override
	public OrcaNode create(ResourceType rt) {
		OrcaNode node = null;
		String name;

		try{ 
			do {
				name = currentSetting.getName() + currentSetting.getCount();
				Class<?> pars[] = new Class[1];
				pars[0] = String.class;
				Constructor<?> ct = currentSetting.getClazz().getConstructor(pars);
				Object args[] = new Object[1];
				args[0] = name;
				node = (OrcaNode)ct.newInstance(args);
			} while (!checkUniqueNodeName(null, name));
		} catch (Exception e) {
			;
		}
		node.setResourceType(rt);
		return node;
	}
	
	public void reset() {
		currentSetting.resetCount();
	}
}
