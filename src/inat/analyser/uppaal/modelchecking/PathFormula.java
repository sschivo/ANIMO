package inat.analyser.uppaal.modelchecking;

import inat.model.Model;

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
		
		Box firstPath = new Box(BoxLayout.X_AXIS);
		String[] choices1 = new String[]{IT_IS_POSSIBLE, IT_IS_NOT_POSSIBLE};
		combo1 = new JComboBox<String>(choices1);
		state1 = new StateFormula();
		JLabel toOccur = new JLabel(TO_OCCUR);
		firstPath.add(first);
		firstPath.add(combo1);
		firstPath.add(state1);
		firstPath.add(toOccur);
		firstPath.add(Box.createGlue());
		
		Box secondPath = new Box(BoxLayout.X_AXIS);
		JLabel ifAState = new JLabel(IF_A_STATE);
		state2 = new StateFormula();
		JLabel occursThenItIsFollowed = new JLabel(OCCURS_THEN_IT_IS_FOLLOWED);
		state3 = new StateFormula();
		secondPath.add(second);
		secondPath.add(ifAState);
		secondPath.add(state2);
		secondPath.add(occursThenItIsFollowed);
		secondPath.add(state3);
		secondPath.add(Box.createGlue());
		
		Box thirdPath = new Box(BoxLayout.X_AXIS);
		JLabel aState = new JLabel(A_STATE);
		state4 = new StateFormula();
		String[] choices2 = new String[]{CAN, MUST};
		combo2 = new JComboBox<String>(choices2);
		JLabel persistIndefinitely = new JLabel(PERSIST_INDEFINITELY);
		thirdPath.add(third);
		thirdPath.add(aState);
		thirdPath.add(state4);
		thirdPath.add(combo2);
		thirdPath.add(persistIndefinitely);
		thirdPath.add(Box.createGlue());
		
		Box formulae = new Box(BoxLayout.Y_AXIS);
		formulae.add(firstPath);
		formulae.add(secondPath);
		formulae.add(thirdPath);
		first.setSelected(true);
		
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
	
	
	public String toString() {
		return getSelectedFormula().toString();
	}
	
	public String toHumanReadable() {
		return getSelectedFormula().toHumanReadable();
	}
}
