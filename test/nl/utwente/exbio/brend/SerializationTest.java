package nl.utwente.exbio.brend;

import inat.InatBackend;
import inat.exceptions.InatException;
import inat.model.Model;
import inat.model.Reactant;
import inat.model.Reaction;
import inat.serializer.XMLSerializer;
import inat.util.Table;

import java.io.File;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

/**
 * Test of the serialization engine. This test requires human comparison.
 * 
 * @author Brend Wanders
 * 
 */
public class SerializationTest {
	/**
	 * Program entry point.
	 * 
	 * @param args the command line arguments
	 * @throws InatException if the system could not be initialised
	 */
	public static void main(String[] args) throws InatException {
		InatBackend.initialise(new File("ANIMO-configuration.xml"));

		Model m1 = new Model();

		m1.add(new Reaction("r0"));
		m1.add(new Reaction("r1"));
		m1.add(new Reactant("s0"));
		m1.add(new Reactant("s1"));

		m1.getReaction("r0").let("from").be("s0");
		m1.getReaction("r0").let("to").be("s1");

		Reaction r1 = m1.getReaction("r1");
		r1.let("from").be("s0");
		r1.let("to").be("s0");
		r1.let("active").be(true);

		m1.getReactant("s0").let("test").be(new Table(5, 5));
		Table t = m1.getReactant("s0").get("test").as(Table.class);
		t.set(3, 3, 1);
		t.set(4, 4, 2);
		t.set(3, 4, 5);

		System.out.println("Original: " + m1);

		XMLSerializer serializer = new XMLSerializer();

		Document doc = serializer.serializeModel(m1);

		System.out.println(domToString(doc));

		Model m2 = serializer.deserializeModel(doc);

		System.out.println("Deserialized: " + m2);
	}

	/**
	 * Converts a {@link Document} to a string.
	 * 
	 * @param doc the document to convert
	 * @return a string containing the document, or <code>null</code> if an
	 *         error occurred
	 */
	public static String domToString(Document doc) {
		try {
			Source source = new DOMSource(doc);
			StringWriter stringWriter = new StringWriter();
			Result result = new StreamResult(stringWriter);
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
			transformer.transform(source, result);
			return stringWriter.getBuffer().toString();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return null;
	}
}
