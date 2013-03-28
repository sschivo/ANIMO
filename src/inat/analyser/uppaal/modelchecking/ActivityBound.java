package inat.analyser.uppaal.modelchecking;

import inat.model.Model;

public class ActivityBound extends StateFormula {
	private static final long serialVersionUID = -774714007280699219L;
	BoundType bound = null;
	ReactantName reactantName = null;
	String expression = null;
	
	public ActivityBound(ReactantName reactantName, BoundType bound, String expression) {
		this.reactantName = reactantName;
		this.bound = bound;
		this.expression = expression;
	}
	
	@Override
	public void setReactantIDs(Model m) {
		reactantName.setReactantID(m);
	}
	
	@Override
	public boolean supportsPriorities() {
		return true;
	}
	
	@Override
	public String toString() {
		String b;
		if (bound.equals(BoundType.EQ)) {
			b = " == ";
		} else {
			b = bound.toString();
		}
		return reactantName.toString() + b + expression;
	}
	
	@Override
	public String toHumanReadable() {
		return reactantName.toHumanReadable() + bound + expression;
	}
}
