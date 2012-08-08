package orca.flukes.ui;

import org.apache.commons.collections15.Factory;

import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.control.EditingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.EditingModalGraphMouse;

/**
 * Class supporting the adding of modifiers to the modal graph mouse. Does not
 * properly work yet.
 * @author ibaldin
 *
 * @param <V>
 * @param <E>
 */
public class EditingModalGraphMouseWithModifiers<V, E> extends EditingModalGraphMouse<V, E> {
	
	protected int modifiers;
	
	/**
	 * create an instance with default values
	 *
	 */
	public EditingModalGraphMouseWithModifiers(int modifiers, RenderContext<V,E> rc,
			Factory<V> vertexFactory, Factory<E> edgeFactory) {
		super(rc, vertexFactory, edgeFactory, 1.1f, 1/1.1f);
		this.modifiers = modifiers;
		editingPlugin = new EditingGraphMousePlugin<V,E>(modifiers, vertexFactory, edgeFactory);
	}

	/**
	 * create an instance with passed values
	 * @param in override value for scale in
	 * @param out override value for scale out
	 */
	public EditingModalGraphMouseWithModifiers(int modifiers, RenderContext<V,E> rc,
			Factory<V> vertexFactory, Factory<E> edgeFactory, float in, float out) {
		super(rc, vertexFactory, edgeFactory, in, out);
		this.modifiers = modifiers;
		editingPlugin = new EditingGraphMousePlugin<V,E>(modifiers, vertexFactory, edgeFactory);
	}

}
