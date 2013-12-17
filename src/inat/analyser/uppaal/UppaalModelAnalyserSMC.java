package inat.analyser.uppaal;


import inat.InatBackend;
import inat.analyser.AnalysisException;
import inat.analyser.LevelResult;
import inat.analyser.ModelAnalyser;
import inat.analyser.SMCResult;
import inat.cytoscape.InatActionTask;
import inat.cytoscape.RunAction;
import inat.model.Model;
import inat.model.Property;
import inat.model.Reactant;
import inat.model.Reaction;
import inat.util.XmlConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import cytoscape.Cytoscape;
import cytoscape.task.TaskMonitor;

/**
 * This class is currently used for all queries.
 * Computes the requested analysis on the given ANIMO model, translating it into
 * the corresponding UPPAAL model depending on which query is asked.
 * Always uses the model produced from VariablesModelSMC, which is tailored to work
 * with UPPAAL SMC engine. As the model does not use priorities, we employ it also
 * for the generation of concrete simulation traces.
 */
public class UppaalModelAnalyserSMC implements ModelAnalyser<LevelResult> {
	
	public static double TIME_SCALE = 0.2; //the factor by which time values are mutiplied before being output on the .csv file (it answers the question "how many real-life minutes does a time unit of the model represent?")
	
	private String verifytaPath, verifytaSMCPath;//, tracerPath; //The paths to the tools used in the analysis
	private TaskMonitor monitor; //The reference to the Monitor in which to show the progress of the task
	private InatActionTask actionTask; //We can ask this one whether the user has asked us to cancel the computation
	private int taskStatus = 0; //Used to define the current status of the analysis task. 0 = still running, 1 = process completed, 2 = user pressed Cancel
	private String dot = "."; //The separator for struct field access. Opaal models currently have no support for structs, so we use "_" instead
	
	public UppaalModelAnalyserSMC(TaskMonitor monitor, InatActionTask actionTask) {
		XmlConfiguration configuration = InatBackend.get().configuration();

		this.monitor = monitor;
		this.actionTask = actionTask;
		this.verifytaPath = configuration.get(XmlConfiguration.VERIFY_KEY);
		this.verifytaSMCPath = configuration.get(XmlConfiguration.VERIFY_SMC_KEY);
	}
	
	public static boolean areWeUnderWindows() {
		if (System.getProperty("os.name").startsWith("Windows")) return true;
		return false;
	}
	
