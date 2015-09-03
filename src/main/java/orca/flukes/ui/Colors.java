package orca.flukes.ui;

import java.awt.Color;

public enum Colors {
	ACTIVE(0x88D3AB),
	FAILED(0xFB394D),
	TICKETED(0x919999),
	REQUEST(0x000000),
	RESOURCE(0x7F94B0),
	COLORLINK(0x2A8FBD);
	
	private final Color c;
	
	private Colors(int rgb) {
		c = new Color(rgb);
	}
	
	public Color getColor() {
		return c;
	}
}
