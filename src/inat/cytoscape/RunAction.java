package inat.cytoscape;

import inat.InatBackend;
import inat.analyser.SMCResult;
import inat.analyser.uppaal.ResultAverager;
import inat.analyser.uppaal.SimpleLevelResult;
import inat.analyser.uppaal.UppaalModelAnalyserSMC;
import inat.exceptions.InatException;
import inat.model.Model;
import inat.model.Reactant;
import inat.network.UPPAALClient;
import inat.util.XmlConfiguration;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import cytoscape.Cytoscape;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTask;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;

/**
 * The run action runs the network through the ANIMO analyser.
 * 
 * @author Brend Wanders
 * 
 */
public class RunAction extends InatActionTask {
	private static final String SECONDS_PER_POINT = Model.Properties.SECONDS_PER_POINT; //The number of real-life seconds represented by a single UPPAAL time unit
	private static final long serialVersionUID = -5018057013811632477L;
	private int timeTo = 1200; //The default number of UPPAAL time units until which a simulation will run
	private double scale = 0.2; //The time scale representing the number of real-life minutes represented by a single UPPAAL time unit
	private JCheckBox remoteUppaal; //The RadioButtons telling us whether we use a local or a remote engine, and whether we use the Statistical Model Checking or the "normal" engine
	private JRadioButton smcUppaal;
	private JRadioButton computeAvgStdDev, //Whether to compute the standard deviation when computing the average of a series of runs (if average of N runs is requested)
						 overlayPlot; //Whether to show all plots as a series each
	private JFormattedTextField timeToFormula, nSimulationRuns; //Up to which point in time (real-life minutes) the simulation(s) will run, and the number of simulations (if average of N runs is requested)
	private JTextField serverName, serverPort, smcFormula; //The name of the server, and the corresponding port, in the case we use a remote engine. The text inserted by the user for the SMC formula. Notice that this formula will need to be changed so that it will be compliant with the UPPAAL time scale, and reactant names
	private boolean needToStop; //Whether the user has pressed the Cancel button on the TaskMonitor while we were running an analysis process
	private InatActionTask meStesso; //Myself
	
	/**
	 * Constructor.
	 * 
	 * @param plugin the plugin we should use
	 */
	public RunAction(InatPlugin plugin, JCheckBox remoteUppaal, JTextField serverName, JTextField serverPort, JRadioButton smcUppaal, JFormattedTextField timeToFormula, JFormattedTextField nSimulationRuns, JRadioButton computeAvgStdDev, JRadioButton overlayPlot, JTextField smcFormula) {
		super("<html>Analyze <br/>network</html>");
		this.remoteUppaal = remoteUppaal;
		this.serverName = serverName;
		this.serverPort = serverPort;
		this.smcUppaal = smcUppaal;
		this.timeToFormula = timeToFormula;
		this.nSimulationRuns = nSimulationRuns;
		this.computeAvgStdDev = computeAvgStdDev;
		this.overlayPlot = overlayPlot;
		this.smcFormula = smcFormula;
		this.meStesso = this;
	}
	

	@Override
	public void actionPerformed(ActionEvent e) {
		RunTask task = new RunTask();

		// Configure JTask Dialog Pop-Up Box
		JTaskConfig jTaskConfig = new JTaskConfig();
		jTaskConfig.setOwner(Cytoscape.getDesktop());
		// jTaskConfig.displayCloseButton(true);
		// jTaskConfig.displayCancelButton(true);

		jTaskConfig.displayStatus(true);
		jTaskConfig.setAutoDispose(true);
		jTaskConfig.displayCancelButton(true);
		jTaskConfig.displayTimeElapsed(true);
		jTaskConfig.setModal(true);
		//if (nSimulationRuns.isEnabled()) {
			jTaskConfig.displayTimeRemaining(true);
		//}
		
		long startTime = System.currentTimeMillis();
		Date now = new Date(startTime);
		Calendar nowCal = Calendar.getInstance();
		File logFile = null;
		PrintStream logStream = null;
		PrintStream oldErr = System.err;
		try {
			if (UppaalModelAnalyserSMC/*FasterConcrete*/.areWeUnderWindows()) {
				logFile = File.createTempFile("ANIMO_run_" + nowCal.get(Calendar.YEAR) + "-" + nowCal.get(Calendar.MONTH) + "-" + nowCal.get(Calendar.DAY_OF_MONTH) + "_" + nowCal.get(Calendar.HOUR_OF_DAY) + "-" + nowCal.get(Calendar.MINUTE) + "-" + nowCal.get(Calendar.SECOND), ".log"); //windows doesn't like long file names..
			} else {
				logFile = File.createTempFile("ANIMO run " + now.toString(), ".log");
			}
			logFile.deleteOnExit();
			logStream = new PrintStream(new FileOutputStream(logFile));
			System.setErr(logStream);
		} catch (Exception ex) {
			//We have no log file, bad luck: we will have to use System.err.
		}
		
		// Execute Task in New Thread; pops open JTask Dialog Box.
		TaskManager.executeTask(task, jTaskConfig);
		
		long endTime = System.currentTimeMillis();
		
		try {
			System.err.println("Time taken: " + timeDifferenceFormat(startTime, endTime));
			System.err.flush();
			System.setErr(oldErr);
			if (logStream != null) {
				logStream.close();
			}
		} catch (Exception ex) {
			
		}
	}
	
