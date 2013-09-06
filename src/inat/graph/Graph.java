package inat.graph;

import inat.InatBackend;
import inat.analyser.LevelResult;
import inat.util.HeatChart;
import inat.util.XmlConfiguration;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import cytoscape.Cytoscape;

public class Graph extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, ActionListener, ComponentListener {
	private static final long serialVersionUID = 8185951065715897260L;
	public static final String AUTOGRAPH_WINDOW_TITLE = "AutoGraph", //If we are a window, this will be our (lame) title
								OPEN_LABEL = "Add data from CSV...",
								SAVE_LABEL = "Save as PNG...",
								RENDER_TO_JPG_LABEL = "Render to JPG...",
								EXPORT_VISIBLE_LABEL = "Export visible as CSV...",
								CLEAR_LABEL = "Clear Data",
								INTERVAL_LABEL = "Graph interval...",
								ZOOM_RECTANGLE_LABEL = "Zoom rectangle",
								ZOOM_EXTENTS_LABEL = "Zoom extents",
								CLOSE_LABEL = "Close",
								SHOW_SIZE_LABEL = "Show picture size",
								SHOW_ZOOM_LEVEL_LABEL = "Show zoom level",
								SHOW_THIN_AXES = "Show thin axes",
								SHOW_THICK_AXES = "Show thick axes",
								SET_Y_LABEL_LABEL = "Set Y label",
								STEP_SHAPED_GRAPH_LABEL = "Step-shaped graph",
								HEATMAP_GRAPH_LABEL = "Heat-map graph",
								CSV_FILE_EXTENSION = ".csv",
								CSV_FILE_DESCRIPTION = "CSV file",
								DEFAULT_CSV_FILE = "/local/schivos/Data_0-240_TNF100.csv", //"/local/schivos/aData1_0-1440_normalized_MK2_JNK1_IKK_with_stddev.csv", //"/local/schivos/aData1_0-1440_times5_normalized_better_onlyMK2_JNK1_IKK_con_stddev.csv",
								CSV_IO_PROBLEM = "Problem reading the CSV file!",
								GENERIC_ERROR_S = "There has been a problem: ",
																//You can "declare" the maximum y value that you intend to represent with a particular series 
								MAX_Y_STRING = "Number_of_levels"; //The idea is to take into account these values in order to rescale the y maximum value for all shown graphs when requested
	private Double maxYValue = null; //The maximum Y value: it is used to rescale other bunches of series (if they themselves declare their maximum Y value). In a .csv file, the user needs to have a column titled "Number_of_levels", and as value (in the first line) the max Y the user wants to declare for that .csv bunch of series
	private static final java.awt.Color BACKGROUND_COLOR = Color.WHITE, FOREGROUND_COLOR = Color.BLACK, DISABLED_COLOR = Color.LIGHT_GRAY; //The colors for the background, the axis and the (possibly disabled) series names
	private static final java.awt.Color RED_LINE_COLOR = Color.RED.darker(); //The color for the red line
	private boolean needRedraw = true; //If true: redraw all the graph. If false: use the saved image graph and redraw only legend
	private	BufferedImage bufferedImage = null; //The image where we save the graph once drawn
	
	private Vector<Series> data = null; //the Series plotted in the graph
	private Vector<String> selectedColumns = null; //the names of the Series to be shown (all others are hidden)
	private Scale scale = null; //contains scale factors
	private Vector<GraphScaleListener> scaleListeners = new Vector<GraphScaleListener>(); //The vector of all graph scale listeners: when our scale changes, we call the scaleChanged() method on all the listeners in this Vector
	private String xSeriesName = null, //the name for the X axis
				   yLabel = null;
	private JPopupMenu popupMenu = null;
	private boolean showLegend = true;
	private boolean showSize = false;
	private boolean showZoomLevel = false;
	private boolean showThinAxes = false;
	private boolean showThickAxes = false;
	private double maxLabelLength = 0; //used to compute the width of the legend box
	private Rectangle legendBounds = null; //Where the legend is, and what are its dimensions
	private boolean customLegendPosition = false;
	private boolean movingLegend = false; //If the user is currently dragging the legend (left mouse button down)
	private boolean drawingZoomRectangle = false; //If the user is using the rectangle-based zoom, and is now drawing the rectangle, this is true
	private Rectangle zoomExtentsBounds = null; //We put here the bounds we find when performing the zoom rectangle. This way, when the user asks for "zoom back" we show them these bounds
	private Rectangle zoomRectangleBounds = null; //The bounds inside which to draw the zoom rectangle when showing it to the user
	private double xRedLine = 0.0; //The vertical red line we paint to indicate a specific X coordinate in the graph. It can be called from the external user (for example, the slider in ANIMO)
	private boolean stepShapedLines = false; //Whether to show the series as a step-shaped graphs, or in a more "smooth" way. Default is smooth.
	private boolean useHeatMap = false; //Whether to show the graph as a "heat map", using HeatChart class by Tom Castle
	private JCheckBoxMenuItem heatMapGraph;
	private int oldLegendX = 0, oldLegendY = 0; //Used to move the legend
	private int SCALA = 1; //used to implement some kind of "zooming" (see the events related to mouse wheel)
	private final int BORDER_X = 25, BORDER_Y = 25; //width of the border around the graph area (in pixel). Notice that it is scaled with SCALA, like all other constants for the drawing
	
	public Graph() {
		XmlConfiguration configuration = InatBackend.get().configuration();
		String areWeTheDeveloperStr = configuration.get(XmlConfiguration.DEVELOPER_KEY);
		boolean areWeTheDeveloper = false;
		if (areWeTheDeveloperStr != null) {
			areWeTheDeveloper = Boolean.parseBoolean(areWeTheDeveloperStr);
		}
		data = new Vector<Series>();
		selectedColumns = new Vector<String>();
		scale = new Scale();
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addComponentListener(this);
		this.addMouseWheelListener(this);
		popupMenu = new JPopupMenu();
		JMenuItem open = new JMenuItem(OPEN_LABEL);
		JMenuItem save = new JMenuItem(SAVE_LABEL);
		JMenuItem renderJpg = new JMenuItem(RENDER_TO_JPG_LABEL);
		JMenuItem export = new JMenuItem(EXPORT_VISIBLE_LABEL);
		JMenuItem clear = new JMenuItem(CLEAR_LABEL);
		JMenuItem newInterval = new JMenuItem(INTERVAL_LABEL);
		JMenuItem zoomRectangle = new JMenuItem(ZOOM_RECTANGLE_LABEL);
		JMenuItem zoomExtents = new JMenuItem(ZOOM_EXTENTS_LABEL);
		JMenuItem close = new JMenuItem(CLOSE_LABEL);
		JCheckBoxMenuItem showSizeM = new JCheckBoxMenuItem(SHOW_SIZE_LABEL);
		JCheckBoxMenuItem showZoomM = new JCheckBoxMenuItem(SHOW_ZOOM_LEVEL_LABEL);
		JMenuItem setYLabel = new JMenuItem(SET_Y_LABEL_LABEL);
		JCheckBoxMenuItem stepShapedGraph = new JCheckBoxMenuItem(STEP_SHAPED_GRAPH_LABEL);
		heatMapGraph = new JCheckBoxMenuItem(HEATMAP_GRAPH_LABEL);
		JCheckBoxMenuItem showThinAxes = new JCheckBoxMenuItem(SHOW_THIN_AXES);
		JCheckBoxMenuItem showThickAxes = new JCheckBoxMenuItem(SHOW_THICK_AXES);
		open.addActionListener(this);
		save.addActionListener(this);
		renderJpg.addActionListener(this);
		export.addActionListener(this);
		clear.addActionListener(this);
		newInterval.addActionListener(this);
		zoomRectangle.addActionListener(this);
		zoomExtents.addActionListener(this);
		close.addActionListener(this);
		showSizeM.addActionListener(this);
		showZoomM.addActionListener(this);
		setYLabel.addActionListener(this);
		showThinAxes.addActionListener(this);
		showThickAxes.addActionListener(this);
		stepShapedGraph.addActionListener(this);
		heatMapGraph.addActionListener(this);
		popupMenu.add(open);
		popupMenu.add(save);
		if (areWeTheDeveloper) {
			popupMenu.add(renderJpg);
		}
		popupMenu.add(export);
		popupMenu.add(clear);
		popupMenu.add(newInterval);
		popupMenu.add(zoomRectangle);
		popupMenu.add(zoomExtents);
		if (areWeTheDeveloper) {
			popupMenu.add(setYLabel);
			popupMenu.add(showSizeM);
			popupMenu.add(showZoomM);
			popupMenu.add(showThinAxes);
			popupMenu.add(showThickAxes);
		}
		popupMenu.add(stepShapedGraph);
		popupMenu.add(heatMapGraph);
		//popupMenu.addSeparator();
		//popupMenu.add(close);
		this.add(popupMenu);
		customLegendPosition = false;
		movingLegend = false;
		legendBounds = null;
		drawingZoomRectangle = false;
		zoomRectangleBounds = null;
	}
	
