package inat.analyser.uppaal.modelchecking;

import inat.model.Model;

public class EventuallyAllPaths extends PathFormula {
	private static final long serialVersionUID = 1963236930138798264L;
	private StateFormula stateFormula = null;
	
	public EventuallyAllPaths(StateFormula stateFormula) {
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
		return "E[] (" + stateFormula.toString() + ")";
	}
	
	@Override
	public String toHumanReadable() {
		return "State " + stateFormula.toHumanReadable() + " can persist indefinitely";
	}
}
