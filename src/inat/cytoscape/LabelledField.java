package inat.cytoscape;


import java.awt.Color;
import java.awt.Component;

import javax.swing.*;
import javax.swing.border.TitledBorder;

/**
 * "Simplified" version of ComponentTitledBorder: show a component (typically a text field)
 * with a border around it, and a title on the border. I sometimes use it to gain some space
 * in the user interface.
 */
public class LabelledField extends JPanel {
	private static final long serialVersionUID = 7369414240895916427L;
	private String title = null;
	private Component field = null;

    public LabelledField(String title, JComponent field) {
    	this.setLayout(new java.awt.BorderLayout());
    	this.title = title;
    	this.field = field;
        this.setBorder(BorderFactory.createTitledBorder(title));
        add(field);
    }

    public LabelledField(String title, JComponent field, String toolTip) {
    	this.setLayout(new java.awt.BorderLayout());
    	this.title = title;
    	this.field = field;
        this.setBorder(BorderFactory.createTitledBorder(title));
        field.setToolTipText(toolTip);
        add(field);
    }
    
    public void setEnabled(boolean enabled) {
    	super.setEnabled(enabled);
    	TitledBorder t = (TitledBorder)getBorder();
    	if (enabled) {
    		t.setTitleColor(Color.BLACK);
    	} else {
    		t.setTitleColor(Color.GRAY);
    	}
    	this.repaint();
    }
    
    public String getTitle() {
    	return title;
    }
    
    public void setTitle(String title) {
    	TitledBorder t = (TitledBorder)getBorder();
    	t.setTitle(title);
    	this.repaint();
    }
    
    public Component getField() {
    	return this.field;
    }
}

