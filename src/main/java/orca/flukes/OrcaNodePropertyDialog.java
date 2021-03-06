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
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import orca.flukes.ndl.RequestSaver;
import orca.flukes.ui.IpAddrField;
import orca.flukes.ui.TextAreaDialog;

import com.hyperrealm.kiwi.text.FormatConstants;
import com.hyperrealm.kiwi.ui.KButton;
import com.hyperrealm.kiwi.ui.KCheckBox;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KTextField;
import com.hyperrealm.kiwi.ui.NumericField;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;
import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

@SuppressWarnings("serial")
public class OrcaNodePropertyDialog extends ComponentDialog implements ActionListener, TextAreaDialog.ITextSetter, ListSelectionListener {
	private JFrame parent;
	private OrcaNode node;
	private KPanel kp;
	private GridBagLayout gbl_contentPanel;
	private KButton postBootButton;
	
	private KTextField name;
	private KTextField openPortsList;
	private JList imageList, domainList, typeList, dependencyList = null;
	private KCheckBox splittableCb;
	//private KCheckBox internalVlanCb;
	private boolean splittableState = false;
	
	// NOTE: All internal VLAN stuff removed - Broadcast links should be used instead 05/24/12 /ib
	//private boolean internalVlanState = false;
	//private NumericField internalVlanBwField = null;
	//private NumericField internalVlanLabel = null;
	// address field for group's internal address
	//private IpAddrField internalIpf = null;
	//JLabel internalVlanBwLabel = null, internalVlanIpLabel = null, internalVlanLabelLabel = null;
	
	private NumericField ns;
	private HashMap<OrcaLink, IpAddrField> ipFields;

	protected int ycoord;
	// we're doing a closure AbstractAction for checkbox and it needs access to 'this'
	// without calling it 'this'
	private ComponentDialog dialog;
	
