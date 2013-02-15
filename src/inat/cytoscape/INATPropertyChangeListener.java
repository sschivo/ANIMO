package inat.cytoscape;

import giny.model.Edge;
import giny.model.Node;
import giny.view.EdgeView;
import giny.view.NodeView;
import inat.model.Model;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.editor.CytoscapeEditor;
import cytoscape.editor.CytoscapeEditorManager;
import cytoscape.view.CyNetworkView;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.visual.ArrowShape;
import cytoscape.visual.CalculatorCatalog;
import cytoscape.visual.EdgeAppearanceCalculator;
import cytoscape.visual.GlobalAppearanceCalculator;
import cytoscape.visual.NodeAppearanceCalculator;
import cytoscape.visual.NodeShape;
import cytoscape.visual.VisualMappingManager;
import cytoscape.visual.VisualPropertyDependency.Definition;
import cytoscape.visual.VisualPropertyType;
import cytoscape.visual.VisualStyle;
import cytoscape.visual.calculators.BasicCalculator;
import cytoscape.visual.calculators.Calculator;
import cytoscape.visual.calculators.GenericNodeCustomGraphicCalculator;
import cytoscape.visual.mappings.BoundaryRangeValues;
import cytoscape.visual.mappings.ContinuousMapping;
import cytoscape.visual.mappings.DiscreteMapping;
import cytoscape.visual.mappings.LinearNumberToColorInterpolator;
import cytoscape.visual.mappings.LinearNumberToNumberInterpolator;
import cytoscape.visual.mappings.ObjectMapping;
import cytoscape.visual.mappings.PassThroughMapping;
import cytoscape.visual.mappings.continuous.ContinuousMappingPoint;
import ding.view.DGraphView;
import ding.view.EdgeContextMenuListener;
import ding.view.NodeContextMenuListener;

public class INATPropertyChangeListener implements PropertyChangeListener {
	
	private int currentEdgeNumber = -1, currentNodeNumber = -1;
	private Object[] edgesArray = null;
	private ColorsLegend legendColors;
	private ShapesLegend legendShapes;

	public INATPropertyChangeListener(ColorsLegend legendColors, ShapesLegend legendShapes) {
		this.legendColors = legendColors;
		this.legendShapes = legendShapes;
	}

