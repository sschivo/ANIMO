package inat.analyser.uppaal.modelchecking;

import inat.model.Model;

public class NotStateFormula extends StateFormula {
	private static final long serialVersionUID = 1302610014450326274L;
	private StateFormula negatedFormula = null;
	
	public NotStateFormula(StateFormula negatedFormula) {
		this.negatedFormula = negatedFormula;
	}
	
	@Override
	public void setReactantIDs(Model m) {
		negatedFormula.setReactantIDs(m);
	}
	
	@Override
	public boolean supportsPriorities() {
		return negatedFormula.supportsPriorities();
	}
	
	@Override
	public String toString() {
		return "not (" + negatedFormula.toString() + ")";
	}
	
	@Override
	public String toHumanReadable() {
		return "NOT (" + negatedFormula.toHumanReadable() + ")";
	}

}
