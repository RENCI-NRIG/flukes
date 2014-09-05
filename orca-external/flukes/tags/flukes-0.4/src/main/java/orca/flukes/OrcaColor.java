/*
* Copyright (c) 2013 RENCI/UNC Chapel Hill 
*
* @author Ilya Baldin
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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.uci.ics.jung.graph.util.Pair;

// represents application-specific color attached to orca resource or 
// to a color link
public class OrcaColor {
	protected String label;
	protected Map<String, String> keys = new HashMap<String, String>();
	protected String blob;
	protected boolean isBlobXML = false;
	
	public OrcaColor(String s) {
		label = s;
	}
	
	public void setLabel(String l) {
		label = l;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void addKey(String key, String val) {
		keys.put(key, val);
	}
	
	public void delKey(String key) {
		keys.remove(key);
	}
	
	public void addKeys(Map<String, String> k) {
		keys.putAll(k);
	}
	
	public Map<String, String> getKeysCopy() {
		return new HashMap<String, String>(keys);
	}
	
	public Map<String, String> getKeys() {
		return keys;
	}
	
	public void setBlob(String b) {
		blob = b;
	}
	
	public String getBlob() {
		return blob;
	}
	
	/**
	 * set if blob is XML
	 * @param xml
	 */
	public void setXMLBlobState(boolean xml) {
		isBlobXML = xml;
	}
	
	/**
	 * is it an XML blob?
	 * @return
	 */
	public boolean getXMLBlobState() {
		return isBlobXML;
	}
	
	public String getViewerText() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("Color label: " + label);
		sb.append("\n\nKey-value pairs: ");
		for(Entry<String, String> e: keys.entrySet()) {
			sb.append("\n\t" + e.getKey() + "=" + e.getValue());
		}
		sb.append("\n\n");
		sb.append("\nText Blob (" + (isBlobXML ? "XML" : "non XML") + "):\n");
		sb.append(blob);
		
		return sb.toString();
	}
}
