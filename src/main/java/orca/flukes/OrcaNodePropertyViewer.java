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

import javax.swing.JFrame;

import com.hyperrealm.kiwi.ui.KTextArea;

import orca.flukes.ui.TextAreaDialog;

public class OrcaNodePropertyViewer extends TextAreaDialog {
	
	public OrcaNodePropertyViewer(JFrame parent, OrcaResource node) {
		super(parent, "View node properties for " + node.getName(), 
				"Node Properties:", 20, 50);
	
	KTextArea ta = this.getTextArea();
	
	if (node instanceof OrcaNode)
		ta.setText(((OrcaNode)node).getViewerText());
	}

	public OrcaNodePropertyViewer(JFrame parent, OrcaResource node, String text) {
		super(parent, "View properties for " + node.getName(), 
				"Properties:", 20, 50);

		KTextArea ta = this.getTextArea();

		ta.setText(text);
	}
}
