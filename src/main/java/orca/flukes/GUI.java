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
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

import com.hp.hpl.jena.graph.Graph;
import com.hyperrealm.kiwi.ui.AboutFrame;
import com.hyperrealm.kiwi.ui.KFileChooser;
import com.hyperrealm.kiwi.ui.UIChangeManager;
import com.hyperrealm.kiwi.ui.dialog.KFileChooserDialog;
import com.hyperrealm.kiwi.ui.dialog.KQuestionDialog;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.EditingModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.util.Animator;

public class GUI implements ComponentListener {

	private static final String FRAME_TITLE = "ORCA FLUKES - The ORCA Network Editor";
	private static final String FLUKES_HREF_URL = "http://geni-images.renci.org/webstart/";
	private static final String ABOUT_DOC = "html/about.html";
	private static final String HELP_DOC = "html/help.html";
	private JFrame frmOrcaFlukes;
	private JTabbedPane tabbedPane;
	private JPanel requestPanel, resourcePanel, manifestPanel;
	private JToolBar toolBar;
	private JButton nodeButton;
	private JButton nodeGroupButton;
	private Component horizontalStrut;
	private JMenuBar menuBar;
	private JMenu fileNewMenu;
	private JMenuItem newMenuItem;
	private JMenuItem openMenuItem, openManifestMenuItem;
	private JMenuItem saveMenuItem, saveAsMenuItem;
	private JSeparator separator;
	private JMenuItem exitMenuItem;
	private JButton reservationButton;
	private Component horizontalStrut_1;
	private JMenu mnNewMenu, outputMenu, layoutMenu;
	private JMenuItem helpMenuItem;
	private JMenuItem aboutMenuItem;
	private JSeparator separator_1;
	
	private JButton attributesButton;
	private Component horizontalStrut_2;

