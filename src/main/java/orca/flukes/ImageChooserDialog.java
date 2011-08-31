package orca.flukes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;

import orca.flukes.ui.ChooserWithNewDialog;

@SuppressWarnings("serial")
public class ImageChooserDialog extends ChooserWithNewDialog<String> implements ActionListener {
	
	public ImageChooserDialog(JFrame parent) {
		super(parent, "Images", "Defined Images", GUIState.getInstance().getImageShortNamesIterator());
		super.setLocationRelativeTo(parent);
		setNewActionListener(this);
	}
	
	public void actionPerformed(ActionEvent e) {
		OrcaImageDialog oid = new OrcaImageDialog(GUI.getInstance().getFrame());
		oid.pack();
		GUIState.getInstance().addingNewImage = true;
		oid.setVisible(true);
	}
	
	@Override
	protected boolean accept() {
		GUIState.getInstance().addingNewImage = false;
		String st = getSelectedItem();
		
		// open image dialog with image details
		if (st != null) {
			OrcaImageDialog oid = new OrcaImageDialog(GUI.getInstance().getFrame());
			oid.setFields(st, GUIState.getInstance().definedImages.get(st).getUrl(), 
					GUIState.getInstance().definedImages.get(st).getHash());
			oid.pack();
			oid.setVisible(true);
		}
		
		return true;
	}
	
	@Override
	protected void cancel() {
		super.cancel();
		GUIState.getInstance().addingNewImage = false;
	}
}
