package orca.flukes.ui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JPasswordField;

import com.hyperrealm.kiwi.ui.KLabel;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;

public class PasswordDialog extends ComponentDialog {
	private KPanel kp;
	private JPasswordField passField;
	
	public PasswordDialog(JFrame parent, String title) {
		super(parent, title, true);
		super.setLocationRelativeTo(parent);
		
		setComment("");
	}
	
	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		kp.setLayout(gbl_contentPanel);
		{
			KLabel kl = new KLabel("Key Password");
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = 0;
			kp.add(kl, gbc_lblNewLabel);
		}
		{
			passField = new JPasswordField(25);
			GridBagConstraints gbc_textField = new GridBagConstraints();
			gbc_textField.fill = GridBagConstraints.HORIZONTAL;
			gbc_textField.insets = new Insets(0, 0, 5, 5);
			gbc_textField.gridwidth = 10;
			gbc_textField.gridx = 1;
			gbc_textField.gridy = 0;
			kp.add(passField, gbc_textField);
		}
		
		return kp;
	}

	@Override
	public boolean accept() {
		char[] pass = passField.getPassword();
		
		if (pass.length == 0)
			return false;
		
		return true;
	}
	
	public String getPassword() {
		return new String(passField.getPassword());
	}
}
