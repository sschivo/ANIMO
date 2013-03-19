package inat.analyser;

import inat.analyser.uppaal.SimpleLevelResult;

import java.io.Serializable;

/**
 * The result of a SMC query: can be true/false, or a numerical value.
 * A confidence about the answer is also provided.
 */
public class SMCResult implements Serializable {
	
	private static final long serialVersionUID = -1032066046223183090L;

	public static int RESULT_TYPE_BOOLEAN = 1,
					  RESULT_TYPE_NUMERIC = 2,
					  RESULT_TYPE_TRACE = 3;
	
	private int resultType; //Tells us if the result is simple boolean, if it is a numerical value or a trace
	
	private boolean booleanResult; //The result of a boolean query.
	
	private double confidence, //The confidence value (example: 0.95 means that we have 95% of confidence)
				   lowerBound, //The numerical result is represented as a couple [lowerBound, upperBound], defining the interval in which the result of the query will lay with the probability given by the confidence value.
				   upperBound;
	
	private SimpleLevelResult levelResult; //The LevelResult in case we have a trace

	/**
	 * Build a boolean query result
	 */
	public SMCResult(boolean result, double confidence) {
		this.resultType = RESULT_TYPE_BOOLEAN;
		this.booleanResult = result;
		this.confidence = confidence;
	}
	
	/**
	 * Build a numerical query result
	 */
	public SMCResult(double lowerBound, double upperBound, double confidence) {
		this.resultType = RESULT_TYPE_NUMERIC;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.confidence = confidence;
	}
	
	/**
	 * Build a LevelResult
	 */
	public SMCResult(boolean result, SimpleLevelResult trace) {
		this.resultType = RESULT_TYPE_TRACE;
		this.booleanResult = result;
		this.levelResult = trace;
	}
	
	public int getResultType() {
		return resultType;
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
	
	public SimpleLevelResult getLevelResult() {
		return this.levelResult;
	}
	
	public String toString() {
		if (resultType == RESULT_TYPE_BOOLEAN) {
			return (booleanResult?"TRUE":"FALSE") + ((confidence != 1)?(" with confidence " + confidence):"");
		} else if (resultType == RESULT_TYPE_NUMERIC) {
			return "Probability in [" + lowerBound + ", " + upperBound + "]" + ((confidence != 1)?(" with confidence " + confidence):"");
		} else {
			return (booleanResult?"TRUE":"FALSE") + " with a sample trace";
		}
	}
}
