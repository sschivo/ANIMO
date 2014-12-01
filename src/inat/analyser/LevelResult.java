/**
 * 
 */
package inat.analyser;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * The concentrations result contains information about the analysis of the
 * activation levels of each substrate in a model.
 * 
 * @author B. Wanders
 */
public abstract class LevelResult {
	
	/**
	 * This method retrieves the level of activation for the given substrate
	 * 
	 * @param id the id of the substrate
	 * @param time the time index to do a look up for
	 * @return the level of concentration
	 */
	public abstract double getConcentration(String id, double time);

	/**
	 * This method retrieves the level of activation for the given reactant, or null if that reactant has not a value for the given instant
	 * 
	 * @param id the id of the reactant
	 * @param time the time index to do a look up for
	 * @return the level of concentration
	 */
	public abstract Double getConcentrationIfAvailable(String id, double time);
	
	/**
	 * If the concentration is available, return its value. Otherwise,
	 * use (linear) interpolation to find a value between the two nearest ones
	 * @param id
	 * @param time
	 * @return
	 */
	public abstract double getInterpolatedConcentration(String id, double time);

	/**
	 * Determines the reactant ID's of substrates of which result are known.
	 * 
	 * @return a set of IDs
	 */
	public abstract Set<String> getReactantIds();

	/**
	 * Returns a list of all time indices at which we have a real data point.
	 * 
	 * @return the list of data point time indices
	 */
	public abstract List<Double> getTimeIndices();
	
	public abstract boolean isEmpty();
	
	/**
	 * Returns a new LevelResult where only the series whose names are in
	 * the given Vector are present. All other series are discarded.
	 * @param acceptedNames
	 * @return
	 */
	public abstract LevelResult filter(Vector<String> acceptedNames);
	
	/**
	 * Returns a new LevelResult containing only the series from this
	 * LevelResult that are also present in "with".
	 * The values of those series will be the difference between the
	 * value in "subtractFrom" and the value in this.
	 * @param subtractFrom
	 * @param myMapModelIDtoCytoscapeID The connection between the IDs used in the
	 * model (and result series)
	 * @param hisMapCytoscapeIDtoModelID The same thing, but to go in subtractFrom
	 * from the original Cytoscape id to the corresponding model id
	 * @return
	 */
	public abstract LevelResult difference(LevelResult subtractFrom, Map<String, String> myMapModelIDtoCytoscapeID, Map<String, String> hisMapCytoscapeIDtoModelID);
}