	public boolean needToStop() {
		return this.needToStop;
	}

	private class RunTask implements Task {
		
		private static final String REACTANT_ALIAS = Model.Properties.ALIAS;
		private TaskMonitor monitor;
		
		@Override
		public String getTitle() {
			return "ANIMO analysis";
		}
		
		@Override
		public void halt() {
			needToStop = true;
		}
		
		@Override
		public void run() {
			try {
				needToStop = false;
				
				
				//Just check that we understand how many minutes to run the simulation
				int nMinutesToSimulate = 0;
				try {
					nMinutesToSimulate = Integer.parseInt(timeToFormula.getValue().toString());
				} catch (Exception ex) {
					if (!smcUppaal.isSelected()) {
						throw new Exception("Unable to understand the number of minutes requested for the simulation.");
					}
				}
				
				this.monitor.setStatus("Creating model representation");
				this.monitor.setPercentCompleted(0);
				
				boolean generateTables = false;
				XmlConfiguration configuration = InatBackend.get().configuration();
				String modelType = configuration.get(XmlConfiguration.MODEL_TYPE_KEY, null);
				if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTION_CENTERED_TABLES)) {
					generateTables = true;
				}
				
				final Model model;
				if (smcUppaal.isSelected()) {
					model = Model.generateModelFromCurrentNetwork(this.monitor, null, generateTables);
				} else {
					model = Model.generateModelFromCurrentNetwork(this.monitor, nMinutesToSimulate, generateTables);
				}
				
				model.getProperties().let(Model.Properties.MODEL_CHECKING_TYPE).be(Model.Properties.STATISTICAL_MODEL_CHECKING);
				
				boolean noReactantsPlotted = true;
				for (Reactant r : model.getReactantCollection()) {
					if (r.get(Model.Properties.ENABLED).as(Boolean.class) && r.get(Model.Properties.PLOTTED).as(Boolean.class)) {
						noReactantsPlotted = false;
						break;
					}
				}
				if (noReactantsPlotted && !smcUppaal.isSelected()) {
					JOptionPane.showMessageDialog((JTask)this.monitor, "No reactants selected for plot: select at least one reactant to be plotted in the graph.", "Error", JOptionPane.ERROR_MESSAGE); 
					throw new InatException("No reactants selected for plot: select at least one reactant to be plotted in the graph.");
				}
				
