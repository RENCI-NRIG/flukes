package orca.flukes;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.EditingModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

public class GUI {

	private JFrame frmOrcaFlukes;
	private JPanel requestPanel, resourcePanel;
	private JToolBar toolBar;
	private JButton nodeButton;
	private JButton domainButton;
	private Component horizontalStrut;
	private JMenuBar menuBar;
	private JMenu fileNewMenu;
	private JMenuItem openMenuItem;
	private JMenuItem saveMenuItem;
	private JSeparator separator;
	private JMenuItem exitMenuItem;
	private JButton reservationButton;
	private Component horizontalStrut_1;
	private JMenu mnNewMenu;
	private JMenuItem helpMenuItem;
	private JMenuItem aboutMenuItem;
	private JSeparator separator_1;
	
	private EditingModalGraphMouse<OrcaNode, OrcaLink> gm;
	private JButton attributesButton;
	private Component horizontalStrut_2;

	private static GUI instance = null;
	
	// All menu actions here
	public class MenuListener implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("exit")) 
				quit();
			else if (e.getActionCommand().equals("open"))
				;
			else if (e.getActionCommand().equals("save"))
				;
			else if (e.getActionCommand().equals("help"))
				helpDialog();
			else if (e.getActionCommand().equals("about"))
				aboutDialog();
			else if (e.getActionCommand().equals("welcome"))
				;
		}
	}
	
	private void quit() {
		// TODO: add checks for saving state
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
		;
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
		SparseMultigraph<OrcaNode, OrcaLink> g = 
			new SparseMultigraph<OrcaNode, OrcaLink>();
		// Layout<V, E>, VisualizationViewer<V,E>
		//	        Map<OrcaNode,Point2D> vertexLocations = new HashMap<OrcaNode, Point2D>();
		Layout<OrcaNode, OrcaLink> layout = new StaticLayout<OrcaNode, OrcaLink>(g);

		
		//layout.setSize(new Dimension(1000,800));
		VisualizationViewer<OrcaNode,OrcaLink> vv = 
			new VisualizationViewer<OrcaNode,OrcaLink>(layout);
		// Show vertex and edge labels
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<OrcaNode>());
		vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<OrcaLink>());
		
		// Create a graph mouse and add it to the visualization viewer
		gm = new EditingModalGraphMouse<OrcaNode, OrcaLink>(vv.getRenderContext(), 
				OrcaNode.OrcaNodeFactory.getInstance(),
				OrcaLink.OrcaLinkFactory.getInstance()); 
		
		// Set some defaults for the Edges...
		OrcaLink.OrcaLinkFactory.setDefaultCapacity(192.0);
		OrcaLink.OrcaLinkFactory.setDefaultWeight(5.0);
		
		// Trying out our new popup menu mouse plugin...
		PopupVertexEdgeMenuMousePlugin<OrcaNode, OrcaLink> myPlugin = new PopupVertexEdgeMenuMousePlugin<OrcaNode, OrcaLink>();
		
		// Add some popup menus for the edges and vertices to our mouse plugin.
		JPopupMenu edgeMenu = new MyMouseMenus.EdgeMenu();
		JPopupMenu vertexMenu = new MyMouseMenus.VertexMenu();
		myPlugin.setEdgePopup(edgeMenu);
		myPlugin.setVertexPopup(vertexMenu);
		gm.remove(gm.getPopupEditingPlugin());  // Removes the existing popup editing plugin
		gm.add(myPlugin);
		
		vv.setGraphMouse(gm);

		vv.setLayout(new BorderLayout(0,0));
		
		c.add(vv);

		gm.setMode(ModalGraphMouse.Mode.EDITING); // Start off in editing mode  
	}
	
	private void aboutDialog() {
		JOptionPane.showMessageDialog(frmOrcaFlukes, 
				"FLUKES - ORCA NDL-OWL Network Editor v0.1\nVisit http://geni-orca.renci.org/trac/flukes",
				"About", JOptionPane.INFORMATION_MESSAGE);
	}
	
	private void helpDialog() {
		JOptionPane.showMessageDialog(frmOrcaFlukes, 
				"Helpful help message", "Help", JOptionPane.INFORMATION_MESSAGE);
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
		JMenu modeMenu = gm.getModeMenu();
		modeMenu.setText("Mouse Mode");
		modeMenu.setPreferredSize(new Dimension(120,20)); 
		menuBar.add(modeMenu);
		
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
		
		nodeButton = new JButton("Add Node");
		nodeButton.setToolTipText("Add new node");
		nodeButton.setVerticalAlignment(SwingConstants.TOP);
		toolBar.add(nodeButton);
		
		horizontalStrut = Box.createHorizontalStrut(10);
		toolBar.add(horizontalStrut);
		
		domainButton = new JButton("Add Domain");
		domainButton.setToolTipText("Add new doedgemmain");
		domainButton.setVerticalAlignment(SwingConstants.TOP);
		toolBar.add(domainButton);
		
		horizontalStrut_1 = Box.createHorizontalStrut(10);
		toolBar.add(horizontalStrut_1);
		
		reservationButton = new JButton("Edit Reservation");
		toolBar.add(reservationButton);
		
		horizontalStrut_2 = Box.createHorizontalStrut(10);
		toolBar.add(horizontalStrut_2);
		
		attributesButton = new JButton("Edit Attributes");
		toolBar.add(attributesButton);
				
		requestPane(requestPanel);

		// now the menu
		commonMenus();
	}

}
