package orca.flukes;


import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;

import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KTextField;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;

public class KeyValueDialog extends ComponentDialog {
	private Map<String, String> keys;
	private String keyOrig;
	private KPanel kp;
	private KTextField keyField, valField;
	private GridBagLayout gbl_contentPanel;
	private JFrame parent;
	private DefaultListModel dlm;
	
	public KeyValueDialog(JFrame parent, String key, Map<String, String> keys) {
		super(parent, "Key/Value pair", true);
		super.setLocationRelativeTo(parent);
		
		this.parent = parent;
		this.keys = keys;
		this.keyOrig = key;
		
		if (key != null) {
			keyField.setObject(key);
			valField.setObject(keys.get(key));
		}
	}
	
	public KeyValueDialog(JFrame parent, String key, Map<String, String> keys, DefaultListModel lm) {
		super(parent, "Key/Value pair", true);
		super.setLocationRelativeTo(parent);
		
		this.parent = parent;
		this.keys = keys;
		this.keyOrig = key;
		this.dlm = lm;
		
		if (key != null) {
			keyField.setObject(key);
			valField.setObject(keys.get(key));
		}
	}
			
	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		
		gbl_contentPanel = new GridBagLayout();
		kp.setLayout(gbl_contentPanel);
		{
			JLabel lblNewLabel_1 = new JLabel("Key: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = 0;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			keyField = new KTextField(10);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridx = 1;
			gbc_list.gridy = 0;
			kp.add(keyField, gbc_list);
		}
		
		{
			JLabel lblNewLabel_1 = new JLabel("Value: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = 1;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			valField = new KTextField(10);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridx = 1;
			gbc_list.gridy = 1;
			kp.add(valField, gbc_list);
		}
		
		return kp;

	}
	
	@Override
	public boolean accept() {
		if (!keyField.getObject().matches("[a-zA-Z0-9_\\-]+"))
			return false;
		if (!valField.getObject().matches("[a-zA-Z0-9_\\-]+"))
			return false;
		
		// remove and maybe reinsert
		if (keyOrig != null)
			keys.remove(keyOrig);
		
		keys.put(keyField.getObject(), valField.getObject());
		if (dlm != null)
			dlm.add(0, keyField.getObject() + " = " + valField.getObject());
		
		return true;
	}
}
