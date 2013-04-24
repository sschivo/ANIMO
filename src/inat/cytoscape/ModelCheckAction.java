package inat.cytoscape;

import inat.InatBackend;
import inat.analyser.SMCResult;
import inat.analyser.uppaal.UppaalModelAnalyserSMC;
import inat.analyser.uppaal.modelchecking.PathFormula;
import inat.model.Model;
import inat.util.XmlConfiguration;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import cytoscape.Cytoscape;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;

public class ModelCheckAction extends InatActionTask {
	private static final long serialVersionUID = 1147435660037202034L;
	private InatActionTask meStesso; //Myself
	private PathFormula formulaToCheck;
	private PrintStream logStream;
	
	public ModelCheckAction() {
		super("<html>Model <br/>checking...</html>");
		meStesso = this;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		final RunTask task = new RunTask();
		
		// Configure JTask Dialog Pop-Up Box
		final JTaskConfig jTaskConfig = new JTaskConfig();
		jTaskConfig.setOwner(Cytoscape.getDesktop());
		
		jTaskConfig.displayStatus(true);
		jTaskConfig.setAutoDispose(true);
		jTaskConfig.displayCancelButton(true);
		jTaskConfig.displayTimeElapsed(true);
		jTaskConfig.setModal(true);
		jTaskConfig.displayTimeRemaining(true);
		
		final long startTime = System.currentTimeMillis();
		Date now = new Date(startTime);
		Calendar nowCal = Calendar.getInstance();
		File logFile = null;
		final PrintStream oldErr = System.err;
		try {
			if (UppaalModelAnalyserSMC.areWeUnderWindows()) {
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
		
		formulaToCheck = null;
		final JDialog dialog = new JDialog(Cytoscape.getDesktop(), "Model checking templates", Dialog.ModalityType.APPLICATION_MODAL);
		Box boxContent = new Box(BoxLayout.Y_AXIS);
		final PathFormula pathFormula = new PathFormula();
		boxContent.add(pathFormula);
		Box buttonsBox = new Box(BoxLayout.X_AXIS);
		buttonsBox.add(Box.createGlue());
		JButton doWork = new JButton("Start model checking");
		doWork.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				formulaToCheck = pathFormula.getSelectedFormula();
				dialog.dispose();
			}
		});
		buttonsBox.add(doWork);
		buttonsBox.add(Box.createGlue());
		boxContent.add(buttonsBox);
		dialog.getContentPane().add(boxContent);
		dialog.pack();
		dialog.setLocationRelativeTo(Cytoscape.getDesktop());
		dialog.setVisible(true);
		
		if (formulaToCheck == null) return; //If the user simply closed the window, the analysis is cancelled
		System.err.println("Checking formula " + formulaToCheck.toHumanReadable());
		
		// Execute Task in New Thread; pops open JTask Dialog Box.
		TaskManager.executeTask(task, jTaskConfig);
		
		long endTime = System.currentTimeMillis();
		
