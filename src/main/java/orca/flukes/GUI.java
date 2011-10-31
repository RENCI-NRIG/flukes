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
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
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
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

import orca.flukes.ndl.ManifestLoader;
import orca.flukes.ndl.RequestLoader;
import orca.flukes.ndl.RequestSaver;
import orca.flukes.ui.TextAreaDialog;

import com.hyperrealm.kiwi.ui.AboutFrame;
import com.hyperrealm.kiwi.ui.KFileChooser;
import com.hyperrealm.kiwi.ui.KTextArea;
import com.hyperrealm.kiwi.ui.KTextField;
import com.hyperrealm.kiwi.ui.UIChangeManager;
import com.hyperrealm.kiwi.ui.dialog.KFileChooserDialog;
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

	public static final String buildVersion = GUI.class.getPackage().getImplementationVersion();
	public static final String aboutText = "ORCA FLUKES " + (buildVersion == null? "" : buildVersion) + "\nNDL-OWL network editor for ORCA (Open Resource Control Architecture)" +
	"\nDeveloped using Jena Semantic Web Framework, JUNG Java Universal Network/Graph Framework and Kiwi Swing toolkit." +
	"\n\nCopyright 2011 RENCI/UNC Chapel Hill";
	private static final String FRAME_TITLE = "ORCA FLUKES - The ORCA Network Editor";
	private static final String FLUKES_HREF_URL = "http://geni-images.renci.org/webstart/";
	private static final String HELP_DOC = "html/help.html";
	private static final String PREF_FILE = ".flukes.properties";
	private JFrame frmOrcaFlukes;
	private JTabbedPane tabbedPane;
	private JPanel requestPanel, resourcePanel, manifestPanel;
	private JMenuBar menuBar;
	private JMenu fileNewMenu;
	private JMenuItem newMenuItem;
	private JMenuItem openMenuItem, openManifestMenuItem;
	private JMenuItem saveMenuItem, saveAsMenuItem;
	private JSeparator separator;
	private JMenuItem exitMenuItem;
	private JMenu mnNewMenu, outputMenu, layoutMenu;
	private JMenuItem helpMenuItem;
	private JMenuItem prefMenuItem;
	private JMenuItem aboutMenuItem;
	private JSeparator separator_1, separator_2;
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
				// kick the layout engine
				switchLayout(GuiTabs.REQUEST_VIEW, savedLayout.get(GuiTabs.REQUEST_VIEW));
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
				// kick the layout engine
				switchLayout(GuiTabs.MANIFEST_VIEW, savedLayout.get(GuiTabs.MANIFEST_VIEW));
			}
			else if (e.getActionCommand().equals("new")) {
				GUIRequestState.getInstance().clear();
				frmOrcaFlukes.setTitle(FRAME_TITLE);
				GUIRequestState.getInstance().vv.repaint();
			}
			else if (e.getActionCommand().equals("save")) {
				if (GUIRequestState.getInstance().getSaveFile() != null) {
					RequestSaver.getInstance().saveGraph(GUIRequestState.getInstance().getSaveFile(), 
							GUIRequestState.getInstance().g);
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
						if (RequestSaver.getInstance().saveGraph(d.getSelectedFile(), GUIRequestState.getInstance().g)) {
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
					if (RequestSaver.getInstance().saveGraph(d.getSelectedFile(), GUIRequestState.getInstance().g)) {
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
		
		if (myGraph.getVertexCount() == 0)
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
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI gui = GUI.getInstance();
					gui.initialize();
					gui.processPreferences();
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
			AboutFrame ab = new AboutFrame("FLUKES Help", new URL(FLUKES_HREF_URL + HELP_DOC));
			ab.setVisible(true);
		} catch (MalformedURLException e) {
			;
		}
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
		
		newMenuItem = new JMenuItem("New");
		newMenuItem.setActionCommand("new");
		newMenuItem.addActionListener(mListener);
		fileNewMenu.add(newMenuItem);

		openManifestMenuItem = new JMenuItem("Open Manifest...");
		openManifestMenuItem.setActionCommand("openmanifest");
		openManifestMenuItem.addActionListener(mListener);
		fileNewMenu.add(openManifestMenuItem);
		
		JSeparator sep = new JSeparator();
		fileNewMenu.add(sep);
		
		openMenuItem = new JMenuItem("Open Request...");
		openMenuItem.setActionCommand("open");
		openMenuItem.addActionListener(mListener);
		fileNewMenu.add(openMenuItem);
		
		saveMenuItem = new JMenuItem("Save Request");
		saveMenuItem.setActionCommand("save");
		saveMenuItem.addActionListener(mListener);
		fileNewMenu.add(saveMenuItem);
		
		saveAsMenuItem = new JMenuItem("Save Request As...");
		saveAsMenuItem.setActionCommand("saveas");
		saveAsMenuItem.addActionListener(mListener);
		fileNewMenu.add(saveAsMenuItem);
		
		separator = new JSeparator();
		fileNewMenu.add(separator);
		
		exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.setActionCommand("exit");
		exitMenuItem.addActionListener(mListener);
		fileNewMenu.add(exitMenuItem);
		
		// output format selection
		outputMenu = new JMenu("Output Format");
		menuBar.add(outputMenu);
		
		ButtonGroup obg = new ButtonGroup();

		JRadioButtonMenuItem mi = new JRadioButtonMenuItem("RDF-XML");
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
		
		prefMenuItem = new JMenuItem("Preference settings");
		prefMenuItem.setActionCommand("prefs");
		prefMenuItem.addActionListener(mListener);
		mnNewMenu.add(prefMenuItem);
		
		separator_2 = new JSeparator();
		mnNewMenu.add(separator_2);
		
		helpMenuItem = new JMenuItem("Help Contents");
		helpMenuItem.setActionCommand("help");
		helpMenuItem.addActionListener(mListener);
		mnNewMenu.add(helpMenuItem);
		
		separator_1 = new JSeparator();
		mnNewMenu.add(separator_1);
		
		aboutMenuItem = new JMenuItem("About");
		aboutMenuItem.setActionCommand("about");
		aboutMenuItem.addActionListener(mListener);
		mnNewMenu.add(aboutMenuItem);
			
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
		
		frmOrcaFlukes.setBounds(100, 100, 1000, 800);
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
			
			JButton queryButton = new JButton("Query Registry");
			queryButton.setToolTipText("Query Actor Registry");
			queryButton.setActionCommand("query");
			queryButton.addActionListener(rbl);
			queryButton.setVerticalAlignment(SwingConstants.TOP);
			toolBar.add(queryButton);
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
			
			JButton nodeButton = new JButton("Add Nodes");
			nodeButton.setToolTipText("Add new nodes");
			nodeButton.setActionCommand("nodes");
			nodeButton.addActionListener(rbl);
			nodeButton.setVerticalAlignment(SwingConstants.TOP);
			toolBar.add(nodeButton);
			
			Component horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);
			
			JButton nodeGroupButton = new JButton("Add Node Groups");
			nodeGroupButton.setToolTipText("Add new node groups");
			nodeGroupButton.setActionCommand("nodegroups");
			nodeGroupButton.addActionListener(rbl);
			nodeGroupButton.setVerticalAlignment(SwingConstants.TOP);
			toolBar.add(nodeGroupButton);
			
			horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);
			
			JButton imageButton = new JButton("Client Images");
			imageButton.setToolTipText("Add or edit VM images");
			imageButton.setActionCommand("images");
			imageButton.addActionListener(rbl);
			toolBar.add(imageButton);
			
			horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);
			
			JButton reservationButton = new JButton("Edit Reservation");
			reservationButton.setToolTipText("Edit reservation details");
			reservationButton.setActionCommand("reservation");
			reservationButton.addActionListener(rbl);
			toolBar.add(reservationButton);
			
			horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);
			
			JButton submitButton = new JButton("Submit Request");
			submitButton.setToolTipText("Submit request to ORCA controller");
			submitButton.setActionCommand("submit");
			submitButton.addActionListener(rbl);
			submitButton.setVerticalAlignment(SwingConstants.TOP);
			toolBar.add(submitButton);
			
			horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);
			
			KTextField ktf = new KTextField(20);
			GUIRequestState.getInstance().setSliceIdField(ktf);
			ktf.setToolTipText("Enter slice id");
			ktf.setMaximumSize(ktf.getMinimumSize());
			toolBar.add(ktf);
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
			
			JButton queryButton = new JButton("Get Slice Manifest");
			queryButton.setToolTipText("Query ORCA for slice manifest");
			queryButton.setActionCommand("manifest");
			queryButton.addActionListener(rbl);
			queryButton.setVerticalAlignment(SwingConstants.TOP);
			toolBar.add(queryButton);
			
			Component horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);
			
			KTextField ktf = new KTextField(20);
			// save the field
			GUIManifestState.getInstance().setSliceIdField(ktf);
			ktf.setToolTipText("Enter slice id");
			ktf.setMaximumSize(ktf.getMinimumSize());
			toolBar.add(ktf);
			
			horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);
			
			JButton rawButton = new JButton("Raw Response");
			rawButton.setToolTipText("View raw controller response");
			rawButton.setActionCommand("raw");
			rawButton.addActionListener(rbl);
			rawButton.setVerticalAlignment(SwingConstants.TOP);
			toolBar.add(rawButton);
			
			horizontalStrut = Box.createHorizontalStrut(10);
			toolBar.add(horizontalStrut);
			
			JButton deleteButton = new JButton("Delete slice");
			deleteButton.setToolTipText("Delete slice");
			deleteButton.setActionCommand("delete");
			deleteButton.addActionListener(rbl);
			deleteButton.setVerticalAlignment(SwingConstants.TOP);
			toolBar.add(deleteButton);

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
			return prefProperties.getProperty(e.getPropName());
		else
			return e.getDefaultValue();
	}
	
	/**
	 * Allowed properties
	 * @author ibaldin
	 *
	 */
	public enum PrefsEnum {
		XTERM_PATH("xterm.path", "/usr/X11/bin/xterm", 
			"Path to XTerm executable on your system"), 
		SCRIPT_COMMENT_SEPARATOR("script.comment.separator", "#", 
			"Default comment character used in post-boot scripts"),
		SSH_KEY("ssh.key", "~/.ssh/id_dsa", 
			"SSH Key to use (public will be installed into instances). You can use ~ to denote user home directory."),
		SSH_PUBKEY("ssh.pubkey", "~/.ssh/id_dsa.pub", "SSH public key to install into instances"),
		SSH_OPTIONS("ssh.options", "-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no", 
			"Options for invoking SSH (the default set turns off checking .ssh/known_hosts"),
		ORCA_REGISTRY("orca.registry.url", "https://geni.renci.org:12443/registry/",
			"URL of the ORCA actor registry to query"),
		ORCA_REGISTRY_CERT_FINGERPRINT("orca.registry.certfingerprint", "49:67:81:66:C0:BA:CC:82:7A:94:2B:B9:EC:00:4D:98",
			"MD5 fingerprint of the certificate used by the registry"),
		ORCA_XMLRPC_CONTROLLER("orca.xmlrpc.url", "http://geni.renci.org:11080/orca/xmlrpc", 
			"URL of the ORCA XMLRPC controller");
		
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
	private void processPreferences() {
		Properties p = System.getProperties();
		
		String prefFilePath = "" + p.getProperty("user.home") + p.getProperty("file.separator") + PREF_FILE;
		try {
			File prefs = new File(prefFilePath);
			FileInputStream is = new FileInputStream(prefs);
			BufferedReader bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			
			prefProperties = new Properties();
			prefProperties.load(bin);
			
		} catch (IOException e) {
			;
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

}
