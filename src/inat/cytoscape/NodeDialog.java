package inat.cytoscape;

import giny.model.Node;
import inat.model.Model;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
			//this.setTitle("Reactant " + res.toString());
			name = res.toString();
		} else {
			name = null;
		}
		if (!nodeAttributes.hasAttribute(node.getIdentifier(), Model.Properties.INITIAL_LEVEL)) {
			nodeAttributes.setAttribute(node.getIdentifier(), Model.Properties.INITIAL_LEVEL, 0);
		}
		
		this.setLayout(new GridBagLayout()); //BorderLayout(2, 2));
		
		//JPanel values = new JPanel(new GridLayout(3, 2, 2, 2));
		//JPanel values = new JPanel(new GridBagLayout()); //You REALLY don't want to know how GridBagLayout works...
		//Box values = new Box(BoxLayout.Y_AXIS);
		
		int levels;
		if (nodeAttributes.hasAttribute(node.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS)) {
			levels = nodeAttributes.getIntegerAttribute(node.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS);
		} else if (networkAttributes.hasAttribute(Cytoscape.getCurrentNetwork().getIdentifier(), Model.Properties.NUMBER_OF_LEVELS)) {
			levels = networkAttributes.getIntegerAttribute(Cytoscape.getCurrentNetwork().getIdentifier(), Model.Properties.NUMBER_OF_LEVELS);
		} else {
			levels = 15;
		}
		
		//JLabel nameLabel = new JLabel("Reactant name:");
		final JTextField nameField = new JTextField(name);
		//values.add(nameLabel, new GridBagConstraints(0, 0, 1, 1, 0.3, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		//values.add(nameField, new GridBagConstraints(1, 0, 1, 1, 1, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		//values.add(new LabelledField("Name", nameField));
		this.add(new LabelledField("Name", nameField), new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		
		//final JLabel totalLevelsLabel = new JLabel("Total activity levels: " + levels);
		//values.add(totalLevelsLabel, new GridBagConstraints(0, 1, 1, 1, 0.3, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
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
		////values.add(totalLevels, new GridBagConstraints(1, 1, 1, 1, 1, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		final LabelledField totalLevelsField = new LabelledField("Total activity levels: " + levels, totalLevels);
		//values.add(totalLevelsField);
		
		
		final JSlider initialConcentration = new JSlider(0, levels);
		initialConcentration.setValue(nodeAttributes.getIntegerAttribute(node.getIdentifier(), Model.Properties.INITIAL_LEVEL));
		
		////final JLabel initialConcentrationLabel = new JLabel("Initial activity level: " + initialConcentration.getValue());
		////values.add(initialConcentrationLabel, new GridBagConstraints(0, 2, 1, 1, 0.3, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		final LabelledField initialLevelField = new LabelledField("Initial activity level: " + initialConcentration.getValue(), initialConcentration);


		initialConcentration.setMajorTickSpacing(levels / 5);
		initialConcentration.setMinorTickSpacing(levels / 10);
		
		initialConcentration.setPaintLabels(true);
		initialConcentration.setPaintTicks(true);

		////values.add(initialConcentration, new GridBagConstraints(1, 2, 1, 1, 1, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		//values.add(initialLevelField);

		//this.add(values, BorderLayout.CENTER);

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
		
		
		initialConcentration.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				initialLevelField.setTitle("Initial activity level: " + initialConcentration.getValue());
			}
			
		});
		
		
		//Box optionBoxes = new Box(BoxLayout.Y_AXIS);
		String[] moleculeTypes = new String[]{Model.Properties.TYPE_CYTOKINE, Model.Properties.TYPE_RECEPTOR, Model.Properties.TYPE_KINASE, Model.Properties.TYPE_PHOSPHATASE, Model.Properties.TYPE_TRANSCRIPTION_FACTOR, Model.Properties.TYPE_GENE, Model.Properties.TYPE_MRNA, Model.Properties.TYPE_DUMMY, Model.Properties.TYPE_OTHER};
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
		//optionBoxes.add(new LabelledField("Molecule type", moleculeType));
		//values.add(new LabelledField("Molecule type", moleculeType));
		this.add(new LabelledField("Molecule type", moleculeType), new GridBagConstraints(0, 1, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		//optionBoxes.add(totalLevelsField);
		//optionBoxes.add(initialLevelField);
		this.add(totalLevelsField, new GridBagConstraints(1, 0, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		this.add(initialLevelField, new GridBagConstraints(1, 1, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		
		
		final JRadioButton enabledNode = new JRadioButton("Enabled"),
					 disabledNode = new JRadioButton("Disabled"),
					 plottedNode = new JRadioButton("Plotted"),
					 hiddenNode = new JRadioButton("Hidden");
		ButtonGroup enabledGroup = new ButtonGroup(),
					plottedGroup = new ButtonGroup();
		enabledGroup.add(enabledNode);
		enabledGroup.add(disabledNode);
		plottedGroup.add(plottedNode);
		plottedGroup.add(hiddenNode);
		if (nodeAttributes.hasAttribute(node.getIdentifier(), Model.Properties.ENABLED)) {
			enabledNode.setSelected(nodeAttributes.getBooleanAttribute(node.getIdentifier(), Model.Properties.ENABLED));
		} else {
			enabledNode.setSelected(true);
		}
		disabledNode.setSelected(!enabledNode.isSelected());
		if (nodeAttributes.hasAttribute(node.getIdentifier(), Model.Properties.PLOTTED)) {
			plottedNode.setSelected(nodeAttributes.getBooleanAttribute(node.getIdentifier(), Model.Properties.PLOTTED));
		} else {
			plottedNode.setSelected(true);
		}
		hiddenNode.setSelected(!plottedNode.isSelected());
		Box enabledBox = new Box(BoxLayout.X_AXIS);
		enabledBox.add(enabledNode);
		enabledBox.add(Box.createGlue());
		enabledBox.add(disabledNode);
		//optionBoxes.add(enabledBox);
		this.add(enabledBox, new GridBagConstraints(1, 2, 1, 1, 0.5, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		Box plottedBox = new Box(BoxLayout.X_AXIS);
		plottedBox.add(plottedNode);
		plottedBox.add(Box.createGlue());
		plottedBox.add(hiddenNode);
		//optionBoxes.add(plottedBox);
		//optionBoxes.add(Box.createVerticalStrut(150));
		this.add(plottedBox, new GridBagConstraints(1, 3, 1, 1, 0.5, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		//this.add(optionBoxes, BorderLayout.EAST);
		
		final JTextPane description = new JTextPane();
		JScrollPane descriptionScrollPane = new JScrollPane(description);
		descriptionScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		descriptionScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		description.setPreferredSize(new Dimension(400, 100));
		description.setMinimumSize(new Dimension(150, 50));
		if (nodeAttributes.hasAttribute(node.getIdentifier(), Model.Properties.DESCRIPTION)) {
			description.setText(nodeAttributes.getStringAttribute(node.getIdentifier(), Model.Properties.DESCRIPTION));
		}
		this.add(new LabelledField("Description", descriptionScrollPane), new GridBagConstraints(0, 4, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		
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
//					Cytoscape.getCurrentNetwork().getRootGraph().removeNode(node);
//					Cytoscape.getRootGraph().removeNode(node);
				}
				NodeDialog.this.dispose();
			}
		});
		controls.add(cancelButton);
		
		//Associate the "Cancel" button with the Esc key
		getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CANCEL");
		getRootPane().getActionMap().put("CANCEL", cancelButton.getAction());

		this.add(controls, new GridBagConstraints(1, 5, 1, 2, 1.0, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0)); //BorderLayout.SOUTH);
	}
}
