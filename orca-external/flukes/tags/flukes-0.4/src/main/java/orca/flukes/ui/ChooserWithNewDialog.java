/*
* Copyright (c) 2011 RENCI/UNC Chapel Hill 
*
* @author Ilia Baldine
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
* and/or hardware specification (the "Work") to deal in the Work without restriction, including 
* without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
* sell copies of the Work, and to permit persons to whom the Work is furnished to do so, subject to 
* the following conditions:  
* The above copyright notice and this permission notice shall be included in all copies or 
* substantial portions of the Work.  
*
* THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
* OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS 
* IN THE WORK.
*/
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
	
	public void setDeleteActionListener(ActionListener al) {
		addDeleteButton(al);
	}
	
	public void setEditActionListener(ActionListener al) {
		addEditButton(al);
	}
	
	private void addNewButton(ActionListener newAction) {
		JButton nb = new JButton("New");
		if (newAction != null) {
			nb.addActionListener(newAction);
			nb.setActionCommand("new");
		}
		
		Container cp = getContentPane();
		KPanel kp = (KPanel)cp.getComponent(0);
		ButtonPanel bp = (ButtonPanel)kp.getComponent(1);

		bp.addButton(nb);
	}
	
	private void addDeleteButton(ActionListener newAction) {
		JButton nb = new JButton("Delete");
		if (newAction != null) {
			nb.addActionListener(newAction);
			nb.setActionCommand("delete");
		}
		
		Container cp = getContentPane();
		KPanel kp = (KPanel)cp.getComponent(0);
		ButtonPanel bp = (ButtonPanel)kp.getComponent(1);

		bp.addButton(nb);
	}
	
	private void addEditButton(ActionListener newAction) {
		JButton nb = new JButton("Edit");
		if (newAction != null) {
			nb.addActionListener(newAction);
			nb.setActionCommand("edit");
		}
		
		Container cp = getContentPane();
		KPanel kp = (KPanel)cp.getComponent(0);
		ButtonPanel bp = (ButtonPanel)kp.getComponent(1);

		bp.addButton(nb);
	}
}
