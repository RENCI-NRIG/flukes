package orca.flukes;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import orca.flukes.ui.IpAddrField;

import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KTextField;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;
import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

import edu.uci.ics.jung.graph.SparseMultigraph;

@SuppressWarnings("serial")
public class OrcaNodePropertyDialog extends ComponentDialog {
	private JFrame parent;
	private OrcaNode node;
	private KPanel kp;
	
	private KTextField name;
	private JList imageList;
	private HashMap<OrcaLink, IpAddrField> ipFields;
	
	public OrcaNodePropertyDialog(JFrame parent, OrcaNode n) {
		super(parent, "Node Properties", true);
		super.setLocationRelativeTo(parent);
		setComment("Node " + n.getName() + " properties");
		this.parent = parent;
		this.node = n;
		if (n != null) {
			int index = 0;
			name.setObject(n.getName());
			for (String i: GUIState.getInstance().getImageShortNamesWithNone()) {
				if (i.equals(n.getImage()))
					break;
				index++;
			}
			if (index == GUIState.getInstance().getImageShortNamesWithNone().length)
				imageList.setSelectedIndex(0);
			else
				imageList.setSelectedIndex(index);
		}
		ipFields = new HashMap<OrcaLink, IpAddrField>();
		addIpFields();
	}
	
	@Override
	public boolean accept() {
		if (!GUIState.getInstance().checkUniqueNodeName(node, name.getObject())) {
			KMessageDialog kmd = new KMessageDialog(parent, "Node name not unique", true);
			kmd.setLocationRelativeTo(parent);
			kmd.setMessage("Node Name " + name.getObject() + " is not unique");
			kmd.setVisible(true);
			return false;
		}
		node.setName(name.getObject());
		node.setImage(GUIState.getInstance().getImageShortNamesWithNone()[imageList.getSelectedIndex()]);
		if (node.getImage().equals(GUIState.NO_GLOBAL_IMAGE))
			node.setImage(null);
		// get IP addresses from GUI and set the on the node
		for (Map.Entry<OrcaLink, IpAddrField> entry: ipFields.entrySet()) {
			node.setIp(entry.getKey(), entry.getValue().getAddress());
		}
		
		return true;
	}
	
	private void addIpFields() {
		int ycoord = 2;
		// query the graph for edges incident on this node and create 
		// labeled IP address fields; populate fields as needed
		Collection<OrcaLink> nodeEdges = GUIState.getInstance().g.getIncidentEdges(node);
		if (nodeEdges == null) {
			return;
		}
		for (OrcaLink e: nodeEdges) {
			{
				JLabel lblNewLabel_1 = new JLabel(e.getName() + " IP Address: ");
				GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
				gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
				gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
				gbc_lblNewLabel_1.gridx = 0;
				gbc_lblNewLabel_1.gridy = ycoord;
				kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
			}
			{
				IpAddrField ipf = new IpAddrField();
				ipf.setAddress(node.getIp(e));
				GridBagConstraints gbc_list = new GridBagConstraints();
				gbc_list.insets = new Insets(0, 0, 5, 5);
				gbc_list.fill = GridBagConstraints.HORIZONTAL;
				gbc_list.gridx = 1;
				gbc_list.gridy = ycoord;
				kp.add(ipf, gbc_list);
				ipFields.put(e, ipf);
			}
			ycoord++;
		}
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
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridx = 1;
			gbc_list.gridy = 0;
			kp.add(name, gbc_list);
		}
		{
			JLabel lblNewLabel_1 = new JLabel("Select image: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = 1;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			imageList = new JList(GUIState.getInstance().getImageShortNamesWithNone());
			imageList.setSelectedIndex(0);
			imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			imageList.setLayoutOrientation(JList.VERTICAL);
			imageList.setVisibleRowCount(1);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridx = 1;
			gbc_list.gridy = 1;
			kp.add(imageList, gbc_list);
		}
		
		return kp;
	}

}
