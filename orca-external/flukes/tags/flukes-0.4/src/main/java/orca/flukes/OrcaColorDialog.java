/*
* Copyright (c) 2013 RENCI/UNC Chapel Hill 
*
* @author Ilya Baldin
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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListModel;

import orca.flukes.ui.TextAreaDialog;

import com.hyperrealm.kiwi.ui.KButton;
import com.hyperrealm.kiwi.ui.KCheckBox;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KTextField;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;

// works for nodes and links, depending on context. Allows defining
// a label, a dictionary and a blob (XML or text)
public class OrcaColorDialog extends ComponentDialog implements ActionListener, TextAreaDialog.ITextSetter {
	private KPanel kp;
	private GridBagLayout gbl_contentPanel;
	private JFrame parent;
	private OrcaColor oc;
	
	private KTextField label;
	private ComponentDialog dialog;
	private JList keyList;
	private KButton addKeyButton, delKeyButton, editBlobButton;
	protected int ycoord;
	private DefaultListModel lm = new DefaultListModel();
	private KCheckBox xmlBlob;
	
	public OrcaColorDialog(JFrame parent, OrcaColorLink e) {
		super(parent, "Color Dependency Details", true);
		super.setLocationRelativeTo(parent);
		
		assert(e != null);
		
		oc = e.getColor();
		assert(oc != null);
		
		commonConstruct(parent);
	}
	
	public OrcaColorDialog(JFrame parent, OrcaResource or, String colorLabel) {
		super(parent, "Resource " + or.getName() + " Color " + colorLabel + " Details", true);
		super.setLocationRelativeTo(parent);
		
		this.oc = or.getColor(colorLabel);
		assert(oc != null);

		commonConstruct(parent);
	}
	
	private void commonConstruct(JFrame parent) {
		this.dialog = this;
		this.parent = parent;
		
		setComment("Color properties");
		
		ycoord = 1;
		
		label.setObject(oc.getLabel());
		
		keyList = OrcaNodePropertyDialog.addSelectList(kp, gbl_contentPanel, ycoord++, getKeyValsModel(), "Key/Value pairs", false, 3);
		
		{
			JLabel lblNewLabel_1 = new JLabel("Manipulate Keys:");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = ycoord;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			KPanel innerPanel = new KPanel();
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridx = 1;
			gbc_list.gridy = ycoord++;
			kp.add(innerPanel, gbc_list);
		
			addKeyButton = new KButton("Add Key");
			addKeyButton.setToolTipText("Add a new key-value pair");
			addKeyButton.setActionCommand("addkey");
			addKeyButton.addActionListener(this);
			innerPanel.add(addKeyButton);
			
			delKeyButton = new KButton("Delete Key");
			delKeyButton.setToolTipText("Delete a key-value pair");
			delKeyButton.setActionCommand("delkey");
			delKeyButton.addActionListener(this);
			innerPanel.add(delKeyButton);
		}
		
		{
			JLabel lblNewLabel_1 = new JLabel("Blob properties:");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = ycoord;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			KPanel innerPanel = new KPanel();
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridx = 1;
			gbc_list.gridy = ycoord;
			kp.add(innerPanel, gbc_list);
		
			editBlobButton = new KButton("Edit a blob");
			editBlobButton.setToolTipText("Edit a text blob");
			editBlobButton.setActionCommand("addblob");
			editBlobButton.addActionListener(this);
			innerPanel.add(editBlobButton);
			
			JLabel lbl = new JLabel("XML:");
			innerPanel.add(lbl);
			
			xmlBlob = new KCheckBox(new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					oc.setXMLBlobState(!oc.getXMLBlobState());
				}
			});
			innerPanel.add(xmlBlob);
			
			xmlBlob.setSelected(oc.getXMLBlobState());
		}
		
	}
	
	@Override
	public boolean accept() {
		if (!label.getObject().matches("[a-zA-Z0-9_\\-]+"))
			return false;
		oc.setLabel(label.getObject());
		
		// key value pairs are maintained separately, and so is the blob
		
		return true;
		
	}
	
	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		
		gbl_contentPanel = new GridBagLayout();
		kp.setLayout(gbl_contentPanel);
		{
			JLabel lblNewLabel_1 = new JLabel("Label: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = 0;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			label = new KTextField(10);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridx = 1;
			gbc_list.gridy = 0;
			kp.add(label, gbc_list);
		}

		return kp;
	}

	// return all key value pairs
	private String[] getKeyVals() {
		Map<String, String> colorKeys = oc.getKeys();
		String[] ret;
		if (colorKeys.size() == 0) {
			return new String[0];
		}
		ret = new String[colorKeys.size()];
		
		Iterator<Entry<String, String>> it = colorKeys.entrySet().iterator();
		int i = 0;
		while(it.hasNext()) {
			Entry<String, String> e = it.next();
			ret[i++] = e.getKey() + " = " + e.getValue();
		}
		return ret;		
	}
	
	private ListModel getKeyValsModel() {
		
		Map<String, String> colorKeys = oc.getKeys();
		Iterator<Entry<String, String>> it = colorKeys.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, String> e = it.next();
			lm.addElement(e.getKey() + " = " + e.getValue());
		}
		return lm;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("addkey")) {
			KeyValueDialog kvd = new KeyValueDialog(parent, null, oc.getKeys(), lm);
			kvd.pack();
			kvd.setVisible(true);
		} else if (e.getActionCommand().equals("delkey")) {
			int index = keyList.getSelectedIndex();
			if (index >= 0) {
				String deleted = (String)lm.remove(index);
				oc.delKey(deleted.split("=")[0].trim());
			}
		} else if (e.getActionCommand().equals("addblob")) {
			TextAreaDialog blobDialog = new TextAreaDialog(parent, this, "Text Blob ", 
					"Enter your text blob:", 20, 50);
			blobDialog.getTextArea().setText(oc.getBlob());
			blobDialog.pack();
			blobDialog.setVisible(true);
		}
	}

	@Override
	public void setText(String t) {
		// set text from the post-boot script text area
		if ((t != null) && (t.length() == 0))
			oc.setBlob(null);
		else
			oc.setBlob(t);
	}
	
}