	/**
	 * Add the right-click menus for nodes (reactants) and edges (reactions)
	 * Menus are: Edit, Enable/disable and Plotted/hidden (last one only for nodes)
	 */
	private void addMenus() {
		Cytoscape.getCurrentNetworkView().addNodeContextMenuListener(new NodeContextMenuListener() {
			
			@Override
			public void addNodeContextMenuItems(final NodeView nodeView, JPopupMenu menu) {
				if (menu != null) {
					menu.add(new AbstractAction("[ANIMO] Edit reactant...") {
						private static final long serialVersionUID = 6233177439944893232L;

						@Override
						public void actionPerformed(ActionEvent e) {
							NodeDialog dialog = new NodeDialog(nodeView.getNode());
							dialog.pack();
							dialog.setLocationRelativeTo(Cytoscape.getDesktop());
							dialog.setVisible(true);
						}
					});
					
					menu.add(new AbstractAction("[ANIMO] Enable/disable") {
						private static final long serialVersionUID = 2579035100338148305L;

						@Override
						public void actionPerformed(ActionEvent e) {
							//CyNetwork network = Cytoscape.getCurrentNetwork();
							CyNetworkView view = Cytoscape.getCurrentNetworkView();
							CyAttributes nodeAttr = Cytoscape.getNodeAttributes();
							//CyAttributes edgeAttr = Cytoscape.getEdgeAttributes();
							for (@SuppressWarnings("unchecked")
							Iterator<NodeView> i = view.getSelectedNodes().iterator(); i.hasNext(); ) {
								NodeView nView = i.next();
								CyNode node = (CyNode)nView.getNode();
								boolean status;
								if (!nodeAttr.hasAttribute(node.getIdentifier(), Model.Properties.ENABLED)) {
									status = false;
								} else {
									status = !nodeAttr.getBooleanAttribute(node.getIdentifier(), Model.Properties.ENABLED);
								}
								nodeAttr.setAttribute(node.getIdentifier(), Model.Properties.ENABLED, status);
								/*int [] adjacentEdges = network.getAdjacentEdgeIndicesArray(node.getRootGraphIndex(), true, true, true);
								for (int edgeIdx : adjacentEdges) {
									CyEdge edge = (CyEdge)view.getEdgeView(edgeIdx).getEdge();
									edgeAttr.setAttribute(edge.getIdentifier(), Model.Properties.ENABLED, status);
								}*/
							}
							if (view.getSelectedNodes().isEmpty()) { //if the user wanted to change only one node (i.e. right click on a node without first selecting one), here we go
								Node node = nodeView.getNode();
								boolean status;
								if (!nodeAttr.hasAttribute(node.getIdentifier(), Model.Properties.ENABLED)) {
									status = false;
								} else {
									status = !nodeAttr.getBooleanAttribute(node.getIdentifier(), Model.Properties.ENABLED);
								}
								nodeAttr.setAttribute(node.getIdentifier(), Model.Properties.ENABLED, status);
								/*int [] adjacentEdges = network.getAdjacentEdgeIndicesArray(node.getRootGraphIndex(), true, true, true);
								for (int edgeIdx : adjacentEdges) {
									CyEdge edge = (CyEdge)view.getEdgeView(edgeIdx).getEdge();
									edgeAttr.setAttribute(edge.getIdentifier(), Model.Properties.ENABLED, status);
								}*/
							}
							/*//In order to keep the model consistent, we disable all edges coming from (or going into) disabled nodes
							for (int i : network.getEdgeIndicesArray()) {
								CyEdge edge = (CyEdge)view.getEdgeView(i).getEdge();
								CyNode source = (CyNode)edge.getSource(),
									   target = (CyNode)edge.getTarget();
								if ((nodeAttr.hasAttribute(source.getIdentifier(), Model.Properties.ENABLED) && !nodeAttr.getBooleanAttribute(source.getIdentifier(), Model.Properties.ENABLED))
									|| (nodeAttr.hasAttribute(target.getIdentifier(), Model.Properties.ENABLED) && !nodeAttr.getBooleanAttribute(target.getIdentifier(), Model.Properties.ENABLED))) {
									edgeAttr.setAttribute(edge.getIdentifier(), Model.Properties.ENABLED, false);
								}
							}*/
							Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);
						}
					});
					
					menu.add(new AbstractAction("[ANIMO] Plot/hide") {

						private static final long serialVersionUID = -4264583436246699628L;

						@SuppressWarnings("unchecked")
						@Override
						public void actionPerformed(ActionEvent e) {
							CyNetworkView view = Cytoscape.getCurrentNetworkView();
							CyAttributes nodeAttr = Cytoscape.getNodeAttributes();
							for (Iterator<NodeView> i = view.getSelectedNodes().iterator(); i.hasNext(); ) {
								NodeView nView = i.next();
								CyNode node = (CyNode)nView.getNode();
								boolean status;
								if (!nodeAttr.hasAttribute(node.getIdentifier(), Model.Properties.PLOTTED)) {
									status = false;
								} else {
									status = !nodeAttr.getBooleanAttribute(node.getIdentifier(), Model.Properties.PLOTTED);
								}
								nodeAttr.setAttribute(node.getIdentifier(), Model.Properties.PLOTTED, status);
							}
							if (view.getSelectedNodes().isEmpty()) { //if the user wanted to change only one node (i.e. right click on a node without first selecting one), here we go
								Node node = nodeView.getNode();
								boolean status;
								if (!nodeAttr.hasAttribute(node.getIdentifier(), Model.Properties.PLOTTED)) {
									status = false;
								} else {
									status = !nodeAttr.getBooleanAttribute(node.getIdentifier(), Model.Properties.PLOTTED);
								}
								nodeAttr.setAttribute(node.getIdentifier(), Model.Properties.PLOTTED, status);
							}
							Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);
						}
						
					});
				}
			}
		});
		Cytoscape.getCurrentNetworkView().addEdgeContextMenuListener(new EdgeContextMenuListener() {
			
			@Override
			public void addEdgeContextMenuItems(final EdgeView edgeView, JPopupMenu menu) {
				if (menu != null) {
					menu.add(new AbstractAction("[ANIMO] Edit reaction...") {
						private static final long serialVersionUID = -5725775462053708399L;

						@Override
						public void actionPerformed(ActionEvent e) {
							EdgeDialog dialog = new EdgeDialog(edgeView.getEdge());
							dialog.pack();
							dialog.setLocationRelativeTo(Cytoscape.getDesktop());
							dialog.setVisible(true);
						}
					});
					
					menu.add(new AbstractAction("[ANIMO] Enable/disable") {

						private static final long serialVersionUID = -1078261166495178010L;

						@Override
						public void actionPerformed(ActionEvent e) {
							CyNetworkView view = Cytoscape.getCurrentNetworkView();
							CyAttributes edgeAttr = Cytoscape.getEdgeAttributes();
							for (@SuppressWarnings("unchecked")
							Iterator<EdgeView> i = view.getSelectedEdges().iterator(); i.hasNext(); ) {
								EdgeView eView = (EdgeView)i.next();
								CyEdge edge = (CyEdge)eView.getEdge();
								boolean status;
								if (!edgeAttr.hasAttribute(edge.getIdentifier(), Model.Properties.ENABLED)) {
									status = false;
								} else {
									status = !edgeAttr.getBooleanAttribute(edge.getIdentifier(), Model.Properties.ENABLED);
								}
								edgeAttr.setAttribute(edge.getIdentifier(), Model.Properties.ENABLED, status);
							}
							if (view.getSelectedEdges().isEmpty()) { //if the user wanted to change only one edge (i.e. right click on an edge without first selecting one), here we go
								Edge edge = edgeView.getEdge();
								boolean status;
								if (!edgeAttr.hasAttribute(edge.getIdentifier(), Model.Properties.ENABLED)) {
									status = false;
								} else {
									status = !edgeAttr.getBooleanAttribute(edge.getIdentifier(), Model.Properties.ENABLED);
								}
								edgeAttr.setAttribute(edge.getIdentifier(), Model.Properties.ENABLED, status);
							}
							Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);
						}
						
					});
				}
			}
		});
	}
	
	
	/**
	 * Add the visual mappings to visually enhance the user interface (nodes colors, shapes, arrows etc)
	 */
	private void addVisualMappings() {
		CyNetworkView currentNetworkView = Cytoscape.getCurrentNetworkView();
		final VisualMappingManager vizMap = Cytoscape.getVisualMappingManager();
		CalculatorCatalog visualStyleCatalog = vizMap.getCalculatorCatalog();
		VisualStyle visualStyle = null;
		final String myVisualStyleName = InatPlugin.TAB_NAME + "_" + currentNetworkView.getIdentifier();
		//System.err.println("\n\n\n\n\n\n\n\n\n\n\nDEVO AGIRE PER LA RETE " + currentNetworkView.getIdentifier());
		if (visualStyleCatalog.getVisualStyle(myVisualStyleName) == null) {
			//System.err.println("Devo aggiungere il mapping per " + myVisualStyleName);
			//try {
				//visualStyle = (VisualStyle)visualStyleCatalog.getVisualStyle("default").clone(); //(VisualStyle)vizMap.getVisualStyle().clone();
				//visualStyle.setName(myVisualStyleName);
			visualStyle = new VisualStyle(visualStyleCatalog.getVisualStyle("default"), myVisualStyleName);
			//} catch (CloneNotSupportedException ex) {
				//I'm sure that VisualStyle supports cloning, so no exception will be thrown here
			//}
			visualStyleCatalog.addVisualStyle(visualStyle);
			
			NodeAppearanceCalculator nac = visualStyle.getNodeAppearanceCalculator();
			EdgeAppearanceCalculator eac = visualStyle.getEdgeAppearanceCalculator();
			GlobalAppearanceCalculator gac = visualStyle.getGlobalAppearanceCalculator();
			visualStyle.getDependency().set(Definition.NODE_SIZE_LOCKED, false);
			
			gac.setDefaultBackgroundColor(Color.WHITE);
			Color selectionColor = new Color(102, 102, 255);
			gac.setDefaultNodeSelectionColor(selectionColor);
			gac.setDefaultEdgeSelectionColor(selectionColor);
	
			/*DiscreteMapping mapping = new DiscreteMapping(Color.class, Model.Properties.ENABLED);
			mapping.putMapValue(false, Color.BLACK);
			mapping.putMapValue(true, Color.WHITE);
			Calculator calco = new GenericNodeCustomGraphicCalculator("Mapping for enabled and disabled nodes (label color)", mapping, VisualPropertyType.NODE_LABEL_COLOR);
			nac.setCalculator(calco);*/
			
			DiscreteMapping mapping = new DiscreteMapping(Float.class, Model.Properties.ENABLED);
			mapping.putMapValue(false, 50.0f);
			mapping.putMapValue(true, 255.0f);
			Calculator calco = new GenericNodeCustomGraphicCalculator(myVisualStyleName + "Mapping_for_enabled_and_disabled_nodes_(node_opacity)", mapping, VisualPropertyType.NODE_OPACITY);
			nac.setCalculator(calco);
			
			mapping = new DiscreteMapping(Float.class, Model.Properties.ENABLED);
			mapping.putMapValue(false, 60.0f);
			mapping.putMapValue(true, 255.0f);
			calco = new GenericNodeCustomGraphicCalculator(myVisualStyleName + "Mapping_for_enabled_and_disabled_nodes_(node_border_opacity)", mapping, VisualPropertyType.NODE_BORDER_OPACITY);
			nac.setCalculator(calco);
			
			mapping = new DiscreteMapping(Float.class, Model.Properties.ENABLED);
			mapping.putMapValue(false, 50.0f);
			mapping.putMapValue(true, 255.0f);
			calco = new BasicCalculator(myVisualStyleName + "Mapping_for_enabled_and_disabled_edges_(edge_opacity)", mapping, VisualPropertyType.EDGE_OPACITY);
			eac.setCalculator(calco);
			
			calco = new BasicCalculator(myVisualStyleName + "Mapping_for_enabled_and_disabled_edges_(edge_target_arrow_opacity)", mapping, VisualPropertyType.EDGE_TGTARROW_OPACITY);
			eac.setCalculator(calco);
	
			mapping = new DiscreteMapping(Float.class, Model.Properties.ENABLED);
			mapping.putMapValue(false, 3.0f);
			mapping.putMapValue(true, 6.0f);
			calco = new GenericNodeCustomGraphicCalculator(myVisualStyleName + "Mapping_to_show_thicker_borders_for_enabled_nodes", mapping, VisualPropertyType.NODE_LINE_WIDTH);
			nac.setCalculator(calco);
			
			mapping = new DiscreteMapping(Color.class, Model.Properties.PLOTTED);
			mapping.putMapValue(false, Color.DARK_GRAY);
			mapping.putMapValue(true, Color.BLUE);
			calco = new GenericNodeCustomGraphicCalculator(myVisualStyleName + "Mapping_to_highlighting_plotted_nodes_with_a_blue_border", mapping, VisualPropertyType.NODE_BORDER_COLOR);
			nac.setCalculator(calco);
			
			mapping = new DiscreteMapping(ArrowShape.class, Model.Properties.INCREMENT);
			mapping.putMapValue(-1, ArrowShape.T);
			mapping.putMapValue(1, ArrowShape.ARROW);
			calco = new BasicCalculator(myVisualStyleName + "Mapping_for_arrow_target_shape", mapping, VisualPropertyType.EDGE_TGTARROW_SHAPE);
			eac.setCalculator(calco);
			
			ContinuousMapping mapLineWidth = new ContinuousMapping(Float.class, Model.Properties.SHOWN_LEVEL);
			Float lowerBoundWidth = 2.0f,
				  upperBoundWidth = 10.0f;
			mapLineWidth.addPoint(0.0, new BoundaryRangeValues(lowerBoundWidth, lowerBoundWidth, lowerBoundWidth));
			mapLineWidth.addPoint(1.0, new BoundaryRangeValues(upperBoundWidth, upperBoundWidth, upperBoundWidth));
			mapLineWidth.setInterpolator(new LinearNumberToNumberInterpolator());
			calco = new BasicCalculator(myVisualStyleName + "Mapping_for_edge_width_from_reaction_activity_(speed)", mapLineWidth, VisualPropertyType.EDGE_LINE_WIDTH);
			eac.setCalculator(calco);
			
			mapping = new DiscreteMapping(NodeShape.class, Model.Properties.MOLECULE_TYPE);
			mapping.putMapValue(Model.Properties.TYPE_CYTOKINE, NodeShape.RECT);
			mapping.putMapValue(Model.Properties.TYPE_RECEPTOR, NodeShape.ELLIPSE);
			mapping.putMapValue(Model.Properties.TYPE_KINASE, NodeShape.ELLIPSE);
			mapping.putMapValue(Model.Properties.TYPE_PHOSPHATASE, NodeShape.DIAMOND);
			mapping.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, NodeShape.ELLIPSE);
			mapping.putMapValue(Model.Properties.TYPE_OTHER, NodeShape.RECT);
			//I purposedly omit TYPE_OTHER, because I want it to stay on the default setting
			calco = new BasicCalculator(myVisualStyleName + "Mapping_node_shape_from_molecule_type", mapping, VisualPropertyType.NODE_SHAPE);
			nac.setCalculator(calco);
			final DiscreteMapping shapesMap = mapping;
			
			/*mapping = new DiscreteMapping(Float.class, Model.Properties.MOLECULE_TYPE);
			mapping.putMapValue(Model.Properties.TYPE_CYTOKINE, 60.0f);
			mapping.putMapValue(Model.Properties.TYPE_RECEPTOR, 50.0f);
			mapping.putMapValue(Model.Properties.TYPE_KINASE, 55.0f);
			mapping.putMapValue(Model.Properties.TYPE_PHOSPHATASE, 55.0f);
			mapping.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, 50.0f);
			calco = new BasicCalculator("Mapping node shape size from molecule type", mapping, VisualPropertyType.NODE_SIZE);
			nac.setCalculator(calco);*/
			
			mapping = new DiscreteMapping(Float.class, Model.Properties.MOLECULE_TYPE);
			mapping.putMapValue(Model.Properties.TYPE_CYTOKINE, 50.0f);
			mapping.putMapValue(Model.Properties.TYPE_RECEPTOR, 45.0f);
			mapping.putMapValue(Model.Properties.TYPE_KINASE, 55.0f);
			mapping.putMapValue(Model.Properties.TYPE_PHOSPHATASE, 55.0f);
			mapping.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, 60.0f);
			mapping.putMapValue(Model.Properties.TYPE_OTHER, 60.0f);
			calco = new BasicCalculator(myVisualStyleName + "Mapping_node_shape_width_from_molecule_type", mapping, VisualPropertyType.NODE_WIDTH);
			nac.setCalculator(calco);
			final DiscreteMapping widthsMap = mapping;
			
			mapping = new DiscreteMapping(Float.class, Model.Properties.MOLECULE_TYPE);
			mapping.putMapValue(Model.Properties.TYPE_CYTOKINE, 50.0f);
			mapping.putMapValue(Model.Properties.TYPE_RECEPTOR, 65.0f);
			mapping.putMapValue(Model.Properties.TYPE_KINASE, 55.0f);
			mapping.putMapValue(Model.Properties.TYPE_PHOSPHATASE, 55.0f);
			mapping.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, 40.0f);
			mapping.putMapValue(Model.Properties.TYPE_OTHER, 35.0f);
			calco = new BasicCalculator(myVisualStyleName + "Mapping_node_shape_height_from_molecule_type", mapping, VisualPropertyType.NODE_HEIGHT);
			nac.setCalculator(calco);
			final DiscreteMapping heightsMap = mapping;
