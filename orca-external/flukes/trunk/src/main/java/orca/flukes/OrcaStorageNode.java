package orca.flukes;

import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ImageIcon;

import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.LayeredIcon;

/**
 * Orca storage node implementation
 * @author ibaldin
 *
 */
public class OrcaStorageNode extends OrcaNode {
	private static final String STORAGE = "Storage";
	private long capacity = 0;
	// is this a storage on shared or dedicated network?
	private boolean sharedNetworkStorage = true;
	
	public OrcaStorageNode(String name) {
		super(name, 
				new LayeredIcon(new ImageIcon(GUIRequestState.class.getResource(OrcaNodeEnum.STORAGE.getIconName())).getImage()));
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
}
