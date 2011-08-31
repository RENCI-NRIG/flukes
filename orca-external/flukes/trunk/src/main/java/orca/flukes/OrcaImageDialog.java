package orca.flukes;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URL;

import javax.swing.JFrame;

import com.hyperrealm.kiwi.ui.KLabel;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KTextField;
import com.hyperrealm.kiwi.ui.URLField;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;
import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

/**
 * Dialog for editing image properties (Short name, URL, Hash)
 * @author ibaldin
 *
 */
public class OrcaImageDialog extends ComponentDialog {
	private KTextField shortName, imageHash;
	private URLField imageUrl;
	JFrame parent;

	public OrcaImageDialog(JFrame parent) {
		super(parent, "ORCA Images", true);
		super.setLocationRelativeTo(parent);
		setComment("Enter image name, URL and hash");
		this.parent = parent;
	}
	
	public void setFields(String shortName, URL url, String hash) {
		this.shortName.setObject(shortName);
		this.imageUrl.setObject(url);
		this.imageHash.setObject(hash);
	}
	
	@Override
	protected Component buildDialogUI() {
		KPanel kp = new KPanel();
		
		GridBagLayout gbl_contentPanel = new GridBagLayout();
//		gbl_contentPanel.columnWidths = new int[]{0, 0, 0, 0};
//		gbl_contentPanel.rowHeights = new int[]{0, 0, 0};
//		gbl_contentPanel.columnWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
//		gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		kp.setLayout(gbl_contentPanel);
		{
			KLabel kl = new KLabel("Short name");
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = 0;
			kp.add(kl, gbc_lblNewLabel);
		}
		{
			shortName = new KTextField(25);
			GridBagConstraints gbc_textField = new GridBagConstraints();
			gbc_textField.fill = GridBagConstraints.HORIZONTAL;
			gbc_textField.insets = new Insets(0, 0, 5, 5);
			gbc_textField.gridwidth = 10;
			gbc_textField.gridx = 1;
			gbc_textField.gridy = 0;
			kp.add(shortName, gbc_textField);
		}
		{
			KLabel kl = new KLabel("Image URL");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = 1;
			kp.add(kl, gbc_lblNewLabel_1);
		}
		{
			imageUrl = new URLField(25);
			GridBagConstraints gbc_textField = new GridBagConstraints();
			gbc_textField.fill = GridBagConstraints.HORIZONTAL;
			gbc_textField.fill = GridBagConstraints.HORIZONTAL;
			gbc_textField.gridwidth = 10;
			gbc_textField.gridx = 1;
			gbc_textField.gridy = 1;
			kp.add(imageUrl, gbc_textField);
		}
		{
			KLabel kl = new KLabel("Image hash");
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = 2;
			kp.add(kl, gbc_lblNewLabel);
		}
		{
			imageHash = new KTextField(25);
			GridBagConstraints gbc_textField = new GridBagConstraints();
			gbc_textField.fill = GridBagConstraints.HORIZONTAL;
			gbc_textField.insets = new Insets(0, 0, 5, 5);
			gbc_textField.gridwidth = 10;
			gbc_textField.gridx = 1;
			gbc_textField.gridy = 2;
			kp.add(imageHash, gbc_textField);
		}
		
		return kp;
	}

	private boolean checkField(String f) {
		if ((f != null) && (f.length() != 0))
			return true;
		return false;
	}
	
	@Override
	protected boolean accept() {
		
		if (checkField(shortName.getObject()) && 
				(imageUrl.getObject() != null) && 
				checkField(imageHash.getObject())) {
			// disallow adding under same name
			if (GUIState.getInstance().addingNewImage && GUIState.getInstance().definedImages.containsKey(shortName.getObject())) {
				KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
				kmd.setMessage("Image with short name " + shortName.getObject() + " already exists!");
				kmd.setLocationRelativeTo(parent);
				kmd.setVisible(true);
				return false;
			}
			if (GUIState.getInstance().addingNewImage && shortName.getObject().equals(GUIState.NO_GLOBAL_IMAGE)) {
				KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
				kmd.setMessage("Short name \"None\" is reserved!");
				kmd.setLocationRelativeTo(parent);
				kmd.setVisible(true);
				return false;
			}
			GUIState.getInstance().definedImages.put(shortName.getObject(), 
					new OrcaImage(shortName.getObject(), imageUrl.getObject(), imageHash.getObject()));
			// TODO: repaint the dialog instead of killing it
			GUIState.getInstance().icd.setVisible(false);
			GUIState.getInstance().icd.destroy();
			GUIState.getInstance().addingNewImage = false;
			return true;
		}
		return false;
	}
}
