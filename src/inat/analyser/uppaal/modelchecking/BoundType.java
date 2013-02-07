package inat.analyser.uppaal.modelchecking;

public enum BoundType {
	LT, LE, EQ, GE, GT;
	
	public String toString() {
		if (this == LT) {
			return " < ";
		}
		if (this == LE) {
			return " <= ";
		}
		if (this == EQ) {
			return " = ";
		}
		if (this == GE) {
			return " >= ";
		}
		if (this == GT) {
			return " > ";
		}
		return null;
	}
	
	public String toHumanReadable() {
		return toString();
	}
}
