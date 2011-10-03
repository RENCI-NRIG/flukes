package orca.flukes;

public class GUIResourceState extends GUICommonState {
	public static final String WORLD_ICON="worldmap.jpg";
	
	private static GUIResourceState instance = null;
	
	private static void initialize() {
		;
	}
	
	static GUIResourceState getInstance() {
		if (instance == null) {
			initialize();
			instance = new GUIResourceState();
		}
		return instance;
	}
}
