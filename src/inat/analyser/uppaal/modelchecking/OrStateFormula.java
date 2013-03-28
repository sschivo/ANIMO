package inat.analyser.uppaal.modelchecking;

import inat.model.Model;

public class OrStateFormula extends StateFormula {
	private static final long serialVersionUID = 7685211050788841583L;
	private StateFormula first = null, second = null;
	
	public OrStateFormula(StateFormula first, StateFormula second) {
		this.first = first;
		this.second = second;
	}
	
	@Override
	public void setReactantIDs(Model m) {
		first.setReactantIDs(m);
		second.setReactantIDs(m);
	}
	
	@Override
	public boolean supportsPriorities() {
		return (this.first.supportsPriorities() && this.second.supportsPriorities());
	}
	
	@Override
	public String toString() {
		return "(" + first + ") || (" + second + ")";
	}
	
	@Override
	public String toHumanReadable() {
		return "(" + first.toHumanReadable() + ") OR (" + second.toHumanReadable() + ")";
	}
}
