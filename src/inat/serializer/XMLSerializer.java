/**
 * 
 */
package inat.serializer;

import inat.exceptions.SerializationException;
import inat.model.Model;
import inat.model.Property;
import inat.model.PropertyBag;
import inat.model.Reaction;
import inat.model.Reactant;
import inat.util.AXPathExpression;
import inat.util.Table;
import inat.util.XmlEnvironment;

import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The XML serializer is used to convert a model from and to XML.
 * 
 * @author B. Wanders
 */
public class XMLSerializer {
	/**
	 * The type serializers.
	 */
	private final Map<String, TypeSerializer<?>> typeSerializers;

	/**
	 * Constructor.
	 */
	public XMLSerializer() {
		this.typeSerializers = new HashMap<String, TypeSerializer<?>>();
		this.typeSerializers.put(String.class.getCanonicalName(), new StringSerializer());
		this.typeSerializers.put(Boolean.class.getCanonicalName(), new BooleanSerializer());
		this.typeSerializers.put(Integer.class.getCanonicalName(), new IntegerSerializer());
		this.typeSerializers.put(Float.class.getCanonicalName(), new FloatSerializer());
		this.typeSerializers.put(Table.class.getCanonicalName(), new TableSerializer());
	}

	/**
	 * Converts a model to it's XML representation.
	 * 
	 * @param m the model
	 * @return the document
	 * @throws SerializationException if the serialization failed
	 */
	public Document serializeModel(Model m) throws SerializationException {
		Document doc = XmlEnvironment.getDocumentBuilder().newDocument();

		// create root and serialize properties to it
		Element root = doc.createElement("ANIMO-model");
		Element properties = this.serializeProperties(doc, m.getProperties());
		root.appendChild(properties);

		// serialize the vertices
		Element reactants = doc.createElement("reactants");
		for (Reactant reactant : m.getReactantCollection()) {
			// create vertex element and set id and properties
			Element e = doc.createElement("reactant");
			e.setAttribute("id", reactant.getId());

			// append property bag
			e.appendChild(this.serializeProperties(doc, reactant.getProperties()));

			reactants.appendChild(e);
		}
		root.appendChild(reactants);

		// serialize the edges
		Element reactions = doc.createElement("reactions");
		for (Reaction reaction : m.getReactionCollection()) {
			// create edge element and set id
			Element e = doc.createElement("reaction");
			e.setAttribute("id", reaction.getId());

			// append property bag
			e.appendChild(this.serializeProperties(doc, reaction.getProperties()));

			reactions.appendChild(e);
		}
		root.appendChild(reactions);

		doc.appendChild(root);

		return doc;
	}

	/**
	 * Serializes the {@link PropertyBag} to a &lt;properties&gt; element.
	 * 
	 * @param doc the document to serialize into
	 * @param bag the bag to serialize
	 * @return an element describing the property bag
	 * @throws SerializationException if the serialization failed
	 */
	public Element serializeProperties(Document doc, PropertyBag bag) throws SerializationException {
		Element result = doc.createElement("properties");

		// loop over properties
		for (Property p : bag) {
			// create property element
			Element e = doc.createElement("property");
			e.setAttribute("name", p.getName());

			// check for null
			if (!p.isNull()) {
				Class<?> type = p.as(Object.class).getClass();

				// set type attribute
				e.setAttribute("type", type.getCanonicalName());

				// retrieve the specialized serializer
				TypeSerializer<?> serializer = this.typeSerializers.get(type.getCanonicalName());

				if (serializer == null) {
					throw new SerializationException("No specialized serializer registered for type <"
							+ type.getCanonicalName() + ">, did you forget adding your new serializer?");
				}

				// append child node to property element
				e.appendChild(serializer.serialize(doc, p.as(Object.class)));

			} else {
				// what if it is null?
			}

			// append property element to bag element
			result.appendChild(e);
		}

		return result;
	}

	/**
	 * Converts an XML representation to an {@link Model}.
	 * 
	 * @param d the document to deserialize
	 * @return the model
	 * @throws SerializationException if the deserialization failed
	 */
	public Model deserializeModel(Document d) throws SerializationException {
		final AXPathExpression modelProperties = XmlEnvironment.hardcodedXPath("/inat-model/properties");
		final AXPathExpression vertices = XmlEnvironment.hardcodedXPath("/inat-model/reactants/reactant");
		final AXPathExpression edges = XmlEnvironment.hardcodedXPath("/inat-model/reactions/reaction");
		final AXPathExpression properties = XmlEnvironment.hardcodedXPath("./properties");
		final AXPathExpression idAttribute = XmlEnvironment.hardcodedXPath("@id");

		Model m = new Model();
		try {
			// deserialize properties on whole model
			this.deserializerProperties(modelProperties.getNode(d.getDocumentElement()), m.getProperties());

			// deserialize the vertices
			for (Node root : vertices.getNodes(d.getDocumentElement())) {
				String id = idAttribute.getString(root);
				Reactant v = new Reactant(id);
				this.deserializerProperties(properties.getNode(root), v.getProperties());
				m.add(v);
			}
			// deserialize edges
			for (Node root : edges.getNodes(d.getDocumentElement())) {
				String id = idAttribute.getString(root);
				Reaction e = new Reaction(id);
				this.deserializerProperties(properties.getNode(root), e.getProperties());
				m.add(e);
			}

		} catch (XPathExpressionException e) {
			throw new SerializationException("Could not evaluate XPath expression during deserialization.", e);
		}

		return m;
	}

	/**
	 * Deserializes the properties described in {@code node} into the given
	 * {@link PropertyBag}.
	 * 
	 * @param root the node containing the properties
	 * @param properties the bag into which they should be deserialized
	 * @throws SerializationException if the deserialization failed
	 */
	public void deserializerProperties(Node root, PropertyBag properties) throws SerializationException {
		// compile XPaths
		final AXPathExpression props = XmlEnvironment.hardcodedXPath("./property");
		final AXPathExpression propName = XmlEnvironment.hardcodedXPath("@name");
		final AXPathExpression propType = XmlEnvironment.hardcodedXPath("@type");

		try {
			for (Node n : props.getNodes(root)) {
				// determine name of property
				String name = propName.getString(n);
				Object value = null;
				// if the property has a type, it is non-null
				if (propType.getBoolean(n)) {
					String type = propType.getString(n);

					// get serializer
					TypeSerializer<?> serializer = this.typeSerializers.get(type);
					if (serializer == null) {
						throw new SerializationException("Could not find deserializer for type <" + type
								+ ">, did the developer forget to register it?");
					}

					// deserialize and set
					value = serializer.deserialize(n);
				}
				properties.let(name).be(value);
			}
		} catch (XPathExpressionException e) {
			throw new SerializationException("Could not evaluate XPath expression.", e);
		}
	}
}