	public OrcaNodePropertyDialog(JFrame parent, OrcaNode n) {
		super(parent, "Node Properties", true);
		super.setLocationRelativeTo(parent);

		this.dialog = this;
		
		assert(n != null);

		if (n instanceof OrcaNodeGroup)
			setComment("Group " + n.getName() + " properties");
		else
			setComment("Node " + n.getName() + " properties");
		
		this.parent = parent;
		this.node = n;

		ycoord = 1;
		
		typeList = addSelectList(kp, gbl_contentPanel, ycoord++, 
				GUIUnifiedState.getInstance().getAvailableNodeTypes(), "Select node type: ", false, 3);
		typeList.addListSelectionListener(this);
		
		imageList = addSelectList(kp, gbl_contentPanel, ycoord++, 
				GUIImageList.getInstance().getImageShortNamesWithNone(), "Select image: ", false, 3);
		
		domainList = addSelectList(kp, gbl_contentPanel, ycoord++, 
				GUIDomainState.getInstance().getAvailableDomains(), "Select domain: ", false, 3);
		
		if (n instanceof OrcaNodeGroup) {
			OrcaNodeGroup ong = (OrcaNodeGroup)n;
			addSplittableCheck(ong.getSplittable(), ycoord++);
		}
		
		// don't show dependency list if not needed
		dependencyList = addSelectList(kp, gbl_contentPanel, ycoord++, 
				GUIUnifiedState.getInstance().getAvailableDependenciesWithNone(node), "Select dependencies: ", true, 5);
		
		name.setObject(n.getName());

		// set what image it is using
		setListSelectedIndex(imageList, GUIImageList.getInstance().getImageShortNamesWithNone(), n.getImage());

		// set what domain it is assigned to
		setListSelectedIndex(domainList, GUIDomainState.getInstance().getAvailableDomains(), n.getDomain());

		// set node type
		setListSelectedIndex(typeList, GUIUnifiedState.getInstance().getAvailableNodeTypes(), n.getNodeType());
		
		// set dependencies
		setListSelectedIndices(dependencyList, GUIUnifiedState.getInstance().getAvailableDependenciesWithNone(node), node.getDependencyNames());
		
		// list of open ports on management network
		// not used for now /ib 05/10/2013
		//addOpenPortsField(ycoord++);
		
		ipFields = new HashMap<OrcaLink, IpAddrField>();
		
		// if a node, IP fields are meaningful
		addIpFields();
		
		// number of servers and splittable 
		if (n instanceof OrcaNodeGroup) {
			addNumServersField(ycoord++);
		}
		
		// additional property dialog
		// e.requestGraph. post boot script
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
		if ((items == null) || (items.size() == 0)) {
			list.addSelectionInterval(0, 0);
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
	
	private boolean checkIPField(IpAddrField ipf) {
		
		String title = "IP address assignment problem with " + ipf.getAddress() + "/" + ipf.getNetmask();
		// check general validity
		if (!ipf.inputValid()) {
			inputErrorDialog(title, "Check address and netmask fields!");
			return false;
		}
		// check that .0 and .255 addresses are not taken
		long startIP = ipf.getAddressAsLong();
		if (startIP != 0) {
			if ((startIP == ipf.getSubnetAsLong()) || (startIP == ipf.getBroadcastAsLong())) {
				inputErrorDialog(title, "Starting address matches subnet or broadcast address!");
 				return false;
			}
			// if not a node, check that the mask is wide enough to accommodate the requested number of servers
			// check that the last address is not a .255
			if (node instanceof OrcaNodeGroup) {
				int nodeCount = (int)ns.getValue();
				long endIP = startIP + nodeCount - 1;
				long netmask = ipf.getNetmaskAsLong();
				if (((startIP & netmask) != (endIP & netmask)) || (endIP == ipf.getBroadcastAsLong())) {
					inputErrorDialog(title, "Number of nodes too large for this IP/netmask combination!");
					return false;
				}
			}
		}
		return true;
	}
	
	@Override
	public boolean accept() {
		if (!GUIUnifiedState.getInstance().nodeCreator.checkUniqueNodeName(node, name.getObject())) {
			inputErrorDialog("Node name is not unique", "Node name " + name.getObject() + " is not unique.");
			return false;
		}
		
		// run the IP checks
		for (Map.Entry<OrcaLink, IpAddrField> entry: ipFields.entrySet()) {
			if (entry.getValue().fieldEmpty())
				continue;
			if (!checkIPField(entry.getValue())) {
				inputErrorDialog("Check the IP addresses", "Check IP address specification.");
				return false;
			}
		}
		
//		if (!node.setPortsList(openPortsList.getObject())) {
//			inputErrorDialog("Check port list specification", "Check the port list specification for this node.");
//			return false;
//		}
		
		// node name
		node.setName(name.getObject());
		
		// image
		node.setImage(GUIImageList.getNodeImageProper(GUIImageList.getInstance().getImageShortNamesWithNone()[imageList.getSelectedIndex()]));

		// domain
		if (node instanceof OrcaNodeGroup) {
			// for splittable groups domain is meaningless
			OrcaNodeGroup ong = (OrcaNodeGroup)node;
			ong.setSplittable(splittableState);
			if (splittableState)
				// set to system select
				node.setDomainWithGlobalReset(GUIDomainState.getNodeDomainProper(GUIDomainState.getInstance().getAvailableDomains()[0]));
			else
				node.setDomainWithGlobalReset(GUIDomainState.getNodeDomainProper(GUIDomainState.getInstance().getAvailableDomains()[domainList.getSelectedIndex()]));
		} else {
			// domain
			node.setDomainWithGlobalReset(GUIDomainState.getNodeDomainProper(GUIDomainState.getInstance().getAvailableDomains()[domainList.getSelectedIndex()]));
		}

		// node type
		node.setNodeType(GUIUnifiedState.getNodeTypeProper(GUIUnifiedState.getInstance().getAvailableNodeTypes()[typeList.getSelectedIndex()]));
		
		// dependencies 
		Object[] deps = dependencyList.getSelectedValues();
		node.clearDependencies();
		for (Object depName: deps) {
			if (!GUIUnifiedState.NO_NODE_DEPS.equals(depName))
				node.addDependency(GUIUnifiedState.getInstance().getNodeByName((String)depName));
		}
		
		// get IP addresses from GUI and set the on the node
		for (Map.Entry<OrcaLink, IpAddrField> entry: ipFields.entrySet()) {
			if (entry.getValue().fieldEmpty()) {
				node.setIp(entry.getKey(), null, null);
				continue;
			}
			node.setIp(entry.getKey(), entry.getValue().getAddress(), entry.getValue().getNetmask());
		}
		
		// if node group, set node count
		if (node instanceof OrcaNodeGroup) {
			OrcaNodeGroup ong = (OrcaNodeGroup)node;
			ong.setNodeCount((int)ns.getValue());
		}
		
		return true;
	}
	
	private void addIpFields() {
		// query the graph for edges incident on this node and create 
		// labeled IP address fields; populate fields as needed
		Collection<OrcaLink> nodeEdges = GUIUnifiedState.getInstance().g.getIncidentEdges(node);
		if (nodeEdges == null) {
			return;
		}
		for (OrcaLink e: nodeEdges) {
			if (e instanceof OrcaColorLink)
				continue;
			if (e.linkToSharedStorage())
				continue;
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
			OrcaNodeGroup ong = (OrcaNodeGroup)node;
			ns.setValue(ong.getNodeCount());
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridx = 1;
			gbc_list.gridy = y;
			kp.add(ns, gbc_list);
		}
	}
	
	private void addOpenPortsField(int y) {
		{
			JLabel lblNewLabel_1 = new JLabel("Additional ports to open (TCP and UDP): ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = y;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			openPortsList = new KTextField();
			openPortsList.setObject(node.getPortsList());
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridx = 1;
			gbc_list.gridy = y;
			kp.add(openPortsList, gbc_list);
		}
	}
	
	private void addSplittableCheck(boolean s, int y) {
		splittableState = s;
		domainList.setVisible(!splittableState);
		{
			JLabel lblNewLabel_1 = new JLabel("Splittable between domains: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = y;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			splittableCb = new KCheckBox(new AbstractAction() {
				
				public void actionPerformed(ActionEvent e) {
					// toggle list of domains - if splittable,
					// list is meaningless
					splittableState = !splittableState;
					domainList.setVisible(!splittableState);
					dialog.pack();
				}
			});
			splittableCb.setSelected(splittableState);
			GridBagConstraints gbc_tf= new GridBagConstraints();
			gbc_tf.anchor = GridBagConstraints.WEST;
			gbc_tf.insets = new Insets(0, 0, 5, 5);
			gbc_tf.gridx = 1;
			gbc_tf.gridy = y;
			kp.add(splittableCb, gbc_tf);
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

	public static JList addSelectList(KPanel kp, GridBagLayout l, int starty, String[] options, String label, boolean multi, int rows) {
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
	
	public static JList addSelectList(KPanel kp, GridBagLayout l, int starty, ListModel options, String label, boolean multi, int rows) {
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
		node.setPostBootScript(RequestSaver.sanitizePostBootScript(t));
	}
	
	// list selection listener interface
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == true)
			return;
		JList l = (JList)e.getSource();
		if (l == typeList) {
			if ((GUIUnifiedState.getInstance().getAvailableNodeTypes()[typeList.getSelectedIndex()].equals(RequestSaver.BAREMETAL)) || 
				(GUIUnifiedState.getInstance().getAvailableNodeTypes()[typeList.getSelectedIndex()].equals(RequestSaver.FORTYGBAREMETAL))){
				imageList.setVisible(false);
				imageList.setSelectedIndex(0);
			} 
			else
				imageList.setVisible(true);
		}
	} 
}
