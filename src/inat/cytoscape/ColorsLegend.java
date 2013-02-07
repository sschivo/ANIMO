package inat.cytoscape;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

public class ColorsLegend extends JPanel {
	private static final long serialVersionUID = 9182942528018588493L;
	private float[] fractions = null;
	private Color[] colors = null;
	
	public ColorsLegend() {
		
	}
	
	public void setParameters(float[] fractions, Color[] colors) {
		this.fractions = fractions;
		this.colors = colors;
		this.repaint();
	}
	
	public void paint(Graphics g1) {
		if (fractions == null || colors == null) {
			super.paint(g1);
			return;
		}
		Graphics2D g = (Graphics2D)g1;
		g.setPaint(Color.WHITE);
		g.fill(new Rectangle2D.Float(0, 0, this.getWidth(), this.getHeight()));
		float width = Math.min(this.getWidth(), 400);
		Rectangle2D.Float rectangle = new Rectangle2D.Float(this.getWidth() / 2 - width / 3, 0, width / 4, 0.9f * this.getHeight());
		FontMetrics fm = g.getFontMetrics();
		rectangle.y += 10 + fm.getHeight();
		rectangle.height -= 10 + fm.getHeight();
		rectangle.x += 1; rectangle.width -= 2; rectangle.y += 1; rectangle.height -= 2; //Otherwise we can't properly see the contours because they would be drawn on a limit that isn't actually there
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Font oldFont = g.getFont();
		g.setFont(oldFont.deriveFont(Font.BOLD));
		g.setPaint(Color.BLACK);
		g.drawString("Activity", this.getWidth() / 2 - fm.stringWidth("Activity") / 2, fm.getHeight());
		g.setFont(oldFont);
		g.setPaint(new LinearGradientPaint(rectangle.x + rectangle.width / 2.0f, rectangle.y, rectangle.x + rectangle.width / 2.0f, rectangle.y + rectangle.height, fractions, colors));
		g.fill(rectangle);
		g.setPaint(Color.BLACK);
		g.draw(rectangle);
		
		int xT[] = {(int)(rectangle.x + rectangle.width + 10), (int)(rectangle.x + rectangle.width + 10 + rectangle.width / 2), (int)(rectangle.x + rectangle.width + 10) + 4},
			yT[] = {(int)rectangle.y, (int)rectangle.y, (int)(rectangle.y + rectangle.height)};
		g.fill(new Polygon(xT, yT, xT.length));
		
		g.drawString("Max", xT[1] + 5, yT[0] + fm.getAscent());
		g.drawString("Min", xT[1] + 5, yT[2]);
	}
}
