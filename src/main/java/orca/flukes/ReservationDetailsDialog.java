package orca.flukes;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import orca.flukes.ui.TimeField;

import com.hyperrealm.kiwi.ui.DateChooserField;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;
import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

@SuppressWarnings("serial")
public class ReservationDetailsDialog extends ComponentDialog {
	private KPanel kp;
	private TimeField stf, etf;
	private DateChooserField sdcf, edcf;
	private JList imageList, domainList;

	private JFrame parent;
	
	/**
	 * Create the dialog.
	 */
	public ReservationDetailsDialog(JFrame parent) {
		super(parent, "Reservation Details", true);
		super.setLocationRelativeTo(parent);
		setComment("Select reservation term, global image and other attributes:");
		this.parent = parent;
	}

	public void setFields(String shortImageName, String domain, Date start, Date end) {
		int index = 0;
		if (shortImageName != null) {
			for (String n: GUIState.getInstance().getImageShortNamesWithNone()) {
				if (n.equals(shortImageName))
					break;
				index++;
			}
			if (index == GUIState.getInstance().getImageShortNamesWithNone().length)
				imageList.setSelectedIndex(0);
			else
				imageList.setSelectedIndex(index);
		}
		
		if (domain !=null) {
			for (String n: GUIState.getInstance().getAvailableDomains()) {
				if (n.equals(domain))
					break;
				index++;
			}
			if (index == GUIState.getInstance().getAvailableDomains().length)
				domainList.setSelectedIndex(0);
			else
				domainList.setSelectedIndex(index);
		}
		// set dates and times
		setTimeDateField(stf, sdcf, GUIState.getInstance().resStart);
		setTimeDateField(etf, edcf, GUIState.getInstance().resEnd);
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
		imageList = OrcaNodePropertyDialog.addImageList(kp, gbl_contentPanel, y++);
		domainList = OrcaNodePropertyDialog.addDomainList(kp, gbl_contentPanel, y++);
		
		{
			JLabel lblNewLabel = new JLabel("Start Time");
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = y;
			kp.add(lblNewLabel, gbc_lblNewLabel);
		}
		{
			stf = new TimeField();
			GridBagConstraints gbc_tf= new GridBagConstraints();
			gbc_tf.anchor = GridBagConstraints.WEST;
			gbc_tf.insets = new Insets(0, 0, 5, 5);
			gbc_tf.gridx = 1;
			gbc_tf.gridy = y;
			kp.add(stf, gbc_tf);
		}
		{
			sdcf = new DateChooserField();
			GridBagConstraints gbc_dcf= new GridBagConstraints();
			gbc_dcf.anchor = GridBagConstraints.WEST;
			gbc_dcf.insets = new Insets(0, 0, 5, 5);
			gbc_dcf.gridx = 2;
			gbc_dcf.gridy = y++;
			kp.add(sdcf, gbc_dcf);
		}
		{
			JLabel lblNewLabel = new JLabel("End Time");
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = y;
			kp.add(lblNewLabel, gbc_lblNewLabel);
		}
		{
			etf = new TimeField();
			GridBagConstraints gbc_tf= new GridBagConstraints();
			gbc_tf.anchor = GridBagConstraints.WEST;
			gbc_tf.insets = new Insets(0, 0, 5, 5);
			gbc_tf.gridx = 1;
			gbc_tf.gridy = y;
			kp.add(etf, gbc_tf);
		}
		{
			edcf = new DateChooserField();
			GridBagConstraints gbc_dcf= new GridBagConstraints();
			gbc_dcf.anchor = GridBagConstraints.WEST;
			gbc_dcf.insets = new Insets(0, 0, 5, 5);
			gbc_dcf.gridx = 2;
			gbc_dcf.gridy = y++;
			kp.add(edcf, gbc_dcf);
		}

		return kp;
	}
	
	@Override
	protected boolean accept() {
	
		if (!stf.validateInput() || !etf.validateInput())
			return false;
		
		// check that start/end time are proper, save the information
		Date sDate = sdcf.getDate();
		Date eDate = edcf.getDate();
		
		if (sDate.compareTo(eDate) > 0) {
			KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
			kmd.setMessage("Start date must be before end date!");
			kmd.setLocationRelativeTo(parent);
			kmd.setVisible(true);
			return false;
		}
		
		if (sDate.compareTo(eDate) == 0) {
			// make sure start time is before end time
			if (stf.getMinutes() > etf.getMinutes()) {
				KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
				kmd.setMessage("Start time must be before end time!");
				kmd.setLocationRelativeTo(parent);
				kmd.setVisible(true);
				return false;
			}
		}
		
		Calendar lc = Calendar.getInstance();
		lc.setTime(sDate);
		lc.set(Calendar.HOUR_OF_DAY, stf.getHour());
		lc.set(Calendar.MINUTE, stf.getMinute());
		GUIState.getInstance().resStart = lc.getTime();
		
		lc = Calendar.getInstance();
		lc.setTime(eDate);
		lc.set(Calendar.HOUR_OF_DAY, etf.getHour());
		lc.set(Calendar.MINUTE, etf.getMinute());
		GUIState.getInstance().resEnd = lc.getTime();
		
		// get the image short name
		String curImName = GUIState.getNodeImageProper(GUIState.getInstance().getImageShortNamesWithNone()[imageList.getSelectedIndex()]);
		GUIState.getInstance().setVMImageInReservation(curImName);
		
		// get the domain for reservation
		String domName = GUIState.getNodeDomainProper(GUIState.getInstance().getAvailableDomains()[domainList.getSelectedIndex()]);
		GUIState.getInstance().setDomainInReservation(domName);
		
		return true;
	}

}
