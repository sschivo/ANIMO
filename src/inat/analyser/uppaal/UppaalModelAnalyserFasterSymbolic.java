package inat.analyser.uppaal;


import inat.InatBackend;
import inat.analyser.AnalysisException;
import inat.analyser.LevelResult;
import inat.analyser.ModelAnalyser;
import inat.analyser.SMCResult;
import inat.cytoscape.RunAction;
import inat.model.Model;
import inat.model.Property;
import inat.model.Reactant;
import inat.util.XmlConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import cytoscape.Cytoscape;
import cytoscape.task.TaskMonitor;

/**
 * This class is currently used for NO queries.
 * Computes the requested analysis on the given ANIMO model, translating it into
 * the corresponding UPPAAL model depending on which query is asked.
 */
public class UppaalModelAnalyserFasterSymbolic implements ModelAnalyser<LevelResult> {
	
	public static double TIME_SCALE = 0.2; //the factor by which time values are mutiplied before being output on the .csv file (it answers the question "how many real-life minutes does a time unit of the model represent?")

	private String verifytaPath, verifytaSMCPath; //The paths to the tools used in the analysis
	private TaskMonitor monitor; //The reference to the Monitor in which to show the progress of the task
	private RunAction runAction; //We can ask this one whether the user has asked us to cancel the computation
	private int taskStatus = 0; //Used to define the current status of the analysis task. 0 = still running, 1 = process completed, 2 = user pressed Cancel
	
	public UppaalModelAnalyserFasterSymbolic(TaskMonitor monitor, RunAction runAction) {
		XmlConfiguration configuration = InatBackend.get().configuration();

		this.monitor = monitor;
		this.runAction = runAction;
		this.verifytaPath = configuration.get(XmlConfiguration.VERIFY_KEY);
		this.verifytaSMCPath = configuration.get(XmlConfiguration.VERIFY_SMC_KEY);
	}
	
	public static boolean areWeUnderWindows() {
		if (System.getProperty("os.name").startsWith("Windows")) return true;
		return false;
	}
	
	/**
	 * Returns the SMCResult that we obtain from analysing with UPPAAL the given model
	 * with the given probabilistic query
	 * @param m The model to analyse
	 * @param probabilisticQuery The probabilistic query already translated with correct UPPAAL time
	 * units and reactant names (ex. Pr[<=12000](<> reactant0 > 40), and not Pr[<=240](<> MK2 > 40), which
	 * is instead what the user inserts. The translation is made in RunAction.performSMCAnalysis)
	 * @return The parsed SMCResult containing the response given by UPPAAL (be it boolean or numerical)
	 * @throws AnalysisException
	 */
	public SMCResult analyzeSMC(Model m, String probabilisticQuery) throws AnalysisException {
		SMCResult result = null;
		try {
			final String uppaalModel = new VariablesModelSMC().transform(m);
			
			File modelFile = File.createTempFile("ANIMO", ".xml");
			final String prefix = modelFile.getAbsolutePath().replace(".xml", "");
			File queryFile = new File(prefix + ".q");

			// write out strings to file
			FileWriter modelFileOut = new FileWriter(modelFile);
			modelFileOut.append(uppaalModel);
			modelFileOut.close();
			modelFile.deleteOnExit();
			
			FileWriter queryFileOut = new FileWriter(queryFile);
			queryFileOut.append(probabilisticQuery);
			queryFileOut.close();
			queryFile.deleteOnExit();
	
			String nomeFileModello = modelFile.getAbsolutePath(),
				   nomeFileQuery = queryFile.getAbsolutePath(),
				   nomeFileOutput = nomeFileModello.substring(0, nomeFileModello.indexOf(".")) + ".output";
						
			String[] cmd = new String[3];
			
			if (areWeUnderWindows()) {
				if (!new File(verifytaSMCPath).exists()) {
					throw new FileNotFoundException("Cannot locate verifyta executable! (tried in " + verifytaSMCPath + ")");
				}
				cmd[0] = "cmd";
				cmd[1] = "/c";
				cmd[2] = " \"" + verifytaSMCPath + "\"";
			} else {
				if (!new File(verifytaSMCPath).exists()) {
					throw new FileNotFoundException("Cannot locate verifyta executable! (tried in " + verifytaSMCPath + ")");
				}
				cmd[0] = "bash";
				cmd[1] = "-c";
				cmd[2] = verifytaSMCPath;				
			}
			cmd[2] += " \"" + nomeFileModello + "\" \"" + nomeFileQuery + "\" > \"" + nomeFileOutput + "\"";
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(cmd);
			try {
				proc.waitFor();
			} catch (InterruptedException ex){
				proc.destroy();
				throw new Exception("Interrupted (1)");
			}
			if (proc.exitValue() != 0) {
				StringBuilder errorBuilder = new StringBuilder();
				errorBuilder.append("[" + nomeFileModello + "] Verify result: " + proc.exitValue() + "\n");
				BufferedReader br = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
				String line = null;
				while ((line = br.readLine()) != null) {
					errorBuilder.append(line + "\n");
				}
				errorBuilder.append(" (current directory: " + new File(".").getAbsolutePath() + ")\n");
				throw new Exception(errorBuilder.toString());
			}
			//N B: it is responsibility of the caller to close all streams when the process is done!!!
			proc.getErrorStream().close();
			proc.getInputStream().close();
			proc.getOutputStream().close();
			
			result = new UppaalModelAnalyserFasterSymbolic.VariablesInterpreter(monitor).analyseSMC(m, new FileInputStream(nomeFileOutput));
			
			new File(nomeFileOutput).delete();
			
		} catch (Exception e) {
			throw new AnalysisException("Error during analysis", e);
		}
		
		if (result == null) {
			throw new AnalysisException("Error during analysis: empty result");
		}
		
		return result;
	}
	
