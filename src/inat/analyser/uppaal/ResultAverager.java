package inat.analyser.uppaal;

import inat.analyser.AnalysisException;
import inat.cytoscape.InatActionTask;
import inat.model.Model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import cytoscape.task.TaskMonitor;

/**
 * Used to produce the average result of a series of simulation queries.
 * Standard deviation can also be produced on request.
 */
public class ResultAverager {
	public static final String STD_DEV = "_stddev", //NOTICE: it needs to be lowercase, because elsewhere we assume it is so. We also assume that it starts with "_"
							   OVERLAY_NAME = "_overlay";
	private TaskMonitor monitor = null; //If we are operating via the user interface, we can show the point at which we are with the simulations
	private InatActionTask runAction = null; //If we are operating via the user interface, this will tell us if the user has requested that we cancel the simulations
	
	public ResultAverager(TaskMonitor monitor, InatActionTask runAction) {
		this.monitor = monitor;
		this.runAction = runAction;
	}
	
	/**
	 * Analyze the model nRuns times, doing simulations until timeTo, and return a vector containing all results
	 * @param m The model to be analyzed
	 * @param timeTo The time limit for the simulation runs
	 * @param nRuns The number of times to analyze the model
	 * @return A Vector containing all the single results coming from the analysis
	 * @throws AnalysisException
	 * @throws Exception
	 */
	private Vector<SimpleLevelResult> computeNRuns(Model m, int timeTo, int nRuns) throws AnalysisException, Exception {
		Vector<SimpleLevelResult> results = new Vector<SimpleLevelResult>(nRuns);
		//UppaalModelAnalyserFasterConcrete analyzer = new UppaalModelAnalyserFasterConcrete(monitor, runAction);
		UppaalModelAnalyserSMC/*FasterSymbolicConcretized*/ analyzer = new UppaalModelAnalyserSMC/*FasterSymbolicConcretized*/(null, runAction);
		monitor.setStatus("Computing " + nRuns + " simulation runs");
		double sumTimes = 0;
		long startTime, endTime;
		for (int i=0;i<nRuns;i++) {
			//analyzer = new UppaalModelAnalyserFasterSymbolicConcretized(null, runAction);
			if (runAction != null && runAction.needToStop()) {
				throw new AnalysisException("User interrupted");
			}
			if (monitor != null) {
				monitor.setPercentCompleted((int)((double)i / nRuns * 100));
			}
			System.err.print((i+1));
			startTime = System.currentTimeMillis();
			results.add((SimpleLevelResult)(analyzer.analyze(m, timeTo)));
			endTime = System.currentTimeMillis();
			sumTimes += (endTime - startTime);
			monitor.setEstimatedTimeRemaining(Math.round(sumTimes / (i + 1) * (nRuns - i)));
		}
		return results;
	}
	
	/**
	 * Analyze the model and return all results grouped in one LevelResult, where
	 * each series name is given by OriginalSeriesName + OVERLAY_NAME + number
	 * so that we will be able to recognize different series that are still to be
	 * considered as "belonging to the same group"
	 */
	public SimpleLevelResult analyzeOverlay(Model m, int timeTo, int nRuns) throws AnalysisException, Exception {
		Vector<SimpleLevelResult> results = computeNRuns(m, timeTo, nRuns);
		return overlay(results);
	}
	
	/**
	 * Analyse the given model, with a reachability query E<> (globalTime > timeTo) (with timeTo given),
	 * and produce a result showing the average activity levels of all reactants in the model during the simulation
	 * interval. The Standard Deviation from the averages is also included.
	 * @param m The model
	 * @param timeTo The time up to which a single simulation will run
	 * @param nRuns How many simulation runs we need to compute the average of
	 * @return A SimpleLevelResult showing the averages (and, is requested, the standard deviations) of activity levels of all reactants in the given model
	 * @throws AnalysisException
	 * @throws Exception
	 */
	public SimpleLevelResult analyzeAverage(Model m, int timeTo, int nRuns) throws AnalysisException, Exception {
		Vector<SimpleLevelResult> results = computeNRuns(m, timeTo, nRuns);
		return average(results);
	}
	