	/*
	 * Clear the graph. Reset the scale in order to plot another different graph, and remove all data
	 */
	public void reset() {
		data = new Vector<Series>();
		scale.reset();
		setXSeriesName(null);
		setYLabel(null);
	}
	
	/*
	 * This is used to let the graph know that any other graph group/set (such as a csv file)
	 * added to this same graph and declaring itself a maxYValue will have all its y values
	 * rescaled with factor maxYValue/hisMaxYValue.
	 */
	public void declareMaxYValue(double maxY) { 
		this.maxYValue = maxY;
	}
	
	/*
	 * Add a new Series with title of the kind Series 0, Series 1, ...
	 */
	private void addSeries(P[] series) {
		data.add(new Series(series));
	}
	
	/*
	 * Add a new series with given title
	 */
	private void addSeries(P[] series, String name) {
		if (name == null) {
			addSeries(series);
		} else {
			data.add(new Series(series, scale, name));
		}
	}
	
	/*
	 * Switch the shown/hidden status of the Series at index seriesIdx
	 */
	public void changeEnabledSeries(int seriesIdx) {
		if (seriesIdx < 0 || seriesIdx >= data.size()) return;
		data.elementAt(seriesIdx).setEnabled(!data.elementAt(seriesIdx).getEnabled());
	}
	
	/**
	 * Switch the shown/hidden status of all series
	 * @param seriesNames
	 */
	public void changeAllEnabledSeries() {
		for (Series s : data) {
			s.setEnabled(!s.getEnabled());
		}
		for (Series s : data) {
			if (s.isSlave()) {
				s.setEnabled(s.getMaster().getEnabled());
			}
		}
	}
	
	/*
	 * All the Series whose names are in the given are shown, all the others are hidden.
	 * An alternative to this is to directly pass the string vector to the parseCSV method
	 */
	public void setEnabledSeries(Vector<String> seriesNames) {
		for (String seriesName : seriesNames) {
			for (Series series : data) {
				if (series.getName().contains(seriesName)) {
					series.setEnabled(true);
				} else {
					series.setEnabled(false);
				}
			}
		}
	}
	
	/*
	 * All the Series whose names are in the given String vector are added to the list of visible Series.
	 * Any Series whose name is not in the list of visible Series is hidden
	 */
	public void addEnabledSeries(Vector<String> selectedColumns) {
		this.selectedColumns.addAll(selectedColumns);
		for (Series s : data) {
			if (!this.selectedColumns.contains(s.getName())) {
				s.setEnabled(false);
			} else {
				s.setEnabled(true);
			}
		}
	}
	
	/*
	 * Get the list of visible Series
	 */
	public Vector<String> getEnabledSeries() {
		Vector<String> enabledSeries = new Vector<String>();
		for (Series series : data) {
			if (series.getEnabled()) {
				enabledSeries.add(series.getName());
			}
		}
		return enabledSeries;
	}
	
	public Vector<String> getSeriesNames() {
		Vector<String> seriesNames = new Vector<String>();
		for (Series series : data) {
			seriesNames.add(series.getName());
		}
		return seriesNames;
	}
	
	/*
	 * Marks the Series at index seriesIdx to be changed of color next time we repaint
	 */
	public void changeSeriesColor(int seriesIdx) {
		if (seriesIdx < 0 || seriesIdx >= data.size()) return;
		data.elementAt(seriesIdx).setChangeColor(true);
	}
	
	/*
	 * If the series at index seriesIdx has a slave, toggle explicit error bars (shown by the slave)
	 */
	public void changeErrorBars(int seriesIdx) {
		if (seriesIdx < 0 || seriesIdx >= data.size()) return;
		Series s = data.elementAt(seriesIdx);
		if (s.isMaster()) {
			s = s.getSlave();
			s.changeErrorBars();
		}
	}
	
	/*
	 * Tell the series to change its symbol (will have effect only if the series has a slave and it displays std deviation as error bars + symbol)
	 */
	public void changeSeriesSymbol(int seriesIdx) {
		if (seriesIdx < 0 || seriesIdx >= data.size()) return;
		Series s = data.elementAt(seriesIdx);
		if (s.isMaster()) {
			s.changeSymbol();
		}
	}
	
	/*
	 * The small title for the X axis (most of the times, it is something like "Time")
	 */
	public void setXSeriesName(String name) {
		this.xSeriesName = name;
	}
	
	public String getXSeriesName() {
		return this.xSeriesName;
	}
	
	public void setYLabel(String name) {
		this.yLabel = name;
	}
	
	public String getYLabel() {
		return this.yLabel;
	}
	
	/*
	 * Make sure that the next time the graph draws, it actually draws everything from scratch
	 */
	public void ensureRedraw() {
		needRedraw = true;
	}
	
	/**
	 * Returns the level of zoom (minimum is 1) of the graph.
	 * Its value determines the thickness of lines and font sizes
	 * and is normally useful when the image gets large.
	 * @return
	 */
	public int getZoomLevel() {
		return SCALA;
	}
	
	public void setZoomLevel(int newSCALA) {
		this.SCALA = newSCALA;
	}
	
	/*
	 * The available colors for the Series.
	 * As we see also with the other following functions, we normally cycle through
	 * colors when drawing a bunch of series. If the user has set a particular color for
	 * a Series, the Series will remember it and we will use that color instead
	 */
	private Color colori[] = {/*Color.RED, Color.BLUE, Color.GREEN, 
							   Color.ORANGE, Color.CYAN, Color.GRAY, 
							   Color.MAGENTA, Color.YELLOW, Color.PINK*/
								/*Color.RED, Color.BLUE, Color.GREEN.darker(),
								Color.ORANGE, Color.CYAN.darker(), new Color(184, 46, 46),
								Color.GREEN, Color.ORANGE.darker()*/
								new Color(166, 206, 227), new Color(31, 120, 180), new Color(178, 223, 138),
								new Color(51, 160, 44), new Color(251, 154, 153), new Color(227, 26, 28),
								new Color(253, 191, 111), new Color(255, 127, 0), new Color(202, 178, 214)
								/*new Color(141, 211, 199), new Color(255, 255, 179), new Color(190, 186, 218),
								new Color(251, 128, 114), new Color(128, 177, 211), new Color(253, 180, 98),
								new Color(179, 222, 105), new Color(252, 205, 229), new Color(217, 217, 217),
								new Color(188, 128, 189), new Color(204, 235, 197), new Color(255, 237, 111)*/
							};
	int idxColore = 1;
	Random random = new Random();
	
	private void resetCol() {
		idxColore = 1;
	}
	
	private Color nextCol() {
		Color c = colori[idxColore];
		idxColore++;
		if (idxColore > colori.length-1) {
			idxColore = 0;
		}
		return c;
	}
	
	int idxRandom = 0;
	private Color randomCol() {
		//return colori[0 + random.nextInt(colori.length-1 - 0)];
		Color c = colori[idxRandom];
		idxRandom++;
		if (idxRandom > colori.length-1) {
			idxRandom = 0;
		}
		return c;
	}
	
