package orca.flukes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import orca.flukes.ui.ChooserWithNewDialog;

@SuppressWarnings("serial")
public class ImageChooserDialog extends ChooserWithNewDialog<String> implements ActionListener {
	
	public ImageChooserDialog() {
		super(GUI.getInstance().getFrame(), "Images", "Defined Images", GUI.getInstance().definedImages.keySet().iterator());
		super.setLocationRelativeTo(GUI.getInstance().getFrame());
		setNewActionListener(this);
	}
	
	public void actionPerformed(ActionEvent e) {
		OrcaImageDialog oid = new OrcaImageDialog(GUI.getInstance().getFrame());
		oid.pack();
		GUI.getInstance().addingNewImage = true;
		oid.setVisible(true);
	}
	
	@Override
	protected boolean accept() {
		GUI.getInstance().addingNewImage = false;
		String st = getSelectedItem();
		
		// open image dialog with image details
		if (st != null) {
			OrcaImageDialog oid = new OrcaImageDialog(GUI.getInstance().getFrame());
			oid.setFields(st, GUI.getInstance().definedImages.get(st).getUrl(), 
					GUI.getInstance().definedImages.get(st).getHash());
			oid.pack();
			oid.setVisible(true);
		}
		
		return true;
	}
	
	protected void cancel() {
		super.cancel();
		GUI.getInstance().addingNewImage = false;
	}
}
