package inat.analyser.uppaal;

import inat.analyser.LevelResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

/**
 * A very simple data container for the concentration/time data.
 * 
 * @author Brend Wanders
 * 
 */
public class SimpleLevelResult extends LevelResult implements Serializable {
	private static final long serialVersionUID = 5440819034905472745L;
	Map<String, SortedMap<Double, Double>> levels;
private int numberOfLevels;
	
	/**
	 * @param levels the levels to enter
	 */
	public SimpleLevelResult(int numberOfLevels, Map<String, SortedMap<Double, Double>> levels) {
		setNumberOfLevels(numberOfLevels);
		this.levels = levels;
	}
	
	public SimpleLevelResult() { //These 3 are for compatibility with java beans and xml encoding/decoding
		
	}
	
	public void setLevels(Map<String, SortedMap<Double, Double>> levels) {
		this.levels = levels;
	}
	
	public Map<String, SortedMap<Double, Double>> getLevels () {
		return this.levels;
	}
	
	public int getNumberOfLevels() {
		return numberOfLevels;
	}

	public void setNumberOfLevels(int numberOfLevels) {
		this.numberOfLevels = numberOfLevels;
	}
	
	@Override
	public double getConcentration(String id, double time) {
		assert this.levels.containsKey(id) : "Can not retrieve level for unknown identifier.";

		SortedMap<Double, Double> data = this.levels.get(id);

		// determine level at requested moment in time:
		// it is either the level set at the requested moment, or the one set
		// before that
		//assert !data.headMap(time + 1).isEmpty() : "Can not retrieve data from any moment before the start of time.";
		//int exactTime = data.headMap(time + 1).lastKey();
		double exactTime = -1;
		for (Double k : data.keySet()) {
			if (k > time) break;
			exactTime = k;
		}

		// use exact time to get value
		return data.get(exactTime);
	}
	
	@Override
	public Double getConcentrationIfAvailable(String id, double time) {
		assert this.levels.containsKey(id) : "Can not retrieve level for unknown identifier.";

		SortedMap<Double, Double> data = this.levels.get(id);
		
		return data.get(time);
	}
	

	/**
	 * Linear interpolation between the two nearest if does not find the requested time 
	 */
	@Override
	public double getInterpolatedConcentration(String id, double time) {
		Double val = this.getConcentrationIfAvailable(id, time);
		if (val == null) {
			SortedMap<Double, Double> data = this.levels.get(id);
			double lowerTime = -1, higherTime = -1;
			for (Double k : data.keySet()) {
				if (k > time) {
					higherTime = k;
					break;
				}
				lowerTime = k;
			}
			if (higherTime == -1) higherTime = lowerTime;
			double lowerVal = data.get(lowerTime),
				   higherVal = data.get(higherTime);
			return lowerVal + (higherVal - lowerVal) * (time - lowerTime) / (higherTime - lowerTime);
		} else {
			return val;
		}
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		
		b.append("Result["+this.getReactantIds()+"] ");

		for (Entry<String, SortedMap<Double, Double>> r : this.levels.entrySet()) {
			b.append(r.getKey() + ": " + r.getValue() + "\n");
		}

		return b.toString();
	}

	@Override
	public List<Double> getTimeIndices() {
		SortedSet<Double> accumulator = new TreeSet<Double>();

		for (SortedMap<Double, Double> e : this.levels.values()) {
			accumulator.addAll(e.keySet());
		}

		return new ArrayList<Double>(accumulator);
	}

	@Override
	public Set<String> getReactantIds() {
		return Collections.unmodifiableSet(this.levels.keySet());
	}

	@Override
	public boolean isEmpty() {
		return levels.isEmpty();
	}
	
	@Override
	public LevelResult filter(Vector<String> acceptedNames) {
		Map<String, SortedMap<Double, Double>> lev = new HashMap<String, SortedMap<Double, Double>>();
		for (String s : levels.keySet()) {
			//System.err.print(s);
			if (!acceptedNames.contains(s)) {
				//System.err.println(" NON lo aggiungo");
				continue;
			}
			//System.err.println(" lo aggiungo");
			SortedMap<Double, Double> m = levels.get(s);
			lev.put(s, m);
		}
		SimpleLevelResult res = new SimpleLevelResult(this.getNumberOfLevels(), lev);
		return res;
	}
	
	@Override
	public LevelResult difference(LevelResult subtractFrom_, Map<String, String> myMapModelIDtoCytoscapeID, Map<String, String> hisMapCytoscapeIDtoModel) {
		Map<String, SortedMap<Double, Double>> lev = new HashMap<String, SortedMap<Double, Double>>();
		SimpleLevelResult subtractFrom = (SimpleLevelResult)subtractFrom_;
		//System.err.println("Differenzio tra " + subtractFrom + " (" + subtractFrom.getNumberOfLevels() + " livelli) e " + this + " (" + this.getNumberOfLevels() + " livelli)");
		int maxNLevels = Math.max(subtractFrom.getNumberOfLevels(), this.getNumberOfLevels());
		List<Double> idxSub = subtractFrom.getTimeIndices(),
					 idxThis = this.getTimeIndices();
		double minDuration = Math.min(idxSub.get(idxSub.size() - 1), idxThis.get(idxThis.size() - 1));
		for (String myKey : levels.keySet()) {
			if (!myMapModelIDtoCytoscapeID.containsKey(myKey)) continue; //Skip the edge identifiers
			String myCytoID = myMapModelIDtoCytoscapeID.get(myKey);
			//System.err.println(myKey + " --> " + myCytoID + " --> " + hisMapCytoscapeIDtoModel.get(myCytoID));
			if (!hisMapCytoscapeIDtoModel.containsKey(myCytoID)) continue; //Skip the nodes that the other does not have
			String hisKey = hisMapCytoscapeIDtoModel.get(myCytoID);
			if (!subtractFrom.levels.containsKey(hisKey)) continue; //Skip the nodes the other does not have (the check before this one may have been about a disabled node, which is not present in the uppaal model. So subtractFrom.levels may not contain any value for hisKey)
			SortedMap<Double, Double> m1 = subtractFrom.levels.get(hisKey),
									  m2 = this.levels.get(myKey),
									  mRes = new TreeMap<Double, Double>();
			for (Double k : m1.keySet()) { //Take first one an then the other as a reference for the X values. Please note: this means that the resulting series will have a number of points that is about as big as the sum of the two input series, so it would not be ideal to continue to compute differences of differences of differences...
				if (k <= minDuration) {
					mRes.put(k, maxNLevels * (subtractFrom.getInterpolatedConcentration(hisKey, k) / subtractFrom.getNumberOfLevels() - this.getInterpolatedConcentration(myKey, k) / this.getNumberOfLevels()));
				}
			}
			for (Double k : m2.keySet()) {
				if (k <= minDuration) {
					mRes.put(k, maxNLevels * (subtractFrom.getInterpolatedConcentration(hisKey, k) / subtractFrom.getNumberOfLevels() - this.getInterpolatedConcentration(myKey, k) / this.getNumberOfLevels()));
				}
			}
			//System.err.println("Ho aggiunto " + mRes.size() + " punti per " + s);
			lev.put(hisKey, mRes);
		}
		SimpleLevelResult res = new SimpleLevelResult(maxNLevels, lev);
		//System.err.println("E finalmente produco il risultato con " + maxNLevels + " livelli");
		return res;
	}

}
