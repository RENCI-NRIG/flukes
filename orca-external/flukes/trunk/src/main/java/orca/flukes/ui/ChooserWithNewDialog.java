package orca.flukes.ui;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.JButton;

import com.hyperrealm.kiwi.ui.ButtonPanel;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.dialog.ItemChooserDialog;

@SuppressWarnings("serial")
public class ChooserWithNewDialog<T> extends ItemChooserDialog<T> {

	public ChooserWithNewDialog(Dialog parent, String title, String comment, Iterator<T> items) {
		super(parent, title, comment);
		setItems(items);
	}
	
	public ChooserWithNewDialog(Frame parent, String title, String comment, Iterator<T> items) {
		super(parent, title, comment);
		setItems(items);
	}
	
	public void setNewActionListener(ActionListener al) {
		addNewButton(al);
	}
	
	private void addNewButton(ActionListener newAction) {
		JButton nb = new JButton("New");
		if (newAction != null)
			nb.addActionListener(newAction);
		
		Container cp = getContentPane();
		KPanel kp = (KPanel)cp.getComponent(0);
		ButtonPanel bp = (ButtonPanel)kp.getComponent(1);

		bp.addButton(nb);
	}
}
