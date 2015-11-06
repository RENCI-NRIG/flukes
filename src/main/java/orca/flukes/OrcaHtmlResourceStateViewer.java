package orca.flukes;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

public class OrcaHtmlResourceStateViewer extends OrcaResourceStateViewer {

	private JTextPane tp;
	
	public OrcaHtmlResourceStateViewer(JFrame parent,
			List<OrcaResource> resources, Date start, Date end) {
		super(parent, resources, start, end);
	}

	@Override
	protected void initResourceViewer(List<OrcaResource> resources, Date start,
			Date end) {
		tp = new JTextPane();
		tp.setContentType("text/html");
		tp.setPreferredSize(new Dimension(800, 600));
		tp.setEditable(false);
		JScrollPane areaScrollPane = new JScrollPane(tp);
		areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		kp.setLayout(new BorderLayout(0,0));
		kp.add(areaScrollPane);
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html>");
		
		sb.append("<p><b>Resources reserved from " + start + " until " + end + "</b></p>");
		
		sb.append("<hr/>");
		sb.append("<table>");
		String color = null;
		Collections.sort(resources);
		for(OrcaResource res: resources) {
			if (!res.isResource())
				continue;
			String mgt = null;
			if (res instanceof OrcaNode) {
				mgt = ((OrcaNode)res).getSSHManagementAccess();
			}
			color = "black";
			if (OrcaResource.ORCA_FAILED.equalsIgnoreCase(res.getState())) 
				color = "red";
			if (OrcaResource.ORCA_ACTIVE.equalsIgnoreCase(res.getState())) 
				color = "green";
			sb.append("<tr><td>" + res.getName() + "</td><td><font color=\"" + color + "\">" + 
				res.getState() + "</color></td><td>" + res.getReservationNotice() + "</td><td>" + (mgt != null ? mgt : "No management access") + "</td></tr>");
		}
		sb.append("</table>");
		sb.append("</html>");
		
		tp.setText(sb.toString());
	}

}
