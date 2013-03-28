package inat.analyser.uppaal.modelchecking;

import inat.model.Model;

public class AlwaysExistsPath extends PathFormula {
	private static final long serialVersionUID = -7293284341284437511L;
	private StateFormula stateFormula = null;
	
	public AlwaysExistsPath(StateFormula stateFormula) {
		this.stateFormula = stateFormula;
	}
	
	@Override
	public void setReactantIDs(Model m) {
		stateFormula.setReactantIDs(m);
	}
	
	@Override
	public boolean supportsPriorities() {
		return false;
	}
	
	@Override
	public String toString() {
		return "A<> (" + stateFormula.toString() + ")";
	}
	
	@Override
	public String toHumanReadable() {
		return "State " + stateFormula.toHumanReadable() + " is always eventually reached";
	}
}
