package orca.flukes.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import orca.flukes.GUIManifestState;

import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;

@SuppressWarnings("serial")
public class TextHTMLPaneDialog extends ComponentDialog {

	KPanel kp;
	final JTextPane tp;
	int rows, cols;
	ITextSetter ts = null;
	private JButton urlBut;
	private final String urlToVisit;
	
	public interface ITextSetter {
		public void setText(String t);
	}
	
	/**
	 * create editable text area
	 * @param parent
	 * @param ts - ITextSetter
	 * @param title
	 * @param message
	 */
	public TextHTMLPaneDialog(JFrame parent, ITextSetter ts, String title, String message) {
		super(parent, title, true);
		super.setLocationRelativeTo(parent);
		
		urlToVisit = null;
		
		setComment(message);
		
		this.ts = ts;

		tp = new JTextPane();
		tp.setContentType("text/html");
		tp.setPreferredSize(new Dimension(500, 600));
		tp.setEditable(true);
		JScrollPane areaScrollPane = new JScrollPane(tp);
		areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		kp.setLayout(new BorderLayout(0,0));
		kp.add(areaScrollPane);
	}
	
	/**
	 * Create uneditable text area
	 * @param parent
	 * @param title
	 * @param message
	 */
	public TextHTMLPaneDialog(JFrame parent, String title, String message) {
		super(parent, title, true);
		super.setLocationRelativeTo(parent);
		
		urlToVisit = null;
		
		setComment(message);
		
		this.ts = null;

		tp = new JTextPane();
		tp.setContentType("text/html");
		tp.setPreferredSize(new Dimension(500, 600));
		tp.setEditable(false);
		JScrollPane areaScrollPane = new JScrollPane(tp);
		areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		kp.setLayout(new BorderLayout(0,0));
		kp.add(areaScrollPane);
	}
	
	/**
	 * Create uneditable text area
	 * @param parent
	 * @param title
	 * @param message
	 * @param url - URL to visit for more information
	 */
	public TextHTMLPaneDialog(JFrame parent, String title, String message, String url) {
		super(parent, title, true);
		super.setLocationRelativeTo(parent);
		
		urlToVisit = url;
		
		setComment(message);
		
		this.ts = null;

		tp = new JTextPane();
		tp.setContentType("text/html");
		tp.setPreferredSize(new Dimension(500, 600));
		tp.setEditable(false);
		JScrollPane areaScrollPane = new JScrollPane(tp);
		areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		kp.setLayout(new BorderLayout(0,0));
		kp.add(areaScrollPane);
	}
	
	public JTextPane getTextPane() {
		return tp;
	}
	
	@Override
	protected Component buildDialogUI() {
		kp = new KPanel();
		
		urlBut = new JButton("More Information");
		urlBut.setActionCommand("moreinfo");
		final ComponentDialog me = this;
		urlBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand().equals("moreinfo")) {
					try {
						java.awt.Desktop.getDesktop().browse(java.net.URI.create(urlToVisit));
					} catch (Exception ee) {
						;
					}
					me.setVisible(false);
				}
			}
		});
		addButton(urlBut);
		
		return kp;
	}
	
	@Override
	public boolean accept() {
		
		if (ts != null)
			ts.setText(tp.getText());
		return true;
	}
}
