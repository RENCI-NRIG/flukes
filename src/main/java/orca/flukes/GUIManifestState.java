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
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.swing.JTextPane;

import orca.flukes.GUI.GuiTabs;
import orca.flukes.GUI.PrefsEnum;
import orca.flukes.irods.IRodsException;
import orca.flukes.irods.IRodsICommands;
import orca.flukes.ndl.ManifestLoader;
import orca.flukes.ndl.ModifySaver;
import orca.flukes.ui.TextAreaDialog;
import orca.flukes.ui.TextHTMLPaneDialog;
import orca.flukes.xmlrpc.GENICHXMLRPCProxy;
import orca.flukes.xmlrpc.GENICHXMLRPCProxy.FedField;
import orca.flukes.xmlrpc.NDLConverter;
import orca.flukes.xmlrpc.OrcaSMXMLRPCProxy;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

import com.hyperrealm.kiwi.ui.KTextArea;
import com.hyperrealm.kiwi.ui.dialog.ExceptionDialog;
import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;
import com.hyperrealm.kiwi.ui.dialog.KQuestionDialog;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.EditingModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.picking.PickedState;

public class GUIManifestState extends GUICommonState implements IDeleteEdgeCallBack<OrcaLink>, IDeleteNodeCallBack<OrcaNode>{
	private static GUIManifestState instance = new GUIManifestState();
	protected String manifestString;
	private Date start = null, end = null, newEnd = null;

	public static GUIManifestState getInstance() {
		return instance;
	}

	public void setManifestString(String s) {
		manifestString = s;
	}
	
	public String getManifestString() {
		return manifestString;
	}
	
	public void setManifestTerm(Date s, Date e) {
		start = s;
		end = e;
	}
	
	public void setNewEndDate(Date s) {

		if ((start == null) || (end == null))
 			return;
		
		Long diff = s.getTime() - start.getTime();
		if (diff < 0)
			return;

		diff = s.getTime() - end.getTime();
		if (diff < 0)
			return;
		
		newEnd = s;	
	}
	
	public void resetEndDate() {
		end = newEnd;
		newEnd = null;
	}
	
	/**
	 * clear the manifest
	 */
	@Override
	public void clear() {
		super.clear();
		
		// clear the graph, 
		if (g == null)
			return;
		Set<OrcaNode> nodes = new HashSet<OrcaNode>(g.getVertices());
		for (OrcaNode n: nodes)
			g.removeVertex(n);
	}

