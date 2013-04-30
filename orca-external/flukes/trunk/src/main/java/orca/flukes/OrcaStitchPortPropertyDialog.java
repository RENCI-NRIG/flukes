package orca.flukes;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JLabel;

import com.hyperrealm.kiwi.text.FormatConstants;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KTextArea;
import com.hyperrealm.kiwi.ui.KTextField;
import com.hyperrealm.kiwi.ui.NumericField;
import com.hyperrealm.kiwi.ui.URLField;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;
import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

public class OrcaStitchPortPropertyDialog extends ComponentDialog {
	JFrame parent;
	OrcaStitchPort stitchPort;
	
	protected URLField port;
	protected KTextField name;
	protected NumericField label;
	
	KPanel kp;
	
	public OrcaStitchPortPropertyDialog(JFrame parent, OrcaStitchPort c) {
		super(parent, "Port Details", true);
		super.setLocationRelativeTo(parent);
		
		assert(c != null);
		
		setComment("Stitch port " + c.getName() + " properties");
		this.parent = parent;
		this.stitchPort = c;
		name.setObject(c.getName());
		if (c.getLabel() != null)
			label.setText(c.getLabel());
		try {
			if (c.getPort() != null)
				port.setURL(new URL(c.getPort()));
		} catch(Exception e) {
			
		}
	}

	@Override
	public boolean accept() {
		if ((name.getObject().length() == 0) || (!label.validateInput()) || (port.getText().length() == 0))
			return false;
		if (!GUIRequestState.getInstance().nodeCreator.checkUniqueNodeName(stitchPort, name.getObject())) {
			KMessageDialog kmd = new KMessageDialog(parent, "Stitch port name not unique", true);
			kmd.setLocationRelativeTo(parent);
			kmd.setMessage("Stitch port Name " + name.getObject() + " is not unique");
			kmd.setVisible(true);
			return false;
		}
		stitchPort.setName(name.getObject());
		if ((long)label.getValue() > 0)
			stitchPort.setLabel("" + (long)label.getValue());
		else
			stitchPort.setLabel(null);
		stitchPort.setPort(port.getText());
		return true;
	}
	
	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		int y = 0;
		
		GUIRequestState.getInstance().getAvailableDomains();
		
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		kp.setLayout(gbl_contentPanel);
		{
			JLabel lblNewLabel_1 = new JLabel("Name: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = y;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			name = new KTextField(10);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridwidth = 10;
			gbc_list.gridx = 1;
			gbc_list.gridy = y++;
			kp.add(name, gbc_list);
		}
		
		{
			JLabel lblNewLabel_1 = new JLabel("Port URL: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = y;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			port = new URLField(25);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridwidth = 10;
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.gridx = 1;
			gbc_list.gridy = y++;
			kp.add(port, gbc_list);
		}
		
		{
			JLabel lblNewLabel_1 = new JLabel("Label/Tag: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = y;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			label = new NumericField(10);
			label.setMinValue(0);
			label.setMaxValue(4095);
			label.setType(FormatConstants.INTEGER_FORMAT);
			label.setDecimals(0);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridwidth = 10;
			gbc_list.gridx = 1;
			gbc_list.gridy = y++;
			kp.add(label, gbc_list);
		}
		
		return kp;
	}
}