//			shapesMap.addChangeListener(new ChangeListener() {
//				@Override
//				public void stateChanged(ChangeEvent e) {
//					legendShapes.setParameters(shapesMap, widthsMap, heightsMap);
//				}
//			});
//			widthsMap.addChangeListener(new ChangeListener() {
//				@Override
//				public void stateChanged(ChangeEvent e) {
//					legendShapes.setParameters(shapesMap, widthsMap, heightsMap);
//				}
//			});
//			heightsMap.addChangeListener(new ChangeListener() {
//				@Override
//				public void stateChanged(ChangeEvent e) {
//					legendShapes.setParameters(shapesMap, widthsMap, heightsMap);
//				}
//			});
			
			/*mapping = new DiscreteMapping(Color.class, Model.Properties.MOLECULE_TYPE);
			mapping.putMapValue(Model.Properties.TYPE_RECEPTOR, Color.BLACK);
			calco = new BasicCalculator("Mapping node label color from molecule type", mapping, VisualPropertyType.NODE_LABEL_COLOR);
			nac.setCalculator(calco);*/
			
			PassThroughMapping mp = new PassThroughMapping(String.class, Model.Properties.CANONICAL_NAME);
			calco = new BasicCalculator(myVisualStyleName + "Mapping_for_node_label", mp, VisualPropertyType.NODE_LABEL);
			nac.setCalculator(calco);
			
			final ContinuousMapping mc = new ContinuousMapping(Color.class, Model.Properties.SHOWN_LEVEL);
			Color lowerBound = new Color(204, 0, 0), //Color.RED.darker(),
			  middleBound = new Color(255, 204, 0), //Color.YELLOW.darker(),
			  upperBound = new Color(0, 204, 0); //Color.GREEN.darker();
			mc.addPoint(0.0, new BoundaryRangeValues(lowerBound, lowerBound, lowerBound));
			mc.addPoint(0.5, new BoundaryRangeValues(middleBound, middleBound, middleBound));
			mc.addPoint(1.0, new BoundaryRangeValues(upperBound, upperBound, upperBound));
			/*Color lowerBound = Color.BLACK,
				  midLowerBound = new Color(204, 0, 0),
				  midUpperBound = new Color(255, 255, 0),
				  upperBound = Color.WHITE;
			mc.addPoint(0.0, new BoundaryRangeValues(lowerBound, lowerBound, lowerBound));
			mc.addPoint(0.33333333, new BoundaryRangeValues(midLowerBound, midLowerBound, midLowerBound));
			mc.addPoint(0.66666666, new BoundaryRangeValues(midUpperBound, midUpperBound, midUpperBound));
			mc.addPoint(1.0, new BoundaryRangeValues(upperBound, upperBound, upperBound));*/
			/*Color lowerBound = new Color(204, 0, 0),
				  upperBound = new Color(255, 204, 0);
			mc.addPoint(0.0, new BoundaryRangeValues(lowerBound, lowerBound, lowerBound));
			mc.addPoint(1.0, new BoundaryRangeValues(upperBound, upperBound, upperBound));*/
			mc.setInterpolator(new LinearNumberToColorInterpolator());
			calco = new GenericNodeCustomGraphicCalculator(myVisualStyleName + "Mapping_for_the_current_activity_level_of_nodes", mc, VisualPropertyType.NODE_FILL_COLOR);