	void deleteSlice(String name) {
		if ((name == null) || 
				(name.length() == 0)) {
			KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
			kmd.setMessage("You must specify a slice id");
			kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
			kmd.setVisible(true);
			return;
		}
		
		try {
			OrcaSMXMLRPCProxy.getInstance().deleteSlice(name);
		} catch (Exception ex) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while deleting slice manifest: ", ex);
			ed.setVisible(true);
		}
	}
	
	void modifySlice(String name, String req) {
		if ((name == null) || 
				(name.length() == 0)) {
			KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
			kmd.setMessage("You must specify a slice id");
			kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
			kmd.setVisible(true);
			return;
		}
		try {
			String s = OrcaSMXMLRPCProxy.getInstance().modifySlice(name, req);
			TextAreaDialog tad = new TextAreaDialog(GUI.getInstance().getFrame(), "Modify Output", 
					"Modify Output", 
					30, 50);
			KTextArea ta = tad.getTextArea();

			if (s != null)
				ta.setText(s);
			tad.pack();
			tad.setVisible(true);
		} catch (Exception ex) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while modifying slice: ", ex);
			ed.setVisible(true);
		}
	}
	
	private String stripManifest(String m) {
		if (m == null)
			return null;
		int ind = m.indexOf("<rdf:RDF");
		if (ind > 0)
			return m.substring(ind);
		else
			return null;
	}
	
	void queryManifest() {
		// run request manifest from controller
		if ((sliceIdField.getText() == null) || 
				(sliceIdField.getText().length() == 0)) {
			KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
			kmd.setMessage("You must specify a slice id");
			kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
			kmd.setVisible(true);
			return;
		}

		try {
			GUIManifestState.getInstance().clear();

			manifestString = OrcaSMXMLRPCProxy.getInstance().sliceStatus(sliceIdField.getText());

			ManifestLoader ml = new ManifestLoader();

			String realM = stripManifest(manifestString);
			if (realM != null) {
				if (ml.loadString(realM))
					GUI.getInstance().kickLayout(GuiTabs.MANIFEST_VIEW);
			} else {
				KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
				kmd.setMessage("Error has occurred, check raw controller response for details.");
				kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
				kmd.setVisible(true);
				return;
			}
		} catch (Exception ex) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while querying ORCA for slice manifest: ", ex);
			ed.setVisible(true);
		}
	}
	
	public class ResourceButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			assert(sliceIdField != null);

			if (e.getActionCommand().equals(GUI.Buttons.manifest.getCommand())) {
				// run request manifest from controller
				queryManifest();
			} else 
				if (e.getActionCommand().equals(GUI.Buttons.raw.getCommand())) {
					TextAreaDialog tad = new TextAreaDialog(GUI.getInstance().getFrame(), "Raw manifest", 
							"Raw manifest", 
							30, 50);
					KTextArea ta = tad.getTextArea();

					if (manifestString != null)
						ta.setText(manifestString);
					tad.pack();
					tad.setVisible(true);
				} else 
					if (e.getActionCommand().equals(GUI.Buttons.delete.getCommand())) {
						if ((sliceIdField.getText() == null) || 
								(sliceIdField.getText().length() == 0)) {
							KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
							kmd.setMessage("You must specify a slice id");
							kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
							kmd.setVisible(true);
							return;
						}

						KQuestionDialog kqd = new KQuestionDialog(GUI.getInstance().getFrame(), "Exit", true);
						kqd.setMessage("Are you sure you want to delete slice " + sliceIdField.getText());
						kqd.setLocationRelativeTo(GUI.getInstance().getFrame());
						kqd.setVisible(true);
						if (!kqd.getStatus()) 
							return;
						deleteSlice(sliceIdField.getText());

					} else 
						if (e.getActionCommand().equals(GUI.Buttons.listSlices.getCommand())) {
							try {
								String[] slices = OrcaSMXMLRPCProxy.getInstance().listMySlices();
								OrcaSliceList osl = new OrcaSliceList(GUI.getInstance().getFrame(), slices);
								osl.pack();
								osl.setVisible(true);
							} catch (Exception ex) {
								ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
								ed.setLocationRelativeTo(GUI.getInstance().getFrame());
								ed.setException("Exception encountered while listing user slices: ", ex);
								ed.setVisible(true);
							}
						} else 
							if (e.getActionCommand().equals(GUI.Buttons.modify.getCommand())) {
								try {
									if ((sliceIdField.getText() == null) || 
											(sliceIdField.getText().length() == 0)) {
										KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
										kmd.setMessage("You must specify a slice id");
										kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
										kmd.setVisible(true);
										return;
									}
									ModifyTextSetter mts = new ModifyTextSetter(sliceIdField.getText());
									TextAreaDialog tad = new TextAreaDialog(GUI.getInstance().getFrame(), mts, 
											"Modify Request", 
											"Cut and paste the modify request into the window", 30, 50);
									String txt = ModifySaver.getInstance().getModifyRequest();
									if (txt != null)
										tad.getTextArea().setText(txt);
									tad.pack();
									tad.setVisible(true);
								} catch(Exception ex) {
									ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
									ed.setLocationRelativeTo(GUI.getInstance().getFrame());
									ed.setException("Exception encountered while modifying slice: ", ex);
									ed.setVisible(true);
								} 
							} else
								if (e.getActionCommand().equals(GUI.Buttons.clearModify.getCommand())) {
									ModifySaver.getInstance().clear();
								} else
									if (e.getActionCommand().equals(GUI.Buttons.extend.getCommand())) {
										ReservationExtensionDialog red = new ReservationExtensionDialog(GUI.getInstance().getFrame());
										red.setFields(new Date());
										red.pack();
										red.setVisible(true);
										
										String sliceUrn = sliceIdField.getText();
										
										if (newEnd != null) {
											boolean saException = false;
											// check slice expiration with SA and extend if necessary
											if (GUI.getInstance().getPreference(GUI.PrefsEnum.ENABLE_GENISA).equalsIgnoreCase("true") ||
													GUI.getInstance().getPreference(GUI.PrefsEnum.ENABLE_GENISA).equalsIgnoreCase("yes")) {
												try {
													Map<String, Object> r = GENICHXMLRPCProxy.getInstance().saLookupSlice(sliceUrn, 
															new FedField[] { GENICHXMLRPCProxy.FedField.SLICE_EXPIRATION});
													String dateString = (String)((Map<String, Object>)r.get(sliceUrn)).get(GENICHXMLRPCProxy.FedField.SLICE_EXPIRATION.name());
													DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss", Locale.ENGLISH);
													df.setTimeZone(TimeZone.getTimeZone("UTC"));
													Date result =  df.parse(dateString);
													if (result.before(newEnd)) {
														// update slice expiration on SA
														df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
														df.setTimeZone(TimeZone.getTimeZone("UTC"));
														String nowAsISO = df.format(newEnd);
														
														GENICHXMLRPCProxy.getInstance().saUpdateSlice(sliceUrn, FedField.SLICE_EXPIRATION, nowAsISO);
													}
												} catch (Exception sae) {
													ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
													ed.setLocationRelativeTo(GUI.getInstance().getFrame());
													ed.setException("Unable to extend slice on the SA: ", sae);
													ed.setVisible(true);
													saException = true;
												}
											}

											if (!saException) {
												try {
													Boolean res = OrcaSMXMLRPCProxy.getInstance().renewSlice(sliceUrn, newEnd);
													KMessageDialog kd = new KMessageDialog(GUI.getInstance().getFrame(), "Result", true);
													kd.setMessage("The extend operation returned: " + res);
													kd.setLocationRelativeTo(GUI.getInstance().getFrame());
													kd.setVisible(true);
													if (res)
														resetEndDate();
													else
														newEnd = null;
												} catch (Exception ee) {
													ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
													ed.setLocationRelativeTo(GUI.getInstance().getFrame());
													ed.setException("Exception encountered while extending slice: ", ee);
													ed.setVisible(true);
												}
											}
										} else {
											KMessageDialog kmd = new KMessageDialog(GUI.getInstance().getFrame());
											kmd.setMessage("Invalid new end date.");
											kmd.setLocationRelativeTo(GUI.getInstance().getFrame());
											kmd.setVisible(true);
											return;
										}
									} else
										if (e.getActionCommand().equals(GUI.Buttons.twitter.getCommand())) {
											TextHTMLPaneDialog tad = new TextHTMLPaneDialog(GUI.getInstance().getFrame(), "Recent Twitter Status Updates", "", "https://groups.google.com/forum/#!forum/geni-orca-users");
											JTextPane ta = tad.getTextPane();

											StringBuilder sb = new StringBuilder();
											sb.append("<html>");
											try {
												Twitter twitter = TwitterFactory.getSingleton();
												Paging p = new Paging(1,10);
												List<Status> statuses = twitter.getUserTimeline("exogeni_ops", p);
												for(int l=statuses.size() - 1; l >= 0; l--) {
													sb.append("<p>" + statuses.get(l).getCreatedAt() + ":<font color=\"red\">   " + statuses.get(l).getText() + "</font></p>");
													sb.append("<hr/>");
												}
											} catch (TwitterException te) {
												sb.append("Unable to retrieve Twitter status: " + te.getMessage());
											}
											sb.append("</html>");
											ta.setText(sb.toString());
											tad.pack();
											tad.setVisible(true);
										}
		}
	}
	
	public void launchResourceStateViewer(Date start, Date end) {
		// get a list of nodes and links
		List<OrcaResource> resources = new ArrayList<OrcaResource>();
		
		resources.addAll(g.getVertices());
		resources.addAll(g.getEdges());
		
		OrcaResourceStateViewer viewer = new OrcaResourceStateViewer(GUI.getInstance().getFrame(), resources, start, end);
		viewer.pack();
		viewer.setVisible(true);
	}

	public void printResourceState(Date start, Date end) {
		// get a list of nodes and links
		List<OrcaResource> resources = new ArrayList<OrcaResource>();
		
		resources.addAll(g.getVertices());
		resources.addAll(g.getEdges());

		System.out.println("Printing resource state:");
		for (OrcaResource r : resources) {
			String resourceInfo =
				"Name: " + r.getName() + ", " +
				"IsResource: " + Boolean.toString(r.isResource()) + ", " +
				"State: " + r.getState() + "\n";

			resourceInfo += "ResNotice: ";
			String resNotice = r.getReservationNotice();
			if (resNotice != null)
				resourceInfo += resNotice; // Contains "\n" on end.
			else
				resourceInfo += "None\n";

			resourceInfo +=	"Colors: ";
			List<OrcaColor> colors = r.getColors();
			if (colors.size() > 0) {
				resourceInfo += "|";
				for (OrcaColor c : colors) {
					resourceInfo += c.getLabel() + "|";
				}
			}
			else {
				resourceInfo += "None";
			}
			System.out.println(resourceInfo);
                }
	}
	
	ResourceButtonListener rbl = new ResourceButtonListener();
	@Override
	public ActionListener getActionListener() {
		return rbl;
	}

	@Override
	public void addPane(Container c) {
		
		Layout<OrcaNode, OrcaLink> layout = new FRLayout<OrcaNode, OrcaLink>(g);
		
		//layout.setSize(new Dimension(1000,800));
		vv = new VisualizationViewer<OrcaNode,OrcaLink>(layout);
		// Show vertex and edge labels
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<OrcaNode>());
		vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<OrcaLink>());
		vv.getRenderContext().setEdgeDrawPaintTransformer(new GUICommonState.LinkPaint());
		
		// Create a graph mouse and add it to the visualization viewer
		OrcaNode.OrcaNodeFactory onf = new OrcaNode.OrcaNodeFactory(nodeCreator);
		OrcaLink.OrcaLinkFactory olf = new OrcaLink.OrcaLinkFactory(linkCreator);
		gm = new EditingModalGraphMouse<OrcaNode, OrcaLink>(vv.getRenderContext(), 
				onf, olf);
		
		// add the plugin
		//PopupVertexEdgeMenuMousePlugin<OrcaNode, OrcaLink> myPlugin = new PopupVertexEdgeMenuMousePlugin<OrcaNode, OrcaLink>();
		PopupMultiVertexEdgeMenuMousePlugin myPlugin = new PopupMultiVertexEdgeMenuMousePlugin();
		
		// Add some popup menus for the edges and vertices to our mouse plugin.
		// mode menu is not set for manifests
		//myPlugin.setEdgePopup(new MouseMenus.ManifestEdgeMenu());
		//myPlugin.setVertexPopup(new MouseMenus.ManifestNodeMenu());
		
		gm.remove(gm.getPopupEditingPlugin());  // Removes the existing popup editing plugin
		gm.add(myPlugin);

		// Add icon and shape (so pickable area roughly matches the icon) transformer
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

		gm.setMode(ModalGraphMouse.Mode.TRANSFORMING); // Start off in panning mode  
	}

	@Override
	public void deleteEdgeCallBack(OrcaLink e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void deleteNodeCallBack(OrcaNode n) {
		ModifySaver.getInstance().removeNodeFromGroup(n.getGroup(), n.getUrl());
	}

	public void saveManifestToIRods() {
		IRodsICommands irods = new IRodsICommands();
		if (manifestString == null) {
			KMessageDialog md = new KMessageDialog(GUI.getInstance().getFrame(), "Manifest Error", true);
			md.setMessage("Manifest is empty!");
			md.setLocationRelativeTo(GUI.getInstance().getFrame());
			md.setVisible(true);
			return;
		}
		try {
			String realM = stripManifest(manifestString);
			// convert if needed
			String iRodsName = IRodsICommands.substituteManifestName();
			if (GUI.getInstance().getPreference(PrefsEnum.IRODS_FORMAT).equalsIgnoreCase("rspec")) {
				String rspec = NDLConverter.callConverter(NDLConverter.MANIFEST_TO_RSPEC, new Object[]{realM, sliceIdField.getText()});
				irods.saveFile(iRodsName, rspec);
			} else if (GUI.getInstance().getPreference(PrefsEnum.IRODS_FORMAT).equalsIgnoreCase("ndl"))
				irods.saveFile(iRodsName, realM);
			else {
				ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
				ed.setLocationRelativeTo(GUI.getInstance().getFrame());
				ed.setException("Exception encountered while saving manifest to iRods: ", 
						new Exception("unknown format " + GUI.getInstance().getPreference(PrefsEnum.IRODS_FORMAT)));
				ed.setVisible(true);
			}
			KMessageDialog md = new KMessageDialog(GUI.getInstance().getFrame(), "Saving to iRods", true);
			md.setMessage("Manifest saved as " + iRodsName);
			md.setLocationRelativeTo(GUI.getInstance().getFrame());
			md.setVisible(true);
		} catch (IRodsException ie) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while saving manifest to iRods: ", ie);
			ed.setVisible(true);
		} catch (Exception e) {
			ExceptionDialog ed = new ExceptionDialog(GUI.getInstance().getFrame(), "Exception");
			ed.setLocationRelativeTo(GUI.getInstance().getFrame());
			ed.setException("Exception encountered while saving manifest to iRods: ", e);
			ed.setVisible(true);
		}
	}
	public static void main(String[] argv) {
		try {
		String dateString = "2015-04-07T19:33:10Z";
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
		System.out.println("*** DATE STRING FORM SA " + dateString);
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date result =  df.parse(dateString);
		System.out.println(result);
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
	}
}
