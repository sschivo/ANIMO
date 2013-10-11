package inat.cytoscape;

import giny.model.Edge;
import giny.model.Node;
import giny.model.RootGraph;
import giny.view.Bend;
import giny.view.EdgeView;
import giny.view.NodeView;
import inat.analyser.uppaal.ResultAverager;
import inat.analyser.uppaal.SimpleLevelResult;
import inat.graph.FileUtils;
import inat.graph.Graph;
import inat.graph.GraphScaleListener;
import inat.graph.Scale;
import inat.model.Model;
import inat.util.Heptuple;
import inat.util.Pair;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.install4j.runtime.util.Base64;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.view.CyNetworkView;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.view.cytopanels.CytoPanelImp;
import cytoscape.view.cytopanels.CytoPanelListener;
import cytoscape.view.cytopanels.CytoPanelState;
import cytoscape.visual.CalculatorCatalog;
import cytoscape.visual.VisualMappingManager;

/**
 * The Inat result panel.
 * 
 * @author Brend Wanders
 */
public class InatResultPanel extends JPanel implements ChangeListener, GraphScaleListener, CytoPanelListener {

	private static final long serialVersionUID = -163756255393221954L;
	private final Model model; //The model from which the results were obtained
	private final SimpleLevelResult result; //Contains the results to be shown in this panel
	private JPanel container; //The panel on which all the components of this resultPanel are layed out (thus, not only the panel itself, but also the slider, buttons etc)
	private int myIndex; //The index at which the panel is placed when added to the CytoPanel
	private JSlider slider; //The slider to allow the user to choose a moment in the simulation time, which will be reflected on the network window as node colors, indicating the corresponding reactant activity level.
	private double scale, //The time scale
				   minValueOnGraph,
				   maxValueOnGraph, //the scale to translate a value of the slider (in the interval [0,1]) to the corresponding position in the graph
				   scaleForConcentration; //the scale to translate a value of the slider (in the interval [0,1]) to the corresponding value resulting from the UPPAAL trace
	private CyNetwork savedNetwork; //The network which generated the data we are displaying in this panel: we can use it to replace the network at the user's command
	private BufferedImage savedNetworkImage; //The image of the saved network: we can use it to display as a tooltip in the "reset to these settings" button
	private HashMap<String, HashMap<String, Object>> savedNodeAttributes, //The attributes of the nodes in the saved network
													 savedEdgeAttributes; //Guess what
	private static final String PROP_POSITION_X = "Position.X",
								PROP_POSITION_Y = "Position.Y",
								PROP_BEND_HANDLES = "Bend.handles",
								DEFAULT_TITLE = "ANIMO Results",
								START_DIFFERENCE = "Difference with...", //The three strings here are for one button. Everybody normally shows START_DIFFERENCE. When the user presses on the button (differenceWith takes the value of this for the InatResultPanel where the button was pressed),
								END_DIFFERENCE = "Difference with this", //every other panel shows the END_DIFFERENCE. If the user presses a button with END_DIFFERENCE, a new InatResultPanel is created with the difference between the data in differenceWith and the panel where END_DIFFERENCE was pressed. Then every panel goes back to START_DIFFERENCE.
								CANCEL_DIFFERENCE = "Cancel difference"; //CANCEL_DIFFERENCE is shown as text of the button only on the panel whare START_DIFFERENCE was pressed. If CANCEL_DIFFERENCE is pressed, then no difference is computed and every panel simply goes back to START_DIFFERENCE
	private String title; //The title to show to the user
	private Graph g;
	
	private JButton differenceButton = null;
	private static InatResultPanel differenceWith = null;
	private static Vector<InatResultPanel> allExistingPanels = new Vector<InatResultPanel>();
	private String vizMapName = null;
	private boolean isDifference = false;
	
	
	/**
	 * Load the simulation data from a file instead than getting it from a simulation we have just made
	 * @param model The representation of the current Cytoscape network, to which the simulation data
	 * will be coupled (it is REQUIRED that the simulation was made on the same network, or else the node IDs
	 * will not be the same, and the slider will not work properly)
	 * @param simulationDataFile The file from which to load the data. For the format, see saveSimulationData()
	 */
	public InatResultPanel(File simulationDataFile) {
		this(loadSimulationData(simulationDataFile, true));
	}
	
	public InatResultPanel(Heptuple<Model, SimpleLevelResult, Double, String, String, HashMap<String, HashMap<String, Object>>, HashMap<String, HashMap<String, Object>>> simulationData) {
		this(simulationData.first, simulationData.second, simulationData.third, simulationData.fourth, null);
	}
	
	public InatResultPanel(Model model, SimpleLevelResult result, double scale, CyNetwork originalNetwork) {
		this(model, result, scale, DEFAULT_TITLE, originalNetwork);
	}
	
	public static InatResultPanel loadFromSessionSimFile(File simulationDataFile) {
		Heptuple<Model, SimpleLevelResult, Double, String, String, HashMap<String, HashMap<String, Object>>, HashMap<String, HashMap<String, Object>>> simulationData = loadSimulationData(simulationDataFile, false);
		InatResultPanel panel = new InatResultPanel(simulationData.first, simulationData.second, simulationData.third, simulationData.fourth, null);
		panel.savedNetwork = Cytoscape.getNetwork(simulationData.fifth);
		if (panel.savedNetwork != null && !panel.savedNetwork.equals(Cytoscape.getNullNetwork())) {
			//System.err.println("La rete salvata " + panel.savedNetwork.getIdentifier() + " ha " + panel.savedNetwork.getNodeCount() + " nodi e " + panel.savedNetwork.getEdgeCount() + " edgi");
			CyNetworkView savedNetworkView = Cytoscape.getNetworkView(simulationData.fifth);
			if (savedNetworkView != null && !savedNetworkView.equals(Cytoscape.getNullNetworkView())) { //Keep all saved networks hidden
				Cytoscape.destroyNetworkView(savedNetworkView);
				CyNetworkView otherView = null;
				Collection<CyNetworkView> c = Cytoscape.getNetworkViewMap().values();
				if (!c.isEmpty()) {
					otherView = c.iterator().next();
				}
				if (otherView != null) {
					try {
						@SuppressWarnings("rawtypes")
						List edgesList = otherView.getEdgeViewsList();
						for (Object o : edgesList) {
							EdgeView edge = (EdgeView)o;
							edge.setLineType(EdgeView.CURVED_LINES);
						}
					} catch (Exception e) {
					}
					try {
						Cytoscape.getDesktop().getNetworkViewManager().getInternalFrame(otherView).setSelected(true);
					} catch (Exception e) {
						//We try, just to keep someone in foreground (possibly, at the end only the "main" network will remain)
					}
				}
			}
		} else {
			//System.err.println("UNA RETE SALVATA NON E' STATA RECUPERATA!!!!!!!!!!!!!!!");
		}
		panel.savedNetworkImage = null; //We cannot save BufferedImages to file as objects, as they are not serializable, so we will show no image of the network
		panel.savedNodeAttributes = simulationData.sixth;
		panel.savedEdgeAttributes = simulationData.seventh;
		allExistingPanels.add(panel);
		return panel;
	}
	