		try {
			System.err.println("Time taken: " + RunAction.timeDifferenceFormat(startTime, endTime));
			System.err.flush();
			System.setErr(oldErr);
			if (logStream != null) {
				logStream.close();
			}
		} catch (Exception ex) {
			
		}
		
	}

	
	private class RunTask implements Task {
		
		private TaskMonitor monitor;
		
		@Override
		public String getTitle() {
			return "ANIMO model checking";
		}
		
		@Override
		public void halt() {
			needToStop = true;
			//Cytoscape.getDesktop().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			//Cytoscape.getDesktop().getNetworkViewManager().getInternalFrame(Cytoscape.getCurrentNetworkView()).setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		
		@Override
		public void run() {
			try {
				needToStop = false;
				//((JDialog)monitor).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				//Cytoscape.getDesktop().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				//Cytoscape.getDesktop().getNetworkViewManager().getInternalFrame(Cytoscape.getCurrentNetworkView()).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				
				
				this.monitor.setStatus("Creating model representation");
				this.monitor.setPercentCompleted(0);
				
				boolean generateTables = false;
				XmlConfiguration configuration = InatBackend.get().configuration();
				String modelType = configuration.get(XmlConfiguration.MODEL_TYPE_KEY, null);
				if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTION_CENTERED_TABLES)) {
					generateTables = true;
				}
				final Model model = Model.generateModelFromCurrentNetwork(this.monitor, null, generateTables);
				
				//Convert the names of the reactants in the formula from their Cytoscape ID to the reactant names in the generated model
				//JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Formula pre-sostituzioni: " + formulaToCheck);
				
				//This is not needed anymore, because:
				// 1- we use the ReactantName class to translate between the Cytoscape ID, canonical name and model ID
				// 2- checking that each reactant in the formula is enabled is not necessary, because only enabled reactants are listed in the StateFormula combo boxes
				/*String formula = formulaToCheck.toString();
				StringTokenizer stk = new StringTokenizer(formula, "" + StateFormula.REACTANT_NAME_DELIMITER);
				StringBuilder newFormula;
				if (formula.charAt(0) == StateFormula.REACTANT_NAME_DELIMITER) { //If the string starts with the chosen token delimiter, StringTokenizer does not return "" as a first string, it simply returns the token after the first delimiter!
					newFormula = new StringBuilder();
				} else {
					newFormula = new StringBuilder(stk.nextToken());
				}
				while (stk.hasMoreTokens()) {
					String n = stk.nextToken();
					//JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "'" + n + "'");
					Reactant r = model.getReactantByCytoscapeID(n);
					if (r == null || r.get(Model.Properties.ENABLED) == null || r.get(Model.Properties.ENABLED).as(Boolean.class) == false) {
						//We have a formula referring to something that is not in the model
						String canonicalName = n;
						if (Cytoscape.getNodeAttributes().hasAttribute(n, Model.Properties.CANONICAL_NAME)) {
							canonicalName = Cytoscape.getNodeAttributes().getStringAttribute(n, Model.Properties.CANONICAL_NAME);
						}
						JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "The reactant \"" + canonicalName + "\" (id: " + n + ") included in the formula is disabled or not in the model", "Error in the formula", JOptionPane.ERROR_MESSAGE);
						throw new InatException("The reactant \"" + canonicalName + "\" (id: " + n + ") included in the formula is disabled or not in the model");
					}
					newFormula.append(r.getId());
					newFormula.append(stk.nextToken());
				}*/
				formulaToCheck.setReactantIDs(model); //This is enough to set the proper model IDs to all reactants in the formula
				//formula = newFormula.toString();
				//JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Formula post-sostituzioni: " + formulaToCheck);
				
//				if (formulaToCheck.supportsPriorities()) { //TODO: unfortunately, we were discouraged from using priorities, so their use will be disabled from now on.
//					model.getProperties().let(Model.Properties.MODEL_CHECKING_TYPE).be(Model.Properties.NORMAL_MODEL_CHECKING);
//				} else {
					model.getProperties().let(Model.Properties.MODEL_CHECKING_TYPE).be(Model.Properties.STATISTICAL_MODEL_CHECKING);
//				}
				
				performModelChecking(model, formulaToCheck.toString(), formulaToCheck.toHumanReadable());
				//JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Ho finito l'analisi senza riportare eccezioni!");
				
			} catch (InterruptedException e) {
				//JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Ho finito l'analisi con l'eccezione " + e);
				e.printStackTrace(System.err);
				this.monitor.setException(e, "Analysis cancelled by the user.");
			} catch (Exception e) {
				//JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Ho finito l'analisi con l'eccezione " + e);
				e.printStackTrace(System.err);
				this.monitor.setException(e, "An error occurred while analyzing the network.");
			} catch (Error e) {
				//JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Ho finito l'analisi con l'ERRORE " + e);
				e.printStackTrace(System.err);
				this.monitor.setException(e, "An error occurred while analyzing the network.");
			}
		}
		
		/**
		 * Send the (already translated) formula to the analyser.
		 * Show the result in a message window.
		 * @param model
		 * @throws Exception
		 */
		private void performModelChecking(final Model model, final String formula, final String humanFormula) throws Exception {
			this.monitor.setStatus("Analyzing model with UPPAAL");
			this.monitor.setPercentCompleted(-1);
			
			// analyse model
			final SMCResult result;
			
			/*if (remoteUppaal.isSelected()) {
				UPPAALClient client = new UPPAALClient(serverName.getText(), Integer.parseInt(serverPort.getText()));
				result = client.analyzeSMC(model, probabilisticFormula);
			} else {*/
				result = new UppaalModelAnalyserSMC(monitor, meStesso).analyzeSMC(model, formula);
			//}
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Property \"" + humanFormula + "\"" + System.getProperty("line.separator") + "is " + result.toString(), "Result", JOptionPane.INFORMATION_MESSAGE);
					if (result.getResultType() == SMCResult.RESULT_TYPE_TRACE) { //We have a trace to show
						double scale = model.getProperties().get(Model.Properties.SECONDS_PER_POINT).as(Double.class) / 60.0;
						final InatResultPanel resultViewer = new InatResultPanel(model, result.getLevelResult(), scale, Cytoscape.getCurrentNetwork());
						resultViewer.addToPanel(Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST));
						resultViewer.setTitle(humanFormula + " (" + (result.getBooleanResult()?"True":"False") + ")");
					}
				}
			});
			
		}

		@Override
		public void setTaskMonitor(TaskMonitor monitor) throws IllegalThreadStateException {
			this.monitor = monitor;
		}

	}
}
