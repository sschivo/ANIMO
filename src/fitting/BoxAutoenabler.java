package fitting;
import java.awt.Component;
import java.awt.Container;

import javax.swing.Box;

public class BoxAutoenabler extends Box {
		private static final long serialVersionUID = -1149150986289957436L;

		public BoxAutoenabler(int axis) {
			super(axis);
		}
		
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			Component[] components = this.getComponents();
			for (int i=0;i<components.length;i++) {
				setEnabledRec(components[i], enabled);
			}
		}
		
		private void setEnabledRec(Component c, boolean enabled) {
			if (c instanceof Container) {
				Container cont = (Container)c;
				Component[] components = cont.getComponents();
				for (int i=0;i<components.length;i++) {
					setEnabledRec(components[i], enabled);
				}
			}
			c.setEnabled(enabled);
		}
	}