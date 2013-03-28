package inat.analyser.uppaal.modelchecking;

import inat.model.Model;

public class Implies extends PathFormula {
	private static final long serialVersionUID = -6814467311406782648L;
	private StateFormula first = null,
						 second = null;
	
	public Implies(StateFormula first, StateFormula second) {
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
		return false;
	}
	
	@Override
	public String toString() {
		return first.toString() + " --> " + second.toString();
	}
	
	@Override
	public String toHumanReadable() {
		return "If state " + first.toHumanReadable() + " occurs, then it is necessarily followed by state " + second.toHumanReadable();
	}
}