	/**
	 * Perform a simple simulation run on the given model, up to the given time.
	 * Please notice that, differently from UppaalModelAnalyserFasterConcrete,
	 * we use here the VariablesModel class to transform the model into the UPPAAL input.
	 * This type of model can only produce symbolic traces, because it contains priorities.
	 * @param m The model to analyse
	 * @param timeTo the length of the simulation, in UPPAAL time units. The translation from
	 * real-life minutes to UPPAAL time units is made in RunAction.performNormalAnalysis.
	 * @return The SimpleLevelResult showing as series the activity levels of all reactants
	 * present in the model during the simulation period
	 */
	public LevelResult analyze(final Model m, final int timeTo) throws AnalysisException {
		LevelResult result = null;
		try {
			final String uppaalModel = new VariablesModel().transform(m);
			final String uppaalQuery = "E<> (globalTime > " + timeTo + ")";
			
			File modelFile = File.createTempFile("ANIMO", ".xml");
			final String prefix = modelFile.getAbsolutePath().replace(".xml", "");
			File queryFile = new File(prefix + ".q");
	
			// write out strings to file
			FileWriter modelFileOut = new FileWriter(modelFile);
			modelFileOut.append(uppaalModel);
			modelFileOut.close();
			modelFile.deleteOnExit();
			
			FileWriter queryFileOut = new FileWriter(queryFile);
			queryFileOut.append(uppaalQuery);
			queryFileOut.close();
			queryFile.deleteOnExit();
	
			String nomeFileModello = modelFile.getAbsolutePath(),
				   nomeFileQuery = queryFile.getAbsolutePath();
			
			
			String[] cmd = new String[3];
			
			if (areWeUnderWindows()) {
				if (!new File(verifytaPath).exists()) {
					throw new FileNotFoundException("Cannot locate verifyta executable! (tried in " + verifytaPath + ")");
				}
				cmd[0] = "cmd";
				cmd[1] = "/c";
				cmd[2] = " \"" + verifytaPath + "\"";
			} else {
				if (!new File(verifytaPath).exists()) {
					throw new FileNotFoundException("Cannot locate verifyta executable! (tried in " + verifytaPath + ")");
				}
				cmd[0] = "bash";
				cmd[1] = "-c";
				cmd[2] = verifytaPath;				
			}
			cmd[2] += " -t0 -o2 -y \"" + nomeFileModello + "\" \"" + nomeFileQuery + "\"";
			Runtime rt = Runtime.getRuntime();
			if (monitor != null) {
				monitor.setStatus("Analyzing model with UPPAAL.");
			}
			System.err.print("\tUPPAAL analysis of " + nomeFileModello);
			final Process proc = rt.exec(cmd);
			final Vector<LevelResult> resultVector = new Vector<LevelResult>(1); //this has no other reason than to hack around the fact that an internal class needs to have all variables it uses declared as final
			final Vector<Exception> errors = new Vector<Exception>(); //same reason as above
			new Thread() {
				@Override
				public void run() {
					try {
						resultVector.add(new UppaalModelAnalyserFasterSymbolic.VariablesInterpreter(monitor).analyse(m, proc.getErrorStream(), timeTo));
					} catch (Exception e) {
						errors.add(e);
					}
				}
			}.start();
			if (runAction != null) {
				taskStatus = 0;
				new Thread() { //wait for the process to end correctly
					@Override
					public void run() {
						try {
							proc.waitFor();
						} catch (InterruptedException ex) {
							taskStatus = 2;
						}
						taskStatus = 1;
					}
				}.start();
				new Thread() { //wait for the process to end by user cancellation
					@Override
					public void run() {
						while (taskStatus == 0) {
							if (runAction.needToStop()) {
								taskStatus = 2;
								return;
							}
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								
							}
						}
					}
				}.start();
				while (taskStatus == 0) {
					Thread.sleep(100);
				}
				if (taskStatus == 2) {
					throw new AnalysisException("User interrupted");
				}
				while (resultVector.isEmpty()) { //if the verifyta process is completed, we may still need to wait for the analysis thread to complete
					Thread.sleep(100);
				}
			} else {
				try {
					proc.waitFor();
				} catch (InterruptedException ex){
					proc.destroy();
					throw new Exception("Interrupted (1)");
				}
			}
			if (!errors.isEmpty()) {
				Exception ex = errors.firstElement();
				throw new AnalysisException("Error during analysis", ex);
			}
			result = resultVector.firstElement();
			if (proc.exitValue() != 0) {
				StringBuilder errorBuilder = new StringBuilder();
				errorBuilder.append("[" + nomeFileModello + "] Verify result: " + proc.exitValue() + "\n");
				BufferedReader br = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
				String line = null;
				while ((line = br.readLine()) != null) {
					errorBuilder.append(line + "\n");
				}
				errorBuilder.append(" (current directory: " + new File(".").getAbsolutePath() + ")\n");
				throw new Exception(errorBuilder.toString());
			}
			//N B: it is responsibility of the caller to close all streams when the process is done!!!
			proc.getErrorStream().close();
			proc.getInputStream().close();
			proc.getOutputStream().close();
			
		} catch (Exception e) {
			throw new AnalysisException("Error during analysis", e);
		}
		
