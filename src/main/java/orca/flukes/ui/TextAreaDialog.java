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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KTextArea;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;

@SuppressWarnings("serial")
public class TextAreaDialog extends ComponentDialog {
	KPanel kp;
	final KTextArea ta;
	int rows, cols;
	ITextSetter ts = null;
	
	public interface ITextSetter {
		public void setText(String t);
	}
	
	/**
	 * create editable text area
	 * @param parent
	 * @param ts
	 * @param title
	 * @param message
	 * @param r
	 * @param c
	 * @param editable
	 */
	public TextAreaDialog(JFrame parent, ITextSetter ts, String title, String message, int r, int c) {
		super(parent, title, true);
		super.setLocationRelativeTo(parent);
		
		setComment(message);
		
		this.rows = r;
		this.cols = c;
		this.ts = ts;

		ta = new KTextArea();
		ta.setRows(r);
		ta.setColumns(c);
		ta.setMinimumSize(new Dimension(500, 500));
		ta.setEditable(true);
		JScrollPane areaScrollPane = new JScrollPane(ta);
		areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		kp.setLayout(new BorderLayout(0,0));
		kp.add(areaScrollPane);
	}
	
	/**
	 * Create uneditable text area
	 * @param parent
	 * @param title
	 * @param message
	 * @param r
	 * @param c
	 */
	public TextAreaDialog(JFrame parent, String title, String message, int r, int c) {
		super(parent, title, true);
		super.setLocationRelativeTo(parent);
		
		setComment(message);
		
		this.rows = r;
		this.cols = c;
		this.ts = null;

		ta = new KTextArea();
		ta.setRows(r);
		ta.setColumns(c);
		ta.setMinimumSize(new Dimension(500, 500));
		ta.setEditable(false);
		JScrollPane areaScrollPane = new JScrollPane(ta);
		areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		kp.setLayout(new BorderLayout(0,0));
		kp.add(areaScrollPane);
	}
	
	public KTextArea getTextArea() {
		return ta;
	}
	
	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		
		return kp;
	}
	
	@Override
	public boolean accept() {
		
		if (ts != null)
			ts.setText(ta.getText());
		return true;
	}
}
