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
import java.awt.event.ActionEvent;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPasswordField;

import orca.flukes.ui.DurationField;
import orca.flukes.ui.TimeField;

import com.hyperrealm.kiwi.ui.DateChooserField;
import com.hyperrealm.kiwi.ui.KCheckBox;
import com.hyperrealm.kiwi.ui.KLabel;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KTextField;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;

@SuppressWarnings("serial")
public class ReservationDetailsDialog extends ComponentDialog {
	private KPanel kp, startDatePanel;
	private TimeField stf;
	private DateChooserField sdcf;
	private DurationField df;
	private JList domainList;
	private boolean isImmediate, openflowEnabled;
	private KCheckBox immCb, ofCb;
	private KTextField ofUserEmail;
	private JPasswordField ofUserPass;
	private KTextField ofCtrlUrl;
	private KLabel ofUserEmailLabel, ofUserPassLabel, ofCtrlUrlLabel;
	
	// we're doing a closure AbstractAction for checkbox and it needs access to 'this'
	// without calling it 'this'
	private ComponentDialog dialog;
	
	private JFrame parent;
	
	private KLabel stLabel;
	
	/**
	 * Create the dialog.
	 */
	public ReservationDetailsDialog(JFrame parent) {
		super(parent, "Reservation Details", true);
		super.setLocationRelativeTo(parent);
		setComment("Select reservation term, global image and other attributes:");
		this.parent = parent;
		this.dialog = this;
	}
	
	/**
	 * Change visibility of openflow-related fields
	 * @param v
	 */
	private void setOfVisible(boolean v) {
		ofUserEmailLabel.setVisible(v);
		ofUserPassLabel.setVisible(v);
		ofCtrlUrlLabel.setVisible(v);
		
		ofUserEmail.setVisible(v);
		ofUserPass.setVisible(v);
		ofCtrlUrl.setVisible(v);
	}

