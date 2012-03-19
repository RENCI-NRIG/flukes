package orca.flukes.ui;

import javax.swing.JFrame;

import com.hyperrealm.kiwi.ui.dialog.LoginDialog;

@SuppressWarnings("serial")
public class KeystoreDialog extends LoginDialog {

	public KeystoreDialog(JFrame parent, String title) {
		super(parent, title);
		super.setLocationRelativeTo(parent);	
		
		setUsernamePromptText("Enter key alias in keystore");
		setPasswordPromptText("Enter key/keystore password");
	}
	
	public String getAlias() {
		return this.t_user.getText();
	}
	
	public String getPassword() {
		return new String(this.t_passwd.getPassword());
	}
	
}
