package orca.flukes;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import com.hyperrealm.kiwi.text.FormatConstants;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KTextField;
import com.hyperrealm.kiwi.ui.NumericField;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;
import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

public class OrcaStoragePropertyDialog extends ComponentDialog {
	JFrame parent;
	OrcaStorageNode storageNode;
	protected KTextField name;
	protected NumericField capacityField;
	private JList domainList;
	private GridBagLayout gbl_contentPanel;
	
	KPanel kp;
	
	public OrcaStoragePropertyDialog(JFrame parent, OrcaStorageNode c) {
		super(parent, "Storage Details", true);
		super.setLocationRelativeTo(parent);
		
		assert(c != null);
		
		setComment("Storage " + c.getName() + " properties");
		this.parent = parent;
		this.storageNode = c;
		name.setObject(c.getName());
		capacityField.setText(c.getCapacity() + "");
		
		int ycoord = 2;
		domainList = OrcaNodePropertyDialog.addSelectList(kp, gbl_contentPanel, ycoord++, 
				GUIRequestState.getInstance().getAvailableDomains(), "Select domain: ", false, 3);
		
		// set what domain it is assigned to
		OrcaNodePropertyDialog.setListSelectedIndex(domainList, GUIRequestState.getInstance().getAvailableDomains(), c.getDomain());
	}
	
	@Override
	public boolean accept() {
		if ((name.getObject().length() == 0) || (!capacityField.validateInput()))
			return false;
		if (!GUIRequestState.getInstance().nodeCreator.checkUniqueNodeName(storageNode, name.getObject())) {
			KMessageDialog kmd = new KMessageDialog(parent, "Storage name not unique", true);
			kmd.setLocationRelativeTo(parent);
			kmd.setMessage("Storage Name " + name.getObject() + " is not unique");
			kmd.setVisible(true);
			return false;
		}
		storageNode.setName(name.getObject().trim());
		storageNode.setCapacity((long)capacityField.getValue());
		// domain
		storageNode.setDomainWithGlobalReset(GUIRequestState.getNodeDomainProper(GUIRequestState.getInstance().getAvailableDomains()[domainList.getSelectedIndex()]));
		return true;
	}
	
	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		int y = 0;
		
		GUIRequestState.getInstance().getAvailableDomains();
		
		gbl_contentPanel = new GridBagLayout();
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
			JLabel lblNewLabel_1 = new JLabel("Capacity: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = y;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			capacityField = new NumericField(10);
			capacityField.setMinValue(0);
			capacityField.setType(FormatConstants.INTEGER_FORMAT);
			capacityField.setDecimals(0);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridwidth = 10;
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.gridx = 1;
			gbc_list.gridy = y++;
			kp.add(capacityField, gbc_list);
		}
		
		return kp;
	}
	
}