		if (result == null || result.isEmpty()) {
			throw new AnalysisException("Error during analysis: empty result");
		}
		
		return result;
	}
	
	
	
	//This is slightly different from the "official" one in the sense that it reads data directly from the input stream. This way, we don't have to read the whole stream to a string (with the consequent waste of memory) before giving an input to the interpreter
	public class VariablesInterpreter {

		private static final String INITIAL_LEVEL = Model.Properties.INITIAL_LEVEL;
		private static final String ALIAS = Model.Properties.ALIAS;
		private static final String NUMBER_OF_LEVELS = Model.Properties.NUMBER_OF_LEVELS;
		private TaskMonitor monitor = null;
		
		public VariablesInterpreter(TaskMonitor monitor) {
			this.monitor = monitor;
		}
		
		/**
		 * Analyse the UPPAAL output from a Statistical Model Checking query
		 * @param m The model on which the result is based 
		 * @param smcOutput The stream from which to read the UPPAAL output
		 * @return The parsed SMCResult containing the boolean/numerical query answer
		 * @throws Exception
		 */
		public SMCResult analyseSMC(Model m, InputStream smcOutput) throws Exception {
			BufferedReader br = new BufferedReader(new InputStreamReader(smcOutput));
			SMCResult res;
			
			String line = null;
			String objective = "-- Property is";
			
			try {
				while ((line = br.readLine()) != null) {
					if (!line.contains(objective)) {
						continue;
					}
					if (line.contains("NOT")) {
						res = new SMCResult(false, findConfidence(line, br));
						br.close();
						return res;
					} else {
						line = br.readLine();
						if ((line.indexOf("runs) H") != -1) || (line.indexOf("runs) Pr(..)/Pr(..)") != -1)) { //it has boolean result
							res = new SMCResult(true, findConfidence(line, br));
							br.close();
							return res;
						} else { //the result is between lower and upper bound
							String lowerBoundS = line.substring(line.indexOf("[") + 1, line.lastIndexOf(",")),
								   upperBoundS = line.substring(line.lastIndexOf(",") + 1, line.lastIndexOf("]"));
							double lowerBound, upperBound;
							try {
								lowerBound = Double.parseDouble(lowerBoundS);
								upperBound = Double.parseDouble(upperBoundS);
							} catch (Exception ex) {
								br.close();
								throw new Exception("Unable to understand probability bounds for the result \"" + line + "\"");
							}
							res = new SMCResult(lowerBound, upperBound, findConfidence(line, br));
							br.close();
							return res;
						}
					}
				}
			} catch (Exception ex) {
				throw new Exception("Unable to understand UPPAAL SMC output: " + readTheRest(line, br), ex);
			}

			throw new Exception("Unable to understand UPPAAL SMC output: " + readTheRest(line, br));
		}
		
		/**
		 * Used when reporting an error about the SMC query answer.
		 * As UPPAAL SMC is still in beta stage, we do our best to understand its output,
		 * but if we fail we try at least to speed up the process of changing the parsing
		 * according to a possibly new output format.
		 * @param line The entire line which caused the problem
		 * @param br The buffered reader from which to continue to read the rest of the input
		 * @return A string containing the rest of the input
		 * @throws Exception
		 */
		private String readTheRest(String line, BufferedReader br) throws Exception {
			StringBuilder content = new StringBuilder();
			String endLine = System.getProperty("line.separator");
			content.append(line + endLine);
			while ((line = br.readLine()) != null) {
				content.append(line + endLine);
			}
			return content.toString();
		}
		
		/**
		 * Find the confidence value given in the UPPAAL SMC query result
		 * @param currentLine The line from which to start looking for a confidence value
		 * @param br The reader from which to continue reading UPPAAL output
		 * @return The confidence value
		 * @throws Exception
		 */
		private double findConfidence(String currentLine, BufferedReader br) throws Exception {
			double confidence = 0;
			boolean weHaveAProblem = false;
			String objective = "with confidence ";
			String line = currentLine;
			StringBuilder savedOutput = new StringBuilder();
			String endLine = System.getProperty("line.separator");
			savedOutput.append(currentLine + endLine);
			if (!line.contains(objective)) {
				while ((line = br.readLine()) != null) {
					savedOutput.append(line + endLine);
					if (line.contains(objective)) {
						break;
					} else if (line.startsWith("State")) { //This actually means that UPPAAL has found a problem in our model. I do my best to report the error to the user.
						weHaveAProblem = true;
					}
				}
			}
			
			if (weHaveAProblem) {
				String errorMsg = savedOutput.toString();
				if (errorMsg.length() > 200) {
					errorMsg = errorMsg.substring(0, 100) + endLine + " [...] " + endLine + errorMsg.substring(errorMsg.length() - 100);
				}
				JOptionPane.showMessageDialog(Cytoscape.getDesktop(), errorMsg, "UPPAAL SMC Exception", JOptionPane.ERROR_MESSAGE);
			}
			if (line == null) {
				throw new Exception("Unable to understand UPPAAL SMC output: " + savedOutput);
			}
			if (line.endsWith(".")) {
				line = line.substring(0, line.length() - 1);
			}
			try {
				confidence = Double.parseDouble(line.substring(line.indexOf(objective) + objective.length()));
			} catch (Exception ex) {
				confidence = -1;
			}
			
			return confidence;
		}

		/**
		 * Parse the UPPAAL output containing a trace run on the given model until the given time
		 * @param m The model on which the trace is based
		 * @param output The stream from which to read the trace
		 * @param timeTo The time up to which the simulation trace arrives (or should arrive)
		 * @return The SimpleLevelResult containing a series for each of the reactants in the model,
		 * showing the activity levels of that reactant for each time point of the trace.
		 * @throws Exception
		 */
		public LevelResult analyse(Model m, InputStream output, int timeTo) throws Exception {
			long startTime = System.currentTimeMillis();
			
			Map<String, SortedMap<Double, Double>> levels = new HashMap<String, SortedMap<Double, Double>>();

			BufferedReader br = new BufferedReader(new InputStreamReader(output));
			String line = null;
			Pattern globalTimePattern = Pattern.compile("globalTime>[=]?[-]?[0-9]+");
			Pattern statePattern = Pattern.compile("[A-Za-z0-9_]+[' ']*[=][' ']*[0-9]+");
			int time = 0;
			int maxNumberOfLevels = m.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
			HashMap<String, Double> numberOfLevels = new HashMap<String, Double>();
			
			while ((line = br.readLine()) != null) {
				if (!line.startsWith("State")) {
					continue;
				}
				line = br.readLine(); //after "State" there is a \n
				line = br.readLine(); //.. then a very useful empty line..
				line = br.readLine(); //the next line lists the states of automata: we need the 3rd line, with variable&clock values
				if (line == null) throw new Exception("Empty result");
				Matcher stateMatcher = statePattern.matcher(line);
				String s = null;
				while (stateMatcher.find()) {
					s = stateMatcher.group();
					if (s.contains("_nonofficial") || s.contains("counter") || s.contains("metro"))
						continue;
					String reactantId = null;
					if (s.indexOf(' ') >= 0 && s.indexOf(' ') < s.indexOf('=')) {
						reactantId = s.substring(0, s.indexOf(' '));
					} else {
						reactantId = s.substring(0, s.indexOf('='));
					}
					if (reactantId.equals("r") 
						//|| reactantId.equals("r1") || reactantId.equals("r2")
						) continue; //private variables are not taken into account
					// put the reactant into the result map
					levels.put(reactantId, new TreeMap<Double, Double>());
				}
				break;
			}

			// add initial concentrations and get number of levels
			for (Reactant r : m.getReactantCollection()) {
				Integer nLvl = r.get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
				if (nLvl == null) {
					Property nameO = r.get(Model.Properties.ALIAS);
					String name;
					if (nameO == null) {
						name = r.getId();
					} else {
						name = nameO.as(String.class);
					}
					String inputLevels = JOptionPane.showInputDialog("Missing number of levels for reactant \"" + name + "\" (" + r.getId() + ").\nPlease insert the max number of levels for \"" + name + "\"", maxNumberOfLevels);
					if (inputLevels != null) {
						try {
							nLvl = new Integer(inputLevels);
						} catch (Exception ex) {
							nLvl = maxNumberOfLevels;
						}
					} else {
						nLvl = maxNumberOfLevels;
					}
				}
				numberOfLevels.put(r.getId(), (double)nLvl);
				
				if (levels.containsKey(r.getId())) {
					double initialLevel = r.get(Model.Properties.INITIAL_LEVEL).as(Integer.class);
					initialLevel = initialLevel / (double)nLvl * (double)maxNumberOfLevels; //of course, the initial "concentration" itself needs to be rescaled correctly
					levels.get(r.getId()).put(0.0, initialLevel);
				}
			}
			
			long endTime = System.currentTimeMillis();
			System.err.println(" took " + RunAction.timeDifferenceFormat(startTime, endTime));
			startTime = System.currentTimeMillis();
			
			if (monitor != null) {
				monitor.setStatus("Analyzing UPPAAL output trace.");
			}

			String oldLine = null;
			while ((line = br.readLine()) != null) {
				while (line != null && !line.contains("update?")) { //wait until the next time we update all the official variables
					line = br.readLine();
				}
				while (line != null && line.contains("update?")) {//the lines with the "update?" are many, as the signal is sent on a broadcast channel.
					line = br.readLine(); 
				}
				br.readLine(); //riga vuota
				br.readLine(); //riga vuota
				br.readLine(); //riga vuota
				line = br.readLine();
				if (line == null) break;
				
				Matcher timeMatcher = globalTimePattern.matcher(line);
				//System.err.println(line);
				if (oldLine == null)
					oldLine = line;
				if (timeMatcher.find()) {
					String value = (timeMatcher.group().split(">")[1]);
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
							if (s.contains("_nonofficial") || s.contains("counter") || s.contains("metro"))
								continue;
							String reactantId = null;
							if (s.indexOf(' ') >= 0 && s.indexOf(' ') < s.indexOf('=')) {
								reactantId = s.substring(0, s.indexOf(' '));
							} else {
								reactantId = s.substring(0, s.indexOf('='));
							}
							if (reactantId.equals("r") 
								//|| reactantId.equals("r1") || reactantId.equals("r2") 
								|| numberOfLevels.get(reactantId) == null) continue; //we check whether it is a private variable
							// we can determine the level of activation
							int level = Integer.valueOf(s.substring(s.indexOf("=") + 1).trim());
							if (numberOfLevels.get(reactantId) != maxNumberOfLevels) {
								level = (int)(level / (double)numberOfLevels.get(reactantId) * (double)maxNumberOfLevels);
							}
							
							SortedMap<Double, Double> rMap = levels.get(reactantId);
							if (rMap.get(rMap.lastKey()) != level) {
								if (rMap.lastKey() < time - 1) { //We use this piece to explicitly keep a level constant when it is not varying (i.e., the graph will never contain non-vertical,non-horizontal lines)
									rMap.put((double)(time - 1), rMap.get(rMap.lastKey()));
								}
								
								rMap.put((double)time, (double)level);
							}
							
						}
						oldLine = line;
					} else if (newTime == time) {
						oldLine = line;
					}
				} else {
					throw new AnalysisException("New state without globalTime. Offending line: \"" + line + "\"");
				}
			}
			if (time < timeTo) { //if the state of the system remains unchanged from a certain time on (and so UPPAAL terminates on that point), but we asked for a later time, we add a final point where all data remain unchanged, so that the user can see the "evolution" up to the requested point
				for (String reactantName : levels.keySet()) {
					SortedMap<Double, Double> values = levels.get(reactantName);
					double lastValue = values.get(values.lastKey());
					values.put((double)timeTo, lastValue);
				}
			}
			
			endTime = System.currentTimeMillis();
			System.err.println("\tParsing the result produced by UPPAAL took " + RunAction.timeDifferenceFormat(startTime, endTime));
			
			return new SimpleLevelResult(maxNumberOfLevels, levels);
		}
		
		public LevelResult analyseNotWorking(Model m, InputStream output, int timeTo) throws Exception {
			long startTime = System.currentTimeMillis();
			
			Map<String, SortedMap<Double, Double>> levels = new HashMap<String, SortedMap<Double, Double>>();

			BufferedReader br = new BufferedReader(new InputStreamReader(output));
			String line = null, lastLine = null;
			Pattern globalTimePattern = Pattern.compile("globalTime>[=][-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?");  //Pattern.compile("globalTime[=][0-9]+"); //The new pattern supports also numbers like 3.434252e+06, which can occur(!!)
			Pattern statePattern = Pattern.compile("[A-Za-z0-9_]+[' ']*[=][' ']*[0-9]+");
			int time = 0, lastTime = 0;
			int maxNumberOfLevels = m.getProperties().get(NUMBER_OF_LEVELS).as(Integer.class);
			HashMap<String, Double> numberOfLevels = new HashMap<String, Double>();
			
			while ((line = br.readLine()) != null) {
				if (!line.startsWith("State"))
					continue;
				line = br.readLine(); //the "State:" string has a \n at the end, so we need to read the next line
				line = br.readLine(); //the second line contains informations about which we don't care. We want variable values
				Matcher stateMatcher = statePattern.matcher(line);
				String s = null;
				while (stateMatcher.find()) {
					s = stateMatcher.group();
					if (s.contains("_nonofficial") || s.contains("counter") || s.contains("metro"))
						continue;
					String reactantId = null;
					if (s.indexOf(' ') >= 0 && s.indexOf(' ') < s.indexOf('=')) {
						reactantId = s.substring(0, s.indexOf(' '));
					} else {
						reactantId = s.substring(0, s.indexOf('='));
					}
					if (reactantId.equals("c") || reactantId.equals("globalTime") || reactantId.equals("r")
						|| reactantId.startsWith("input_reactant_") || reactantId.startsWith("output_reactant_") //private variables are not taken into account
						|| reactantId.equals("r1") || reactantId.equals("r2")
						|| reactantId.endsWith("_sem")) continue; //Nor do we need to count semaphores
					// put the reactant into the result map
					levels.put(reactantId, new TreeMap<Double, Double>());
				}
				break;
			}
			
			// add initial concentrations and get number of levels
			for (Reactant r : m.getReactantCollection()) {
				Integer nLvl = r.get(NUMBER_OF_LEVELS).as(Integer.class);
				if (nLvl == null) {
					Property nameO = r.get(ALIAS);
					String name;
					if (nameO == null) {
						name = r.getId();
					} else {
						name = nameO.as(String.class);
					}
					String inputLevels = JOptionPane.showInputDialog("Missing number of levels for reactant \"" + name + "\" (" + r.getId() + ").\nPlease insert the max number of levels for \"" + name + "\"", maxNumberOfLevels);
					if (inputLevels != null) {
						try {
							nLvl = new Integer(inputLevels);
						} catch (Exception ex) {
							nLvl = maxNumberOfLevels;
						}
					} else {
						nLvl = maxNumberOfLevels;
					}
				}
				numberOfLevels.put(r.getId(), (double)nLvl);
				
				if (levels.containsKey(r.getId())) {
					double initialLevel = r.get(INITIAL_LEVEL).as(Integer.class);
					initialLevel = initialLevel / (double)nLvl * (double)maxNumberOfLevels; //of course, the initial "concentration" itself needs to be rescaled correctly
					levels.get(r.getId()).put(0.0, initialLevel);
				}
			}
			
			long endTime = System.currentTimeMillis();
			System.err.println(" took " + RunAction.timeDifferenceFormat(startTime, endTime));
			startTime = System.currentTimeMillis();
			
			if (monitor != null) {
				monitor.setStatus("Analyzing UPPAAL output trace.");
			}
			
			lastTime = time;
			lastLine = line;
			while ((line = br.readLine()) != null) {
				/*while (line != null && !line.contains("inform_reacting")) {
					line = br.readLine();
				}
				while ((line = br.readLine()) != null) {
					if (line.startsWith("State")) break;
				}
				if (line == null) break;*/
				if (!line.startsWith("State")) continue;
				br.readLine(); //as said before, the "State:" string ends with \n, so we need to read the next line in order to get the actual state data
				line = br.readLine(); //and the line after that contains only the states of the processes, while we are interested in variable values, which are in the 3rd line
				Matcher timeMatcher = globalTimePattern.matcher(line);
				if (timeMatcher.find()) {
					String value = (timeMatcher.group().split("=")[1]);
					int newTime = -1;
					if (value.substring(0, 1).equals("=")) {
						if (value.substring(1, 2).equals("-")) {
							newTime = (int)Math.round(Double.parseDouble(value.substring(2, value.length())));
						} else {
							newTime = (int)Math.round(Double.parseDouble(value.substring(1, value.length())));
						}
					} else {
						if (value.substring(0, 1).equals("-")) {
							newTime = (int)(Math.round(Double.parseDouble(value.substring(1, value.length())))); //why +1??
						} else {
							newTime = (int)(Math.round(Double.parseDouble(value.substring(0, value.length())))); //why +1??
						}
					}
					
					if (lastTime == time) {
						if (newTime > time) {
							lastTime = newTime;
							lastLine = line;
						} else {
							//This actually cannot happen, because the first time we find that newTime > lastTime we also set time = lastTime (and we parse lastLine)
						}
					} else {
						if (newTime == lastTime) {
							//we don't need to parse now
							lastLine = line;
						} else if (newTime > lastTime) {
							time = lastTime;
							parseLine(time, lastLine, numberOfLevels, maxNumberOfLevels, statePattern, levels);
						}
					}
				} else {
					throw new AnalysisException("New state without globalTime. Offending line: \"" + line + "\"");
				}
			}
			//The parse of the latest values needs to be done when we are sure there will be no more changes (i.e. at the end)
			time = lastTime;
			parseLine(time, lastLine, numberOfLevels, maxNumberOfLevels, statePattern, levels);
			
			//if (time < timeTo) { //if the state of the system remains unchanged from a certain time on (and so UPPAAL terminates on that point), but we asked for a later time, we add a final point where all data remain unchanged, so that the user can see the "evolution" up to the requested point
			//we do it always, because there can be some situations in which reactants are not read while time increases, and thus we can reach the end of time without having an updated value for each reactant
				for (String reactantName : levels.keySet()) {
					SortedMap<Double, Double> values = levels.get(reactantName);
					double lastValue = values.get(values.lastKey());
					values.put((double)timeTo, lastValue);
				}
			//}
			
			endTime = System.currentTimeMillis();
			System.err.println("\tParsing the result produced by UPPAAL took " + RunAction.timeDifferenceFormat(startTime, endTime));
			
			return new SimpleLevelResult(maxNumberOfLevels, levels);
		}
		private void parseLine(int time, String line, Map<String, Double> numberOfLevels, int maxNumberOfLevels, Pattern statePattern, Map<String, SortedMap<Double, Double>> levels) {
			Matcher stateMatcher = statePattern.matcher(line);
			String s = null;
			while (stateMatcher.find()) {
				s = stateMatcher.group();
				if (s.contains("_nonofficial") || s.contains("counter") || s.contains("metro")) {
					continue;
				}
				String reactantId = null;
				if (s.indexOf(' ') >= 0 && s.indexOf(' ') < s.indexOf('=')) {
					reactantId = s.substring(0, s.indexOf(' '));
				} else {
					reactantId = s.substring(0, s.indexOf('='));
				}
				/*if (reactantId.equals("c") || reactantId.equals("globalTime") || reactantId.equals("r")
					|| reactantId.startsWith("input_reactant_") || reactantId.startsWith("output_reactant_")
					|| reactantId.equals("r1") || reactantId.equals("r2") || maximumValues.get(reactantId) == null) continue; //we check whether it is a private variable*/
				if (!levels.containsKey(reactantId)) continue;
				// we can determine the level of activation
				int level = Integer.valueOf(s.substring(s.indexOf("=") + 1).trim());
				if (numberOfLevels.get(reactantId) != maxNumberOfLevels) {
					level = (int)(level / (double)numberOfLevels.get(reactantId) * (double)maxNumberOfLevels);
				}
				
				SortedMap<Double, Double> rMap = levels.get(reactantId);
				if (rMap.get(rMap.lastKey()) != level) { //if we didn't register a variation, we don't plot a point
					/*if (rMap.lastKey() < time - 1) { //We use this piece to explicitly keep a level constant when it is not varying (i.e., the graph will never contain non-vertical,non-horizontal lines)
						rMap.put((double)(time - 1), rMap.get(rMap.lastKey()));
					}*/
					rMap.put((double)time, (double)level);
				}
			}
		}
	}
	
}
