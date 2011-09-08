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

import java.awt.Component;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URL;

import javax.swing.JFrame;

import com.hyperrealm.kiwi.ui.KLabel;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KTextField;
import com.hyperrealm.kiwi.ui.URLField;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;
import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

/**
 * Dialog for editing image properties (Short name, URL, Hash)
 * @author ibaldin
 *
 */
public class OrcaImageDialog extends ComponentDialog {
	private KTextField shortName, imageHash;
	private URLField imageUrl;
	JFrame parent;
	OrcaImage oi = null;

	public OrcaImageDialog(JFrame parent) {
		super(parent, "ORCA Images", true);
		super.setLocationRelativeTo(parent);
		setComment("Enter image name, URL and hash");
		this.parent = parent;
	}
	
	public void setImage(OrcaImage oi) {
		this.oi = oi;
		shortName.setObject(oi.getShortName());
		imageUrl.setObject(oi.getUrl());
		imageHash.setObject(oi.getHash());
	}
	
	@Override
	protected Component buildDialogUI() {
		KPanel kp = new KPanel();
		
		GridBagLayout gbl_contentPanel = new GridBagLayout();
//		gbl_contentPanel.columnWidths = new int[]{0, 0, 0, 0};
//		gbl_contentPanel.rowHeights = new int[]{0, 0, 0};
//		gbl_contentPanel.columnWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
//		gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		kp.setLayout(gbl_contentPanel);
		{
			KLabel kl = new KLabel("Short name");
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = 0;
			kp.add(kl, gbc_lblNewLabel);
		}
		{
			shortName = new KTextField(25);
			GridBagConstraints gbc_textField = new GridBagConstraints();
			gbc_textField.fill = GridBagConstraints.HORIZONTAL;
			gbc_textField.insets = new Insets(0, 0, 5, 5);
			gbc_textField.gridwidth = 10;
			gbc_textField.gridx = 1;
			gbc_textField.gridy = 0;
			kp.add(shortName, gbc_textField);
		}
		{
			KLabel kl = new KLabel("Image URL");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = 1;
			kp.add(kl, gbc_lblNewLabel_1);
		}
		{
			imageUrl = new URLField(25);
			GridBagConstraints gbc_textField = new GridBagConstraints();
			gbc_textField.fill = GridBagConstraints.HORIZONTAL;
			gbc_textField.fill = GridBagConstraints.HORIZONTAL;
			gbc_textField.gridwidth = 10;
			gbc_textField.gridx = 1;
			gbc_textField.gridy = 1;
			kp.add(imageUrl, gbc_textField);
		}
		{
			KLabel kl = new KLabel("Image hash");
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = 2;
			kp.add(kl, gbc_lblNewLabel);
		}
		{
			imageHash = new KTextField(25);
			GridBagConstraints gbc_textField = new GridBagConstraints();
			gbc_textField.fill = GridBagConstraints.HORIZONTAL;
			gbc_textField.insets = new Insets(0, 0, 5, 5);
			gbc_textField.gridwidth = 10;
			gbc_textField.gridx = 1;
			gbc_textField.gridy = 2;
			kp.add(imageHash, gbc_textField);
		}
		
		return kp;
	}

	private boolean checkField(String f) {
		if ((f != null) && (f.length() != 0))
			return true;
		return false;
	}
	
	@Override
	protected boolean accept() {
		
		if (checkField(shortName.getObject()) && 
				(imageUrl.getObject() != null) && 
				checkField(imageHash.getObject())) {
			// disallow adding under same name
			if (GUIState.getInstance().addingNewImage && GUIState.getInstance().definedImages.containsKey(shortName.getObject())) {
				KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
				kmd.setMessage("Image with short name " + shortName.getObject() + " already exists!");
				kmd.setLocationRelativeTo(parent);
				kmd.setVisible(true);
				return false;
			}
			if (GUIState.getInstance().addingNewImage && shortName.getObject().equals(GUIState.NO_GLOBAL_IMAGE)) {
				KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
				kmd.setMessage("Short name \"None\" is reserved!");
				kmd.setLocationRelativeTo(parent);
				kmd.setVisible(true);
				return false;
			}
			// add or replace image
			GUIState.getInstance().addImage(new OrcaImage(shortName.getObject(), imageUrl.getObject(), imageHash.getObject()), oi);
			//GUIState.getInstance().definedImages.put(shortName.getObject(), 
			//		new OrcaImage(shortName.getObject(), imageUrl.getObject(), imageHash.getObject()));
			// TODO: repaint the dialog instead of killing it
			GUIState.getInstance().icd.setVisible(false);
			GUIState.getInstance().icd.destroy();
			GUIState.getInstance().addingNewImage = false;
			return true;
		}
		return false;
	}
}
