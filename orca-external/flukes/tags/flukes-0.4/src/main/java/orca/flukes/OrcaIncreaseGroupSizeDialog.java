package orca.flukes;

import java.awt.Frame;

import javax.swing.JRootPane;
import javax.swing.JTextField;

import orca.flukes.ndl.ModifySaver;

import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.dialog.KInputDialog;

public class OrcaIncreaseGroupSizeDialog extends KInputDialog {
	OrcaNode node;

	public OrcaIncreaseGroupSizeDialog(Frame arg0, OrcaNode on) {
		super(arg0, "Increase group size", true);
		super.setComment((on.getGroup() != null ? on.getGroup() : "Not part of a group"));
		super.setLocationRelativeTo(arg0);
		node = on;
	}
	
	@Override
	protected boolean accept() {
		try {
			JRootPane jrp = (JRootPane)this.getComponent(0);
			KPanel kpn = (KPanel)jrp.getContentPane().getComponent(0);
			KPanel kpn1 = (KPanel)kpn.getComponent(3);
			JTextField jtf = (JTextField)kpn1.getComponent(0);
			Integer i = Integer.parseInt(jtf.getText());
			if (i <= 0) {
				return false;
			}
			ModifySaver.getInstance().addNodesToGroup(node.getGroup(), i);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

}
