package orca.flukes;

import javax.swing.JFrame;

import com.hyperrealm.kiwi.ui.KTextArea;

import orca.flukes.ui.TextAreaDialog;

public class OrcaColorViewer extends TextAreaDialog {
	private OrcaColor oc;
	
	public OrcaColorViewer(JFrame parent, OrcaColor oc) {
		super(parent, "View color properties for " + oc.getLabel(), 
					"Node Properties:", 20, 50);
		this.oc = oc;
		
		KTextArea ta = this.getTextArea();
		
		ta.setText(oc.getViewerText());
	}
}
