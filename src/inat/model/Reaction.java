/**
 * 
 */
package inat.model;

import fitting.ScenarioCfg;

import java.io.Serializable;

/**
 * A connection between two components in the network.
 * 
 * @author B. Wanders
 */
public class Reaction extends Entity implements Serializable {
	private static final long serialVersionUID = 6737480973051526963L;

	/**
	 * Constructor.
	 * 
	 * @param id the identifier for this edge
	 */
	public Reaction(String id) {
		super(id);
	}

	public Reaction() {
	}

	/**
	 * returns a (deep) copy of this
	 * @return
	 */
	public Reaction copy() {
		Reaction e = new Reaction(this.id);
		e.setModel(this.getModel());
		e.properties = this.properties.copy();
		return e;
	}

	
	public ScenarioCfg getScenarioCfg() {
		return this.get(Model.Properties.SCENARIO_CFG).as(ScenarioCfg.class);
	}
	
	public void setScenarioCfg(ScenarioCfg cfg) {
		this.let(Model.Properties.SCENARIO_CFG).be(cfg);
	}
	
	public boolean getEnabled() {
		return this.get(Model.Properties.ENABLED).as(Boolean.class);
	}
	
	public String getName() {
		return this.get(Model.Properties.ALIAS).as(String.class);
	}

	@Override
	public String toString() {
		return "Reaction '" + this.getId() + "'";
	}
}
