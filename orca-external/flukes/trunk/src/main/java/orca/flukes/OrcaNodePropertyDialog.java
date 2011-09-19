/*
* Copyright (c) 2011 RENCI/UNC Chapel Hill 
*
* @author Ilia Baldine
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
* and/or hardware specification (the "Work") to deal in the Work without restriction, including 
* without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
* sell copies of the Work, and to permit persons to whom the Work is furnished to do so, subject to 
* the following conditions:  
* The above copyright notice and this permission notice shall be included in all copies or 
* substantial portions of the Work.  
*
* THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
* OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS 
* IN THE WORK.
*/
package orca.flukes;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import orca.flukes.ui.IpAddrField;
import orca.flukes.ui.TextAreaDialog;

import com.hyperrealm.kiwi.text.FormatConstants;
import com.hyperrealm.kiwi.ui.KButton;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KTextField;
import com.hyperrealm.kiwi.ui.NumericField;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;
import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

@SuppressWarnings("serial")
public class OrcaNodePropertyDialog extends ComponentDialog implements ActionListener, TextAreaDialog.ITextSetter {
	private JFrame parent;
	private OrcaNode node;
	private KPanel kp;
	private GridBagLayout gbl_contentPanel;
	private KButton postBootButton;
	private TextAreaDialog postBootDialog;
	
	private KTextField name;
	private JList imageList, domainList, typeList, dependencyList = null;
	NumericField ns;
	private HashMap<OrcaLink, IpAddrField> ipFields;
	int ycoord;
	
	public OrcaNodePropertyDialog(JFrame parent, OrcaNode n) {
		super(parent, "Node Properties", true);
		super.setLocationRelativeTo(parent);

		assert(n != null);

		if (n.isNode())
			setComment("Node " + n.getName() + " properties");
		else
			setComment("Domain " + n.getName() + " properties");
		this.parent = parent;
		this.node = n;

		ycoord = 1;
		
		typeList = addSelectList(kp, gbl_contentPanel, ycoord++, 
				GUIState.getInstance().getAvailableNodeTypes(), "Select node type: ", false, 3);
		imageList = addSelectList(kp, gbl_contentPanel, ycoord++, 
				GUIState.getInstance().getImageShortNamesWithNone(), "Select image: ", false, 3);
		domainList = addSelectList(kp, gbl_contentPanel, ycoord++, 
				GUIState.getInstance().getAvailableDomains(), "Select domain: ", false, 3);
		// don't show dependency list if not needed
		if (GUIState.getInstance().getAvailableDependencies(node).length > 0)
			dependencyList = addSelectList(kp, gbl_contentPanel, ycoord++, 
					GUIState.getInstance().getAvailableDependencies(node), "Select dependencies: ", true, 5);
		
		name.setObject(n.getName());

		// set what image it is using
		setListSelectedIndex(imageList, GUIState.getInstance().getImageShortNamesWithNone(), n.getImage());

		// set what domain it is assigned to
		setListSelectedIndex(domainList, GUIState.getInstance().getAvailableDomains(), n.getDomain());

		// set node type
		setListSelectedIndex(typeList, GUIState.getInstance().getAvailableNodeTypes(), n.getNodeType());
		
		// set dependencies
		if (dependencyList != null)
			setListSelectedIndices(dependencyList, GUIState.getInstance().getAvailableDependencies(node), node.getDependencyNames());
		
		ipFields = new HashMap<OrcaLink, IpAddrField>();
		
		// if a node, IP fields are meaningful
		addIpFields();
		if (!n.isNode())
			addNumServersField(ycoord++);
		
		// additional property dialog
		// e.g. post boot script
		addPropertyButtons(ycoord++);
	}
	
	private static Set<String> getStringAsSet(String s) {
		Set<String> myset = new HashSet<String>();
		if (s == null)
			return myset;
		
		myset.add(s);
		return myset;
	}
	
	public static void setListSelectedIndex(JList list, String[] options, String item) {
		if (item == null)
			list.setSelectedIndex(0);
		else
			setListSelectedIndices(list, options, getStringAsSet(item));
	}
	
	/**
	 * set the index of the selected item in a list. 
	 * @param list
	 * @param options
	 * @param item
	 */
	public static void setListSelectedIndices(JList list, String[] options, Set<String> items) {
		if (items.size() == 0) {
			return;
		}
		int index = 0;
		int maxIndex = 0;
		for (String i: options) {
			if (items.contains(i)) {
				maxIndex = index;
				list.addSelectionInterval(index, index);
			}
			index++;
		}
		list.ensureIndexIsVisible(maxIndex);
	}
	
	private void inputErrorDialog(String title, String message) {
		KMessageDialog kmd = new KMessageDialog(parent, title, true);
		kmd.setLocationRelativeTo(parent);
		kmd.setMessage(message);
		kmd.setVisible(true);
	}
	
