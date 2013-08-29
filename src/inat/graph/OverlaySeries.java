package inat.graph;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Vector;

public class OverlaySeries extends Series {
	private Vector<Series> allSeries;
	
	public OverlaySeries(String name, Vector<Series> allSeries, Scale scale) {
		super(name, scale);
		this.allSeries = allSeries;
		for (Series s : this.allSeries) {
			s.setMaster(this);
			s.setScale(this.getScale());
		}
	}
	
	@Override
	public void plot(Graphics2D g, Rectangle bounds, boolean stepShapedLines, int SCALA) {
		myColor = g.getColor();
		for (Series s : allSeries) {
			s.plot(g, bounds, stepShapedLines, SCALA);
		}
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		for (Series s : this.allSeries) {
			s.setEnabled(enabled);
		}
	}
}