	/**
	 * Returns the SMCResult that we obtain from analysing with UPPAAL the given model
	 * with the given probabilistic query.
	 * @param m The model to analyse
	 * @param probabilisticQuery The probabilistic query already translated with correct UPPAAL time
	 * units and reactant names (ex. Pr[<=12000](<> reactant0 > 40), and not Pr[<=240](<> MK2 > 40), which
	 * is instead what the user inserts. The translation is made in RunAction.performSMCAnalysis)
	 * @return The parsed SMCResult containing the response given by UPPAAL (be it boolean or numerical)
	 * @throws AnalysisException
	 */
	public SMCResult analyzeSMC(final Model m, String probabilisticQuery) throws AnalysisException {
		SMCResult result = null;
		try {
			final VariablesModel variablesModel;
			XmlConfiguration configuration = InatBackend.get().configuration();
			String modelType = configuration.get(XmlConfiguration.MODEL_TYPE_KEY, null);
			if (modelType == null || modelType.equals(XmlConfiguration.MODEL_TYPE_REACTANT_CENTERED)) {
				variablesModel = new VariablesModelReactantCentered(); //Reactant-centered model
			} else if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTION_CENTERED_TABLES)) {
				variablesModel = new VariablesModelReactionCenteredTables(); //Reaction-centered with tables
			} else if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTION_CENTERED)) {
				variablesModel = new VariablesModelReactionCentered(); //Reaction-centered model
			} else if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTANT_CENTERED_OPAAL)) {
				variablesModel = new VariablesModelOpaal();
				dot = "_";
			} else {
				variablesModel = new VariablesModelReactantCentered(); //Reactant-centered model is the default
			}
			final String uppaalModel = variablesModel.transform(m);
			
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
				   nomeFileOutput = nomeFileModello.substring(0, nomeFileModello.lastIndexOf(".")) + ".output";
			File fileOutput = new File(nomeFileOutput);
			fileOutput.deleteOnExit();
			
			//the following string is used in order to make sure that the name of .xtr output files is unique even when we are called by an application which is multi-threaded itself (it supposes of course that the input file is unique =))
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
			if (m.getProperties().get(Model.Properties.MODEL_CHECKING_TYPE) != null && m.getProperties().get(Model.Properties.MODEL_CHECKING_TYPE).as(Integer.class) == Model.Properties.NORMAL_MODEL_CHECKING) {
				cmd[2] += " -s -y -o2 -t0 \"" + nomeFileModello + "\" \"" + nomeFileQuery + "\"";// > \"" + nomeFileOutput + "\"";//2>&1";
			} else {
				cmd[2] += " -s \"" + nomeFileModello + "\" \"" + nomeFileQuery + "\"";
			}
			ProcessBuilder pb = new ProcessBuilder(cmd[0], cmd[1], cmd[2]);
			pb.redirectErrorStream(true);
			if (monitor != null) {
				monitor.setStatus("Analyzing model with UPPAAL.");
			}
			System.err.print("\tUPPAAL analysis of " + nomeFileModello);
			final long startTime = System.currentTimeMillis();
			final Process proc = pb.start();
			final Vector<SMCResult> resultVector = new Vector<SMCResult>(1); //this has no other reason than to hack around the fact that an internal class needs to have all variables it uses declared as final
			final Vector<Exception> errors = new Vector<Exception>(); //same reason as above
			final InputStream inputStream = proc.getInputStream();
			if (inputStream.markSupported()) {
				inputStream.mark(10485760); //10 Mbyte to replay a bit of the trace in case of error
			}
			new Thread() {
				@Override
				public void run() {
					try {
						SMCResult result = new UppaalModelAnalyserSMC.VariablesInterpreterConcrete(monitor).analyseSMC(m, inputStream, startTime);
						resultVector.add(result);
					} catch (Exception e) {
						System.err.println("Eccezione " + e);
						e.printStackTrace(System.err);
						errors.add(e);
						proc.destroy();
						taskStatus = 1;
					}
				}
			}.start();
			if (actionTask != null) {
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
							if (actionTask.needToStop()) {
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
					System.err.println(" was interrupted by the user");
					proc.destroy();
					throw new AnalysisException("User interrupted");
				}
				if (errors.isEmpty()) {
					while (resultVector.isEmpty()) { //if the verifyta process is completed, we may still need to wait for the analysis thread to complete
						Thread.sleep(100);
					}
				}
			} else {
				try {
					proc.waitFor();
				} catch (InterruptedException ex){
					proc.destroy();
					throw new Exception("Interrupted (1)");
				}
				while (resultVector.isEmpty()) { //if the verifyta process is completed, we may still need to wait for the analysis thread to complete
					Thread.sleep(100);
				}
			}
			if (!errors.isEmpty()) {
				Exception ex = errors.firstElement();
				throw new AnalysisException("Error during analysis", ex);
			}
			if (proc.exitValue() != 0 && ((result = resultVector.firstElement()) == null || (result.getResultType() == SMCResult.RESULT_TYPE_TRACE && result.getLevelResult().isEmpty()))) {
				StringBuilder errorBuilder = new StringBuilder();
				errorBuilder.append("[" + nomeFileModello + "] Verify result: " + proc.exitValue() + "\n");
				if (result == null) {
					errorBuilder.append(" null result\n");
				} else if (result.getResultType() == SMCResult.RESULT_TYPE_TRACE && result.getLevelResult().isEmpty()) {
					errorBuilder.append(" empty trace result\n");
				} else {
					errorBuilder.append(" result is " + result + "\n");
				}
				if (inputStream.markSupported()) {
					try {
						inputStream.reset();
					} catch (Exception ex) {
						errorBuilder.append(" (could not reset stream to diagnose error)");
						System.err.println("Stream reset failed");
					}
				}
				BufferedReader br = new BufferedReader(new InputStreamReader(inputStream/*proc.getErrorStream()*/));
				String line = null;
				try {
					while ((line = br.readLine()) != null) {
						errorBuilder.append(line + "\n");
					}
				} catch (Exception exc) {
					errorBuilder.append(" (moreover, exception " + exc + ")");
				}
				errorBuilder.append(" (current directory: " + new File(".").getAbsolutePath() + ")\n");
				
				throw new Exception(errorBuilder.toString());
			} else {
				result = resultVector.firstElement();
			}
			//N B: it is responsibility of the caller to close all streams when the process is done!!!
			proc.getErrorStream().close();
			proc.getInputStream().close();
			proc.getOutputStream().close();
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
	 * Please notice that we use the VariablesModelSMC class to transform the model into
	 * the UPPAAL input, because that type of model allows us to get concrete simulation traces,
	 * while the other model (obtained by using VariableModel) can only produce symbolic traces.
	 * Notice furthermore that we do not analyse the compiled model via the tracer tool, because
	 * (for an unexplained reason) the only way to obtain exact time values for the globalTime
	 * clock along the simulation is reading the direct verifyta output. If we try to parse its
	 * file output with the tracer tool, all exact values are substituted by a series of clock
	 * difference inequalities, which need to be solved to regain the knowledge which was there
	 * in the first place (!!). So, for simplicity's sake, we simply analyse the direct output,
	 * even if it is in principle a little slower than analysing the file output (it would
	 * certainly be if the file output also contained exact clock values instead of bounds).
	 * @param m The model to analyse
	 * @param timeTo the length of the simulation, in UPPAAL time units. The translation from
	 * real-life minutes to UPPAAL time units is made in RunAction.performNormalAnalysis.
	 * @return The SimpleLevelResult showing as series the activity levels of all reactants
	 * present in the model during the simulation period
	 */
	public SimpleLevelResult analyze(final Model m, final int timeTo) throws AnalysisException {
		SimpleLevelResult result = null;
		try {
			final VariablesModel variablesModel;
			XmlConfiguration configuration = InatBackend.get().configuration();
			String modelType = configuration.get(XmlConfiguration.MODEL_TYPE_KEY, null);
			if (modelType == null || modelType.equals(XmlConfiguration.MODEL_TYPE_REACTANT_CENTERED)) {
				variablesModel = new VariablesModelReactantCentered(); //Reactant-centered model
			} else if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTION_CENTERED_TABLES)) {
				variablesModel = new VariablesModelReactionCenteredTables(); //Reaction-centered with tables
			} else if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTION_CENTERED)) {
				variablesModel = new VariablesModelReactionCentered(); //Reaction-centered model
			} else if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTANT_CENTERED_OPAAL)) {
				variablesModel = new VariablesModelOpaal(); //Multi-core reachability analysis
				dot = "_";
			} else {
				variablesModel = new VariablesModelReactantCentered(); //Reactant-centered model is the default
			}
			final String uppaalModel = variablesModel.transform(m);
			final String uppaalQuery; //"E<> (globalTime > " + timeTo + ")";
			
			StringBuilder build = new StringBuilder("simulate 1 [<=" + timeTo + "] { ");
			for (Reactant r : m.getReactantCollection()) {
				if (r.get(Model.Properties.ENABLED).as(Boolean.class)) {
					build.append(r.getId() + ", ");
				}
			}
			for (Reaction r : m.getReactionCollection()) {
				build.append(r.getId() + dot + "T, ");
			}
			build.setCharAt(build.length() - 2, ' '); //We check when controling the model integrity that at least one reactant is enabled, so we are sure to delete a ", " here
			build.setCharAt(build.length() - 1, '}');
			
			uppaalQuery = build.toString();
			
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
			cmd[2] += " -s \"" + nomeFileModello + "\" \"" + nomeFileQuery + "\" ";
			//Runtime rt = Runtime.getRuntime();
			ProcessBuilder pb = new ProcessBuilder(cmd[0], cmd[1], cmd[2]);
			pb.redirectErrorStream(true);
			if (monitor != null) {
				monitor.setStatus("Analyzing model with UPPAAL.");
			}
			System.err.print("\tUPPAAL analysis of " + nomeFileModello);
			final Process proc = pb.start(); //rt.exec(cmd);
			final Vector<SimpleLevelResult> resultVector = new Vector<SimpleLevelResult>(1); //this has no other reason than to hack around the fact that an internal class needs to have all variables it uses declared as final
			final Vector<Exception> errors = new Vector<Exception>(); //same reason as above
			final InputStream inputStream = proc.getInputStream();
			if (inputStream.markSupported()) {
				inputStream.mark(10485760);
			}
			new Thread() {
				@Override
				public void run() {
					try {
						/*if (areWeUnderWindows()) { //If we are under windows, we need to close these unused streams, otherwise the process will mysteriously stall.
							proc.getOutputStream().close();
							proc.getErrorStream().close();
						}*/
						resultVector.add(new UppaalModelAnalyserSMC.VariablesInterpreterConcrete(monitor).analyse(m, inputStream, timeTo));
					} catch (Exception e) {
						System.err.println("Eccezione " + e);
						e.printStackTrace(System.err);
						System.out.println("Eccezione " + e);
						e.printStackTrace(System.out);
						errors.add(e);
					}
				}
			}.start();
			if (actionTask != null) {
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
							if (actionTask.needToStop()) {
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
					System.err.println(" was interrupted by the user");
					proc.destroy();
					throw new AnalysisException("User interrupted");
				}
				if (errors.isEmpty()) {
					while (resultVector.isEmpty()) { //if the verifyta process is completed, we may still need to wait for the analysis thread to complete
						Thread.sleep(100);
					}
				}
			} else {
				try {
					proc.waitFor();
				} catch (InterruptedException ex){
					proc.destroy();
					throw new Exception("Interrupted (1)");
				}
				while (resultVector.isEmpty()) { //if the verifyta process is completed, we may still need to wait for the analysis thread to complete
					Thread.sleep(100);
				}
			}
			if (!errors.isEmpty()) {
				Exception ex = errors.firstElement();
				throw new AnalysisException("Error during analysis", ex);
			}
			//result = resultVector.firstElement();
			if (proc.exitValue() != 0 && ((result = resultVector.firstElement()) == null || result.isEmpty())) {
				StringBuilder errorBuilder = new StringBuilder();
				errorBuilder.append("[" + nomeFileModello + "] Verify result: " + proc.exitValue() + "\n");
				if (result == null) {
					errorBuilder.append(" null result\n");
				} else if (result.isEmpty()) {
					errorBuilder.append(" empty result\n");
				} else {
					errorBuilder.append(" result contains " + result.getTimeIndices().size() + " time points\n");
				}
				if (inputStream.markSupported()) {
					try {
						inputStream.reset();
					} catch (Exception ex) {
						errorBuilder.append(" (could not reset stream to diagnose error)");
						System.err.println("Stream reset failed");
					}
				}
				BufferedReader br = new BufferedReader(new InputStreamReader(inputStream/*proc.getErrorStream()*/));
				String line = null;
				try {
					while ((line = br.readLine()) != null) {
						errorBuilder.append(line + "\n");
					}
				} catch (Exception exc) {
					errorBuilder.append(" (moreover, exception " + exc + ")");
				}
				errorBuilder.append(" (current directory: " + new File(".").getAbsolutePath() + ")\n");
				throw new Exception(errorBuilder.toString());
			} else {
				result = resultVector.firstElement();
			}
			//N B: it is responsibility of the caller to close all streams when the process is done!!!
			proc.getErrorStream().close();
			//if (!areWeUnderWindows()) { //These were already closed if we are under Windows
				proc.getInputStream().close();
				proc.getOutputStream().close();
			//}
			
		} catch (Exception e) {
			throw new AnalysisException("Error during analysis: " + e.getMessage(), e);
		}
		
		if (result == null || result.isEmpty()) {
			throw new AnalysisException("Error during analysis: empty result");
		}
		
		return result;
	}
	
	
	public class VariablesInterpreterConcrete {
		
		private static final String NUMBER_OF_LEVELS = Model.Properties.NUMBER_OF_LEVELS;
		private static final String ALIAS = Model.Properties.ALIAS;
		private TaskMonitor monitor = null;
		
		public VariablesInterpreterConcrete(TaskMonitor monitor) {
			this.monitor = monitor;
		}
		
		
		/**
		 * Analyse the UPPAAL output from a Statistical Model Checking query
		 * @param m The model on which the result is based 
		 * @param smcOutput The stream from which to read the UPPAAL output
		 * @return The parsed SMCResult containing the boolean/numerical query answer
		 * @throws Exception
		 */
		public SMCResult analyseSMC(Model m, InputStream smcOutput, long startTime) throws Exception {
			BufferedReader br = new BufferedReader(new InputStreamReader(smcOutput));
			SMCResult res;
			
			if (br.markSupported()) {
				br.mark(1024);
			}
			
			String line = null;
			String objective = "-- Property "; //TODO: we ignore the fact that a property MAY be satisfied! We now just consider it as satisfied.
			
			try {
				while ((line = br.readLine()) != null) {
					if (!line.contains(objective)) {
						continue;
					}
					System.err.println(" took " + RunAction.timeDifferenceFormat(startTime, System.currentTimeMillis()));
					boolean boolResult = true;
					if (line.contains("NOT")) {
						boolResult = false;
					}
					line = br.readLine();
					if (line == null) { //Property is just true or false
						res = new SMCResult(boolResult, findConfidence(line, br));
						br.close();
						return res;
					} else { //There are other components to the answer
						if (line.startsWith("Showing example trace") || line.startsWith("Showing counter example")) { //has a trace result
							if (monitor != null) {
								monitor.setStatus("Property is " + (boolResult?"true":"false") + ". Parsing " + (boolResult?"":"counter") + "example trace.");
							}
							SimpleLevelResult trace = null;
							trace = analyseNormalTraceFromStream(m, br, -1);
							res = new SMCResult(boolResult, trace);
							br.close();
							return res;
						} else if ((line.indexOf("runs) H") != -1) || (line.indexOf("runs) Pr(..)/Pr(..)") != -1)) { //it has boolean result with confidence
							res = new SMCResult(boolResult, findConfidence(line, br));
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
							
							if (br.markSupported()) {
								br.reset();
								System.err.println("All the result obtained from UPPAAL:\n" + readTheRest("", br));
							}
							
							br.close();
							return res;
						}
					}
				}
			} catch (Exception ex) {
				System.err.println("Eccezione " + ex);
				ex.printStackTrace(System.err);
				throw new Exception("Unable to understand UPPAAL SMC output: " + readTheRest(line, br), ex);
			}
			
			if (br.markSupported()) {
				br.reset();
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
			if (currentLine == null) return 1.0;
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
				//throw new Exception("Unable to understand UPPAAL SMC output: " + savedOutput);
				confidence = 1;
			} else {
				if (line.endsWith(".")) {
					line = line.substring(0, line.length() - 1);
				}
				try {
					confidence = Double.parseDouble(line.substring(line.indexOf(objective) + objective.length()));
				} catch (Exception ex) {
					confidence = 1;
				}
			}
			
			return confidence;
		}
		
		
		/**
		 * Parse the UPPAAL output containing a trace run on the given model until the given time
		 * The trace is the result of one SMC simulation query in the form
		 * simulate 1 [<=timeTo] { var1, var2, ..., varn }
		 * @param m The model on which the trace is based
		 * @param output The stream from which to read the trace
		 * @param timeTo The time up to which the simulation trace arrives (or should arrive)
		 * @return The SimpleLevelResult containing a series for each of the reactants in the model,
		 * showing the activity levels of that reactant for each time point of the trace.
		 * @throws Exception
		 */
		public SimpleLevelResult analyse(Model m, InputStream output, int timeTo) throws Exception {
			long startTime = System.currentTimeMillis();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(output));
			String line = null;
			
			while ((line = br.readLine()) != null && !line.startsWith(" -- Property is satisfied."));
			
			long endTime = System.currentTimeMillis();
			System.err.println(" took " + RunAction.timeDifferenceFormat(startTime, endTime));
			
			return analyseFromStream(m, br, timeTo);
		}
		
		private SimpleLevelResult analyseFromStream(Model m, BufferedReader br, int timeTo) throws Exception {
			long startTime, endTime;
			String line = null;
			Pattern simPointPattern = Pattern.compile("\\([-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?\\,[0-9]*\\)");
			int maxNumberOfLevels = m.getProperties().get(NUMBER_OF_LEVELS).as(Integer.class);
			HashMap<String, Double> numberOfLevels = new HashMap<String, Double>();
			Map<String, SortedMap<Double, Double>> levels = new HashMap<String, SortedMap<Double, Double>>();
			
			if (monitor != null) {
				monitor.setStatus("Analyzing UPPAAL output trace.");
			}
			startTime = System.currentTimeMillis();
			
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
			}
			
			SortedMap<Double, Double> rMap;
			String reactantId, reactionId = "";
			Reaction reaction = null;
			int minTime = m.getProperties().get(Model.Properties.MINIMUM_DURATION).as(Integer.class), //Use the global minimum instead of the local minimum for time table values
				maxTime = m.getProperties().get(Model.Properties.MAXIMUM_DURATION).as(Integer.class);
			double activityIntervalWidth = Math.log10(1.0 * minTime / maxTime);
			
			while ((line = br.readLine()) != null) {
				reactantId = line.substring(0, line.indexOf(":"));
				//System.err.println(reactantId + ": ");
				line = br.readLine();
				//System.err.println(line);
				line = line.substring(line.indexOf(":"));
				Matcher pointMatcher = simPointPattern.matcher(line);
				
				if (m.getReactant(reactantId) != null) {
					levels.put(reactantId, new TreeMap<Double, Double>());
				} else {
					reactionId = reactantId.substring(0, reactantId.lastIndexOf(dot));
					reaction = m.getReaction(reactionId);
					if (reaction != null) {
						levels.put(reaction.get(Model.Properties.CYTOSCAPE_ID).as(String.class), new TreeMap<Double, Double>());
					}
				}
				
				while (pointMatcher.find()) {
					String point = pointMatcher.group();
					//System.err.print(point);
					point = point.substring(1, point.length() - 1);
					//System.err.println("-->" + point);
					double time = Double.valueOf(point.substring(0, point.indexOf(",")).trim());
					double level = Integer.valueOf(point.substring(point.indexOf(",") + 1).trim());
					String chosenMap = reactantId;
					if (m.getReactant(reactantId) != null) { //it is a reactant, so rescale the value as activity level, using the maximum number of levels
						if (numberOfLevels.get(reactantId) != maxNumberOfLevels) {
							level = level / (double)numberOfLevels.get(reactantId) * (double)maxNumberOfLevels;
						}
					} else { //It is not a reactant: for the moment, this can only mean that it is a reaction duration
						reactionId = reactantId.substring(0, reactantId.lastIndexOf(dot)); //It is in the form R6_R4.T, so we remove the ".T" and keep the rest, which is used as reaction ID in the Model object
						reaction = m.getReaction(reactionId);
						if (reaction != null) {
							chosenMap = reaction.get(Model.Properties.CYTOSCAPE_ID).as(String.class);
							//int minTime = reaction.get(Model.Properties.MINIMUM_DURATION).as(Integer.class); //We use global instead of local minimum: see definition of minTime
							if (level == 0 || level == VariablesModelReactionCentered.INFINITE_TIME || minTime == VariablesModelReactionCentered.INFINITE_TIME) { //I put also level == 0 because otherwise we go in the "else" and we divide by 0 =)
								level = 0;
							} else if (activityIntervalWidth > -2) { //If there are not orders of magnitude of difference between minimum and maximum, we can simply use a normal linear scale as we did before
								level = 1.0 * minTime / level;
							} else {
								//level = 1.0 * minTime / level;
								level = (activityIntervalWidth - Math.log10(1.0 * minTime / level)) / activityIntervalWidth;
							}
						}
					}
					rMap = levels.get(chosenMap);
					if (rMap.isEmpty() || timeTo == -1 || (rMap.get(rMap.lastKey()) != level && time <= timeTo)) { //if we didn't register a variation, we don't plot a point
																								   //Also, if the current time is already over the requested simulation time, we don't put it in the data (but still read the values, to clear the stream)
						rMap.put(time, (double)level);
					}
				}
				//System.err.println();
			}
			
			//if (time < timeTo) { //if the state of the system remains unchanged from a certain time on (and so UPPAAL terminates on that point), but we asked for a later time, we add a final point where all data remain unchanged, so that the user can see the "evolution" up to the requested point
			//we do it always, because there can be some situations in which reactants are not read while time increases, and thus we can reach the end of time without having an updated value for each reactant
			if (timeTo != -1) {
				for (String reactantName : levels.keySet()) {
					SortedMap<Double, Double> values = levels.get(reactantName);
					double lastValue = values.get(values.lastKey());
					values.put((double)timeTo, lastValue);
				}
			}
			//}
			
			endTime = System.currentTimeMillis();
			System.err.println("\tParsing the result produced by UPPAAL took " + RunAction.timeDifferenceFormat(startTime, endTime));
			
			return new SimpleLevelResult(maxNumberOfLevels, levels);
		}
		
		
		
		/**
		 * Parse the UPPAAL output containing a trace run on the given model until the given time
		 * The trace is the (counter)example resulting from a (true) <> query or (false) [] query.
		 * @param m The model on which the trace is based
		 * @param output The stream from which to read the trace
		 * @param timeTo The time up to which the simulation trace arrives (or should arrive)
		 * @return The SimpleLevelResult containing a series for each of the reactants in the model,
		 * showing the activity levels of that reactant for each time point of the trace.
		 * @throws Exception
		 */
		public SimpleLevelResult analyseNormalTrace(Model m, InputStream output, int timeTo) throws Exception {
			BufferedReader br = new BufferedReader(new InputStreamReader(output));
//			String line = null;
//			while ((line = br.readLine()) != null && !line.startsWith("Showing example trace")); //"Showing example trace" is written on a different stream!
			
			return analyseNormalTraceFromStream(m, br, timeTo);
		}
		
		private SimpleLevelResult analyseNormalTraceFromStream(Model m, BufferedReader br, int timeTo) throws Exception {
			//rand = new Random(randInitializer.nextLong());
			lastComputed = -1; //Lascialo a -1! Quando chiedo di calcolare il random dentro un intervallo, se lastComputed e' < 0 uso come lastComputed il lowerBound dell'intervallo.
			
			
			Map<String, SortedMap<Double, Double>> levels = new HashMap<String, SortedMap<Double, Double>>();
			
			String line = null, lastLine = null;
			Pattern globalTimeLowerBoundPattern = Pattern.compile("[' ']globalTime>[=]?[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?");  //Pattern.compile("globalTime[=][0-9]+"); //The new pattern supports also numbers like 3.434252e+06, which can occur(!!)
			Pattern globalTimeUpperBoundPattern = Pattern.compile("[' ']globalTime<[=]?[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?");
			Pattern globalTimePrecisePattern = Pattern.compile(" globalTime[=][-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?");
			Pattern statePattern = Pattern.compile("[A-Za-z0-9_\\.]+[' ']*[=][' ']*[0-9]+");
			int timeLB = -1, timeUB = -1, lastTimeLB = -1, lastTimeUB = -1; //This is done so that when parsing the lines we correctly set the initial value for every variable (also the .T's), because we will read the latest value of those variables ALSO for time = 0
			int maxNumberOfLevels = m.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
			HashMap<String, Double> numberOfLevels = new HashMap<String, Double>();
//			int readAheadLimit = 1000; //Used to define the buffer to memorize the last line when we put a mark to read ahead the next line
			
			
			int minTime = m.getProperties().get(Model.Properties.MINIMUM_DURATION).as(Integer.class), //Use the global minimum instead of the local minimum for time table values
					maxTime = m.getProperties().get(Model.Properties.MAXIMUM_DURATION).as(Integer.class);
			double activityIntervalWidth = Math.log10(1.0 * minTime / maxTime);
			while ((line = br.readLine()) != null) {
				if (!line.startsWith("State"))
					continue;
				line = br.readLine(); //the "State:" string has a \n at the end, so we need to read the next line
				line = br.readLine(); //the second line contains informations about which we don't care. We want variable values
				line = br.readLine();
				Matcher stateMatcher = statePattern.matcher(line);
				String s = null;
				while (stateMatcher.find()) {
					s = stateMatcher.group();
					String reactantId = null;
					if (s.indexOf(' ') >= 0 && s.indexOf(' ') < s.indexOf('=')) {
						reactantId = s.substring(0, s.indexOf(' '));
					} else {
						reactantId = s.substring(0, s.indexOf('='));
					}
					if (reactantId.equals("globalTime") || reactantId.equals("r")
						|| reactantId.startsWith("reactant") || reactantId.startsWith("output") //private variables are not taken into account
						|| reactantId.equals("r1") || reactantId.equals("r2")
						|| reactantId.endsWith(dot + "r")
						|| reactantId.endsWith(dot + "reactant") || reactantId.endsWith(dot + "output")
						|| reactantId.endsWith(dot + "r1") || reactantId.endsWith(dot + "r2")
						|| reactantId.endsWith(dot + "e") || reactantId.endsWith(dot + "b")
						|| reactantId.endsWith(dot + "c") || reactantId.endsWith(dot + "delta")
						|| reactantId.endsWith(dot + "tL") || reactantId.endsWith(dot + "tU")
						|| reactantId.endsWith(dot + "deltaNew") || reactantId.endsWith(dot + "deltaOld")
						|| reactantId.endsWith(dot + "deltaOldOld") || reactantId.endsWith(dot + "deltaOldOldOld")
						|| reactantId.endsWith(dot + "deltaAlternating")) continue;
					if (reactantId.endsWith(dot + "T")) {
						reactantId = m.getReaction(reactantId.substring(0, reactantId.lastIndexOf(dot + "T"))).get(Model.Properties.CYTOSCAPE_ID).as(String.class);
					}
					//System.err.println("Inserisco il reagente " + reactantId);
					// put the reactant into the result map
					levels.put(reactantId, new TreeMap<Double, Double>());
					
					double level = Integer.valueOf(s.substring(s.indexOf("=") + 1).trim());
					if (m.getReactant(reactantId) != null) { //it is a reactant, so rescale the value as activity level, using the maximum number of levels
						Reactant r = m.getReactant(reactantId);
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
						if (nLvl != maxNumberOfLevels) {
							level = level / (double)numberOfLevels.get(reactantId) * (double)maxNumberOfLevels;
						}
					} else { //It is not a reactant: for the moment, this can only mean that it is a reaction duration
						if (level == 0 || level == VariablesModelReactionCentered.INFINITE_TIME || minTime == VariablesModelReactionCentered.INFINITE_TIME) { //I put also level == 0 because otherwise we go in the "else" and we divide by 0 =)
							level = 0;
						} else if (activityIntervalWidth > -2) { //If there are not orders of magnitude of difference between minimum and maximum, we can simply use a normal linear scale as we did before
							level = 1.0 * minTime / level;
						} else {
							//level = 1.0 * minTime / level;
							level = (activityIntervalWidth - Math.log10(1.0 * minTime / level)) / activityIntervalWidth;
						}
					}
					
					SortedMap<Double, Double> rMap = levels.get(reactantId);
					rMap.put(0.0, level);
				}
				break;
			}
			
//			// add initial concentrations and get number of levels
//			for (Reactant r : m.getReactantCollection()) {
//				Integer nLvl = r.get(NUMBER_OF_LEVELS).as(Integer.class);
//				if (nLvl == null) {
//					Property nameO = r.get(ALIAS);
//					String name;
//					if (nameO == null) {
//						name = r.getId();
//					} else {
//						name = nameO.as(String.class);
//					}
//					String inputLevels = JOptionPane.showInputDialog("Missing number of levels for reactant \"" + name + "\" (" + r.getId() + ").\nPlease insert the max number of levels for \"" + name + "\"", maxNumberOfLevels);
//					if (inputLevels != null) {
//						try {
//							nLvl = new Integer(inputLevels);
//						} catch (Exception ex) {
//							nLvl = maxNumberOfLevels;
//						}
//					} else {
//						nLvl = maxNumberOfLevels;
//					}
//				}
//				numberOfLevels.put(r.getId(), (double)nLvl);
//				
////				if (levels.containsKey(r.getId())) {
////					double initialLevel = r.get(Model.Properties.INITIAL_LEVEL).as(Integer.class);
////					initialLevel = initialLevel / (double)nLvl * (double)maxNumberOfLevels; //of course, the initial "concentration" itself needs to be rescaled correctly
////					levels.get(r.getId()).put(0.0, initialLevel);
////				}
//			}
			
			long startTime = System.currentTimeMillis();
			
			lastTimeLB = timeLB;
			lastTimeUB = timeUB;
			lastLine = line;
			String lastState = null;
			double currentTime = 0; //we use it to see where we are along the simulation run (we get its value from the parseLine function), and thus compute a % of advancement with respect to timeTo
			int lastPercent = 0;
			while ((line = br.readLine()) != null) {
				if (!line.startsWith("State")) continue;
				br.readLine(); //as said before, the "State:" string ends with \n, so we need to read the next line in order to get the actual state data
				br.readLine(); //and the line after that contains only the states of the processes, while we are interested in variable values, which are in the 3rd line
				line = " " + br.readLine(); //If I don't add a space at the start of the line, the patterns will not match correctly. I need a space in front of the "globalTime" string because otherwise I would also match things like clockOfWhichIDontReallyCare-globalTime<=12345
				//System.err.print(line);
				lastState = line;
				Matcher timeMatcherPrecise = globalTimePrecisePattern.matcher(line);
				if (!timeMatcherPrecise.find()) {
					//System.err.print("Bounds:"); System.err.flush();
					Matcher timeMatcherLB = globalTimeLowerBoundPattern.matcher(line),
							timeMatcherUB = globalTimeUpperBoundPattern.matcher(line);
					int newTimeUB = -1, newTimeLB = -1;
					if (timeMatcherLB.find()) {
						String valueLB = (timeMatcherLB.group()).split(">")[1];
						//System.err.print(" (valueLB = " + valueLB + ") "); System.err.flush();
						if (valueLB.substring(0, 1).equals("=")) {
							if (valueLB.substring(1, 2).equals("-")) {
								newTimeLB = (int)Math.round(Double.parseDouble(valueLB.substring(2, valueLB.length())));
							} else {
								newTimeLB = (int)Math.round(Double.parseDouble(valueLB.substring(1, valueLB.length())));
							}
						} else {
							if (valueLB.substring(0, 1).equals("-")) {
								newTimeLB = (int)(Math.round(Double.parseDouble(valueLB.substring(1, valueLB.length())))); //why +1??
							} else {
								newTimeLB = (int)(Math.round(Double.parseDouble(valueLB.substring(0, valueLB.length())))); //why +1??
							}
						}
						//System.err.print(" ma il tempo LB si': " + newTimeLB);
					} else {
						newTimeLB = 0;
						//System.err.print(" ne' il tempo LB");
					}
					
					if (timeMatcherUB.find()) {
						String valueUB = (timeMatcherUB.group()).split("<")[1];
						//System.err.print(" (valueUB = " + valueUB + ") "); System.err.flush();
						if (valueUB.substring(0, 1).equals("=")) {
							if (valueUB.substring(1, 2).equals("-")) {
								newTimeUB = (int)Math.round(Double.parseDouble(valueUB.substring(2, valueUB.length())));
							} else {
								newTimeUB = (int)Math.round(Double.parseDouble(valueUB.substring(1, valueUB.length())));
							}
						} else {
							if (valueUB.substring(0, 1).equals("-")) {
								newTimeUB = (int)(Math.round(Double.parseDouble(valueUB.substring(1, valueUB.length())))); //why +1??
							} else {
								newTimeUB = (int)(Math.round(Double.parseDouble(valueUB.substring(0, valueUB.length())))); //why +1??
							}
						}
						//System.err.println(" ma il tempo UB si': " + newTimeUB);
					} else {
						newTimeUB = newTimeLB;
						//System.err.println(" ne' il tempo UB");
					}
					//System.err.println(" [" + newTimeLB + ", " + newTimeUB + "]");
					
					if (newTimeLB > timeLB || newTimeUB > timeUB) {
						//System.err.println("La mia scusa e' che " + newTimeLB + " > " + timeLB + " o che " + newTimeUB + " > " + timeUB);
						//System.err.println("  Analizzo quindi la riga " + lastLine);
						currentTime = parseLine(m, timeLB, timeUB, timeTo, lastLine, numberOfLevels, maxNumberOfLevels, statePattern, levels);
						timeLB = newTimeLB;
						timeUB = newTimeUB;
						lastLine = line;
					} else {
						lastLine = line;
					}
				} else {
					//System.err.print("Tempo preciso:");
					String value = (timeMatcherPrecise.group().split("=")[1]);
					int newTimeLB = -1;
					if (value.substring(0, 1).equals("=")) {
						if (value.substring(1, 2).equals("-")) {
							newTimeLB = (int)Math.round(Double.parseDouble(value.substring(2, value.length())));
						} else {
							newTimeLB = (int)Math.round(Double.parseDouble(value.substring(1, value.length())));
						}
					} else {
						if (value.substring(0, 1).equals("-")) {
							newTimeLB = (int)(Math.round(Double.parseDouble(value.substring(1, value.length())))); //why +1??
						} else {
							newTimeLB = (int)(Math.round(Double.parseDouble(value.substring(0, value.length())))); //why +1??
						}
					}
					int newTimeUB = newTimeLB;
					//System.err.println(" " + newTimeLB);
					
					if (lastTimeLB == timeLB) {
						if (newTimeLB > timeLB) {
							lastTimeLB = newTimeLB;
							lastTimeUB = newTimeUB;
							lastLine = line;
						} else {
							//This actually cannot happen, because the first time we find that newTime > lastTime we also set time = lastTime (and we parse lastLine)
						}
					} else {
						if (newTimeLB == lastTimeLB) {
							//we don't need to parse now
							lastLine = line;
						} else if (newTimeLB > lastTimeLB) {
							//System.err.println("La mia (2a) scusa e' che " + newTimeLB + " > " + lastTimeLB);
							timeLB = lastTimeLB;
							timeUB = lastTimeUB;
							currentTime = parseLine(m, timeLB, timeUB, timeTo, lastLine, numberOfLevels, maxNumberOfLevels, statePattern, levels);
						}
					}
				}
				
				if (monitor != null && timeTo != -1) {
					int currentPercent = (int)Math.round(currentTime / timeTo * 100);
					monitor.setPercentCompleted(currentPercent);
					if (currentPercent - lastPercent > 0) {
						double remainingTimeEstimation = (double)(System.currentTimeMillis() - startTime) / currentPercent * (100.0 - currentPercent + 1);
						monitor.setEstimatedTimeRemaining(Math.round(remainingTimeEstimation));
						lastPercent = currentPercent;
					}
				}
			}
			//The parse of the latest values needs to be done when we are sure there will be no more changes (i.e. at the end)
//			timeLB = lastTimeLB;
//			timeUB = lastTimeUB;
//			currentTime = parseLine(m, timeLB, timeUB, timeTo, lastLine, numberOfLevels, maxNumberOfLevels, statePattern, levels);
			
			if (lastState != null) {
				//Now parse the latest point (the one commented above was not right: we actually need to read the time bounds)
				
				line = lastLine = lastState;
				Matcher timeMatcherPrecise = globalTimePrecisePattern.matcher(line);
				if (!timeMatcherPrecise.find()) {
					Matcher timeMatcherLB = globalTimeLowerBoundPattern.matcher(line),
							timeMatcherUB = globalTimeUpperBoundPattern.matcher(line);
					int newTimeUB = -1, newTimeLB = -1;
					if (timeMatcherLB.find()) {
						String valueLB = (timeMatcherLB.group()).split(">")[1];;
						if (valueLB.substring(0, 1).equals("=")) {
							if (valueLB.substring(1, 2).equals("-")) {
								newTimeLB = (int)Math.round(Double.parseDouble(valueLB.substring(2, valueLB.length())));
							} else {
								newTimeLB = (int)Math.round(Double.parseDouble(valueLB.substring(1, valueLB.length())));
							}
						} else {
							if (valueLB.substring(0, 1).equals("-")) {
								newTimeLB = (int)(Math.round(Double.parseDouble(valueLB.substring(1, valueLB.length())))); //why +1??
							} else {
								newTimeLB = (int)(Math.round(Double.parseDouble(valueLB.substring(0, valueLB.length())))); //why +1??
							}
						}
						//System.err.println(" ma il tempo LB si': " + newTimeLB);
					} else {
						newTimeLB = 0;
						//System.err.println(" ne' il tempo LB");
					}
					
					if (timeMatcherUB.find()) {
						String valueUB = (timeMatcherUB.group()).split("<")[1];;
						if (valueUB.substring(0, 1).equals("=")) {
							if (valueUB.substring(1, 2).equals("-")) {
								newTimeUB = (int)Math.round(Double.parseDouble(valueUB.substring(2, valueUB.length())));
							} else {
								newTimeUB = (int)Math.round(Double.parseDouble(valueUB.substring(1, valueUB.length())));
							}
						} else {
							if (valueUB.substring(0, 1).equals("-")) {
								newTimeUB = (int)(Math.round(Double.parseDouble(valueUB.substring(1, valueUB.length())))); //why +1??
							} else {
								newTimeUB = (int)(Math.round(Double.parseDouble(valueUB.substring(0, valueUB.length())))); //why +1??
							}
						}
						//System.err.println(" ma il tempo UB si': " + newTimeUB);
					} else {
						newTimeUB = newTimeLB;
						//System.err.println(" ne' il tempo UB");
					}
					//System.err.println("Tempi letti: [" + newTimeLB + ", " + newTimeUB + "]");
					
					currentTime = parseLine(m, newTimeLB, newTimeUB, timeTo, lastState, numberOfLevels, maxNumberOfLevels, statePattern, levels);
				} else {
					String value = (timeMatcherPrecise.group().split("=")[1]);
					int newTimeLB = -1;
					if (value.substring(0, 1).equals("=")) {
						if (value.substring(1, 2).equals("-")) {
							newTimeLB = (int)Math.round(Double.parseDouble(value.substring(2, value.length())));
						} else {
							newTimeLB = (int)Math.round(Double.parseDouble(value.substring(1, value.length())));
						}
					} else {
						if (value.substring(0, 1).equals("-")) {
							newTimeLB = (int)(Math.round(Double.parseDouble(value.substring(1, value.length())))); //why +1??
						} else {
							newTimeLB = (int)(Math.round(Double.parseDouble(value.substring(0, value.length())))); //why +1??
						}
					}
					int newTimeUB = newTimeLB;
					//System.err.println("Tempo letto: " + newTimeLB);
					
					currentTime = parseLine(m, newTimeLB, newTimeUB, timeTo, lastState, numberOfLevels, maxNumberOfLevels, statePattern, levels);
				}
			}
			
			
			//if (time < timeTo) { //if the state of the system remains unchanged from a certain time on (and so UPPAAL terminates on that point), but we asked for a later time, we add a final point where all data remain unchanged, so that the user can see the "evolution" up to the requested point
			//we do it always, because there can be some situations in which reactants are not read while time increases, and thus we can reach the end of time without having an updated value for each reactant
			if (timeTo != -1) {
				for (String reactantName : levels.keySet()) {
					SortedMap<Double, Double> values = levels.get(reactantName);
					double lastValue = values.get(values.lastKey());
					values.put((double)timeTo, lastValue);
				}
			} else {
				double lastPoint = -1;
				for (String k : levels.keySet()) {
					double t = levels.get(k).lastKey();
					if (lastPoint == -1 || t > lastPoint) {
						lastPoint = t;
					}
				}
				for (String reactantName : levels.keySet()) {
					SortedMap<Double, Double> values = levels.get(reactantName);
					double lastValue = values.get(values.lastKey());
					values.put((double)lastPoint, lastValue);
				}
			}
			//}
			
			long endTime = System.currentTimeMillis();
			System.err.println("\tParsing the result produced by UPPAAL took " + RunAction.timeDifferenceFormat(startTime, endTime));
			
			return new SimpleLevelResult(maxNumberOfLevels, levels);
			
		}
		
		private Random rand = new Random();
		//private static Random randInitializer = new Random(); //This way we can safely call this analyzer also in the same millisecond
		private double lastComputed;
		private double chooseTime(double lowerBound, double upperBound) {
			if (lowerBound == upperBound) {
				lastComputed = lowerBound;
			} else {
				if (lastComputed < 0) {
					lastComputed = lowerBound;
				}
				lastComputed = (rand.nextDouble() * (upperBound - lastComputed)) + lastComputed; //(rand.nextDouble() * (upperBound - lowerBound)) + lowerBound;
			}
			//System.err.println("[" + lowerBound + ", " + upperBound + "] --> " + lastComputed);
			return lastComputed;
		}
		
		private double parseLine(Model m, double timeLB, double timeUB, int timeTo, String line, Map<String, Double> numberOfLevels, int maxNumberOfLevels, Pattern statePattern, Map<String, SortedMap<Double, Double>> levels) {
			Matcher stateMatcher = statePattern.matcher(line);
			String s = null;
			if (timeLB == -1) {
				timeLB = timeUB = 0;
			}
			double time = chooseTime(timeLB, timeUB);
			//System.err.println("[" + timeLB + ", " + timeUB + "] --> t = " + time + " Parso la linea " + line);
			//System.err.print("===========[" + timeLB + ", " + timeUB + "] --> " + time + ":");
			String reactantId, reactionId = "";
			Reaction reaction = null;
			int minTime = m.getProperties().get(Model.Properties.MINIMUM_DURATION).as(Integer.class), //Use the global minimum instead of the local minimum for time table values
				maxTime = m.getProperties().get(Model.Properties.MAXIMUM_DURATION).as(Integer.class);
			double activityIntervalWidth = Math.log10(1.0 * minTime / maxTime);
			while (stateMatcher.find()) {
				s = stateMatcher.group();
				reactantId = null;
				if (s.indexOf(' ') >= 0 && s.indexOf(' ') < s.indexOf('=')) {
					reactantId = s.substring(0, s.indexOf(' '));
				} else {
					reactantId = s.substring(0, s.indexOf('='));
				}
				/*if (reactantId.equals("c") || reactantId.equals("globalTime") || reactantId.equals("r")
					|| reactantId.startsWith("input_reactant_") || reactantId.startsWith("output_reactant_")
					|| reactantId.equals("r1") || reactantId.equals("r2") || maximumValues.get(reactantId) == null) continue; //we check whether it is a private variable*/
				if (!reactantId.endsWith(dot + "T") && !levels.containsKey(reactantId)) continue;
				// we can determine the level of activation
				double level = Integer.valueOf(s.substring(s.indexOf("=") + 1).trim());
				String chosenMap = reactantId;
				if (m.getReactant(reactantId) != null) { //it is a reactant, so rescale the value as activity level, using the maximum number of levels
					if (numberOfLevels.get(reactantId) != maxNumberOfLevels) {
						level = level / (double)numberOfLevels.get(reactantId) * (double)maxNumberOfLevels;
					}
				} else { //It is not a reactant: for the moment, this can only mean that it is a reaction duration
					reactionId = reactantId.substring(0, reactantId.lastIndexOf(dot + "T")); //It is in the form R6_R4.T, so we remove the ".T" and keep the rest, which is used as reaction ID in the Model object
					reaction = m.getReaction(reactionId);
					if (reaction != null) {
						chosenMap = reaction.get(Model.Properties.CYTOSCAPE_ID).as(String.class);
						//int minTime = reaction.get(Model.Properties.MINIMUM_DURATION).as(Integer.class); //We use global instead of local minimum: see definition of minTime
						if (level == 0 || level == VariablesModelReactionCentered.INFINITE_TIME || minTime == VariablesModelReactionCentered.INFINITE_TIME) { //I put also level == 0 because otherwise we go in the "else" and we divide by 0 =)
							level = 0;
						} else if (activityIntervalWidth > -2) { //If there are not orders of magnitude of difference between minimum and maximum, we can simply use a normal linear scale as we did before
							level = 1.0 * minTime / level;
						} else {
							//level = 1.0 * minTime / level;
							level = (activityIntervalWidth - Math.log10(1.0 * minTime / level)) / activityIntervalWidth;
						}
					}
				}
				//System.err.print(chosenMap + " = " + level + ", ");
				
				SortedMap<Double, Double> rMap = levels.get(chosenMap);
				if (rMap.isEmpty() || (rMap.get(rMap.lastKey()) != level)) { //if we didn't register a variation, we don't plot a point
					/*if (rMap.lastKey() < time - 1) { //We use this piece to explicitly keep a level constant when it is not varying (i.e., the graph will never contain non-vertical,non-horizontal lines)
						rMap.put((double)(time - 1), rMap.get(rMap.lastKey()));
					}*/
					//System.err.println("  e aggiungo il punto (" + time + ", " + level + ")");
					rMap.put(time, level);
					//System.err.print(chosenMap + " = " + level + ", ");
				}
			}
			//System.err.println("===========");
			return time;
		}
	}
	
}