package orca.flukes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.swing.JPopupMenu;



/**
 * A generic resource with a state, a notice and color
 * @author ibaldin
 *
 */
public abstract class OrcaResource implements Comparable<OrcaResource> {
	public static final String ORCA_ACTIVE = "active";
	public static final String ORCA_TICKETED = "ticketed";
	public static final String ORCA_FAILED = "failed";
	
	private boolean isResource = false;
	protected String name;
	protected String state = null;
	protected String resNotice = null;
	protected String reservationGuid = null;
	protected String requestGuid = null;
	protected Set<OrcaColor> colors = new HashSet<OrcaColor>();
	
	// distinguishing new and existing resources helps in determining
	// which menus to show
	public enum ResourceType { INVALID, RESOURCE, REQUEST, MANIFEST};
	// by default assume resource already exists (has been provisioned)
	private ResourceType myType = ResourceType.MANIFEST;
	protected String url;
	
	/**
	 * Allow to override resource type
	 * @param rt
	 */
	public void setResourceType(ResourceType rt) {
		myType = rt;
	}
	
	public ResourceType getResourceType() {
		return myType;
	}
	
	protected OrcaResource(String n) {
		name = n;
		requestGuid = UUID.randomUUID().toString();
	}
	
	protected OrcaResource(String n, boolean res) {
		this(n);
		isResource = res;
	}
	
	public boolean isResource() {
		return isResource;
	}
	public void setIsResource() {
		isResource = true;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String s) {
		name = s;
	}
	
	public String getState() {
		return state;
	}
	
	public void setState(String s) {
		state = s;
	}
	
	public String getReservationNotice() {
		return resNotice;
	}

	public void setReservationNotice(String s) {
		resNotice = s;
	}
	
	public String getReservationGuid() {
		return reservationGuid;
	}
	
	public void setReservationGuid(String g) {
		reservationGuid = g;
	}
	
	public String getRequestGuid() {
		return requestGuid;
	}
	
	public void setRequestGuid(String g) {
		requestGuid = g;
	}
	
	public List<OrcaColor> getColors() {
		return new ArrayList<OrcaColor>(colors);
	}
	
	public void addColor(OrcaColor oc) {
		assert(oc != null);
		colors.add(oc);
	}
	
	public void delColor(OrcaColor oc) {
		assert(oc != null);
		colors.remove(oc);
	}
	
	public void delColor(String label) {
		assert(label != null);
		OrcaColor oc = null;
		for(OrcaColor toc: colors) { 
			if (label.equals(toc.getLabel())) {
				oc = toc;
				break;
			}
		}
		colors.remove(oc);
	}
	
	public OrcaColor getColor(String label) {
		assert(label != null);
		OrcaColor oc = null;
		for(OrcaColor toc: colors) { 
			if (label.equals(toc.getLabel())) {
				oc = toc;
				break;
			}
		}
		return oc;
	}
	
	public abstract void setSubstrateInfo(String t, String o);
	public abstract String getSubstrateInfo(String t);
	
    @Override
    public String toString() {
        return name;
    }
    
    //
    // Dealing with menus
    //
    
    public abstract JPopupMenu requestMenu();
    public abstract JPopupMenu manifestMenu();
    public abstract JPopupMenu resourceMenu();
	
	public JPopupMenu contextMenu() {
		switch(getResourceType()) {
		case RESOURCE:
			return resourceMenu();
		case REQUEST:
			return requestMenu();
		case MANIFEST:
			return manifestMenu();
		default: return null;
		}
	}
	
    // comparable (lexicographic, based on name)
    @Override
    public int compareTo(OrcaResource o) {
    	return this.getName().compareTo(o.getName());
    }

	public void setUrl(String u) {
		url = u;
	}

	public String getUrl() {
		return url;
	}
    
	/**
	 * [<reservation id>:
	 * 		"allowed": "yes"|"no"
	 * 		[<stitch guid>:
	 * 			"performed": <RFC3399 date/time>
	 * 			"undone":    <RFC3399 date/time> - optional
	 * 			"toreservation": <guid>
	 * 			"toslice": <string>
	 * 			"stitch.dn": <string>]]
	 */
    public static final String SliceStitchAllowed = "allowed";
    public static final String SliceStitchToReservation = "toreservation";
    public static final String SliceStitchToSlice = "toslice";
    public static final String SliceStitchPerformed = "performed";
    public static final String SliceStitchUndone = "undone";
    public static final String SliceStitchDN = "stitch.dn";
	// get and stitching properties
	public String getStitchingProperties(Map<String, Object> stitch) {
		StringBuilder ret = new StringBuilder();
		
		for(Map.Entry<String, Object> e: stitch.entrySet()) {
			ret.append("Reservation: " + e.getKey() + "\n");
			Map<String, Object> resMap = (Map<String, Object>)e.getValue();
			ret.append("\tStitching Allowed: " + 
					(resMap.get(SliceStitchAllowed) == null ? "no" : resMap.get(SliceStitchAllowed)) + "\n\tStitching Properties:\n");
			for (Map.Entry<String, Object> ee: resMap.entrySet()) {
				if (SliceStitchAllowed.equals(ee.getKey()))
					continue;
				Map<String, String> stitchProps = (Map<String, String>)ee.getValue();
				for(Map.Entry<String, String> eee: stitchProps.entrySet()) {
					ret.append("\t\t" + eee.getKey() + ": " + eee.getValue() + "\n");
				}
				ret.append("\n");
			}
			ret.append("\n");
		}
		
		return ret.toString();
	}
}
