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

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;

import orca.flukes.ui.DurationField;
import orca.flukes.ui.TimeField;

import com.hyperrealm.kiwi.ui.DateChooserField;
import com.hyperrealm.kiwi.ui.KCheckBox;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;

@SuppressWarnings("serial")
public class ReservationDetailsDialog extends ComponentDialog {
	private KPanel kp, startDatePanel;
	private TimeField stf;
	private DateChooserField sdcf;
	private DurationField df;
	private JList imageList, domainList;
	private boolean isImmediate, openflowEnabled;
	private KCheckBox immCb, ofCb;
	// we're doing a closure AbstractAction for checkbox and it needs access to 'this'
	// without calling it 'this'
	private ComponentDialog dialog;
	
	private JFrame parent;
	
	private JLabel stLabel;
	
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

	public void setFields(String shortImageName, String domain, OrcaReservationTerm term, String ofVersion) {
		
		OrcaNodePropertyDialog.setListSelectedIndex(imageList, 
				GUIRequestState.getInstance().getImageShortNamesWithNone(), shortImageName);

		OrcaNodePropertyDialog.setListSelectedIndex(domainList, 
				GUIRequestState.getInstance().getAvailableDomains(), domain);
		isImmediate = term.isImmediate();
		immCb.setSelected(isImmediate);
		if (!isImmediate) {
			// set start dates and times
			setTimeDateField(stf, sdcf, term.getStart());
		}
		
		if (ofVersion != null) {
			openflowEnabled = true;
		}
		else {
			openflowEnabled = false;
		}
		ofCb.setSelected(openflowEnabled);
		
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
		imageList = OrcaNodePropertyDialog.addSelectList(kp, gbl_contentPanel, y++,
				GUIRequestState.getInstance().getImageShortNamesWithNone(), "Select image: ", false, 3);
		domainList = OrcaNodePropertyDialog.addSelectList(kp, gbl_contentPanel, y++, 
				GUIRequestState.getInstance().getAvailableDomains(), "Select domain: ", false, 3);		

		{
			JLabel lblNewLabel = new JLabel("Openflow reservation:");
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
			JLabel lblNewLabel = new JLabel("Immediate reservation:");
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
			stLabel = new JLabel("Start Time:");
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
			JLabel durLabel = new JLabel("Term Duration:");
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
			
			GUIRequestState.getInstance().getTerm().setStart(lc.getTime());
		} else
			GUIRequestState.getInstance().getTerm().setStart(null);
		
		if (openflowEnabled) {
			GUIRequestState.getInstance().setOF1_0();
		} else
			GUIRequestState.getInstance().setNoOF();
		
		// get duration
		GUIRequestState.getInstance().getTerm().setDuration(df.getDays(), df.getHours(), df.getMinutes());
		
		// get the image short name
		String curImName = GUIRequestState.getNodeImageProper(GUIRequestState.getInstance().getImageShortNamesWithNone()[imageList.getSelectedIndex()]);
		GUIRequestState.getInstance().setVMImageInReservation(curImName);
		
		// get the domain for reservation
		String domName = GUIRequestState.getNodeDomainProper(GUIRequestState.getInstance().getAvailableDomains()[domainList.getSelectedIndex()]);
		GUIRequestState.getInstance().setDomainInReservation(domName);
		
		return true;
	}

}
