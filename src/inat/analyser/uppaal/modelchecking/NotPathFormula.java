package inat.analyser.uppaal.modelchecking;

import inat.model.Model;

public class NotPathFormula extends PathFormula {
	private static final long serialVersionUID = 1302610014450326274L;
	private PathFormula negatedFormula = null;
	
	public NotPathFormula(PathFormula negatedFormula) {
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