//			ChangeListener listener = new ChangeListener() {
//				@Override
//				public void stateChanged(ChangeEvent e) {
//					VisualMappingManager vizMap = Cytoscape.getVisualMappingManager();
//					VisualStyle visualStyle = vizMap.getVisualStyle();
//					NodeAppearanceCalculator nac = visualStyle.getNodeAppearanceCalculator();
//					Vector<ObjectMapping> mappings = nac.getCalculator(VisualPropertyType.NODE_FILL_COLOR).getMappings();
//					for (ObjectMapping om : mappings) {
//						if (!(om instanceof ContinuousMapping)) continue;
//						ContinuousMapping mapping = (ContinuousMapping)om;
//						List<ContinuousMappingPoint> points = mapping.getAllPoints();
//						float fractions[] = new float[points.size()];
//						Color colors[] = new Color[points.size()];
//						
//						int i = 0;
//						for (ContinuousMappingPoint point : points) {
//							fractions[fractions.length - 1 - i] = 1 - (float)point.getValue().doubleValue();
//							colors[colors.length - 1 - i] = (Color)point.getRange().equalValue;
//							i++;
//						}
//						legendColors.setParameters(fractions, colors);
//					}
//				}
//			};
//			calco.addChangeListener(listener);
			nac.setCalculator(calco);
