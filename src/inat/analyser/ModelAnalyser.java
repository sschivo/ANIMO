/**
 * 
 */
package inat.analyser;

import inat.model.Model;

/**
 * A model analyzer is responsible for analyzing the {@link Model}. After
 * analysis it will return a result.
 * 
 * @author B. Wanders
 * @param <R> the result type
 */
public interface ModelAnalyser<R> {

	/**
	 * Analyzes the model and returns the result.
	 * 
	 * @param m the model to analyze
	 * @param timeTo the time up to which to analyze the model
	 * @return the result of the analysis.
	 * @throws AnalysisException if the analysis went wrong
	 */
	public R analyze(Model m, int timeTo) throws AnalysisException;
}
