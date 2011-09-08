/*
* Copyright (c) 2011 RENCI/UNC Chapel Hill 
*
* @author Ilia Baldine
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
* and/or hardware specification (the “Work”) to deal in the Work without restriction, including 
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;

import orca.flukes.ui.ChooserWithNewDialog;

@SuppressWarnings("serial")
public class ImageChooserDialog extends ChooserWithNewDialog<String> implements ActionListener {
	
	public ImageChooserDialog(JFrame parent) {
		super(parent, "Images", "Defined Images", GUIState.getInstance().getImageShortNamesIterator());
		super.setLocationRelativeTo(parent);
		setNewActionListener(this);
	}
	
	public void actionPerformed(ActionEvent e) {
		OrcaImageDialog oid = new OrcaImageDialog(GUI.getInstance().getFrame());
		oid.pack();
		GUIState.getInstance().addingNewImage = true;
		oid.setVisible(true);
	}
	
	@Override
	protected boolean accept() {
		GUIState.getInstance().addingNewImage = false;
		String st = getSelectedItem();
		
		// open image dialog with image details
		if (st != null) {
			OrcaImageDialog oid = new OrcaImageDialog(GUI.getInstance().getFrame());
			//oid.setFields(st, GUIState.getInstance().definedImages.get(st).getUrl(), 
			//		GUIState.getInstance().definedImages.get(st).getHash());
			oid.setImage(GUIState.getInstance().getImageByName(st));
			oid.pack();
			oid.setVisible(true);
		}
		
		return true;
	}
	
	@Override
	protected void cancel() {
		super.cancel();
		GUIState.getInstance().addingNewImage = false;
	}
}
