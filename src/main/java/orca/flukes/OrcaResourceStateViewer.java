package orca.flukes;

import java.awt.Component;
import java.util.Date;
import java.util.List;

import javax.swing.JFrame;

import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;

public abstract class OrcaResourceStateViewer extends ComponentDialog {
	protected KPanel kp;
	
	public OrcaResourceStateViewer(JFrame parent, List<OrcaResource> resources, Date start, Date end) {
		super(parent, "View current resource states.", false);
		
		super.setLocationRelativeTo(parent);

		initResourceViewer(resources, start, end);
	}
	
	protected abstract void initResourceViewer(List<OrcaResource> resources, Date start, Date end);
	
	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		
		return kp;
	}
}
