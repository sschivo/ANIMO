package inat.analyser.uppaal.modelchecking;

import inat.model.Model;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class PathFormula extends JPanel {
	private static final long serialVersionUID = 8808396267534966874L;
	private final String IT_IS_POSSIBLE = "It is possible for state",
						 IT_IS_NOT_POSSIBLE = "It is NOT possible for state",
						 TO_OCCUR = "to occur",
						 IF_A_STATE = "If a state ",
						 OCCURS_THEN_IT_IS_FOLLOWED = "occurs, then it is NECESSARILY followed by state ",
						 A_STATE = "A state ",
						 CAN = "can",
						 MUST = "must",
						 PERSIST_INDEFINITELY = "persist indefinitely";
	private JRadioButton first, second, third;
	private StateFormula state1, state2, state3, state4;
	private JComboBox<String> combo1, combo2;
	private PathFormula selectedFormula = null;
	
	public PathFormula() {
		first = new JRadioButton();
		second = new JRadioButton();
		third = new JRadioButton();
		ButtonGroup bg = new ButtonGroup();
		bg.add(first);
		bg.add(second);
		bg.add(third);
		final ActionListener al = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (first.isSelected()) {
					combo1.setEnabled(true);
					state1.setEnabled(true);
					state2.setEnabled(false);
					state3.setEnabled(false);
					state4.setEnabled(false);
					combo2.setEnabled(false);
				} else if (second.isSelected()) {
					combo1.setEnabled(false);
					state1.setEnabled(false);
					state2.setEnabled(true);
					state3.setEnabled(true);
					state4.setEnabled(false);
					combo2.setEnabled(false);
				} else if (third.isSelected()) {
					combo1.setEnabled(false);
					state1.setEnabled(false);
					state2.setEnabled(false);
					state3.setEnabled(false);
					state4.setEnabled(true);
					combo2.setEnabled(true);
				}
			}
		};
		first.addActionListener(al);
		second.addActionListener(al);
		third.addActionListener(al);
		
		JPanel firstPath = new JPanel();
		firstPath.setLayout(new GridBagLayout());
		String[] choices1 = new String[]{IT_IS_POSSIBLE, IT_IS_NOT_POSSIBLE};
		combo1 = new JComboBox<String>(choices1);
		state1 = new StateFormula();
		JLabel toOccur = new JLabel(TO_OCCUR);
		firstPath.add(first, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 9));
		firstPath.add(combo1, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 9));
		firstPath.add(state1, new GridBagConstraints(2, 0, 1, 2, 0, 0, GridBagConstraints.PAGE_END, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		firstPath.add(toOccur, new GridBagConstraints(3, 0, GridBagConstraints.REMAINDER, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 9));
		MouseListener ml1 = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!first.isSelected()) {
					first.setSelected(true);
					al.actionPerformed(null);
				}
			}
		};
		combo1.addMouseListener(ml1);
		state1.addMouseListener(ml1);
		toOccur.addMouseListener(ml1);
		
		JPanel secondPath = new JPanel();
		secondPath.setLayout(new GridBagLayout());
		JLabel ifAState = new JLabel(IF_A_STATE);
		state2 = new StateFormula();
		JLabel occursThenItIsFollowed = new JLabel(OCCURS_THEN_IT_IS_FOLLOWED);
		state3 = new StateFormula();
		secondPath.add(second, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 16));
		secondPath.add(ifAState, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 16));
		secondPath.add(state2, new GridBagConstraints(2, 0, 1, 2, 0, 0, GridBagConstraints.PAGE_END, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		secondPath.add(occursThenItIsFollowed, new GridBagConstraints(3, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 16));
		secondPath.add(state3, new GridBagConstraints(4, 0, GridBagConstraints.REMAINDER, 2, 1, 0, GridBagConstraints.PAGE_END, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		MouseListener ml2 = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!second.isSelected()) {
					second.setSelected(true);
					al.actionPerformed(null);
				}
			}
		};
		ifAState.addMouseListener(ml2);
		state2.addMouseListener(ml2);
		occursThenItIsFollowed.addMouseListener(ml2);
		state3.addMouseListener(ml2);
		
		JPanel thirdPath = new JPanel();
		thirdPath.setLayout(new GridBagLayout());
		JLabel aState = new JLabel(A_STATE);
		state4 = new StateFormula();
		String[] choices2 = new String[]{CAN, MUST};
		combo2 = new JComboBox<String>(choices2);
		JLabel persistIndefinitely = new JLabel(PERSIST_INDEFINITELY);
		thirdPath.add(third, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 9));
		thirdPath.add(aState, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 9));
		thirdPath.add(state4, new GridBagConstraints(2, 0, 1, 2, 0, 0, GridBagConstraints.PAGE_END, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		thirdPath.add(combo2, new GridBagConstraints(3, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 9));
		thirdPath.add(persistIndefinitely, new GridBagConstraints(4, 0, GridBagConstraints.REMAINDER, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 9));
		MouseListener ml3 = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!third.isSelected()) {
					third.setSelected(true);
					al.actionPerformed(null);
				}
			}
		};
		aState.addMouseListener(ml3);
		state4.addMouseListener(ml3);
		combo2.addMouseListener(ml3);
		persistIndefinitely.addMouseListener(ml3);
		
		Box formulae = new Box(BoxLayout.Y_AXIS);
		formulae.add(firstPath);
		formulae.add(Box.createVerticalStrut(15));
		formulae.add(secondPath);
		formulae.add(Box.createVerticalStrut(15));
		formulae.add(thirdPath);
		first.setSelected(true);
		al.actionPerformed(null);
		
		this.add(formulae);
	}
	
	public void setReactantIDs(Model m) {
		getSelectedFormula().setReactantIDs(m);
	}
	
	public PathFormula getSelectedFormula() {
		if (selectedFormula == null) {
			if (first.isSelected()) {
				if (combo1.getSelectedItem().equals(IT_IS_POSSIBLE)) {
					selectedFormula = new EventuallyExistsPath(state1.getSelectedFormula());
				} else if (combo1.getSelectedItem().equals(IT_IS_NOT_POSSIBLE)) {
					selectedFormula = new AlwaysAllPaths(new NotStateFormula(state1.getSelectedFormula()));
				}
			} else if (second.isSelected()) {
				selectedFormula = new Implies(state2.getSelectedFormula(), state3.getSelectedFormula());
			} else if (third.isSelected()) {
				if (combo2.getSelectedItem().equals(CAN)) {
					selectedFormula = new EventuallyAllPaths(state4.getSelectedFormula());
				} else if (combo2.getSelectedItem().equals(MUST)) {
					selectedFormula = new AlwaysAllPaths(state4.getSelectedFormula());
				}
			}
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
}
