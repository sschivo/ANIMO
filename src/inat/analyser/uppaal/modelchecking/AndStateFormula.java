package inat.analyser.uppaal.modelchecking;

import inat.model.Model;

public class AndStateFormula extends StateFormula {
	private static final long serialVersionUID = 4675788718842277408L;
	private StateFormula first = null, second = null;
	
	public AndStateFormula(StateFormula first, StateFormula second) {
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
		return "(" + first + ") && (" + second + ")";
	}
	
	@Override
	public String toHumanReadable() {
		return "(" + first.toHumanReadable() + ") AND (" + second.toHumanReadable() + ")";
	}
}
