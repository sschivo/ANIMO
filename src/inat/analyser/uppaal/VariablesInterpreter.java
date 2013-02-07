package inat.analyser.uppaal;

import inat.analyser.AnalysisException;
import inat.analyser.LevelResult;
import inat.model.Model;
import inat.model.Reactant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interprets the traces produced by a {@link VariablesModel}.
 * 
 * @author Brend Wanders
 * 
 */
public class VariablesInterpreter implements ResultInterpreter<LevelResult> {

	@Override
	public LevelResult analyse(Model m, String output) throws AnalysisException {
		try {
			Map<String, SortedMap<Double, Double>> levels = new HashMap<String, SortedMap<Double, Double>>();

			BufferedReader br = new BufferedReader(new StringReader(output));
			String line = null;
			Pattern globalTimePattern = Pattern.compile("t\\(0\\)-globalTime<[=]?[-]?[0-9]+");
			Pattern statePattern = Pattern.compile("[A-Za-z0-9_]+[' ']*[=][' ']*[0-9]+");
			int time = 0;

			while ((line = br.readLine()) != null) {
				if (!line.startsWith("State"))
					continue;
				Matcher stateMatcher = statePattern.matcher(line);
				String s = null;
				while (stateMatcher.find()) {
					s = stateMatcher.group();
					if (s.contains("_nonofficial") || s.contains("counter"))
						continue;
					String reactantId = null;
					if (s.indexOf(' ') < s.indexOf('=')) {
						reactantId = s.substring(0, s.indexOf(' '));
					} else {
						reactantId = s.substring(0, s.indexOf('='));
					}
					// put the reactant into the result map
					levels.put(reactantId, new TreeMap<Double, Double>());
				}
				break;
			}

			// add initial concentrations
			for (Reactant r : m.getReactantCollection()) {
				if (levels.containsKey(r.getId())) {
					levels.get(r.getId()).put(0.0, (double)r.get("initialConcentration").as(Integer.class));
				}
			}

			String oldLine = null;
			while ((line = br.readLine()) != null) {
				if (!line.startsWith("State"))
					continue;
				Matcher timeMatcher = globalTimePattern.matcher(line);
				if (oldLine == null)
					oldLine = line;
				if (timeMatcher.find()) {
					String value = (timeMatcher.group().split("<")[1]);
					int newTime = -1;
					if (value.substring(0, 1).equals("=")) {
						if (value.substring(1, 2).equals("-")) {
							newTime = Integer.parseInt(value.substring(2, value.length()));
						} else {
							newTime = Integer.parseInt(value.substring(1, value.length()));
						}
					} else {
						if (value.substring(0, 1).equals("-")) {
							newTime = Integer.parseInt(value.substring(1, value.length())) + 1;
						} else {
							newTime = Integer.parseInt(value.substring(0, value.length())) + 1;
						}
					}
					if (time < newTime) {
						time = newTime;
						// we now know the time
						Matcher stateMatcher = statePattern.matcher(oldLine);
						String s = null;
						while (stateMatcher.find()) {
							s = stateMatcher.group();
							if (s.contains("_nonofficial") || s.contains("counter"))
								continue;
							String reactantId = null;
							if (s.indexOf(' ') < s.indexOf('=')) {
								reactantId = s.substring(0, s.indexOf(' '));
							} else {
								reactantId = s.substring(0, s.indexOf('='));
							}
							// we can determine the level of activation
							int level = Integer.valueOf(s.substring(s.indexOf("=") + 1).trim());
							levels.get(reactantId).put((double)time, (double)level);
						}
						oldLine = line;
					} else if (newTime == time) {
						oldLine = line;
					}
				} else {
					throw new AnalysisException("New state without globalTime. Offending line: '" + line + "'");
				}
			}

			return new SimpleLevelResult(m.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class), levels);
		} catch (IOException e) {
			// This should never happen (assert and throw)
			assert false : "Assumption failed: IOException was thrown while reading from a String.";
			throw new AnalysisException("Assumption failed: IOException was thrown while reading from a String.", e);
		}
	}

}