	/*
	 * Given a point of coordinates (x, y) inside the legend rectangle, find the index of the Series whose name is in the line containing (x, y)
	 */
	public int findSeriesInLegend(int x, int y) {
		if (x >= 5 * SCALA && x <= legendBounds.width - 5 * SCALA
			&& y >= 5 * SCALA && y <= legendBounds.height - 5 * SCALA) {
			int seriesIdx = y / (20 * SCALA);
			int countShownSeries = 0,
				countExistingSeries = 0;
			while (countExistingSeries<data.size()) {
				if (data.elementAt(countExistingSeries).isSlave()) {
					countShownSeries--;
				}
				if (countShownSeries == seriesIdx) break;
				countExistingSeries++;
				countShownSeries++;
			}
			return countExistingSeries;
		} else {
			return -1;
		}
	}
	
	/*
	 * Draw axes, arrow points, ticks and label X axis with the label found in the first column of the CSV datafile
	 */
	public void drawAxes(Graphics2D g, Rectangle bounds, Rectangle pictureBounds) {
		FontMetrics fm = g.getFontMetrics();
		int leftBorder = fm.stringWidth("" + (int)scale.getMaxY());
		if (yLabel != null) {
			leftBorder = leftBorder + fm.getHeight(); //Math.max(leftBorder, fm.getHeight());
		}
		
		Stroke oldStroke = g.getStroke();
		if (showThickAxes) {
			g.setStroke(new BasicStroke(2.0f * SCALA));
		} else if (showThinAxes) {
			g.setStroke(new BasicStroke(0.5f * SCALA));
		}
		
		g.setPaint(BACKGROUND_COLOR);
		g.fillRect(0, 0, bounds.x + leftBorder, (int)pictureBounds.getHeight());
		g.fillRect(0, bounds.height + bounds.y, (int)pictureBounds.getWidth(), (int)pictureBounds.getHeight()-bounds.height-bounds.y);
		g.fillRect(0, 0, (int)pictureBounds.getWidth(), bounds.y - 10 * SCALA);
		g.fillRect(bounds.x + bounds.width, 0, (int)pictureBounds.getWidth()-bounds.x-bounds.width, (int)pictureBounds.getHeight());
		
		g.setPaint(FOREGROUND_COLOR);
		
		int rightBorder = 0;
		g.drawLine(bounds.x + leftBorder, bounds.height + bounds.y, bounds.x + bounds.width - rightBorder, bounds.height + bounds.y);
		g.drawLine(bounds.x + leftBorder, bounds.height + bounds.y, bounds.x + leftBorder, bounds.y - 10 * SCALA);
		
		if (fm.getHeight() < bounds.height) { //If we can't fit the text, we don't try to put it anywhere
			int xTick = bounds.x + leftBorder,
			yTick = bounds.y + bounds.height;
			double minX = scale.getMinX(),
				   maxX = scale.getMaxX(),
				   scaleX = scale.getXScale();
			int interval = (int)(maxX - minX + 1);
			int increase = 1;
			//awful heuristic in order to get some ticks 
			while (interval > 0) {
				interval = interval / 10;
				increase = increase * 10;
			}
			while ((maxX - minX + 1) / increase < 8) {
				increase = increase / 10;
			}
			if (increase < 1) increase = 1;
			//questa condizione dice: se le due etichette pi� lunghe si sovrappongono perch� sono troppo vicine..
			while (increase * scaleX < 5 * SCALA + fm.stringWidth(new Integer((int)maxX).toString())) {
			//if ((maxX - minX + 1) / increase > 20) { //questa invece si limitava a vedere se venivano troppe (in assoluto) tick: ma non sappiamo quanto � largo il grafico!
				increase = increase * 2; //Questo continua a raddoppiare perche' la scala e' troppo piccola e non raggiungi mai il numero grande!
			}
			int xStartString = bounds.x + bounds.width - rightBorder;
			if (xSeriesName != null) {
				xStartString -= fm.stringWidth(xSeriesName) - 5 * SCALA;
			}
			for (int i=increase; i<maxX && (xTick + increase * scaleX + fm.stringWidth(new Integer(i).toString())) < xStartString; i+=increase) {
				xTick = (int) (bounds.x + leftBorder + scaleX * (i - minX));
				if (xTick < bounds.x + leftBorder) continue;
				if (xTick > bounds.x + bounds.width) break;
				g.drawLine(xTick, yTick - 5 * SCALA, xTick, yTick + 5 * SCALA);
				String label = new Integer(i).toString();
				g.drawString(label, xTick - fm.stringWidth(label)/2, yTick + 3 * SCALA + fm.getHeight());
			}
			
			xTick = bounds.x + leftBorder;
			yTick = bounds.y + bounds.height;
			double minY = scale.getMinY(),
				   maxY = scale.getMaxY(),
				   scaleY = scale.getYScale();
			interval = (int)(maxY - minY + 1);
			increase = 1;
			while (interval > 0) {
				interval = interval / 10;
				increase = increase * 10;
			}
			while ((maxY - minY + 1) / increase < 8) {
				increase = increase / 10;
			}
			if (increase < 1) increase = 1;
			while (increase * scaleY < fm.getHeight()) { //Qui succede qualcosa di grave
				increase = increase * 2;
			}
			for (int i=increase; i < maxY; i+=increase) {
				yTick = (int)(bounds.y + bounds.height - scaleY * (i - minY));
				if (yTick > bounds.y + bounds.height) continue;
				if (yTick < bounds.y) break;
				g.drawLine(xTick - 5 * SCALA, yTick, xTick + 5 * SCALA, yTick);
				String label = new Integer(i).toString();
				g.drawString(label, xTick - fm.stringWidth(label) - 5 * SCALA, yTick - 3 * SCALA + fm.getHeight()/2);
			}
			if (increase > minY) {
				for (int i=increase; i > minY; i-=increase) {
					yTick = (int)(bounds.y + bounds.height - scaleY * (i - minY));
					if (yTick > bounds.y + bounds.height) continue;
					if (yTick < bounds.y) break;
					g.drawLine(xTick - 5 * SCALA, yTick, xTick + 5 * SCALA, yTick);
					String label = new Integer(i).toString();
					g.drawString(label, xTick - fm.stringWidth(label) - 5 * SCALA, yTick - 3 * SCALA + fm.getHeight()/2);
				}
			}
			
			
	
			if  (showThinAxes || showThickAxes) {
				g.setStroke(oldStroke);
			}
			
			if (xSeriesName != null) {
				g.drawString(xSeriesName, xStartString, bounds.y + bounds.height + 3 * SCALA + fm.getHeight());
			}
			
			if (yLabel != null) {
				String label = yLabel;
				AffineTransform at = new AffineTransform(),
								at2 = new AffineTransform();
				at.setToTranslation(bounds.x, bounds.y + bounds.height / 2.0 + fm.stringWidth(label) / 2.0);
				at2.setToRotation(-Math.PI / 2.0);
				at.concatenate(at2);
				g.setTransform(at);
				//g.drawString(label, bounds.x + leftBorder - fm.stringWidth(label) - 6 * SCALA, bounds.y - 5 * SCALA + fm.getHeight()/2);
				g.drawString(label, 0, 0);
				g.setTransform(new AffineTransform());
			}
			
		}
		
	}
	
	
	public void drawLegend(Graphics2D g, Rectangle bounds) {
		//g.clearRect(bounds.x, bounds.y, bounds.width, bounds.height);
		g.setPaint(BACKGROUND_COLOR);
		g.fill(bounds);
		Stroke oldStroke = g.getStroke();
		if (showThickAxes) {
			g.setStroke(new BasicStroke(2.0f * SCALA));
		}
		g.setPaint(FOREGROUND_COLOR);
		g.draw(bounds);
		resetCol();
		g.setStroke(new BasicStroke(3 * SCALA));
		int nLegend = 0;
		for (Series series : data) {
			if (series.isSlave()) continue;
			g.setPaint(series.getColor());
			if (!series.drawSymbol(g, bounds.x + 15 * SCALA, bounds.y + 10 * SCALA + nLegend * 20 * SCALA, 10 * SCALA, 10 * SCALA)) { //If the series has no symbol, then we draw a line
				g.drawLine(bounds.x + 5 * SCALA, bounds.y + 10 * SCALA + nLegend * 20 * SCALA, bounds.x + 25 * SCALA, bounds.y + 10 * SCALA + nLegend * 20 * SCALA);
			}
			if (series.getEnabled()) {
				g.setPaint(FOREGROUND_COLOR);
			} else {
				g.setPaint(DISABLED_COLOR);
			}
			g.drawString(series.getName(), bounds.x + 30 * SCALA, bounds.y + 15 * SCALA + nLegend * 20 * SCALA);
			nLegend++;
		}
		g.setStroke(oldStroke);
	}
	
