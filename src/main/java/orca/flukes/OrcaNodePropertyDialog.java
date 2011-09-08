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

import com.hyperrealm.kiwi.text.FormatConstants;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KTextField;
import com.hyperrealm.kiwi.ui.NumericField;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;
import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

@SuppressWarnings("serial")
public class OrcaNodePropertyDialog extends ComponentDialog {
	private JFrame parent;
	private OrcaNode node;
	private KPanel kp;
	
	private KTextField name;
	private JList imageList, domainList;
	NumericField ns;
	private HashMap<OrcaLink, IpAddrField> ipFields;
	int ycoord;
	
	public OrcaNodePropertyDialog(JFrame parent, OrcaNode n) {
		super(parent, "Node Properties", true);
		super.setLocationRelativeTo(parent);
		if (n.isNode())
			setComment("Node " + n.getName() + " properties");
		else
			setComment("Domain " + n.getName() + " properties");
		this.parent = parent;
		this.node = n;
		if (n != null) {
			name.setObject(n.getName());
			// set what image it is using
			int index = 0;
			for (String i: GUIState.getInstance().getImageShortNamesWithNone()) {
				if (i.equals(n.getImage()))
					break;
				index++;
			}
			if (index == GUIState.getInstance().getImageShortNamesWithNone().length)
				imageList.setSelectedIndex(0);
			else
				imageList.setSelectedIndex(index);
			// set what domain it is assigned to
			index = 0;
			for (String i: GUIState.getInstance().getAvailableDomains()) {
				if (i.equals(n.getDomain()))
					break;
				index++;
			}
			if (index == GUIState.getInstance().getAvailableDomains().length)
				domainList.setSelectedIndex(0);
			else
				domainList.setSelectedIndex(index);
		}
		ipFields = new HashMap<OrcaLink, IpAddrField>();
		
		ycoord = 3;
		// if a node, IP fields are meaningful
		addIpFields();
		if (!n.isNode())
			addNumServersField();
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
		
		// image
		node.setImage(GUIState.getNodeImageProper(GUIState.getInstance().getImageShortNamesWithNone()[imageList.getSelectedIndex()]));

		// domain
		node.setDomain(GUIState.getNodeDomainProper(GUIState.getInstance().getAvailableDomains()[domainList.getSelectedIndex()]));

		// get IP addresses from GUI and set the on the node
		for (Map.Entry<OrcaLink, IpAddrField> entry: ipFields.entrySet()) {
			node.setIp(entry.getKey(), entry.getValue().getAddress(), entry.getValue().getNetmask());
		}
		
		// if cluster, set node count
		if (!node.isNode()) {
			node.setNodeCount((int)ns.getValue());
		}
		
		return true;
	}
	
	private void addIpFields() {
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
				IpAddrField ipf;
				ipf = new IpAddrField();
				ipf.setAddress(node.getIp(e), node.getNm(e));
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
	
	private void addNumServersField() {
		{
			JLabel lblNewLabel_1 = new JLabel("Number of servers: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = ycoord;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			ns = new NumericField(5);
			ns.setDecimals(0);
			ns.setType(FormatConstants.INTEGER_FORMAT);
			ns.setMinValue(1);
			ns.setValue(node.getNodeCount());
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridx = 1;
			gbc_list.gridy = ycoord;
			kp.add(ns, gbc_list);
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
		
		imageList = addImageList(kp, gbl_contentPanel, 1);
		domainList = addDomainList(kp, gbl_contentPanel, 2);
		
		return kp;
	}

	/**
	 * Add an image list element (usable in other classes)
	 * @param kp
	 * @param l
	 * @param starty
	 * @return
	 */
	static JList addImageList(KPanel kp, GridBagLayout l, int starty) {
		JList il;
		{
			JLabel lblNewLabel_1 = new JLabel("Select image: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = starty;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			il = new JList(GUIState.getInstance().getImageShortNamesWithNone());
			il.setSelectedIndex(0);
			il.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			il.setLayoutOrientation(JList.VERTICAL);
			il.setVisibleRowCount(1);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridx = 1;
			gbc_list.gridy = starty;
			kp.add(il, gbc_list);
		}
		return il;
	}
	
	/**
	 * Add a domain list element (usable in other classes)
	 * @param kp
	 * @param l
	 * @param starty
	 * @return
	 */
	static JList addDomainList(KPanel kp, GridBagLayout l, int starty) {
		JList domList;
		{
			JLabel lblNewLabel_1 = new JLabel("Select domain: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = starty;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			domList = new JList(GUIState.getInstance().getAvailableDomains());
			domList.setSelectedIndex(0);
			domList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			domList.setLayoutOrientation(JList.VERTICAL);
			domList.setVisibleRowCount(1);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridx = 1;
			gbc_list.gridy = starty;
			kp.add(domList, gbc_list);
		}
		return domList;
	}
	
}
