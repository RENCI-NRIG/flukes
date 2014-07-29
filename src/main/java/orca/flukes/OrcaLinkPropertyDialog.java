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

import javax.swing.JFrame;
import javax.swing.JLabel;

import com.hyperrealm.kiwi.text.FormatConstants;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KTextField;
import com.hyperrealm.kiwi.ui.NumericField;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;
import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

public class OrcaLinkPropertyDialog extends ComponentDialog {
	JFrame parent;
	OrcaLink edge;
	
	protected NumericField bandwidth, latency, label;
	protected KTextField name;
	
	KPanel kp;
	
	public OrcaLinkPropertyDialog(JFrame parent, OrcaLink e) {
		super(parent, "Edge Details", true);
		super.setLocationRelativeTo(parent);
		
		assert(e != null);
		
		setComment("Edge " + e.getName() + " properties");
		this.parent = parent;
		this.edge = e;
		name.setObject(e.getName());
		bandwidth.setValue(e.getBandwidth());
//		latency.setValue(e.getLatency());
		if (e.getLabel() != null)
			label.setValue(Integer.parseInt(e.getLabel()));
	}

	@Override
	public boolean accept() {
		//if ((name.getObject().length() == 0) || (!bandwidth.validateInput()) || (!latency.validateInput()) || (!label.validateInput()))
		if ((name.getObject().length() == 0) || (!bandwidth.validateInput()) || (!label.validateInput()))
			return false;
		if (!GUIRequestState.getInstance().linkCreator.checkUniqueLinkName(edge, name.getObject())) {
			KMessageDialog kmd = new KMessageDialog(parent, "Link name not unique", true);
			kmd.setLocationRelativeTo(parent);
			kmd.setMessage("Link Name " + name.getObject() + " is not unique");
			kmd.setVisible(true);
			return false;
		}
		edge.setName(name.getObject());
		edge.setBandwidth((long)bandwidth.getValue());
		//edge.setLatency((long)latency.getValue());
		if ((long)label.getValue() > 0)
			edge.setLabel("" + (long)label.getValue());
		else
			edge.setLabel(null);
		
		return true;
	}
	
	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		int y = 0;
		GridBagLayout gbl_contentPanel = new GridBagLayout();
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
			gbc_list.fill = GridBagConstraints.WEST;
			gbc_list.gridx = 1;
			gbc_list.gridy = y++;
			kp.add(name, gbc_list);
		}
		
		
		{
			JLabel lblNewLabel_1 = new JLabel("Bandwidth (bps): ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = y;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			bandwidth = new NumericField(10);
			bandwidth.setMinValue(0);
			bandwidth.setType(FormatConstants.LONG);
			bandwidth.setDecimals(0);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.WEST;
			gbc_list.gridx = 1;
			gbc_list.gridy = y++;
			kp.add(bandwidth, gbc_list);
		}
		
//		{
//			JLabel lblNewLabel_1 = new JLabel("Latency: ");
//			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
//			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
//			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
//			gbc_lblNewLabel_1.gridx = 0;
//			gbc_lblNewLabel_1.gridy = y;
//			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
//		}
//		{
//			latency = new NumericField(10);
//			latency.setMinValue(0);
//			latency.setType(FormatConstants.LONG);
//			latency.setDecimals(0);
//			GridBagConstraints gbc_list = new GridBagConstraints();
//			gbc_list.insets = new Insets(0, 0, 5, 5);
//			gbc_list.fill = GridBagConstraints.WEST;
//			gbc_list.gridx = 1;
//			gbc_list.gridy = y++;
//			kp.add(latency, gbc_list);
//		}
		
		{
			JLabel lblNewLabel_1 = new JLabel("Label/Tag: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = y;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			label = new NumericField(10);
			label.setMinValue(0);
			label.setMaxValue(4095);
			label.setType(FormatConstants.INTEGER_FORMAT);
			label.setDecimals(0);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.WEST;
			gbc_list.gridx = 1;
			gbc_list.gridy = y++;
			kp.add(label, gbc_list);
		}
		
		return kp;
	}

}
