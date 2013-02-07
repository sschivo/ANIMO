/**
 * 
 */
package inat.analyser.uppaal;

import inat.analyser.AnalysisException;
import inat.model.Model;

/**
 * The result analyser is responsible for the analysis of the UPPAAL trace or
 * verivication result.
 * 
 * @author B. Wanders
 * @param <R> the result type
 */
public interface ResultInterpreter<R> {
	/**
	 * Analyzes the UPPAAL output and converts it to a result.
	 * 
	 * @param model the ANIMO model for which these results were created
	 * @param output the UPPAAL output
	 * @return a result
	 * @throws AnalysisException if the analysis of the trace failed
	 */
	public R analyse(Model model, String output) throws AnalysisException;
}
