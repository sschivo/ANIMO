/**
 * 
 */
package inat.util;

import inat.analyser.uppaal.UppaalModelAnalyserSMC;
import inat.graph.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.JOptionPane;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cytoscape.Cytoscape;

/**
 * An XML configuration file.
 * 
 * @author B. Wanders
 */
public class XmlConfiguration {
	/**
	 * Path to the verifyta tool for non-SMC properties.
	 */
	public static final String VERIFY_KEY = "/ANIMO/UppaalInvoker/verifyta";
	private static final String DEFAULT_VERIFY = "\\uppaal-4.1.11\\bin-Win32\\verifyta.exe";
	
	/**
	 * Path to the verifyta tool for SMC properties.
	 */
	public static final String VERIFY_SMC_KEY = "/ANIMO/UppaalInvoker/verifytaSMC";
	private static final String DEFAULT_VERIFY_SMC = "\\uppaal-4.1.11\\bin-Win32\\verifyta.exe";
	
	/**
	 * The path to the tracer tool (used to parse traces generated by verifyta).
	 */
	public static final String TRACER_KEY = "/ANIMO/UppaalInvoker/tracer";
	
	/**
	 * Are we the developer? If so, enable more options and more debugging
	 */
	public static final String DEVELOPER_KEY = "/ANIMO/Developer";
	private static final String DEFAULT_DEVELOPER = Boolean.FALSE.toString();
	
	//The value for uncertainty in the reaction parameters
	public static final String UNCERTAINTY_KEY = "/ANIMO/Uncertainty";
	private static final String DEFAULT_UNCERTAINTY = "5";
	
	//We can use reaction-centered model (default), reaction-centered with tables, and reactant-centered.
	public static final String MODEL_TYPE_KEY = "/ANIMO/ModelType";
	public static final String MODEL_TYPE_REACTION_CENTERED = "ReactionCentered",
							   MODEL_TYPE_REACTION_CENTERED_TABLES = "ReactionCenteredTables",
							   MODEL_TYPE_REACTANT_CENTERED = "ReactantCentered";
	public static final String DEFAULT_MODEL_TYPE = MODEL_TYPE_REACTION_CENTERED;
	
	/**
	 * The document that backs this configuration.
	 */
	private Document document;
	private File configFile = null;
	
	//The configuration pairs key->val on which to base the document writing
	private HashMap<String, String> sourceConfig = new HashMap<String, String>();
	
