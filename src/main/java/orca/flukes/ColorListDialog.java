package orca.flukes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;

import orca.flukes.ui.ChooserWithNewDialog;

public class ColorListDialog extends ChooserWithNewDialog<String> implements ActionListener {
	private static int colorIndex = 0;
	private OrcaResource or;
	private boolean modify;

	public ColorListDialog(JFrame parent, OrcaResource or, boolean modify) {
		super(parent, "Colors for " + or.getName(), "Defined Colors", getColorNames(or));
		super.setLocationRelativeTo(parent);
		this.modify = modify;
		if (modify)
			setNewActionListener(this);
		setEditActionListener(this);
		if (modify)
			setDeleteActionListener(this);
		this.or = or;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		synchronized(this) {
			if (e.getActionCommand().equals("new")) {
				OrcaColor oc = new OrcaColor("NewColor" + colorIndex);
				or.addColor(oc);

				OrcaColorDialog ocd = new OrcaColorDialog(GUI.getInstance().getFrame(), or, "NewColor" + colorIndex++);
				ocd.pack();
				ocd.setVisible(true);
			} else if (e.getActionCommand().equals("delete")) {
				String st = getSelectedItem();
				or.delColor(st);
			} else if (e.getActionCommand().equals("edit")) {
				String st = getSelectedItem();

				if (st != null ) {
					if (modify) {
						OrcaColorDialog ocd = new OrcaColorDialog(GUI.getInstance().getFrame(), or, st);
						ocd.pack();
						ocd.setVisible(true);
					} else {
						OrcaColorViewer dialog = new OrcaColorViewer(GUI.getInstance().getFrame(), or.getColor(st));
						dialog.pack();
						dialog.setVisible(true);
					}
				}
			}
		}
		setVisible(false);
	}

	private static Iterator<String> getColorNames(OrcaResource or) {
		List<String> colorNames = new ArrayList<String>();
		for (OrcaColor c: or.getColors()) {
			colorNames.add(c.getLabel());
		}
		return colorNames.iterator();

	}

}
