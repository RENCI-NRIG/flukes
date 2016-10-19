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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JPasswordField;

import orca.flukes.ui.IpAddrField;

import com.hyperrealm.kiwi.ui.KLabel;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KTextField;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;

@SuppressWarnings("serial")
public class PerformUndoStitchDialog extends ComponentDialog {
	private KPanel kp;
	private KTextField toSlice, toReservation;
	private JPasswordField stitchPass;
	private IpAddrField ip;
	private KLabel toSliceLabel, toReservationLabel, stitchPassLabel, ipAddrLabel;
	boolean performDo = true;
	
	// we're doing a closure AbstractAction for checkbox and it needs access to 'this'
	// without calling it 'this'
	private ComponentDialog dialog;
	
	private JFrame parent;
	
	/**
	 * Create the dialog.
	 */
	public PerformUndoStitchDialog(JFrame parent, boolean doPerform) {
		super(parent, "Perform Slice Stitch", true);
		super.setLocationRelativeTo(parent);
		setComment("Set slice name, reservation id and password:");
		this.parent = parent;
		this.dialog = this;
		performDo = doPerform;
	}
	
	
	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		kp.setLayout(gbl_contentPanel);
		int y = 0;
		
		{
			toSliceLabel = new KLabel("To Slice:");
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = y;
			kp.add(toSliceLabel, gbc_lblNewLabel);
		}
		
		{
			toSlice = new KTextField(25);
			GridBagConstraints gbc_textField = new GridBagConstraints();
			gbc_textField.fill = GridBagConstraints.HORIZONTAL;
			gbc_textField.insets = new Insets(0, 0, 5, 5);
			gbc_textField.gridwidth = 10;
			gbc_textField.gridx = 1;
			gbc_textField.gridy = y++;
			kp.add(toSlice, gbc_textField);
		}
		
		{
			toReservationLabel = new KLabel("To Reservation:");
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = y;
			kp.add(toReservationLabel, gbc_lblNewLabel);
		}
		
		{
			toReservation = new KTextField(25);
			GridBagConstraints gbc_textField = new GridBagConstraints();
			gbc_textField.fill = GridBagConstraints.HORIZONTAL;
			gbc_textField.insets = new Insets(0, 0, 5, 5);
			gbc_textField.gridwidth = 10;
			gbc_textField.gridx = 1;
			gbc_textField.gridy = y++;
			kp.add(toReservation, gbc_textField);
		}
		
		if (performDo) {
			{
				stitchPassLabel = new KLabel("Stitching Password:");
				GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
				gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
				gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
				gbc_lblNewLabel.gridx = 0;
				gbc_lblNewLabel.gridy = y;
				kp.add(stitchPassLabel, gbc_lblNewLabel);
			}

			{
				stitchPass = new JPasswordField(25);
				GridBagConstraints gbc_textField = new GridBagConstraints();
				gbc_textField.fill = GridBagConstraints.HORIZONTAL;
				gbc_textField.fill = GridBagConstraints.HORIZONTAL;
				gbc_textField.gridwidth = 10;
				gbc_textField.gridx = 1;
				gbc_textField.gridy = y++;
				kp.add(stitchPass, gbc_textField);
			}

			{
				ipAddrLabel = new KLabel("IP Address:");
				GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
				gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
				gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
				gbc_lblNewLabel.gridx = 0;
				gbc_lblNewLabel.gridy = y;
				kp.add(ipAddrLabel, gbc_lblNewLabel);
			}

			{
				ip = new IpAddrField();
				GridBagConstraints gbc_textField = new GridBagConstraints();
				gbc_textField.fill = GridBagConstraints.HORIZONTAL;
				gbc_textField.fill = GridBagConstraints.HORIZONTAL;
				gbc_textField.gridwidth = 10;
				gbc_textField.gridx = 1;
				gbc_textField.gridy = y++;
				kp.add(ip, gbc_textField);
			}
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

		if ((!checkField(toReservation.getObject())) || 
				(!checkField(toSlice.getObject()))) 
			return false;
		
		if (performDo) {
			if ((!checkField(new String(stitchPass.getPassword()))) ||
					(!ip.inputValid()))
				return false;
		}
		
		return true;
	}
	
	public String getToSlice() {
		if (toSlice != null)
			return toSlice.getObject();
		return null;
	}
	
	public String getToReservation() {
		if (toReservation != null)
			return toReservation.getObject();
		return null;
	}
	
	public String getStitchPassword() {
		if (performDo && (stitchPass != null))
			return new String(stitchPass.getPassword());
		return null;
	}

	public String getIpAddr() {
		if (performDo && (ip != null))
			return ip.getAddress() + "/" + ip.getNetmask();
		return null;
	}
}
