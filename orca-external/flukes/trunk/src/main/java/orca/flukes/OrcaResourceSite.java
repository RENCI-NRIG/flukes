package orca.flukes;

import javax.swing.ImageIcon;

import edu.uci.ics.jung.visualization.LayeredIcon;

public class OrcaResourceSite extends OrcaNode {

	public OrcaResourceSite(String name) {
		super(name, 
				new LayeredIcon(new ImageIcon(GUIRequestState.class.getResource(OrcaNodeEnum.RESOURCESITE.getIconName())).getImage()));
		domain = name;
	}
}
