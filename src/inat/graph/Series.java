package inat.graph;

import inat.analyser.uppaal.ResultAverager;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

/**
 * Represents a series of points (x,y) with a title.
 * It draws itself on the graph panel, with the given color.
 * If the series is a "slave" it means that it is "attached"
 * to another series. In particular, the slave series represents
 * the standard deviation of its master with (optional) vertical
 * bars and shading
 */
public class Series implements Comparable<Series> {
	protected static int seriesCounter = 0;
	
	private P[] data = null;
	private String name = "";
	private boolean enabled = true;
	private Scale scale = null;
	private Series master = null, slave = null; //ideally, the slave series should be used to represent confidence intervals for the corresponding master series
	//TODO: also here, we assume that ResultAverager.STD_DEV is all lowercase
	public static String SLAVE_SUFFIX = ResultAverager.STD_DEV, //for a series to be a representation of confidence intervals of series ABC, its name should be "ABC" + SLAVE_SUFFIX (suffix can have any capitalization).
						 OVERLAY_SUFFIX = ResultAverager.OVERLAY_NAME; //if a series contains this string, it is part of an overlay graph, so we should make a group of all the series with the same identifier
	protected Color myColor = null;
	private boolean changeColor = false;
	private enum BarsState {NOT_SHOWN, ONLY_BARS, ONLY_SHADING, SHADING_WITH_BARS, DOTS_WITH_BARS};
	private enum Symbol {CIRCLE, TRIANGLE_UP, SQUARE, TRIANGLE_DOWN, DIAMOND, X};
	private Symbol[] symbols = new Symbol[]{Symbol.CIRCLE, Symbol.TRIANGLE_UP, Symbol.SQUARE, Symbol.TRIANGLE_DOWN, Symbol.DIAMOND, Symbol.X};
	protected Symbol symbol = Symbol.CIRCLE;
	private BarsState barsState = BarsState.ONLY_BARS; //valid only if this Series is a slave. Tells to show the vertical error bars
	
	public Series(String name, Scale scale) {
		this.data = null;
		this.name = name;
		this.scale = scale;
	}
	
	public Series(P[] data) {
		this(data, new Scale());
	}
	
	public Series(P[] data, Scale scale) {
		this(data, scale, "Series " + (++seriesCounter)); 
	}
	
	public Series(P[] data, Scale scale, String name) {
		this.data = data;
		this.setScale(scale);
		this.name = name;
	}
	
	@Override
	public int compareTo(Series s) {
		return this.name.toLowerCase().compareTo(s.name.toLowerCase());
	}
	
	public void setScale(Scale scale) {
		this.scale = scale;
		if (!isStdDev()) {
			this.scale.addData(data);
		} else {
			P[] dataLow = new P[data.length];
			P[] dataHigh = new P[data.length];
			for (int i=0;i<data.length;i++) {
				dataLow[i] = new P(data[i].x, master.data[i].y - data[i].y);
				dataHigh[i] = new P(data[i].x, master.data[i].y + data[i].y);
			}
			this.scale.addData(dataLow);
			this.scale.addData(dataHigh);
		}
	}
	
	public Scale getScale() {
		return this.scale;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public P[] getData() {
		return this.data;
	}
	
	public void setSlave(Series s) {
		this.setSlave(s, true);
	}
	
	private void setSlave(Series s, boolean propagate) {
		this.slave = s;
		if (propagate) {
			s.setMaster(this, false);
		}
	}
	
	public void setMaster(Series s) {
		this.setMaster(s, true);
	}
	
	private void setMaster(Series s, boolean propagate) {
		this.master = s;
		this.setScale(this.master.getScale());
		if (propagate) {
			s.setSlave(this, false);
		}
	}
	
	public boolean isSlave() {
		return this.master != null;
	}
	
	public boolean isStdDev() {
		return isSlave() && !(this.master instanceof OverlaySeries);
	}
	
	public boolean isMaster() {
		return this.slave != null; 
	}
	
	public Series getSlave() {
		return this.slave;
	}
	
	public Series getMaster() {
		return this.master;
	}
	
	//wether to show this series or not
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (slave != null) {
			this.slave.setEnabled(enabled);
		}
	}
	
