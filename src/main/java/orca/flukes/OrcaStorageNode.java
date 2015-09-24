package orca.flukes;

import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import orca.flukes.MouseMenus.DomainDisplay;
import orca.flukes.MouseMenus.NodeColorItem;
import orca.flukes.MouseMenus.NodePropItem;
import orca.flukes.MouseMenus.NodeTypeDisplay;
import orca.flukes.MouseMenus.NodeViewItem;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.LayeredIcon;

/**
 * Orca storage node implementation
 * @author ibaldin
 *
 */
public class OrcaStorageNode extends OrcaNode {
	private static final String STORAGE = "Storage";
	protected long capacity = 0;
	// is this a storage on shared or dedicated network?
	protected boolean sharedNetworkStorage = true;
	protected boolean doFormat = true;
	protected String hasFSType = "ext4", hasFSParam = "-F -b 2048", hasMntPoint = "/mnt/target"; 
	
	public OrcaStorageNode(String name) {
		super(name, 
				new LayeredIcon(new ImageIcon(GUIUnifiedState.class.getResource(OrcaNodeEnum.STORAGE.getIconName())).getImage()));
		setNodeType(STORAGE);
	}
	
	public void setCapacity(long cap) {
		assert(cap >= 0);
		capacity = cap;
	}
	
	public long getCapacity() {
		return capacity;
	}
	
	/** 
	 * Create a detailed printout of properties
	 * @return
	 */
	@Override
	public String getViewerText() {
		String viewText = "";
		viewText += "Storage node: " + name;
		viewText += "\nReservation ID: " + reservationGuid;
		viewText += "\nStorage reservation state: " + (state != null ? state : NOT_SPECIFIED);
		viewText += "\nReservation notice: " + (resNotice != null ? resNotice : NOT_SPECIFIED);
		viewText += "Capacity: " + capacity;
		
		viewText += "\n\nInterfaces: ";
		for(Map.Entry<OrcaLink, Pair<String>> e: addresses.entrySet()) {
			viewText += "\n\t" + e.getKey().getName() + ": " + e.getValue().getFirst() + "/" + e.getValue().getSecond();
		}
		return viewText;
	}
	
	public void setSharedNetwork() {
		sharedNetworkStorage = true;
	}
	
	public void setDedicatedNetwork() {
		sharedNetworkStorage = false;
	}
	
	public boolean getSharedNetwork() {
		return sharedNetworkStorage;
	}
	
	public void setDoFormat(boolean m) {
		doFormat = m;
	}
	
	public boolean getDoFormat() {
		return doFormat;
	}
	
	/**
	 * Set FS prameters
	 * @param t type
	 * @param p params
	 * @param m mnt point
	 */
	public void setFS(String t, String p, String m) {
		hasFSType = t;
		hasFSParam = p;
		hasMntPoint = m;
	}
	
	public String getFSType() {
		return hasFSType;
	}
	
	public String getFSParam() {
		return hasFSParam;
	}
	
	public String getMntPoint() {
		return hasMntPoint;
	}
	
    //
    // Menus for the nodes
    //
	public static class RequestMenu extends JPopupMenu {
		public RequestMenu() {
			super("Storage Menu");
			this.add(new DeleteVertexMenuItem<OrcaNode, OrcaLink>(GUIUnifiedState.getInstance()));
			this.addSeparator();
			this.add(new DomainDisplay());
			this.add(new NodeTypeDisplay());
			this.addSeparator();
			this.add(new NodePropItem(GUI.getInstance().getFrame()));
			this.addSeparator();
			this.add(new NodeColorItem(GUI.getInstance().getFrame(), true));
		}
	}
	
	public static class ManifestMenu extends JPopupMenu {
		public ManifestMenu() {
			super("Storage Menu");
			this.add(new DeleteVertexMenuItem<OrcaNode, OrcaLink>(GUIUnifiedState.getInstance()));
			this.addSeparator();
			this.add(new DomainDisplay());
			this.add(new NodeTypeDisplay());
			this.addSeparator();
			this.add(new NodeViewItem(GUI.getInstance().getFrame()));
			this.addSeparator();
			this.add(new NodeColorItem(GUI.getInstance().getFrame(), false));
		}
	}
	
	private static JPopupMenu requestMenu, manifestMenu, resourceMenu;
	
	{
		requestMenu = new RequestMenu();
		manifestMenu = new ManifestMenu();
		resourceMenu = new ResourceMenu();
	}
	
	public JPopupMenu requestMenu() {
		return requestMenu;
	}
	
	public JPopupMenu manifestMenu() {
		return manifestMenu;
	}
	
	public JPopupMenu resourceMenu() {
		return resourceMenu;
	}
}
