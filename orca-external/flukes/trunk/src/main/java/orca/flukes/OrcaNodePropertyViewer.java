package orca.flukes;

import javax.swing.JFrame;

import com.hyperrealm.kiwi.ui.KTextArea;

import orca.flukes.ui.TextAreaDialog;

public class OrcaNodePropertyViewer extends TextAreaDialog {
	private OrcaNode node;
	
	public OrcaNodePropertyViewer(JFrame parent, OrcaNode node) {
		super(parent, "View node properties for " + node.getName(), 
					"Node Properties:", 20, 50);
		this.node = node;
		
		KTextArea ta = this.getTextArea();
		
		ta.setText(node.getViewerText());
	}

}
