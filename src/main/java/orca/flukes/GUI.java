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

package orca.flukes;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

import orca.flukes.ndl.ManifestLoader;
import orca.flukes.ndl.RequestLoader;
import orca.flukes.ndl.RequestSaver;
import orca.flukes.ui.KeystoreDialog;
import orca.flukes.ui.PasswordDialog;
import orca.flukes.ui.SplitButton;
import orca.flukes.ui.TextAreaDialog;
import orca.flukes.xmlrpc.OrcaSMXMLRPCProxy;
import orca.flukes.xmlrpc.RegistryXMLRPCProxy;
import orca.ndl.NdlCommons;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import com.hyperrealm.kiwi.ui.KFileChooser;
import com.hyperrealm.kiwi.ui.KTextArea;
import com.hyperrealm.kiwi.ui.KTextField;
import com.hyperrealm.kiwi.ui.UIChangeManager;
import com.hyperrealm.kiwi.ui.dialog.KFileChooserDialog;
import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;
import com.hyperrealm.kiwi.ui.dialog.KQuestionDialog;
import com.hyperrealm.kiwi.ui.dialog.ProgressDialog;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.util.Animator;

public class GUI implements ComponentListener {

	private static final String FLUKES_HELP_WIKI = "https://geni-orca.renci.org/trac/wiki/flukes";
	public static final String buildVersion = GUI.class.getPackage().getImplementationVersion();
	public static final String aboutText = "ORCA FLUKES " + (buildVersion == null? "Eclipse build" : buildVersion) + "\nNDL-OWL network editor for ORCA (Open Resource Control Architecture)" +
	"\nDeveloped using Jena Semantic Web Framework, JUNG Java Universal Network/Graph Framework and Kiwi Swing toolkit. \nSplitButton adopted from implementation by Edward Scholl (edscholl@atwistedweb.com)" +
	"\n\nCopyright 2011-2013 RENCI/UNC Chapel Hill";
	private static final String FRAME_TITLE = "ORCA FLUKES - The ORCA Network Editor";
	private static final String PREF_FILE = ".flukes.properties";
	private JFrame frmOrcaFlukes;
	private JTabbedPane tabbedPane;
	private JPanel requestPanel, resourcePanel, manifestPanel;
	private JMenuBar menuBar;
	private JMenu fileNewMenu;
	private JSeparator separator;
	private JMenu mnNewMenu, controllerMenu, outputMenu, layoutMenu;
	private JSeparator separator_1, separator_2;
	private Logger logger;
	private String[] controllerUrls;
	private String selectedControllerUrl;
	private SplitButton splitNodeButton, splitLinkButton;
	
	private boolean withIRods = false;
	
	// alias and password within a keystore to be used for XMLRPC calls
	private String keyAlias = null, keyPassword = null;
	// preferences
	private Properties prefProperties;
	private static GUI instance = null;
	
	protected final MenuListener mListener = new MenuListener();
	
	// remember which layout was associated with which view
	private Map<GuiTabs, GraphLayouts> savedLayout = new HashMap<GuiTabs, GraphLayouts>();
	
	public static final Set<String> NDL_EXTENSIONS = new HashSet<String>();
	static {
		NDL_EXTENSIONS.add("ndl");
		NDL_EXTENSIONS.add("rdf");
		NDL_EXTENSIONS.add("n3");
	}
	// file fiter
	public class NdlFileFilter extends FileFilter {
		
	    //Accept all directories and all gif, jpg, tiff, or png files.
	    @Override
		public boolean accept(File f) {
	        if (f.isDirectory()) {
	            return true;
	        }

	        String extension = getExtension(f);
	        if (NDL_EXTENSIONS.contains(extension))
	        	return true;

	        return false;
	    }

	    //The description of this filter
	    @Override
		public String getDescription() {
	        return "NDL Files";
	    }
	    
	    private String getExtension(File f) {
	        String ext = null;
	        String s = f.getName();
	        int i = s.lastIndexOf('.');

	        if (i > 0 &&  i < s.length() - 1) {
	            ext = s.substring(i+1).toLowerCase();
	        }
	        return ext;
	    }
	}
	
	public Logger getLogger() {
		return logger;
	}
	
	public static Logger logger() {
		return GUI.getInstance().getLogger();
	}
	
	// keystore identity
	public String getKeystoreAlias() {
		if ((keyAlias == null) || (keyPassword == null)) 
			identityDialog();
		return keyAlias;
	}
	
	public String getKeystorePassword() {
		if ((keyAlias == null) || (keyPassword == null)) 
			identityDialog();
		return keyPassword;
	}
	
	public String getKeystorePasswordOnly() {
		if (keyPassword == null) 
			passwordDialog();
		return keyPassword;
	}
	
	public void resetKeystoreAliasAndPassword() {
		keyAlias = null;
		keyPassword = null;
	}
	