	private static GUI instance = null;
	private JButton imageButton;
	private Component horizontalStrut_3;
	
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
				d.pack();
				d.setVisible(true);
				if (d.getSelectedFile() != null) {
					GUIRequestState.getInstance().clear();
					if (RequestLoader.getInstance().loadGraph(d.getSelectedFile())) {
						frmOrcaFlukes.setTitle(FRAME_TITLE + " : " + d.getSelectedFile().getName());
						GUIRequestState.getInstance().saveFile = d.getSelectedFile();
					}	
				}
				// kick the layout engine
				switchLayout(savedLayout.get(GuiTabs.REQUEST_VIEW));
			}
			else if (e.getActionCommand().equals("openmanifest")) {
				KFileChooserDialog d = new KFileChooserDialog(getFrame(), "Load NDL manifest", KFileChooser.OPEN_DIALOG);
				d.setLocationRelativeTo(getFrame());
				d.getFileChooser().setAcceptAllFileFilterUsed(true);
				d.getFileChooser().addChoosableFileFilter(new NdlFileFilter());
				d.pack();
				d.setVisible(true);
				if (d.getSelectedFile() != null) {
					GUIManifestState.getInstance().clear();
					ManifestLoader.getInstance().loadGraph(d.getSelectedFile());
				}
				// kick the layout engine
				switchLayout(savedLayout.get(GuiTabs.MANIFEST_VIEW));
			}
			else if (e.getActionCommand().equals("new")) {
				GUIRequestState.getInstance().clear();
				frmOrcaFlukes.setTitle(FRAME_TITLE);
				GUIRequestState.getInstance().vv.repaint();
			}
			else if (e.getActionCommand().equals("save")) {
				if (GUIRequestState.getInstance().saveFile != null) {
					RequestSaver.getInstance().saveGraph(GUIRequestState.getInstance().saveFile, 
							GUIRequestState.getInstance().requestGraph);
				}
			}
			else if (e.getActionCommand().equals("saveas")) {
				KFileChooserDialog d = new KFileChooserDialog(getFrame(), "Save Request in NDL", KFileChooser.SAVE_DIALOG);
				d.setLocationRelativeTo(getFrame());
				d.getFileChooser().setAcceptAllFileFilterUsed(true);
				d.getFileChooser().addChoosableFileFilter(new NdlFileFilter());
				d.pack();
				d.setVisible(true);
				if (d.getSelectedFile() != null) {
					if (RequestSaver.getInstance().saveGraph(d.getSelectedFile(), GUIRequestState.getInstance().requestGraph)) {
						frmOrcaFlukes.setTitle(FRAME_TITLE + " : " + d.getSelectedFile().getName());
						GUIRequestState.getInstance().saveFile = d.getSelectedFile();
					}
				}
			}
			else if (e.getActionCommand().equals("help"))
				helpDialog();
			else if (e.getActionCommand().equals("about"))
				aboutDialog();
			else if (e.getActionCommand().equals("xml"))
				RequestSaver.getInstance().setOutputFormat(RequestSaver.RDF_XML_FORMAT);
			else if (e.getActionCommand().equals("n3"))
				RequestSaver.getInstance().setOutputFormat(RequestSaver.N3_FORMAT);
			else if (e.getActionCommand().equals(GraphLayouts.KK.getName())) {
				switchLayout(GraphLayouts.KK);
			} else if (e.getActionCommand().equals(GraphLayouts.FR.getName())) {
				switchLayout(GraphLayouts.FR);
			} else if (e.getActionCommand().equals(GraphLayouts.ISOM.getName())) {
				switchLayout(GraphLayouts.ISOM);
			} 
		}
	}
	
	private void switchLayout(GraphLayouts l) {
		//final Layout<OrcaNode, OrcaLink> oldL = vv.getGraphLayout();
		Layout<OrcaNode, OrcaLink> newL = null;
		
		VisualizationViewer<OrcaNode,OrcaLink> myVv = null;
		SparseMultigraph<OrcaNode, OrcaLink> myGraph = null;
		
		GuiTabs at = activeTab();
		switch(at) {
		case RESOURCE_VIEW:
			break;
		case REQUEST_VIEW:
			myVv = GUIRequestState.getInstance().vv;
			myGraph = GUIRequestState.getInstance().requestGraph;
			break;
		case MANIFEST_VIEW:
			myVv = GUIManifestState.getInstance().vv;
			myGraph = GUIManifestState.getInstance().manifestGraph;
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
	
	/**
	 * Request pane button actions
	 * @author ibaldin
	 *
	 */
	public class RequestButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("images")) {
				GUIRequestState.getInstance().icd = new ImageChooserDialog(getFrame());
				GUIRequestState.getInstance().icd.pack();
				GUIRequestState.getInstance().icd.setVisible(true);
			} else if (e.getActionCommand().equals("reservation")) {
				GUIRequestState.getInstance().rdd = new ReservationDetailsDialog(getFrame());
				GUIRequestState.getInstance().rdd.setFields(GUIRequestState.getInstance().getVMImageInReservation(), 
						GUIRequestState.getInstance().getDomainInReservation(),
						GUIRequestState.getInstance().getTerm());
				GUIRequestState.getInstance().rdd.pack();
				GUIRequestState.getInstance().rdd.setVisible(true);
			} else if (e.getActionCommand().equals("nodes")) {
				GUIRequestState.getInstance().nodesOrGroups = true;
			} else if (e.getActionCommand().equals("nodegroups")) {
				GUIRequestState.getInstance().nodesOrGroups = false;
			}
		}
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
	
	/**
	 * Initialize request pane 
	 */
	protected void requestPane(Container c) {
		GUIRequestState.getInstance().requestGraph = 
			new SparseMultigraph<OrcaNode, OrcaLink>();
		// Layout<V, E>, VisualizationViewer<V,E>
		//	        Map<OrcaNode,Point2D> vertexLocations = new HashMap<OrcaNode, Point2D>();
		
		Layout<OrcaNode, OrcaLink> layout = new FRLayout<OrcaNode, OrcaLink>(GUIRequestState.getInstance().requestGraph);
		
		//layout.setSize(new Dimension(1000,800));
		GUIRequestState.getInstance().vv = 
			new VisualizationViewer<OrcaNode,OrcaLink>(layout);
		// Show vertex and edge labels
		GUIRequestState.getInstance().vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<OrcaNode>());
		GUIRequestState.getInstance().vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<OrcaLink>());
		
		// Create a graph mouse and add it to the visualization viewer
		OrcaNode.OrcaNodeFactory onf = new OrcaNode.OrcaNodeFactory(GUIRequestState.getInstance());
		OrcaLink.OrcaLinkFactory olf = new OrcaLink.OrcaLinkFactory();
		GUIRequestState.getInstance().gm = new EditingModalGraphMouse<OrcaNode, OrcaLink>(GUIRequestState.getInstance().vv.getRenderContext(), 
				onf, olf);
		
		// Set some defaults for the Edges...
		olf.setDefaultBandwidth(10000000);
		olf.setDefaultLatency(5000);
		
		// add the plugin
		PopupVertexEdgeMenuMousePlugin<OrcaNode, OrcaLink> myPlugin = new PopupVertexEdgeMenuMousePlugin<OrcaNode, OrcaLink>();
		
		// Add some popup menus for the edges and vertices to our mouse plugin.
		myPlugin.setEdgePopup(new MouseMenus.RequestEdgeMenu());
		myPlugin.setVertexPopup(new MouseMenus.RequestNodeMenu());
		myPlugin.setModePopup(new MouseMenus.ModeMenu());
		GUIRequestState.getInstance().gm.remove(GUIRequestState.getInstance().gm.getPopupEditingPlugin());  // Removes the existing popup editing plugin
		GUIRequestState.getInstance().gm.add(myPlugin);

		// Add icon and shape (so pickable areal roughly matches the icon) transformer
		OrcaNode.OrcaNodeIconShapeTransformer st = new OrcaNode.OrcaNodeIconShapeTransformer();
		GUIRequestState.getInstance().vv.getRenderContext().setVertexShapeTransformer(st);
		
		OrcaNode.OrcaNodeIconTransformer it = new OrcaNode.OrcaNodeIconTransformer();
		GUIRequestState.getInstance().vv.getRenderContext().setVertexIconTransformer(it);
		
		// add listener to add/remove checkmarks on selected nodes
		PickedState<OrcaNode> ps = GUIRequestState.getInstance().vv.getPickedVertexState();
        ps.addItemListener(new OrcaNode.PickWithIconListener(it));
		
		GUIRequestState.getInstance().vv.setGraphMouse(GUIRequestState.getInstance().gm);

		GUIRequestState.getInstance().vv.setLayout(new BorderLayout(0,0));
		
		c.add(GUIRequestState.getInstance().vv);

		GUIRequestState.getInstance().gm.setMode(ModalGraphMouse.Mode.EDITING); // Start off in editing mode  
	}
	
	/**
	 * Initialize manifest pane
	 * @param c
	 */
	protected void manifestPane(Container c) {
		GUIManifestState.getInstance().manifestGraph = 
			new SparseMultigraph<OrcaNode, OrcaLink>();
		
		Layout<OrcaNode, OrcaLink> layout = new FRLayout<OrcaNode, OrcaLink>(GUIManifestState.getInstance().manifestGraph);
		
		//layout.setSize(new Dimension(1000,800));
		GUIManifestState.getInstance().vv = 
			new VisualizationViewer<OrcaNode,OrcaLink>(layout);
		// Show vertex and edge labels
		GUIManifestState.getInstance().vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<OrcaNode>());
		GUIManifestState.getInstance().vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<OrcaLink>());
		
		// Create a graph mouse and add it to the visualization viewer
		OrcaNode.OrcaNodeFactory onf = new OrcaNode.OrcaNodeFactory(GUIManifestState.getInstance());
		OrcaLink.OrcaLinkFactory olf = new OrcaLink.OrcaLinkFactory();
		GUIManifestState.getInstance().gm = new EditingModalGraphMouse<OrcaNode, OrcaLink>(GUIManifestState.getInstance().vv.getRenderContext(), 
				onf, olf);
		
		// add the plugin
		PopupVertexEdgeMenuMousePlugin<OrcaNode, OrcaLink> myPlugin = new PopupVertexEdgeMenuMousePlugin<OrcaNode, OrcaLink>();
		
		// Add some popup menus for the edges and vertices to our mouse plugin.
		// mode menu is not set for manifests
		myPlugin.setEdgePopup(new MouseMenus.ManifestEdgeMenu());
		myPlugin.setVertexPopup(new MouseMenus.ManifestNodeMenu());
		
		GUIManifestState.getInstance().gm.remove(GUIManifestState.getInstance().gm.getPopupEditingPlugin());  // Removes the existing popup editing plugin
		GUIManifestState.getInstance().gm.add(myPlugin);

		// Add icon and shape (so pickable area roughly matches the icon) transformer
		OrcaNode.OrcaNodeIconShapeTransformer st = new OrcaNode.OrcaNodeIconShapeTransformer();
		GUIManifestState.getInstance().vv.getRenderContext().setVertexShapeTransformer(st);
		
		OrcaNode.OrcaNodeIconTransformer it = new OrcaNode.OrcaNodeIconTransformer();
		GUIManifestState.getInstance().vv.getRenderContext().setVertexIconTransformer(it);
		
		// add listener to add/remove checkmarks on selected nodes
		PickedState<OrcaNode> ps = GUIManifestState.getInstance().vv.getPickedVertexState();
        ps.addItemListener(new OrcaNode.PickWithIconListener(it));
		
		GUIManifestState.getInstance().vv.setGraphMouse(GUIManifestState.getInstance().gm);

		GUIManifestState.getInstance().vv.setLayout(new BorderLayout(0,0));
		
		c.add(GUIManifestState.getInstance().vv);

		GUIManifestState.getInstance().gm.setMode(ModalGraphMouse.Mode.TRANSFORMING); // Start off in panning mode  
	}
	
	private void aboutDialog() {
		try {
			AboutFrame ab = new AboutFrame("About FLUKES", new URL(FLUKES_HREF_URL + ABOUT_DOC));
			ab.setVisible(true);
		} catch (MalformedURLException e) {
			;
		}
	}
	
	private void helpDialog() {
		try {
			AboutFrame ab = new AboutFrame("About FLUKES", new URL(FLUKES_HREF_URL + HELP_DOC));
			ab.setVisible(true);
		} catch (MalformedURLException e) {
			;
		}
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
		resourcePanel.setLayout(new BorderLayout(0, 0));
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

		toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.setAlignmentX(Component.LEFT_ALIGNMENT);
		toolBar.setAlignmentY(Component.CENTER_ALIGNMENT);
		requestPanel.add(toolBar);
		
		RequestButtonListener rbl = new RequestButtonListener();
		
		nodeButton = new JButton("Add Nodes");
		nodeButton.setToolTipText("Add new nodes");
		nodeButton.setActionCommand("nodes");
		nodeButton.addActionListener(rbl);
		nodeButton.setVerticalAlignment(SwingConstants.TOP);
		toolBar.add(nodeButton);
		
		horizontalStrut = Box.createHorizontalStrut(10);
		toolBar.add(horizontalStrut);
		
		nodeGroupButton = new JButton("Add Node Groups");
		nodeGroupButton.setToolTipText("Add new node groups");
		nodeGroupButton.setActionCommand("nodegroups");
		nodeGroupButton.addActionListener(rbl);
		nodeGroupButton.setVerticalAlignment(SwingConstants.TOP);
		toolBar.add(nodeGroupButton);
		
		horizontalStrut_1 = Box.createHorizontalStrut(10);
		toolBar.add(horizontalStrut_1);
		
		imageButton = new JButton("Client Images");
		imageButton.setToolTipText("Add or edit VM images");
		imageButton.setActionCommand("images");
		imageButton.addActionListener(rbl);
		toolBar.add(imageButton);
		
		horizontalStrut_3 = Box.createHorizontalStrut(10);
		toolBar.add(horizontalStrut_3);
		
		reservationButton = new JButton("Edit Reservation");
		reservationButton.setToolTipText("Edit reservation details");
		reservationButton.setActionCommand("reservation");
		reservationButton.addActionListener(rbl);
		toolBar.add(reservationButton);
		
//		horizontalStrut_2 = Box.createHorizontalStrut(10);
//		toolBar.add(horizontalStrut_2);
//		
//		attributesButton = new JButton("Edit Attributes");
//		toolBar.add(attributesButton);
				
		requestPane(requestPanel);
		manifestPane(manifestPanel);

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
	
	// callback for switching between view tabs
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

}
