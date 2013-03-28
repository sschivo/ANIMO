package inat.analyser.uppaal.modelchecking;

import giny.model.Node;
import inat.model.Model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;

public class StateFormula extends JPanel {
private static final long serialVersionUID = -7020762010666811781L;

	public static char REACTANT_NAME_DELIMITER = '@';
	private ReactantId[] reactants;
	private JComboBox<ReactantId> combo1;
	private JComboBox<BoundType> combo2;
	private JLabel boundValue;
	private JSlider slider;
	private StateFormula selectedFormula = null;
	
	@SuppressWarnings("unchecked")
	public StateFormula() {
		Box content = new Box(BoxLayout.X_AXIS);
		//Read the list of reactant identifiers and aliases from the nodes in the current network
		CyNetwork network = Cytoscape.getCurrentNetwork();
		final CyAttributes nodeAttrib = Cytoscape.getNodeAttributes();
		//reactantAliases = new String[network.getNodeCount()];
		//reactantIdentifiers = new String[network.getNodeCount()];
		Vector<ReactantId> reactantsV = new Vector<ReactantId>();
		Iterator<Node> nodes = (Iterator<Node>) network.nodesIterator();
		//for (int i = 0; nodes.hasNext(); i++) {
		while (nodes.hasNext()) {
			Node node = nodes.next();
			if (nodeAttrib.hasAttribute(node.getIdentifier(), Model.Properties.ENABLED)
				&& nodeAttrib.getBooleanAttribute(node.getIdentifier(), Model.Properties.ENABLED) == false) { //Don't allow disabled nodes to be used in the formula
				continue;
			}
			//reactantIdentifiers[i] = node.getIdentifier();
			String identif = node.getIdentifier(),
				   alias = identif;
			if (nodeAttrib.hasAttribute(node.getIdentifier(), Model.Properties.CANONICAL_NAME)) {
				//reactantAliases[i] = nodeAttrib.getStringAttribute(node.getIdentifier(), Model.Properties.CANONICAL_NAME);
				alias = nodeAttrib.getStringAttribute(node.getIdentifier(), Model.Properties.CANONICAL_NAME);
			}
			reactantsV.add(new ReactantId(alias, identif));
		}
		Collections.sort(reactantsV);
		reactants = reactantsV.toArray(new ReactantId[]{});
		combo1 = new JComboBox<ReactantId>(reactants);
		combo1.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				ReactantId reactant = (ReactantId)combo1.getSelectedItem();
				int nLevels = nodeAttrib.getIntegerAttribute(reactant.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS);
				if (slider != null) {
					slider.setMaximum(nLevels);
					int space = (slider.getMaximum() - slider.getMinimum() + 1) / 5;
					if (space < 1) space = 1;
					slider.setMajorTickSpacing(space);
					slider.setMinorTickSpacing(space / 2);
					Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
					for (int i=slider.getMinimum();i<=slider.getMaximum();i+=space) {
						labelTable.put(i, new JLabel("" + i));
					}
					slider.setLabelTable(labelTable);
					int init = nodeAttrib.getIntegerAttribute(reactant.getIdentifier(), Model.Properties.INITIAL_LEVEL);
					slider.setValue(init);
				}
			}
			
		});
		BoundType[] bounds = new BoundType[]{BoundType.LT, BoundType.LE, BoundType.EQ, BoundType.GE, BoundType.GT};
		combo2 = new JComboBox<BoundType>(bounds);
		boundValue = new JLabel("0");
		slider = new JSlider(0, 100);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.setMajorTickSpacing(20);
		slider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				boundValue.setText("" + slider.getValue());
			}

		});
		content.add(combo1);
		content.add(combo2);
		content.add(boundValue);
		//content.add(slider);
		Box contentV = new Box(BoxLayout.Y_AXIS);
		contentV.add(content);
		contentV.add(slider);
		combo1.setSelectedIndex(0);
		this.add(contentV);
		slider.setValue(slider.getMaximum() / 2);
		boundValue.setText("" + slider.getValue());
	}
	
	public void setEnabled(boolean enabled) {
		combo1.setEnabled(enabled);
		combo2.setEnabled(enabled);
		slider.setEnabled(enabled);
	}
	
	public void addMouseListener(MouseListener ml) {
		super.addMouseListener(ml);
		combo1.addMouseListener(ml);
		combo2.addMouseListener(ml);
		slider.addMouseListener(ml);
	}
	
	public void setReactantIDs(Model m) {
		getSelectedFormula().setReactantIDs(m);
	}
	
	public StateFormula getSelectedFormula() {
		if (selectedFormula == null) {
			//String selectedID = REACTANT_NAME_DELIMITER + reactantIdentifiers[combo1.getSelectedIndex()] + REACTANT_NAME_DELIMITER;
			ReactantId reactant = (ReactantId)combo1.getSelectedItem();
			selectedFormula = new ActivityBound(new ReactantName(reactant.getIdentifier(), reactant.getAlias()), (BoundType)combo2.getSelectedItem(), "" + slider.getValue());
		}
		return selectedFormula;
	}
	
	public boolean supportsPriorities() {
		return getSelectedFormula().supportsPriorities();
	}
	
	public String toString() {
		return getSelectedFormula().toString();
	}
	
	public String toHumanReadable() {
		return getSelectedFormula().toHumanReadable();
	}
	
	private class ReactantId implements Comparable<ReactantId> {
		private String alias, identifier;
		
		public ReactantId(String alias, String identifier) {
			this.alias = alias;
			this.identifier = identifier;
		}
		
		public String getAlias() {
			return alias;
		}
		
		public String getIdentifier() {
			return identifier;
		}
		
		public String toString() {
			return getAlias();
		}
		
		@Override
		public int compareTo(ReactantId o) {
			return this.alias.compareTo(o.alias);
		}
	}
}
