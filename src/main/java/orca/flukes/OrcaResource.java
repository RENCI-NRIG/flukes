package orca.flukes;

/**
 * A generic resource with a state and a notice
 * @author ibaldin
 *
 */
public interface OrcaResource {
	public String getName();
	public void setName(String s);
	public String getState();
	public String getReservationNotice();
	public void setState(String s);
	public void setReservationNotice(String s);
}
