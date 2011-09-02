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
	
	private JList imageList;
	
	public OrcaMultiNodePropertyDialog(JFrame parent, Set<OrcaNode> ns) {
		super(parent, "Shared Node Properties", true);
		super.setLocationRelativeTo(parent);
		this.parent = parent;
		this.nodes = ns;
	}
	
	@Override
	public boolean accept() {
		for (OrcaNode node: nodes) {
			node.setImage(GUIState.getInstance().getImageShortNamesWithNone()[imageList.getSelectedIndex()]);
			if (node.getImage().equals(GUIState.NO_GLOBAL_IMAGE))
				node.setImage(null);
		}
		return true;
	}
	
	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		kp.setLayout(gbl_contentPanel);
		{
			JLabel lblNewLabel_1 = new JLabel("Select image: ");
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = 1;
			kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
		}
		{
			imageList = new JList(GUIState.getInstance().getImageShortNamesWithNone());
			imageList.setSelectedIndex(0);
			imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			imageList.setLayoutOrientation(JList.VERTICAL);
			imageList.setVisibleRowCount(1);
			GridBagConstraints gbc_list = new GridBagConstraints();
			gbc_list.insets = new Insets(0, 0, 5, 5);
			gbc_list.fill = GridBagConstraints.HORIZONTAL;
			gbc_list.gridx = 1;
			gbc_list.gridy = 1;
			kp.add(imageList, gbc_list);
		}
		
		return kp;
	}

}
