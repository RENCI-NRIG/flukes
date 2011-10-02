package orca.flukes;

import javax.swing.JFrame;

import com.hyperrealm.kiwi.ui.KTextArea;

import orca.flukes.ui.TextAreaDialog;

public class OrcaLinkPropertyViewer extends TextAreaDialog {
	private OrcaLink link;
	
	public OrcaLinkPropertyViewer(JFrame parent, OrcaLink link) {
		super(parent, "View link properties for " + link.getName(), 
					"link Properties:", 20, 50);
		this.link = link;
		
		KTextArea ta = this.getTextArea();
		
		ta.setText(link.getViewerText());
	}
}
