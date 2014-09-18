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

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Date;
import java.util.List;

import javax.swing.JFrame;

import orca.flukes.ui.TableSelection;

import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;

public class OrcaResourceStateViewer extends ComponentDialog {
	private KPanel kp;
	
	
	public OrcaResourceStateViewer(JFrame parent, List<OrcaResource> resources, Date start, Date end) {
		super(parent, "View current resource states.", false);
		
		super.setLocationRelativeTo(parent);
		setComment("Resource states (start: " + start + " end: " + end + "):");
		
		kp.setLayout(new BorderLayout(0,0));
		kp.add(new TableSelection(resources));
	}

	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		
		return kp;
	}

}