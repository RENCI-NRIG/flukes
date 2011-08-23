package orca.flukes;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.EditingModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import java.awt.FlowLayout;
import javax.swing.BoxLayout;

public class GUI {

	private JFrame frmOrcaFlukes;
	private JPanel requestPanel, resourcePanel;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI window = new GUI();
					window.frmOrcaFlukes.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GUI() {
		initialize();
	}

	/**
	 * Initialize request pane 
	 */
	protected void requestPane(Container c) {
		SparseMultigraph<GraphElements.MyVertex, GraphElements.MyEdge> g = 
			new SparseMultigraph<GraphElements.MyVertex, GraphElements.MyEdge>();
		// Layout<V, E>, VisualizationViewer<V,E>
		//	        Map<GraphElements.MyVertex,Point2D> vertexLocations = new HashMap<GraphElements.MyVertex, Point2D>();
		Layout<GraphElements.MyVertex, GraphElements.MyEdge> layout = new StaticLayout<GraphElements.MyVertex, GraphElements.MyEdge>(g);

		//layout.setSize(new Dimension(1000,800));
		VisualizationViewer<GraphElements.MyVertex,GraphElements.MyEdge> vv = 
			new VisualizationViewer<GraphElements.MyVertex,GraphElements.MyEdge>(layout);
		// Show vertex and edge labels
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<GraphElements.MyVertex>());
		vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<GraphElements.MyEdge>());
		// Create a graph mouse and add it to the visualization viewer
		EditingModalGraphMouse gm = new EditingModalGraphMouse(vv.getRenderContext(), 
				GraphElements.MyVertexFactory.getInstance(),
				GraphElements.MyEdgeFactory.getInstance()); 
		// Set some defaults for the Edges...
		GraphElements.MyEdgeFactory.setDefaultCapacity(192.0);
		GraphElements.MyEdgeFactory.setDefaultWeight(5.0);
		// Trying out our new popup menu mouse plugin...
		PopupVertexEdgeMenuMousePlugin myPlugin = new PopupVertexEdgeMenuMousePlugin();
		
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

		// Let's add a menu for changing mouse modes
//		JMenuBar menuBar = new JMenuBar();
//		JMenu modeMenu = gm.getModeMenu();
//		modeMenu.setText("Mouse Mode");
//		modeMenu.setIcon(null); // I'm using this in a main menu
//		modeMenu.setPreferredSize(new Dimension(80,20)); // Change the size so I can see the text

//		menuBar.add(modeMenu);
//		frame.setJMenuBar(menuBar);
		gm.setMode(ModalGraphMouse.Mode.EDITING); // Start off in editing mode
//		frame.pack();
//		frame.setVisible(true);    
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmOrcaFlukes = new JFrame();
		frmOrcaFlukes.setTitle("ORCA FLUKES - The ORCA Network Editor");
		//frmOrcaFlukes.getContentPane().setLayout(new BoxLayout(frmOrcaFlukes.getContentPane(), BoxLayout.X_AXIS));
		frmOrcaFlukes.getContentPane().setLayout(new BorderLayout(0,0));
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frmOrcaFlukes.getContentPane().add(tabbedPane);
		
		resourcePanel = new JPanel();
		tabbedPane.addTab("Resource View", null, resourcePanel, null);
		resourcePanel.setLayout(new BorderLayout(0, 0));
		
		requestPanel = new JPanel();
		tabbedPane.addTab("Request View", null, requestPanel, null);
		requestPanel.setLayout(new BorderLayout(0, 0));
		
		frmOrcaFlukes.setBounds(100, 100, 1000, 800);
		frmOrcaFlukes.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		requestPane(requestPanel);
	}

}