	// All menu actions here
	/**
	 * Menu actions
	 */
	public class MenuListener implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("exit")) 
				quit();
			else if (e.getActionCommand().equals("open")) {
				KFileChooserDialog d = new KFileChooserDialog(getFrame(), "Load NDL Request", KFileChooser.OPEN_DIALOG);
				d.setLocationRelativeTo(getFrame());
				d.getFileChooser().setAcceptAllFileFilterUsed(true);
				d.getFileChooser().addChoosableFileFilter(new NdlFileFilter());
				if (GUIRequestState.getInstance().getSaveDir() != null)
					d.setCurrentDirectory(new File(GUIRequestState.getInstance().getSaveDir()));
				d.pack();
				d.setVisible(true);
				if (d.getSelectedFile() != null) {
					GUIRequestState.getInstance().clear();
					RequestLoader rl = new RequestLoader();
					if (rl.loadGraph(d.getSelectedFile())) {
						frmOrcaFlukes.setTitle(FRAME_TITLE + " : " + d.getSelectedFile().getName());
						GUIRequestState.getInstance().setSaveFile(d.getSelectedFile());
						GUIRequestState.getInstance().setSaveDir(d.getSelectedFile().getParent());
					}	
				}
				kickLayout(GuiTabs.REQUEST_VIEW);
			}
			else if (e.getActionCommand().equals("openmanifest")) {
				KFileChooserDialog d = new KFileChooserDialog(getFrame(), "Load NDL manifest", KFileChooser.OPEN_DIALOG);
				d.setLocationRelativeTo(getFrame());
				d.getFileChooser().setAcceptAllFileFilterUsed(true);
				// see if we saved the directory
				if (GUIManifestState.getInstance().getSaveDir() != null)
					d.setCurrentDirectory(new File(GUIManifestState.getInstance().getSaveDir()));
				d.getFileChooser().addChoosableFileFilter(new NdlFileFilter());
				d.pack();
				d.setVisible(true);
				if (d.getSelectedFile() != null) {
					GUIManifestState.getInstance().clear();
					ManifestLoader ml = new ManifestLoader();
					ml.loadGraph(d.getSelectedFile());
					// save the directory
					GUIManifestState.getInstance().setSaveDir(d.getSelectedFile().getParent());
				}
				kickLayout(GuiTabs.MANIFEST_VIEW);
			}
			else if (e.getActionCommand().equals("new")) {
				GUIRequestState.getInstance().clear();
				frmOrcaFlukes.setTitle(FRAME_TITLE);
				GUIRequestState.getInstance().vv.repaint();
			}
			else if (e.getActionCommand().equals("save")) {
				if (GUIRequestState.getInstance().getSaveFile() != null) {
					RequestSaver.getInstance().saveGraph(GUIRequestState.getInstance().getSaveFile(), 
							GUIRequestState.getInstance().g,
							GUIRequestState.getInstance().nsGuid);
				} else {
					KFileChooserDialog d = new KFileChooserDialog(getFrame(), "Save Request in NDL", KFileChooser.SAVE_DIALOG);
					d.setLocationRelativeTo(getFrame());
					d.getFileChooser().setAcceptAllFileFilterUsed(true);
					d.getFileChooser().addChoosableFileFilter(new NdlFileFilter());
					if (GUIRequestState.getInstance().getSaveDir() != null)
						d.setCurrentDirectory(new File(GUIRequestState.getInstance().getSaveDir()));
					d.pack();
					d.setVisible(true);
					if (d.getSelectedFile() != null) {
						if (RequestSaver.getInstance().saveGraph(d.getSelectedFile(), 
								GUIRequestState.getInstance().g,
								GUIRequestState.getInstance().nsGuid)) {
							frmOrcaFlukes.setTitle(FRAME_TITLE + " : " + d.getSelectedFile().getName());
							GUIRequestState.getInstance().setSaveFile(d.getSelectedFile());
							GUIRequestState.getInstance().setSaveDir(d.getSelectedFile().getParent());
						}
					}
				}
			}
			else if (e.getActionCommand().equals("saveas")) {
				KFileChooserDialog d = new KFileChooserDialog(getFrame(), "Save Request in NDL", KFileChooser.SAVE_DIALOG);
				d.setLocationRelativeTo(getFrame());
				d.getFileChooser().setAcceptAllFileFilterUsed(true);
				d.getFileChooser().addChoosableFileFilter(new NdlFileFilter());
				if (GUIRequestState.getInstance().getSaveDir() != null)
					d.setCurrentDirectory(new File(GUIRequestState.getInstance().getSaveDir()));
				d.pack();
				d.setVisible(true);
				if (d.getSelectedFile() != null) {
					if (RequestSaver.getInstance().saveGraph(d.getSelectedFile(), 
							GUIRequestState.getInstance().g,
							GUIRequestState.getInstance().nsGuid)) {
						frmOrcaFlukes.setTitle(FRAME_TITLE + " : " + d.getSelectedFile().getName());
						GUIRequestState.getInstance().setSaveFile(d.getSelectedFile());
						GUIRequestState.getInstance().setSaveDir(d.getSelectedFile().getParent());
					}
				}
			}
			else if (e.getActionCommand().equals("help"))
				helpDialog();
			else if (e.getActionCommand().equals("about"))
				aboutDialog();
			else if (e.getActionCommand().equals("prefs"))
				prefsDialog();
			else if (e.getActionCommand().equals("license"))
				licenseDialog();
			else if (e.getActionCommand().equals("relnotes"))
				relnotesDialog();
			else if (e.getActionCommand().equals("xml"))
				RequestSaver.getInstance().setOutputFormat(RequestSaver.RDF_XML_FORMAT);
			else if (e.getActionCommand().equals("n3"))
				RequestSaver.getInstance().setOutputFormat(RequestSaver.N3_FORMAT);
			else if (e.getActionCommand().equals(GraphLayouts.KK.getName())) {
				switchLayout(activeTab(), GraphLayouts.KK);
			} else if (e.getActionCommand().equals(GraphLayouts.FR.getName())) {
				switchLayout(activeTab(), GraphLayouts.FR);
			} else if (e.getActionCommand().equals(GraphLayouts.ISOM.getName())) {
				switchLayout(activeTab(), GraphLayouts.ISOM);
			} else if (e.getActionCommand().startsWith("url")) {
				// controller url selection
				int urlIndex = Integer.parseInt(e.getActionCommand().substring(3, e.getActionCommand().length()));
				selectedControllerUrl = controllerUrls[urlIndex];
				OrcaSMXMLRPCProxy.getInstance().resetSSLIdentity();
				keyAlias = null;
				keyPassword = null;
				GUIRequestState.getInstance().listSMResources();
			} else if (e.getActionCommand().equals("openirods")) {
				KMessageDialog md = new KMessageDialog(GUI.getInstance().getFrame(), "Not implemented.", true);
				md.setMessage("This function is not yet implemented!");
				md.setLocationRelativeTo(GUI.getInstance().getFrame());
				md.setVisible(true);
			} else if (e.getActionCommand().equals("saveirods")) {
				KMessageDialog md = new KMessageDialog(GUI.getInstance().getFrame(), "Not implemented.", true);
				md.setMessage("This function is not yet implemented!");
				md.setLocationRelativeTo(GUI.getInstance().getFrame());
				md.setVisible(true);
			} else if (e.getActionCommand().equals("openmanifestirods")) {
				KMessageDialog md = new KMessageDialog(GUI.getInstance().getFrame(), "Not implemented.", true);
				md.setMessage("This function is not yet implemented!");
				md.setLocationRelativeTo(GUI.getInstance().getFrame());
				md.setVisible(true);
			} else if (e.getActionCommand().equals("savemanifestirods")) {
				GUIManifestState.getInstance().saveManifestToIRods();
			} else {
				// catchall
				KMessageDialog md = new KMessageDialog(GUI.getInstance().getFrame(), "Not implemented.", true);
				md.setMessage("Unknown or unimplemented function!");
				md.setLocationRelativeTo(GUI.getInstance().getFrame());
				md.setVisible(true);
			}
		}
	}
	
	/**
	 * switch layout at specific tab
	 * @param at
	 * @param l
	 */
	@SuppressWarnings("unchecked")
	private void switchLayout(GuiTabs at, GraphLayouts l) {

		//final Layout<OrcaNode, OrcaLink> oldL = vv.getGraphLayout();
		Layout<OrcaNode, OrcaLink> newL = null;
		
		VisualizationViewer<OrcaNode,OrcaLink> myVv = null;
		SparseMultigraph<OrcaNode, OrcaLink> myGraph = null;
		
		switch(at) {
		case RESOURCE_VIEW:
			break;
		case REQUEST_VIEW:
			myVv = GUIRequestState.getInstance().vv;
			myGraph = GUIRequestState.getInstance().g;
			break;
		case MANIFEST_VIEW:
			myVv = GUIManifestState.getInstance().vv;
			myGraph = GUIManifestState.getInstance().g;
			break;
		}
		
		if ((myVv == null) || (myGraph == null))
			return;

		try {
			Class<?> pars[] = new Class[1];
			pars[0] = edu.uci.ics.jung.graph.Graph.class;
			Constructor<?> ct = l.getClazz().getConstructor(pars);
			Object args[] = new Object[1];
			args[0] = myGraph;
			newL = (Layout<OrcaNode, OrcaLink>)ct.newInstance(args);
		} catch (Exception e) {
			;
		}
		
		if (newL == null)
			return;
		
        newL.setInitializer(myVv.getGraphLayout());
        newL.setSize(myVv.getSize());
        LayoutTransition<OrcaNode, OrcaLink> lt =
        	new LayoutTransition<OrcaNode, OrcaLink>(myVv, myVv.getGraphLayout(), newL);
        Animator animator = new Animator(lt);
        animator.start();
        myVv.getRenderContext().getMultiLayerTransformer().setToIdentity();
		myVv.repaint();
		
		// save this layout for this view
		savedLayout.put(activeTab(), l);
	}
	
	protected void kickLayout(GuiTabs tab) {
		switchLayout(tab, savedLayout.get(tab));
	}
	
	private void quit() {
		KQuestionDialog kqd = new KQuestionDialog(GUI.getInstance().getFrame(), "Exit", true);
		kqd.setMessage("Are you sure you want to exit?");
		kqd.setLocationRelativeTo(GUI.getInstance().getFrame());
		kqd.setVisible(true);
		if (kqd.getStatus())
			System.exit(0);
	}
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		// Jena stuff needs to be set up early
		NdlCommons.setGlobalJenaRedirections();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI gui = GUI.getInstance();

					gui.processPreferences();
					gui.getImagesFromPreferences();
					gui.getImagesFromRegistry();

					gui.getControllersFromPreferences();
					gui.getIRodsPreferences();
					gui.getCustomInstancePreferences();
					
					gui.initialize();
					gui.getFrame().setVisible(true);					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	private GUI() {
		logger = Logger.getLogger(GUI.class.getCanonicalName());
		logger.setLevel(Level.DEBUG);
		ConsoleAppender capp = new ConsoleAppender();
		capp.setImmediateFlush(true);
		capp.setName("Flukes Console Appender");
		org.apache.log4j.SimpleLayout sl = new SimpleLayout();
		capp.setLayout(sl);
		capp.setWriter(new PrintWriter(System.out));
		logger.addAppender(capp);
		
		//logger.addAppender(new Appender);
		UIChangeManager.setDefaultTexture(null);
	}

	public static GUI getInstance() {
		if (instance == null)
			instance = new GUI();
		return instance;
	}
	
	public JFrame getFrame() {
		return frmOrcaFlukes;
	}
	

	private void aboutDialog() {
		TextAreaDialog tad = new TextAreaDialog(frmOrcaFlukes, "About FLUKES", "", 8,50);
		KTextArea ta = tad.getTextArea();
		
		ta.setText(aboutText);
		tad.pack();
		tad.setVisible(true);
	}
	
	private void helpDialog() {

		try {
			java.awt.Desktop.getDesktop().browse(java.net.URI.create(FLUKES_HELP_WIKI));
		} catch (Exception e) {

		}
//		try {
//			//AboutFrame ab = new AboutFrame("FLUKES Help", new URL(FLUKES_HREF_URL + HELP_DOC));
//			AboutFrame ab = new AboutFrame("FLUKES Help", new URL("https://geni-orca.renci.org/trac/wiki/flukes"));
//			ab.setVisible(true);
//		} catch (MalformedURLException e) {
//			;
//		}
	}
	
	private void prefsDialog() {
		TextAreaDialog tad = new TextAreaDialog(frmOrcaFlukes, "Preference settings", 
				"Current preference settings (cut and paste into $HOME/.flukes.properties, modify and restart to change)", 
				PrefsEnum.values().length*2 + 2, 50);
		KTextArea ta = tad.getTextArea();
		
		String prefs = "";
		for (int i = 0; i < PrefsEnum.values().length; i++) {
			PrefsEnum e = PrefsEnum.values()[i];
			prefs += "\n# " + e.getComment();
			prefs += "\n" + e.getPropName() + "=" + getPreference(e);
		}
		ta.setText(prefs);
		tad.pack();
        tad.setVisible(true);
	}

	private String readResourceToString(String res) {
		StringBuilder sb = new StringBuilder();
		try {
			InputStream is = GUI.class.getResourceAsStream(res);
			BufferedReader bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String line = null;
			while((line = bin.readLine()) != null) {
				sb.append(line);
				// re-add line separator
				sb.append(System.getProperty("line.separator"));
			}

			bin.close();
		} catch (UnsupportedEncodingException uee) {
			
		} catch (IOException ioe) {
			
		}
		return sb.toString();
	}
	
	private void licenseDialog() {
		TextAreaDialog tad = new TextAreaDialog(frmOrcaFlukes, "LICENSE", 
				"License", 
				30, 50);
		KTextArea ta = tad.getTextArea();

		ta.setText(readResourceToString("/LICENSE"));
		tad.pack();
        tad.setVisible(true);
	}
	
	private void relnotesDialog() {
		TextAreaDialog tad = new TextAreaDialog(frmOrcaFlukes, "Release Notes", 
				"Release Notes", 
				30, 50);
		KTextArea ta = tad.getTextArea();

		ta.setText(readResourceToString("/RELEASE-NOTES"));
		tad.pack();
        tad.setVisible(true);
	}
	
	private void passwordDialog() {
		PasswordDialog pd = new PasswordDialog(frmOrcaFlukes, "Enter key password to be used with " + getPreference(PrefsEnum.USER_CERTKEYFILE));
		
		pd.pack();
		pd.setVisible(true);
		
		keyPassword = pd.getPassword();
	}
	
	private void identityDialog() {
		KeystoreDialog ld = new KeystoreDialog(frmOrcaFlukes, 
				"Enter key alias and key password to be used with " + getPreference(PrefsEnum.USER_KEYSTORE));
		
		ld.pack();
		ld.setVisible(true);
		
		keyAlias = ld.getAlias();
		keyPassword = ld.getPassword();
	}
	
	/*
	 * Common menus between tabs
	 */
	private void commonMenus() {
		// create a common action
		
		// populate the menu
		menuBar = new JMenuBar();
		frmOrcaFlukes.setJMenuBar(menuBar);
		
		fileNewMenu = new JMenu("File ");
		menuBar.add(fileNewMenu);
		
		fileNewMenu.add(addMenuItem("New Request", "new", mListener));
		fileNewMenu.add(addMenuItem("Open Request...", "open", mListener));
		
		if (withIRods) 
			fileNewMenu.add(addMenuItem("Open Request from iRods ...", "openirods", mListener));
		
		fileNewMenu.add(addMenuItem("Save Request", "save", mListener));
		fileNewMenu.add(addMenuItem("Save Request As...", "saveas", mListener));
		
		if (withIRods) 
			fileNewMenu.add(addMenuItem("Save Request into iRods ...", "saveirods", mListener));
		
		JSeparator sep = new JSeparator();
		fileNewMenu.add(sep);
		
		fileNewMenu.add(addMenuItem("Open Manifest...", "openmanifest", mListener));
		
		if (withIRods) {
			fileNewMenu.add(addMenuItem("Open Manifest from iRods ...", "openmanifestirods", mListener));
			fileNewMenu.add(addMenuItem("Save Manifest into iRods ...", "savemanifestirods", mListener));
		}
		
		separator = new JSeparator();
		fileNewMenu.add(separator);
		
		fileNewMenu.add(addMenuItem("Exit", "exit", mListener));
		
		// controller selection
		controllerMenu = new JMenu("Orca Controller");
		menuBar.add(controllerMenu);
		
		ButtonGroup cbg = new ButtonGroup();
		
		JRadioButtonMenuItem mi;
		
		// loop through controllers
		int i = 0;
		for (String url: controllerUrls) {
			mi = new JRadioButtonMenuItem(url);
			mi.setActionCommand("url" + i++);
			mi.addActionListener(mListener);
			if (i == 1) {
				mi.setSelected(true);
				selectedControllerUrl = url;
			}
			controllerMenu.add(mi);
			cbg.add(mi);
		}
		
		// output format selection
		outputMenu = new JMenu("Output Format");
		menuBar.add(outputMenu);
		
		ButtonGroup obg = new ButtonGroup();

		mi = new JRadioButtonMenuItem("RDF-XML");
		mi.setActionCommand("xml");
		mi.addActionListener(mListener);
		mi.setSelected(true);
		outputMenu.add(mi);	
		obg.add(mi);
		
		mi = new JRadioButtonMenuItem("N3");
		mi.setActionCommand("n3");
		mi.addActionListener(mListener);
		outputMenu.add(mi);	
		obg.add(mi);
		
		layoutMenu = new JMenu("Graph Layout");
		menuBar.add(layoutMenu);
		
		ButtonGroup lbg = new ButtonGroup();
		
		// all layouts (and their menu items)
		for(GraphLayouts l: GraphLayouts.values()) {
			layoutMenu.add(l.getItem());
			lbg.add(l.getItem());
		}
		
		mnNewMenu = new JMenu("Help");
		menuBar.add(mnNewMenu);
		
		mnNewMenu.add(addMenuItem("Preference settings", "prefs", mListener));
		
		separator_2 = new JSeparator();
		mnNewMenu.add(separator_2);
		
		mnNewMenu.add(addMenuItem("Help Contents (opens in external browser)", "help", mListener));
		
		separator_1 = new JSeparator();
		mnNewMenu.add(separator_1);
		
		mnNewMenu.add(addMenuItem("About", "about", mListener));
		mnNewMenu.add(addMenuItem("License", "license", mListener));
		mnNewMenu.add(addMenuItem("Release Notes", "relnotes", mListener));
	}
	
	public enum GuiTabs {
		RESOURCE_VIEW("Resource View"), 
		REQUEST_VIEW("Request View"), 
		MANIFEST_VIEW("Manifest View");
		
		private final String layoutName;
		
		GuiTabs(String n) {
			layoutName = n;
		}
		public String getName() {
			return layoutName;
		}
	}
	
	public GuiTabs activeTab() {
		switch(tabbedPane.getSelectedIndex()) {
		case 0: return GuiTabs.RESOURCE_VIEW;
		case 1: return GuiTabs.REQUEST_VIEW;
		case 2: return GuiTabs.MANIFEST_VIEW;
		default: return GuiTabs.REQUEST_VIEW;
		}
	}
	
	public enum GraphLayouts {
		KK("Karmada-Kawai", KKLayout.class),
		FR("Fruchterman-Rheingold", FRLayout.class),
		ISOM("Self-organizing", ISOMLayout.class);
		
		private final String name;
		private final Class<?> clazz;
		private final JRadioButtonMenuItem menuItem;
		
		GraphLayouts(String n, Class<?> s) {
			name = n;
			clazz = s;
			menuItem = new JRadioButtonMenuItem(name);
			menuItem.setActionCommand(name);
			menuItem.addActionListener(GUI.getInstance().mListener);
		}
		
		public String getName() {
			return name;
		}
		
		public Class<?> getClazz() {
			return clazz;
		}
		
		public JRadioButtonMenuItem getItem() {
			return menuItem;
		}
	}
	
	private static JMenuItem addMenuItem(String name, String action, ActionListener al) {
		JMenuItem i = new JMenuItem(name);
		i.setActionCommand(action);
		i.addActionListener(al);
		return i;
	}
	
	void hideNodeMenu() {
		splitNodeButton.hideMenu();
		splitLinkButton.hideMenu();
	}
	
	public enum Buttons {
		// 
		query("Query Registry", "Query Actor Registry"),
		// requests
		nodes("Add Nodes", "Add new nodes"),
		links("Link Types", "Choose link type"),
		images("Compute Images", "Add or edit compute images"),
		autoip("Auto IP", "Auto-assign IP addresses"),
		reservation("Reservation Details", "Edit reservation details"),
		submit("Submit", "Submit request to selected ORCA controller"),
		availres("Available Resources", "View resource availability of the currently selected controller"),
		//manifests
		listSlices("My Slices", "Query ORCA for list of slices with active reservations"),
		manifest("Query for Manifest", "Query ORCA for slice manifest"),
		raw("Raw Response", "View raw controller response"),
		extend("Extend Reservation", "Extend the end date of the reservation"),
		modify("Commit Modify Actions", "Commit modify slice actions"),
		clearModify("Clear Modify Actions", "Clear modify slice actions"),
		delete("Delete Slice", "Delete this slice"),
		twitter("Twitter", "Check Twitter");
		
		private String name, tooltip;
		Buttons(String name, String tooltip) {
			this.name = name;
			this.tooltip = tooltip;
		}
		
		public String getButton() {
			return name;
		}
		
		public String getToolTip() {
			return tooltip;
		}
		
		public String getCommand() {
			return name();
		}
	}
	
	/**
	 * Create a button, add to toolbar and attach a listener
	 * @param b
	 * @param tb
	 * @param a
	 * @return
	 */
	private JButton createButton(Buttons b, JToolBar tb, ActionListener a) {
		JButton bb = new JButton(b.getButton());
		bb.setToolTipText(b.getToolTip());
		bb.setActionCommand(b.getCommand());
		bb.addActionListener(a);
		bb.setVerticalAlignment(SwingConstants.TOP);
		tb.add(bb);
		return bb;
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	public void initialize() {
		frmOrcaFlukes = new JFrame();
		frmOrcaFlukes.setTitle(FRAME_TITLE);
		//frmOrcaFlukes.getContentPane().setLayout(new BoxLayout(frmOrcaFlukes.getContentPane(), BoxLayout.X_AXIS));
		frmOrcaFlukes.getContentPane().setLayout(new BorderLayout(0,0));
		
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frmOrcaFlukes.getContentPane().add(tabbedPane);
		
		resourcePanel = new JPanel();
		tabbedPane.addTab(GuiTabs.RESOURCE_VIEW.getName(), null, resourcePanel, null);
		resourcePanel.setLayout(new BoxLayout(resourcePanel, BoxLayout.PAGE_AXIS));
		resourcePanel.addComponentListener(this);
		
		requestPanel = new JPanel();
		tabbedPane.addTab(GuiTabs.REQUEST_VIEW.getName(), null, requestPanel, null);
		requestPanel.setLayout(new BoxLayout(requestPanel, BoxLayout.PAGE_AXIS));
		requestPanel.addComponentListener(this);

		manifestPanel = new JPanel();
		tabbedPane.addTab(GuiTabs.MANIFEST_VIEW.getName(), null, manifestPanel, null);
		manifestPanel.setLayout(new BoxLayout(manifestPanel, BoxLayout.PAGE_AXIS));
		manifestPanel.addComponentListener(this);
		
		frmOrcaFlukes.setBounds(100, 100, 1200, 800);
		frmOrcaFlukes.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		{
			// add button panel to resource pane
			JToolBar toolBar = new JToolBar();
			toolBar.setFloatable(false);
			toolBar.setAlignmentX(Component.LEFT_ALIGNMENT);
			toolBar.setAlignmentY(Component.CENTER_ALIGNMENT);
			resourcePanel.add(toolBar);
			
			// add buttons to resource pane toolbar
			ActionListener rbl = GUIResourceState.getInstance().getActionListener();
			
			createButton(Buttons.query, toolBar, rbl);
		}
		
		//
		// add buttons to request pane
		//
		{
			// add button panel to request pane
			JToolBar toolBar = new JToolBar();
			toolBar.setFloatable(false);
			toolBar.setAlignmentX(Component.LEFT_ALIGNMENT);
			toolBar.setAlignmentY(Component.CENTER_ALIGNMENT);
			requestPanel.add(toolBar);
			
			// add buttons to request pane toolbar
			ActionListener rbl = GUIRequestState.getInstance().getActionListener();
			
			JButton nodeButton = createButton(Buttons.nodes, toolBar, rbl);
			
			Component horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);
			
			//first instantiate the control
			splitNodeButton = new SplitButton(nodeButton, SwingConstants.SOUTH);
		    JPopupMenu nodeMenu = new JPopupMenu("Node menu");
		    nodeMenu.add(addMenuItem("Node", "nodes", rbl));
		    nodeMenu.add(addMenuItem("Node Group", "nodegroups", rbl));
		    nodeMenu.add(addMenuItem("Broadcast Link", "bcastlinks", rbl));
		    nodeMenu.add(addMenuItem("Storage", "storage", rbl));
		    nodeMenu.add(addMenuItem("StitchPort", "stitchport", rbl));

		    splitNodeButton.setMenu(nodeMenu);
			toolBar.add(splitNodeButton);
			
			horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);
			
			JButton linkButton = createButton(Buttons.links, toolBar, rbl);
			
			//first instantiate the control
			splitLinkButton = new SplitButton(linkButton, SwingConstants.SOUTH);
		    JPopupMenu linkMenu = new JPopupMenu("Link menu");
		    linkMenu.add(addMenuItem("Topo link", "topo", rbl));
		    linkMenu.add(addMenuItem("Color Link", "color", rbl));
		    splitLinkButton.setMenu(linkMenu);
			toolBar.add(splitLinkButton);
			
			horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);
			
			createButton(Buttons.images, toolBar, rbl);
			
			horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);

			createButton(Buttons.autoip, toolBar, rbl);

			horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);
			
			createButton(Buttons.reservation, toolBar, rbl);
			
			horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);
			
			createButton(Buttons.submit, toolBar, rbl);
			
			horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);
			
			KTextField ktf = new KTextField(20);
			GUIRequestState.getInstance().setSliceIdField(ktf);
			ktf.setToolTipText("Enter slice id");
			ktf.setMaximumSize(ktf.getMinimumSize());
			toolBar.add(ktf);
			
			horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);
			
			createButton(Buttons.availres, toolBar, rbl);
		} 
		//
		// add button panel to manifest pane
		//
		
		{ 
			JToolBar toolBar = new JToolBar();
			toolBar.setFloatable(false);
			toolBar.setAlignmentX(Component.LEFT_ALIGNMENT);
			toolBar.setAlignmentY(Component.TOP_ALIGNMENT);
			manifestPanel.add(toolBar);
			
			// add buttons to resource pane toolbar
			ActionListener rbl = GUIManifestState.getInstance().getActionListener();
			
			createButton(Buttons.listSlices, toolBar, rbl);
			
			Component horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);
			
			createButton(Buttons.manifest, toolBar, rbl);
			
			horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);
			
			KTextField ktf = new KTextField(20);
			// save the field
			GUIManifestState.getInstance().setSliceIdField(ktf);
			ktf.setToolTipText("Enter slice id");
			ktf.setMaximumSize(ktf.getMinimumSize());
			toolBar.add(ktf);
			
			horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);
			
			createButton(Buttons.raw, toolBar, rbl);
			
			horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);

			createButton(Buttons.extend, toolBar, rbl);

			horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);

			if (getPreference(PrefsEnum.ENABLE_MODIFY).equalsIgnoreCase("true") ||
					getPreference(PrefsEnum.ENABLE_MODIFY).equalsIgnoreCase("yes")) {
				createButton(Buttons.modify, toolBar, rbl);

				horizontalStrut = Box.createHorizontalStrut(10);
				toolBar.add(horizontalStrut);

				createButton(Buttons.clearModify, toolBar, rbl);

				horizontalStrut = Box.createHorizontalStrut(10);
				toolBar.add(horizontalStrut);
			}
			
			horizontalStrut = Box.createHorizontalStrut(10);
			createButton(Buttons.delete, toolBar, rbl);
			toolBar.add(horizontalStrut);
			
			createButton(Buttons.twitter, toolBar, rbl);

		} 
		
		GUIResourceState.getInstance().addPane(resourcePanel);
		GUIRequestState.getInstance().addPane(requestPanel);
		GUIManifestState.getInstance().addPane(manifestPanel);
		
		// now the menu
		commonMenus();
		
		// populate default saved layouts
		for (GuiTabs t: GuiTabs.values()) {
			savedLayout.put(t, GraphLayouts.FR);
		}
		
		tabbedPane.setSelectedComponent(requestPanel);
		
	}

	public void componentHidden(ComponentEvent arg0) {
		;
	}

	public void componentMoved(ComponentEvent arg0) {
		;
	}

	public void componentResized(ComponentEvent arg0) {
		;
	}

	private void selectSavedLayout(GraphLayouts l) {
		l.getItem().setSelected(true);
	}
	
	// callback for switching between view tabs and keeping layout
	public void componentShown(ComponentEvent arg0) {
		// Track which tab is showing, adjust the layout menu
		GuiTabs at = activeTab();

		selectSavedLayout(savedLayout.get(at));
		
		switch(at) {
		case RESOURCE_VIEW:

			break;
		case REQUEST_VIEW:
			
			break;
		case MANIFEST_VIEW:
			
			break;
		}
	}
	
	/**
	 * Return user preferences specified in .flukes.properties or default value otherwise.
	 * Never null;
	 * @return
	 */
	public String getPreference(PrefsEnum e) {
		if (prefProperties == null)
			return e.getDefaultValue();
		if (prefProperties.containsKey(e.getPropName()))
			return prefProperties.getProperty(e.getPropName()).trim();
		else
			return e.getDefaultValue();
	}
	
	/**
	 * Programmatically overwrite a preference setting
	 * @param e
	 * @param val
	 */
	public void setPreference(PrefsEnum e, String val) {
		prefProperties.setProperty(e.getPropName(), val);
	}
	
	/**
	 * Allowed properties
	 * @author ibaldin
	 *
	 */
	public enum PrefsEnum {
		XTERM_PATH("xterm.path", "/usr/X11/bin/xterm", 
			"Path to XTerm executable on your system"), 
		PUTTY_PATH("putty.path", "C:/Program Files (x86)/PuTTY/putty.exe", 
			"Path to PuTTY executable on your system (Windows-specific)"), 
		SCRIPT_COMMENT_SEPARATOR("script.comment.separator", "#", 
			"Default comment character used in post-boot scripts"),
		SSH_KEY_SOURCE("ssh.key.source", "file", 
			"Where do SSH keys come from - the file system (then using ssh.key and ssh.pubkey parameters) or Member Authority/GENI Portal. Can be set to 'file' or 'portal'. The MA/portal must have both private and public keys for this to work."),
		SSH_KEY("ssh.key", "~/.ssh/id_dsa", 
			"SSH Private Key to use to access VM instances(public will be installed into instances). You can use ~ to denote user home directory."),
		SSH_PUBKEY("ssh.pubkey", "~/.ssh/id_dsa.pub", "SSH Public key to install into VM instances"),
		SSH_OTHER_LOGIN("ssh.other.login", "root", "Secondary login (works with ssh.other.pubkey)"),
		SSH_OTHER_PUBKEY("ssh.other.pubkey", "~/.ssh/id_rsa.pub", "Secondary public SSH keys (perhaps belonging to other users) that should be installed in the slice."),
		SSH_OTHER_SUDO("ssh.other.sudo", "yes", "Should the secondary account have sudo privileges"),
		SSH_OPTIONS("ssh.options", "-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no", 
			"Options for invoking SSH (the default set turns off checking .ssh/known_hosts)"),
		ORCA_REGISTRY("orca.registry.url", "https://geni.renci.org:15443/registry/",
			"URL of the ORCA actor registry to query"),
		ORCA_REGISTRY_USER("orca.registry.user", "someuser", "Username for actor registry"),
		ORCA_REGISTRY_PASS("orca.registry.pass", "pass", "Password for actor registry"),
		ORCA_REGISTRY_CERT_FINGERPRINT("orca.registry.certfingerprint", "78:B6:1A:F0:6C:F8:C7:0F:C0:05:10:13:06:79:E0:AC",
			"MD5 fingerprint of the certificate used by the registry"),
		USER_KEYSTORE("user.keystore", "~/.ssl/user.jks", 
			"Keystore containing your private key and certificate issued by GPO, Emulab or BEN"),
		USER_CERTFILE("user.certfile", "~/.ssl/user.crt", 
			"CRT or PEM file containing your certificate issued by GPO, Emulab or BEN"),
		USER_CERTKEYFILE("user.certkeyfile", "~/.ssl/user.key", 
			"KEY or PEM file containing your private key issued by GPO, Emulab or BEN"),
		ORCA_XMLRPC_CONTROLLER("orca.xmlrpc.url", "https://some.hostname.org:11443/orca/xmlrpc", 
			"Comma-separated list of URLs of the ORCA XMLRPC controllers where you can submit slice requests"),
		ENABLE_MODIFY("enable.modify", "false", "Enable experimental support for slice modify operations (at your own risk!)"),
		ENABLE_IRODS("enable.irods", "false", "Enable experimental support for iRods (at your own risk!)"),
		AUTOIP_MASK("autoip.mask", "25", "Length of netmask (in bits) to use when assigning IP addresses to groups and broadcast links (simple point-to-point links always use 30 bit masks)"),
		ENABLE_GENISA("enable.genisa", "false", "Enable support for GENI Slice Authority"),
		ENABLE_GENIMA("enable.genima", "false", "Enable support for GENI Member Authority"),
		GENISA_URL("genisa.url", "https://ch.geni.net/SA", "URL of the GENI Slice Authority (defaults to GENI Portal SA)"),
		GENIMA_URL("genima.url", "https://ch.geni.net/MA", "URL of the GENI Member Authority (defaults to GENI Portal MA)"),
		GENISA_PROJECT("genisa.project.urn", "urn:publicid:IDN+ch.geni.net+project+SomeProject", "URN of a project you want this slice to belong to on SA (must be valid on this SA)"),
		IRODS_FORMAT("irods.format", "ndl", "Specify the format in which requests and manifests should be saved to iRods ('ndl' or 'rspec')"),
		IRODS_MANIFEST_TEMPLATE("irods.manifest.template", "${slice.name}/manifest-${date}.${irods.format}", 
				"Specify the format for manifest file names (substitutions are performed, multiple directory levels are respected)"),
		IRODS_REQUEST_TEMPLATE("irods.request.template", "${slice.name}/request-${date}.${irods.format}", 
				"Specify the format for request file names (substitutions are performed, multiple directory levels are respected))"),
		IRODS_ICOMMANDS_PATH("irods.icommands.path", "/usr/bin", "Path to icommands"),
		NDL_CONVERTER_LIST("ndl.converter.list", "http://geni.renci.org:12080/ndl-conversion/, http://bbn-hn.exogeni.net:15080/ndl-conversion/", 
				"Comma-separated list of available NDL converters"),
		CUSTOM_INSTANCE_LIST("custom.instance.list", "", "Comma-separated list of custom instance sizes. For debugging only!"),
		IMAGE_NAME("image.name", "Debian-6-Standard-Multi-Size-Image-v.1.0.6", 
			"Name of a known image, you can add more images by adding image1.name, image2.name etc. To see defined images click on 'Client Images' button."),
		IMAGE_URL("image.url", "http://geni-images.renci.org/images/standard/debian/deb6-neuca-v1.0.7.xml", 
			"URL of a known image description file, you can add more images by adding image1.url, image2.url etc."),
		IMAGE_HASH("image.hash", "ba15fa6f56cc00d354e505259b9cb3804e1bcb73", 
			"SHA-1 hash of the image description file, you can add more images by adding image1.hash, image2.hash etc.");
		
		private final String propName;
		private final String defaultValue;
		private final String comment;
		
		PrefsEnum(String s, String d, String c) {
			propName = s;
			defaultValue = d;
			comment = c;
		}
		
		public String getPropName() {
			return propName;
		}
		
		public String getDefaultValue() {
			return defaultValue;
		}
		
		public String getComment() {
			return comment;
		}
	}
	
	/**
	 * Read and process preferences file
	 */
	 void processPreferences() {
		try {
			File prefs = new File(System.getProperty("user.home"), PREF_FILE);
			FileInputStream is = new FileInputStream(prefs);
			BufferedReader bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			
			prefProperties = new Properties();
			prefProperties.load(bin);
			
		} catch (IOException e) {
			;
		}
	}
	
	/**
	 * Get images from preferences imageX.name imageX.url and imageX.hash
	 * @return list of OrcaImage beans
	 */
	void getImagesFromPreferences() {
		List<OrcaImage> images = new ArrayList<OrcaImage>();
		
		// add the default
		try {
			images.add(new OrcaImage(getPreference(PrefsEnum.IMAGE_NAME), 
					new URL(getPreference(PrefsEnum.IMAGE_URL)), getPreference(PrefsEnum.IMAGE_HASH)));
		} catch (MalformedURLException ue) {
			;
		}
		
		if (prefProperties == null)
			return;
		
		// see if there are more
		int i = 1;
		while(true) {
			String nmProp = "image" + i + ".name";
			String urlProp = "image" + i + ".url";
			String hashProp = "image" + i + ".hash";
			
			String nmPropVal = prefProperties.getProperty(nmProp);
			String urlPropVal = prefProperties.getProperty(urlProp);
			String hashPropVal = prefProperties.getProperty(hashProp);
			if ((nmPropVal != null) && (urlPropVal != null) && (hashPropVal != null)) {
				try {
					if ((nmPropVal.trim().length() > 0) && (urlPropVal.trim().length() > 0) && (hashPropVal.trim().length() > 0))
						images.add(new OrcaImage(nmPropVal.trim(), new URL(urlPropVal.trim()), hashPropVal.trim()));
				} catch (MalformedURLException ue) {
					;
				}
			} else
				break;
			i++;
		}
		
		GUIRequestState.getInstance().addImages(images);
	}
	
	/**
	 * Get images from image registry
	 */
	void getImagesFromRegistry() {
		List<OrcaImage> images = new ArrayList<OrcaImage>();
		
		List<Map<String, String>> regImages = null;
		try {
			regImages = RegistryXMLRPCProxy.getInstance().getImages();
		} catch (Exception e) {
			KMessageDialog md = new KMessageDialog(GUI.getInstance().getFrame(), "Unable to fetch images from image registry.", true);
			md.setMessage("Unable to fetch images from image registry  " + PrefsEnum.ORCA_REGISTRY + ". This is not a fatal error.");
			md.setLocationRelativeTo(GUI.getInstance().getFrame());
			md.setVisible(true);
			return;
		}
		
		for (Map<String, String> regImg: regImages) {
			if ((regImg.get("ImageName") != null) &&
					(regImg.get("ImageURL") != null) && 
					(regImg.get("ImageHash") != null)) {
				try {
					OrcaImage im = new OrcaImage(regImg.get("ImageName"), 
							new URL(regImg.get("ImageURL")), 
							regImg.get("ImageHash"));
					images.add(im);
				} catch (MalformedURLException me) {
					continue;
				}
			}
		}
		GUIRequestState.getInstance().addImages(images);
	}
	
	void getControllersFromPreferences() {
		
		controllerUrls = getPreference(PrefsEnum.ORCA_XMLRPC_CONTROLLER).split(",");
		for (int index = 0; index < controllerUrls.length; index++) {
			controllerUrls[index] = controllerUrls[index].trim();
		}
	}
	
	/**
	 * Create a progress dialog
	 * @param msg
	 * @return
	 */
	static ProgressDialog getProgressDialog(String msg) {
		ProgressDialog pd = new ProgressDialog(GUI.getInstance().getFrame(), true);
		pd.setLocationRelativeTo(GUI.getInstance().getFrame());
		pd.setMessage(msg);
		pd.pack();
		
		return pd;
	}
	
	public String getSelectedController() {
		return selectedControllerUrl;
	}
	
	public void setSelectedController(String url) {
		selectedControllerUrl = url;
	}
	
	private void getIRodsPreferences() {
		if (getPreference(PrefsEnum.ENABLE_IRODS).equalsIgnoreCase("true") ||
				getPreference(PrefsEnum.ENABLE_IRODS).equalsIgnoreCase("yes")) 
			withIRods = true;		
	}
	
	private void getCustomInstancePreferences() {
		String[] customInstances = getPreference(PrefsEnum.CUSTOM_INSTANCE_LIST).split(",");
		
		for (String instance: customInstances) 
			if (instance.startsWith("Euca") || instance.startsWith("EC2")) {
				RequestSaver.addCustomType(instance);
			}
	}
}