	/**
	 * Given a vector of SimpleLevelResults, computes a new SimpleLevelResult in which the
	 * series represent the averages (and standard deviations) of the series
	 * contained in the given vector. Of course, all SimpleLevelResults in the vector are expected
	 * to have the exact same series names. Time instants can be instead different: the number of time
	 * instants contained in the result will correspond to the average number of time instants
	 * found in the input SimpleLevelResults.
	 * @param results The vector containing all the SimpleLevelResults of which we have to compute the average/StdDev
	 * @return A single SimpleLevelResult containing the averages (and StdDevs, if needed) of the
	 * series present in the input SimpleLevelResults
	 * @throws Exception
	 */
	public SimpleLevelResult average(Vector<SimpleLevelResult> results) throws Exception {
		if (results.isEmpty()) throw new Exception("Empty result set");
		Map<String, SortedMap<Double, Double>> result = new HashMap<String, SortedMap<Double, Double>>();
		Set<String> reactantIds = results.firstElement().getReactantIds();
		int maxNLevels = -1;
		
		for (String k : reactantIds) {
			result.put(k, new TreeMap<Double, Double>());
			result.put(k + STD_DEV, new TreeMap<Double, Double>());
		}
		
		double finalTime = results.firstElement().getTimeIndices().get(results.firstElement().getTimeIndices().size()-1);
		int avgSize = 0;
		for (SimpleLevelResult l : results) {
			avgSize += l.getTimeIndices().size();
			if (maxNLevels < l.getNumberOfLevels()) {
				maxNLevels = l.getNumberOfLevels();
			}
		}
		avgSize = (int)Math.round(1.0 * avgSize / results.size());
		if (avgSize < 200) avgSize = 200;
		double increment = finalTime / avgSize;
		int nValues = results.size();
		double sum = 0, sumSqrs = 0, average = 0, stdDev = 0;
		if (monitor != null) {
			monitor.setStatus("Computing averages");
			monitor.setPercentCompleted(0);
		}
		for (double i=0;i<finalTime;i+=increment) {
			for (String k : reactantIds) {
				sum = 0;
				sumSqrs = 0;
				for (SimpleLevelResult l : results) {
					double val = l.getInterpolatedConcentration(k, i);
					sum += val;
					sumSqrs += val * val;
				}
				average = sum / nValues;
				stdDev = Math.sqrt((nValues * sumSqrs - sum * sum)/ (nValues * (nValues - 1)));
				result.get(k).put(i, average);
				result.get(k + STD_DEV).put(i, stdDev);
			}
			monitor.setPercentCompleted((int)((double)i / finalTime * 100));
		}
		for (String k : reactantIds) {
			sum = 0;
			sumSqrs = 0;
			for (SimpleLevelResult l : results) {
				double val = l.getInterpolatedConcentration(k, finalTime);
				sum += val;
				sumSqrs += val * val;
			}
			average = sum / nValues;
			stdDev = Math.sqrt((nValues * sumSqrs - sum * sum)/ (nValues * (nValues - 1)));
			result.get(k).put(finalTime, average);
			result.get(k + STD_DEV).put(finalTime, stdDev);
		}
		
		
		return new SimpleLevelResult(maxNLevels, result);
	}
	/*public SimpleLevelResult average(Vector<SimpleLevelResult> results) throws Exception {
		if (results.isEmpty()) throw new Exception("Empty result set");
		Map<String, SortedMap<Double, Double>> result = new HashMap<String, SortedMap<Double, Double>>();
		Set<String> reactantIds = results.firstElement().getReactantIds();
		
		int nPoints = 0;
		for (String k : reactantIds) {
			result.put(k, new TreeMap<Double, Double>());
			result.put(k + STD_DEV, new TreeMap<Double, Double>());
			nPoints += results.firstElement().levels.get(k).size();
		}
		
		int nValues = results.size();
		double sum, sumSqrs, average, stdDev;
		if (monitor != null) {
			monitor.setStatus("Computing averages");
			monitor.setPercentCompleted(0);
		}
		
		int currentPoint = 0;
		for (String k : reactantIds) {
			Map<Double, Double> vals = results.firstElement().levels.get(k);
			for (double i : vals.keySet()) { //Instead of choosing the average number of points in a series, I take the time instants of the first series to compute the average
				sum = 0;
				sumSqrs = 0;
				for (SimpleLevelResult l : results) {
					double val = l.getConcentration(k, i);
					sum += val;
					sumSqrs += val * val;
				}
				average = sum / nValues;
				stdDev = Math.sqrt((nValues * sumSqrs - sum * sum)/ (nValues * (nValues - 1)));
				result.get(k).put(i, average);
				result.get(k + STD_DEV).put(i, stdDev);
				currentPoint++;
				monitor.setPercentCompleted((int)Math.round((double)currentPoint / nPoints * 100));
			}
		}
		
		
		return new SimpleLevelResult(result);
	}*/
	
	
	public SimpleLevelResult overlay(Vector<SimpleLevelResult> results) throws Exception {
		if (results.isEmpty()) throw new Exception("Empty result set");
		Map<String, SortedMap<Double, Double>> result = new HashMap<String, SortedMap<Double, Double>>();
		Set<String> reactantIds = results.firstElement().getReactantIds();
		
		//All the time series are already there: we simply need to rename them
		for (String k : reactantIds) {
			for (int i=0;i<results.size();i++) {
				result.put(k + OVERLAY_NAME + i, results.get(i).levels.get(k)); //new TreeMap<Double, Double>());
			}
		}
		
		int maxNLevels = -1;
		
		for (SimpleLevelResult l : results) {
			if (l.getNumberOfLevels() > maxNLevels) {
				maxNLevels = l.getNumberOfLevels();
			}
		}
		
		/*double finalTime = results.firstElement().getTimeIndices().get(results.firstElement().getTimeIndices().size()-1);
		int avgSize = 0;
		for (SimpleLevelResult l : results) {
			avgSize += l.getTimeIndices().size();
		}
		avgSize = (int)Math.round(1.0 * avgSize / results.size());
		double increment = finalTime / avgSize;
		if (monitor != null) {
			monitor.setStatus("Computing overlays");
			monitor.setPercentCompleted(0);
		}
		for (double i=0;i<=finalTime;i+=increment) {
			for (String k : reactantIds) {
				for (int j=0;j<results.size();j++) {
					SimpleLevelResult l = results.get(j);
					double val = l.getConcentration(k, i);
					result.get(k + OVERLAY_NAME + j).put(i, val);
				}
			}
			monitor.setPercentCompleted((int)((double)i / finalTime * 100));
		}*/
		
		return new SimpleLevelResult(maxNLevels, result);
	}
}
