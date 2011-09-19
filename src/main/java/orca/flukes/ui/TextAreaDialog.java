package orca.flukes.ui;

import java.awt.Component;

import javax.swing.JFrame;

import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KTextArea;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;

@SuppressWarnings("serial")
public class TextAreaDialog extends ComponentDialog {
	KPanel kp;
	final KTextArea ta;
	int rows, cols;
	ITextSetter ts;
	
	public interface ITextSetter {
		public void setText(String t);
	}
	
	public TextAreaDialog(JFrame parent, ITextSetter ts, String title, String message, int r, int c) {
		super(parent, title, true);
		super.setLocationRelativeTo(parent);
		
		setComment(message);
		
		this.rows = r;
		this.cols = c;
		this.ts = ts;

		ta = new KTextArea(rows, cols);
		kp.add(ta);
		
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
		
		ts.setText(ta.getText());
		return true;
	}
}
