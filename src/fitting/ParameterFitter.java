package fitting;
import fitting.multithread.ThreadPool;
import inat.InatBackend;
import inat.analyser.LevelResult;
import inat.analyser.uppaal.UppaalModelAnalyserSMC;
import inat.analyser.uppaal.VariablesModel;
import inat.cytoscape.ComponentTitledBorder;
import inat.cytoscape.LabelledField;
import inat.cytoscape.RunAction;
import inat.exceptions.InatException;
import inat.graph.FileUtils;
import inat.graph.Graph;
import inat.model.Model;
import inat.model.Reactant;
import inat.model.Reaction;
import inat.model.Scenario;
import inat.util.Pair;
import inat.util.Table;
import inat.util.XmlConfiguration;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;

public class ParameterFitter {
	private JFrame window = null;
	private static final String DECIMAL_FORMAT_STRING = "#.####",
								BUTTON_START = "Start",
								BUTTON_CANCEL = "Cancel";
	private HashMap<Reaction, ScenarioFitting> scenarioFittingParameters = null;
	private HashMap<Reactant, ReactantComparison> reactantComparisons = null;
	private Scenario[] scenarios = null;
	private JButton startExecution = null;
	private boolean terminate = false;
	private long nComputations, totalComputations;
	private int currentSettingIndex; //Please notice that currentSettingIndex counts the computation index as we start it (so that we can keep the results ordered), while nComputations counts the number of computations we have finished
	private Vector<AcceptableConfiguration> acceptableConfigurations = null;
	private Vector<AcceptableConfiguration> allConfigurations = null;
	private int timeTo = 120;
	private double scale = 1.0;
	private Model model = null;
	private boolean generateTables = false;
	private String CSV_FILE_NAME = "test.csv"; ///local/schivos/Data_0-120_TNF100_semplificato_con_dichiarazione_solo_MK2_e_JNK1.csva";
	private JProgressBar progress = null;
	private long startTime = 0;
	private JFormattedTextField numberOfParallelExecutions = null;
	private ThreadPool pool = null;
	private final String WINDOW_TITLE = "Acceptable configurations";
	private JFrame acceptableGraphsWindow = new JFrame(WINDOW_TITLE);
	
	
	/*private class FormattedField extends JFormattedTextField {
		private static final long serialVersionUID = 6070789097404839599L;
		private JRadioButton boss = null;
		
		public FormattedField(NumberFormat format, JRadioButton boss) { //I activate/deactivate following the state of my JRadioButton boss
			super(format);
			this.boss = boss;
		}
		
		public void setEnabled(boolean enabled) {
			if (enabled) {
				super.setEnabled(boss.isSelected());
			} else {
				super.setEnabled(enabled);
			}
		}
	}*/
	
	public ParameterFitter() {
		try {
			this.scenarioFittingParameters = new HashMap<Reaction, ScenarioFitting>();
			this.reactantComparisons = new HashMap<Reactant, ReactantComparison>();
			this.scenarios = Scenario.sixScenarios; //scenarios;
		} catch (Exception e) {
			JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Error: " + e);
		}
	}
	
	public void setTimeParameters(Integer timeLimit, Double timeScale) {
		this.timeTo = timeLimit;
	}
	
	public void setComparisonCSV(String csvFileName) {
		this.CSV_FILE_NAME = csvFileName;
	}
	
	public void addReaction(Reaction reaction) {
		ScenarioCfg cfg = reaction.getScenarioCfg();
		scenarioFittingParameters.put(reaction, new ScenarioFitting(cfg));
		for (String parName : cfg.getParameters().keySet()) {
			scenarioFittingParameters.get(reaction).setParameterSetting(parName, new ParameterSetting(parName, cfg.getParameters().get(parName)));
			scenarioFittingParameters.get(reaction).setScenarioCfg(reaction.getScenarioCfg());
		}
	}
	
	public void setFixedParameter(Reaction reaction, String parName, Double fixedValue) {
		ParameterSetting parSetting = scenarioFittingParameters.get(reaction).getParameterSetting(parName);
		if (parSetting != null) {
			parSetting.setFixed(fixedValue);
		} else {
			parSetting = new ParameterSetting(parName, fixedValue);
			scenarioFittingParameters.get(reaction).setParameterSetting(parName, parSetting);
		}
	}
	
	public void setVariableParameter(Reaction reaction, String parName, Double min, Double max, Double increase) {
		ParameterSetting parSetting = scenarioFittingParameters.get(reaction).getParameterSetting(parName);
		if (parSetting != null) {
			parSetting.setVariable(min, max, increase);
		} else {
			parSetting = new ParameterSetting(parName, min, max, increase);
			scenarioFittingParameters.get(reaction).setParameterSetting(parName, parSetting);
		}
	}

	public void setReactantComparison(Reactant reactant, ReactantComparison comparison) {
		reactantComparisons.put(reactant, comparison);
	}
	
	public ReactantComparison getReactantComparison(Reactant reactant) {
		return reactantComparisons.get(reactant);
	}
	