	public boolean getEnabled() {
		return this.enabled;
	}
	
	public void setChangeColor(boolean changeColor) {
		this.changeColor = changeColor;
	}
	
	public boolean getChangeColor() {
		return this.changeColor;
	}
	
	public Color getColor() {
		return this.myColor;
	}
	
	public void changeErrorBars() {
		BarsState[] states = BarsState.values();
		int idx = 0;
		for (int i = 0; i < states.length; i++) {
			if (this.barsState.equals(states[i])) {
				idx = i;
			}
		}
		idx++;
		if (idx >= states.length) idx = 0;
		this.barsState = states[idx];
	}
	
	public BarsState getErrorBars() {
		return this.barsState;
	}
	
	/**
	 * x and y identify the center of the symbol
	 * @param g
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @return true if the symbol was drawn, false otherwise
	 */
	private Polygon polygon = null;
	public boolean drawSymbol(Graphics2D g, int x, int y, int width, int height) {
		if (this.slave != null && this.slave.barsState == BarsState.DOTS_WITH_BARS) {
			switch (symbol) {
				case CIRCLE:
					g.fillOval((int)(x - width / 2.0), (int)(y - height / 2.0), width, height);
					return true;
				case TRIANGLE_UP:
					width = (int)(width * 1.2);
					height = (int)(height * 1.2);
					int ydown = y + (int)Math.round(Math.tan(Math.PI / 6) * Math.tan(Math.PI / 6) * height),
						yup = ydown - height;
					int xTU[] = new int[]{(int)(x - width / 2.0), x, (int)(x + width / 2.0)},
						yTU[] = new int[]{ydown, yup, ydown}; //(int)(y + height / 2.0), (int)(y - height / 2.0), (int)(y + height / 2.0)};
					polygon = new Polygon(xTU, yTU, xTU.length);
					g.fillPolygon(polygon);
					return true;
				case SQUARE:
					g.fillRect((int)(x - width / 2.0), (int)(y - height / 2.0), width, height);
					return true;
				case TRIANGLE_DOWN:
					width = (int)(width * 1.2);
					height = (int)(height * 1.2);
					int yu = y - (int)Math.round(Math.tan(Math.PI / 6) * Math.tan(Math.PI / 6) * height),
						yd = yu + height;
					int xTD[] = new int[]{(int)(x - width / 2.0), (int)(x + width / 2.0), x},
						yTD[] = new int[]{yu, yu, yd}; //(int)(y - height / 2.0), (int)(y - height / 2.0), (int)(y + height / 2.0)};
					polygon = new Polygon(xTD, yTD, xTD.length);
					g.fillPolygon(polygon);
					return true;
				case DIAMOND:
					width = (int)(width * 1.5);
					height = (int)(height * 1.5);
					int xD[] = new int[]{(int)(x - width / 2.0), x, (int)(x + width / 2.0), x},
						yD[] = new int[]{y, (int)(y - height / 2.0), y, (int)(y + height / 2.0)};
					polygon = new Polygon(xD, yD, xD.length);
					g.fillPolygon(polygon);
					return true;
				case X:
					width = (int)(width * 0.8);
					height = (int)(height * 0.8);
					g.drawLine((int)(x - width / 2.0), (int)(y - height / 2.0), (int)(x + width / 2.0), (int)(y + height / 2.0));
					g.drawLine((int)(x + width / 2.0), (int)(y - height / 2.0), (int)(x - width / 2.0), (int)(y + height / 2.0));
					return true;
				default:
					break;
			}
		}
		return false;
	}
	
