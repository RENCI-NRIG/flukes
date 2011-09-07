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
			node.setImage(GUIState.getNodeImageProper(GUIState.getInstance().getImageShortNamesWithNone()[imageList.getSelectedIndex()]));
			// domain
			node.setDomain(GUIState.getNodeDomainProper(GUIState.getInstance().getAvailableDomains()[domainList.getSelectedIndex()]));
		}
		return true;
	}


	
	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		kp.setLayout(gbl_contentPanel);
		
		imageList = OrcaNodePropertyDialog.addImageList(kp, gbl_contentPanel, 0);
		domainList = OrcaNodePropertyDialog.addDomainList(kp, gbl_contentPanel, 1);
		
		return kp;
	}

}
