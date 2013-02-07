/**
 * 
 */
package inat.model;

import java.io.Serializable;

/**
 * A vertex is a component in the network.
 * 
 * @author B. Wanders
 */
public class Reactant extends Entity implements Serializable {
	private static final long serialVersionUID = -8610211944385660028L;

	/**
	 * Constructor.
	 * 
	 * @param id the vertex id
	 */
	public Reactant(String id) {
		super(id);
	}
	
	public Reactant() {
	}
	
	/**
	 * returns a (deep) copy of this
	 * @return
	 */
	public Reactant copy() {
		Reactant e = new Reactant(this.id);
		e.setModel(this.getModel());
		e.properties = this.properties.copy();
		return e;
	}

	
	public String getName() {
		return get(Model.Properties.ALIAS).as(String.class);
	}

	@Override
	public String toString() {
		return "Reactant '" + this.getId() + "'";
	}
}