				if (smcUppaal.isSelected()) {
					performSMCAnalysis(model);
				} else {
					performNormalAnalysis(model);
				}
				
			} catch (InterruptedException e) {
				this.monitor.setException(e, "Analysis cancelled by the user.");
			} catch (Exception e) {
				this.monitor.setException(e, "An error occurred while analyzing the network.");
			}
		}
		
		/**
		 * Translate the SMC formula into UPPAAL time units, reactant names
		 * and give it to the analyser. Show the result in a message window.
		 * @param model
		 * @throws Exception
		 */
		private void performSMCAnalysis(final Model model) throws Exception {
			//TODO: "understand" the formula and correctly change time values and reagent names
			String probabilisticFormula = smcFormula.getText();
			for (Reactant r : model.getReactantCollection()) {
				String name = r.get(REACTANT_ALIAS).as(String.class);
				if (probabilisticFormula.contains(name)) {
					probabilisticFormula = probabilisticFormula.replace(name, r.getId());
				}
			}
			if (probabilisticFormula.contains("Pr[<")) {
	            String[] parts = probabilisticFormula.split("Pr\\[<");
	            StringBuilder sb = new StringBuilder();
	            for (String p : parts) {
	               if (p.length() < 1) continue;
	               String timeS;
	               if (p.startsWith("=")) {
	                  timeS = p.substring(1, p.indexOf("]"));
	               } else {
	                  timeS = p.substring(0, p.indexOf("]"));
	               }
	               int time;
	               try {
	                  time = Integer.parseInt(timeS);
	               } catch (Exception ex) {
	                  throw new Exception("Problems with the identification of time string \"" + timeS + "\"");
	               }
	               time = (int)(time * 60.0 / model.getProperties().get(SECONDS_PER_POINT).as(Double.class));
	               sb.append("Pr[<");
	               if (p.startsWith("=")) {
	                  sb.append("=");
	               }
	               sb.append(time);
	               sb.append(p.substring(p.indexOf("]")));
	            }
	            probabilisticFormula = sb.toString();
	         }

			
			this.monitor.setStatus("Analyzing model with UPPAAL");
			this.monitor.setPercentCompleted(-1);

			// analyse model
			final SMCResult result;
			
			if (remoteUppaal.isSelected()) {
				UPPAALClient client = new UPPAALClient(serverName.getText(), Integer.parseInt(serverPort.getText()));
				result = client.analyzeSMC(model, probabilisticFormula);
			} else {
				result = new UppaalModelAnalyserSMC/*FasterConcrete*/(monitor, meStesso).analyzeSMC(model, probabilisticFormula);
			}
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(Cytoscape.getDesktop(), result.toString(), "Result", JOptionPane.INFORMATION_MESSAGE);
				}
			});
			
		}
		
		/**
		 * Perform a simulation analysis. Translate the user-set number of real-life minutes
		 * for the length of the simulation, and obtain all input data for the model engine,
		 * based on the control the user has set (average, N simulation, StdDev, etc).
		 * When the analysis is done, display the obtained SimpleLevelResult on a ResultPanel
		 * @param model
		 * @throws Exception
		 */
		private void performNormalAnalysis(final Model model) throws Exception {

			int nMinutesToSimulate = 0;
			try {
				nMinutesToSimulate = Integer.parseInt(timeToFormula.getValue().toString());
			} catch (Exception ex) {
				throw new Exception("Unable to understand the number of minutes requested for the simulation.");
			}
				/*(int)(timeTo * model.getProperties().get(SECONDS_PER_POINT).as(Double.class) / 60);
			String inputTime = JOptionPane.showInputDialog(Cytoscape.getDesktop(), "Up to which time (in real-life MINUTES)?", nMinutesToSimulate);
			if (inputTime != null) {
				try {
					nMinutesToSimulate = Integer.parseInt(inputTime);
				} catch (Exception ex) {
					//the default value is still there, so nothing to change
				}
			} else {
				return;
			}*/
			
			timeTo = (int)(nMinutesToSimulate * 60.0 / model.getProperties().get(SECONDS_PER_POINT).as(Double.class));
			scale = (double)nMinutesToSimulate / timeTo;
			
			//this.monitor.setStatus("Analyzing model with UPPAAL");
			this.monitor.setPercentCompleted(-1);

			// composite the analyser (this should be done from
			// configuration)
			//ModelAnalyser<LevelResult> analyzer = new UppaalModelAnalyser(new VariablesInterpreter(), new VariablesModel());

			// analyse model
			final SimpleLevelResult result;
			
			if (remoteUppaal.isSelected()) {
				UPPAALClient client = new UPPAALClient(serverName.getText(), Integer.parseInt(serverPort.getText()));
				int nSims = 1;
				if (nSimulationRuns.isEnabled()) {
					try {
						nSims = Integer.parseInt(nSimulationRuns.getValue().toString());
					} catch (Exception e) {
						throw new Exception("Unable to understand the number of requested simulations.");
					}
				} else {
					nSims = 1;
				}
				monitor.setStatus("Forwarding the request to the server " + serverName.getText() + ":" + serverPort.getText());
				result = client.analyze(model, timeTo, nSims, computeAvgStdDev.isSelected(), overlayPlot.isSelected());
			} else {
				//ModelAnalyser<LevelResult> analyzer = new UppaalModelAnalyser(new VariablesInterpreter(), new VariablesModel());
				//result = analyzer.analyze(model, timeTo);
				if (nSimulationRuns.isEnabled()) {
					int nSims = 0;
					try {
						nSims = Integer.parseInt(nSimulationRuns.getValue().toString());
					} catch (Exception e) {
						throw new Exception("Unable to understand the number of requested simulations.");
					}
					if (computeAvgStdDev.isSelected()) {
						result = new ResultAverager(monitor, meStesso).analyzeAverage(model, timeTo, nSims);
					} else if (overlayPlot.isSelected()) {
						result = new ResultAverager(monitor, meStesso).analyzeOverlay(model, timeTo, nSims);
					} else {
						result = null;
					}
				} else {
					result = new UppaalModelAnalyserSMC/*FasterSymbolicConcretizedFaster*/(monitor, meStesso).analyze(model, timeTo);
				}
			}
			
			/*CsvWriter csvWriter = new CsvWriter();
			csvWriter.writeCsv("/tmp/test.csv", model, result);*/
			if (result == null) {
				throw new Exception("No result was obtained.");
			}
			if (result.getReactantIds().isEmpty()) {
				throw new Exception("No reactants selected for plot, or no reactants present in the result");
			} else {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						final InatResultPanel resultViewer = new InatResultPanel(model, result, scale, Cytoscape.getCurrentNetwork());
						resultViewer.addToPanel(Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST));
					}
				});
			}
		}

		@Override
		public void setTaskMonitor(TaskMonitor monitor) throws IllegalThreadStateException {
			this.monitor = monitor;
		}

	}
}
