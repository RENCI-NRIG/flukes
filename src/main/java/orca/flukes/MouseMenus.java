/*
 * MouseMenus.java
 *
 * Created on March 21, 2007, 3:34 PM; Updated May 29, 2007
 *
 * Copyright March 21, 2007 Grotto Networking
 * 
 * Modified for Flukes. Additional code copyright 2011 RENCI/UNC Chapel Hill by Ilia Baldine
 *
 */

package orca.flukes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import orca.flukes.GUI.PrefsEnum;
import orca.ndl.ScaledFormatPrinter;

import com.hyperrealm.kiwi.ui.dialog.ExceptionDialog;
import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;

/**
 * A collection of classes used to assemble popup mouse menus for the custom
 * edges and vertices developed in this example.
 * @author Dr. Greg M. Bernstein
 */
public class MouseMenus {
    
    private static final String PREFIX_LATENCY = "Latency: ";
    private static final String PREFIX_LABEL = "Label: ";
    private static final String PREFIX_BANDWIDTH = "Bandwidth: ";
	private static final String UNSPECIFIED = "unspecified";
	private static final String PREFIX_IMAGE = "Image: ";
	private static final String PREFIX_DOMAIN = "Domain: ";
	private static final String PREFIX_NODE_TYPE = "Node Type: ";
	private static final String NOT_APPLICABLE = "N/A";
    
	public static class ModeMenu extends JPopupMenu implements ActionListener {
		final ButtonGroup bg;
		
		public ModeMenu() {
			super("Mode Menu");
			bg = new ButtonGroup();
//			JMenu mm = GUI.getInstance().getMouse().getModeMenu();
//			for(int itemCnt = 0; itemCnt < mm.getItemCount(); itemCnt ++) {
//				if (mm.getItem(itemCnt) != null)
//					this.add(mm.getItem(itemCnt));
//			}
			JRadioButtonMenuItem mi = new JRadioButtonMenuItem("Edit");
			mi.setActionCommand("edit");
			mi.addActionListener(this);
			mi.setSelected(true);
			this.add(mi);
			bg.add(mi);
			mi = new JRadioButtonMenuItem("Pick");
			mi.setActionCommand("pick");
			mi.addActionListener(this);
			this.add(mi);
			bg.add(mi);
			mi = new JRadioButtonMenuItem("Pan");
			mi.setActionCommand("pan");
			mi.addActionListener(this);
			this.add(mi);
			bg.add(mi);
			this.addSeparator();
			// add button to create multi-node property window
			CommonPropItem rmi = new CommonPropItem(GUI.getInstance().getFrame());
			this.add(rmi);
		}
		
		public void actionPerformed(ActionEvent e) {
			AbstractModalGraphMouse m = null;
			
			// which mouse?
			switch(GUI.getInstance().activeTab()) {
			case RESOURCE_VIEW:
				break;
			case REQUEST_VIEW:
				m = GUIRequestState.getInstance().gm;
				break;
			case MANIFEST_VIEW:
				m = GUIManifestState.getInstance().gm;
				break;
			}
			if (m == null)
				return;
			
			if (e.getActionCommand().equals("edit")) {
				m.setMode(ModalGraphMouse.Mode.EDITING);
			}
			else if (e.getActionCommand().equals("pick")) {
				m.setMode(ModalGraphMouse.Mode.PICKING);
			}
			else if (e.getActionCommand().equals("pan")) {
				m.setMode(ModalGraphMouse.Mode.TRANSFORMING);
			}
		}
	}
	
	public static class CommonPropItem extends JMenuItem implements SelectListener<OrcaNode> {
		Set<OrcaNode> nodes;
		
