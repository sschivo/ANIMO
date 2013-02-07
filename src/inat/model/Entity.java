package inat.model;

import java.io.Serializable;

/**
 * A base entity in the data model. This class exists to provide a base of
 * functionality for all entities stored within an ANIMO data model.
 * 
 * @author Brend Wanders
 */
public abstract class Entity implements Serializable {
	private static final long serialVersionUID = 5881406500127638652L;
	/**
	 * The unique edge identifier.
	 */
	protected String id;
	/**
	 * The property bag for this edge.
	 */
	protected PropertyBag properties;
	/**
	 * The owning model.
	 */
	private Model model;

	public Entity() {
		this.properties = new PropertyBag();
	}
	
	/**
	 * Constructor with identifier.
	 * 
	 * @param id the identifier of this entity
	 */
	public Entity(String id) {
		this.id = id;
		this.properties = new PropertyBag();
	}
	
	
	/**
	 * Sets the owning model of this species.
	 * 
	 * @param m the owning model
	 */
	protected void setModel(Model m) {
		this.model = m;
	}

	/**
	 * Returns the model in which this reaction is defined.
	 * 
	 * @return the model that owns this reaction
	 */
	public Model getModel() {
		return this.model;
	}

	/**
	 * Returns the properties of this reaction.
	 * 
	 * @return the properties
	 */
	public PropertyBag getProperties() {
		return this.properties;
	}
	
	public void setProperties(PropertyBag properties) {
		this.properties = properties;
	}

	/**
	 * Retrieves a property of this entity.
	 * 
	 * @param name the name of the proeprty to retrieve
	 * @return the property, or <code>null</code> if the property was not
	 *         available
	 */
	public Property get(String name) {
		return this.getProperties().get(name);
	}

	/**
	 * Checks for the availability of a specific property.
	 * 
	 * @param name the name of the property to check for
	 * @return {@code true} if the property is available, {@code false}
	 *         otherwise
	 */
	public boolean has(String name) {
		return this.getProperties().has(name);
	}

	/**
	 * Retrieves a property, creating it if necessary.
	 * 
	 * @param name the name of the property
	 * @return a {@link Property}
	 */
	public Property let(String name) {
		return this.properties.let(name);
	}
	
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Returns the identifier of this edge.
	 * 
	 * @return the id
	 */
	public String getId() {
		return this.id;
	}
	
	/**
	 * comparison
	 * @param e
	 * @return
	 */
	public boolean equals(Entity e) {
		return this.id.equals(e.id);
	}

}