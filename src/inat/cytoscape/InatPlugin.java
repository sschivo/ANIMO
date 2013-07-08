package inat.cytoscape;

import fitting.ParameterFitter;
import inat.InatBackend;
import inat.analyser.uppaal.UppaalModelAnalyserSMC;
import inat.cytoscape.INATPropertyChangeListener.ColorsListener;
import inat.cytoscape.INATPropertyChangeListener.ShapesListener;
import inat.exceptions.InatException;
import inat.graph.FileUtils;
import inat.util.XmlConfiguration;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.install4j.runtime.util.Base64;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.data.attr.MultiHashMapListener;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.visual.VisualMappingManager;
import cytoscape.visual.VisualPropertyType;
import cytoscape.visual.calculators.Calculator;
import cytoscape.visual.mappings.ObjectMapping;

/**
 * The ANIMO Cytoscape plugin main class.
 * 
 * @author Brend Wanders
 */
public class InatPlugin extends CytoscapePlugin {
	private final File configurationFile;
	private static final String SECONDS_PER_POINT = "seconds per point";
	private ShapesLegend legendShapes;
	private ColorsLegend legendColors;
	public static final String TAB_NAME = "ANIMO";
	public static int TAB_INDEX = 0;
	private ColorsListener colorsListener = null;
	private ShapesListener shapesListener = null;