	/**
	 * The panel constructor.
	 * 
	 * @param model the model this panel uses
	 * @param result the results object this panel uses
	 */
	@SuppressWarnings("unchecked")
	public InatResultPanel(final Model model, final SimpleLevelResult result, double scale, String title, CyNetwork originalNetwork) {
		super(new BorderLayout(), true);
		allExistingPanels.add(this);
		this.model = model;
		this.result = result;
		this.scale = scale;
		this.title = title;
		if (originalNetwork != null && !originalNetwork.equals(Cytoscape.getNullNetwork())) {
			try {
				CyNetworkView view = Cytoscape.getNetworkView(originalNetwork.getIdentifier());
				@SuppressWarnings("rawtypes")
				Iterator it = view.getNodeViewsIterator();
				Vector<NodeView> hiddenNodes = new Vector<NodeView>();
				while (it.hasNext()) {
					Object o = it.next();
					NodeView n = (NodeView)o;
					//System.err.println("Altezza del nodo " + n + " = " + n.getHeight());
					if (n.getHeight() < 0) { //n.isHidden()) { //Unfortunately, isHidden does not work (!!!), so we see if the node has a negative size: in that case it is probably hidden.. 
						hiddenNodes.add(n);
						view.showGraphObject(n);
					}
				}
				//at this point, all nodes are visible and nodesToBeReHiddenAfterDoneHere contains the indexes of those nodes which were hidden before we started. They will be re-hidden after we have finished copying the needed things
				
				try {
					Component componentToBeSaved = Cytoscape.getCurrentNetworkView().getComponent();
					savedNetworkImage = new BufferedImage(componentToBeSaved.getWidth()/2, componentToBeSaved.getHeight()/2, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = savedNetworkImage.createGraphics();
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g.scale(0.5, 0.5);
					g.setPaint(Cytoscape.getCurrentNetworkView().getBackgroundPaint()); //Cytoscape.getVisualMappingManager().getVisualStyle().getGlobalAppearanceCalculator().getDefaultBackgroundColor());
					g.fillRect(0, 0, componentToBeSaved.getWidth(), componentToBeSaved.getHeight());
					componentToBeSaved.paint(g);
				} catch (Exception ex) {
					//We don't want this trick with images to fail and make really important things fail.
					ex.printStackTrace();
				}
				/*Random rand = new Random();
				String name = "Network_" + rand.nextInt();
				while (!Cytoscape.getNetwork(name).equals(Cytoscape.getNullNetwork())) {
					name = "Network_" + rand.nextInt();
				}*/
				savedNetwork = Cytoscape.createNetwork(null);//name);
				//savedNetwork.setIdentifier(name);
				CyAttributes nodeAttr = Cytoscape.getNodeAttributes(),
							 edgeAttr = Cytoscape.getEdgeAttributes();
				String[] nodeAttrNames = nodeAttr.getAttributeNames(),
						 edgeAttrNames = edgeAttr.getAttributeNames();
				List<CyNode> origNodes = originalNetwork.nodesList();
				List<CyEdge> origEdges = originalNetwork.edgesList();
				RootGraph rgNew = savedNetwork.getRootGraph();
				HashMap<Integer, Integer> mapEdgeIDs = new HashMap<Integer, Integer>(),
										  mapNodeIDs = new HashMap<Integer, Integer>();
				savedNodeAttributes = new HashMap<String, HashMap<String, Object>>();
				savedEdgeAttributes = new HashMap<String, HashMap<String, Object>>();
				for (CyNode n : origNodes) {
					int idx = rgNew.createNode();
					mapNodeIDs.put(n.getRootGraphIndex(), idx);
					Node o = rgNew.getNode(idx);
					o.setIdentifier(n.getIdentifier());
					savedNodeAttributes.put(o.getIdentifier(), new HashMap<String, Object>());
					for (String s : nodeAttrNames) {
						if (nodeAttr.hasAttribute(n.getIdentifier(), s)) {
							byte type = nodeAttr.getType(s);
							switch (type) {
								case CyAttributes.TYPE_BOOLEAN:
									savedNodeAttributes.get(o.getIdentifier()).put(s, nodeAttr.getBooleanAttribute(n.getIdentifier(), s));
									break;
								case CyAttributes.TYPE_FLOATING:
									savedNodeAttributes.get(o.getIdentifier()).put(s, nodeAttr.getDoubleAttribute(n.getIdentifier(), s));
									break;
								case CyAttributes.TYPE_INTEGER:
									savedNodeAttributes.get(o.getIdentifier()).put(s, nodeAttr.getIntegerAttribute(n.getIdentifier(), s));
									break;
								case CyAttributes.TYPE_SIMPLE_LIST:
									savedNodeAttributes.get(o.getIdentifier()).put(s, nodeAttr.getListAttribute(n.getIdentifier(), s));
									break;
								case CyAttributes.TYPE_SIMPLE_MAP:
									savedNodeAttributes.get(o.getIdentifier()).put(s, nodeAttr.getMapAttribute(n.getIdentifier(), s));
									break;
								case CyAttributes.TYPE_STRING:
									savedNodeAttributes.get(o.getIdentifier()).put(s, nodeAttr.getStringAttribute(n.getIdentifier(), s));
									break;
								case CyAttributes.TYPE_COMPLEX:
									break;
								case CyAttributes.TYPE_UNDEFINED:
									break;
								default:
									break;
							}
						}
					}
					savedNetwork.addNode(o);
				}
				for (CyEdge e : origEdges) {
					int idx = rgNew.createEdge(mapNodeIDs.get(e.getSource().getRootGraphIndex()), mapNodeIDs.get(e.getTarget().getRootGraphIndex()));
					mapEdgeIDs.put(e.getRootGraphIndex(), idx);
					Edge d = rgNew.getEdge(idx);
					d.setIdentifier(e.getIdentifier());
					savedEdgeAttributes.put(d.getIdentifier(), new HashMap<String, Object>());
					for (String s : edgeAttrNames) {
						if (edgeAttr.hasAttribute(e.getIdentifier(), s)) {
							byte type = edgeAttr.getType(s);
							switch (type) {
								case CyAttributes.TYPE_BOOLEAN:
									savedEdgeAttributes.get(d.getIdentifier()).put(s, edgeAttr.getBooleanAttribute(e.getIdentifier(), s));
									break;
								case CyAttributes.TYPE_FLOATING:
									savedEdgeAttributes.get(d.getIdentifier()).put(s, edgeAttr.getDoubleAttribute(e.getIdentifier(), s));
									break;
								case CyAttributes.TYPE_INTEGER:
									savedEdgeAttributes.get(d.getIdentifier()).put(s, edgeAttr.getIntegerAttribute(e.getIdentifier(), s));
									break;
								case CyAttributes.TYPE_SIMPLE_LIST:
									savedEdgeAttributes.get(d.getIdentifier()).put(s, edgeAttr.getListAttribute(e.getIdentifier(), s));
									break;
								case CyAttributes.TYPE_SIMPLE_MAP:
									savedEdgeAttributes.get(d.getIdentifier()).put(s, edgeAttr.getMapAttribute(e.getIdentifier(), s));
									break;
								case CyAttributes.TYPE_STRING:
									savedEdgeAttributes.get(d.getIdentifier()).put(s, edgeAttr.getStringAttribute(e.getIdentifier(), s));
									break;
								case CyAttributes.TYPE_COMPLEX:
									break;
								case CyAttributes.TYPE_UNDEFINED:
									break;
								default:
									break;
							}
						}
					}
					savedNetwork.addEdge(d);
				}
				CyNetworkView originalView = Cytoscape.getNetworkView(originalNetwork.getIdentifier());
				Iterator<NodeView> itNodes = originalView.getNodeViewsIterator();
				while (itNodes.hasNext()) {
					NodeView n = itNodes.next();
					Point2D p = n.getOffset();
					nodeAttr.setAttribute(savedNetwork.getNode(mapNodeIDs.get(n.getRootGraphIndex())).getIdentifier(), PROP_POSITION_X, p.getX());
					nodeAttr.setAttribute(savedNetwork.getNode(mapNodeIDs.get(n.getRootGraphIndex())).getIdentifier(), PROP_POSITION_Y, p.getY());
					savedNodeAttributes.get(n.getNode().getIdentifier()).put(PROP_POSITION_X, p.getX());
					savedNodeAttributes.get(n.getNode().getIdentifier()).put(PROP_POSITION_Y, p.getY());
				}
				Iterator<EdgeView> itEdges = originalView.getEdgeViewsIterator();
				while (itEdges.hasNext()) {
					EdgeView e = itEdges.next();
					@SuppressWarnings("rawtypes")
					List handles = e.getBend().getHandles();
					Vector<Double> savedHandles = new Vector<Double>();
					for (Object o : handles) {
						Point2D handle = (Point2D)o;
						savedHandles.add(handle.getX());
						savedHandles.add(handle.getY());
					}
					edgeAttr.setListAttribute(savedNetwork.getEdge(mapEdgeIDs.get(e.getRootGraphIndex())).getIdentifier(), PROP_BEND_HANDLES, savedHandles);
					savedEdgeAttributes.get(e.getEdge().getIdentifier()).put(PROP_BEND_HANDLES, savedHandles);
				}
				Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);
				Cytoscape.setCurrentNetworkView(originalView.getIdentifier());
				CytoPanel controlPanel = Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
				controlPanel.setSelectedIndex(controlPanel.indexOfComponent(InatPlugin.TAB_NAME));
				for (NodeView n : hiddenNodes) { //re-hide the hidden nodes after finishing
					originalView.hideGraphObject(n);
				}
				Cytoscape.getDesktop().getNetworkViewManager().getInternalFrame(originalView).setSelected(true);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Exception: " + ex);
				ex.printStackTrace();
			}
		} else {
			this.savedNetwork = null;
			this.savedNetworkImage = null;
			this.savedNodeAttributes = null;
			this.savedEdgeAttributes = null;
		}
		
		JPanel sliderPanel = new JPanel(new BorderLayout());
		this.slider = new JSlider();
		this.slider.setOrientation(JSlider.HORIZONTAL);
		this.slider.setMinimum(0);
		//int max = (int)Math.round(result.getTimeIndices().get(result.getTimeIndices().size() - 1) * scale);
		if (result.isEmpty()) {
			this.scaleForConcentration = 1;
		} else {
			this.scaleForConcentration = result.getTimeIndices().get(result.getTimeIndices().size() - 1);
		}
		this.minValueOnGraph = 0;
		this.maxValueOnGraph = this.scaleForConcentration * this.scale;
		this.slider.setMaximum(200);
		this.slider.setPaintTicks(true);
		this.slider.setPaintLabels(true);
		slider.setMajorTickSpacing(slider.getMaximum() / 10);
		slider.setMinorTickSpacing(slider.getMaximum() / 100);
		/*if (max / 10 >= 1) {
			this.slider.setMajorTickSpacing(max / 10);
		} else {
			this.slider.setMajorTickSpacing(1);
		}
		if (max / 100 >= 1) {
			this.slider.setMinorTickSpacing(max / 100);
		} else {
			this.slider.setMinorTickSpacing(1);
		}*/
		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		DecimalFormat formatter = new DecimalFormat("0.###");
		int nLabels = 10;
		for (int i=0;i<=nLabels;i++) {
			double val = 1.0 * i / nLabels * this.maxValueOnGraph;
			String valStr = formatter.format(val);
			labels.put((int)Math.round(1.0 * i / nLabels * slider.getMaximum()), new JLabel(valStr));
		}
		this.slider.setLabelTable(labels);
		
		this.slider.setValue(0);
		this.slider.getModel().addChangeListener(this);
		
		ImageIcon icon = null;
		try {
			icon = new ImageIcon(getClass().getResource("/copy20x20.png"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		JButton setParameters;
		if (icon != null) {
			setParameters = new JButton(icon);
		} else {
			setParameters = new JButton("Copy");
		}
		setParameters.setToolTipText("Copy the currently shown activity levels as initial activity levels in the model");
		setParameters.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//set the initial activity levels of the reactants in the network as they are in this point of the simulation ("this point" = where the slider is currently)
				double t = getSliderTime();
				CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
				int nLevels = result.getNumberOfLevels();
				for (String r : result.getReactantIds()) {
					if (model.getReactant(r) == null) continue;
					final String id = model.getReactant(r).get(Model.Properties.REACTANT_NAME).as(String.class);
					//System.err.println("R id = " + id);
					final double level = result.getConcentration(r, t) / nLevels * nodeAttributes.getIntegerAttribute(id, Model.Properties.NUMBER_OF_LEVELS); //model.getReactant(r).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class); //We also rescale the value to the correct number of levels of each node. Attention: we need to use the CURRENT number of levels of the node, or we will get inconsistent results!
					nodeAttributes.setAttribute(id, Model.Properties.INITIAL_LEVEL, (int)Math.round(level));
				}
			}
		});
		sliderPanel.add(setParameters, BorderLayout.WEST);
		
		sliderPanel.add(this.slider, BorderLayout.CENTER);
		
		this.add(sliderPanel, BorderLayout.SOUTH);
		
		g = new Graph();
		//We map reactant IDs to their corresponding aliases (canonical names, i.e., the names displayed to the user in the network window), so that
		//we will be able to use graph series names consistent with what the user has chosen.
		Map<String, String> seriesNameMapping = new HashMap<String, String>();
		Vector<String> filteredSeriesNames = new Vector<String>(); //profit from the cycle for the series mapping to create a filter for the series to be actually plotted
		for (String r : result.getReactantIds()) {
			if (r.charAt(0) == 'E') continue; //If the identifier corresponds to an edge (e.g. "Enode123 (DefaultEdge) node456") we don't consider it, as we are looking only at series to be plotted in the graph, and those data instead are used for the slider (edge highlighting corresponding to reaction "strength")
			String name = null;
			String stdDevReactantName = null;
			if (model.getReactant(r) != null) { //we can also refer to a name not present in the reactant collection
				name = model.getReactant(r).get(Model.Properties.ALIAS).as(String.class); //if an alias is set, we prefer it
				if (name == null) {
					name = model.getReactant(r).get(Model.Properties.REACTANT_NAME).as(String.class);
				}
			} else if (r.toLowerCase().contains(ResultAverager.STD_DEV.toLowerCase())) {
				stdDevReactantName = r.substring(0, r.lastIndexOf(ResultAverager.STD_DEV));
				if (model.getReactant(stdDevReactantName).get(Model.Properties.ALIAS).as(String.class) != null) {
					name = model.getReactant(stdDevReactantName).get(Model.Properties.ALIAS).as(String.class) + ResultAverager.STD_DEV;
				} else {
					name = r; //in this case, I simply don't know what we are talking about =)
				}
			} else if (r.toLowerCase().contains(ResultAverager.OVERLAY_NAME.toLowerCase())) {
				stdDevReactantName = r.substring(0, r.lastIndexOf(ResultAverager.OVERLAY_NAME));
				//System.err.println("Overlay series: \"" + r + "\" --> \"" + stdDevReactantName + "\"");
				/*if (model.getReactant(stdDevReactantName) == null) {
					System.err.println("Reactant \"" + stdDevReactantName + "\" unknown to the model!");
				}*/
				if (model.getReactant(stdDevReactantName).get(Model.Properties.ALIAS).as(String.class) != null) {
					name = model.getReactant(stdDevReactantName).get(Model.Properties.ALIAS).as(String.class); // + r.substring(r.lastIndexOf(ResultAverager.OVERLAY_NAME));
					seriesNameMapping.put(stdDevReactantName, name);
				} else {
					name = r; //boh
				}
			}
			if ((!r.toLowerCase().contains(ResultAverager.STD_DEV.toLowerCase()) && !r.toLowerCase().contains(ResultAverager.OVERLAY_NAME.toLowerCase()) 
						&& model.getReactant(r) != null && model.getReactant(r).get(Model.Properties.PLOTTED).as(Boolean.class))
				|| ((r.toLowerCase().contains(ResultAverager.STD_DEV.toLowerCase()) || r.toLowerCase().contains(ResultAverager.OVERLAY_NAME.toLowerCase())) 
						&& model.getReactant(stdDevReactantName) != null && model.getReactant(stdDevReactantName).get(Model.Properties.PLOTTED).as(Boolean.class))) {
				
				filteredSeriesNames.add(r);
			}
			seriesNameMapping.put(r, name);
		}
		g.parseLevelResult(result.filter(filteredSeriesNames), seriesNameMapping, scale); //Add all series to the graph, using the mapping we built here to "translate" the names into the user-defined ones.
		g.setXSeriesName("Time (min)");
		g.setYLabel("Protein activity (a. u.)");
		
		if (!model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).isNull()) { //if we find a maximum value for activity levels, we declare it to the graph, so that other added graphs (such as experimental data) will be automatically rescaled to match us
			int nLevels = model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
			g.declareMaxYValue(nLevels);
			double maxTime = scale * result.getTimeIndices().get(result.getTimeIndices().size()-1);
//			int maxTimeInt = (int)maxTime;
//			if (maxTimeInt < maxTime) { //If we get 6.2 as last time on the trace, we show up to 7, so that we let the user see also the end of the trace
//				maxTimeInt++;
//			}
			g.setDrawArea(0, maxTime, 0, nLevels); //This is done because the graph automatically computes the area to be shown based on minimum and maximum values for X and Y, including StdDev. So, if the StdDev of a particular series (which represents an average) in a particular point is larger that the value of that series in that point, the minimum y value would be negative. As this is not very nice to see, I decided that we will recenter the graph to more strict bounds instead.
														//Also, if the maximum value reached during the simulation is not the maximum activity level, the graph does not loook nice
		}
		this.add(g, BorderLayout.CENTER);
		g.addGraphScaleListener(this);
	}
	
	/**
	 * Callback method to be used by a Cytoscape session events listener
	 * We make sure that whatever features were linked to the previously
	 * available session are not accessible to the user: this avoids unwanted
	 * behavior.
	 */
	public void sessionHasChanged() {
		//this.savedNetwork = null; //Do NOT set it to null: we get this notification that the session has changed also when we have just loaded our network
		countSessionChanges++;
		//If we had not set the session name before (i.e., we have just been loaded), we set the session name. Otherwise, the current session has changed, so it is not possible/safe to change current network (mostly because the saved network is not reachable anymore, being stored in the other session!)
		//Disable the button for resetting the network if the current session is not the session in which we have saved the network
		if (resetToThisNetwork != null && countSessionChanges > 1) { //The first session change corresponds to loading "our" session (unfortunately, when the SESSION_LOADED event is triggered the session name is still the old one, so we cannot remember the name!)
			resetToThisNetwork.setVisible(false); //Any other session change means that we don't know which session we are in (as we cannot know the name of "our" session), so to avoid unwanted behavior we disable the reset button
		}
	}
	
	public CyNetwork getSavedNetwork() {
		return this.savedNetwork;
	}
	
	public BufferedImage getSavedNetworkImage() {
		return this.savedNetworkImage;
	}
	
	public void setSavedNetworkImage(BufferedImage image) {
		this.savedNetworkImage = image;
	}

	
	private Map<String, Edge> cytoscapeEdgesByIdentifier = null;
	private Edge getEdgeFromIdentifier(String identifier) {
		if (cytoscapeEdgesByIdentifier == null) {
			cytoscapeEdgesByIdentifier = new HashMap<String, Edge>();
			@SuppressWarnings("rawtypes")
			List edges = Cytoscape.getCurrentNetwork().edgesList();
			for (Object o : edges) {
				Edge e = (Edge)o;
				cytoscapeEdgesByIdentifier.put(e.getIdentifier(), e);
			}
		}
		return cytoscapeEdgesByIdentifier.get(identifier);
	}
	
	private double getSliderTime() {
		return (1.0 * this.slider.getValue() / this.slider.getMaximum() * (this.maxValueOnGraph - this.minValueOnGraph) + this.minValueOnGraph) / this.scale; //1.0 * this.slider.getValue() / this.slider.getMaximum() * this.scaleForConcentration; //this.slider.getValue() / scale;
	}
	
	private HashMap<Integer, Pair<Boolean, Vector<Integer>>> convergingEdges = null;
	/**
	 * When the user moves the time slider, we update the activity ratio (SHOWN_LEVEL) of
	 * all nodes in the network window, so that, thanks to the continuous Visual Mapping
	 * defined when the interface is augmented (see AugmentAction), different colors will
	 * show different activity levels. 
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		final double t = getSliderTime();
		
		double graphWidth = this.maxValueOnGraph - this.minValueOnGraph;
		g.setRedLinePosition(1.0 * this.slider.getValue() / this.slider.getMaximum() * graphWidth);
		
		if (convergingEdges == null) {
			convergingEdges = new HashMap<Integer, Pair<Boolean, Vector<Integer>>>();
			HashMap<String, Integer> cytoscapeNodesByIdentifier = new HashMap<String, Integer>();
			@SuppressWarnings("rawtypes")
			List nodes = Cytoscape.getCurrentNetwork().nodesList();
			for (Object o : nodes) {
				Node n = (Node)o;
				cytoscapeNodesByIdentifier.put(n.getIdentifier(), n.getRootGraphIndex());
			}
			RootGraph rootG = Cytoscape.getCurrentNetwork().getRootGraph();
			for (String r : this.result.getReactantIds()) {
				if (this.model.getReactant(r) == null) continue;
				final String id = this.model.getReactant(r).get(Model.Properties.REACTANT_NAME).as(String.class);
				int[] incomingEdges = rootG.getAdjacentEdgeIndicesArray(cytoscapeNodesByIdentifier.get(id), true, true, false);
				if (incomingEdges.length > 1) {
					Pair<Boolean, Vector<Integer>> edgesGroup = new Pair<Boolean, Vector<Integer>>(true, new Vector<Integer>());
					for (int i=0;i<incomingEdges.length;i++) {
						edgesGroup.second.add(incomingEdges[i]);
					}
					for (int i=0;i<incomingEdges.length;i++) {
						convergingEdges.put(incomingEdges[i], edgesGroup);
					}
				}
			}
		} else {
			for (Pair<Boolean, Vector<Integer>> group : convergingEdges.values()) {
				group.first = true; //Each edge in each group is initially assumed to be a candidate for having 0 activityRatio
			}
		}
		
		CyAttributes nodeAttributes = Cytoscape.getNodeAttributes(),
					 edgeAttributes = Cytoscape.getEdgeAttributes();
		final int levels = this.model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class); //at this point, all levels have already been rescaled to the maximum (= the number of levels of the model), so we use it as a reference for the number of levels to show on the network nodes 
		for (String r : this.result.getReactantIds()) {
			if (this.model.getReactant(r) == null) continue;
			final String id = this.model.getReactant(r).get(Model.Properties.REACTANT_NAME).as(String.class);
			//System.err.println("R id = " + id);
			final double level = this.result.getConcentration(r, t);
			nodeAttributes.setAttribute(id, Model.Properties.SHOWN_LEVEL, level / levels);
		}
		try {
			for (String r : this.result.getReactantIds()) {
				if (!(r.charAt(0) == 'E')) continue;
				//if (!edgeAttributes.hasAttribute(r, Model.Properties.ENABLED)) continue; //Just to check if that is a valid Cytoscape id for a reaction
				//int edgeId = Integer.parseInt(r.substring(1));
				String edgeId = r.substring(1);
				Edge edge = null;// = Cytoscape.getCurrentNetwork().getEdge(edgeId); //Let's thank again Cytoscape for not keeping the rootGraphIndices constant...
				edge = getEdgeFromIdentifier(edgeId);
				if (edge == null) continue;
				if (t == 0) {
					if (edgeAttributes.hasAttribute(edge.getIdentifier(), Model.Properties.SHOWN_LEVEL)) {
						edgeAttributes.deleteAttribute(edge.getIdentifier(), Model.Properties.SHOWN_LEVEL);
					}
				} else {
					int scenario = edgeAttributes.getIntegerAttribute(edge.getIdentifier(), Model.Properties.SCENARIO);
					double concentration = this.result.getConcentration(r, t);
					boolean candidate = false;
					switch (scenario) {
						case 0:
							if (nodeAttributes.getDoubleAttribute(edge.getSource().getIdentifier(), Model.Properties.SHOWN_LEVEL) == 0) {
								concentration = 0;
								candidate = true;
							}
							break;
						case 1:
							if (nodeAttributes.getDoubleAttribute(edge.getSource().getIdentifier(), Model.Properties.SHOWN_LEVEL) == 0) {
								concentration = 0;
								candidate = true;
							} else if ((edgeAttributes.getIntegerAttribute(edge.getIdentifier(), Model.Properties.INCREMENT) >= 0
										&& nodeAttributes.getDoubleAttribute(edge.getTarget().getIdentifier(), Model.Properties.SHOWN_LEVEL) == 1)
									   || (edgeAttributes.getIntegerAttribute(edge.getIdentifier(), Model.Properties.INCREMENT) < 0
										&& nodeAttributes.getDoubleAttribute(edge.getTarget().getIdentifier(), Model.Properties.SHOWN_LEVEL) == 0)) {
								//concentration = 0;
								candidate = true;
							}
							break;
						case 2:
							String e1 = edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1"),
								   e2 = edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2");
							//System.err.println("Scenario 2, E1 = " + e1 + ", isActive? " + edgeAttributes.getBooleanAttribute(edge.getIdentifier(), Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E1") + ", level = " + nodeAttributes.getDoubleAttribute(e1, Model.Properties.SHOWN_LEVEL));
							if (((edgeAttributes.getBooleanAttribute(edge.getIdentifier(), Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E1") && nodeAttributes.getDoubleAttribute(e1, Model.Properties.SHOWN_LEVEL) == 0)
								|| (!edgeAttributes.getBooleanAttribute(edge.getIdentifier(), Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E1") && nodeAttributes.getDoubleAttribute(e1, Model.Properties.SHOWN_LEVEL) == 1))
								||
								((edgeAttributes.getBooleanAttribute(edge.getIdentifier(), Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E2") && nodeAttributes.getDoubleAttribute(e2, Model.Properties.SHOWN_LEVEL) == 0)
								|| (!edgeAttributes.getBooleanAttribute(edge.getIdentifier(), Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E2") && nodeAttributes.getDoubleAttribute(e2, Model.Properties.SHOWN_LEVEL) == 1))) {
								//concentration = 0;
								candidate = true;
							}
							break;
						default:
							candidate = false;
							break;
					}
					if (!candidate && convergingEdges.get(edge.getRootGraphIndex()) != null) {
						//If at least one edge is NOT a candidate for having 0 activityRatio, then all edges will have their activityRatio set as it comes from the result
						convergingEdges.get(edge.getRootGraphIndex()).first = false;
					}
					
					edgeAttributes.setAttribute(edge.getIdentifier(), Model.Properties.SHOWN_LEVEL, concentration);
				}
			}
			if (t != 0) { //At the initial time we have already done what was needed, i.e. remove the attribute
				CyNetwork network = Cytoscape.getCurrentNetwork();
				for (Pair<Boolean, Vector<Integer>> edgeGroup : convergingEdges.values()) {
					if (edgeGroup.first) { //All the edges of this group were still candidates at the end of the first cycle, so we will set all their activityRatios to 0
						for (Integer i : edgeGroup.second) {
							edgeAttributes.setAttribute(network.getEdge(i).getIdentifier(), Model.Properties.SHOWN_LEVEL, 0.0);
						}
						edgeGroup.first = false;
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);
		
		/* Unfortunately, it is not possible to set a background other than simply java.awt.Color... So, for the moment I have no idea on what to do in order to paint in the background image (for example, to add a legend for colors/shapes, or a counter for the current time, to better reflect the scale in the slider)
		BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g1 = img.createGraphics();
		g1.setPaint(java.awt.Color.RED.darker());
		g1.setBackground(java.awt.Color.WHITE);
		//g1.fill(new Rectangle2D.Float(0, 0, 1000, 1000));
		g1.drawString("aaaa", 50, 50);
		Cytoscape.getCurrentNetworkView().setBackgroundPaint(new TexturePaint(img, new Rectangle2D.Float(0, 0, 100, 100)));
		//new TexturePaint(img, new Rectangle2D.Float(0, 0, 1000, 1000))); //java.awt.Color.RED.darker()); 
		 */

		Cytoscape.getCurrentNetworkView().updateView();
	}
	
	/**
	 * When the graph scale changes (due to a zoom or axis range change),
	 * we change the min and max of the JSlider to adapt it to the graph area.
	 */
	@Override
	public void scaleChanged(Scale newScale) {
		this.minValueOnGraph = newScale.getMinX();
		this.maxValueOnGraph = newScale.getMaxX();
		this.scaleForConcentration = this.maxValueOnGraph / this.scale;
		
		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		DecimalFormat formatter = new DecimalFormat("0.###");
		int nLabels = 10;
		double graphWidth = this.maxValueOnGraph - this.minValueOnGraph;
		for (int i=0;i<=nLabels;i++) {
			double val = this.minValueOnGraph + 1.0 * i / nLabels * graphWidth;
			String valStr = formatter.format(val);
			labels.put((int)Math.round(1.0 * i / nLabels * slider.getMaximum()), new JLabel(valStr));
		}
		this.slider.setLabelTable(labels);
		stateChanged(null); //Note that we don't use that parameter, so I can also call the function with null
	}
	
	/**
	 * Save the simulation data to a file
	 * @param outputFile
	 * @param normalFile True if the file is user-chosen.
	 * Otherwise, we need to save also all saved network-related data (network id, image, properties)
	 */
	public void saveSimulationData(File outputFile, boolean normalFile) {
		if (isDifference) return; //We don't save the differences!
		try {
			if (normalFile) {
				FileOutputStream fOut = new FileOutputStream(outputFile);
				ObjectOutputStream out = new ObjectOutputStream(fOut);
				out.writeObject(model);
				out.writeObject(result);
				out.writeDouble(new Double(scale));
				out.writeObject(title);
				out.close();
			} else {
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
				org.w3c.dom.Node rootNode = doc.createElement("root"),
								 modelNode = doc.createElement("model"),
								 resultNode = doc.createElement("result"),
								 scaleNode = doc.createElement("scale"),
								 titleNode = doc.createElement("title"),
								 networkIdNode = doc.createElement("networkId"),
								 nodePropertiesNode = doc.createElement("nodeProperties"),
								 edgePropertiesNode = doc.createElement("edgeProperties");
				CDATASection modelValue = doc.createCDATASection(encodeObjectToBase64(model)),
							 resultValue = doc.createCDATASection(encodeObjectToBase64(result)),
							 nodePropsValue = doc.createCDATASection(encodeObjectToBase64(savedNodeAttributes)),
							 edgePropsValue = doc.createCDATASection(encodeObjectToBase64(savedEdgeAttributes));
				modelNode.appendChild(modelValue);
				rootNode.appendChild(modelNode);
				resultNode.appendChild(resultValue);
				rootNode.appendChild(resultNode);
				scaleNode.setTextContent(new Double(scale).toString());
				rootNode.appendChild(scaleNode);
				titleNode.setTextContent(title);
				rootNode.appendChild(titleNode);
				networkIdNode.setTextContent(savedNetwork.getIdentifier());
				rootNode.appendChild(networkIdNode);
				nodePropertiesNode.appendChild(nodePropsValue);
				rootNode.appendChild(nodePropertiesNode);
				edgePropertiesNode.appendChild(edgePropsValue);
				rootNode.appendChild(edgePropertiesNode);
				doc.appendChild(rootNode);
				Transformer tra = TransformerFactory.newInstance().newTransformer();
				tra.setOutputProperty(OutputKeys.INDENT, "yes");
				tra.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				tra.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				FileOutputStream fos = new FileOutputStream(outputFile);
				tra.transform(new DOMSource(doc), new StreamResult(fos));
				fos.flush();
				fos.close();
			}
		} catch (Exception ex) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			ex.printStackTrace(ps);
			JOptionPane.showMessageDialog(Cytoscape.getDesktop(), baos.toString(), "Error: " + ex, JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * Encode the given object to Base64, so that it can be included in a CDATA section in an xml file
	 * @param o The object to be encoded
	 * @return The encoded object
	 * @throws Exception Any exception that may be thrown by ByteArrayInputStream, ObjectOutputStream and Base64
	 */
	private static String encodeObjectToBase64(Object o) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.flush();
		String encodedObject = Base64.encode(baos.toByteArray());
		baos.close();
		return encodedObject;
	}
	
	/**
	 * Do exactly the opposite of encodeObjectToBase64
	 * @param encodedObject The encoded object as a CDATA text
	 * @return The decoded object
	 * @throws Exception
	 */
	private static Object decodeObjectFromBase64(String encodedObject) throws Exception {
		byte[] decodedModel = Base64.decode(encodedObject);
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decodedModel));
		Object o = ois.readObject();
		ois.close();
		return o;
	}
	
	
	/**
	 * Load the data needed for creating a InatResultPanel from a .sim file.
	 * @param inputFile The file name
	 * @param normalFile True if we are loading from an user-chosen file and not a cytoscape-associated file.
	 * In the latter case, we need also to load the saved network id, image and properties
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Heptuple<Model, SimpleLevelResult, Double, String, String, HashMap<String, HashMap<String, Object>>, HashMap<String, HashMap<String, Object>>> loadSimulationData(File inputFile, boolean normalFile) {
		try {
			Model model = null;
			SimpleLevelResult result = null;
			Double scale = null;
			String title = null;
			String networkId = null;
			HashMap<String, HashMap<String, Object>> nodeProperties = null,
													 edgeProperties = null;
			if (normalFile) {
				ObjectInputStream in = new ObjectInputStream(new FileInputStream(inputFile));
				model = (Model)in.readObject();
				result = (SimpleLevelResult)in.readObject();
				scale = in.readDouble();
				title = in.readObject().toString();
				in.close();
			} else {
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputFile);
				doc.getDocumentElement().normalize();
				NodeList children = doc.getFirstChild().getChildNodes(); //The first child is unique and is the "root" node
				for (int i=0;i<children.getLength();i++) {
					org.w3c.dom.Node n = children.item(i);
					String name = n.getNodeName();
					if (name.equals("model")) {
						model = (Model)decodeObjectFromBase64(n.getFirstChild().getTextContent());
					} else if (name.equals("result")) {
						result = (SimpleLevelResult)decodeObjectFromBase64(n.getFirstChild().getTextContent());
					} else if (name.equals("scale")) {
						scale = Double.parseDouble(n.getFirstChild().getTextContent());
					} else if (name.equals("title")) {
						title = n.getFirstChild().getTextContent();
					} else if (name.equals("networkId")) {
						networkId = n.getFirstChild().getTextContent();
					} else if (name.equals("nodeProperties")) {
						nodeProperties = (HashMap<String, HashMap<String, Object>>)decodeObjectFromBase64(n.getFirstChild().getTextContent());
					} else if (name.equals("edgeProperties")) {
						edgeProperties = (HashMap<String, HashMap<String, Object>>)decodeObjectFromBase64(n.getFirstChild().getTextContent());
					}
				}
			}
			return new Heptuple<Model, SimpleLevelResult, Double, String, String, HashMap<String, HashMap<String, Object>>, HashMap<String, HashMap<String, Object>>>(model, result, scale, title, networkId, nodeProperties, edgeProperties);
		} catch (Exception ex) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			ex.printStackTrace(ps);
			JOptionPane.showMessageDialog(Cytoscape.getDesktop(), baos.toString(), "Error: " + ex, JOptionPane.ERROR_MESSAGE);
		}
		return null;
	}
	
	
	public void closeResultsPanel(CytoPanel cytoPanel) {
		if (savedNetwork != null && countSessionChanges <= 1) { //We destroy network & vizmap only if we are still in "our" session. Otherwise, we would risk destroying a network from another session
			try {
				final VisualMappingManager vizMap = Cytoscape.getVisualMappingManager();
				CalculatorCatalog visualStyleCatalog = vizMap.getCalculatorCatalog();
				final String myVisualStyleName = InatPlugin.TAB_NAME + "_" + savedNetwork.getIdentifier();
				visualStyleCatalog.removeVisualStyle(myVisualStyleName);
				visualStyleCatalog.removeVisualStyle(myVisualStyleName + "_Diff");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
			try {
				Cytoscape.destroyNetwork(savedNetwork);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		cytoPanel.remove(container);
		allExistingPanels.remove(this);
	}
	
	private int countSessionChanges = 0;
	private JButton resetToThisNetwork = null;
	
	/**
	 * Add the InatResultPanel to the given Cytoscape panel
	 * @param cytoPanel
	 */
	public void addToPanel(final CytoPanel cytoPanel) {
		container = new JPanel(new BorderLayout(2, 2));
		container.add(this, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout()); //new GridLayout(2, 2, 2, 2));
		
		resetToThisNetwork = null;
		if (savedNetwork != null) {
			resetToThisNetwork = new JButton(new AbstractAction("Reset to here") {
				private static final long serialVersionUID = 7265495749842776073L;
				
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void actionPerformed(ActionEvent e) {
					if (savedNetwork == null) {
						JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "No network was actually saved, so no network will be restored.\n(Please report this as a bug)");
						return; //The button does nothing when it discovers that the session was changed and the network is not available anymore
					}
					//System.err.println("Nota che voglio restaurare una rete con " + savedNetwork.getNodeCount() + " nodi!");
					if (savedNetwork.getNodeCount() < 1 || countSessionChanges > 1) { //If there are no nodes, I am not deleting the whole network to replace it with something useless
						resetToThisNetwork.setEnabled(false);
						return;
					}
					CyNetwork net = Cytoscape.getCurrentNetwork();
					List<CyNode> nodes = net.nodesList();
					List<CyEdge> edges = net.edgesList();
					for (CyNode n : nodes) {
						net.removeNode(n.getRootGraphIndex(), false);
					}
					for (CyEdge ed : edges) {
						net.removeEdge(ed.getRootGraphIndex(), false);
					}
					net.appendNetwork(savedNetwork);
					//JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Ho restituito la rete con " + savedNetwork.getNodeCount() + " nodi e " + savedNetwork.getEdgeCount() + " edgi");
					CyNetworkView currentView = Cytoscape.getCurrentNetworkView();
					CyAttributes nodeAttr = Cytoscape.getNodeAttributes(),
								 edgeAttr = Cytoscape.getEdgeAttributes();
					nodes = net.nodesList();
					for (CyNode n : nodes) {
						if (savedNodeAttributes.containsKey(n.getIdentifier())) {
							HashMap<String, Object> m = savedNodeAttributes.get(n.getIdentifier());
							for (String k : m.keySet()) {
								Object o = m.get(k);
								if (o instanceof Boolean) {
									nodeAttr.setAttribute(n.getIdentifier(), k, (Boolean)o);
								} else if (o instanceof Double) {
									nodeAttr.setAttribute(n.getIdentifier(), k, (Double)o);
								} else if (o instanceof Float) {
									nodeAttr.setAttribute(n.getIdentifier(), k, new Double((Float)o));
								} else if (o instanceof Integer) {
									nodeAttr.setAttribute(n.getIdentifier(), k, (Integer)o);
								} else if (o instanceof List) {
									nodeAttr.setListAttribute(n.getIdentifier(), k, (List)o);
								} else if (o instanceof Map) {
									nodeAttr.setMapAttribute(n.getIdentifier(), k, (Map)o);
								} else if (o instanceof String) {
									nodeAttr.setAttribute(n.getIdentifier(), k, (String)o);
								}
							}
						}
					}
					edges = net.edgesList();
					for (CyEdge ed : edges) {
						if (savedEdgeAttributes.containsKey(ed.getIdentifier())) {
							HashMap<String, Object> m = savedEdgeAttributes.get(ed.getIdentifier());
							for (String k : m.keySet()) {
								Object o = m.get(k);
								if (o instanceof Boolean) {
									edgeAttr.setAttribute(ed.getIdentifier(), k, (Boolean)o);
								} else if (o instanceof Double) {
									edgeAttr.setAttribute(ed.getIdentifier(), k, (Double)o);
								} else if (o instanceof Float) {
									edgeAttr.setAttribute(ed.getIdentifier(), k, new Double((Float)o));
								} else if (o instanceof Integer) {
									edgeAttr.setAttribute(ed.getIdentifier(), k, (Integer)o);
								} else if (o instanceof List) {
									edgeAttr.setListAttribute(ed.getIdentifier(), k, (List)o);
								} else if (o instanceof Map) {
									edgeAttr.setMapAttribute(ed.getIdentifier(), k, (Map)o);
								} else if (o instanceof String) {
									edgeAttr.setAttribute(ed.getIdentifier(), k, (String)o);
								}
							}
						}
					}
					Iterator<NodeView> itNodes = currentView.getNodeViewsIterator();
					while (itNodes.hasNext()) {
						NodeView n = itNodes.next();
						n.setOffset(nodeAttr.getDoubleAttribute(n.getNode().getIdentifier(), PROP_POSITION_X), nodeAttr.getDoubleAttribute(n.getNode().getIdentifier(), PROP_POSITION_Y));
					}
					Iterator<EdgeView> itEdges = currentView.getEdgeViewsIterator();
					while (itEdges.hasNext()) {
						EdgeView ed = itEdges.next();
						Bend bend = ed.getBend();
						List<Double> handles = edgeAttr.getListAttribute(ed.getEdge().getIdentifier(), PROP_BEND_HANDLES);
						if (handles != null) {
							Iterator<Double> h = handles.iterator();
							while (h.hasNext()) {
								Double x = h.next(),
									   y = h.next();
								bend.addHandle(new Point2D.Double(x, y));
							}
							ed.setLineType(EdgeView.CURVED_LINES);
						}
					}
					Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);
				}
			});
			try {
				if (savedNetworkImage != null) {
					File tmpFile = File.createTempFile("ANIMOimg", ".png");
					tmpFile.deleteOnExit();
					ImageIO.write(savedNetworkImage, "png", tmpFile);
					resetToThisNetwork.setToolTipText("<html>Reset the network to the input that gave this result, i.e. this network:<br/><img src=\"file:" + tmpFile.getCanonicalPath() + "\"/></html>");
				} else {
					throw new Exception(); //Just to do the same when we have no image as when the image has problems
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				resetToThisNetwork.setToolTipText("Reset the network to the input that gave this result");
			} catch (Error er) {
				er.printStackTrace();
				resetToThisNetwork.setToolTipText("Reset the network to the input that gave this result");
			}
		}
		
		JButton close = new JButton(new AbstractAction("Close") {
			private static final long serialVersionUID = 4327349309742276633L;

			@Override
			public void actionPerformed(ActionEvent e) {
				closeResultsPanel(cytoPanel);
			}
		});
		
		JButton save = new JButton(new AbstractAction("Save simulation data...") {
			private static final long serialVersionUID = -2492923184151760584L;

			@Override
			public void actionPerformed(ActionEvent e) {
				String fileName = FileUtils.save(".sim", "ANIMO simulation data", Cytoscape.getDesktop());
				saveSimulationData(new File(fileName), true);
			}
		});
		
		JButton changeTitle = new JButton(new AbstractAction("Change title") {
			private static final long serialVersionUID = 7093059357198172376L;

			@Override
			public void actionPerformed(ActionEvent e) {
				String newTitle = JOptionPane.showInputDialog(Cytoscape.getDesktop(), "Please give a new title", title);
				if (newTitle == null) {
					return;
				}
				setTitle(newTitle);
			}
		});
		
		differenceButton = new JButton(START_DIFFERENCE);
		differenceButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				differenceButtonPressed(differenceButton.getText());
			}
		});
		
		buttons.add(changeTitle);
		if (resetToThisNetwork != null) {
			buttons.add(resetToThisNetwork);
		}
		buttons.add(differenceButton);
		if (!isDifference) { //The differences are not saved (for the moment)
			buttons.add(save);
		}
		buttons.add(close);
		container.add(buttons, BorderLayout.NORTH);
		
		
		if (cytoPanel.getState().equals(CytoPanelState.HIDE)) {
			cytoPanel.setState(CytoPanelState.DOCK); //We show the Results panel if it was hidden.
		}
		fCytoPanel = (CytoPanelImp)cytoPanel;
		cytoPanel.addCytoPanelListener(this);
		
		cytoPanel.add(this.title, container);
		this.myIndex = cytoPanel.getCytoPanelComponentCount() - 1;
		cytoPanel.setSelectedIndex(this.myIndex);
		resetDivider();
	}
	
	public void setTitle(String newTitle) {
		title = newTitle;
		int currentIdx = fCytoPanel.getSelectedIndex();
		fCytoPanel.remove(currentIdx);
		fCytoPanel.add(title, null, container, newTitle, currentIdx);
		fCytoPanel.setSelectedIndex(currentIdx);
		resetDivider();
	}
	
	public String getTitle() {
		return this.title;
	}
	
	private void differenceButtonPressed(String caption) {
		if (caption.equals(START_DIFFERENCE)) {
			differenceButton.setText(CANCEL_DIFFERENCE);
			differenceWith = this;
			for (InatResultPanel others : allExistingPanels) {
				if (others == this) continue;
				others.differenceButton.setText(END_DIFFERENCE);
			}
		} else if (caption.equals(END_DIFFERENCE)) {
			if (differenceWith != null) {
				Map<String, String> hisMapCytoscapeIDtoModelID = differenceWith.model.getMapCytoscapeIDtoReactantID();
				//System.err.println("La sua mappa da Cytoscape a Model ID: " + hisMapCytoscapeIDtoModelID);
				Map<String, String> myMapCytoscapeIDtoModelID = this.model.getMapCytoscapeIDtoReactantID();
				Map<String, String> myMapModelIDtoCytoscapeID = new HashMap<String, String>();
				//System.err.println("La mia mappa da Cytoscape a Model ID: " + myMapCytoscapeIDtoModelID);
				for (String k : myMapCytoscapeIDtoModelID.keySet()) {
					myMapModelIDtoCytoscapeID.put(myMapCytoscapeIDtoModelID.get(k), k);
				}
				//System.err.println("La mia mappa da Model a Cytoscape ID: " + myMapModelIDtoCytoscapeID);
				SimpleLevelResult diff = (SimpleLevelResult)this.result.difference(differenceWith.result, myMapModelIDtoCytoscapeID, hisMapCytoscapeIDtoModelID);
				if (diff.isEmpty()) {
					JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Error: empty difference. Please contact the developers and send them the current model,\nwith a reference to which simulations were used for the difference.");
					return;
				}
				InatResultPanel newPanel = new InatResultPanel(differenceWith.model, diff, differenceWith.scale, differenceWith.title + " - " + this.title, null);
				newPanel.isDifference = true;
				double maxY = Math.max(this.g.getScale().getMaxY(), differenceWith.g.getScale().getMaxY());
				Scale scale = newPanel.g.getScale();
				newPanel.g.setDrawArea((int)scale.getMinX(), (int)scale.getMaxX(), (int)-maxY, (int)maxY); //(int)scale.getMaxY());
				newPanel.vizMapName = InatPlugin.TAB_NAME + "_" + savedNetwork.getIdentifier() + "_Diff";
				if (fCytoPanel != null) {
					newPanel.addToPanel(fCytoPanel);
				}
				for (InatResultPanel panel : allExistingPanels) {
					panel.differenceButton.setText(START_DIFFERENCE);
				}
			}
		} else if (caption.equals(CANCEL_DIFFERENCE)) {
			differenceWith = null;
			for (InatResultPanel panel : allExistingPanels) {
				panel.differenceButton.setText(START_DIFFERENCE);
			}
		}
	}
	
	private int lastWidth;
	private void resetDivider() {
		JSplitPane par = (JSplitPane)(fCytoPanel.getParent());
		CyNetworkView p2 = Cytoscape.getCurrentNetworkView();
		int width = 0;
		if (p2 != null) {
			try {
				width += Cytoscape.getDesktop().getNetworkViewManager().getInternalFrame(p2).getWidth(); //There may be null pointers here (e.g. when you have already closed all network windows and are attempting to close a results tab)
			} catch (Exception ex) {
				width += 0;
			}
		}
		if (width == 0) {
			width = lastWidth;
		}
		par.setDividerLocation(width);
		lastWidth = width;
	}
	
	private CytoPanelImp fCytoPanel;
	@Override
	public void onComponentAdded(int arg0) {
		resetDivider();
	}

	@Override
	public void onComponentRemoved(int arg0) {
		resetDivider();
	}

	@Override
	public void onComponentSelected(int arg0) {
		if (arg0 == myIndex) {
			VisualMappingManager vizMap = Cytoscape.getVisualMappingManager();
			if (vizMapName != null) {
				//CalculatorCatalog visualStyleCatalog = vizMap.getCalculatorCatalog();
				vizMap.setVisualStyle(vizMapName);
			} else {
				vizMap.setVisualStyle("default");
			}
			stateChanged(null); //We must do this to make sure that the visual mapping is correctly applied, otherwise Cytoscape will not notice a possibly different scale on the mapping values (e.g. [-1.0, 1.0] instead of [0.0, 1.0])
		}
		resetDivider();
	}

	@Override
	public void onStateChange(CytoPanelState arg0) {
		resetDivider();
	}
}
