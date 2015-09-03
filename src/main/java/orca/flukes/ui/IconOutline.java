package orca.flukes.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.Collections;

import javax.swing.Icon;

/**
 * Takes an outline shape and makes it larger and of different colors
 * @author ibaldin
 *
 */
public class IconOutline implements Icon {

	Shape outline;
	Color color;
	
	public IconOutline(Shape o, Color c) {
		AffineTransform enlarge = AffineTransform.getScaleInstance(1.1, 1.1);
		outline = enlarge.createTransformedShape(o);
		AffineTransform translate = AffineTransform.getTranslateInstance(outline.getBounds().getWidth()/2, outline.getBounds().getHeight()/2);
		outline = translate.createTransformedShape(outline);
		color = c;
	}
	
	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Shape shape = AffineTransform.getTranslateInstance(x, y).createTransformedShape(outline);
		Graphics2D g2d = (Graphics2D)g;
		g2d.addRenderingHints(Collections.singletonMap(RenderingHints.KEY_ANTIALIASING, 
				RenderingHints.VALUE_ANTIALIAS_ON));
		Stroke stroke = g2d.getStroke();
		g2d.setStroke(new BasicStroke(2));
		g2d.setColor(color);
		g2d.draw(shape);
		
		g2d.setStroke(stroke);
	}

	@Override
	public int getIconWidth() {
		return outline.getBounds().width;
	}

	@Override
	public int getIconHeight() {
		return outline.getBounds().height;
	}

}
