package fitting;
public class ParameterSetting {
	private String name = null;
	private Double fixedValue = null,
				   min = null,
				   max = null,
				   increase = null;
	private boolean fixed = false;
	
	public ParameterSetting(String name, Double fixedValue) {
		this.name = name;
		this.fixedValue = fixedValue;
		this.fixed = true;
	}
	
	public ParameterSetting(String name, Double min, Double max, Double increase) {
		this.name = name;
		this.min = min;
		this.max = max;
		this.increase = increase;
		this.fixed = false;
	}
	
	public void setFixed(Double fixedValue) {
		this.fixedValue = fixedValue;
		this.fixed = true;
	}
	
	public Double getFixedValue() {
		return this.fixedValue;
	}
	
	public boolean isFixed() {
		return fixed;
	}
	
	public void setVariable(Double min, Double max, Double increase) {
		this.min = min;
		this.max = max;
		this.increase = increase;
		this.fixed = false;
	}
	
	public Double getMin() {
		return this.min;
	}
	
	public Double getMax() {
		return this.max;
	}
	
	public Double getIncrease() {
		return this.increase;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String toString() {
		if (isFixed()) {
			return this.name + " fixed: value = " + fixedValue; 
		} else {
			return this.name + " variable: min = " + min + ", max = " + max + ", inc = " + increase; 
		}
	}
}