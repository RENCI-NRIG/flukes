package orca.flukes;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JLabel;

import com.hyperrealm.kiwi.text.FormatConstants;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KTextField;
import com.hyperrealm.kiwi.ui.NumericField;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;

public class OrcaLinkPropertyDialog extends ComponentDialog {
	JFrame parent;
	OrcaLink edge;
	
	NumericField bandwidth, latency;
	KTextField name;
	
	KPanel kp;
	
	public OrcaLinkPropertyDialog(JFrame parent, OrcaLink e) {
		super(parent, "Edge Details", true);
		super.setLocationRelativeTo(parent);
		setComment("Edge " + e.getName() + " properties");
		this.parent = parent;
		this.edge = e;
		if (e != null) {
			name.setObject(e.getName());
			bandwidth.setValue(e.getBandwidth());
			latency.setValue(e.getLatency());
		}
	}

	@Override
	public boolean accept() {
		if ((name.getObject().length() == 0) || (!bandwidth.validateInput()) || (!latency.validateInput()))
			return false;
		edge.setName(name.getObject());
		edge.setBandwidth((long)bandwidth.getValue());
		edge.setLatency((long)latency.getValue());
		return true;
	}
	
	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		kp.setLayout(gbl_contentPanel);
		{
			JLabel lblNewLabel_1 = new JLabel("Name: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = 0;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			name = new KTextField(10);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.WEST;
			gbc_list.gridx = 1;
			gbc_list.gridy = 0;
			kp.add(name, gbc_list);
		}
		{
			JLabel lblNewLabel_1 = new JLabel("Bandwidth: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = 1;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			bandwidth = new NumericField(10);
			bandwidth.setMinValue(0);
			bandwidth.setType(FormatConstants.LONG);
			bandwidth.setDecimals(0);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.WEST;
			gbc_list.gridx = 1;
			gbc_list.gridy = 1;
			kp.add(bandwidth, gbc_list);
		}
		{
			JLabel lblNewLabel_1 = new JLabel("Latency: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = 2;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			latency = new NumericField(10);
			latency.setMinValue(0);
			latency.setType(FormatConstants.LONG);
			latency.setDecimals(0);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.WEST;
			gbc_list.gridx = 1;
			gbc_list.gridy = 2;
			kp.add(latency, gbc_list);
		}
		
		return kp;
	}

}
