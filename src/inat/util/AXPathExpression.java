/**
 * 
 */
package inat.util;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * An augmneted {@link XPathExpression}. This expression provides functionality
 * to evaluate the expression in the context of a given {@link Node} while
 * taking the requested type in consideration.
 * 
 * @author B. Wanders
 */
public class AXPathExpression implements XPathExpression {
	/**
	 * The backing expression.
	 */
	private final XPathExpression backing;
	/**
	 * The expression. Mostly used for debugging purposes.
	 */
	private final String expression;

	/**
	 * Constructor.
	 * 
	 * @param backing the backing expression
	 * @param expression a string representation of the expression
	 */
	public AXPathExpression(final XPathExpression backing, String expression) {
		this.backing = backing;
		this.expression = expression;
	}

	@Override
	public Object evaluate(InputSource source, QName returnType) throws XPathExpressionException {
		return this.backing.evaluate(source, returnType);
	}

	@Override
	public String evaluate(InputSource source) throws XPathExpressionException {
		return this.backing.evaluate(source);
	}

	@Override
	public Object evaluate(Object item, QName returnType) throws XPathExpressionException {
		return this.backing.evaluate(item, returnType);
	}

	@Override
	public String evaluate(Object item) throws XPathExpressionException {
		return this.backing.evaluate(item);
	}

	/**
	 * Retrieves a single {@link Node}.
	 * 
	 * @param root the node to be used as context
	 * @return a Node
	 * @throws XPathExpressionException if the xpath failed to evaluate
	 */
	public Node getNode(Node root) throws XPathExpressionException {
		return (Node) this.evaluate(root, XPathConstants.NODE);
	}

	/**
	 * Retrieves a single {@link String}.
	 * 
	 * @param root the node to be used as context
	 * @return a Node
	 * @throws XPathExpressionException if the xpath failed to evaluate
	 */
	public String getString(Node root) throws XPathExpressionException {
		return (String) this.evaluate(root, XPathConstants.STRING);
	}

	/**
	 * Retrieves a list of nodes as a {@link ANodeList}.
	 * 
	 * @param root the node to be used as context
	 * @return a Node
	 * @throws XPathExpressionException if the xpath failed to evaluate
	 */
	public ANodeList getNodes(Node root) throws XPathExpressionException {
		return new ANodeList((NodeList) this.evaluate(root, XPathConstants.NODESET));
	}

	/**
	 * Retrieves a boolean.
	 * 
	 * @param root the node to be used as context
	 * @return a Node
	 * @throws XPathExpressionException if the xpath failed to evaluate
	 */

	public boolean getBoolean(Node root) throws XPathExpressionException {
		return (Boolean) this.evaluate(root, XPathConstants.BOOLEAN);
	}

	@Override
	public String toString() {
		return "'" + this.expression + "'";
	}
}