	@Override
	public boolean accept() {
		if (!GUIState.getInstance().checkUniqueNodeName(node, name.getObject())) {
			inputErrorDialog("Node name is not unique", "Node name " + name.getObject() + " is not unique");
			return false;
		}
		
		// run the IP checks
		for (Map.Entry<OrcaLink, IpAddrField> entry: ipFields.entrySet()) {
			if (entry.getValue().fieldEmpty())
				continue;
			String title = "IP address assignment problem with " + entry.getValue().getAddress() + "/" + entry.getValue().getNetmask();
			// check general validity
			if (!entry.getValue().inputValid()) {
				inputErrorDialog(title, "Check address and netmask fields!");
				return false;
			}
			// check that .0 and .255 addresses are not taken
			long startIP = entry.getValue().getAddressAsLong();
			if (startIP != 0) {
				if ((startIP == entry.getValue().getSubnetAsLong()) || (startIP == entry.getValue().getBroadcastAsLong())) {
					inputErrorDialog(title, "Starting address matches subnet or broadcast address!");
	 				return false;
				}
				// if not a node, check that the mask is wide enough to accommodate the requested number of servers
				// check that the last address is not a .255
				if (!node.isNode()) {
					int nodeCount = (int)ns.getValue();
					long endIP = startIP + nodeCount - 1;
					long netmask = entry.getValue().getNetmaskAsLong();
					if (((startIP & netmask) != (endIP & netmask)) || (endIP == entry.getValue().getBroadcastAsLong())) {
						inputErrorDialog(title, "Number of nodes too large for this IP/netmask combination!");
						return false;
					}
				}
			}
		}
		
		// node name
		node.setName(name.getObject());
		
		// image
		node.setImage(GUIState.getNodeImageProper(GUIState.getInstance().getImageShortNamesWithNone()[imageList.getSelectedIndex()]));

		// domain
		node.setDomain(GUIState.getNodeDomainProper(GUIState.getInstance().getAvailableDomains()[domainList.getSelectedIndex()]));

		// node type
		node.setNodeType(GUIState.getNodeTypeProper(GUIState.getInstance().getAvailableNodeTypes()[typeList.getSelectedIndex()]));
		
		// dependencies 
		if (dependencyList != null) {
			Object[] deps = dependencyList.getSelectedValues();
			node.clearDependencies();
			for (Object depName: deps) {
				node.addDependency(GUIState.getInstance().getNodeByName((String)depName));
			}
		}
		
		// get IP addresses from GUI and set the on the node
		for (Map.Entry<OrcaLink, IpAddrField> entry: ipFields.entrySet()) {
			if (entry.getValue().fieldEmpty())
				continue;
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
	
	private void addNumServersField(int y) {
		{
			JLabel lblNewLabel_1 = new JLabel("Number of servers: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = y;
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
			gbc_list.gridy = y;
			kp.add(ns, gbc_list);
		}
	}
	
	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		
		gbl_contentPanel = new GridBagLayout();
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

		return kp;
	}

	static JList addSelectList(KPanel kp, GridBagLayout l, int starty, String[] options, String label, boolean multi, int rows) {
		JList il;
		{
			JLabel lblNewLabel_1 = new JLabel(label);
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = starty;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			il = new JList(options);
			if (multi)
				il.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			else
				il.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			il.setLayoutOrientation(JList.VERTICAL);
			il.setVisibleRowCount(rows);
			JScrollPane scrollPane = new JScrollPane(il);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridx = 1;
			gbc_list.gridy = starty;
			kp.add(scrollPane, gbc_list);
		}
		return il;
	}
	
	private void addPropertyButtons(int y) {
		{
			JLabel lblNewLabel_1 = new JLabel("Additional properties: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = y;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			KPanel innerPanel = new KPanel();
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridx = 1;
			gbc_list.gridy = y;
			kp.add(innerPanel, gbc_list);
		
			postBootButton = new KButton("PostBoot Script");
			postBootButton.setToolTipText("Edit post-boot script");
			postBootButton.setActionCommand("pbscript");
			postBootButton.addActionListener(this);
			innerPanel.add(postBootButton);
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("pbscript")) {
			TextAreaDialog postBootDialog = new TextAreaDialog(parent, this, "Post Boot Script Editor for " + node.getName(), 
					"Enter your post-boot script:", 20, 50);
			postBootDialog.getTextArea().setText(node.getPostBootScript());
			postBootDialog.pack();
			postBootDialog.setVisible(true);
		}
	}

	public void setText(String t) {
		// set text from the post-boot script text area
		if ((t != null) && (t.length() == 0))
			node.setPostBootScript(null);
		node.setPostBootScript(t);
	}
}
