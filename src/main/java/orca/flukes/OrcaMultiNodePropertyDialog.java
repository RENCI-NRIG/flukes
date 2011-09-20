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
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;

@SuppressWarnings("serial")
public class OrcaMultiNodePropertyDialog extends ComponentDialog {
	private JFrame parent;
	private Set<OrcaNode> nodes = null;
	private KPanel kp;
	
	private JList imageList, domainList;
	
	public OrcaMultiNodePropertyDialog(JFrame parent, Set<OrcaNode> ns) {
		super(parent, "Shared Node Properties", true);
		setComment("Edit shared properties of selected nodes");
		super.setLocationRelativeTo(parent);
		this.parent = parent;
		this.nodes = ns;
	}
	
	@Override
	public boolean accept() {
		for (OrcaNode node: nodes) {
			// image
			node.setImage(GUIRequestState.getNodeImageProper(GUIRequestState.getInstance().getImageShortNamesWithNone()[imageList.getSelectedIndex()]));
			// domain
			node.setDomain(GUIRequestState.getNodeDomainProper(GUIRequestState.getInstance().getAvailableDomains()[domainList.getSelectedIndex()]));
		}
		return true;
	}


	
	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		kp.setLayout(gbl_contentPanel);
		
		imageList = OrcaNodePropertyDialog.addSelectList(kp, gbl_contentPanel, 0, 
				GUIRequestState.getInstance().getImageShortNamesWithNone(), "Select image: ", false, 3);
		domainList = OrcaNodePropertyDialog.addSelectList(kp, gbl_contentPanel, 1,
				GUIRequestState.getInstance().getAvailableDomains(), "Select domain: ", false, 3);
		
		return kp;
	}

}
