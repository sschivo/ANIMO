package inat.cytoscape;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.Vector;

import javax.swing.JPanel;

import cytoscape.Cytoscape;
import cytoscape.visual.NodeAppearanceCalculator;
import cytoscape.visual.NodeShape;
import cytoscape.visual.VisualMappingManager;
import cytoscape.visual.VisualPropertyType;
import cytoscape.visual.VisualStyle;
import cytoscape.visual.calculators.Calculator;
import cytoscape.visual.mappings.DiscreteMapping;
import cytoscape.visual.mappings.ObjectMapping;

public class ShapesLegend extends JPanel {
	private static final long serialVersionUID = 8963894565747542198L;
	private DiscreteMapping shapesMap, widthsMap, heightsMap;

	public ShapesLegend() {
				
	}
	
	public void updateFromSettings() {
		VisualMappingManager vizMap = Cytoscape.getVisualMappingManager();
		VisualStyle visualStyle = vizMap.getVisualStyle();
		NodeAppearanceCalculator nac = visualStyle.getNodeAppearanceCalculator();
		DiscreteMapping shapesMap = null,
						widthsMap = null,
						heightsMap = null;
		Vector<ObjectMapping> mappings;
		Calculator calc = nac.getCalculator(VisualPropertyType.NODE_SHAPE);
		if (calc != null) {
			mappings = calc.getMappings();
			for (ObjectMapping om : mappings) {
				if (om instanceof DiscreteMapping) {
					shapesMap = (DiscreteMapping)om;
					break;
				}
			}
		}
		calc = nac.getCalculator(VisualPropertyType.NODE_WIDTH);
		if (calc != null) {
			mappings = calc.getMappings();
			for (ObjectMapping om : mappings) {
				if (om instanceof DiscreteMapping) {
					widthsMap = (DiscreteMapping)om;
					break;
				}
			}
		}
		calc = nac.getCalculator(VisualPropertyType.NODE_HEIGHT);
		if (calc != null) {
			mappings = calc.getMappings();
			for (ObjectMapping om : mappings) {
				if (om instanceof DiscreteMapping) {
					heightsMap = (DiscreteMapping)om;
					break;
				}
			}
		}
		if (shapesMap != null && widthsMap != null && heightsMap != null) {
			this.setParameters(shapesMap, widthsMap, heightsMap);
		}
	}
	
	public void setParameters(DiscreteMapping shapesMap, DiscreteMapping widthsMap, DiscreteMapping heightsMap) {
		this.shapesMap = shapesMap;
		this.widthsMap = widthsMap;
		this.heightsMap = heightsMap;
		this.repaint();
	}
	
