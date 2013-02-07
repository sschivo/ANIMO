/**
 * 
 */
package inat.util;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A utility singleton to maintain the XML environment.
 * 
 * @author B. Wanders
 */
public class XmlEnvironment {
	/**
	 * the document builder factory. (Yes it's a factory factory)
	 */
	private final DocumentBuilderFactory documentBuilderFactory;

	/**
	 * The document builder itself.
	 */
	private final DocumentBuilder documentBuilder;

	/**
	 * The xpath factory. (Actually also a factory factory)
	 */
	private final XPathFactory xpathFactory;

	/**
	 * The xpath. (This is actually a facotry for the {@link XPathExpression})
	 */
	private final XPath xpath;

	/**
	 * Private constructor.
	 */
	private XmlEnvironment() {
		try {
			this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
			this.documentBuilder = this.documentBuilderFactory.newDocumentBuilder();
			this.xpathFactory = XPathFactory.newInstance();
			this.xpath = this.xpathFactory.newXPath();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("XML environment configuration exception.", e);
		}
	}

	/**
	 * The singleton instance.
	 */
	private static XmlEnvironment instance;

	/**
	 * Returns the singleton instance of the {@link XmlEnvironment}.
	 * 
	 * @return the singleton instance
	 */
	public static XmlEnvironment getInstance() {
		if (XmlEnvironment.instance == null) {
			XmlEnvironment.instance = new XmlEnvironment();
		}

		return XmlEnvironment.instance;
	}

	/**
	 * Parses an XML document from a file.
	 * 
	 * @param f the file to load
	 * @return a {@link Document}
	 * @throws SAXException if the XML file is not correct in some way
	 * @throws IOException if the file could not be accessed for some reasion
	 */
	public static Document parse(File f) throws SAXException, IOException {
		return XmlEnvironment.getInstance().documentBuilder.parse(f);
	}

	/**
	 * Parses an XML document from some input stream.
	 * 
	 * @param s the input source
	 * @return a parsed document
	 * @throws SAXException if the XML file is not correct in some way
	 * @throws IOException if the input source could not be accessed for some
	 *             reason
	 */
	public static Document parse(InputSource s) throws SAXException, IOException {
		return XmlEnvironment.getInstance().documentBuilder.parse(s);
	}

	/**
	 * Compiles an hardcoded XPath expression. Use this method if the expression
	 * is hardcoded. If the expression is part of the user input, use
	 * {@link #compileXPath(String)}.
	 * 
	 * @param expression the expression to compile.
	 * @return an augmented XPath exprfession
	 */
	public static AXPathExpression hardcodedXPath(String expression) {
		try {
			return new AXPathExpression(XmlEnvironment.getInstance().xpath.compile(expression), expression);
		} catch (XPathExpressionException e) {
			assert false : "Could not compile XPath expression: " + e.getMessage();
			return null;
		}
	}

	/**
	 * Compiles an hardcoded XPath expression. Use this method if the expression
	 * is hardcoded. if the expression is part of the user input, use
	 * {@link #getXpath()} followed by the appropriate method calls.
	 * 
	 * @param expression the expression to compile.
	 * @return an augmented XPath exprfession
	 * @throws XPathExpressionException if the xpath did not compile
	 */
	public static AXPathExpression compileXPath(String expression) throws XPathExpressionException {
		return new AXPathExpression(XmlEnvironment.getInstance().xpath.compile(expression), expression);

	}

	/**
	 * Returns the document builder.
	 * 
	 * @return the documentBuilder
	 */
	public static DocumentBuilder getDocumentBuilder() {
		return XmlEnvironment.getInstance().documentBuilder;
	}

	/**
	 * Returns the {@link XPath} in this environment.
	 * 
	 * @return the xpath
	 */
	public static XPath getXpath() {
		return XmlEnvironment.getInstance().xpath;
	}
}