//			mc.addChangeListener(listener);
			
			
			//legendColors.setParameters(new float[]{1 - 1, 1 - 2/3.0f, 1 - 1/3.0f, 1 - 0}, new Color[]{Color.WHITE, new Color(255, 255, 0), new Color(204, 0, 0), Color.BLACK});
			legendColors.setParameters(new float[]{1 - 1, 1 - 0.5f, 1 - 0/*1 - 1, 1 - 0*/}, new Color[]{new Color(0, 204, 0), new Color(255, 204, 0), new Color(204, 0, 0)/*new Color(255, 204, 0), new Color(204, 0, 0)*/});
			
			legendShapes.setParameters(shapesMap, widthsMap, heightsMap);
			
			VisualPropertyType.NODE_BORDER_COLOR.setDefault(visualStyle, Color.BLUE);//Color.DARK_GRAY);
			VisualPropertyType.NODE_FILL_COLOR.setDefault(visualStyle, Color.RED);
			VisualPropertyType.NODE_LABEL_COLOR.setDefault(visualStyle, Color.BLACK); //new Color(102, 102, 255));//Color.WHITE);
			VisualPropertyType.NODE_FONT_SIZE.setDefault(visualStyle, 14);
			VisualPropertyType.NODE_LINE_WIDTH.setDefault(visualStyle, 6.0f);
			VisualPropertyType.NODE_SHAPE.setDefault(visualStyle, NodeShape.RECT);
			VisualPropertyType.NODE_SIZE.setDefault(visualStyle, 50.0f);
			VisualPropertyType.NODE_WIDTH.setDefault(visualStyle, 60.0f);
			VisualPropertyType.NODE_HEIGHT.setDefault(visualStyle, 35.0f);
			VisualPropertyType.EDGE_LINE_WIDTH.setDefault(visualStyle, 4.0f);
			VisualPropertyType.EDGE_COLOR.setDefault(visualStyle, Color.BLACK);
			VisualPropertyType.EDGE_TGTARROW_COLOR.setDefault(visualStyle, Color.BLACK);
			
			//Recompute the activityRatio property for all nodes, to make sure that it exists
			CyNetwork network = Cytoscape.getCurrentNetwork();
			if (network != null) {
				CyAttributes nodeAttr = Cytoscape.getNodeAttributes(),
							 networkAttr = Cytoscape.getNetworkAttributes();
				for (int i : network.getNodeIndicesArray()) {
					Node n = network.getNode(i);
					double level = 0;
					int nLevels = 0;
					if (nodeAttr.hasAttribute(n.getIdentifier(), Model.Properties.INITIAL_LEVEL)) {
						level = nodeAttr.getIntegerAttribute(n.getIdentifier(), Model.Properties.INITIAL_LEVEL);
					} else {
						level = 0;
					}
					if (nodeAttr.hasAttribute(n.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS)) {
						nLevels = nodeAttr.getIntegerAttribute(n.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS);
					} else if (networkAttr.hasAttribute(network.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS)) {
						nLevels = networkAttr.getIntegerAttribute(network.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS);
					} else {
						nLevels = 1;
					}
					nodeAttr.setAttribute(n.getIdentifier(), Model.Properties.SHOWN_LEVEL, level / nLevels);
				}
				
				Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);
			}
			
			visualStyleCatalog.removeVisualStyle(myVisualStyleName); //Why should I do this to get things working?????????
			VisualStyle denovo = new VisualStyle(visualStyle, myVisualStyleName);
			visualStyleCatalog.addVisualStyle(denovo);
			
			currentNetworkView.setVisualStyle(visualStyle.getName());
			vizMap.setVisualStyle(visualStyle);
			currentNetworkView.redrawGraph(true, true);
			
			/*visualStyleCatalog.removeVisualStyle("default"); //So that bastard Cytoscape can try how much he wants: now the default style is MY style!
			VisualStyle newDefault = null;
			//try {
				//newDefault = (VisualStyle)visualStyle.clone();
				//newDefault.setName("default");
			newDefault = new VisualStyle(visualStyle, "default");
			//} catch (CloneNotSupportedException ex) {
				//as usual
			//}
			visualStyleCatalog.addVisualStyle(newDefault);*/
			
			VisualStyle differenceVisualStyle = null;
			//try {
				//differenceVisualStyle = (VisualStyle)visualStyle.clone();
			differenceVisualStyle = new VisualStyle(visualStyle, InatPlugin.TAB_NAME + "_" + currentNetworkView.getIdentifier() + "_Diff");
			//} catch (CloneNotSupportedException ex) {
				//Again, VisualStyle supports cloning, so no problem here
			//}
			//differenceVisualStyle.setName(InatPlugin.TAB_NAME + "_" + currentNetworkView.getIdentifier() + "_Diff");
			nac = differenceVisualStyle.getNodeAppearanceCalculator();
			nac.removeCalculator(VisualPropertyType.NODE_FILL_COLOR);
			Color lowerBound1 = new Color(204, 0, 0),
				  middleBound1 = new Color(255, 255, 255),
				  upperBound1 = new Color(0, 204, 0);
			ContinuousMapping mcDiff = new ContinuousMapping(Color.class, Model.Properties.SHOWN_LEVEL);
			mcDiff.addPoint(-1.0, new BoundaryRangeValues(lowerBound1, lowerBound1, lowerBound1));
			mcDiff.addPoint(0.0, new BoundaryRangeValues(middleBound1, middleBound1, middleBound1));
			mcDiff.addPoint(1.0, new BoundaryRangeValues(upperBound1, upperBound1, upperBound1));
			mcDiff.setInterpolator(new LinearNumberToColorInterpolator());
			calco = new GenericNodeCustomGraphicCalculator(differenceVisualStyle.getName() + "Mapping_for_the_current_activity_level_of_nodes_(difference_of_activity)", mcDiff, VisualPropertyType.NODE_FILL_COLOR);
			nac.setCalculator(calco);
			if (visualStyleCatalog.getVisualStyle(differenceVisualStyle.getName()) == null) {
				visualStyleCatalog.addVisualStyle(differenceVisualStyle);
			}
		}
		//System.err.println("Il mapping per " + myVisualStyleName + " esiste di sicuro ora, quindi lo uso");
		VisualStyle myVisualStyle = visualStyleCatalog.getVisualStyle(myVisualStyleName);
		currentNetworkView.setVisualStyle(myVisualStyleName);
		vizMap.setVisualStyle(myVisualStyleName);
		currentNetworkView.redrawGraph(true, true);
		visualStyleCatalog.removeVisualStyle("default");
		visualStyleCatalog.addVisualStyle(new VisualStyle(myVisualStyle, "default"));
		
		NodeAppearanceCalculator calco = myVisualStyle.getNodeAppearanceCalculator();
		try {
			ShapesListener shapesListener = getShapesListener();
			calco.getCalculator(VisualPropertyType.NODE_SHAPE).addChangeListener(shapesListener);
			calco.getCalculator(VisualPropertyType.NODE_WIDTH).addChangeListener(shapesListener);
			calco.getCalculator(VisualPropertyType.NODE_HEIGHT).addChangeListener(shapesListener);
			//shapesListener.stateChanged(new ChangeEvent(calco));
		} catch (ClassCastException ex) {
			//Just to avoid dying abruptly if the user changed the mapping to something else and we cannot represent it as a type->shape discrete mapping anymore
		}
		try {
			final Calculator colorCalculator = calco.getCalculator(VisualPropertyType.NODE_FILL_COLOR);
			final ContinuousMapping colorsMapping = (ContinuousMapping)colorCalculator.getMapping(0);
			ColorsListener colorsListener = getColorsListener();
			colorCalculator.addChangeListener(colorsListener);
			colorsMapping.addChangeListener(colorsListener);
			colorsMapping.fireStateChanged();
			vizMap.addChangeListener(colorsListener);
		} catch (ClassCastException ex) {
			//as before: avoid possible problems in case the mapping was changed to something not Continuous
		}
	}
	
	
	/**
	 * Add the listeners for double click on nodes and edges
	 * that allows to open the properties dialog.
	 * Also listen for Ctrl-click for adding nodes/edges. 
	 */
	private void addDblClckListener() {
		CyNetworkView view = Cytoscape.getCurrentNetworkView();
		((DGraphView)view).getCanvas().addMouseListener(new MouseAdapter() {
			private boolean drawingEdge = false;
			private Node firstNode = null;
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					DGraphView view = (DGraphView)Cytoscape.getCurrentNetworkView();
					NodeView nv = view.getPickedNodeView(e.getPoint());
					if (nv != null) {
						CyNode node = (CyNode)nv.getNode();
						NodeDialog dialog = new NodeDialog(node);
						dialog.pack();
						dialog.setLocationRelativeTo(Cytoscape.getDesktop());
						dialog.setVisible(true);
						return;
					}
					EdgeView ev = view.getPickedEdgeView(e.getPoint());
					if (ev != null) {
						CyEdge edge = (CyEdge)ev.getEdge();
						EdgeDialog dialog = new EdgeDialog(edge);
						dialog.pack();
						dialog.setLocationRelativeTo(Cytoscape.getDesktop());
						dialog.setVisible(true);
						return;
					}
				} else if (e.getClickCount() == 1) {
					if (drawingEdge) { //Ctrl is not important, we just make sure that there was only one click
						DGraphView view = (DGraphView)Cytoscape.getCurrentNetworkView();
						NodeView nv = view.getPickedNodeView(e.getPoint());
						if (nv != null) { //We have a valid second point for the edge, so we create the edge
							CytoscapeEditor editor = CytoscapeEditorManager.getEditorForView(Cytoscape.getCurrentNetworkView());
							if (editor ==  null) {
								editor = CytoscapeEditorManager.getCurrentEditor();
							}
							editor.addEdge(firstNode, nv.getNode(), CytoscapeEditorManager.EDGE_TYPE, "DefaultEdge");
						} else {
							return;
						}
						drawingEdge = false;
						firstNode = null;
					} else if (e.isControlDown()) {
						DGraphView view = (DGraphView)Cytoscape.getCurrentNetworkView();
						NodeView nv = view.getPickedNodeView(e.getPoint());
						EdgeView ev = view.getPickedEdgeView(e.getPoint());
						if (nv == null && ev == null) { //we are drawing a node
							CytoscapeEditor editor = CytoscapeEditorManager.getEditorForView(Cytoscape.getCurrentNetworkView());
							if (editor ==  null) {
								editor = CytoscapeEditorManager.getCurrentEditor();
							}
							editor.addNode("node", CytoscapeEditorManager.NODE_TYPE, "DefaultNode", new Point2D.Double(e.getPoint().getX(), e.getPoint().getY()));
							drawingEdge = false; //If we started an edge and added a node, we don't want to continue the edge
						} else {
							if (nv != null && ev == null) { //we are adding an edge (a Ctrl-click on an edge should add one of those handles..)
								drawingEdge = true;
								firstNode = nv.getNode();
							}
						}
					}
				}
			}
		});
		
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		//System.err.println("Propertya' chambiata! " + evt.getPropertyName());
		if (evt.getPropertyName().equalsIgnoreCase(CytoscapeDesktop.NETWORK_VIEW_CREATED)) {
			addVisualMappings();
			addMenus();
			addDblClckListener();
			CyNetwork network = Cytoscape.getCurrentNetwork();
			currentEdgeNumber = network.getEdgeCount();
			currentNodeNumber = network.getNodeCount();
			edgesArray = network.edgesList().toArray();
			//System.err.println("Numero iniziale di nodi: " + currentNodeNumber);
		}
		if (evt.getPropertyName().equalsIgnoreCase(Cytoscape.NETWORK_LOADED)) {
			//As there can be edges with intermediate curving points, make those points curved instead of angled (they look nicer)
			List<EdgeView> edgeList = Cytoscape.getCurrentNetworkView().getEdgeViewsList();
			for (Iterator<EdgeView> i = edgeList.listIterator();i.hasNext();) {
				EdgeView ev = (EdgeView)(i.next());
				ev.setLineType(EdgeView.CURVED_LINES);
			}

			VisualMappingManager vizMap = Cytoscape.getVisualMappingManager();
			vizMap.setVisualStyle("default");
			@SuppressWarnings("rawtypes")
			Iterator it = Cytoscape.getCurrentNetwork().nodesList().iterator();
			CyAttributes nodeAttr = Cytoscape.getNodeAttributes();
			while (it.hasNext()) {
				Node n = (Node)it.next();
				if (nodeAttr.hasAttribute(n.getIdentifier(), Model.Properties.INITIAL_LEVEL) && nodeAttr.hasAttribute(n.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS)) {
					double val = 1.0 * nodeAttr.getIntegerAttribute(n.getIdentifier(), Model.Properties.INITIAL_LEVEL) / nodeAttr.getIntegerAttribute(n.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS);
					nodeAttr.setAttribute(n.getIdentifier(), Model.Properties.SHOWN_LEVEL, val); //Set the initial values for the activity ratio of the nodes, to color them correctly
				}
			}
			Cytoscape.getCurrentNetworkView().applyVizmapper(vizMap.getVisualStyle());
		}
		if (evt.getPropertyName().equalsIgnoreCase(Cytoscape.NETWORK_CREATED)) {
			//addVisualMappings();
		}
		if (evt.getPropertyName().equalsIgnoreCase(Cytoscape.NETWORK_MODIFIED)) {
			//System.err.println("Rete modificata!");
			if (currentEdgeNumber != -1 && currentNodeNumber != -1) {
				CyNetwork network = Cytoscape.getCurrentNetwork();
				int newEdgeNumber = network.getEdgeCount(),
					newNodeNumber = network.getNodeCount();
				//System.err.println("Cambiamento alla rete: n. nodi = " + newNodeNumber + " (precedente: " + currentNodeNumber + "), n. edgi = " + newEdgeNumber + " (precedente: " + currentEdgeNumber + ")");
				if (newEdgeNumber > currentEdgeNumber) {
					//JOptionPane.showMessageDialog(null, "Nuovo arco inserito");
					//System.err.println("\tNuovo arco inserito: ora ne hai " + newEdgeNumber + "!");
					List<Object> oldEdges = new Vector<Object>(),
						 newEdges;
					for (Object o : edgesArray) {
						oldEdges.add(o);
					}
					newEdges = network.edgesList();
					CyEdge edge = null;
					for (Object o : newEdges) {
						if (!oldEdges.contains(o)) {
							edge = (CyEdge)o;
							break;
						}
					}
					if (edge != null) {
						Cytoscape.getCurrentNetworkView().getEdgeView(edge).setLineType(EdgeView.CURVED_LINES);
						EdgeDialog dialog = new EdgeDialog(edge);
						dialog.pack();
						dialog.setLocationRelativeTo(Cytoscape.getDesktop());
						dialog.setCreatedNewEdge();
						dialog.setVisible(true);
						if (network.getEdgeCount() < newEdgeNumber) { //Re-update the current number of edges, because the new edge may have been deleted by hitting "Cancel"
							newEdges.remove(edge);
							newEdgeNumber = network.getEdgeCount();
						}
					}
					edgesArray = newEdges.toArray();
				} else if (newEdgeNumber < currentEdgeNumber) {
					//JOptionPane.showMessageDialog(null, "Arco rimosso");
				}
				if (newNodeNumber > currentNodeNumber) {
					network.getSelectedNodes();
					//JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Nuovo nodo inserito");
					//System.err.println("\tNuovo nodo! Ora ne hai " + newNodeNumber);
					Set<CyNode> nodes = network.getSelectedNodes();
					Object[] nodesArray = nodes.toArray();
					for (Object o : nodesArray) {
						CyNode node = (CyNode)o;
						NodeDialog dialog = new NodeDialog(node);
						dialog.pack();
						dialog.setLocationRelativeTo(Cytoscape.getDesktop());
						dialog.setCreatedNewNode();
						dialog.setVisible(true);
						newNodeNumber = network.getNodeCount(); //Re-update the current number of nodes, because the new node may have been deleted by hitting "Cancel"
					}
				} else if(newNodeNumber < currentNodeNumber) {
					//JOptionPane.showMessageDialog(null, "Nodo rimosso");
					//System.err.println("\tNodo rimosso!");
				}
				currentEdgeNumber = newEdgeNumber;
				currentNodeNumber = newNodeNumber;
				//System.err.println("Ecco i nodi ora: " + currentNodeNumber);
				//System.err.println("Ecco i edgi ora: " + currentEdgeNumber);
			}
		}
		if (evt.getPropertyName().equalsIgnoreCase(Cytoscape.SAVE_VIZMAP_PROPS)) {
			
		}
		if (evt.getPropertyName().equalsIgnoreCase(Cytoscape.SESSION_LOADED)) {
			//Notify all result panels that the session has changed
			CytoPanel results = Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST);
			if (results != null) {
				for (int i=0;i<results.getCytoPanelComponentCount();i++) {
					Component c = results.getComponentAt(i);
					findResultsPanelAndNotify(c);
				}
			}
			//Also reset the view to the ANIMO panel
			CytoPanel controlPanel = Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
			int idx = controlPanel.indexOfComponent(InatPlugin.TAB_NAME);
			if (idx >= 0 && idx < controlPanel.getCytoPanelComponentCount()) {
				controlPanel.setSelectedIndex(idx);
			} 
			
			/*//Make sure that if any network is open it has the visual style we made for it
			//Unfortunately this is not enough, because Cytoscape thinks it is a good idea to set the default style to a loaded network AFTER any event about that has been already called
			CyNetworkView currentNetworkView = Cytoscape.getCurrentNetworkView();
			if (currentNetworkView != null) {
				VisualMappingManager vizMap = Cytoscape.getVisualMappingManager();
				CalculatorCatalog visualStyleCatalog = vizMap.getCalculatorCatalog();
				VisualStyle myStyle = visualStyleCatalog.getVisualStyle(InatPlugin.TAB_NAME + "_" + currentNetworkView.getIdentifier());
				if (myStyle != null) {
					//System.err.println("Reimposto lo stile della rete " + currentNetworkView.getIdentifier() + " a " + myStyle);
					currentNetworkView.setVisualStyle(myStyle.getName());
					vizMap.setVisualStyle(myStyle);
					currentNetworkView.redrawGraph(true, true);
				}
			}*/
		}
	}
	
	private void findResultsPanelAndNotify(Component c) {
		if (c instanceof InatResultPanel) {
			((InatResultPanel)c).sessionHasChanged();
		} else if (c instanceof Container) {
			for (Component o : ((Container)c).getComponents()) {
				findResultsPanelAndNotify(o);
			}
		}
	}

	private ColorsListener colorsListener = null;
	private ShapesListener shapesListener = null;
	
	public ColorsListener getColorsListener() {
		if (colorsListener == null) {
			colorsListener = new ColorsListener();
		}
		return this.colorsListener;
	}
	
	public ShapesListener getShapesListener() {
		if (shapesListener == null) {
			shapesListener = new ShapesListener();
		}
		return this.shapesListener;
	}
	
	public class ColorsListener implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent e) {
			try {
				VisualMappingManager vizMap = Cytoscape.getVisualMappingManager();
				VisualStyle visualStyle = vizMap.getVisualStyle();
				NodeAppearanceCalculator nac = visualStyle.getNodeAppearanceCalculator();
				Vector<ObjectMapping> mappings = nac.getCalculator(VisualPropertyType.NODE_FILL_COLOR).getMappings();
				for (ObjectMapping om : mappings) {
					if (!(om instanceof ContinuousMapping)) continue;
					ContinuousMapping mapping = (ContinuousMapping)om;
					List<ContinuousMappingPoint> points = mapping.getAllPoints();
					float fractions[] = new float[points.size()];
					Color colors[] = new Color[points.size()];
					
					int i = 0;
					float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY, intervalSize = 0.0f;
					for (ContinuousMappingPoint point : points) {
						float v = (float)point.getValue().doubleValue();
						if (v < min) {
							min = v;
						}
						if (v > max) {
							max = v;
						}
					}
					intervalSize = max - min;
					for (ContinuousMappingPoint point : points) {
						//System.err.println("Leggo un punto dal valore di " + (float)point.getValue().doubleValue());
						//fractions[fractions.length - 1 - i] = 1 - (float)point.getValue().doubleValue();
						fractions[fractions.length - 1 - i] = 1 - ((float)point.getValue().doubleValue() - min) / intervalSize;
						colors[colors.length - 1 - i] = (Color)point.getRange().equalValue;
						i++;
					}
					legendColors.setParameters(fractions, colors);
				}
			} catch (Exception ex) {
				
			}
		}
	}
	
	
	public class ShapesListener implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent arg0) {
			try {
				VisualMappingManager vizMap = Cytoscape.getVisualMappingManager();
				VisualStyle visualStyle = vizMap.getVisualStyle();
				NodeAppearanceCalculator nac = visualStyle.getNodeAppearanceCalculator();
				DiscreteMapping shapesMap = null,
								widthsMap = null,
								heightsMap = null;
				Vector<ObjectMapping> mappings = nac.getCalculator(VisualPropertyType.NODE_SHAPE).getMappings();
				for (ObjectMapping om : mappings) {
					if (om instanceof DiscreteMapping) {
						shapesMap = (DiscreteMapping)om;
						break;
					}
				}
				mappings = nac.getCalculator(VisualPropertyType.NODE_WIDTH).getMappings();
				for (ObjectMapping om : mappings) {
					if (om instanceof DiscreteMapping) {
						widthsMap = (DiscreteMapping)om;
						break;
					}
				}
				mappings = nac.getCalculator(VisualPropertyType.NODE_HEIGHT).getMappings();
				for (ObjectMapping om : mappings) {
					if (om instanceof DiscreteMapping) {
						heightsMap = (DiscreteMapping)om;
						break;
					}
				}
				if (shapesMap != null && widthsMap != null && heightsMap != null) {
					legendShapes.setParameters(shapesMap, widthsMap, heightsMap);
				}
			} catch (Exception ex) {
				
			}
		}
	}
}