	public void drawZoomRectangle(Graphics2D g) {
		Stroke oldStroke = g.getStroke();
		float dash[] = { 10.0f * SCALA };
		g.setStroke(new BasicStroke(2.0f * SCALA, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f * SCALA, dash, 0.0f));
		g.draw(zoomRectangleBounds);
		g.setStroke(oldStroke);
	}
	
	public void paint(Graphics g) {
		paint(g, this.getWidth(), this.getHeight());
	}
	
	private Image heatMapImg = null;
	private HeatChart heatMapChart = null;
	private Rectangle heatMapBounds = null;
	public void paint(Graphics g1, int areaWidth, int areaHeight) {
		try {
			Graphics2D g = (Graphics2D)g1;
			if (useHeatMap) { //TODO: will not work when we consider overlay graphs (will show nothing at all).
				if (heatMapImg == null || needRedraw) {
					try {
						int countSeries = 0;
						for (Series s : data) {
							if (!s.isSlave()) countSeries++;
						}
						double[][] graphValues = new double[countSeries][];
						String[] seriesNames = new String[countSeries];
						int nPoints = 200; //TODO: Totally arbitrary!
						String[] timePoints = new String[nPoints];
						int i = 0, j = 0;
						double x, xIncr;
						x = this.scale.getMinX();
						xIncr = (this.scale.getMaxX() - this.scale.getMinX()) / nPoints;
						DecimalFormat f = new DecimalFormat("#.####");
						while (x < this.scale.getMaxX() && j < nPoints) {
							timePoints[j++] = " " + f.format(x);
							x += xIncr;
						}
						x = this.scale.getMinX();
						for (Series s : data) {
							if (s.isSlave()) continue;
							seriesNames[i] = s.getName() + " ";
							P[] points = s.getData();
							graphValues[i] = new double[nPoints];
							int k = 0;
							double lastY = points[k].y;
							x = this.scale.getMinX();
							j = 0;
							while (x < this.scale.getMaxX() && j < graphValues[i].length) {
								if (points[0].x <= x) { //We make sure that the x we consider is inside the interval where this series has values
									while (k < points.length && points[k].x < x) {
										lastY = points[k].y;
										k++;
									}
									graphValues[i][j++] = lastY;
								}
								x += xIncr;
							}
							i++;
						}
						heatMapChart = new HeatChart(graphValues);
						heatMapChart.setLowValue(this.scale.getMinY());
						heatMapChart.setHighValue(this.scale.getMaxY());
						heatMapChart.setYValues(seriesNames);
						heatMapChart.setXValues(timePoints);
						if (this.yLabel != null) {
							heatMapChart.setYAxisLabel(this.yLabel);
						}
						if (this.xSeriesName != null) {
							heatMapChart.setXAxisLabel(this.xSeriesName);
						}
						heatMapChart.setXAxisValuesFrequency(timePoints.length / 20);
						heatMapChart.setXValuesHorizontal(true);
						heatMapChart.setAxisLabelsFont(new Font("Sans-Serif", Font.PLAIN, 12 * SCALA));
						heatMapChart.setAxisValuesFont(new Font("Sans-Serif", Font.PLAIN, 10 * SCALA));
						heatMapChart.setChartSize(new Dimension(areaWidth, areaHeight));
						heatMapImg = heatMapChart.getChartImage();
						heatMapBounds = heatMapChart.getChartBounds();
					} catch (Exception ex) {
						System.err.println(ex);
						ex.printStackTrace(System.err);
					}
				}
				g.drawImage(heatMapImg, 0, 0, null);
				if (xRedLine > 0) { //Esattamente i minuti in tempo umano, ovvero il numero che leggo nell'asse x
					g.setPaint(Color.BLACK);
					Stroke olStroke = g.getStroke();
					g.setStroke(new BasicStroke(3 * SCALA));
					Rectangle bounds = heatMapBounds;
					double scaleLine = bounds.width / this.scale.getMaxX();
					g.drawLine((int)Math.round(bounds.x + xRedLine * scaleLine), bounds.height + bounds.y, (int)Math.round(bounds.x + xRedLine * scaleLine), bounds.y - 10 * SCALA);
					g.setStroke(olStroke);
				}
				return;
			}
			Graphics2D gBackground;
			if (needRedraw) {
				bufferedImage = new BufferedImage(areaWidth, areaHeight, BufferedImage.TYPE_INT_RGB);
				gBackground = bufferedImage.createGraphics();
			} else {
				gBackground = (Graphics2D)g1;
			}
			Font oldFont = g.getFont();
			int fontSize = oldFont.getSize() * SCALA;
			if (showThickAxes) {
				fontSize = (oldFont.getSize() + 4) * SCALA;
			}
			Font newFont = new Font(oldFont.getName(), oldFont.getStyle(), fontSize);
			g.setFont(newFont);
			gBackground.setFont(newFont);
			
			//if (!movingLegend) {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				gBackground.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			//}
			
			//g.clearRect(0, 0, this.getWidth(), this.getHeight());
			gBackground.setPaint(BACKGROUND_COLOR);
			Rectangle bounds = new Rectangle(0, 0, areaWidth, areaHeight);
			bounds.x = bounds.y = 0; //we don't care where we are inside our containing object: we only need the width and height of the drawing area. The starting x and y are of course 0.
			if (needRedraw) {
				gBackground.fill(bounds);
			}
			
			FontMetrics fm = g.getFontMetrics();
			maxLabelLength = 0;
			bounds.setBounds(bounds.x + BORDER_X * SCALA, bounds.y + BORDER_Y * SCALA, bounds.width - 2 * BORDER_X * SCALA, bounds.height - 2 * BORDER_Y * SCALA);
			int leftBorder = fm.stringWidth("" + (int)scale.getMaxY());
			if (yLabel != null) {
				leftBorder = leftBorder + fm.getHeight();
			}
			int rightBorder = 0;
			bounds.setBounds(bounds.x + leftBorder, bounds.y, bounds.width - (leftBorder + rightBorder), bounds.height);
			
			resetCol();
			Stroke oldStroke = gBackground.getStroke();
			gBackground.setStroke(new BasicStroke(2 * SCALA));
			Stroke fineStroke = new BasicStroke(1 * SCALA);
			for (Series series : data) {
				if (series.isSlave()) continue; //first plot all masters, then all slaves: this way we are sure that the master has set all it needs and the slave can lazily copy the same settings
				
				if (needRedraw) {
					if (series.getColor() == null || series.getChangeColor()) {
						if (!series.getChangeColor()) {
							gBackground.setPaint(nextCol());
						} else {
							gBackground.setPaint(randomCol());
							series.setChangeColor(false);
						}
					} else {
						gBackground.setPaint(series.getColor());
					}
					series.plot(gBackground, bounds, stepShapedLines, SCALA);
					if (series.isMaster()) {
						series.getSlave().plot(gBackground, bounds, stepShapedLines, SCALA);
					}
				}
				
				double labelLength = fm.stringWidth(series.getName());
				if (labelLength > maxLabelLength) {
					maxLabelLength = labelLength;
				}
			}
			
			g.setStroke(fineStroke);
			gBackground.setStroke(fineStroke);
			
			if (needRedraw) {
				bounds.setBounds(bounds.x - leftBorder, bounds.y, bounds.width + (leftBorder + rightBorder), bounds.height);
				drawAxes(gBackground, bounds, new Rectangle(0, 0, areaWidth, areaHeight));
				bounds.setBounds(bounds.x + leftBorder, bounds.y, bounds.width - (leftBorder + rightBorder), bounds.height);
			}
			
			if (needRedraw) {
				needRedraw = false;
			}
			
			//g.drawImage(bufferedImage, 0, 0, this);
			g.drawImage(bufferedImage, 0, 0, areaWidth, areaHeight, null);
			
			if (xRedLine > 0) {
				g.setPaint(RED_LINE_COLOR);
				Stroke olStroke = g.getStroke();
				g.setStroke(new BasicStroke(3 * SCALA));
				g.drawLine((int)Math.round(bounds.x + xRedLine * scale.getXScale()), bounds.height + bounds.y, (int)Math.round(bounds.x + xRedLine * scale.getXScale()), bounds.y - 10 * SCALA);
				g.setStroke(olStroke);
			}
			
			if (legendBounds == null || !customLegendPosition || this.getWidth() != areaWidth || this.getHeight() != areaHeight) {
				int nGraphs = 0;
				for (Series s : data) {
					if (!s.isSlave()) nGraphs++;
				}
				legendBounds = new Rectangle(bounds.width - 1 * SCALA - (int)maxLabelLength, bounds.y + 1 * SCALA, 35 * SCALA + (int)maxLabelLength, 20 * SCALA * nGraphs);
			}
			if (showLegend) {
				drawLegend(g, legendBounds);
			}
			
			if (drawingZoomRectangle) {
				drawZoomRectangle(g);
			}
	
			if (showSize) {
				g.drawString("(" + areaWidth + ", " + areaHeight + ")", 0, fm.getMaxAscent());
			}
			if (showZoomLevel) {
				g.drawString("Zoom: " + SCALA, 0, 2 + 2 * fm.getMaxAscent());
			}
			g.setStroke(oldStroke);
			g.setFont(oldFont);
			
		} catch (Exception ex) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			ex.printStackTrace(ps);
			ps.flush();
			String labelContent = "<html>" + baos.toString().replaceAll("\\n", "<br/>") + "</html>";
			final JLabel label = new JLabel(labelContent);
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(Cytoscape.getDesktop(), label, "Error", JOptionPane.ERROR_MESSAGE);
				}
			});
		}
	}
	
	
	/*
	 * Add a new set of Series from a given LevelResult, marking all as shown
	 */
	public void parseLevelResult(LevelResult result, Map<String, String> seriesNameMapping, double xScale) {
		parseLevelResult(result, seriesNameMapping, xScale, null);
	}
	
	/*
	 * Add a new set of Series from a given LevelResult, marking the given ones as shown
	 */
	public void parseLevelResult(LevelResult result, Map<String, String> seriesNameMapping, double xScale, Vector<String> selectedColumns) {
		boolean mustRescaleYValues = false; //if we find a column whose name is equal to my constant MAX_Y_STRING, two things can happen:
											//1. maxYValue == null, then we update its value with the (only) value present in this special column
											//2. maxYValue != null, then we rescale the y values of all the series we find in this csv file to the value of maxYValue,
											//   using maxYValue/valueInTheSpecialColumn as scale factor.
											//The mustRescaleYValues is used to signal the fact that we are in the second case
		if (selectedColumns != null) {
			this.selectedColumns.addAll(selectedColumns);
		}
		String[] graphNames = result.getReactantIds().toArray(new String[] {""});
		int nColonne = graphNames.length;
		if (nColonne <= 10) {
			useHeatMap = false;
			heatMapGraph.setSelected(false);
		} else {
			useHeatMap = true;
			heatMapGraph.setSelected(true);
		}
		Vector<Vector<P>> grafici = new Vector<Vector<P>>(nColonne);
		xSeriesName = null;
		for (int i=0;i<nColonne;i++) {
			graphNames[i] = graphNames[i].replace('\"',' ');
			if (graphNames[i].toLowerCase().contains(MAX_Y_STRING.toLowerCase()) && maxYValue != null) {
				mustRescaleYValues = true;
			}
			grafici.add(new Vector<P>());
		}
		for (double xValue : result.getTimeIndices()) {
			for (int i=0;i<nColonne;i++) {
				Double level = result.getConcentrationIfAvailable(graphNames[i], xValue);
				if (level != null) {
					grafici.elementAt(i).add(new P(xValue * xScale, level));
				}
			}
		}
		
		if (!mustRescaleYValues) {
								 //With reference to the two cases listed at the start of the function, this means that we are either in case 1 (and thus we simply need to
								 // get a value for maxYValue), or we are in a "normal" case, where the MAX_X_STRING was not found as a column header. In both cases,
								 // we don't need to rescale the y values of all graphs, which will be simply added to the set of existing series.
			for (int i=0;i<graphNames.length;i++) {
				P[] grafico = new P[1];
				grafico = grafici.elementAt(i).toArray(grafico);
				if (grafico != null && grafico.length > 1) {
					addSeries(grafico, graphNames[i]);
				} else if (graphNames[i].equals(MAX_Y_STRING)) {
					//the y value is the value under this column, the x value is ALWAYS FOR EVERY GRAPH the value of the first column on the same line
					maxYValue = grafico[0].y;
				}
			}
		} else { //We are in case 2
			int indexForOtherMaxY = -1;
			double scaleFactor = 0;
			for (int i=0;i<graphNames.length;i++) {
				if (graphNames[i].equals(MAX_Y_STRING)) {
					indexForOtherMaxY = i;
					scaleFactor = maxYValue / grafici.elementAt(i).elementAt(0).y;
					break;
				}
			}
			for (int i=0;i<graphNames.length;i++) {
				if (i == indexForOtherMaxY) continue;
				P[] grafico = new P[1];
				grafico = grafici.elementAt(i).toArray(grafico);
				if (grafico != null && grafico.length > 1) {
					for (P p : grafico) { //before adding the graph data, we update it by rescaling the y values
						p.y *= scaleFactor;
					}
					addSeries(grafico, graphNames[i]);
				}
			}
		}
		
		boolean doneWithOverlays = false;
		while (!doneWithOverlays) {
			Series seriesToBeInOverlay = null;
			for (Series s : data) {
				if (s.getName().toLowerCase().trim().contains(Series.OVERLAY_SUFFIX.toLowerCase())) {
					seriesToBeInOverlay = s;
					break;
				}
			}
			if (seriesToBeInOverlay != null) {
				String baseName = seriesToBeInOverlay.getName();
				baseName = baseName.substring(0, baseName.lastIndexOf(Series.OVERLAY_SUFFIX));
				Vector<Series> overlay = new Vector<Series>();
				for (Series s : data) {
					if (s.getName().startsWith(baseName + Series.OVERLAY_SUFFIX)) {
						overlay.add(s);
					}
				}
				for (Series s : overlay) {
					data.remove(s);
				}
				data.add(new OverlaySeries(baseName, overlay, this.scale));
				doneWithOverlays = false;
			} else {
				doneWithOverlays = true;
			}
		}
		
		for (Series s : data) {
			if (s.getName().toLowerCase().trim().endsWith(Series.SLAVE_SUFFIX.toLowerCase())) {
				for (Series s2 : data) {
					if (s2.getName().trim().equals(s.getName().trim().substring(0, s.getName().toLowerCase().trim().lastIndexOf(Series.SLAVE_SUFFIX)))) {
						s.setMaster(s2);
					}
				}
			}
			if (this.selectedColumns.size() > 0 && !this.selectedColumns.contains(s.getName())) {
				s.setEnabled(false);
			} else {
				s.setEnabled(true);
			}
		}
		
		//Set the names for all masters/overlays only
		for (Series s : data) {
			if (!s.isSlave()) {
				s.setName(seriesNameMapping.get(s.getName()));
			}
		}
		//Set the names for the remaining slaves (we don't see them printed, but they are exported in csv)
		for (Series s : data) {
			if (s.isSlave()) {
				s.setName(s.getMaster().getName() + Series.SLAVE_SUFFIX);
			}
		}
		
		Collections.sort(data);
		
		customLegendPosition = false;
		needRedraw = true;
	}
	
	/*
	 * Add a new set of Series from a given CSV file, marking all as shown
	 */
	public void parseCSV(String fileName) throws FileNotFoundException, IOException {
		parseCSV(fileName, null);
	}
	
	/*
	 * Add a new set of Series from a given CSV file, marking the given ones as shown
	 */
	public void parseCSV(String fileName, Vector<String> selectedColumns) throws FileNotFoundException, IOException {
		boolean mustRescaleYValues = false; //if we find a column whose name is equal to my constant MAX_Y_STRING, two things can happen:
											//1. maxYValue == null, then we update its value with the (only) value present in this special column
											//2. maxYValue != null, then we rescale the y values of all the series we find in this csv file to the value of maxYValue,
											//   using maxYValue/valueInTheSpecialColumn as scale factor.
											//The mustRescaleYValues is used to signal the fact that we are in the second case
		if (selectedColumns != null) {
			this.selectedColumns.addAll(selectedColumns);
		}
		File f = new File(fileName);
		BufferedReader is = new BufferedReader(new FileReader(f));
		String firstLine = is.readLine();
		if (firstLine == null) {
			is.close();
			throw new IOException("Error: the file " + fileName + " is empty!");
		}
		StringTokenizer tritatutto = new StringTokenizer(firstLine, ",");
		int nColonne = tritatutto.countTokens();
		String[] graphNames = new String[nColonne - 1];
		Vector<Vector<P>> grafici = new Vector<Vector<P>>(graphNames.length);
		xSeriesName = tritatutto.nextToken().replace('\"', ' '); //il primo � la X (tempo)
		for (int i=0;i<graphNames.length;i++) {
			graphNames[i] = tritatutto.nextToken();
			graphNames[i] = graphNames[i].replace('\"',' ');
			if (graphNames[i].toLowerCase().contains(MAX_Y_STRING.toLowerCase()) && maxYValue != null) {
				mustRescaleYValues = true;
			}
			grafici.add(new Vector<P>());
		}
		while (true) {
			String result = is.readLine();
			if (result == null || result.length() < 2) {
				break;
			}
			SmartTokenizer rigaSpezzata = new SmartTokenizer(result, ",");
			String s = rigaSpezzata.nextToken();
			double xValue = Double.parseDouble(s); //here s can't be null (differently from below) because there is absolutely no sense in not giving the x value for the entire line
			int lungRiga = rigaSpezzata.countTokens();
			for (int i=0;i<lungRiga;i++) {
				s = rigaSpezzata.nextToken();
				if (s == null || s.trim().length() < 1) continue; //there could be one of the series which does not have a point in this line: we skip it
				grafici.elementAt(i).add(new P(xValue, Double.parseDouble(s)));
			}
		}
		is.close();
		
		if (!mustRescaleYValues) {
								 //With reference to the two cases listed at the start of the function, this means that we are either in case 1 (and thus we simply need to
								 // get a value for maxYValue), or we are in a "normal" case, where the MAX_X_STRING was not found as a column header. In both cases,
								 // we don't need to rescale the y values of all graphs, which will be simply added to the set of existing series.
			for (int i=0;i<graphNames.length;i++) {
				P[] grafico = new P[1];
				grafico = grafici.elementAt(i).toArray(grafico);
				if (grafico != null && grafico.length > 1) {
					addSeries(grafico, graphNames[i]);
				} else if (graphNames[i].equals(MAX_Y_STRING)) {
					//the y value is the value under this column, the x value is ALWAYS FOR EVERY GRAPH the value of the first column on the same line
					maxYValue = grafico[0].y;
				}
			}
		} else { //We are in case 2
			int indexForOtherMaxY = -1;
			double scaleFactor = 0;
			for (int i=0;i<graphNames.length;i++) {
				if (graphNames[i].equals(MAX_Y_STRING)) {
					indexForOtherMaxY = i;
					scaleFactor = maxYValue / grafici.elementAt(i).elementAt(0).y;
					break;
				}
			}
			for (int i=0;i<graphNames.length;i++) {
				if (i == indexForOtherMaxY) continue;
				P[] grafico = new P[1];
				grafico = grafici.elementAt(i).toArray(grafico);
				if (grafico != null && grafico.length > 1) {
					for (P p : grafico) { //before adding the graph data, we update it by rescaling the y values
						p.y *= scaleFactor;
					}
					addSeries(grafico, graphNames[i]);
				}
			}
		}
		
		for (Series s : data) {
			if (s.getName().toLowerCase().trim().endsWith(Series.SLAVE_SUFFIX)) {
				for (Series s2 : data) {
					if (s2.getName().trim().equals(s.getName().trim().substring(0, s.getName().toLowerCase().trim().lastIndexOf(Series.SLAVE_SUFFIX)))) {
						s.setMaster(s2);
					}
				}
			}
			if (this.selectedColumns.size() > 0 && !this.selectedColumns.contains(s.getName())) {
				s.setEnabled(false);
			} else {
				s.setEnabled(true);
			}
		}
		
		Collections.sort(data);
		
		customLegendPosition = false;
		needRedraw = true;
	}
	
	/*
	 * Export to a CSV file only the Series that are currently visible
	 */
	public void exportVisible(String fileName) throws FileNotFoundException, IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
		out.write(xSeriesName + ",");
		P[][] points = new P[data.size()][];
		int[] indices = new int[data.size()];
		boolean[] finished = new boolean[data.size()];
		int nFinished = 0;
		for (int i=0;i<data.size();i++) { //we use only the enabled series
			if (data.elementAt(i).getEnabled()) {
				finished[i] = false;
			} else {
				finished[i] = true;
				nFinished++;
			}
		}
		for (int i=0;i<data.size();i++) {
			if (finished[i]) continue;
			out.write(data.elementAt(i).getName() + ",");
			points[i] = data.elementAt(i).getData();
			indices[i] = 0;
			finished[i] = false;
		}
		out.newLine();
		//at every cycle, output the minimum x value, and then output all values for which this min x is their x value. the others output an empty space
		//(they will be skipped by my csv parser, and thus we will obtain the exact same graph as the one displayed)
		DecimalFormat formatter = new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.US));
		while (nFinished < finished.length) {
			double minX = Double.NaN;
			for (int i=0;i<points.length;i++) {
				if (!finished[i] && (Double.isNaN(minX) || minX > points[i][indices[i]].x)) {
					minX = points[i][indices[i]].x;
				}
			}
			out.write(formatter.format(minX) + ",");
			for (int i=0;i<points.length;i++) {
				if (!finished[i] && points[i][indices[i]].x == minX) {
					out.write(formatter.format(points[i][indices[i]].y) + ",");
					indices[i]++; //this datum has been used, so we can go to the next
					if (indices[i] == points[i].length) {
						finished[i] = true;
						nFinished++;
					}
				} else {
					if (data.elementAt(i).getEnabled()) out.write(" ,");
				}
			}
			out.newLine();
		}
		out.close();
	}
	
	/*
	 * Set the minimum and maximum for X and Y, defining the area of the graph
	 * to be drawn.
	 */
	public void setDrawArea(int minX, int maxX, int minY, int maxY) {
		scale.setMinX(new Double(minX));
		scale.setMaxX(new Double(maxX));
		scale.setMinY(new Double(minY));
		scale.setMaxY(new Double(maxY));
		for (GraphScaleListener gl : scaleListeners) {
			gl.scaleChanged(scale);
		}
	}
	
	public void setDrawArea(double minX, double maxX, double minY, double maxY) {
		scale.setMinX(minX);
		scale.setMaxX(maxX);
		scale.setMinY(minY);
		scale.setMaxY(maxY);
		for (GraphScaleListener gl : scaleListeners) {
			gl.scaleChanged(scale);
		}
	}
	
	//Simply the string version of the one above. If the numbers in the strings are actually not numbers, I am sorry for the user.
	public void setDrawArea(String minX, String maxX, String minY, String maxY) {
		scale.setMinX(new Double(minX));
		scale.setMaxX(new Double(maxX));
		scale.setMinY(new Double(minY));
		scale.setMaxY(new Double(maxY));
		for (GraphScaleListener gl : scaleListeners) {
			gl.scaleChanged(scale);
		}
	}
	
	//Return the scale
	public Scale getScale() {
		return this.scale;
	}
	
	/**
	 * Set the X position of the red line
	 * @param redLineX
	 */
	public void setRedLinePosition(double redLineX) {
		this.xRedLine = redLineX;
		this.repaint();
	}

	/*
	 * Used only for testing purposes
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			/*Graph g = new Graph();
			JFrame fin = new JFrame("Graph");
			fin.getContentPane().add(g);
			fin.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			fin.setBounds(250,50,800,800);
			fin.setVisible(true);
			Vector<String> cols = new Vector<String>();
			cols.add("MK2_data");
			g.parseCSV(DEFAULT_CSV_FILE, cols);
			g.repaint();*/
			Graph.plotGraph(new File(DEFAULT_CSV_FILE));
			exitOnClose();
		} else {
			Graph.plotGraph(new File(args[0]));
			exitOnClose();
		}
	}

	/*
	 * Plot the given CSV file as a graph in a new Graph window
	 * Notice the "static". Typical usage: Graph.plotGraph("myfile.csv");
	 */
	public static void plotGraph(File csvFile) {
		Graph g = new Graph();
		g.reset();
		try {
			g.parseCSV(csvFile.getAbsolutePath());
		} catch (Exception e) {
			System.err.println(CSV_IO_PROBLEM);
			e.printStackTrace();
		}
		JFrame fin = new JFrame(AUTOGRAPH_WINDOW_TITLE);
		Frame[] listaFinestre = JFrame.getFrames();
		for (int i=0;i<listaFinestre.length;i++) {
			if (listaFinestre[i].getTitle().equals(AUTOGRAPH_WINDOW_TITLE)) {
				fin = (JFrame)listaFinestre[i];
				fin.getContentPane().removeAll();
				break;
			}
		}
		fin.getContentPane().add(g);
		java.awt.Dimension dim = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		fin.setBounds((int)(0.15 * dim.width), (int)(0.15 * dim.height), (int)(0.7 * dim.width), (int)(0.7 * dim.height));
		fin.setVisible(true);
	}
	
	/*
	 * Add a graph to the existing Graph window.
	 * You can use this if you want to use the existing window and add a new .csv files to the already shown ones (the plotGraph method substitutes the window)
	 */
	public static void addGraph(File csvFile) {
		JFrame fin = new JFrame(AUTOGRAPH_WINDOW_TITLE);
		Frame[] listaFinestre = JFrame.getFrames();
		Graph g = null;
		for (int i=0;i<listaFinestre.length;i++) {
			if (listaFinestre[i].getTitle().equals(AUTOGRAPH_WINDOW_TITLE)) {
				fin = (JFrame)listaFinestre[i];
				Component[] components = fin.getContentPane().getComponents();
				for (int j=0;j<components.length;j++) {
					if (components[j] instanceof Graph) {
						g = (Graph)components[j];
						break;
					}
				}
				break;
			}
		}
		if (g == null) {
			g = new Graph();
		}
		try {
			g.parseCSV(csvFile.getAbsolutePath());
		} catch (Exception e) {
			System.err.println(CSV_IO_PROBLEM);
			e.printStackTrace();
		}
		java.awt.Dimension dim = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		fin.setBounds((int)(0.15 * dim.width), (int)(0.15 * dim.height), (int)(0.7 * dim.width), (int)(0.7 * dim.height));
		fin.setVisible(true);
	}

	/*
	 * Set the close event for the Graph window to end also the application
	 */
	public static void exitOnClose() {
		Frame[] listaFinestre = JFrame.getFrames();
		for (int i=0;i<listaFinestre.length;i++) {
			if (listaFinestre[i].getTitle().equals(AUTOGRAPH_WINDOW_TITLE)) {
				JFrame fin = (JFrame)listaFinestre[i];
				fin.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				break;
			}
		}
	}

	/*
	 * You can use the left mouse button to move the legend box around the graph
	 * The middle mouse button can be used anywhere in the graph area to hide/show the legend (showing a hidden legend puts it back to the default place)
	 * The middle mouse button can be used inside the legend box, on the name of a Series to hide/show that Series
	 * The middle mouse button can be used inside the legend box, on the line representing the color of a Series to change the color for that series
	 * The right mouse button inside the legend allows you to change the way the Standard Deviation of the selected series, if present, is shown (none, only bars, only shading, both)
	 * The right mouse button can be used to open the menu for other graph options
	 * The mouse wheel can be used to "zoom" the graph, changing the width of lines and height of fonts (useful for very large/small windows)
	 */
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 1) {
			if (e.getButton() == MouseEvent.BUTTON2) { //Center button
				if (!legendBounds.contains(e.getX(), e.getY()) || !showLegend) {
					showLegend = !showLegend;
					if (showLegend) {
						legendBounds = null;
						customLegendPosition = false;
					}
				} else if (showLegend) { //you can disable/enable series only if the legend is visible
					int xPresumed = e.getX() - legendBounds.x,
						yPresumed = e.getY() - legendBounds.y;
					int seriesIdx = findSeriesInLegend(xPresumed, yPresumed);
					if (seriesIdx != -1) {
						if (xPresumed > 30 * SCALA) {
							this.changeEnabledSeries(seriesIdx);
						} else {
							this.changeSeriesColor(seriesIdx);
						}
					}
				}
				needRedraw = true;
				this.repaint();
			} else if (e.getButton() == MouseEvent.BUTTON3) { //Right button
				if (legendBounds != null && legendBounds.contains(e.getX(), e.getY())) {
					int xPresumed = e.getX() - legendBounds.x,
						yPresumed = e.getY() - legendBounds.y;
					int seriesIdx = findSeriesInLegend(xPresumed, yPresumed);
					if (seriesIdx != -1) {
						if (e.isShiftDown()) {
							this.changeSeriesSymbol(seriesIdx);
							needRedraw = true;
							this.repaint();
						} else {
							/*legendBounds = null;
							customLegendPosition = false;
							this.repaint();*/
							//Instead of replacing the legend in the default position, I let the user ask for explicit error bars
							this.changeErrorBars(seriesIdx);
							needRedraw = true;
							this.repaint();
						}
					}
				} else {
					popupMenu.show(this, e.getX(), e.getY());
				}
			}
		} else if (e.getClickCount() == 2) {
			if (e.getButton() == MouseEvent.BUTTON2) { //Center button
				if (showLegend) { //you can disable/enable series only if the legend is visible
					int xPresumed = e.getX() - legendBounds.x;
					if (xPresumed > 30 * SCALA) {
						this.changeAllEnabledSeries();
					}
				}
				needRedraw = true;
				this.repaint();
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1 && legendBounds != null && legendBounds.contains(e.getX(), e.getY())) {
			customLegendPosition = true;
			oldLegendX = legendBounds.x - e.getX();
			oldLegendY = legendBounds.y - e.getY();
			movingLegend = true;
		} else if (e.getButton() == MouseEvent.BUTTON1 && zoomRectangleBounds != null) {
			drawingZoomRectangle = true;
			zoomRectangleBounds.x = e.getX();
			zoomRectangleBounds.y = e.getY();
			zoomRectangleBounds.width = 0;
			zoomRectangleBounds.height = 0;
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (movingLegend) {
			legendBounds.x = oldLegendX + e.getX();
			legendBounds.y = oldLegendY + e.getY();
			movingLegend = false;
			this.repaint();
		} else if (drawingZoomRectangle) {
			int minimumX = Math.min(zoomRectangleBounds.x, e.getX()), //this (I hope) allows the user to draw the rectangle also "backwards" (i.e. not necessarily from top-left to bottom-right)
				maximumX = Math.max(zoomRectangleBounds.x, e.getX()),
				minimumY = Math.min(zoomRectangleBounds.y, e.getY()),
				maximumY = Math.max(zoomRectangleBounds.y, e.getY());
			if (zoomExtentsBounds == null) {
				zoomExtentsBounds = new Rectangle((int)scale.getMinX(), (int)scale.getMinY(), (int)scale.getMaxX(), (int)scale.getMaxY());
			} else {
				//there is already the starting measure
			}
			this.setDrawArea((int)(((minimumX - 1 - BORDER_X * SCALA) / scale.getXScale() + scale.getMinX())), (int)((maximumX - 1 - BORDER_X * SCALA) / scale.getXScale() + scale.getMinX()), (int)((this.getBounds().height + 1 - maximumY - BORDER_Y * SCALA) / scale.getYScale() + scale.getMinY()), (int)((this.getBounds().height + 1 - minimumY - BORDER_Y * SCALA) / scale.getYScale() + scale.getMinY()));
			zoomRectangleBounds = null;
			drawingZoomRectangle = false;
			this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			needRedraw = true;
			this.repaint();
		}
	}

	public void mouseDragged(MouseEvent e) {
		if (movingLegend) {
			legendBounds.x = oldLegendX + e.getX();
			legendBounds.y = oldLegendY + e.getY();
			oldLegendX = legendBounds.x - e.getX();
			oldLegendY = legendBounds.y - e.getY();
			this.repaint();
		} else if (drawingZoomRectangle) {
			zoomRectangleBounds.width = e.getX() - zoomRectangleBounds.x;
			zoomRectangleBounds.height = e.getY() - zoomRectangleBounds.y;
			this.repaint();
		}
	}

	public void mouseMoved(MouseEvent e) {
		if (legendBounds != null && showLegend && legendBounds.contains(e.getX(), e.getY())) {
			this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		} else if (zoomRectangleBounds != null) {
			this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		} else {
			this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		int notches = e.getWheelRotation();
		if (notches < 0) {
			SCALA++;
		} else {
			SCALA--;
		}
		if (SCALA == 0) SCALA = 1;
		legendBounds = null;
		customLegendPosition = false;
		needRedraw = true;
		this.repaint();
	}
	
	/*
	 * Possible menu options:
	 * - OPEN: add the set of Series contained in a given CSV file
	 * - SAVE to PNG: save the content of the window in a PNG image file
	 * - EXPORT VISIBLE: save the data of all visible Series in a CSV file
	 * - CLEAR: reset the graph, clearing all drawings and Series
	 * - INTERVAL: change the minimum and maximum values for X and Y (a rough zooming feature)
	 * - CLOSE: closes the graph window (when available)
	 */
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source instanceof JMenuItem) {
			JMenuItem menu = (JMenuItem)source;
			if (menu.getText().equals(OPEN_LABEL)) {
				String fileName = FileUtils.open(CSV_FILE_EXTENSION, CSV_FILE_DESCRIPTION, this);
				if (fileName != null) {
					try {
						this.parseCSV(fileName);
						needRedraw = true;
						this.repaint();
						for (GraphScaleListener gl : scaleListeners) {
							gl.scaleChanged(scale);
						}
					} catch (Exception ex) {
						System.err.println(GENERIC_ERROR_S + ex);
						ex.printStackTrace();
					}
				}
			} else if (menu.getText().equals(SAVE_LABEL)) {
				FileUtils.saveToPNG(this);
			} else if (menu.getText().equals(RENDER_TO_JPG_LABEL)) {
				FileUtils.renderToJPG(this);
			} else if (menu.getText().equals(EXPORT_VISIBLE_LABEL)) {
				String fileName = FileUtils.save(CSV_FILE_EXTENSION, CSV_FILE_DESCRIPTION, this);
				if (fileName != null) {
					try {
						this.exportVisible(fileName);
					} catch (Exception ex) {
						System.err.println(GENERIC_ERROR_S + ex);
						ex.printStackTrace();
					}
				}
			} else if (menu.getText().equals(CLEAR_LABEL)) {
				this.reset();
				needRedraw = true;
				this.repaint();
			} else if (menu.getText().equals(INTERVAL_LABEL)) {
				String valMinX = JOptionPane.showInputDialog(this, "Give the value of minimum X", scale.getMinX());
				if (valMinX == null) return;
				String valMaxX = JOptionPane.showInputDialog(this, "Give the value of maximum X", scale.getMaxX());
				if (valMaxX == null) return;
				String valMinY = JOptionPane.showInputDialog(this, "Give the value of minimum Y", scale.getMinY());
				if (valMinY == null) return;
				String valMaxY = JOptionPane.showInputDialog(this, "Give the value of maximum Y", scale.getMaxY());
				if (valMaxY == null) return;
				this.setDrawArea(valMinX, valMaxX, valMinY, valMaxY);
				/*scale.setMinX(new Double(valMinX));
				scale.setMaxX(new Double(valMaxX));
				scale.setMinY(new Double(valMinY));
				scale.setMaxY(new Double(valMaxY));*/
				needRedraw = true;
				this.repaint();
			} else if (menu.getText().equals(CLOSE_LABEL)) {
				//findJFrame(this).setVisible(false);
				findJFrame(this).dispose();
			} else if (menu.getText().equals(ZOOM_RECTANGLE_LABEL)) {
				zoomRectangleBounds = new Rectangle(0, 0, 0, 0);
				this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			} else if (menu.getText().equals(ZOOM_EXTENTS_LABEL)) {
				if (zoomExtentsBounds != null) {
					this.setDrawArea(zoomExtentsBounds.x, zoomExtentsBounds.width, zoomExtentsBounds.y, zoomExtentsBounds.height);
					needRedraw = true;
					this.repaint();
				} else {
					//do nothing
				}
			} else if (menu.getText().equals(SHOW_SIZE_LABEL)) {
				showSize = !showSize;
				this.repaint();
			} else if (menu.getText().equals(SHOW_ZOOM_LEVEL_LABEL)) {
				showZoomLevel = !showZoomLevel;
				this.repaint();
			} else if (menu.getText().equals(SET_Y_LABEL_LABEL)) {
				String newLabel;
				if (this.getYLabel() == null) {
					newLabel = "Y";
				} else {
					newLabel = this.getYLabel();
				}
				newLabel = JOptionPane.showInputDialog(this, "Give the new label for Y axis", newLabel);
				if (newLabel != null) {
					this.setYLabel(newLabel);
					needRedraw = true;
					this.repaint();
				}
			} else if (menu.getText().equals(SHOW_THIN_AXES)) {
				this.showThinAxes = !this.showThinAxes;
				needRedraw = true;
				this.repaint();
			} else if (menu.getText().equals(SHOW_THICK_AXES)) {
				this.showThickAxes = !this.showThickAxes;
				needRedraw = true;
				this.repaint();
			} else if (menu.getText().equals(STEP_SHAPED_GRAPH_LABEL)) {
				this.stepShapedLines = !this.stepShapedLines;
				needRedraw = true;
				this.repaint();
			} else if (menu.getText().equals(HEATMAP_GRAPH_LABEL)) {
				this.useHeatMap = ! this.useHeatMap;
				needRedraw = true;
				this.repaint();
			}
		}
	}
	
	private JFrame findJFrame(Component c) {
		if (c instanceof JFrame) {
			return (JFrame)c;
		} else {
			return findJFrame(c.getParent());
		}
	}

	private int oldWidth = -1, oldHeight = -1;
	public void componentResized(ComponentEvent e) {
		if (!showLegend || !customLegendPosition) {
			oldWidth = this.getWidth();
			oldHeight = this.getHeight();
		} else {
			if (oldWidth != -1 && oldHeight != -1) {
				legendBounds.x = (int)((double)legendBounds.x / oldWidth * this.getWidth());
				legendBounds.y = (int)((double)legendBounds.y / oldHeight * this.getHeight());
			}
			oldWidth = this.getWidth();
			oldHeight = this.getHeight();
		}
		needRedraw = true;
		repaint();
	}
	
	public void componentHidden(ComponentEvent e) {
		//Auto-generated method stub
	}

	public void componentMoved(ComponentEvent e) {
		//Auto-generated method stub
	}

	public void componentShown(ComponentEvent e) {
		//Auto-generated method stub
	}

	public void addGraphScaleListener(GraphScaleListener gl) {
		scaleListeners.add(gl);
	}
	
	public void removeGraphScaleListener(GraphScaleListener gl) {
		scaleListeners.remove(gl);
	}
}
