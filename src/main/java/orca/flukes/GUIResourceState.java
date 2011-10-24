package orca.flukes;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;

import orca.flukes.ndl.ResourceQueryProcessor;

import org.apache.commons.collections15.Transformer;

import com.hyperrealm.kiwi.ui.dialog.ExceptionDialog;
import com.hyperrealm.kiwi.ui.dialog.ProgressDialog;
import com.hyperrealm.kiwi.util.Task;

public class GUIResourceState extends GUICommonState {
	
	public static final String WORLD_ICON="worldmap.jpg";
	
	private static GUIResourceState instance = null;

	/**
	 * Transform coordinates into a point
	 * @author ibaldin
	 *
	 */
	static class LatLonPixelTransformer implements Transformer<OrcaNode,Point2D> {
		Dimension d;
		int startOffset;

		public LatLonPixelTransformer(Dimension d) {
			this.d = d;
		}
		
		/**
		 * transform a lat
		 */
		 public Point2D transform(OrcaNode s) {
			 if (!(s instanceof OrcaResourceSite))
				 return null;
			 OrcaResourceSite rs = (OrcaResourceSite)s;
			 double latitude = rs.getLat();
			 double longitude = rs.getLon();
			 latitude *= d.height/180f;
			 longitude *= d.width/360f;
			 latitude = d.height/2 - latitude;
			 longitude += d.width/2;

			 return new Point2D.Double(longitude,latitude);
		 }
	}
	
	@SuppressWarnings("unchecked")
	private GUIResourceState() {
		;
	}
	
	private static void initialize() {

	}
	
	public static GUIResourceState getInstance() {
		if (instance == null) {
			initialize();
			instance = new GUIResourceState();
		}
		return instance;
	}
	
	/**
	 * Resource pane button actions
	 * @author ibaldin
	 *
	 */
	public class ResourceButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("query")) {
				// run XMLRPC query

				try {
					final ProgressDialog pd = getProgressDialog("Contacting registry");
					pd.track(new Task (){

						@Override
						public void run() {
							try {
								ResourceQueryProcessor.processAMQuery(pd);
							} catch (Exception e) {
								;
							}
						}
					});
				} catch (Exception ex) {
					ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
					ed.setLocationRelativeTo(GUI.getInstance().getFrame());
					ed.setException("Exception encountered while making XMLRPC query: ", ex);
					ed.setVisible(true);
				}
			} 
		}
	}
	
	private ActionListener al = new ResourceButtonListener();
	public ActionListener getActionListener() {
		return al;
	}
	
	/**
	 * Create a progress dialog
	 * @param msg
	 * @return
	 */
	ProgressDialog getProgressDialog(String msg) {
		ProgressDialog pd = new ProgressDialog(GUI.getInstance().getFrame(), true);
		pd.setLocationRelativeTo(GUI.getInstance().getFrame());
		pd.setMessage(msg);
		pd.pack();
		
		return pd;
	}
	
	/**
	 * Create a new site or return existing ones with similar coordinates
	 * @param dom
	 * @return
	 */
	private double tolerance = 0.01;
	public synchronized OrcaResourceSite createSite(String dom, float lat, float lon) {
		for (OrcaNode node: g.getVertices()) {
			OrcaResourceSite ors = (OrcaResourceSite)node;
			if ((Math.abs(lat - ors.getLat()) < tolerance) &&
					(Math.abs(lon - ors.getLon()) < tolerance)) {
				ors.addDomain(dom);
				return ors;
			}
		}
		OrcaResourceSite newOrs = 
			new OrcaResourceSite(dom, lat, lon);
		newOrs.addDomain(dom);
		g.addVertex(newOrs);
		return newOrs;
	}
}
