/**
 * 
 */
package inat.model;

import inat.util.Pair;
import inat.util.Quadruple;
import inat.util.Table;
import inat.util.Triple;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A property on a model component.
 * 
 * @author B. Wanders
 */
public class Property implements Serializable {
	private static final long serialVersionUID = 5029044740449565050L;

	/**
	 * The array of accepted types.
	 */
	private static final Class<?>[] acceptedTypesArray = { Table.class, String.class, Boolean.class, Integer.class,
			Float.class };

	/**
	 * The {@link Set} containing all accepted property types.
	 */
	public static final Set<Class<?>> TYPES;
	static {
		TYPES = new HashSet<Class<?>>(Arrays.asList(acceptedTypesArray));
	}

	/**
	 * The property name.
	 */
	private String name;

	/**
	 * The property value.
	 */
	private Object value;

	/**
	 * Is the given type accepted?
	 * 
	 * @param type a {@link Class} instance representing the type
	 * @return {@code true} if the type is allowed, {@code false} otherwise
	 */
	public static boolean isAccepted(Class<?> type) {
		return TYPES.contains(type);
	}

	/**
	 * Property constructor.
	 * 
	 * @param name the name of the property
	 */
	public Property(String name) {
		this.name = name;
		this.value = null;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param p the property to copy
	 */
	public Property(Property p) {
		this.name = p.name;
		this.value = p.value;
	}
	
	public Property() {
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setValue(Object value) {
		this.value = value;
	}
	
	public Object getValue() {
		return value;
	}
	
	/**
	 * returns a (deep) copy of this
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public Property copy() {
		Property property = new Property(this.name);
		Object o = this.value;
		if (o instanceof Table) {
			property.value = ((Table)o).copy();
		} else if (o instanceof Pair) {
			property.value = ((Pair)o).copy();
		} else if (o instanceof Triple) {
			property.value = ((Triple)o).copy();
		} else if (o instanceof Quadruple) {
			property.value = ((Quadruple)o).copy();
		} else {
			property.value = this.value;
		}
		return property;
	}

	/**
	 * Retrieves a property as the requested value type.
	 * 
	 * @param <R> the requested type
	 * @param requested a {@link Class} instance representing the requested type
	 * @return the value of this property
	 * @throws ClassCastException if the value stored in this property is can
	 *             not be cast to the requested type
	 */
	public <R> R as(Class<R> requested) {
		assert this.isA(requested) : "Could not cast property '" + this.name + "' from <"
				+ this.value.getClass().getCanonicalName() + "> to <" + requested.getCanonicalName() + ">";

		return requested.cast(this.value);
	}

	/**
	 * Determines whether this property's value is of the requested type.
	 * 
	 * @param <R> the request type
	 * @param requested a {@link Class} instance representing the requested type
	 * @return {@code true} if the contained value is of the requested type,
	 *         {@code false} otherwise
	 */
	public <R> boolean isA(Class<R> requested) {
		return this.value == null || requested.isAssignableFrom(this.value.getClass());
	}

	/**
	 * Checks to see if this property is null.
	 * 
	 * @return {@code true} if the value of this property is null, {@code false}
	 *         otherwise
	 */
	public boolean isNull() {
		return this.value == null;
	}

	/**
	 * Sets the value of this property.
	 * 
	 * @param <T> the type of the value
	 * @param value the value itself
	 */
	public <T> void set(T value) {
		assert value == null || isAccepted(value.getClass()) : "Property '" + this.name + "' value type <"
				+ value.getClass().getCanonicalName() + "> is not allowed.";

		this.value = value;
	}

	/**
	 * Lets the property be {@code value}.
	 * 
	 * @param <T> the type of the value
	 * @param value the value itself
	 */
	public <T> void be(T value) {
		this.set(value);
	}

	/**
	 * Returns the name of this property.
	 * 
	 * @return the name of this property
	 */
	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return "Property['" + this.name + "' = " + this.value + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
		result = prime * result + ((this.value == null) ? 0 : this.value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Property other = (Property) obj;
		if (this.name == null) {
			if (other.name != null)
				return false;
		} else if (!this.name.equals(other.name))
			return false;
		if (this.value == null) {
			if (other.value != null)
				return false;
		} else if (!this.value.equals(other.value))
			return false;
		return true;
	}

}
