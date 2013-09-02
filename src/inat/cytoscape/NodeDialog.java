package inat.cytoscape;

import giny.model.Node;
import inat.model.Model;
import inat.util.JSwitchBox;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;

/**
 * The node dialog contains the settings of a node.
 * 
 * @author Brend Wanders
 * 
 */
public class NodeDialog extends JDialog {
	
	private static final long serialVersionUID = 1498730989498413815L;
	private boolean wasNewlyCreated = false;
	
	public NodeDialog(final Node node) {
		this(Cytoscape.getDesktop(), node);
	}
	
	public void setCreatedNewNode() {
		wasNewlyCreated = true;
	}

	/**
	 * Constructor.
	 * 
	 * @param node the node to display for.
	 */
	@SuppressWarnings("unchecked")
	public NodeDialog(final Window owner, final Node node) {
		super(owner, "Reactant '" + node.getIdentifier() + "'", Dialog.ModalityType.APPLICATION_MODAL);
		CyAttributes networkAttributes = Cytoscape.getNetworkAttributes(),
					 nodeAttributes = Cytoscape.getNodeAttributes();
		this.setTitle("Edit reactant");
		Object res = nodeAttributes.getAttribute(node.getIdentifier(), Model.Properties.CANONICAL_NAME);
		String name;
		if (res != null) {
			name = res.toString();
		} else {
			name = null;
		}
		if (!nodeAttributes.hasAttribute(node.getIdentifier(), Model.Properties.INITIAL_LEVEL)) {
			nodeAttributes.setAttribute(node.getIdentifier(), Model.Properties.INITIAL_LEVEL, 0);
		}
		
		this.setLayout(new GridBagLayout());
		
		int levels;
		if (nodeAttributes.hasAttribute(node.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS)) {
			levels = nodeAttributes.getIntegerAttribute(node.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS);
		} else if (networkAttributes.hasAttribute(Cytoscape.getCurrentNetwork().getIdentifier(), Model.Properties.NUMBER_OF_LEVELS)) {
			levels = networkAttributes.getIntegerAttribute(Cytoscape.getCurrentNetwork().getIdentifier(), Model.Properties.NUMBER_OF_LEVELS);
		} else {
			levels = 15;
		}
		
		final JTextField nameField = new JTextField(name);
		this.add(new LabelledField("Name", nameField), new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		
		final JSlider totalLevels = new JSlider(1, 100);
		totalLevels.setValue(levels);
		totalLevels.setMajorTickSpacing(20);
		totalLevels.setMinorTickSpacing(10);
		
		totalLevels.setPaintLabels(true);
		totalLevels.setPaintTicks(true);
		if (totalLevels.getMaximum() == 100) {
			Dictionary<Integer, JLabel> labelTable = totalLevels.getLabelTable();
			labelTable.put(totalLevels.getMaximum(), new JLabel("" + totalLevels.getMaximum()));
			totalLevels.setLabelTable(labelTable);
		}
		final LabelledField totalLevelsField = new LabelledField("Total activity levels: " + levels, totalLevels);
		
		final JSlider initialConcentration = new JSlider(0, levels);
		initialConcentration.setValue(nodeAttributes.getIntegerAttribute(node.getIdentifier(), Model.Properties.INITIAL_LEVEL));
		
		final LabelledField initialLevelField = new LabelledField("Initial activity level: " + initialConcentration.getValue(), initialConcentration);


		initialConcentration.setMajorTickSpacing(levels / 5);
		initialConcentration.setMinorTickSpacing(levels / 10);
		
		initialConcentration.setPaintLabels(true);
		initialConcentration.setPaintTicks(true);

		//When the user changes the total number of levels, we automatically update the "current activity level" slider, adapting maximum and current values in a sensible way
		totalLevels.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				totalLevelsField.setTitle("Total activity levels: " + totalLevels.getValue());
				if (totalLevels.getValueIsAdjusting()) return;
				double prevMax = initialConcentration.getMaximum(),
					   currMax = totalLevels.getValue();
				int currValue = (int)((initialConcentration.getValue()) / prevMax * currMax);
				initialConcentration.setMaximum(totalLevels.getValue());
				initialConcentration.setValue(currValue);
				int space = (initialConcentration.getMaximum() - initialConcentration.getMinimum() + 1) / 5;
				if (space < 1) space = 1;
				initialConcentration.setMajorTickSpacing(space);
				initialConcentration.setMinorTickSpacing(space / 2);
				Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
				for (int i=initialConcentration.getMinimum();i<=initialConcentration.getMaximum();i+=space) {
					labelTable.put(i, new JLabel("" + i));
				}
				initialConcentration.setLabelTable(labelTable);
				initialLevelField.setTitle("Initial activity level: " + initialConcentration.getValue());
				initialConcentration.setValue(currValue);
			}
		});
		
		
		NumberFormat percentDisplayFormat = NumberFormat.getPercentInstance(),
					 percentEditFormat = NumberFormat.getNumberInstance();
		percentDisplayFormat.setMinimumFractionDigits(2);
		percentEditFormat.setMinimumFractionDigits(2);
		NumberFormatter percentEditFormatter = new NumberFormatter(percentEditFormat) {
			private static final long serialVersionUID = 6939037965613797062L;
			public String valueToString(Object o)
		          throws ParseException {
		        Number number = (Number)o;
		        if (number != null) {
		            double d = number.doubleValue() * 100.0;
		            number = new Double(d);
		        }
		        return super.valueToString(number);
		    }
		    public Object stringToValue(String s)
		           throws ParseException {
		        Number number = (Number)super.stringToValue(s);
		        if (number != null) {
		            double d = number.doubleValue() / 100.0;
		            number = new Double(d);
		        }
		        return number;
		    }
		};
		NumberFormat stepsFormat = NumberFormat.getNumberInstance();
		stepsFormat.setMinimumFractionDigits(0);
		stepsFormat.setMaximumFractionDigits(0);
		final JFormattedTextField logStepPercent = new JFormattedTextField(new DefaultFormatterFactory(new NumberFormatter(percentDisplayFormat), new NumberFormatter(percentDisplayFormat), percentEditFormatter));
		final JFormattedTextField logTotalSteps = new JFormattedTextField(stepsFormat);
		logStepPercent.setColumns(6);
		logTotalSteps.setColumns(5);
		final JPanel logPercentageField = new JPanel() {
			private static final long serialVersionUID = -2572281488142054778L;
			@Override
			public void setEnabled(boolean enabled) {
				for (Component c : this.getComponents()) {
					c.setEnabled(enabled);
				}
				super.setEnabled(enabled);
			}
		},
				  	 logTotalStepsField = new JPanel() {
			private static final long serialVersionUID = -9038682316472470140L;
			@Override
			public void setEnabled(boolean enabled) {
				for (Component c : this.getComponents()) {
					c.setEnabled(enabled);
				}
				super.setEnabled(enabled);
			}
		};
		logPercentageField.setLayout(new GridBagLayout());
		logTotalStepsField.setLayout(new GridBagLayout());
		logPercentageField.add(new JLabel("One step changes activity by "),  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		logPercentageField.add(logStepPercent,  new GridBagConstraints(1, 0, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		logTotalStepsField.add(new JLabel("Total "), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		logTotalStepsField.add(logTotalSteps, new GridBagConstraints(1, 0, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		logTotalStepsField.add(new JLabel(" steps"), new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		
		final JSwitchBox discretizationChoice = new JSwitchBox("Linear", "Log-scale");
		
		initialConcentration.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (discretizationChoice.isSelected()) { //Linear scale: the value can be read directly
					initialLevelField.setTitle("Initial activity level: " + initialConcentration.getValue());
				} else { //Log-scale: the value needs to be calculated, then we snap the cursor to the closest "tick"
					if (initialConcentration.getValueIsAdjusting()) return;
					int iActivity = initialConcentration.getValue();
					if (iActivity == initialConcentration.getMinimum()) {
						initialLevelField.setTitle("Initial activity: 0%");
					} else if (iActivity == initialConcentration.getMaximum()) {
						initialLevelField.setTitle("Initial activity: 100%");
					} else {
						double stepSize = 1.5;
						try {
							stepSize = Double.parseDouble(logStepPercent.getValue().toString()) + 1.0;
						} catch (NumberFormatException ex) {
						}
						double tickPosition = Math.round(Math.log(iActivity) / Math.log(stepSize));
						if (tickPosition == 0) {
							initialLevelField.setTitle("Initial activity: 0%");
							initialConcentration.setValue(initialConcentration.getMinimum());
						} else {
							double activity = Math.pow(stepSize, tickPosition);
							initialLevelField.setTitle("Initial activity: " + activity + "%");
							initialConcentration.setValue((int)Math.round(activity));
						}
					}
				}
			}
		});
		
		class InitialActivityUpdater {
			void update() {
				if (logStepPercent.isEnabled()) {
					initialConcentration.setMinimum(0);
					initialConcentration.setMaximum(100);
					initialConcentration.setValue(0);
					int space = (initialConcentration.getMaximum() - initialConcentration.getMinimum() + 1) / 5;
					if (space < 1) space = 1;
					initialConcentration.setMajorTickSpacing(space);
					initialConcentration.setMinorTickSpacing(space / 2);
					Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
					double stepSize = 2.5119; //This gives 5 steps from 0 to 100 (just to have a value)
					try {
						stepSize = Double.parseDouble(logStepPercent.getValue().toString()) + 1.0;
					} catch (NumberFormatException ex) {
					}
					labelTable.put(0, new JLabel("0"));
					for (double pos = stepSize;pos < 100;pos *= stepSize) {
						int iPos = (int)Math.round(pos);
						labelTable.put(iPos, new JLabel("|")); // + iPos));
					}
					labelTable.put(100, new JLabel("100"));
					initialConcentration.setLabelTable(labelTable);
					initialLevelField.setTitle("Initial activity: " + initialConcentration.getValue() + "%");
					initialConcentration.setValue(0);
					initialConcentration.setPaintTicks(false);
				} else {
					ChangeEvent e = new ChangeEvent(totalLevels);
					for(ChangeListener l : totalLevels.getChangeListeners()){
						l.stateChanged(e);
					}
					initialConcentration.setPaintTicks(true);
				}
			}
		}
		final InitialActivityUpdater initialActivityUpdater = new InitialActivityUpdater();
		
		PropertyChangeListener pcl = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				Object source = e.getSource();
				try {
					if (source == logStepPercent) {
						double percValue = 1.5;
						try {
							percValue = Double.parseDouble(logStepPercent.getValue().toString());
						} catch (NumberFormatException ex) {
						}
//						System.err.println("Dimensione di un passo: " + percValue + "%");
						double totSteps = Math.log(100.0) / Math.log(1.0 + percValue);
//						System.err.println("N. di passi: :" + totSteps);
						PropertyChangeListener[] listeneri = logTotalSteps.getPropertyChangeListeners("value");
						for (PropertyChangeListener l : listeneri) {
							logTotalSteps.removePropertyChangeListener("value", l);
						}
						logTotalSteps.setValue(totSteps);
						logTotalSteps.addPropertyChangeListener("value", this);
					} else if (source == logTotalSteps) {
						double totSteps = 5;
						try {
							totSteps = Double.parseDouble(logTotalSteps.getValue().toString());
						} catch (NumberFormatException ex) {
						}
						double percValue = Math.pow(100, 1.0 / totSteps) - 1.0;
						PropertyChangeListener[] listeneri = logStepPercent.getPropertyChangeListeners("value");
						for (PropertyChangeListener l : listeneri) {
							logStepPercent.removePropertyChangeListener("value", l);
						}
						logStepPercent.setValue(percValue);
						logStepPercent.addPropertyChangeListener("value", this);
					}
					initialActivityUpdater.update();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		};
		logStepPercent.addPropertyChangeListener("value", pcl);
		logTotalSteps.addPropertyChangeListener("value", pcl);
		if (nodeAttributes.hasAttribute(node.getIdentifier(), Model.Properties.LOG_STEP_PERCENT)) {
			logStepPercent.setValue(nodeAttributes.getDoubleAttribute(node.getIdentifier(), Model.Properties.LOG_STEP_PERCENT));
		} else {
			logStepPercent.setValue(0.1);
		}
		
		
		String[] moleculeTypes = new String[]{Model.Properties.TYPE_CYTOKINE, Model.Properties.TYPE_RECEPTOR, Model.Properties.TYPE_KINASE, Model.Properties.TYPE_PHOSPHATASE, Model.Properties.TYPE_TRANSCRIPTION_FACTOR, Model.Properties.TYPE_OTHER};
		final JComboBox<String> moleculeType = new JComboBox<String>(moleculeTypes);
		if (nodeAttributes.hasAttribute(node.getIdentifier(), Model.Properties.MOLECULE_TYPE)) {
			String type = nodeAttributes.getStringAttribute(node.getIdentifier(), Model.Properties.MOLECULE_TYPE);
			boolean notContained = true;
			for (String s : moleculeTypes) {
				if (s.equals(type)) {
					notContained = false;
				}
			}
			if (notContained) {
				moleculeType.setSelectedItem(Model.Properties.TYPE_OTHER);
			} else {
				moleculeType.setSelectedItem(nodeAttributes.getStringAttribute(node.getIdentifier(), Model.Properties.MOLECULE_TYPE));
			}
		} else {
			moleculeType.setSelectedItem(Model.Properties.TYPE_KINASE);
		}

		this.add(new LabelledField("Molecule type", moleculeType), new GridBagConstraints(0, 1, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		
		final JSwitchBox enabledNode = new JSwitchBox("Enabled", "Disabled"),
						 plottedNode = new JSwitchBox("Plotted", "Hidden");
		if (nodeAttributes.hasAttribute(node.getIdentifier(), Model.Properties.ENABLED)) {
			enabledNode.setSelected(nodeAttributes.getBooleanAttribute(node.getIdentifier(), Model.Properties.ENABLED));
		} else {
			enabledNode.setSelected(true);
		}
		if (nodeAttributes.hasAttribute(node.getIdentifier(), Model.Properties.PLOTTED)) {
			plottedNode.setSelected(nodeAttributes.getBooleanAttribute(node.getIdentifier(), Model.Properties.PLOTTED));
		} else {
			plottedNode.setSelected(true);
		}
		if (nodeAttributes.hasAttribute(node.getIdentifier(), Model.Properties.LINEAR_SCALE)) {
			discretizationChoice.setSelected(nodeAttributes.getBooleanAttribute(node.getIdentifier(), Model.Properties.LINEAR_SCALE));
		} else {
			discretizationChoice.setSelected(true);
		}
		ChangeListener cl = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				boolean linear = discretizationChoice.isSelected();
				totalLevelsField.setEnabled(linear);
				//initialLevelField.setEnabled(linear);
				logPercentageField.setEnabled(!linear);
				logTotalStepsField.setEnabled(!linear);
				initialActivityUpdater.update();
				NodeDialog.this.pack();
			}
		};
		discretizationChoice.addChangeListener(cl);
		cl.stateChanged(null);
		
		Box discretizationChoiceBox = new Box(BoxLayout.X_AXIS);
		discretizationChoiceBox.add(Box.createGlue());
		discretizationChoiceBox.add(discretizationChoice);
		discretizationChoiceBox.add(Box.createGlue());
		JPanel discretization = new JPanel();
		discretization.setLayout(new GridBagLayout());
		discretization.add(discretizationChoiceBox, new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		discretization.add(totalLevelsField, new GridBagConstraints(1, 1, 1, 2, 0.5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		//discretization.add(initialLevelField, new GridBagConstraints(0, 3, 2, 1, 0.5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		discretization.add(logPercentageField, new GridBagConstraints(0, 1, 1, 1, 0.5, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		discretization.add(logTotalStepsField, new GridBagConstraints(0, 2, 1, 1, 0.5, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		this.add(new LabelledField("Discretization", discretization), new GridBagConstraints(0, 3, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		this.add(initialLevelField, new GridBagConstraints(0, 4, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		
		this.add(enabledNode, new GridBagConstraints(1, 5, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		this.add(plottedNode, new GridBagConstraints(1, 6, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		
		final JTextPane description = new JTextPane();
		JScrollPane descriptionScrollPane = new JScrollPane(description);
		descriptionScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		descriptionScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		description.setPreferredSize(new Dimension(200, 100));
		description.setMinimumSize(new Dimension(150, 50));
		if (nodeAttributes.hasAttribute(node.getIdentifier(), Model.Properties.DESCRIPTION)) {
			description.setText(nodeAttributes.getStringAttribute(node.getIdentifier(), Model.Properties.DESCRIPTION));
		}
		this.add(new LabelledField("Description", descriptionScrollPane), new GridBagConstraints(1, 0, 1, 2, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		
		JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		controls.add(new JButton(new AbstractAction("Save") {
			private static final long serialVersionUID = -6179643943409321939L;

			@Override
			public void actionPerformed(ActionEvent e) {
				CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
				nodeAttributes.setAttribute(node.getIdentifier(), Model.Properties.INITIAL_LEVEL,
						initialConcentration.getValue());
				
				nodeAttributes.setAttribute(node.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS, totalLevels.getValue());
				
				double activityRatio = (double)initialConcentration.getValue() / totalLevels.getValue();
				nodeAttributes.setAttribute(node.getIdentifier(), Model.Properties.SHOWN_LEVEL, activityRatio);
				
				if (nameField.getText() != null && nameField.getText().length() > 0) {
					nodeAttributes.setAttribute(node.getIdentifier(), Model.Properties.CANONICAL_NAME, nameField.getText());
				}
				
				nodeAttributes.setAttribute(node.getIdentifier(), Model.Properties.MOLECULE_TYPE, moleculeType.getSelectedItem().toString());
				
				nodeAttributes.setAttribute(node.getIdentifier(), Model.Properties.ENABLED, enabledNode.isSelected());
				
				nodeAttributes.setAttribute(node.getIdentifier(), Model.Properties.PLOTTED, plottedNode.isSelected());
				
				nodeAttributes.setAttribute(node.getIdentifier(), Model.Properties.DESCRIPTION, description.getText());
				
				nodeAttributes.setAttribute(node.getIdentifier(), Model.Properties.LINEAR_SCALE, discretizationChoice.isSelected());
				nodeAttributes.setAttribute(node.getIdentifier(), Model.Properties.LOG_STEP_PERCENT, Double.parseDouble(logStepPercent.getValue().toString()));

				Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);

				NodeDialog.this.dispose();
			}
		}));
		
		JButton cancelButton = new JButton(new AbstractAction("Cancel") {
			private static final long serialVersionUID = -2038333013177775241L;
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// discard changes
				if (wasNewlyCreated) {
					Cytoscape.getCurrentNetwork().removeNode(node.getRootGraphIndex(), true);
				}
				NodeDialog.this.dispose();
			}
		});
		controls.add(cancelButton);
		
		//Associate the "Cancel" button with the Esc key
		getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CANCEL");
		getRootPane().getActionMap().put("CANCEL", cancelButton.getAction());

		this.add(controls, new GridBagConstraints(1, 7, 1, 2, 1.0, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0)); //BorderLayout.SOUTH);
	}
}