		public CommonPropItem(final JFrame parent) {
			super("Edit Selected Nodes...");
			this.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (nodes.size() > 0) {
						OrcaMultiNodePropertyDialog mnp = new OrcaMultiNodePropertyDialog(GUI.getInstance().getFrame(), nodes);
						mnp.pack();
						mnp.setVisible(true);
					} else {
						KMessageDialog kqd = new KMessageDialog(GUI.getInstance().getFrame(), "Empty selection", true);
						kqd.setMessage("You have not selected any nodes!");
						kqd.setLocationRelativeTo(GUI.getInstance().getFrame());
						kqd.setVisible(true);
					}
				}
			});
		}
		
		public void setSelectedNodes(Set<OrcaNode> nodes) {
			this.nodes = nodes;			
		}
		
	}
	
    public static class RequestEdgeMenu extends JPopupMenu {        
        // private JFrame frame; 
        public RequestEdgeMenu() {
            super("Edge Menu");
            // this.frame = frame;
            this.add(new DeleteEdgeMenuItem<OrcaNode, OrcaLink>(GUIRequestState.getInstance()));
            this.addSeparator();
            //this.add(new LatencyDisplay());
            this.add(new BandwidthDisplay());
            this.add(new LabelDisplay());
            this.addSeparator();
            this.add(new EdgePropItem(GUI.getInstance().getFrame()));           
        }
        
    }
    
    public static class ManifestEdgeMenu extends JPopupMenu {        
        // private JFrame frame; 
        public ManifestEdgeMenu() {
            super("Edge Menu");
            //this.add(new LatencyDisplay());
            this.add(new BandwidthDisplay());
            this.add(new LabelDisplay());
            this.addSeparator();
            this.add(new EdgeViewerItem(GUI.getInstance().getFrame()));           
        }
    }
    
    public static class EdgePropItem extends JMenuItem implements EdgeMenuListener<OrcaNode, OrcaLink>,
            MenuPointListener {
        OrcaLink edge;
        VisualizationViewer<OrcaNode, OrcaLink> visComp;
        Point2D point;
        
        public void setEdgeAndView(OrcaLink edge, VisualizationViewer<OrcaNode, OrcaLink> visComp) {
            this.edge = edge;
            this.visComp = visComp;
        }

        public void setPoint(Point2D point) {
            this.point = point;
        }
        
        public  EdgePropItem(final JFrame frame) {            
            super("Edit Properties...");
            this.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                	if (!edge.linkToBroadcast()) {
                		OrcaLinkPropertyDialog dialog = new OrcaLinkPropertyDialog(frame, edge);
                		dialog.pack();
                		dialog.setVisible(true);
                	}
                }
            });
        }
    }
    
    public static class EdgeViewerItem extends JMenuItem implements EdgeMenuListener<OrcaNode, OrcaLink>,
    MenuPointListener {
    	OrcaLink edge;
    	VisualizationViewer<OrcaNode, OrcaLink> visComp;
    	Point2D point;

    	public void setEdgeAndView(OrcaLink edge, VisualizationViewer<OrcaNode, OrcaLink> visComp) {
    		this.edge = edge;
    		this.visComp = visComp;
    	}

    	public void setPoint(Point2D point) {
    		this.point = point;
    	}

    	public  EdgeViewerItem(final JFrame frame) {            
    		super("View Properties...");
    		this.addActionListener(new ActionListener() {
    			public void actionPerformed(ActionEvent e) {
    				OrcaLinkPropertyViewer dialog = new OrcaLinkPropertyViewer(frame, edge);
    				dialog.pack();
    				dialog.setVisible(true);
    			}
    		});
    	}

    }
    
    public static class LatencyDisplay extends JMenuItem implements EdgeMenuListener<OrcaNode, OrcaLink> {
		public void setEdgeAndView(OrcaLink e, VisualizationViewer<OrcaNode, OrcaLink> visComp) {
			if (e.linkToBroadcast())
				this.setText(PREFIX_LATENCY + NOT_APPLICABLE);
			else
	        	if (e.getLatency() == 0)
	        		this.setText(PREFIX_LATENCY + UNSPECIFIED);
	        	else
	        		this.setText(PREFIX_LATENCY + new ScaledFormatPrinter(e.getLatency()/1e6,"us"));
        }
    }
    
    public static class BandwidthDisplay extends JMenuItem implements EdgeMenuListener<OrcaNode, OrcaLink> {
		public void setEdgeAndView(OrcaLink e, VisualizationViewer<OrcaNode, OrcaLink> visComp) {
			if (e.linkToBroadcast())
				this.setText(PREFIX_BANDWIDTH + NOT_APPLICABLE);
			else
	        	if (e.getBandwidth() == 0)
	        		this.setText(PREFIX_BANDWIDTH + UNSPECIFIED);
	        	else
	        		this.setText(PREFIX_BANDWIDTH + new ScaledFormatPrinter(e.getBandwidth(), "bps"));
        }
    }
    
    public static class LabelDisplay extends JMenuItem implements EdgeMenuListener<OrcaNode, OrcaLink> {

		public void setEdgeAndView(OrcaLink e, VisualizationViewer<OrcaNode, OrcaLink> visComp) {
			if (e.linkToBroadcast())
				this.setText(PREFIX_LABEL + NOT_APPLICABLE);
			else
	        	if (e.getLabel() == null)
	        		this.setText(PREFIX_LABEL + UNSPECIFIED);
	        	else
	        		this.setText(PREFIX_LABEL + e.getLabel());
        }
    }
    
    public static class RequestNodeMenu extends JPopupMenu {
        public RequestNodeMenu() {
            super("Node Menu");
            this.add(new DeleteVertexMenuItem<OrcaNode, OrcaLink>(GUIRequestState.getInstance()));
            this.addSeparator();
            this.add(new ImageDisplay());
            this.add(new DomainDisplay());
            this.add(new NodeTypeDisplay());
            this.addSeparator();
            this.add(new NodePropItem(GUI.getInstance().getFrame()));
        }
    }
    
    public static class ManifestNodeMenu extends JPopupMenu {
        public ManifestNodeMenu() {
            super("Node Menu");
            this.add(new ImageDisplay());
            this.add(new DomainDisplay());
            this.add(new NodeTypeDisplay());
            this.addSeparator();
            if ((GUI.getInstance().getPreference(PrefsEnum.ENABLE_MODIFY).equalsIgnoreCase("true")) ||
            		(GUI.getInstance().getPreference(PrefsEnum.ENABLE_MODIFY).equalsIgnoreCase("yes"))) {
            	this.add(new DeleteVertexMenuItem<OrcaNode, OrcaLink>(GUIManifestState.getInstance()));
            	this.add(new IncreaseByNodeGroupItem(GUI.getInstance().getFrame()));
            	this.addSeparator();
            }
            this.add(new NodeViewItem(GUI.getInstance().getFrame()));
            this.add(new NodeLoginItem(GUI.getInstance().getFrame()));
        }
    }
    
    public static class ResourceNodeMenu extends JPopupMenu {
        public ResourceNodeMenu() {
            super("Site Menu");
            this.add(new MultiDomainDisplay());
        }
    }
    
    public static class ImageDisplay extends JMenuItem implements NodeMenuListener<OrcaNode, OrcaLink> {
    	public void setNodeAndView(OrcaNode v,
				VisualizationViewer<OrcaNode, OrcaLink> visView) {
    		if (v instanceof OrcaCrossconnect) {
    			OrcaCrossconnect oc = (OrcaCrossconnect)v;
    			this.setText(PREFIX_IMAGE + NOT_APPLICABLE);
    		} else {
    			if ((v.getImage() != null) && (v.getImage().length() > 0))
    				this.setText(PREFIX_IMAGE + v.getImage());
    			else
    				this.setText(PREFIX_IMAGE + GUIRequestState.NO_GLOBAL_IMAGE);
    		}
    	}
    }
    
    public static class DomainDisplay extends JMenuItem implements NodeMenuListener<OrcaNode, OrcaLink> {
    	public void setNodeAndView(OrcaNode v,
				VisualizationViewer<OrcaNode, OrcaLink> visView) {
    		if ((v.getDomain() != null) && (v.getDomain().length() > 0))
    			this.setText(PREFIX_DOMAIN + v.getDomain());
    		else
    			this.setText(PREFIX_DOMAIN + GUIRequestState.NO_DOMAIN_SELECT);
    	}
    }
    
    public static class MultiDomainDisplay extends JMenuItem implements NodeMenuListener<OrcaNode, OrcaLink> {
    	public void setNodeAndView(OrcaNode v,
				VisualizationViewer<OrcaNode, OrcaLink> visView) {
			if (v instanceof OrcaResourceSite) {
				OrcaResourceSite ors = (OrcaResourceSite)v;
				String domains = "";
				for (String dom: ors.getDomains()) {
					domains += dom + ", ";
				}
				this.setText(PREFIX_DOMAIN + domains);
			} else
    		if ((v.getDomain() != null) && (v.getDomain().length() > 0))
    			this.setText(PREFIX_DOMAIN + v.getDomain());
    		else
    			this.setText(PREFIX_DOMAIN + GUIRequestState.NO_DOMAIN_SELECT);
    	}
    }
    
    public static class NodeTypeDisplay extends JMenuItem implements NodeMenuListener<OrcaNode, OrcaLink> {
    	public void setNodeAndView(OrcaNode v,
    			VisualizationViewer<OrcaNode, OrcaLink> visView) {
    		
    		if (v instanceof OrcaCrossconnect) {
    			OrcaCrossconnect oc = (OrcaCrossconnect)v;
    			this.setText(PREFIX_NODE_TYPE + "vlan, tag " + (oc.getLabel() == null ? UNSPECIFIED : oc.getLabel()));
    		} else {
    			if ((v.getNodeType() != null) && (v.getNodeType().length() > 0))
    				this.setText(PREFIX_NODE_TYPE + v.getNodeType());
    			else
    				this.setText(PREFIX_NODE_TYPE + GUIRequestState.NODE_TYPE_SITE_DEFAULT);
    		}
    	}
    }
    
    public static class NodePropItem extends JMenuItem implements NodeMenuListener<OrcaNode, OrcaLink>, MenuPointListener {
        OrcaNode node;
        VisualizationViewer<OrcaNode, OrcaLink> visComp;
        Point2D point;
        
        public  NodePropItem(final JFrame frame) {            
            super("Edit Properties...");
            this.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                	if (node instanceof OrcaCrossconnect) {
                		OrcaBroadcastLinkPropertyDialog dialog = new OrcaBroadcastLinkPropertyDialog(frame, (OrcaCrossconnect)node);
                		dialog.pack();
                		dialog.setVisible(true);
                	} else if (node instanceof OrcaStitchPort) {
                		OrcaStitchPortPropertyDialog dialog = new OrcaStitchPortPropertyDialog(frame, (OrcaStitchPort)node);
                		dialog.pack();
                		dialog.setVisible(true);
                	} else if (node instanceof OrcaStorageNode) {
                		OrcaStoragePropertyDialog dialog = new OrcaStoragePropertyDialog(frame, (OrcaStorageNode) node);
                		dialog.pack();
                		dialog.setVisible(true);
                	} else {
                		OrcaNodePropertyDialog dialog = new OrcaNodePropertyDialog(frame, node);
                		dialog.pack();
                		dialog.setVisible(true);
                	}
                }
            });
        }
        
		public void setNodeAndView(OrcaNode v,
				VisualizationViewer<OrcaNode, OrcaLink> visView) {
			visComp = visView;
			node = v;
		}

		public void setPoint(Point2D point) {
			this.point = point;
		}	
    }
    
    public static class NodeViewItem extends JMenuItem implements NodeMenuListener<OrcaNode, OrcaLink>, MenuPointListener {
        OrcaNode node;
        VisualizationViewer<OrcaNode, OrcaLink> visComp;
        Point2D point;
        
        public  NodeViewItem(final JFrame frame) {
            super("View Properties...");
            this.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    OrcaNodePropertyViewer dialog = new OrcaNodePropertyViewer(frame, node);
                    dialog.pack();
                    dialog.setVisible(true);
                }
            });
        }
        
		public void setNodeAndView(OrcaNode v,
				VisualizationViewer<OrcaNode, OrcaLink> visView) {
			visComp = visView;
			node = v;
		}

		public void setPoint(Point2D point) {
			this.point = point;
		}	
    }
    
    public static class IncreaseByNodeGroupItem extends JMenuItem implements NodeMenuListener<OrcaNode, OrcaLink>, MenuPointListener {
        OrcaNode node;
        VisualizationViewer<OrcaNode, OrcaLink> visComp;
        Point2D point;
        
        public  IncreaseByNodeGroupItem(final JFrame frame) {
            super("Increase node group size...");
            this.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    OrcaIncreaseGroupSizeDialog dialog = new OrcaIncreaseGroupSizeDialog(frame, node);
                    dialog.pack();
                    dialog.setVisible(true);
                }
            });
        }
        
		@Override
		public void setNodeAndView(OrcaNode v,
				VisualizationViewer<OrcaNode, OrcaLink> visView) {
			visComp = visView;
			node = v;
		}

		@Override
		public void setPoint(Point2D point) {
			this.point = point;
		}
    	
    }
    
    public static class NodeLoginItem extends JMenuItem implements NodeMenuListener<OrcaNode, OrcaLink>, MenuPointListener {
        OrcaNode node;
        VisualizationViewer<OrcaNode, OrcaLink> visComp;
        Point2D point;
        
        public  NodeLoginItem(final JFrame frame) {
            super("Login to Node ...");
            this.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                	try {
                		String mgt = node.getSSHManagementAccess();
                		if (mgt == null) {
                			KMessageDialog kqd = new KMessageDialog(GUI.getInstance().getFrame(), "Node login", true);
                                        kqd.setMessage("Node " + node.getName() + " does not allow user logins.");
                                        kqd.setLocationRelativeTo(GUI.getInstance().getFrame());
                                        kqd.setVisible(true);
                			return;
                		}

                                // For now, we only support SSH as an access method;
                                // when we add new access methods (in the glorious future),
                                // this should become some sort of switch...
                		if (!mgt.startsWith("ssh")) {
                                    KMessageDialog kqd = new KMessageDialog(GUI.getInstance().getFrame(), "Node login", true);
                                    kqd.setMessage("Node " + node.getName() + " uses unsupported access method: " + mgt);
                                    kqd.setLocationRelativeTo(GUI.getInstance().getFrame());
                                    kqd.setVisible(true);
                                    return;
                		}

                                boolean isWindows = (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1);
                                String xtermCmd = GUI.getInstance().getPreference(GUI.PrefsEnum.XTERM_PATH);
                                File xtermFile = new File(xtermCmd);
                                if (!xtermFile.canExecute()) {
                                    if (isWindows) {
                                        String puttyCmd = GUI.getInstance().getPreference(GUI.PrefsEnum.PUTTY_PATH);
                                        File puttyFile = new File(puttyCmd);
                                        if (!puttyFile.canExecute()) {
                                            KMessageDialog kqd = new KMessageDialog(GUI.getInstance().getFrame(),
                                                                                    "Node login", true);
                                            kqd.setMessage("Path to PuTTY " + puttyCmd +
                                                           " is not valid. Please fix $HOME/.flukes.properties!");
                                            kqd.setLocationRelativeTo(GUI.getInstance().getFrame());
                                            kqd.setVisible(true);
                                            return;
                                        } else {
                                            // Parse access method
                                            mgt = mgt.replaceAll("ssh://", "");
                                            // run putty
                                            String[] mgtHostInfo = mgt.split(":");

                                            String mgtPrivKey = GUI.getInstance().getPreference(GUI.PrefsEnum.SSH_KEY);
                                            File mgtPrivKeyPath;
                                            if (mgtPrivKey.startsWith("~/")) {
                                                mgtPrivKey = mgtPrivKey.replaceAll("~/", "/");
                                                mgtPrivKeyPath = new File(System.getProperty("user.home"), mgtPrivKey);
                                            }
                                            else {
                                                mgtPrivKeyPath = new File(mgtPrivKey);
                                            }

                                            Runtime rt = Runtime.getRuntime();
                                            String[] cmdArr = new String[] {puttyCmd, "-ssh", mgtHostInfo[0], "-P", mgtHostInfo[1],
                                                                            "-i", mgtPrivKeyPath.getCanonicalPath()};
                                            System.out.println(cmdArr);
                                            rt.exec(cmdArr);
                                        }
                                    }
                                    else {
                                        KMessageDialog kqd = new KMessageDialog(GUI.getInstance().getFrame(), "Node login", true);
                                        kqd.setMessage("Path to xterm " + xtermCmd +
                                                       " is not valid. Please fix $HOME/.flukes.properties!");
                                        kqd.setLocationRelativeTo(GUI.getInstance().getFrame());
                                        kqd.setVisible(true);
                                        return;
                                    }
                                } else {
                                    // Parse access method...
                                    mgt = mgt.replaceAll("://", " " + " -i " +
                                                         GUI.getInstance().getPreference(GUI.PrefsEnum.SSH_KEY) + " " +
                                                         GUI.getInstance().getPreference(GUI.PrefsEnum.SSH_OPTIONS) + " ");
                                    mgt = mgt.replaceAll(":", " -p ");

                                    String[] envp = null;
                                    if (System.getenv("DISPLAY") == null)
                                        envp = new String[]{ "DISPLAY=:0.0" };

                                    String pathEnvStr = System.getenv("PATH");
                                    if (pathEnvStr == null ||
                                        pathEnvStr.indexOf("/bin") < 0 ||
                                        pathEnvStr.indexOf("/usr/bin") < 0) {
                                        mgt = "\"PATH=/bin:/usr/bin " + mgt + "\"";
                                    }
                                    
                                    // run xterm
                                    String command = xtermCmd + " -T \"" + node.getName() + "\" -e " + mgt;
                                    Runtime rt = Runtime.getRuntime();
                                    rt.exec(command, envp);
                                }
                	} catch (IOException ex) {
                		ExceptionDialog ked = new ExceptionDialog(GUI.getInstance().getFrame(),
                                                                          "Unable to login due to exception!");
                		ked.setException("Exception encountered: ", ex);
                		ked.setLocationRelativeTo(GUI.getInstance().getFrame());
                		ked.setVisible(true);
                		return;
                	}
                }
            });
        }
        
		@Override
		public void setNodeAndView(OrcaNode v,
				VisualizationViewer<OrcaNode, OrcaLink> visView) {
			visComp = visView;
			node = v;
		}

		@Override
		public void setPoint(Point2D point) {
			this.point = point;			
		}
    	
    }
}
