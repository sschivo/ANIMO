package inat.analyser.uppaal.modelchecking;

import inat.model.Model;

public class AlwaysAllPaths extends PathFormula {
	private static final long serialVersionUID = -3658792845599547773L;
	private StateFormula stateFormula = null;
	
	public AlwaysAllPaths(StateFormula stateFormula) {
		this.stateFormula = stateFormula;
	}
	
	@Override
	public void setReactantIDs(Model m) {
		stateFormula.setReactantIDs(m);
	}
	
	@Override
	public boolean supportsPriorities() {
		return true;
	}
	
	@Override
	public String toString() {
		return "A[] (" + stateFormula.toString() + ")";
	}
	
	@Override
	public String toHumanReadable() {
		return "State " + stateFormula.toHumanReadable() + " must persist indefinitely";
	}
}
