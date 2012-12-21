package orca.flukes;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;

import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;

public class OrcaSliceList extends ComponentDialog {
	private KPanel kp;
	private JList sliceList;
	private GridBagLayout gbl_contentPanel;
	private JButton del;
	
	public OrcaSliceList(JFrame parent, String[] slices) {
		super(parent, "View current slices.", false);
		
		super.setLocationRelativeTo(parent);
		setComment("Current slices:");
		
		sliceList = OrcaNodePropertyDialog.addSelectList(kp, gbl_contentPanel, 0, 
				slices, "Select slice: ", false, 5);
		
		sliceList.setPrototypeCellValue("012345678901234567890123456789");
	}

	
	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		gbl_contentPanel = new GridBagLayout();
		kp.setLayout(gbl_contentPanel);

		setAcceptButtonText("Query");
		del = new JButton("Delete");
		del.setActionCommand("delete");
		final ComponentDialog me = this;
		del.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand().equals("delete")) {
					GUIManifestState.getInstance().deleteSlice((String)sliceList.getSelectedValue());
					me.setVisible(false);
				}
			}
		});
		addButton(del);
		return kp;
	}

	@Override
	public boolean accept() {
		String s = (String)sliceList.getSelectedValue();
		GUIManifestState.getInstance().setSliceIdFieldText(s);
		GUIManifestState.getInstance().queryManifest();
		return true;
	}
}
