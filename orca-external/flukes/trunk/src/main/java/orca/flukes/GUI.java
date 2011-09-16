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
import java.net.MalformedURLException;
import java.net.URL;

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
import edu.uci.ics.jung.visualization.control.AbstractModalGraphMouse;
import edu.uci.ics.jung.visualization.control.EditingModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.picking.PickedState;

public class GUI {

	private static final String FLUKES_HREF_URL = "http://geni-images.renci.org/webstart/";
	private static final String ABOUT_DOC = "html/about.html";
	private static final String HELP_DOC = "html/help.html";
	private JFrame frmOrcaFlukes;
	private JPanel requestPanel, resourcePanel;
	private JToolBar toolBar;
	private JButton nodeButton;
	private JButton clusterButton;
	private Component horizontalStrut;
	private JMenuBar menuBar;
	private JMenu fileNewMenu;
	private JMenuItem newMenuItem;
	private JMenuItem openMenuItem;
	private JMenuItem saveMenuItem;
	private JSeparator separator;
	private JMenuItem exitMenuItem;
	private JButton reservationButton;
	private Component horizontalStrut_1;
	private JMenu mnNewMenu, outputMenu, layoutMenu;
	private JMenuItem helpMenuItem;
	private JMenuItem aboutMenuItem;
	private JSeparator separator_1;
	
	private VisualizationViewer<OrcaNode,OrcaLink> vv;
	
	private EditingModalGraphMouse<OrcaNode, OrcaLink> gm;
	private JButton attributesButton;
	private Component horizontalStrut_2;

	private static GUI instance = null;
	private JButton imageButton;
	private Component horizontalStrut_3;
	