	/**
	 * Mandatory constructor.
	 */
	public InatPlugin() {
		this.configurationFile = CytoscapeInit.getConfigFile("ANIMO-configuration.xml");

		try {
			InatBackend.initialise(this.configurationFile);
			System.setProperty("java.security.policy", CytoscapeInit.getConfigFile("ANIMO-security.policy").getAbsolutePath());

			CytoPanel p = Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
			p.add(TAB_NAME, this.setupPanel(this));
			TAB_INDEX = p.getCytoPanelComponentCount() - 1; //We assume that the component was added at the end
			
			INATPropertyChangeListener pcl = new INATPropertyChangeListener(legendColors, legendShapes);
			final PropertyChangeSupport pcs = Cytoscape.getPropertyChangeSupport();
			pcs.addPropertyChangeListener(Cytoscape.NETWORK_CREATED, pcl); //Add all visual mappings
			pcs.addPropertyChangeListener(Cytoscape.NETWORK_LOADED, pcl); //Make arrows "smooth"
			pcs.addPropertyChangeListener(CytoscapeDesktop.NETWORK_VIEW_CREATED, pcl); //Add right-click menus
			pcs.addPropertyChangeListener(Cytoscape.NETWORK_MODIFIED, pcl); //Add/remove nodes
			pcs.addPropertyChangeListener(Cytoscape.SESSION_LOADED, pcl); //We listen for the session events in order to make sure that nothing "strange" happens when the current session file is closed
			
			//Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(pcl);
			
			colorsListener = pcl.getColorsListener();
			shapesListener = pcl.getShapesListener();
			
			final VisualMappingManager vizMap = Cytoscape.getVisualMappingManager();
			ChangeListener vizMapChangeListener = new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					if (Cytoscape.getCurrentNetworkView() != null) {
						Calculator ca = vizMap.getVisualStyle().getNodeAppearanceCalculator().getCalculator(VisualPropertyType.NODE_FILL_COLOR);
						if (ca != null) {
							for (ObjectMapping m : ca.getMappings()) {
								try {
									m.removeChangeListener(colorsListener);
								} catch (Exception ex) {
								}
								m.addChangeListener(colorsListener);
							}
						}
						ca = vizMap.getVisualStyle().getNodeAppearanceCalculator().getCalculator(VisualPropertyType.NODE_SHAPE);
						if (ca != null) {
							for (ObjectMapping m : ca.getMappings()) {
								try {
									m.removeChangeListener(shapesListener);
								} catch (Exception ex) {
								}
								m.addChangeListener(shapesListener);
							}
						}
						legendColors.updateFromSettings();
						legendShapes.updateFromSettings();
					}
				}
			};
			vizMap.addChangeListener(vizMapChangeListener);
			
			
		} catch (InatException e) {
			// show error panel
			CytoPanel p = Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
			JPanel errorPanel = new JPanel(new BorderLayout());
			JTextArea message = new JTextArea(e.getMessage());
			message.setEditable(false);
			JScrollPane viewport = new JScrollPane(message);
			errorPanel.add(viewport, BorderLayout.CENTER);
			p.add(TAB_NAME, errorPanel);
		}
	}
	
	
	/**
	 * Constructs the panel
	 * 
	 * @param plugin the plugin for which the control panel is constructed.
	 * @return
	 */
	private JPanel setupPanel(InatPlugin plugin) {
		final XmlConfiguration configuration = InatBackend.get().configuration();
		String areWeTheDeveloperStr = configuration.get(XmlConfiguration.DEVELOPER_KEY);
		boolean areWeTheDeveloper = false;
		if (areWeTheDeveloperStr != null) {
			areWeTheDeveloper = Boolean.parseBoolean(areWeTheDeveloperStr);
		}
		
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); //BorderLayout(2, 2));

		// the button container
		JPanel buttons = new JPanel();// GridLayout(5, 1, 2, 2));
		buttons.setLayout(new GridBagLayout()); //new BoxLayout(buttons, BoxLayout.Y_AXIS));
		int yPositionCounter = 0; //The Y position of the various elements to be put in the GridBagLayout. TODO: remember to increment it each time you add an element to the buttons JPanel, and to use it as index for the y position
		
		//This part allows the user to choose whether to perform the computations on the local machine or on a remote machine.
		Box uppaalBox = new Box(BoxLayout.Y_AXIS);
		final JCheckBox remoteUppaal = new JCheckBox("Remote");
		final Box serverBox = new Box(BoxLayout.Y_AXIS);
		final JTextField serverName = new JTextField("my.server.com"),
				   serverPort = new JFormattedTextField("1234");
		remoteUppaal.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				boolean sel = remoteUppaal.isSelected();
				serverName.setEnabled(sel);
				serverPort.setEnabled(sel);
			}
		});
		remoteUppaal.setSelected(false);
		serverName.setEnabled(false);
		serverPort.setEnabled(false);
		//serverBox.add(serverName);
		//serverBox.add(serverPort);
		Box sBox = new Box(BoxLayout.X_AXIS);
		sBox.add(new JLabel("Server"));
		sBox.add(serverName);
		serverBox.add(Box.createVerticalStrut(10));
		serverBox.add(sBox);
		Box pBox = new Box(BoxLayout.X_AXIS);
		pBox.add(new JLabel("Port"));
		pBox.add(serverPort);
		serverBox.add(Box.createVerticalStrut(10));
		serverBox.add(pBox);
		serverBox.add(Box.createVerticalStrut(10));
		
		/*Box localUppaalBox = new Box(BoxLayout.X_AXIS);
		localUppaalBox.add(Box.createHorizontalStrut(5));
		localUppaalBox.add(localUppaal);
		localUppaalBox.add(Box.createGlue());
		uppaalBox.add(localUppaalBox);*/
		//uppaalBox.add(remoteUppaal);
		remoteUppaal.setOpaque(true);
		final ComponentTitledBorder border = new ComponentTitledBorder(remoteUppaal, serverBox, BorderFactory.createEtchedBorder());
		/*localUppaal.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				serverBox.repaint();
			}
		});*/
		serverBox.setBorder(border);
		uppaalBox.add(serverBox);
		//buttons.add(uppaalBox);
		if (areWeTheDeveloper) {
			buttons.add(serverBox, new GridBagConstraints(0, yPositionCounter++, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		}
		
		//This part allows the user to choose between simulation run(s) and Statistical Model Checking
		final JRadioButton normalUppaal = new JRadioButton("Simulation"),
						   smcUppaal = new JRadioButton("SMC");
		ButtonGroup modelCheckingGroup = new ButtonGroup();
		modelCheckingGroup.add(normalUppaal);
		modelCheckingGroup.add(smcUppaal);
		
		final JCheckBox multipleRuns = new JCheckBox("Compute");
		final JRadioButton overlayPlot = new JRadioButton("Overlay plot"),
						   computeAvgStdDev = new JRadioButton("Average and std deviation"); //"Show standard deviation as error bars");
		ButtonGroup multipleRunsGroup = new ButtonGroup();
		multipleRunsGroup.add(overlayPlot);
		multipleRunsGroup.add(computeAvgStdDev);
		computeAvgStdDev.setSelected(true);
		computeAvgStdDev.setToolTipText(computeAvgStdDev.getText());
		overlayPlot.setToolTipText("Plot all run results one above the other");
		final JFormattedTextField timeTo = new JFormattedTextField(240);
		final JFormattedTextField nSimulationRuns = new JFormattedTextField(10);
		final JTextField smcFormula = new JTextField("Pr[<=50](<> MK2 > 50)");
		timeTo.setToolTipText("Plot activity levels up to this time point (real-life MINUTES).");
		nSimulationRuns.setToolTipText("Number of simulations of which to show the average. NO statistical guarantees!");
		smcFormula.setToolTipText("Give an answer to this probabilistic query (times in real-life MINUTES).");
		normalUppaal.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (normalUppaal.isSelected()) {
					timeTo.setEnabled(true);
					multipleRuns.setEnabled(true);
					nSimulationRuns.setEnabled(multipleRuns.isSelected());
					computeAvgStdDev.setEnabled(multipleRuns.isSelected());
					overlayPlot.setEnabled(multipleRuns.isSelected());
					smcFormula.setEnabled(false);
				} else {
					timeTo.setEnabled(false);
					multipleRuns.setEnabled(false);
					nSimulationRuns.setEnabled(false);
					computeAvgStdDev.setEnabled(false);
					overlayPlot.setEnabled(false);
					smcFormula.setEnabled(true);
				}
			}
		});
		multipleRuns.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (multipleRuns.isSelected() && normalUppaal.isSelected()) {
					nSimulationRuns.setEnabled(true);
					computeAvgStdDev.setEnabled(true);
					overlayPlot.setEnabled(true);
				} else {
					nSimulationRuns.setEnabled(false);
					computeAvgStdDev.setEnabled(false);
					overlayPlot.setEnabled(false);
				}
			}
		});
		normalUppaal.setSelected(true);
		smcUppaal.setSelected(false);
		timeTo.setEnabled(true);
		multipleRuns.setEnabled(true);
		multipleRuns.setSelected(false);
		computeAvgStdDev.setEnabled(false);
		computeAvgStdDev.setSelected(true);
		overlayPlot.setEnabled(false);
		nSimulationRuns.setEnabled(false);
		smcFormula.setEnabled(false);
		Box modelCheckingBox = new Box(BoxLayout.Y_AXIS);
		final Box normalBox = new Box(BoxLayout.Y_AXIS);
		Box smcBox = new Box(BoxLayout.X_AXIS);
		//Box normalUppaalBox = new Box(BoxLayout.X_AXIS);
		//normalUppaalBox.add(normalUppaal);
		//normalUppaalBox.add(Box.createGlue());
		//normalBox.add(normalUppaalBox);
		Box timeToBox = new Box(BoxLayout.X_AXIS);
		timeToBox.add(new JLabel("until"));
		timeToBox.add(timeTo);
		timeToBox.add(new JLabel("minutes"));
		normalBox.add(timeToBox);
		Box averageBox = new Box(BoxLayout.X_AXIS);
		averageBox.add(multipleRuns);
		averageBox.add(nSimulationRuns);
		averageBox.add(new JLabel("runs"));
		normalBox.add(averageBox);
		Box stdDevBox = new Box(BoxLayout.X_AXIS);
		stdDevBox.add(computeAvgStdDev);
		stdDevBox.add(Box.createGlue());
		normalBox.add(stdDevBox);
		Box overlayPlotBox = new Box(BoxLayout.X_AXIS);
		overlayPlotBox.add(overlayPlot);
		overlayPlotBox.add(Box.createGlue());
		normalBox.add(overlayPlotBox);
		smcBox.add(smcUppaal);
		smcBox.add(smcFormula);
		normalUppaal.setOpaque(true);
		//final ComponentTitledBorder border2 = new ComponentTitledBorder(normalUppaal, normalBox, BorderFactory.createEtchedBorder());
		//smcUppaal.addChangeListener(new ChangeListener() {
		//	@Override
		//	public void stateChanged(ChangeEvent e) {
		//		normalBox.repaint();
		//	}
		//});
		//normalBox.setBorder(border2);
		//modelCheckingBox.add(normalBox);
		//modelCheckingBox.add(smcBox);
		
		

		JButton loadSimulationDataButton = new JButton(new AbstractAction("Load simulation data...") {
			private static final long serialVersionUID = -998176729911500957L;

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					CyNetwork net = Cytoscape.getCurrentNetwork();
					if (net != null) {
						//Model model = Model.generateModelFromCurrentNetwork(null, -1);
						
						String inputFileName = FileUtils.open(".sim", "ANIMO simulation", Cytoscape.getDesktop());
						if (inputFileName == null) return;
						final InatResultPanel resultViewer = new InatResultPanel(new File(inputFileName));
						resultViewer.addToPanel(Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST));
						
					} else {
						JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "There is no current network to which to associate the simulation data.\nPlease load a network first.", "No current network", JOptionPane.ERROR_MESSAGE);
					}
				} catch (Exception ex) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					PrintStream ps = new PrintStream(baos);
					ex.printStackTrace(ps);
					JOptionPane.showMessageDialog(Cytoscape.getDesktop(), baos.toString(), "Error: " + ex, JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		Box loadSimulationBox = new Box(BoxLayout.X_AXIS);
		loadSimulationBox.add(Box.createGlue());
		loadSimulationBox.add(loadSimulationDataButton);
		loadSimulationBox.add(Box.createGlue());
		//buttonsBox.add(loadSimulationBox);
		modelCheckingBox.add(loadSimulationBox);
		
		
		modelCheckingBox.add(new LabelledField("Simulation", normalBox));
		buttons.add(modelCheckingBox, new GridBagConstraints(0, yPositionCounter++, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		
		Box buttonsBox = new Box(BoxLayout.Y_AXIS);
		
		final JButton changeSecondsPerPointbutton = new JButton();
		changeSecondsPerPointbutton.setToolTipText("Click here to change the number of seconds to which a single simulation step corresponds");
		new ChangeSecondsAction(plugin, changeSecondsPerPointbutton); //This manages the button for changing the number of seconds per UPPAAL time unit
		//buttons.add(changeSecondsPerPointbutton);
		Box changeSecondsPerPointbuttonBox = new Box(BoxLayout.X_AXIS);
		changeSecondsPerPointbuttonBox.add(Box.createGlue());
		changeSecondsPerPointbuttonBox.add(changeSecondsPerPointbutton);
		changeSecondsPerPointbuttonBox.add(Box.createGlue());
		buttonsBox.add(changeSecondsPerPointbuttonBox);
		Cytoscape.getNetworkAttributes().getMultiHashMap().addDataListener(new MultiHashMapListener() {

			@Override
			public void allAttributeValuesRemoved(String arg0, String arg1) {
				
			}

			@Override
			public void attributeValueAssigned(String objectKey, String attributeName,
					Object[] keyIntoValue, Object oldAttributeValue, Object newAttributeValue) {
				
				if (attributeName.equals(SECONDS_PER_POINT)) {
					if (newAttributeValue != null) {
						double value = 1;
						try {
							value = Double.parseDouble(newAttributeValue.toString());
						} catch (Exception e) {
							value = 1;
						}
						DecimalFormat df = new DecimalFormat("#.########");
						changeSecondsPerPointbutton.setText(df.format(value) + " seconds/step");
					} else {
						changeSecondsPerPointbutton.setText("Choose seconds/step");
					}
				}
			}

			@Override
			public void attributeValueRemoved(String arg0, String arg1,
					Object[] arg2, Object arg3) {
				
			}
		});
		
		
		//The "Parameter Fitter"
		if (areWeTheDeveloper) {
			JButton parameterFit = new JButton("Parameter fitter...");
			parameterFit.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ParameterFitter fitter = new ParameterFitter();
					fitter.showWindow(false, Integer.parseInt(timeTo.getValue().toString()));
				}
			});
			Box parameterFitBox = new Box(BoxLayout.X_AXIS);
			parameterFitBox.add(Box.createGlue());
			parameterFitBox.add(parameterFit);
			parameterFitBox.add(Box.createGlue());
			buttonsBox.add(parameterFitBox);
		}
		

		//The "Analyse network" button: perform the requested analysis on the current network with the given parameters
		JButton runButton = new JButton(new RunAction(plugin, remoteUppaal, serverName, serverPort, smcUppaal, timeTo, nSimulationRuns, computeAvgStdDev, overlayPlot, smcFormula));
		//buttons.add(runButton);
		Box runButtonBox = new Box(BoxLayout.X_AXIS);
		runButtonBox.add(Box.createGlue());
		runButtonBox.add(runButton);
		runButtonBox.add(Box.createGlue());
		JButton mcButton = new JButton(new ModelCheckAction());
		runButtonBox.add(mcButton);
		runButtonBox.add(Box.createGlue());
		buttonsBox.add(runButtonBox);
		
		/*Box mcButtonBox = new Box(BoxLayout.X_AXIS);
		mcButtonBox.add(Box.createGlue());
		mcButtonBox.add(mcButton);
		mcButtonBox.add(Box.createGlue());
		buttonsBox.add(Box.createGlue());
		buttonsBox.add(mcButtonBox);*/
		
		/*JButton augmentButton = new JButton(new AugmentAction(plugin));
		//buttons.add(augmentButton);
		Box augmentButtonBox = new Box(BoxLayout.X_AXIS);
		augmentButtonBox.add(Box.createGlue());
		augmentButtonBox.add(augmentButton);
		augmentButtonBox.add(Box.createGlue());
		buttonsBox.add(augmentButtonBox);*/
		
		
		buttons.add(buttonsBox, new GridBagConstraints(0, yPositionCounter++, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		

		legendColors = new ColorsLegend();
		legendShapes = new ShapesLegend();
		JPanel panelLegends = new JPanel();
		panelLegends.setBackground(Color.WHITE);
		panelLegends.setLayout(new GridBagLayout());
		panelLegends.add(legendColors, new GridBagConstraints(0, 0, 1, 1, 1, 0.5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		panelLegends.add(legendShapes, new GridBagConstraints(0, 1, 1, 1, 1, 0.5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		panelLegends.setPreferredSize(new Dimension(200, 400));
		buttons.add(new LabelledField("Legend", new JScrollPane(panelLegends, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)), new GridBagConstraints(0, yPositionCounter++, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(30, 5, 5, 5), 0, 0));
		
		JButton options = new JButton("Options...");
		options.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final JDialog optionsDialog = new JDialog(Cytoscape.getDesktop(), "ANIMO Options", Dialog.ModalityType.APPLICATION_MODAL);
				optionsDialog.getContentPane().setLayout(new BorderLayout());
				JPanel content = new JPanel(new GridBagLayout());
				String location = configuration.get(XmlConfiguration.VERIFY_KEY);
				final JLabel verifytaLocation = new JLabel(location);
				verifytaLocation.setToolTipText(location);
				JPanel verifytaPanel = new JPanel(new BorderLayout());
				verifytaPanel.add(verifytaLocation, BorderLayout.CENTER);
				final JButton changeLocation = new JButton("Change...");
				changeLocation.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent ev) {
						String verifytaFileName = "verifyta";
						if (UppaalModelAnalyserSMC.areWeUnderWindows()) {
							verifytaFileName += ".exe";
						}
						JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Please, find and select the \"verifyta\" tool.\nIt is usually located in the \"bin\" directory of UPPAAL.", "Verifyta", JOptionPane.QUESTION_MESSAGE);
						String verifytaFileLocation = FileUtils.open(verifytaFileName, "Verifyta Executable", optionsDialog);
						if (verifytaFileLocation != null) {
							verifytaLocation.setText(verifytaFileLocation);
							verifytaLocation.setToolTipText(verifytaFileName);
						}
					}
				});
				verifytaPanel.add(changeLocation, BorderLayout.EAST);
				content.add(new LabelledField("UPPAAL verifyta location", verifytaPanel), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				
				
				final String useUncertaintyTitle = "Use uncertainty value (in %): ",
							 noUncertaintyTitle = "Uncertainty is 0";
				final JCheckBox useUncertainty = new JCheckBox(useUncertaintyTitle);
				Integer unc = 5;
				try {
					unc = new Integer(configuration.get(XmlConfiguration.UNCERTAINTY_KEY));
				} catch (NumberFormatException ex) {
					unc = 5;
				} catch (NullPointerException ex) { //If the property wasn't there, we shouldn't assume that the user knows of its existence: so we make its effects null
					unc = 0;
				}
				final JFormattedTextField uncertainty = new JFormattedTextField(unc);
				Dimension dim = uncertainty.getPreferredSize();
				dim.setSize(dim.getWidth() * 1.5, dim.getHeight());
				uncertainty.setPreferredSize(dim);
				useUncertainty.setSelected(true);
				useUncertainty.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent ev) {
						uncertainty.setVisible(useUncertainty.isSelected());
						if (useUncertainty.isSelected()) {
							useUncertainty.setText(useUncertaintyTitle);
						} else {
							useUncertainty.setText(noUncertaintyTitle);
						}
					}
				});
				if (unc == 0) {
					useUncertainty.setSelected(false);
					useUncertainty.setText(noUncertaintyTitle);
					uncertainty.setVisible(useUncertainty.isSelected());
				}
				JPanel uncertaintyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
				uncertaintyPanel.add(useUncertainty);
				uncertaintyPanel.add(uncertainty);
				content.add(new LabelledField("Reaction parameters uncertainty", uncertaintyPanel), new GridBagConstraints(0, 1, 1, 1, 1.0, 0.5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				
				
				final String reactionCenteredTitle = "Reaction-centered model",
							 reactionCenteredTablesTitle = "Reaction-centered model with tables",
							 reactantCenteredTitle = "Reactant-centered model",
							 reactantCenteredOpaalTitle = "Reactant-centered for multi-core analysis";
				final JRadioButton useReactionCentered = new JRadioButton(reactionCenteredTitle),
								   useReactionCenteredTables = new JRadioButton(reactionCenteredTablesTitle),
								   useReactantCentered = new JRadioButton(reactantCenteredTitle),
								   useReactantCenteredOpaal = new JRadioButton(reactantCenteredOpaalTitle);
				useReactionCentered.setToolTipText("Advised when the network is not reaction-heavy");
				useReactionCenteredTables.setToolTipText("Advised when the network is not reaction-heavy. Also, tends to use more memory.");
				useReactantCentered.setToolTipText("Advised when the network is reaction-heavy (experimental)");
				useReactantCenteredOpaal.setToolTipText("Reactant-centered model for use the generated model with opaal and ltsmin");
				final ButtonGroup reactionCenteredGroup = new ButtonGroup();
				reactionCenteredGroup.add(useReactionCentered);
				reactionCenteredGroup.add(useReactionCenteredTables);
				reactionCenteredGroup.add(useReactantCentered);
				reactionCenteredGroup.add(useReactantCenteredOpaal);
				String modelType = null;
				try {
					modelType = configuration.get(XmlConfiguration.MODEL_TYPE_KEY);
				} catch (Exception ex) {
					modelType = XmlConfiguration.DEFAULT_MODEL_TYPE;
				}
				if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTION_CENTERED)) {
					useReactionCentered.setSelected(true);
					useReactionCenteredTables.setSelected(false);
					useReactantCentered.setSelected(false);
					useReactantCenteredOpaal.setSelected(false);
				} else if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTION_CENTERED_TABLES)) {
					useReactionCentered.setSelected(false);
					useReactionCenteredTables.setSelected(true);
					useReactantCentered.setSelected(false);
					useReactantCenteredOpaal.setSelected(false);
				} else if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTANT_CENTERED)) {
					useReactionCentered.setSelected(false);
					useReactionCenteredTables.setSelected(false);
					useReactantCentered.setSelected(true);
					useReactantCenteredOpaal.setSelected(false);
				} else if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTANT_CENTERED_OPAAL)) {
					useReactionCentered.setSelected(false);
					useReactionCenteredTables.setSelected(false);
					useReactantCentered.setSelected(false);
					useReactantCenteredOpaal.setSelected(true);
				} else {
					useReactionCentered.setSelected(false);
					useReactionCenteredTables.setSelected(false);
					useReactantCentered.setSelected(true);
					useReactantCenteredOpaal.setSelected(false);
				}
				JPanel modelTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
				modelTypePanel.add(useReactionCentered);
				modelTypePanel.add(useReactionCenteredTables);
				modelTypePanel.add(useReactantCentered);
				modelTypePanel.add(useReactantCenteredOpaal);
				content.add(new LabelledField("Model type", modelTypePanel), new GridBagConstraints(0, 2, 1, 1, 1.0, 0.5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				
				
				JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
				JButton okButton = new JButton("OK"),
						cancelButton = new JButton("Cancel");
				okButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent ev) {
						String verifyta = verifytaLocation.getText();
						configuration.set(XmlConfiguration.VERIFY_KEY, verifyta);
						configuration.set(XmlConfiguration.VERIFY_SMC_KEY, verifyta);
						String uncertaintyValue = "5";
						if (useUncertainty.isSelected()) {
							uncertaintyValue = uncertainty.getText();
							try {
								uncertaintyValue = "" + new Integer(uncertaintyValue);
							} catch (NumberFormatException ex) {
								JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "\"" + uncertaintyValue + "\" is not a number.", "Error", JOptionPane.ERROR_MESSAGE);
								return;
							}
						} else {
							uncertaintyValue = "0";
						}
						configuration.set(XmlConfiguration.UNCERTAINTY_KEY, uncertaintyValue);
						String modelTypeValue = XmlConfiguration.DEFAULT_MODEL_TYPE;
						if (useReactionCentered.isSelected()) {
							modelTypeValue = XmlConfiguration.MODEL_TYPE_REACTION_CENTERED;
						} else if (useReactionCenteredTables.isSelected()) {
							modelTypeValue = XmlConfiguration.MODEL_TYPE_REACTION_CENTERED_TABLES;
						} else if (useReactantCentered.isSelected()) {
							modelTypeValue = XmlConfiguration.MODEL_TYPE_REACTANT_CENTERED;
						} else if (useReactantCenteredOpaal.isSelected()) {
							modelTypeValue = XmlConfiguration.MODEL_TYPE_REACTANT_CENTERED_OPAAL;
						} else {
							modelTypeValue = XmlConfiguration.DEFAULT_MODEL_TYPE;
						}
						configuration.set(XmlConfiguration.MODEL_TYPE_KEY, modelTypeValue);
						try {
							configuration.writeConfigFile();
						} catch (Exception ex) {
							JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Unexpected problem: " + ex);
							ex.printStackTrace();
						}
						optionsDialog.dispose();
					}
				});
				cancelButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent ev) {
						optionsDialog.dispose();
					}
				});
				buttonsPanel.add(okButton);
				buttonsPanel.add(cancelButton);
				optionsDialog.getContentPane().add(content, BorderLayout.CENTER);
				optionsDialog.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
				optionsDialog.pack();
				optionsDialog.setLocationRelativeTo(null);
				optionsDialog.setVisible(true);
			}
		});
		buttons.add(options, new GridBagConstraints(0, yPositionCounter++, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		
		panel.add(buttons);
		

		return panel;
	}
	
	
	/**
	 * Save the needed files in this session, so that we will be able
	 * to retrieve them when reloading the same session later.
	 */
	@Override
	public void saveSessionStateFiles(List<File> myFiles) {
		//Save any open results panel as a .sim file and add it to the list
		CytoPanel results = Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST);
		if (results != null) {
			for (int i=0;i<results.getCytoPanelComponentCount();i++) {
				Component c = results.getComponentAt(i);
				saveAnyOpenResults(myFiles, c);
			}
		}
	}
	
	private void saveAnyOpenResults(List<File> myFiles, Component c) {
		if (c instanceof InatResultPanel) {
			try {
				String tmpDir = System.getProperty("java.io.tmpdir");
				InatResultPanel panel = (InatResultPanel)c;
				CyNetwork savedNetwork = panel.getSavedNetwork();
				if (savedNetwork == null) return;
				File outputFile = new File(tmpDir, savedNetwork.getIdentifier() + ".sim");
				outputFile.deleteOnExit();
				panel.saveSimulationData(outputFile, false);
				myFiles.add(outputFile);
				outputFile = new File(tmpDir, savedNetwork.getIdentifier() + ".png");
				outputFile.deleteOnExit();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(panel.getSavedNetworkImage(), "png", baos); //"Why do all this mess instead of simply writing the image to a file with ImageIO?", you may ask. Well, Cytoscape does not allow you to simply save binary files (images or files output with ObjectOutputStream) because it preprocesses (adding some useless encoding stuff) when opening them. While if we save them as binary inside an xml (thus in a CDATA section) all is good.
				baos.flush();
				String encodedImage = Base64.encode(baos.toByteArray());
				baos.close();
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
				Element root = doc.createElement("image");
				doc.appendChild(root);
				CDATASection node = doc.createCDATASection(encodedImage);
				root.appendChild(node);
				Transformer tra = TransformerFactory.newInstance().newTransformer();
				tra.setOutputProperty(OutputKeys.INDENT, "yes");
				tra.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				tra.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				FileOutputStream fos = new FileOutputStream(outputFile);
				tra.transform(new DOMSource(doc), new StreamResult(fos));
				fos.flush();
				fos.close();
				myFiles.add(outputFile);
			} catch (Exception ex) {
				ex.printStackTrace();
				//I have tried: if something went wrong I can't do much more
			}
		} else if (c instanceof Container) {
			for (Component o : ((Container)c).getComponents()) {
				saveAnyOpenResults(myFiles, o);
			}
		}
	}
	
	/*private void closeAnyOpenResults(CytoPanel results, Component c) { //Don't use this. See the comment here for closeResultsPanel
		if (c instanceof InatResultPanel) {
			((InatResultPanel)c).closeResultsPanel(results); //Using closeResultPanel would cause the associated network to be destroyed. But networks are named by identifier, and even if I used random identifiers, it is still possible that a network with the same identifier as the old (and already removed from memory) network to which the panel was referring is now available, and will thus get destroyed. So when we will press the "reset to here" button, we will simply get an empty network!
		} else if (c instanceof Container) {
			for (Component o : ((Container)c).getComponents()) {
				closeAnyOpenResults(results, o);
			}
		}
	}*/
	
	/**
	 * Retrieve the relevant information for ANIMO in the saved files.
	 */
	@Override
	public void restoreSessionState(List<File> myFiles) {
		
		CytoPanel results = Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST);
		if (results != null) {
			//results.removeAll(); //Please note: we cannot simply removeAll() because the InatResultPanel needs to perform its own operations when it is being removed (in particular, remove the associated saved network)
			List<Component> panelsToBeRemoved = new Vector<Component>();
			for (int i=0;i<results.getCytoPanelComponentCount();i++) {
				Component c = results.getComponentAt(i);
				panelsToBeRemoved.add(c);
			}
			for (Component c : panelsToBeRemoved) {
				//closeAnyOpenResults(results, c);
				results.remove(c);
			}
		}
		//Then load the .sim files from the list of files
		if ((myFiles == null) || (myFiles.size() == 0)) {
			//No simulations to restore
			return;
		}
		Map<String, BufferedImage> mapIdToImage = new HashMap<String, BufferedImage>();
		for (File f : myFiles) {
			String name = f.getName();
			if (!name.endsWith(".png")) continue;
			try {
				BufferedImage image;// = ImageIO.read(f);
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
				doc.getDocumentElement().normalize();
				byte[] decodedImage = Base64.decode(doc.getFirstChild().getFirstChild().getTextContent()); //The first child is called "image", and its child is the CDATA section with all the binary data
				image = ImageIO.read(new ByteArrayInputStream(decodedImage));
				mapIdToImage.put(name.substring(0, name.lastIndexOf(".png")), image);
				//System.err.println("Immagine di id " + name.substring(0, name.lastIndexOf(".png")) + " = " + image);
			} catch (Exception ex) {
				//Hopefully we could load the image. Otherwise, it's no drama
				ex.printStackTrace();
			}
		}
		for (File f : myFiles) {
			String name = f.getName();
			if (!name.endsWith(".sim")) continue;
			InatResultPanel panel = InatResultPanel.loadFromSessionSimFile(f);
			BufferedImage image = mapIdToImage.get(name.substring(0, name.lastIndexOf(".sim")));
			//System.err.println("Retrievo immagine di id " + name.substring(0, name.lastIndexOf(".sim")) + ": " + image);
			panel.setSavedNetworkImage(image);
			panel.addToPanel(results);
		}

	}

}