	/**
	 * Read the configuration from already existing file.
	 * 
	 * @param doc the configuration document
	 */
	public XmlConfiguration(Document doc, File configuration) {
		this.document = doc;
		this.configFile = configuration;
		String v;
		v = this.get(VERIFY_KEY, null);
		if (v != null) {
			sourceConfig.put(VERIFY_KEY, v);
		} else {
			sourceConfig.put(VERIFY_KEY, DEFAULT_VERIFY);
		}
		v = this.get(VERIFY_SMC_KEY, null);
		if (v != null) {
			sourceConfig.put(VERIFY_SMC_KEY, v);
		} else {
			sourceConfig.put(VERIFY_SMC_KEY, DEFAULT_VERIFY_SMC);
		}
		v = this.get(DEVELOPER_KEY, null);
		if (v != null) {
			sourceConfig.put(DEVELOPER_KEY, v);
		} else {
			sourceConfig.put(DEVELOPER_KEY, DEFAULT_DEVELOPER);
		}
		v = this.get(UNCERTAINTY_KEY, null);
		if (v != null) {
			sourceConfig.put(UNCERTAINTY_KEY, v);
		} else {
			sourceConfig.put(UNCERTAINTY_KEY, DEFAULT_UNCERTAINTY);
		}
		v = this.get(MODEL_TYPE_KEY, null);
		if (v != null) {
			sourceConfig.put(MODEL_TYPE_KEY, v);
		} else {
			sourceConfig.put(MODEL_TYPE_KEY, DEFAULT_MODEL_TYPE);
		}
		try {
			writeConfigFile();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Create the default configuration file.
	 * @throws ParserConfigurationException 
	 * @throws TransformerException
	 */
	public XmlConfiguration(File configuration) throws ParserConfigurationException, TransformerException, IOException {
		JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Please, find and select the \"verifyta\" tool.\nIt is usually located in the \"bin\" directory of UPPAAL.", "Verifyta", JOptionPane.QUESTION_MESSAGE);
		String verifytaFileName = "verifyta";
		if (UppaalModelAnalyserSMC.areWeUnderWindows()) {
			verifytaFileName += ".exe";
		}
		String verifytaLocationStr = FileUtils.open(verifytaFileName, "Verifyta Executable", Cytoscape.getDesktop());
		if (verifytaLocationStr != null) {
			sourceConfig.put(VERIFY_KEY, verifytaLocationStr);
			sourceConfig.put(VERIFY_SMC_KEY, verifytaLocationStr);
		} else {
			sourceConfig.put(VERIFY_KEY, DEFAULT_VERIFY);
			sourceConfig.put(VERIFY_SMC_KEY, DEFAULT_VERIFY_SMC);
		}
		/*JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Please, find and select the \"tracer\" tool.", "Tracer", JOptionPane.QUESTION_MESSAGE);
		String tracerLocation = FileUtils.open(null, "Tracer Executable", Cytoscape.getDesktop());
		if (tracerLocation != null) {
			sourceConfig.put(TRACER_KEY, tracerLocation);
		} else {
			sourceConfig.put(TRACER_KEY, "\\uppaal-4.1.4\\bin-Win32\\tracer.exe");
		}*/
		sourceConfig.put(DEVELOPER_KEY, DEFAULT_DEVELOPER);
		
		sourceConfig.put(UNCERTAINTY_KEY, DEFAULT_UNCERTAINTY);
		
		sourceConfig.put(MODEL_TYPE_KEY, DEFAULT_MODEL_TYPE);
		
		writeConfigFile(configuration);
	}
	
	public void set(String k, String v) {
		sourceConfig.put(k, v);
	}
	
	public void writeConfigFile() throws ParserConfigurationException, TransformerException, IOException {
		if (configFile == null) throw new IOException("Configuration file null");
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		docBuilder = docFactory.newDocumentBuilder();
		document = docBuilder.newDocument();
		
		Element rootElement = document.createElement("ANIMO");
		document.appendChild(rootElement);
		
		Element uppaalInvoker = document.createElement("UppaalInvoker");
		rootElement.appendChild(uppaalInvoker);
		
		/*Element tracerLocation = document.createElement("tracer");
		tracerLocation.appendChild(document.createTextNode(sourceConfig.get(TRACER_KEY)));
		uppaalInvoker.appendChild(tracerLocation);*/
		
		Element verifytaLocation = document.createElement("verifyta");
		Element verifytaSMCLocation = document.createElement("verifytaSMC");
		verifytaLocation.appendChild(document.createTextNode(sourceConfig.get(VERIFY_KEY)));
		verifytaSMCLocation.appendChild(document.createTextNode(sourceConfig.get(VERIFY_SMC_KEY)));

		uppaalInvoker.appendChild(verifytaLocation);
		uppaalInvoker.appendChild(verifytaSMCLocation);
		
		Element developerNode = document.createElement("Developer");
		developerNode.appendChild(document.createTextNode(sourceConfig.get(DEVELOPER_KEY)));
		rootElement.appendChild(developerNode);
		
		Element uncertaintyNode = document.createElement("Uncertainty");
		uncertaintyNode.appendChild(document.createTextNode(sourceConfig.get(UNCERTAINTY_KEY)));
		rootElement.appendChild(uncertaintyNode);
		
		Element modelTypeNode = document.createElement("ModelType");
		modelTypeNode.appendChild(document.createTextNode(sourceConfig.get(MODEL_TYPE_KEY)));
		rootElement.appendChild(modelTypeNode);
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		DOMSource source = new DOMSource(document);
		StreamResult result = new StreamResult(configFile);
		
		transformer.transform(source, result);
	}
	
	private void writeConfigFile(File configuration) throws ParserConfigurationException, TransformerException, IOException {
		if (configuration == null) throw new IOException("Configuration file null");
		this.configFile = configuration;
		writeConfigFile();
	}
	
	/**
	 * Evaluates the given XPath expression in the context of this document.
	 * 
	 * @param expression the expression to evaluate
	 * @param resultType the result type
	 * @return an object or {@code null}
	 */
	private Object evaluate(String expression, QName resultType) {
		try {
			AXPathExpression xpath = XmlEnvironment.hardcodedXPath(expression);
			return xpath.evaluate(this.document, resultType);
		} catch (XPathExpressionException e) {
			return null;
		}
	}

	/**
	 * Returns a node from this document.
	 * 
	 * @param xpath the selection expression
	 * @return a node
	 */
	public Node getNode(String xpath) {
		return (Node) this.evaluate(xpath, XPathConstants.NODE);
	}

	/**
	 * Returns a set of nodes from this document.
	 * 
	 * @param xpath the selection expression
	 * @return a set of nodes
	 */
	public ANodeList getNodes(String xpath) {
		return new ANodeList((NodeList) this.evaluate(xpath, XPathConstants.NODESET));
	}

	/**
	 * Returns a string from this document.
	 * 
	 * @param xpath the selection expression
	 * @return the string, or {@code null}
	 */
	public String get(String xpath) {
		return (String) this.evaluate(xpath, XPathConstants.STRING);
	}

	/**
	 * Returns a string from this document, or the default value if the string
	 * is not present.
	 * 
	 * @param xpath the selection expression
	 * @param defaultValue the default value
	 * @return the string from the document or the default value
	 */
	public String get(String xpath, String defaultValue) {
		if (this.has(xpath)) {
			return this.get(xpath);
		} else {
			return defaultValue;
		}
	}

	/**
	 * Checks to see whether this document matches the given expression.
	 * 
	 * @param xpath the expression to test
	 * @return {@code true} if the document matches, {@code false} otherwise
	 */
	public boolean has(String xpath) {
		return (Boolean) this.evaluate(xpath, XPathConstants.BOOLEAN);
	}
}