	public void showWindow(boolean exitOnClose, int nMinutesToSimulate) {
		//System.err.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
		window = new JFrame("Parameter fitter");
		model = null;
		generateTables = false;
		XmlConfiguration configuration = InatBackend.get().configuration();
		String modelType = configuration.get(XmlConfiguration.MODEL_TYPE_KEY, null);
		if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTION_CENTERED_TABLES)) {
			generateTables = true;
		}
		try {
			//System.err.println("Costruisco il modello");
			model = Model.generateModelFromCurrentNetwork(null, (int)Math.round(nMinutesToSimulate), generateTables);

			//System.err.println("Modello costruito");
			
			timeTo = (int)Math.round(nMinutesToSimulate * 60.0 / model.getProperties().get(Model.Properties.SECONDS_PER_POINT).as(Double.class));
			scale = (double)nMinutesToSimulate / timeTo;
			
		} catch (InatException e1) {
			e1.printStackTrace();
		}
		final JPanel panelReactions = new JPanel(new GridLayout2((int)Math.ceil(model.getReactionCollection().size()/2.0), 2, 30, 30));
		for (Reaction r : model.getReactionCollection()) {
			if (!r.getEnabled()) continue;
			if (r.getScenarioCfg() == null) {
				//System.err.println("La reazione " + r.getName() + " non ha un ScenarioCfg associato e la salto!");
				continue;
			} else {
				/*System.err.println("La reazione " + r.getName() + " ha il suo ScenarioCfg, che mi dice");
				ScenarioCfg c = r.getScenarioCfg();
				for (String pn : c.getParameters().keySet()) {
					System.err.print(" " + pn + " = " + c.getParameters().get(pn));
				}
				System.err.println();*/
			}
			addReaction(r);
			final JCheckBox checkReaction = new JCheckBox(r.getName());
			final Reaction reaction = r;
			final JPanel placeForParameters = new JPanel();
			checkReaction.setSelected(false);
			checkReaction.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					placeForParameters.removeAll();
					if (checkReaction.isSelected()) {
						addReactionVariableParameters(placeForParameters, checkReaction, reaction);
						updateTotalComputations();
					} else {
						addReactionFixedParameters(placeForParameters, checkReaction, reaction);
						updateTotalComputations();
					}
					window.validate();
					window.repaint();
				}
			});
			addReactionFixedParameters(placeForParameters, checkReaction, reaction);
			panelReactions.add(placeForParameters);
		}
		JScrollPane scroll = new JScrollPane(panelReactions);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		JComponent comparisonPanel = createReactionComparisonPanel(model);
		JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, comparisonPanel);
		mainSplit.setDividerLocation(0.7);
		mainSplit.setResizeWeight(0.7);
		window.getContentPane().add(mainSplit, BorderLayout.CENTER);
		startExecution = new JButton(BUTTON_START);
		startExecution.addActionListener(new ActionListener() {
			
			public void showAcceptableGraphsWindow() {
				//System.err.println("Ecco le configurazioni accettabili (" + acceptableConfigurations.size() + "):");
				//acceptableGraphsWindow.getContentPane().setLayout(new GridLayout(2,2));
				acceptableGraphsWindow.getContentPane().setLayout(new BorderLayout());
				acceptableGraphsWindow.getContentPane().removeAll();
				final int itemsPerPage = 6;
				final Vector<Container> pages = new Vector<Container>();
				final JLabel showPageNumber = new JLabel("Page 1/1");
				int countItems = 0;
				int countPages = 0;
				DecimalFormat decimalFormat = new DecimalFormat("##0.##E0");
				for (final AcceptableConfiguration acceptableConfiguration : acceptableConfigurations) {
					if (acceptableConfiguration == null) continue;
					try {
						Graph g = new Graph();
						
						Map<String, String> seriesNameMapping = new HashMap<String, String>();
						for (String r : acceptableConfiguration.getResult().getReactantIds()) {
							String name = null;
							//String stdDevReactantName = null;
							if (model.getReactant(r) != null) { //we can also refer to a name not present in the reactant collection
								name = model.getReactant(r).get(Model.Properties.ALIAS).as(String.class); //if an alias is set, we prefer it
								if (name == null) {
									name = model.getReactant(r).get(Model.Properties.REACTANT_NAME).as(String.class);
								}
							}
							seriesNameMapping.put(r, name);
						}
						
						Vector<String> shownReactants = new Vector<String>(),
									   filteredReactants = new Vector<String>();
						LevelResult res = acceptableConfiguration.getResult();
						for (Reactant r : reactantComparisons.keySet()) {
							shownReactants.add(r.getName());
						}
						for (Reactant r : model.getReactantCollection()) {
							if (shownReactants.contains(r.getName()) || r.get(Model.Properties.PLOTTED).as(Boolean.class)) {
								filteredReactants.add(r.getId());
							}
						}
						res = res.filter(filteredReactants);
						g.parseLevelResult(res, seriesNameMapping, scale, shownReactants);
						g.setXSeriesName("Time (min)");
						g.setYLabel("Protein activity (a. u.)");
						if (!model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).isNull()) { //if we find a maximum value for activity levels, we declare it to the graph, so that other added graphs (such as experimental data) will be automatically rescaled to match us
							int nLevels = model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
							g.declareMaxYValue(nLevels);
						}
						
						/*for (Reactant reactant : reactantComparisons.keySet()) { //TODO: sarebbe bello fare cosi', nel senso che ciascun reagente si prende i dati dal suo csv personale. Per il momento evitiamo e usiamo lo stesso csv per tutti
							ReactantComparison compare = reactantComparisons.get(reactant);
							Vector<String> namesToCompare = compare.getSeriesNames();
							namesToCompare.add(reactant.getName());
							g.parseCSV(compare.getCsvFile(), namesToCompare); 
						}*/
						Vector<String> namesToCompare = new Vector<String>();
						String csvFile = null;
						for (Reactant r : reactantComparisons.keySet()) {
							ReactantComparison compare = reactantComparisons.get(r);
							if (compare.getCsvFile() != null && csvFile == null) {
								csvFile = compare.getCsvFile();
							}
							namesToCompare.addAll(compare.getSeriesNames());
							namesToCompare.add(r.getName());
						}
						g.parseCSV(csvFile, namesToCompare);
						
						StringBuilder builder = new StringBuilder();
						for (Reaction r : scenarioFittingParameters.keySet()) {
							boolean onlyFixed = true;
							for (ParameterSetting parSetting : scenarioFittingParameters.get(r).getParameterSettings().values()) {
								if (!parSetting.isFixed()) {
									onlyFixed = false;
									break;
								}
							}
							if (onlyFixed) continue;
							builder.append(r.getName() + " [");
							ScenarioFitting scnFitting = scenarioFittingParameters.get(r);
							for (String parName : scnFitting.getParameterSettings().keySet()) {
								ParameterSetting parSetting = scnFitting.getParameterSettings().get(parName);
								if (!parSetting.isFixed()) {
									builder.append(parName + "=" + decimalFormat.format(acceptableConfiguration.getScenarioConfigurations().get(r).getParameters().get(parName)) + ",");
								}
							}
							builder.append("]; ");
						}
						final String title = builder.toString();
						Box graphBox = new Box(BoxLayout.Y_AXIS);
						graphBox.add(g);
						JButton accept = new JButton("I want this");
						accept.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								if (JOptionPane.showConfirmDialog(window, "Are you sure that you want this graph?\n" + title, "Confirm parameter setting choice", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
								//JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Dobbiamo impostare i parametri!");
								CyAttributes edgeAttr = Cytoscape.getEdgeAttributes();
								for (Reaction r : scenarioFittingParameters.keySet()) {
									ScenarioFitting scnFitting = scenarioFittingParameters.get(r);
									for (String parName : scnFitting.getParameterSettings().keySet()) {
										ParameterSetting parSetting = scnFitting.getParameterSettings().get(parName);
										if (parSetting.isFixed()) {
											//r.getScenarioCfg().getParameters().put(parName, parSetting.getFixedValue());
											edgeAttr.setAttribute(r.get(Model.Properties.CYTOSCAPE_ID).as(String.class), parName, parSetting.getFixedValue());
										} else {
											//r.getScenarioCfg().getParameters().put(parName, acceptableConfiguration.getScenarioConfigurations().get(r).getParameters().get(parName));
											edgeAttr.setAttribute(r.get(Model.Properties.CYTOSCAPE_ID).as(String.class), parName, acceptableConfiguration.getScenarioConfigurations().get(r).getParameters().get(parName));
										}
									}
								}
								Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);
								/*for (Reaction r : scenarioFittingParameters.keySet()) {
									ScenarioFitting scnFitting = scenarioFittingParameters.get(r);
									for (String parName : scnFitting.getParameterSettings().keySet()) {
										ParameterSetting parSetting = scnFitting.getParameterSettings().get(parName);
										if (parSetting.isFixed()) {
											r.getScenarioCfg().getParameters().put(parName, parSetting.getFixedValue());
										} else {
											r.getScenarioCfg().getParameters().put(parName, acceptableConfiguration.getScenarioConfigurations().get(r).getParameters().get(parName));
										}
									}
								}
								//sporco trucco per fargli ricalcolare tutte le reazioni: non serve dire che fa accapponare la pelle
								model.changeLevels(model.getNLevels(), scenarios);*/
								acceptableGraphsWindow.dispose();
								window.dispose();
								JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "The selected parameters have been correctly set in the network");
							}
						});
						Box acceptBox = new Box(BoxLayout.X_AXIS);
						acceptBox.add(new JLabel(acceptableConfiguration.getErrorEstimation() + "  "));
						acceptBox.add(accept);
						graphBox.add(acceptBox);
						//acceptableGraphsWindow.getContentPane().add(new LabelledField(title, graphBox, title));
						if (countPages >= pages.size()) {
							pages.add(new JPanel());
							pages.elementAt(countPages).setLayout(new GridLayout(2,itemsPerPage/2));
						}
						pages.elementAt(countPages).add(new LabelledField(title, graphBox, title));
						countItems++;
						if (countItems % itemsPerPage == 0) {
							countPages++;
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				}
				if (pages.size() > 0) {
					acceptableGraphsWindow.getContentPane().add(pages.elementAt(0), BorderLayout.CENTER);
					JButton prev = new JButton("<--");
					prev.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							//System.err.println("Previous page");
							for (Component c : acceptableGraphsWindow.getContentPane().getComponents()) {
								if (pages.contains(c)) {
									//System.err.println("Found that " + c + " is in the list of pages");
									int idx = pages.indexOf(c);
									idx--;
									if (idx < 0) {
										idx = pages.size() - 1;
									}
									showPageNumber.setText("Page " + (idx+1) + "/" + pages.size());
									acceptableGraphsWindow.getContentPane().remove(c);
									acceptableGraphsWindow.getContentPane().add(pages.elementAt(idx), BorderLayout.CENTER);
									acceptableGraphsWindow.validate();
									acceptableGraphsWindow.repaint();
									break;
								}
							}
						}
					});
					JButton next = new JButton("-->");
					next.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							//System.err.println("Next page");
							for (Component c : acceptableGraphsWindow.getContentPane().getComponents()) {
								if (pages.contains(c)) {
									//System.err.println("Found that " + c + " is in the list of pages");
									int idx = pages.indexOf(c);
									idx++;
									if (idx >= pages.size()) {
										idx = 0;
									}
									showPageNumber.setText("Page " + (idx+1) + "/" + pages.size());
									acceptableGraphsWindow.getContentPane().remove(c);
									acceptableGraphsWindow.getContentPane().add(pages.elementAt(idx), BorderLayout.CENTER);
									acceptableGraphsWindow.validate();
									acceptableGraphsWindow.repaint();
									break;
								}
							}
						}
					});
					Box boxButtons = new Box(BoxLayout.X_AXIS);
					boxButtons.add(prev);
					boxButtons.add(Box.createGlue());
					showPageNumber.setText("Page 1/" + pages.size());
					boxButtons.add(showPageNumber);
					boxButtons.add(Box.createGlue());
					boxButtons.add(next);
					acceptableGraphsWindow.getContentPane().add(boxButtons, BorderLayout.SOUTH);
				}
				final Box filterButtonsBox = new Box(BoxLayout.X_AXIS);
				filterButtonsBox.add(Box.createGlue());
				JButton filter = new JButton("Filter");
				final Vector<Pair<JTextField, ReactantComparison>> comparisonFieldParameters = new Vector<Pair<JTextField, ReactantComparison>>();
				for (Reactant r : reactantComparisons.keySet()) {
					ReactantComparison compare = reactantComparisons.get(r);
					JTextField comparisonError = new JTextField("" + compare.getMaxError());
					comparisonFieldParameters.add(new Pair<JTextField, ReactantComparison>(comparisonError, compare));
					filterButtonsBox.add(new LabelledField(r.getName() + " error", comparisonError));
				}
				filter.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						for (Pair<JTextField, ReactantComparison> pair : comparisonFieldParameters) {
							double value = 0;
							try {
								value = Double.parseDouble(pair.first.getText());
							} catch (Exception ex) {
								value = pair.second.getMaxError();
							}
							pair.second.setMaxError(value);
						}
						acceptableConfigurations.removeAllElements();
						DecimalFormat decimalFormat = new DecimalFormat("##0.####");
						for (AcceptableConfiguration tryConfiguration : allConfigurations) {
							if (tryConfiguration == null) continue;
							try {
								Pair<Boolean, Double> comparisonResult = compareResults(tryConfiguration.getResult());
								if (comparisonResult.first) {
									acceptableConfigurations.add(tryConfiguration);
									tryConfiguration.setErrorEstimation("Max abs diff: " + decimalFormat.format(comparisonResult.second));
								}
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}
						showAcceptableGraphsWindow();
					}
				});
				filterButtonsBox.add(Box.createGlue());
				filterButtonsBox.add(filter);
				JButton showAll = new JButton("Show all");
				showAll.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						acceptableConfigurations.removeAllElements();
						DecimalFormat decimalFormat = new DecimalFormat("##0.####");
						for (AcceptableConfiguration tryConfiguration : allConfigurations) {
							if (tryConfiguration == null) continue;
							try {
								Pair<Boolean, Double> comparisonResult = compareResults(tryConfiguration.getResult());
								acceptableConfigurations.add(tryConfiguration);
								tryConfiguration.setErrorEstimation("Max abs diff: " + decimalFormat.format(comparisonResult.second));
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}
						showAcceptableGraphsWindow();
					}
				});
				filterButtonsBox.add(Box.createGlue());
				filterButtonsBox.add(showAll);
				acceptableGraphsWindow.getContentPane().add(filterButtonsBox, BorderLayout.NORTH);
				acceptableGraphsWindow.setBounds(window.getBounds());
				int actuallyAccepted = 0;
				for (int i=0;i<acceptableConfigurations.size();i++) {
					if (acceptableConfigurations.get(i) != null) actuallyAccepted++;
				}
				acceptableGraphsWindow.setTitle(WINDOW_TITLE + ": " + allConfigurations.size() + " configurations tried, " + actuallyAccepted + " shown.");
				acceptableGraphsWindow.validate();
				acceptableGraphsWindow.getContentPane().validate();
				acceptableGraphsWindow.setVisible(true);
			}
			
			public void actionPerformed(ActionEvent e) {
				if (startExecution.getText().equals(BUTTON_START)) {
					new Thread() {
						public void run() {
							startExecution.setText(BUTTON_CANCEL);
							progress.setValue(progress.getMinimum());
							startTime = System.currentTimeMillis();
							terminate = false;
							acceptableConfigurations = new Vector<AcceptableConfiguration>();
							allConfigurations = new Vector<AcceptableConfiguration>();
							acceptableConfigurations.setSize((int) totalComputations);
							allConfigurations.setSize((int) totalComputations);
							int nParallelExecutions = 2*(Runtime.getRuntime().availableProcessors() -1);
							if (numberOfParallelExecutions.getText() != null) {
								try {
									nParallelExecutions = Integer.parseInt(numberOfParallelExecutions.getText());
								} catch (Exception e) {
									//nothing to do
								}
							}
							pool = new ThreadPool(nParallelExecutions);
							parameterSweep(pool);
							showAcceptableGraphsWindow();
							startExecution.setText(BUTTON_START);
						}
					}.start();
				} else {
					startExecution.setText(BUTTON_START);
					terminate = true;
					pool.terminateAll();
				}
			}
		});
		progress = new JProgressBar(0, 100);
		progress.setStringPainted(true);
		progress.setString("Messages");
		Box buttonsBox = new Box(BoxLayout.X_AXIS);
		buttonsBox.add(Box.createGlue());
		buttonsBox.add(progress);
		buttonsBox.add(Box.createGlue());
		buttonsBox.add(startExecution);
		window.getContentPane().add(buttonsBox, BorderLayout.SOUTH);
		//window.pack(); //NON FARLO MAI! SE LO FAI CRASHA X!!! ZIO GUSTAVO
		window.setBounds(0,0,1160,607);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		window.setBounds((int)((screenSize.getWidth() - window.getWidth()) / 2), (int)((screenSize.getHeight() - window.getHeight()) / 2), window.getWidth(), window.getHeight());
		if (exitOnClose) {
			window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		} else {
			window.addWindowListener(new WindowListener() {
				@Override
				public void windowClosed(WindowEvent e) {
				}

				@Override
				public void windowActivated(WindowEvent e) {
				}

				@Override
				public void windowClosing(WindowEvent e) {
					acceptableGraphsWindow.dispose();
					window.dispose();
				}

				@Override
				public void windowDeactivated(WindowEvent e) {
				}

				@Override
				public void windowDeiconified(WindowEvent e) {
				}

				@Override
				public void windowIconified(WindowEvent e) {
				}

				@Override
				public void windowOpened(WindowEvent e) {
				}
			});
		}
		window.setVisible(true);
	}
	
	private void updateTotalComputations() {
		if (scenarioFittingParameters == null || progress == null) return;
		totalComputations = 1;
		for (ScenarioFitting fitting : scenarioFittingParameters.values()) {
			for (ParameterSetting setting : fitting.getParameterSettings().values()) {
				if (!setting.isFixed()) {
					Double min = setting.getMin(),
						   max = setting.getMax(),
						   inc = setting.getIncrease();
					long nSteps = Math.round((max - min) / inc + 1);
					totalComputations *= nSteps;
				}
			}
		}
		NumberFormat formatter = new DecimalFormat("#,###");
		progress.setValue(progress.getMinimum());
		progress.setString(formatter.format(totalComputations) + " total runs needed");
	}
	
	private void addReactionFixedParameters(JPanel panel, JCheckBox check, final Reaction reaction) {
		final Box parametersBox = new BoxAutoenabler(BoxLayout.Y_AXIS);
		parametersBox.add(Box.createGlue());
		ScenarioCfg cfg = reaction.getScenarioCfg();
		if (cfg == null) return;
		HashMap<String, Double> parameters = cfg.getParameters();
		DecimalFormat format = new DecimalFormat(DECIMAL_FORMAT_STRING);
		format.setMinimumFractionDigits(5);
		for (final String parName : parameters.keySet()) {
			final JFormattedTextField paramValue = new JFormattedTextField(format);
			paramValue.setValue(parameters.get(parName));
			Dimension prefSize = paramValue.getPreferredSize();
			prefSize.width *= 1.5;
			paramValue.setPreferredSize(prefSize);
			paramValue.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void changedUpdate(DocumentEvent e) {
					setFixedParameter(reaction, parName, new Double(paramValue.getValue().toString()));
					updateTotalComputations();
				}

				@Override
				public void insertUpdate(DocumentEvent e) {
					changedUpdate(e);
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					changedUpdate(e);
				}
			});
			setFixedParameter(reaction, parName, new Double(paramValue.getValue().toString()));
			updateTotalComputations();
			parametersBox.add(new LabelledField(parName, paramValue));
		}
		
		parametersBox.add(Box.createGlue());
		Box boxParameter = new Box(BoxLayout.X_AXIS);
		boxParameter.add(Box.createHorizontalStrut(check.getPreferredSize().width/2));
		boxParameter.add(parametersBox);
		boxParameter.add(Box.createHorizontalStrut(check.getPreferredSize().width/2));
		boxParameter.setBorder(new ComponentTitledBorder(check, boxParameter, BorderFactory.createEtchedBorder()));
		panel.add(boxParameter);
		parametersBox.setEnabled(true);
	}
	
	private void addReactionVariableParameters(JPanel panel, JCheckBox check, final Reaction reaction) {
		final Box parametersBox = new BoxAutoenabler(BoxLayout.Y_AXIS);
		parametersBox.add(Box.createGlue());
		ScenarioCfg cfg = reaction.getScenarioCfg();
		if (cfg == null) return;
		HashMap<String, Double> parameters = cfg.getParameters();
		DecimalFormat format = new DecimalFormat(DECIMAL_FORMAT_STRING);
		format.setMinimumFractionDigits(5);
		for (final String parName : parameters.keySet()) {
			Box parameterBox = new Box(BoxLayout.X_AXIS);
			parameterBox.add(Box.createGlue());
			final JFormattedTextField minValue = new JFormattedTextField(format);
			final JFormattedTextField maxValue = new JFormattedTextField(format);
			final JFormattedTextField incrementValue = new JFormattedTextField(format);
			minValue.setValue(parameters.get(parName)/10.0);
			Dimension prefSize = minValue.getPreferredSize();
			prefSize.width *= 1.5;
			minValue.setPreferredSize(prefSize);
			minValue.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void changedUpdate(DocumentEvent e) {
					setVariableParameter(reaction, parName, new Double(minValue.getValue().toString()), new Double(maxValue.getValue().toString()), new Double(incrementValue.getValue().toString()));
					updateTotalComputations();
				}

				@Override
				public void insertUpdate(DocumentEvent e) {
					changedUpdate(e);
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					changedUpdate(e);
				}
			});
			parameterBox.add(new LabelledField("Min", minValue));
			maxValue.setValue(parameters.get(parName));
			prefSize = maxValue.getPreferredSize();
			prefSize.width *= 1.5;
			maxValue.setPreferredSize(prefSize);
			maxValue.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void changedUpdate(DocumentEvent e) {
					setVariableParameter(reaction, parName, new Double(minValue.getValue().toString()), new Double(maxValue.getValue().toString()), new Double(incrementValue.getValue().toString()));
					updateTotalComputations();
				}

				@Override
				public void insertUpdate(DocumentEvent e) {
					changedUpdate(e);
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					changedUpdate(e);
				}
			});
			parameterBox.add(new LabelledField("Max", maxValue));
			incrementValue.setValue(parameters.get(parName)/10.0);
			prefSize = incrementValue.getPreferredSize();
			prefSize.width *= 1.5;
			incrementValue.setPreferredSize(prefSize);
			incrementValue.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void changedUpdate(DocumentEvent e) {
					setVariableParameter(reaction, parName, new Double(minValue.getValue().toString()), new Double(maxValue.getValue().toString()), new Double(incrementValue.getValue().toString()));
					updateTotalComputations();
				}

				@Override
				public void insertUpdate(DocumentEvent e) {
					changedUpdate(e);
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					changedUpdate(e);
				}
			});
			parameterBox.add(new LabelledField("Increment", incrementValue));
			parameterBox.add(Box.createGlue());
			parametersBox.add(new LabelledField(parName, parameterBox));
			setVariableParameter(reaction, parName, new Double(minValue.getValue().toString()), new Double(maxValue.getValue().toString()), new Double(incrementValue.getValue().toString()));
			updateTotalComputations();
		}
		parametersBox.add(Box.createGlue());
		Box boxParameter = new Box(BoxLayout.Y_AXIS);
		boxParameter.add(parametersBox);
		boxParameter.setBorder(new ComponentTitledBorder(check, boxParameter, BorderFactory.createEtchedBorder()));
		panel.add(boxParameter);
		
	}
	
	public JComponent createReactionComparisonPanel(final Model model) {
		try {
			final JPanel comparisonPanel = new JPanel();
			final Box comparisonBox = new Box(BoxLayout.Y_AXIS);
			comparisonBox.add(Box.createGlue());
			numberOfParallelExecutions = new JFormattedTextField(2*(Runtime.getRuntime().availableProcessors() -1));
			final JTextField csvFileName = new JTextField(15);
			csvFileName.setText(CSV_FILE_NAME);
			File f = new File(CSV_FILE_NAME);
			if (f.exists()) {
				BufferedReader is = new BufferedReader(new FileReader(f));
				String firstLine = is.readLine();
				StringTokenizer tritatutto = new StringTokenizer(firstLine, ",");
				int nColonne = tritatutto.countTokens();
				final String[] graphNames = new String[nColonne - 1];
				tritatutto.nextToken(); //il primo � la X (tempo)
				for (int i=0;i<graphNames.length;i++) {
					graphNames[i] = tritatutto.nextToken().replace('\"',' ');
				}
				for (Reactant reactant : model.getReactantCollection()) {
					final Box reagentBox = new Box(BoxLayout.X_AXIS);
					reagentBox.add(Box.createGlue());
					Vector<String> selectedSeries = new Vector<String>();
					selectedSeries.add(graphNames[0]);
					final ReactantComparison comparison = new ReactantComparison(CSV_FILE_NAME, selectedSeries, 0.2);
					final JCheckBox checkBox = new JCheckBox(reactant.getName());
					final JComboBox<String> comboBox = new JComboBox<String>(graphNames);
					final JFormattedTextField error = new JFormattedTextField(new Double(0.2));
					final Reactant cherottura = reactant;
					checkBox.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							comboBox.setEnabled(checkBox.isSelected());
							error.setEnabled(checkBox.isSelected());
							if (checkBox.isEnabled()) {
								reactantComparisons.put(cherottura, comparison);
							} else {
								reactantComparisons.remove(cherottura);
							}
						}
					});
					comboBox.setEnabled(false);
					error.setEnabled(false);
					comboBox.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							Vector<String> seriesNames = new Vector<String>();
							seriesNames.add(comboBox.getSelectedItem().toString());
							comparison.setSeriesNames(seriesNames);
						}
					});
					error.getDocument().addDocumentListener(new DocumentListener() {
						@Override
						public void changedUpdate(DocumentEvent e) {
							comparison.setMaxError(Double.parseDouble(error.getValue().toString()));
						}
		
						@Override
						public void insertUpdate(DocumentEvent e) {
							changedUpdate(e);
						}
		
						@Override
						public void removeUpdate(DocumentEvent e) {
							changedUpdate(e);
						}
					});
					reagentBox.add(checkBox);
					reagentBox.add(Box.createGlue());
					reagentBox.add(comboBox);
					reagentBox.add(Box.createGlue());
					reagentBox.add(error);
					reagentBox.add(Box.createGlue());
					comparisonBox.add(reagentBox);
				}
				is.close();
			}
			
			csvFileName.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void changedUpdate(DocumentEvent e) {
					File f = new File(csvFileName.getText());
					try {
						BufferedReader is = new BufferedReader(new FileReader(f));
						String firstLine = is.readLine();
						StringTokenizer tritatutto = new StringTokenizer(firstLine, ",");
						int nColonne = tritatutto.countTokens();
						final String[] graphNames = new String[nColonne - 1];
						tritatutto.nextToken(); //il primo � la X (tempo)
						for (int i=0;i<graphNames.length;i++) {
							graphNames[i] = tritatutto.nextToken().replace('\"',' ');
						}
						comparisonBox.removeAll();
						comparisonBox.add(Box.createGlue());
						for (final Reactant reactant : model.getReactantCollection()) {
							final Box reagentBox = new Box(BoxLayout.X_AXIS);
							reagentBox.add(Box.createGlue());
							Vector<String> selectedSeries = new Vector<String>();
							selectedSeries.add(graphNames[0]);
							final ReactantComparison comparison = new ReactantComparison(csvFileName.getText(), selectedSeries, 0.2);
							final JCheckBox checkBox = new JCheckBox(reactant.getName());
							final JComboBox<String> comboBox = new JComboBox<String>(graphNames);
							final JFormattedTextField error = new JFormattedTextField(new Double(0.2));
							checkBox.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									comboBox.setEnabled(checkBox.isSelected());
									error.setEnabled(checkBox.isSelected());
									if (checkBox.isEnabled()) {
										reactantComparisons.put(reactant, comparison);
									} else {
										reactantComparisons.remove(reactant);
									}
								}
							});
							comboBox.setEnabled(false);
							error.setEnabled(false);
							comboBox.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									Vector<String> seriesNames = new Vector<String>();
									seriesNames.add(comboBox.getSelectedItem().toString());
									comparison.setSeriesNames(seriesNames);
								}
							});
							error.getDocument().addDocumentListener(new DocumentListener() {
								@Override
								public void changedUpdate(DocumentEvent e) {
									comparison.setMaxError(Double.parseDouble(error.getValue().toString()));
								}
				
								@Override
								public void insertUpdate(DocumentEvent e) {
									changedUpdate(e);
								}
				
								@Override
								public void removeUpdate(DocumentEvent e) {
									changedUpdate(e);
								}
							});
							reagentBox.add(checkBox);
							reagentBox.add(Box.createGlue());
							reagentBox.add(comboBox);
							reagentBox.add(Box.createGlue());
							reagentBox.add(error);
							reagentBox.add(Box.createGlue());
							comparisonBox.add(reagentBox);
						}
						comparisonBox.add(Box.createGlue());
						comparisonPanel.validate();
						is.close();
					} catch (Exception ex) {
						
					}
				}
	
				@Override
				public void insertUpdate(DocumentEvent e) {
					changedUpdate(e);
				}
	
				@Override
				public void removeUpdate(DocumentEvent e) {
					changedUpdate(e);
				}
			});
			comparisonBox.add(Box.createGlue());
			comparisonPanel.add(comparisonBox, BorderLayout.CENTER);
			Box vTextBox = new Box(BoxLayout.Y_AXIS);
			Box csvBox = new Box(BoxLayout.X_AXIS);
			csvBox.add(csvFileName);
			csvBox.add(Box.createGlue());
			JButton openCSV = new JButton("Browse...");
			openCSV.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String oldFile = csvFileName.getText();
					String file = FileUtils.open(".csv", "CSV file", Cytoscape.getDesktop());
					if (file != null && !file.equals(oldFile)) {
						csvFileName.setText(file);
					}
				}
			});
			csvBox.add(openCSV);
			vTextBox.add(new LabelledField("CSV file name", csvBox));
			vTextBox.add(new LabelledField("Number of parallel executions", numberOfParallelExecutions));
			comparisonPanel.add(vTextBox, BorderLayout.SOUTH);
			JScrollPane scrollComparison = new JScrollPane(comparisonPanel);
			scrollComparison.getVerticalScrollBar().setUnitIncrement(16);
			return scrollComparison;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public void parameterSweep(ThreadPool pool) {
		try {
			for (Reaction reaction : scenarioFittingParameters.keySet()) {
				ScenarioFitting scnFitting = scenarioFittingParameters.get(reaction);
				if (scnFitting == null) continue;
				for (String parName : reaction.getScenarioCfg().getParameters().keySet()) {
					if (scnFitting.getParameterSetting(parName).isFixed() && !scnFitting.getParameterSetting(parName).getFixedValue().equals(reaction.getScenarioCfg().getParameters().get(parName))) {
						reaction.getScenarioCfg().getParameters().put(parName, scnFitting.getParameterSetting(parName).getFixedValue());
					}
				}
			}
			Vector<Pair<ScenarioCfg, ParameterSetting>> parameterList = enumerateVariableParameters();
			updateTotalComputations();
			nComputations = 0;
			currentSettingIndex = -1; //Please notice that nSettings counts the computation index as we start it (so that we can keep the results ordered), while nComputations counts the number of computations we have finished
			visitParameterSettings(parameterList, 0, pool);
			do {
				try {
					Thread.sleep(200);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} while (!pool.isEmpty());
			pool.terminateAll();
		} catch (Exception ex) {
			System.err.println("Error: " + ex);
			ex.printStackTrace();
		}
	}
	
	public Vector<Pair<ScenarioCfg, ParameterSetting>> enumerateVariableParameters() {
		Vector<Pair<ScenarioCfg, ParameterSetting>> list = new Vector<Pair<ScenarioCfg, ParameterSetting>>();
		for (ScenarioFitting scnFitting : scenarioFittingParameters.values()) {
			for (ParameterSetting parSetting : scnFitting.getParameterSettings().values()) {
				if (!parSetting.isFixed()) {
					list.add(new Pair<ScenarioCfg, ParameterSetting>(scnFitting.getScenarioCfg(), parSetting));
				}
			}
		}
		return list;
	}

	public void visitParameterSettings(Vector<Pair<ScenarioCfg, ParameterSetting>> parSettings, int startIndex, ThreadPool pool) {
		if (terminate) {
			return;
		}
		if (startIndex > parSettings.size() -1) { //siamo alla fine: dobbiamo solo fare il conto e casomai ritornare
			/*for (Pair<ScenarioCfg, ParameterSetting> setting : parSettings) {
				if (!setting.second.isFixed()) {
					System.err.print(setting.second.getName() + " = " + setting.first.getParameters().get(setting.second.getName()) + ", ");
				}
			}
			System.err.println();*/
			currentSettingIndex++;
			doCompute(pool);
		} else {
			Pair<ScenarioCfg, ParameterSetting> pair = parSettings.elementAt(startIndex);
			ScenarioCfg cfg = pair.first;
			ParameterSetting parSetting = pair.second;
			long computationsAtMyLevel = Math.round((parSetting.getMax() - parSetting.getMin()) / parSetting.getIncrease() + 1);
			long mySteps = 0;
			for (Double val = parSetting.getMin(); mySteps < computationsAtMyLevel; val += parSetting.getIncrease()) { //the termination condition on the for is correct: we don't want to stop too early just because of some rounding problems
				HashMap<String, Double> params = cfg.getParameters();
				if (params == null) {
					params = new HashMap<String, Double>();
				}
				params.put(parSetting.getName(), val);
				cfg.setParameters(params);
				visitParameterSettings(parSettings, startIndex + 1, pool);
				mySteps++;
			}
		}
	}
	
	public void stampaRapporto (double a, double b, PrintStream out) {
		int rap = (int)(a/b*10.0);
		int i=0;
		out.print("[");
		while(i<rap) {
			out.print("#");
			i++;
		}
		while(i<10) {
			out.print("-");
			i++;
		}
		out.print("]\r");
	}
	
	public void stampaRapporto(double a, double b) {
		if (a < b) {
			long estimation = (long)((System.currentTimeMillis() - startTime) / (a + 1) * (b - a - 1));
			progress.setToolTipText("Estimated remaining time: " + RunAction.timeDifferenceFormat(estimation));
		} else {
			long duration = System.currentTimeMillis() - startTime;
			progress.setToolTipText("Process completed in " + RunAction.timeDifferenceFormat(duration));
		}
		progress.setValue(progress.getMinimum() + (int)(a / b * (progress.getMaximum() - progress.getMinimum())));
		NumberFormat formatter = new DecimalFormat("#,###");
		progress.setString(formatter.format((int)a) + " / " + formatter.format((int)b));
	}
	
	/*private final String TIME_REACHABILITY_QUERY = "E<> (globalTime >= %d)",
						 UPPAAL_FILE_EXTENSION = ".xml",
						 UPPAAL_QUERY_FILE_EXTENSION = ".q",
						 UPPAAL_MODEL_TMP_PREFIX = "iknat_uppaal_model";*/
	
	public void doCompute(ThreadPool pool) {
		final HashMap<Reaction, ScenarioCfg> currentConfiguration = new HashMap<Reaction, ScenarioCfg>();
		double secStepFactor = model.getProperties().get(Model.Properties.SECS_POINT_SCALE_FACTOR).as(Double.class);
		//System.err.println("Ho appena creato un modello nuovo apposta (non so quanto sia saggio), e sto facendo i conti.");
		//System.err.println("Conto ben " + model.getReactions().size() + " reazioni");
		for (Reaction r : model.getReactionCollection()) {
			//System.err.println("La reazione " + r.getName() + " non e' contenuta tra le chiaviche dei setting, perche' non la conosce!");
			if (!scenarioFittingParameters.keySet().contains(r)) continue;
			//System.err.println("Trovata la reazione " + r + " che ha parametri suoi");
			ScenarioCfg cfg = r.getScenarioCfg();
			Scenario scenario = scenarios[cfg.getIndex()];
			//scenario.setReaction((Reaction2) r); //TODO: riferito al TODO di sopra. Questo tratta solo Reaction2, niente Reaction1!!
			for (String parName : scenario.getParameters().keySet()) {
				scenario.setParameter(parName, cfg.getParameters().get(parName));
			}

			Integer unc = 5; //Uncertainty value is now ANIMO-wide (TODO: is that a bit excessive? One would expect the uncertainty to be connected to the model...)
			XmlConfiguration configuration = InatBackend.get().configuration();
			try {
				unc = new Integer(configuration.get(XmlConfiguration.UNCERTAINTY_KEY));
			} catch (NumberFormatException ex) {
				unc = 0;
			}
//			if (r.has(Model.Properties.UNCERTAINTY)) { //We always want to use the uncertainty set in the options, so no specific uncertainty values for single reactions
//				unc = r.get(Model.Properties.UNCERTAINTY).as(Integer.class);
//			}
			double uncertainty = unc;
			int scenarioIdx = cfg.getIndex();
			int nLevelsR1, nLevelsR2;
			double levelsScaleFactor = r.get(Model.Properties.LEVELS_SCALE_FACTOR + "_reaction").as(Double.class);
			
			String cata, reac;
			cata = r.get(Model.Properties.CATALYST).as(String.class); //nodeNameToId.get(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1"));
			reac = r.get(Model.Properties.REACTANT).as(String.class); //nodeNameToId.get(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2"));
			//r.let(CATALYST).be(cata);
			//r.let(REACTANT).be(reac);
			if (!model.getReactant(cata).get(Model.Properties.NUMBER_OF_LEVELS).isNull()) {
				nLevelsR1 = model.getReactant(cata).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
			} else {
				nLevelsR1 = model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
			}
			if (!model.getReactant(reac).get(Model.Properties.NUMBER_OF_LEVELS).isNull()) {
				nLevelsR2 = model.getReactant(reac).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
			} else {
				nLevelsR2 = model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
			}
			
			
			
			boolean activeR1 = true, activeR2 = false;
			boolean reactant1IsDownstream = false, reactant2IsDownstream = true;
			
			if (scenarioIdx == 0 || scenarioIdx == 1) {
				activeR1 = true;
				if (r.get(Model.Properties.INCREMENT).as(Integer.class) >= 0) {
					activeR2 = false;
				} else {
					activeR2 = true;
				}
			} else if (scenarioIdx == 2) {
				activeR1 = r.get(Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E1").as(Boolean.class);
				activeR2 = r.get(Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E2").as(Boolean.class);
				reactant1IsDownstream = r.get(Model.Properties.CATALYST).as(String.class).equals(r.get(Model.Properties.OUTPUT_REACTANT).as(String.class));
				reactant2IsDownstream = r.get(Model.Properties.REACTANT).as(String.class).equals(r.get(Model.Properties.OUTPUT_REACTANT).as(String.class));
			} else {
				//TODO: this should never happen, because we have already made these checks
				activeR1 = activeR2 = true;
			}
			
			if (generateTables) {
				//System.err.println("Calcolo i tempi per la reazione " + nodeAttributes.getStringAttribute(edge.getSource().getIdentifier(), CANONICAL_NAME) + " -- " + nodeAttributes.getStringAttribute(edge.getTarget().getIdentifier(), CANONICAL_NAME) + ", con activeR1 = " + activeR1 + ", activeR2 = " + activeR2);
				List<Double> times = scenario.generateTimes(1 + nLevelsR1, activeR1, reactant1IsDownstream, 1 + nLevelsR2, activeR2, reactant2IsDownstream);
				Table timesLTable = new Table(nLevelsR2 + 1, nLevelsR1 + 1);
				Table timesUTable = new Table(nLevelsR2 + 1, nLevelsR1 + 1);
			
				for (int j = 0; j < nLevelsR2 + 1; j++) {
					for (int k = 0; k < nLevelsR1 + 1; k++) {
						Double t = times.get(j * (nLevelsR1 + 1) + k);
						if (Double.isInfinite(t)) {
							timesLTable.set(j, k, VariablesModel.INFINITE_TIME);
							timesUTable.set(j, k, VariablesModel.INFINITE_TIME);
						} else if (uncertainty == 0) {
							timesLTable.set(j, k, (int)Math.round(secStepFactor * levelsScaleFactor * t));
							timesUTable.set(j, k, (int)Math.round(secStepFactor * levelsScaleFactor * t));
						} else {
							timesLTable.set(j, k, Math.max(0, (int)Math.round(secStepFactor * levelsScaleFactor * t * (1 - uncertainty / 100.0))));
							timesUTable.set(j, k, Math.max(0, (int)Math.round(secStepFactor * levelsScaleFactor * t * (1 + uncertainty / 100.0))));
						}
					}
				}
				r.let(Model.Properties.TIMES_LOWER).be(timesLTable);
				r.let(Model.Properties.TIMES_UPPER).be(timesUTable);
			}
			
			/*System.err.print("Nella configurazione corrente, abbiamo che la reazione " + r.getName() + " ha la configurazione (" + cfg.getIndex());
			for (String pn : cfg.getParameters().keySet()) {
				System.err.print(", " + pn + " = " + cfg.getParameters().get(pn));
			}
			System.err.println(")");*/
			currentConfiguration.put(r, new ScenarioCfg(cfg));
		}
		/*final EstraiValori estrattore = new EstraiValori();*/
		try {
			/*final File uppaalModel = File.createTempFile(UPPAAL_MODEL_TMP_PREFIX, UPPAAL_FILE_EXTENSION);
			String uppaalModelFileName = uppaalModel.getAbsolutePath();
			final File uppaalQuery = new File(uppaalModelFileName.replace(UPPAAL_FILE_EXTENSION, UPPAAL_QUERY_FILE_EXTENSION));
			BufferedWriter out = new BufferedWriter(new FileWriter(uppaalQuery));
			out.write(String.format(TIME_REACHABILITY_QUERY, (int)Double.parseDouble(TIME_TO.toString())));
			out.close();
			uppaalModel.deleteOnExit();
			uppaalQuery.deleteOnExit();
			model.saveUPPAAL(uppaalModel.getAbsolutePath());
			EstraiValori.TIME_SCALE = TIME_SCALE;*/
			final int actualIndexToRemember = currentSettingIndex;
			final DecimalFormat decimalFormat = new DecimalFormat("##0.####");
			synchronized(model) { //TODO: siamo sicuri che questo sia sufficiente a fare in modo che i parametri del modello non vengano modificati da altri processi prima che venga generato il file uppaal? Non credo mica!
				final Model analyzedModel = model.copy();
				pool.addTask(new Runnable() {
					public void run() {
						/*File outputCSV = estrattore.mediaVeloce(uppaalModel.getAbsolutePath(), uppaalQuery.getAbsolutePath(), 1, false, false);*/
						//int timeTo = (int)(TIME_TO * 60.0 / analyzedModel.getProperties().get(Model.Properties.SECONDS_PER_POINT).as(Double.class));
						try {
							LevelResult result = new UppaalModelAnalyserSMC(null, null).analyze(analyzedModel, timeTo);
							Pair<Boolean, Double> comparisonResult = compareResults(result);
							if (comparisonResult.first) {
								synchronized(acceptableConfigurations) {
									//System.err.println("Configurazione accettabile al numero " + actualIndexToRemember + ", max differenza: " + comparisonResult.second);
									acceptableConfigurations.set(actualIndexToRemember, new AcceptableConfiguration(currentConfiguration, result, "Max abs diff: " + decimalFormat.format(comparisonResult.second)));
								}
							}
							allConfigurations.set(actualIndexToRemember, new AcceptableConfiguration(currentConfiguration, result, "Max abs diff: " + decimalFormat.format(comparisonResult.second)));
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						synchronized (System.out) {
							nComputations++;
							//stampaRapporto(nComputations, totalComputations, System.out);
							stampaRapporto(nComputations, totalComputations);
						}
					}
				});
			}
			/*if (!estrattore.interrotto() && compareResult.isSelected()) {
				Graph.addGraph(new File(graphToCompare.getText()));
			}*/
		} catch (Exception ex) {
			System.err.println("Error: " + ex);
			ex.printStackTrace();
		}
	}
	
	
	private Pair<Boolean, Double> compareResults(LevelResult myResult) throws Exception {
		double maxDiff = 0;
		for (Reactant reactant : reactantComparisons.keySet()) {
			ReactantComparison compare = reactantComparisons.get(reactant);
			//System.err.println("Devo comparare il reagente " + reactant + " con " + compare.getSeriesNames().firstElement() + ", errore massimo di " + compare.getMaxError());
			double allowedError = compare.getMaxError();
			File hisCSV = new File(compare.getCsvFile());
			String hisColumn = compare.getSeriesNames().firstElement(),
				   hisNLevelsColumn = Graph.MAX_Y_STRING.toLowerCase();
			int hisColumnIndex = 0,
				hisNLevelsColumnIndex = 0;
			double hisNLevels = 15,
				   myNLevels = model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
			//System.err.println("Il mio tempo va da 0 a " + myResult.getTimeIndices().get(myResult.getTimeIndices().size()-1) + " e ho " + myNLevels + " livelli");
			
			BufferedReader hisIS = new BufferedReader(new FileReader(hisCSV));
			String hisFirstLine = hisIS.readLine();
			//while ((myFirstLine = myIS.readLine()) == null);
			//while ((hisFirstLine = hisIS.readLine()) == null);
			if (hisFirstLine == null) {
				System.err.println("ERROR file " + hisCSV + ": is empty!");
				hisIS.close();
				return new Pair<Boolean, Double>(false, 100.0);
			}
			StringTokenizer hisTritatutto = new StringTokenizer(hisFirstLine, ",");
			boolean foundColumn = false,
					foundNLevels = false;
			while (hisTritatutto.hasMoreElements()) {
				String colName = hisTritatutto.nextToken().replace('\"',' ');
				if (!colName.equals(hisColumn) && !foundColumn) {
					hisColumnIndex++;
				} else if (colName.equals(hisColumn)) {
					foundColumn = true;
				}
				if (!colName.toLowerCase().equals(hisNLevelsColumn) && !foundNLevels) {
					hisNLevelsColumnIndex++;
				} else if (colName.equals(hisNLevelsColumn)) {
					foundNLevels = true;
				}
				if (foundColumn && foundNLevels) break;
			}
			
			String hisLine = null;
			double maxDifference = 0;
			foundNLevels = false;
			while ((hisLine = hisIS.readLine()) != null) {
				hisTritatutto = new StringTokenizer(hisLine, ",");
				double hisTime = Double.parseDouble(hisTritatutto.nextToken());
				for (int i=1;i<hisColumnIndex;i++) {
					if (i == hisNLevelsColumnIndex && !foundNLevels) {
						try {
							hisNLevels = Double.parseDouble(hisTritatutto.nextToken());
							foundNLevels = true;
							//System.err.println("Lui ha " + hisNLevels);
						} catch (Exception ex) {
							foundNLevels = false;
						}
					} else {
						hisTritatutto.nextToken();
					}
				}
				double hisValue = Double.parseDouble(hisTritatutto.nextToken()) / hisNLevels * myNLevels;
				double myValue = myResult.getConcentration(reactant.getId(), hisTime / scale);
				double difference = Math.abs(myValue - hisValue)/myNLevels;
				//System.err.println("IO (" + (hisTime / scale) + "): " + myValue + "/" + myNLevels + ", LUI (" + hisTime + "): " + (hisValue / myNLevels * hisNLevels) + "/" + hisNLevels + ". Diff: " + difference);
				if (difference > maxDifference) {
					maxDifference = difference;
				}
				if (difference > allowedError) {
					//System.err.println("E la comparazione fallisce! Mio valore = " + myValue + ", suo valore = " + hisValue);
					//System.err.println("Differenza attuale: " + difference + ", massimo consentito: " + allowedError);
					hisIS.close();
					return new Pair<Boolean, Double>(false, 100.0);
				}
			}
			//System.err.println("Differenza massima per il mio reagente " + reactant + " = " + maxDifference);
			if (maxDifference > maxDiff) {
				maxDiff = maxDifference;
			}
			hisIS.close();
		}
		//System.err.println("Siamo abbastanza uguali da passare la comparazione, urr�.");
		return new Pair<Boolean, Double>(true, maxDiff);
	}
	
	@SuppressWarnings("unused")
	private boolean compareResults(File myCSV) throws Exception {
		for (Reactant reactant : reactantComparisons.keySet()) {
			ReactantComparison compare = reactantComparisons.get(reactant);
			//System.err.println("Devo comparare il reagente " + reactant + " con " + compare.getSeriesNames().firstElement() + ", errore massimo di " + compare.getMaxError());
			double allowedError = compare.getMaxError();
			String myColumn = reactant.getName();
			File hisCSV = new File(compare.getCsvFile());
			String hisColumn = compare.getSeriesNames().firstElement();
			int myColumnIndex = 0,
				hisColumnIndex = 0;
			
			BufferedReader myIS = new BufferedReader(new FileReader(myCSV)),
						   hisIS = new BufferedReader(new FileReader(hisCSV));
			String myFirstLine = myIS.readLine(),
				   hisFirstLine = hisIS.readLine();
			//while ((myFirstLine = myIS.readLine()) == null);
			//while ((hisFirstLine = hisIS.readLine()) == null);
			if (myFirstLine == null) {
				System.err.println("ERROR: file " + myCSV + ": is empty!");
				myIS.close();
				hisIS.close();
				return false;
			}
			if (hisFirstLine == null) {
				System.err.println("ERROR file " + hisCSV + ": is empty!");
				myIS.close();
				hisIS.close();
				return false;
			}
			StringTokenizer myTritatutto = new StringTokenizer(myFirstLine, ","),
							hisTritatutto = new StringTokenizer(hisFirstLine, ",");
			while (myTritatutto.hasMoreElements()) {
				String colName = myTritatutto.nextToken().replace('\"',' ');
				if (colName.equals(myColumn)) {
					break;
				}
				myColumnIndex++;
			}
			while (hisTritatutto.hasMoreElements()) {
				String colName = hisTritatutto.nextToken().replace('\"',' ');
				if (colName.equals(hisColumn)) {
					break;
				}
				hisColumnIndex++;
			}
			
			String myLine = null,
				   hisLine = null;
			double maxDifference = 0;
			while ((hisLine = hisIS.readLine()) != null) {
				hisTritatutto = new StringTokenizer(hisLine, ",");
				double hisTime = Double.parseDouble(hisTritatutto.nextToken());
				for (int i=1;i<hisColumnIndex;i++) hisTritatutto.nextToken();
				double hisValue = Double.parseDouble(hisTritatutto.nextToken());
				while ((myLine = myIS.readLine()) != null) {
					myTritatutto = new StringTokenizer(myLine, ",");
					double myTime = Double.parseDouble(myTritatutto.nextToken());
					if (myTime < hisTime) continue;
					for (int i=1;i<myColumnIndex;i++) myTritatutto.nextToken();
					double myValue = Double.parseDouble(myTritatutto.nextToken());
					double difference = Math.abs(myValue - hisValue)/hisValue;
					if (difference > maxDifference) {
						maxDifference = difference;
					}
					if (difference > allowedError) { //TODO: interessante notare che la differenza calcolata cos� mi esclude paradossalmente i grafici pi� belli... o_O
						//System.err.println("E la comparazione fallisce! Mio valore = " + myValue + ", suo valore = " + hisValue);
						//System.err.println("Differenza attuale: " + difference + ", massimo consentito: " + allowedError);
						myIS.close();
						hisIS.close();
						return false;
					}
				}
			}
			//System.err.println("Differenza massima per il mio reagente " + reactant + " = " + maxDifference);
			myIS.close();
			hisIS.close();
		}
		//System.err.println("Siamo abbastanza uguali da passare la comparazione, urr�.");
		return true;
	}
	
	/*public static void main(String[] args) {
		ParameterFitter fitter = new ParameterFitter();
		fitter.showWindow(true);
	}*/
}