	public void setFields(String domain, OrcaReservationTerm term, String ofVersion) {

		OrcaNodePropertyDialog.setListSelectedIndex(domainList, 
				GUIDomainState.getInstance().getAvailableDomains(), domain);
		isImmediate = term.isImmediate();
		immCb.setSelected(isImmediate);
		if (!isImmediate) {
			// set start dates and times
			setTimeDateField(stf, sdcf, term.getStart());
		}
		
		if (ofVersion != null) {
			openflowEnabled = true;
			ofUserEmail.setObject(GUIUnifiedState.getInstance().getOfUserEmail());
			ofUserPass.setText(GUIUnifiedState.getInstance().getOfSlicePass());
			ofCtrlUrl.setObject(GUIUnifiedState.getInstance().getOfCtrlUrl().toString());
		}
		else {
			openflowEnabled = false;
		}
		ofCb.setSelected(openflowEnabled);
		setOfVisible(openflowEnabled);
		
		// set duration
		df.setDurationField(term.getDurationDays(), term.getDurationHours(), term.getDurationMins());
		
		// adjust visibility based on initial setting
		stf.setVisible(!isImmediate);
		sdcf.setVisible(!isImmediate);
		stLabel.setVisible(!isImmediate);
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

		domainList = OrcaNodePropertyDialog.addSelectList(kp, gbl_contentPanel, y++, 
				GUIDomainState.getInstance().getAvailableDomains(), "Select domain: ", false, 3);		

		{
			KLabel lblNewLabel = new KLabel("Openflow reservation:");
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = y;
			kp.add(lblNewLabel, gbc_lblNewLabel);
		}
		{
			ofCb = new KCheckBox(new AbstractAction() {
				
				public void actionPerformed(ActionEvent e) {
					openflowEnabled = !openflowEnabled;
					setOfVisible(openflowEnabled);
					dialog.pack();
				}
			});
			GridBagConstraints gbc_tf= new GridBagConstraints();
			gbc_tf.anchor = GridBagConstraints.WEST;
			gbc_tf.insets = new Insets(0, 0, 5, 5);
			gbc_tf.gridx = 1;
			gbc_tf.gridy = y++;
			kp.add(ofCb, gbc_tf);
		}
		
		{
			ofUserEmailLabel = new KLabel("User email:");
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = y;
			kp.add(ofUserEmailLabel, gbc_lblNewLabel);
		}
		
		{
			ofUserEmail = new KTextField(25);
			GridBagConstraints gbc_textField = new GridBagConstraints();
			gbc_textField.fill = GridBagConstraints.HORIZONTAL;
			gbc_textField.insets = new Insets(0, 0, 5, 5);
			gbc_textField.gridwidth = 10;
			gbc_textField.gridx = 1;
			gbc_textField.gridy = y++;
			kp.add(ofUserEmail, gbc_textField);
		}
		
		{
			ofUserPassLabel = new KLabel("Slice password:");
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = y;
			kp.add(ofUserPassLabel, gbc_lblNewLabel);
		}
		
		{
			ofUserPass = new JPasswordField(25);
			GridBagConstraints gbc_textField = new GridBagConstraints();
			gbc_textField.fill = GridBagConstraints.HORIZONTAL;
			gbc_textField.insets = new Insets(0, 0, 5, 5);
			gbc_textField.gridwidth = 10;
			gbc_textField.gridx = 1;
			gbc_textField.gridy = y++;
			kp.add(ofUserPass, gbc_textField);
		}
		
		{
			ofCtrlUrlLabel = new KLabel("User controller URL:");
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = y;
			kp.add(ofCtrlUrlLabel, gbc_lblNewLabel);
		}
		
		{
			ofCtrlUrl = new KTextField(25);
			GridBagConstraints gbc_textField = new GridBagConstraints();
			gbc_textField.fill = GridBagConstraints.HORIZONTAL;
			gbc_textField.fill = GridBagConstraints.HORIZONTAL;
			gbc_textField.gridwidth = 10;
			gbc_textField.gridx = 1;
			gbc_textField.gridy = y++;
			kp.add(ofCtrlUrl, gbc_textField);
		}
		
		{
			KLabel lblNewLabel = new KLabel("Immediate reservation:");
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = y;
			kp.add(lblNewLabel, gbc_lblNewLabel);
		}
		{
			immCb = new KCheckBox(new AbstractAction() {
				
				public void actionPerformed(ActionEvent e) {
					// toggle the enable state for time components
					stf.setVisible(isImmediate);
					sdcf.setVisible(isImmediate);
					stLabel.setVisible(isImmediate);
					startDatePanel.setVisible(isImmediate);
					// set initial time to now if not immediate
					if (isImmediate) { 
						setTimeDateField(stf, sdcf, Calendar.getInstance().getTime());
					}
					dialog.pack();
					isImmediate = !isImmediate;
				}
			});
			GridBagConstraints gbc_tf= new GridBagConstraints();
			gbc_tf.anchor = GridBagConstraints.WEST;
			gbc_tf.insets = new Insets(0, 0, 5, 5);
			gbc_tf.gridx = 1;
			gbc_tf.gridy = y++;
			kp.add(immCb, gbc_tf);
		}
		{
			stLabel = new KLabel("Start Time:");
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = y;
			kp.add(stLabel, gbc_lblNewLabel);
		}
		{
			startDatePanel = new KPanel();
			stf = new TimeField();
			startDatePanel.add(stf);
			sdcf = new DateChooserField();
			startDatePanel.add(sdcf);
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 1;
			gbc_lblNewLabel.gridy = y++;
			kp.add(startDatePanel, gbc_lblNewLabel);
		}
		
		// duration field
		{
			KLabel durLabel = new KLabel("Term Duration:");
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = y;
			kp.add(durLabel, gbc_lblNewLabel);
		}
		{
			df = new DurationField();
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 1;
			gbc_lblNewLabel.gridy = y++;
			kp.add(df, gbc_lblNewLabel);
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

		if (!isImmediate) {
			// if not immediate reservation get start time/date
			if (!stf.validateInput())
				return false;
		
			// check that start/end time are proper, save the information
			Date sDate = sdcf.getDate();

			Calendar lc = Calendar.getInstance();
			lc.setTime(sDate);
			lc.set(Calendar.HOUR_OF_DAY, stf.getHour());
			lc.set(Calendar.MINUTE, stf.getMinute());
			
			// time before now is not allowed
			Calendar tc = Calendar.getInstance();
			if (lc.before(tc))
				return false;
			
			GUIUnifiedState.getInstance().getTerm().setStart(lc.getTime());
		} else
			GUIUnifiedState.getInstance().getTerm().setStart(null);
		
		if (openflowEnabled) {
			GUIUnifiedState.getInstance().setOF1_0();
			if ((!checkField(ofCtrlUrl.getObject())) ||
					(!checkField(ofUserEmail.getObject())) ||
							(!checkField(ofUserPass.getPassword().toString())))
				return false;
			GUIUnifiedState.getInstance().setOfUserEmail(ofUserEmail.getObject());
			GUIUnifiedState.getInstance().setOfSlicePass(new String(ofUserPass.getPassword()));
			GUIUnifiedState.getInstance().setOfCtrlUrl(ofCtrlUrl.getObject());
		} else
			GUIUnifiedState.getInstance().setNoOF();
		
		// get duration
		GUIUnifiedState.getInstance().getTerm().setDuration(df.getDays(), df.getHours(), df.getMinutes());
		
		// get the domain for reservation
		String domName = GUIDomainState.getNodeDomainProper(GUIDomainState.getInstance().getAvailableDomains()[domainList.getSelectedIndex()]);
		GUIUnifiedState.getInstance().setDomainInReservation(domName);
		
		return true;
	}

}
