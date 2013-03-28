package inat.analyser.uppaal.modelchecking;

import inat.model.Model;

public class EventuallyExistsPath extends PathFormula {
	private static final long serialVersionUID = 4862946305830347855L;
	private StateFormula stateFormula = null;
	
	public EventuallyExistsPath(StateFormula stateFormula) {
		this.stateFormula = stateFormula;
	}
	
	@Override
	public void setReactantIDs(Model m) {
		stateFormula.setReactantIDs(m);
	}
	
	@Override
	public boolean supportsPriorities() {
		return stateFormula.supportsPriorities();
	}
	
	@Override
	public String toString() {
		return "E<> (" + stateFormula.toString() + ")";
	}
	
	@Override
	public String toHumanReadable() {
		return "It is possible to reach state " + stateFormula.toHumanReadable();
	}
}
