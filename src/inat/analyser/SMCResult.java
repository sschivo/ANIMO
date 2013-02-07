package inat.analyser;

import java.io.Serializable;

/**
 * The result of a SMC query: can be true/false, or a numerical value.
 * A confidence about the answer is also provided.
 */
public class SMCResult implements Serializable {
	
	private static final long serialVersionUID = -1032066046223183090L;

	private boolean isBoolean, //Tells us if the result is boolean, or if it is instead a numerical value
					booleanResult; //The result of a boolean query.
	
	private double confidence, //The confidence value (example: 0.95 means that we have 95% of confidence)
				   lowerBound, //The numerical result is represented as a couple [lowerBound, upperBound], defining the interval in which the result of the query will lay with the probability given by the confidence value.
				   upperBound;

	/**
	 * Build a boolean query result
	 */
	public SMCResult(boolean result, double confidence) {
		this.isBoolean = true;
		this.booleanResult = result;
		this.confidence = confidence;
	}
	
	/**
	 * Build a numerical query result
	 */
	public SMCResult(double lowerBound, double upperBound, double confidence) {
		this.isBoolean = false;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.confidence = confidence;
	}
	
	public boolean isBoolean() {
		return isBoolean;
	}
		
	public double getLowerBound() {
		return this.lowerBound;
	}
	
	public double getUpperBound() {
		return this.upperBound;
	}
	
	public double getConfidence() {
		return this.confidence;
	}
	
	public boolean getBooleanResult() {
		return this.booleanResult;
	}
	
	public String toString() {
		if (isBoolean) {
			return (booleanResult?"TRUE":"FALSE") + " with confidence " + confidence;
		} else {
			return "Probability in [" + lowerBound + ", " + upperBound + "] with confidence " + confidence;
		}
	}
}
