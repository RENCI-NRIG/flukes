package orca.flukes;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;

import com.hyperrealm.kiwi.text.FormatConstants;
import com.hyperrealm.kiwi.ui.KCheckBox;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KTextField;
import com.hyperrealm.kiwi.ui.NumericField;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;
import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

public class OrcaStoragePropertyDialog extends ComponentDialog {
	JFrame parent;
	OrcaStorageNode storageNode;
	protected KTextField name, fstype, fsparam, fsmntpoint;
	protected KCheckBox formatCb;
	protected NumericField capacityField;
	private JList domainList;
	private GridBagLayout gbl_contentPanel;
	protected boolean doFormat;
	private ComponentDialog dialog;
	
	KPanel kp;
	
	public OrcaStoragePropertyDialog(JFrame parent, OrcaStorageNode c) {
		super(parent, "Storage Details", true);
		super.setLocationRelativeTo(parent);
		
		assert(c != null);
		
		this.dialog = this;
		
		setComment("Storage " + c.getName() + " properties");
		this.parent = parent;
		this.storageNode = c;
		name.setObject(c.getName());
		capacityField.setText(c.getCapacity() + "");
		doFormat = c.getDoFormat();
		formatCb.setSelected(doFormat);
		fstype.setObject(c.getFSType());
		fsparam.setObject(c.getFSParam());
		fsmntpoint.setObject(c.getMntPoint());
		
		
		// set what domain it is assigned to
		OrcaNodePropertyDialog.setListSelectedIndex(domainList, GUIDomainState.getInstance().getAvailableDomains(), c.getDomain());
	}
	
	@Override
	public boolean accept() {
		if ((name.getObject().length() == 0) || (!capacityField.validateInput()) || 
				(fstype.getObject().length() == 0) || (fsparam.getObject().length() == 0) || (fsmntpoint.getObject().length() == 0))
			return false;
		if (!GUIUnifiedState.getInstance().nodeCreator.checkUniqueNodeName(storageNode, name.getObject())) {
			KMessageDialog kmd = new KMessageDialog(parent, "Storage name not unique", true);
			kmd.setLocationRelativeTo(parent);
			kmd.setMessage("Storage Name " + name.getObject() + " is not unique");
			kmd.setVisible(true);
			return false;
		}
		storageNode.setName(name.getObject().trim());
		storageNode.setCapacity((long)capacityField.getValue());
		// domain
		storageNode.setDomainWithGlobalReset(GUIDomainState.getNodeDomainProper(GUIDomainState.getInstance().getAvailableDomains()[domainList.getSelectedIndex()]));
		// fs stuff
		storageNode.setFS(fstype.getObject(), fsparam.getObject(), fsmntpoint.getObject());
		storageNode.setDoFormat(doFormat);
		return true;
	}
	
	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		int y = 0;
		
		GUIDomainState.getInstance().getAvailableDomains();
		
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
		
		domainList = OrcaNodePropertyDialog.addSelectList(kp, gbl_contentPanel, y++, 
				GUIDomainState.getInstance().getAvailableDomains(), "Select domain: ", false, 3);

		
		{
			JLabel lblNewLabel_1 = new JLabel("Capacity (GB): ");
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
		
		{
			JLabel lblNewLabel_1 = new JLabel("Filesystem Type: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = y;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			fstype = new KTextField(10);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridwidth = 10;
			gbc_list.gridx = 1;
			gbc_list.gridy = y++;
			kp.add(fstype, gbc_list);
		}
		
		{
			JLabel lblNewLabel_1 = new JLabel("Filesystem format parameters: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = y;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			fsparam = new KTextField(10);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridwidth = 10;
			gbc_list.gridx = 1;
			gbc_list.gridy = y++;
			kp.add(fsparam, gbc_list);
		}
		
		{
			JLabel lblNewLabel_1 = new JLabel("Filesystem mount point: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = y;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			fsmntpoint = new KTextField(10);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridwidth = 10;
			gbc_list.gridx = 1;
			gbc_list.gridy = y++;
			kp.add(fsmntpoint, gbc_list);
		}
		
		{
			JLabel lblNewLabel_1 = new JLabel("Format filesystem: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = y;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			formatCb = new KCheckBox(new AbstractAction() {
				
				public void actionPerformed(ActionEvent e) {
					doFormat = !doFormat;
					dialog.pack();
				}
			});
			formatCb.setSelected(doFormat);
			GridBagConstraints gbc_tf= new GridBagConstraints();
			gbc_tf.anchor = GridBagConstraints.WEST;
			gbc_tf.insets = new Insets(0, 0, 5, 5);
			gbc_tf.gridx = 1;
			gbc_tf.gridy = y;
			kp.add(formatCb, gbc_tf);
		}
		
		return kp;
	}
	
}