	private int idxSymbol = 1;
	public void changeSymbol() {
		if (this.slave != null && this.slave.barsState == BarsState.DOTS_WITH_BARS) {
			this.symbol = symbols[idxSymbol];
			idxSymbol++;
			if (idxSymbol > symbols.length-1) {
				idxSymbol = 0;
			}
			//System.err.println("Nuovo simbolo: " + this.symbol);
		}
	}
	
	public void plot(Graphics2D g, Rectangle bounds, boolean stepShaped, int SCALA) {
		scale.computeScale(bounds);
		double scaleX = scale.getXScale(),
			   scaleY = scale.getYScale(),
			   minX = scale.getMinX(),
			   minY = scale.getMinY();
		if (!enabled) return;
		
		if (master != null) {
			myColor = master.myColor;
		} else {
			myColor = g.getColor();
			if (slave != null) {
				slave.myColor = myColor;
			}
			
		}
		if (isStdDev()) {
			P[] masterData = master.getData();
			P vecchio = null;
			int i = 0;
			Color c = g.getColor();
			if (barsState.equals(BarsState.ONLY_SHADING) || barsState.equals(BarsState.SHADING_WITH_BARS)) { //Draw standard deviation as shading
				for (P punto : data) {
					if (punto.y < 1e-7 || punto.x < minX) {
						vecchio = punto;
						continue;
					}
					for (;i<masterData.length && masterData[i].x<punto.x;i++);
					if (i < masterData.length) {
						if (i > 0) {
							float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
							Color c1 = Color.getHSBColor(hsb[0], hsb[1]/3, hsb[2]);
								  //c2 = Color.getHSBColor(hsb[0], hsb[1]*3/2, hsb[2]);
							float[] rgb = c1.getRGBComponents(null);
							Color c3 = new Color(rgb[0], rgb[1], rgb[2], 0.5f);
							rgb = c.getRGBColorComponents(null);
							Color c4 = new Color(rgb[0], rgb[1], rgb[2], 0.6f);
							/*g.setColor(Color.getHSBColor(hsb[0], hsb[1]/4, hsb[2]));
							g.drawLine((int)(bounds.x + scaleX * (masterData[i-1].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y - vecchio.y - minY)),
									   (int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y - punto.y - minY)));
							g.drawLine((int)(bounds.x + scaleX * (masterData[i-1].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y + vecchio.y - minY)),
									   (int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y + punto.y - minY)));*/
							/*Polygon grayedError = new Polygon();
							grayedError.addPoint((int)(bounds.x + scaleX * (masterData[i-1].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y - vecchio.y - minY)));
							grayedError.addPoint((int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y - punto.y - minY)));
							grayedError.addPoint((int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y + punto.y - minY)));
							grayedError.addPoint((int)(bounds.x + scaleX * (masterData[i-1].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y + vecchio.y - minY)));
							g.setPaint(new GradientPaint(new Point2D.Float((int)(bounds.x + scaleX * (masterData[i-1].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y - vecchio.y - minY))), c3, new Point2D.Float((int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y + punto.y - minY))), c4));
							g.fill(grayedError);*/
							/*Polygon error1 = new Polygon();
							error1.addPoint((int)(bounds.x + scaleX * (masterData[i-1].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y - minY)));
							error1.addPoint((int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y - minY)));
							error1.addPoint((int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y - punto.y - minY)));
							error1.addPoint((int)(bounds.x + scaleX * (masterData[i-1].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y - vecchio.y - minY)));
							g.drawLine((int)(bounds.x + scaleX * (masterData[i-1].x + masterData[i].x) / 2.0 - minX), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y + masterData[i].y) / 2.0 - minY), (int)(bounds.x + scaleX * (masterData[i-1].x + masterData[i].x) / 2.0 - minX), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y - vecchio.y + masterData[i].y - punto.y) / 2.0 - minY));
							g.setPaint(new GradientPaint((float)(bounds.x + scaleX * (masterData[i-1].x + masterData[i].x) / 2.0 - minX), (float)(bounds.y + bounds.height - scaleY * (masterData[i-1].y + masterData[i].y) / 2.0 - minY), c, (float)(bounds.x + scaleX * (masterData[i-1].x + masterData[i].x) / 2.0 - minX), (float)(bounds.y + bounds.height - scaleY * (masterData[i-1].y - vecchio.y + masterData[i].y - punto.y) / 2.0 - minY), c4));
							g.fill(error1);*/
							
							//I would like to make it simpler, so that it does not negatively influence performances, but I have no time..
							double maxY = Math.max(vecchio.y, punto.y);
							Point2D.Float A = new Point2D.Float((float)(bounds.x + scaleX * (masterData[i-1].x - minX)), (float)(bounds.y + bounds.height - scaleY * (masterData[i-1].y + maxY - minY))),
										  B = new Point2D.Float((float)(bounds.x + scaleX * (masterData[i].x - minX)), (float)(bounds.y + bounds.height - scaleY * (masterData[i].y + maxY - minY))),
										  C = new Point2D.Float((float)(bounds.x + scaleX * (masterData[i].x - minX)), (float)(bounds.y + bounds.height - scaleY * (masterData[i].y - maxY - minY))),
										  D = new Point2D.Float((float)(bounds.x + scaleX * (masterData[i-1].x - minX)), (float)(bounds.y + bounds.height - scaleY * (masterData[i-1].y - maxY - minY))),
										  E = new Point2D.Float((float)(bounds.x + scaleX * (masterData[i-1].x - minX)), (float)(bounds.y + bounds.height - scaleY * (masterData[i-1].y - minY))),
										  F = new Point2D.Float((float)(bounds.x + scaleX * (masterData[i].x - minX)), (float)(bounds.y + bounds.height - scaleY * (masterData[i].y - minY))),
										  I = new Point2D.Float((E.x + F.x) / 2.0f, (E.y + F.y) / 2.0f);
							float Gx, Gy;
							if (A.y != B.y) {
								Gx = (A.x*(B.y-A.y)/(B.x-A.x) - A.y + I.x*(B.x-A.x)/(B.y-A.y) + I.y) / ((B.y-A.y)/(B.x-A.x) + (B.x-A.x)/(B.y-A.y));
								Gy = (B.y-A.y)/(B.x-A.x) * (Gx-A.x) + A.y;
							} else {
								Gx = (A.x + B.x) / 2.0f;
								Gy = A.y;
							}
							Point2D.Float G = new Point2D.Float(Gx, Gy);
							float Hx, Hy;
							if (C.y != D.y) {
								Hx = (D.x*(C.y-D.y)/(C.x-D.x) - D.y + I.x*(C.x-D.x)/(C.y-D.y) + I.y) / ((C.y-D.y)/(C.x-D.x) + (C.x-D.x)/(C.y-D.y));
								Hy = (C.y-D.y)/(C.x-D.x) * (Gx-D.x) + D.y;
							} else {
								Hx = (C.x + D.x) / 2.0f;
								Hy = C.y;
							}
							Point2D.Float H = new Point2D.Float(Hx, Hy);
							Polygon error1 = new Polygon();
							error1.addPoint((int)E.x, (int)E.y);
							error1.addPoint((int)F.x, (int)F.y);
							error1.addPoint((int)B.x, (int)(bounds.y + bounds.height - scaleY * (masterData[i].y + punto.y - minY)));
							error1.addPoint((int)A.x, (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y + vecchio.y - minY)));
							//g.drawLine((int)I.x, (int)I.y, (int)G.x, (int)G.y);
							//g.drawLine((int)A.x, (int)A.y, (int)B.x, (int)B.y);
							g.setPaint(new GradientPaint(I, c4, G, c3));
							g.fill(error1);
							Polygon error2 = new Polygon();
							error2.addPoint((int)E.x, (int)E.y);
							error2.addPoint((int)F.x, (int)F.y);
							error2.addPoint((int)C.x, (int)(bounds.y + bounds.height - scaleY * (masterData[i].y - punto.y - minY)));
							error2.addPoint((int)D.x, (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y - vecchio.y - minY)));
							g.setPaint(new GradientPaint(I, c4, H, c3));
							g.fill(error2);
						}
					}
					vecchio = punto;
				}
			}
			g.setColor(c);
			i = 0;
			P lastBar = null;
			for (P punto : data) {
				if (punto.x < minX) {
					vecchio = punto;
					continue;
				}
				for (;i<masterData.length && masterData[i].x<punto.x;i++);
				if (i < masterData.length) {
					if (barsState.equals(BarsState.ONLY_BARS) || barsState.equals(BarsState.SHADING_WITH_BARS) || barsState.equals(BarsState.DOTS_WITH_BARS)) { //Draw standard deviation as error bars
						//these lines draw the vertical error bars, but if we have a lot of points the thing becomes extremely clumsy
						if (lastBar == null || scaleX * (punto.x - lastBar.x) > 10) {
							g.drawLine((int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y - punto.y - minY)), 
									(int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y  + punto.y - minY)));
							g.drawLine((int)(bounds.x + scaleX * (masterData[i].x - minX)) - 3 * SCALA, (int)(bounds.y + bounds.height - scaleY * (masterData[i].y - punto.y - minY)), 
									(int)(bounds.x + scaleX * (masterData[i].x - minX)) + 3 * SCALA, (int)(bounds.y + bounds.height - scaleY * (masterData[i].y  - punto.y - minY)));
							g.drawLine((int)(bounds.x + scaleX * (masterData[i].x - minX)) - 3 * SCALA, (int)(bounds.y + bounds.height - scaleY * (masterData[i].y + punto.y - minY)), 
									(int)(bounds.x + scaleX * (masterData[i].x - minX)) + 3 * SCALA, (int)(bounds.y + bounds.height - scaleY * (masterData[i].y  + punto.y - minY)));
							lastBar = punto;
						}
					}
					if (i > 0 && (barsState.equals(BarsState.ONLY_SHADING) || barsState.equals(BarsState.SHADING_WITH_BARS))) { //If there was shading, we need to redraw the "master" line, which can have been partially overdrawn
						g.drawLine((int)(bounds.x + scaleX * (masterData[i-1].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y - minY)),
								   (int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y - minY)));
					}
				}
				vecchio = punto;
			}
		} else {
			P vecchio = data[0];
			for (int j=1;j<data.length;j++) {
				P punto = data[j];
				if (punto.x < minX) {
					vecchio = punto;
					continue;
				}
				if (slave != null && slave.barsState == BarsState.DOTS_WITH_BARS) {
					int w, h;
					w = h = 4 * (int)(((BasicStroke)g.getStroke()).getLineWidth());
					drawSymbol(g, (int)(bounds.x + scaleX * (punto.x - minX)), (int)(bounds.y + bounds.height - scaleY * (punto.y - minY)), w, h);
				} else {
					if (stepShaped) {
						g.drawLine((int)(bounds.x + scaleX * (vecchio.x - minX)), (int)(bounds.y + bounds.height - scaleY * (vecchio.y - minY)),
								(int)(bounds.x + scaleX * (punto.x - minX)), (int)(bounds.y + bounds.height - scaleY * (vecchio.y - minY)));
						g.drawLine((int)(bounds.x + scaleX * (punto.x - minX)), (int)(bounds.y + bounds.height - scaleY * (vecchio.y - minY)),
								(int)(bounds.x + scaleX * (punto.x - minX)), (int)(bounds.y + bounds.height - scaleY * (punto.y - minY)));
					} else {
						g.drawLine((int)(bounds.x + scaleX * (vecchio.x - minX)), (int)(bounds.y + bounds.height - scaleY * (vecchio.y - minY)), 
								(int)(bounds.x + scaleX * (punto.x - minX)), (int)(bounds.y + bounds.height - scaleY * (punto.y - minY)));
					}
				}
				vecchio = punto;
			}
		}
	}
}