	// All menu actions here
	/**
	 * Menu actions
	 */
	public class MenuListener implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("exit")) 
				quit();
			else if (e.getActionCommand().equals("open")) {
				// do a transition from static layout to some sane layout back to static
				GUIState.getInstance().clear();
				
				Layout<OrcaNode, OrcaLink> oldL = vv.getGraphLayout();
				Layout<OrcaNode, OrcaLink> newL = new FRLayout<OrcaNode, OrcaLink>(GUIState.getInstance().g);
				vv.setGraphLayout(newL);
				
				KFileChooserDialog d = new KFileChooserDialog(getFrame(), "Load NDL", KFileChooser.OPEN_DIALOG);
				d.setLocationRelativeTo(getFrame());
				d.pack();
				d.setVisible(true);
				if (d.getSelectedFile() != null)
					GraphLoader.getInstance().loadGraph(d.getSelectedFile());

				//LayoutTransition<OrcaNode, OrcaLink> lt1 = new LayoutTransition<OrcaNode, OrcaLink>(vv, oldL, newL);
				//Animator an1 = new Animator(lt1);
				//an1.start();
				//vv.getRenderContext().getMultiLayerTransformer().setToIdentity();

				vv.repaint();
			}
			else if (e.getActionCommand().equals("new")) {
				GUIState.getInstance().clear();
				vv.repaint();
			}
			else if (e.getActionCommand().equals("save")) {
				KFileChooserDialog d = new KFileChooserDialog(getFrame(), "Save in NDL", KFileChooser.SAVE_DIALOG);
				d.setLocationRelativeTo(getFrame());
				d.pack();
				d.setVisible(true);
				if (d.getSelectedFile() != null)
					GraphSaver.getInstance().saveGraph(d.getSelectedFile(), GUIState.getInstance().g);
			}
			else if (e.getActionCommand().equals("help"))
				helpDialog();
			else if (e.getActionCommand().equals("about"))
				aboutDialog();
			else if (e.getActionCommand().equals("xml"))
				GraphSaver.getInstance().setOutputFormat(GraphSaver.RDF_XML_FORMAT);
			else if (e.getActionCommand().equals("n3"))
				GraphSaver.getInstance().setOutputFormat(GraphSaver.N3_FORMAT);
			else if (e.getActionCommand().equals("kklayout")) {
				Layout<OrcaNode, OrcaLink> newL = new KKLayout<OrcaNode, OrcaLink>(GUIState.getInstance().g);
				vv.setGraphLayout(newL);
				vv.repaint();
			} else if (e.getActionCommand().equals("frlayout")) {
				Layout<OrcaNode, OrcaLink> newL = new FRLayout<OrcaNode, OrcaLink>(GUIState.getInstance().g);
				vv.setGraphLayout(newL);
				vv.repaint();
			} else if (e.getActionCommand().equals("isomlayout")) {
				Layout<OrcaNode, OrcaLink> newL = new ISOMLayout<OrcaNode, OrcaLink>(GUIState.getInstance().g);
				vv.setGraphLayout(newL);
				vv.repaint();
			} 
		}
	}
	
	/**
	 * Request pane button actions
	 * @author ibaldin
	 *
	 */
	public class RequestButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("images")) {
				GUIState.getInstance().icd = new ImageChooserDialog(getFrame());
				GUIState.getInstance().icd.pack();
				GUIState.getInstance().icd.setVisible(true);
			} else if (e.getActionCommand().equals("reservation")) {
				GUIState.getInstance().rdd = new ReservationDetailsDialog(getFrame());
				GUIState.getInstance().rdd.setFields(GUIState.getInstance().getVMImageInReservation(), 
						GUIState.getInstance().getDomainInReservation(),
						GUIState.getInstance().getTerm());
				GUIState.getInstance().rdd.pack();
				GUIState.getInstance().rdd.setVisible(true);
			} else if (e.getActionCommand().equals("nodes")) {
				GUIState.getInstance().nodesOrClusters = true;
			} else if (e.getActionCommand().equals("clusters")) {
				GUIState.getInstance().nodesOrClusters = false;
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
	
	public AbstractModalGraphMouse getMouse() {
		return gm;
	}
	
	/**
	 * Initialize request pane 
	 */
	protected void requestPane(Container c) {
		GUIState.getInstance().g = 
			new SparseMultigraph<OrcaNode, OrcaLink>();
		// Layout<V, E>, VisualizationViewer<V,E>
		//	        Map<OrcaNode,Point2D> vertexLocations = new HashMap<OrcaNode, Point2D>();
		
		Layout<OrcaNode, OrcaLink> layout = new FRLayout<OrcaNode, OrcaLink>(GUIState.getInstance().g);
		
		//layout.setSize(new Dimension(1000,800));
		vv = 
			new VisualizationViewer<OrcaNode,OrcaLink>(layout);
		// Show vertex and edge labels
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<OrcaNode>());
		vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<OrcaLink>());
		
		// Create a graph mouse and add it to the visualization viewer
		gm = new EditingModalGraphMouse<OrcaNode, OrcaLink>(vv.getRenderContext(), 
				OrcaNode.OrcaNodeFactory.getInstance(),
				OrcaLink.OrcaLinkFactory.getInstance()); 
		
		// Set some defaults for the Edges...
		OrcaLink.OrcaLinkFactory.setDefaultBandwidth(10000000);
		OrcaLink.OrcaLinkFactory.setDefaultLatency(5000);
		
		// add the plugin
		PopupVertexEdgeMenuMousePlugin<OrcaNode, OrcaLink> myPlugin = new PopupVertexEdgeMenuMousePlugin<OrcaNode, OrcaLink>();
		
		// Add some popup menus for the edges and vertices to our mouse plugin.
		myPlugin.setEdgePopup(new MouseMenus.EdgeMenu());
		myPlugin.setVertexPopup(new MouseMenus.NodeMenu());
		myPlugin.setModePopup(new MouseMenus.ModeMenu());
		gm.remove(gm.getPopupEditingPlugin());  // Removes the existing popup editing plugin
		gm.add(myPlugin);

		// Add icon and shape (so pickable areal roughly matches the icon) transformer
		OrcaNode.OrcaNodeIconShapeTransformer st = new OrcaNode.OrcaNodeIconShapeTransformer();
		vv.getRenderContext().setVertexShapeTransformer(st);
		
		OrcaNode.OrcaNodeIconTransformer it = new OrcaNode.OrcaNodeIconTransformer();
		vv.getRenderContext().setVertexIconTransformer(it);
		
		// add listener to add/remove checkmarks on selected nodes
		PickedState<OrcaNode> ps = vv.getPickedVertexState();
        ps.addItemListener(new OrcaNode.PickWithIconListener(it));
		
		vv.setGraphMouse(gm);

		vv.setLayout(new BorderLayout(0,0));
		
		c.add(vv);

		gm.setMode(ModalGraphMouse.Mode.EDITING); // Start off in editing mode  
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
		MenuListener mListener = new MenuListener();
		
		// populate the menu
		menuBar = new JMenuBar();
		frmOrcaFlukes.setJMenuBar(menuBar);
		
		fileNewMenu = new JMenu("File ");
		menuBar.add(fileNewMenu);
		
		newMenuItem = new JMenuItem("New");
		newMenuItem.setActionCommand("new");
		newMenuItem.addActionListener(mListener);
		fileNewMenu.add(newMenuItem);
		
		openMenuItem = new JMenuItem("Open...");
		openMenuItem.setActionCommand("open");
		openMenuItem.addActionListener(mListener);
		fileNewMenu.add(openMenuItem);
		
		saveMenuItem = new JMenuItem("Save...");
		saveMenuItem.setActionCommand("save");
		saveMenuItem.addActionListener(mListener);
		fileNewMenu.add(saveMenuItem);
		
		separator = new JSeparator();
		fileNewMenu.add(separator);
		
		exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.setActionCommand("exit");
		exitMenuItem.addActionListener(mListener);
		fileNewMenu.add(exitMenuItem);
		
		// Let's add a menu for changing mouse modes
		/*	JMenu modeMenu = gm.getModeMenu();
		modeMenu.setText("Mouse Mode");
		modeMenu.setPreferredSize(new Dimension(120,20)); 
		menuBar.add(modeMenu);*/
		
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
				
		mi = new JRadioButtonMenuItem("Karmada-Kawai");
		mi.setActionCommand("kklayout");
		mi.addActionListener(mListener);
		layoutMenu.add(mi);
		lbg.add(mi);
		
		mi = new JRadioButtonMenuItem("Fruchterman-Rheingold");
		mi.setActionCommand("frlayout");
		mi.setSelected(true);
		mi.addActionListener(mListener);
		layoutMenu.add(mi);
		lbg.add(mi);
		
		mi = new JRadioButtonMenuItem("Self-organizing");
		mi.setActionCommand("isomlayout");
		mi.addActionListener(mListener);
		layoutMenu.add(mi);
		lbg.add(mi);
		
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
	
	/**
	 * Initialize the contents of the frame.
	 */
	public void initialize() {
		frmOrcaFlukes = new JFrame();
		frmOrcaFlukes.setTitle("ORCA FLUKES - The ORCA Network Editor");
		//frmOrcaFlukes.getContentPane().setLayout(new BoxLayout(frmOrcaFlukes.getContentPane(), BoxLayout.X_AXIS));
		frmOrcaFlukes.getContentPane().setLayout(new BorderLayout(0,0));
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frmOrcaFlukes.getContentPane().add(tabbedPane);
		
		requestPanel = new JPanel();
		tabbedPane.addTab("Request View", null, requestPanel, null);
		
		resourcePanel = new JPanel();
		tabbedPane.addTab("Resource View", null, resourcePanel, null);
		resourcePanel.setLayout(new BorderLayout(0, 0));
		
		frmOrcaFlukes.setBounds(100, 100, 1000, 800);
		frmOrcaFlukes.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		requestPanel.setLayout(new BoxLayout(requestPanel, BoxLayout.PAGE_AXIS));

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
		
		clusterButton = new JButton("Add Clusters");
		clusterButton.setToolTipText("Add new clusters");
		clusterButton.setActionCommand("clusters");
		clusterButton.addActionListener(rbl);
		clusterButton.setVerticalAlignment(SwingConstants.TOP);
		toolBar.add(clusterButton);
		
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

		// now the menu
		commonMenus();
	}

}