	@SuppressWarnings("unchecked")
	public void paint(Graphics g1) {
		if (shapesMap == null || widthsMap == null || heightsMap == null) {
			super.paint(g1);
			return;
		}
		Graphics2D g = (Graphics2D)g1;
		g.setPaint(Color.WHITE);
		g.fill(new Rectangle2D.Float(0, 0, this.getWidth(), this.getHeight()));
		float rWidth = Math.min(this.getWidth(), 400);
		Rectangle2D.Float rectangle = new Rectangle2D.Float((this.getWidth() - rWidth / 2.5f) / 2, 0.1f * this.getHeight(), rWidth / 3, 0.9f * this.getHeight() - 1); //this.getWidth() / 2 - (this.getWidth() / 1.5f), 0, this.getWidth() / 3, this.getHeight());
		FontMetrics fm = g.getFontMetrics();
		rectangle.y += 10 + fm.getHeight();
		rectangle.height -= 10 + fm.getHeight();
		rectangle.x += 1; rectangle.width -= 2; rectangle.y += 1; rectangle.height -= 2; //Otherwise we can't properly see the contours because they would be drawn on a limit that isn't actually there
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Font oldFont = g.getFont();
		g.setFont(oldFont.deriveFont(Font.BOLD));
		g.setPaint(Color.BLACK);
		g.drawString("Protein category", this.getWidth() / 2 - fm.stringWidth("Protein category") / 2, 0.1f * this.getHeight() + fm.getHeight());
		g.setFont(oldFont);
		
		Map<String, NodeShape> shapes = shapesMap.getAll();
		Map<String, Object> widths = widthsMap.getAll(), //The maps SHOULD be to Float, but the smart Cytoscape reads the mapping from file as Double, so we must make sure that we work in both cases.
							heights = heightsMap.getAll();
		
		float nodeSpace = rectangle.height / shapes.size();
		
		float maxHeight = -1;
		int maxStrLength = -1;
		for (String moleculeType : heights.keySet()) {
			Object o = heights.get(moleculeType);
			float h = 50.0f; 
			if (o instanceof Float) {
				h = (Float)o;
			} else if (o instanceof Double) {
				h = new Float((Double)o);
			}
			if (h > maxHeight) {
				maxHeight = h;
			}
			int strLen = fm.stringWidth(moleculeType);
			if (strLen > maxStrLength) {
				maxStrLength = strLen;
			}
		}
		float x = rectangle.x + rectangle.width / 2 - maxStrLength / 2.0f,
			  y = rectangle.y;
		
		for (String moleculeType : shapes.keySet()) {
			NodeShape shape = shapes.get(moleculeType);
			float width = 50.0f,
				  height = 50.0f;
			Object o1 = widths.get(moleculeType);
			if (o1 instanceof Float) {
				width = (Float)o1;
			} else if (o1 instanceof Double) {
				width = new Float((Double)o1);
			}
			Object o2 = heights.get(moleculeType);
			if (o2 instanceof Float) {
				height = (Float)o2;
			} else if (o2 instanceof Double) {
				height = new Float((Double)o2);
			}
			float rate = 0.75f * nodeSpace / maxHeight;
			width *= rate;
			height *= rate;
			g.setStroke(new BasicStroke(2.0f));
			switch (shape) {
				case DIAMOND:
					int xD[] = new int[]{(int)(x - width / 2), (int)x, (int)(x + width / 2), (int)x},
						yD[] = new int[]{(int)(y + nodeSpace / 2), (int)(y + nodeSpace / 2 - height / 2), (int)(y + nodeSpace / 2), (int)(y + nodeSpace / 2 + height / 2)};
					Polygon polygon = new Polygon(xD, yD, xD.length);
					g.setPaint(Color.DARK_GRAY);
					g.fillPolygon(polygon);
					g.setPaint(Color.BLACK);
					g.drawPolygon(polygon);
					break;
				case ELLIPSE:
					int xE = (int)(x - width / 2),
						yE = (int)(y + nodeSpace / 2 - height / 2);
					g.setPaint(Color.DARK_GRAY);
					g.fillOval(xE, yE, (int)width, (int)height);
					g.setPaint(Color.BLACK);
					g.drawOval(xE, yE, (int)width, (int)height);
					break;
				case HEXAGON:
					break;
				case OCTAGON:
					break;
				case PARALLELOGRAM:
					break;
				case RECT:
					int xR = (int)(x - width / 2),
						yR = (int)(y + nodeSpace / 2 - height / 2);
					Rectangle2D.Float rect = new Rectangle2D.Float(xR, yR, width, height);
					g.setPaint(Color.DARK_GRAY);
					g.fill(rect);
					g.setPaint(Color.BLACK);
					g.draw(rect);
					break;
				case RECT_3D:
					break;
				case ROUND_RECT:
					break;
				case TRAPEZOID:
					break;
				case TRAPEZOID_2:
					break;
				case TRIANGLE:
					break;
				case VEE:
					break;
				default: break;
			}
			g.drawString(moleculeType, x + rectangle.width /2, y + nodeSpace / 2 + fm.getAscent() / 2.0f);
			y += nodeSpace;
		}
	}
}
