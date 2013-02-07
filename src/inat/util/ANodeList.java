/**
 * 
 */
package inat.util;

import java.util.AbstractList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An augmented node list to take advantage of the java language features.
 * 
 * @author B. Wanders
 */
public class ANodeList extends AbstractList<Node> implements NodeList {
	/**
	 * The backing node list.
	 */
	private final NodeList backing;

	/**
	 * Constructor.
	 * 
	 * @param backing the backing {@link NodeList}
	 */
	public ANodeList(final NodeList backing) {
		this.backing = backing;
	}

	@Override
	public int getLength() {
		return this.backing.getLength();
	}

	@Override
	public Node item(int index) {
		return this.backing.item(index);
	}

	@Override
	public Node get(int index) {
		return this.item(index);
	}

	@Override
	public int size() {
		return this.getLength();
	}

}
