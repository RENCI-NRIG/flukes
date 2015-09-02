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
import java.util.Calendar;
import java.util.Date;

import javax.swing.JFrame;

import orca.flukes.ui.TimeField;

import com.hyperrealm.kiwi.ui.DateChooserField;
import com.hyperrealm.kiwi.ui.KLabel;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;

@SuppressWarnings("serial")
public class ReservationExtensionDialog extends ComponentDialog {
	private KPanel kp, endDatePanel;
	private TimeField etf;
	private DateChooserField edcf;
	
	private JFrame parent;
	
	private KLabel etLabel;
	
	/**
	 * Create the dialog.
	 */
	public ReservationExtensionDialog(JFrame parent) {
		super(parent, "Reservation Extension", true);
		super.setLocationRelativeTo(parent);
		setComment("Select new reservation end date:");
		this.parent = parent;
	}
	

	public void setFields(Date s) {

		// set new date/time
		setTimeDateField(etf, edcf, s);
	}
	
	private void setTimeDateField(TimeField tf, DateChooserField dcf, Date d) {
		Calendar lc = Calendar.getInstance();
		if (d != null) {
			lc.setTime(d);
			tf.setTime(lc.get(Calendar.HOUR_OF_DAY), lc.get(Calendar.MINUTE));
			dcf.setDate(d);
		} else {
			tf.setNow();
			dcf.setDate(Calendar.getInstance().getTime());
		}
	}
	
	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		
		GridBagLayout gbl_contentPanel = new GridBagLayout();
//		gbl_contentPanel.columnWidths = new int[]{0, 0, 0};
//		gbl_contentPanel.rowHeights = new int[]{0, 0, 0, 0};
//		gbl_contentPanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
//		gbl_contentPanel.rowWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		kp.setLayout(gbl_contentPanel);
		int y = 0;
		
		{
			etLabel = new KLabel("New End Time:");
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = y;
			kp.add(etLabel, gbc_lblNewLabel);
			etLabel.setVisible(true);
		}
		{
			endDatePanel = new KPanel();
			etf = new TimeField();
			endDatePanel.add(etf);
			edcf = new DateChooserField();
			endDatePanel.add(edcf);
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 1;
			gbc_lblNewLabel.gridy = y++;
			kp.add(endDatePanel, gbc_lblNewLabel);
			etf.setVisible(true);
			edcf.setVisible(true);
			endDatePanel.setVisible(true);
		}

		return kp;
	}
	
	@Override
	protected boolean accept() {

		// if not immediate reservation get start time/date
		if (!etf.validateInput())
			return false;

		// check that start/end time are proper, save the information
		Date eDate = edcf.getDate();

		Calendar lc = Calendar.getInstance();
		lc.setTime(eDate);
		lc.set(Calendar.HOUR_OF_DAY, etf.getHour());
		lc.set(Calendar.MINUTE, etf.getMinute());

		// time before now is not allowed
		Calendar tc = Calendar.getInstance();
		if (lc.before(tc))
			return false;

		GUIUnifiedState.getInstance().setNewEndDate(lc.getTime());
		
		return true;
	}

}
