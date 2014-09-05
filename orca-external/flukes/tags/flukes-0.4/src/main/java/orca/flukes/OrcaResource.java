package orca.flukes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPopupMenu;



/**
 * A generic resource with a state, a notice and color
 * @author ibaldin
 *
 */
public abstract class OrcaResource {
	private boolean isResource = false;
	protected String name;
	protected String state = null;
	protected String resNotice = null;
	protected String reservationGuid = null;
	protected Set<OrcaColor> colors = new HashSet<OrcaColor>();
	
	protected OrcaResource(String n) {
		name = n;
	}
	
	protected OrcaResource(String n, boolean res) {
		name = n;
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
    
}
