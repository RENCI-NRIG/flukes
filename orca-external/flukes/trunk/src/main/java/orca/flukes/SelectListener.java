package orca.flukes;

import java.util.Set;

/**
 * For components that want to know about selected vertices
 * @author ibaldin
 *
 * @param <V>
 */
public interface SelectListener<V> {

	void setSelectedNodes(Set<V> nodes);
}
