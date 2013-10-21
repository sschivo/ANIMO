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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import ding.view.DGraphView;
import ding.view.EdgeContextMenuListener;
import ding.view.NodeContextMenuListener;

public class INATPropertyChangeListener implements PropertyChangeListener {
	
	//private int currentEdgeNumber = -1, currentNodeNumber = -1;
	private Map<String, Integer> currentEdgeNumber = new HashMap<String, Integer>(),
								 currentNodeNumber = new HashMap<String, Integer>(); //We keep count of the current number of nodes and edges for each network
	private Map<String, Object[]> currentEdgesArray = new HashMap<String, Object[]>();
	//private Object[] edgesArray = null;
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
		} else {
			visualStyle = visualStyleCatalog.getVisualStyle(myVisualStyleName);
		}
			
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
		DiscreteMapping mapping = null;
		Calculator calco = null;
		
		if (nac.getCalculator(VisualPropertyType.NODE_OPACITY) == null) {
			mapping = new DiscreteMapping(Float.class, Model.Properties.ENABLED);
			mapping.putMapValue(false, 50.0f);
			mapping.putMapValue(true, 255.0f);
			calco = new GenericNodeCustomGraphicCalculator(myVisualStyleName + "Mapping_for_enabled_and_disabled_nodes_(node_opacity)", mapping, VisualPropertyType.NODE_OPACITY);
			nac.setCalculator(calco);
		}
		
		if (nac.getCalculator(VisualPropertyType.NODE_BORDER_COLOR) == null) {
			mapping = new DiscreteMapping(Float.class, Model.Properties.ENABLED);
			mapping.putMapValue(false, 60.0f);
			mapping.putMapValue(true, 255.0f);
			calco = new GenericNodeCustomGraphicCalculator(myVisualStyleName + "Mapping_for_enabled_and_disabled_nodes_(node_border_opacity)", mapping, VisualPropertyType.NODE_BORDER_OPACITY);
			nac.setCalculator(calco);
		}
		
		if (eac.getCalculator(VisualPropertyType.EDGE_TGTARROW_OPACITY) == null) {
			mapping = new DiscreteMapping(Float.class, Model.Properties.ENABLED);
			mapping.putMapValue(false, 50.0f);
			mapping.putMapValue(true, 255.0f);
			calco = new BasicCalculator(myVisualStyleName + "Mapping_for_enabled_and_disabled_edges_(edge_opacity)", mapping, VisualPropertyType.EDGE_OPACITY);
			eac.setCalculator(calco);
		
			calco = new BasicCalculator(myVisualStyleName + "Mapping_for_enabled_and_disabled_edges_(edge_target_arrow_opacity)", mapping, VisualPropertyType.EDGE_TGTARROW_OPACITY);
			eac.setCalculator(calco);
		}

		if (nac.getCalculator(VisualPropertyType.NODE_LINE_WIDTH) == null) {
			mapping = new DiscreteMapping(Float.class, Model.Properties.ENABLED);
			mapping.putMapValue(false, 3.0f);
			mapping.putMapValue(true, 6.0f);
			calco = new GenericNodeCustomGraphicCalculator(myVisualStyleName + "Mapping_to_show_thicker_borders_for_enabled_nodes", mapping, VisualPropertyType.NODE_LINE_WIDTH);
			nac.setCalculator(calco);
		}
		
		if (nac.getCalculator(VisualPropertyType.NODE_BORDER_COLOR) == null) {
			mapping = new DiscreteMapping(Color.class, Model.Properties.PLOTTED);
			mapping.putMapValue(false, Color.DARK_GRAY);
			mapping.putMapValue(true, Color.BLUE);
			calco = new GenericNodeCustomGraphicCalculator(myVisualStyleName + "Mapping_to_highlighting_plotted_nodes_with_a_blue_border", mapping, VisualPropertyType.NODE_BORDER_COLOR);
			nac.setCalculator(calco);
		}
		
		if (eac.getCalculator(VisualPropertyType.EDGE_TGTARROW_SHAPE) == null) {
			mapping = new DiscreteMapping(ArrowShape.class, Model.Properties.INCREMENT);
			mapping.putMapValue(-1, ArrowShape.T);
			mapping.putMapValue(1, ArrowShape.ARROW);
			calco = new BasicCalculator(myVisualStyleName + "Mapping_for_arrow_target_shape", mapping, VisualPropertyType.EDGE_TGTARROW_SHAPE);
			eac.setCalculator(calco);
		}
		
		if (eac.getCalculator(VisualPropertyType.EDGE_LINE_WIDTH) == null) {
			ContinuousMapping mapLineWidth = new ContinuousMapping(Float.class, Model.Properties.SHOWN_LEVEL);
			Float lowerBoundWidth = 2.0f,
				  upperBoundWidth = 10.0f;
			mapLineWidth.addPoint(0.0, new BoundaryRangeValues(lowerBoundWidth, lowerBoundWidth, lowerBoundWidth));
			mapLineWidth.addPoint(1.0, new BoundaryRangeValues(upperBoundWidth, upperBoundWidth, upperBoundWidth));
			mapLineWidth.setInterpolator(new LinearNumberToNumberInterpolator());
			calco = new BasicCalculator(myVisualStyleName + "Mapping_for_edge_width_from_reaction_activity_(speed)", mapLineWidth, VisualPropertyType.EDGE_LINE_WIDTH);
			eac.setCalculator(calco);
		}
		
		final DiscreteMapping shapesMap;
		boolean redoShapeMappings = false;
		
		if (nac.getCalculator(VisualPropertyType.NODE_SHAPE) == null) {
			redoShapeMappings = true;
		} else { //Check if there are missing mappings, and add only those that are not already there (we let the users define their own mappings and don't overwrite them)
			for (ObjectMapping m : nac.getCalculator(VisualPropertyType.NODE_SHAPE).getMappings()) {
				if (m instanceof DiscreteMapping && ((DiscreteMapping)m).getRangeClass().equals(NodeShape.class) && ((DiscreteMapping)m).getControllingAttributeName().equals(Model.Properties.MOLECULE_TYPE)) {
					DiscreteMapping dm = (DiscreteMapping)m;
					if (dm.getMapValue(Model.Properties.TYPE_CYTOKINE) == null) {
						dm.putMapValue(Model.Properties.TYPE_CYTOKINE, NodeShape.RECT);
					}
					if (dm.getMapValue(Model.Properties.TYPE_RECEPTOR) == null) {
						dm.putMapValue(Model.Properties.TYPE_RECEPTOR, NodeShape.ELLIPSE);
					}
					if (dm.getMapValue(Model.Properties.TYPE_KINASE) == null) {
						dm.putMapValue(Model.Properties.TYPE_KINASE, NodeShape.ELLIPSE);
					}
					if (dm.getMapValue(Model.Properties.TYPE_PHOSPHATASE) == null) {
						dm.putMapValue(Model.Properties.TYPE_PHOSPHATASE, NodeShape.DIAMOND);
					}
					if (dm.getMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR) == null) {
						dm.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, NodeShape.ELLIPSE);
					}
					if (dm.getMapValue(Model.Properties.TYPE_GENE) == null) {
						dm.putMapValue(Model.Properties.TYPE_GENE, NodeShape.TRIANGLE);
					}
					if (dm.getMapValue(Model.Properties.TYPE_MRNA) == null) {
						dm.putMapValue(Model.Properties.TYPE_MRNA, NodeShape.PARALLELOGRAM);
					}
					if (dm.getMapValue(Model.Properties.TYPE_DUMMY) == null) {
						dm.putMapValue(Model.Properties.TYPE_DUMMY, NodeShape.ROUND_RECT);
					}
					if (dm.getMapValue(Model.Properties.TYPE_OTHER) == null) {
						dm.putMapValue(Model.Properties.TYPE_OTHER, NodeShape.RECT);
					}
					break;
				}
			}
		}
		
		if (redoShapeMappings) {
			mapping = new DiscreteMapping(NodeShape.class, Model.Properties.MOLECULE_TYPE);
			mapping.putMapValue(Model.Properties.TYPE_CYTOKINE, NodeShape.RECT);
			mapping.putMapValue(Model.Properties.TYPE_RECEPTOR, NodeShape.ELLIPSE);
			mapping.putMapValue(Model.Properties.TYPE_KINASE, NodeShape.ELLIPSE);
			mapping.putMapValue(Model.Properties.TYPE_PHOSPHATASE, NodeShape.DIAMOND);
			mapping.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, NodeShape.ELLIPSE);
			mapping.putMapValue(Model.Properties.TYPE_GENE, NodeShape.TRIANGLE);
			mapping.putMapValue(Model.Properties.TYPE_MRNA, NodeShape.PARALLELOGRAM);
			mapping.putMapValue(Model.Properties.TYPE_DUMMY, NodeShape.ROUND_RECT);
			mapping.putMapValue(Model.Properties.TYPE_OTHER, NodeShape.RECT);
			calco = new BasicCalculator(myVisualStyleName + "Mapping_node_shape_from_molecule_type", mapping, VisualPropertyType.NODE_SHAPE);
			nac.setCalculator(calco);
			shapesMap = mapping;
		} else {
			shapesMap = null;
		}
		
		final DiscreteMapping widthsMap;
		boolean redoWidthMappings = false;
		
		if (nac.getCalculator(VisualPropertyType.NODE_WIDTH) == null) {
			redoWidthMappings = true;
		} else {
			for (ObjectMapping m : nac.getCalculator(VisualPropertyType.NODE_WIDTH).getMappings()) {
				if (m instanceof DiscreteMapping && ((DiscreteMapping)m).getRangeClass().equals(Float.class) && ((DiscreteMapping)m).getControllingAttributeName().equals(Model.Properties.MOLECULE_TYPE)) {
					DiscreteMapping dm = (DiscreteMapping)m;
					if (dm.getMapValue(Model.Properties.TYPE_CYTOKINE) == null) {
						dm.putMapValue(Model.Properties.TYPE_CYTOKINE, 50.0f);
					}
					if (dm.getMapValue(Model.Properties.TYPE_RECEPTOR) == null) {
						dm.putMapValue(Model.Properties.TYPE_RECEPTOR, 45.0f);
					}
					if (dm.getMapValue(Model.Properties.TYPE_KINASE) == null) {
						dm.putMapValue(Model.Properties.TYPE_KINASE, 55.0f);
					}
					if (dm.getMapValue(Model.Properties.TYPE_PHOSPHATASE) == null) {
						dm.putMapValue(Model.Properties.TYPE_PHOSPHATASE, 55.0f);
					}
					if (dm.getMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR) == null) {
						dm.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, 60.0f);
					}
					if (dm.getMapValue(Model.Properties.TYPE_GENE) == null) {
						dm.putMapValue(Model.Properties.TYPE_GENE, 50.0f);
					}
					if (dm.getMapValue(Model.Properties.TYPE_MRNA) == null) {
						dm.putMapValue(Model.Properties.TYPE_MRNA, 50.0f);
					}
					if (dm.getMapValue(Model.Properties.TYPE_DUMMY) == null) {
						dm.putMapValue(Model.Properties.TYPE_DUMMY, 60.0f);
					}
					if (dm.getMapValue(Model.Properties.TYPE_OTHER) == null) {
						dm.putMapValue(Model.Properties.TYPE_OTHER, 60.0f);
					}
					break;
				}
			}
		}
		
		if (redoWidthMappings) {
			mapping = new DiscreteMapping(Float.class, Model.Properties.MOLECULE_TYPE);
			mapping.putMapValue(Model.Properties.TYPE_CYTOKINE, 50.0f);
			mapping.putMapValue(Model.Properties.TYPE_RECEPTOR, 45.0f);
			mapping.putMapValue(Model.Properties.TYPE_KINASE, 55.0f);
			mapping.putMapValue(Model.Properties.TYPE_PHOSPHATASE, 55.0f);
			mapping.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, 60.0f);
			mapping.putMapValue(Model.Properties.TYPE_GENE, 50.0f);
			mapping.putMapValue(Model.Properties.TYPE_MRNA, 50.0f);
			mapping.putMapValue(Model.Properties.TYPE_DUMMY, 60.0f);
			mapping.putMapValue(Model.Properties.TYPE_OTHER, 60.0f);
			calco = new BasicCalculator(myVisualStyleName + "Mapping_node_shape_width_from_molecule_type", mapping, VisualPropertyType.NODE_WIDTH);
			nac.setCalculator(calco);
			widthsMap = mapping;
		} else {
			widthsMap = null;
		}
		
		final DiscreteMapping heightsMap;
		boolean redoHeightMappings = false;
		
		if (nac.getCalculator(VisualPropertyType.NODE_HEIGHT) == null) {
			redoHeightMappings = true;
		} else {
			for (ObjectMapping m : nac.getCalculator(VisualPropertyType.NODE_HEIGHT).getMappings()) {
				if (m instanceof DiscreteMapping && ((DiscreteMapping)m).getRangeClass().equals(Float.class) && ((DiscreteMapping)m).getControllingAttributeName().equals(Model.Properties.MOLECULE_TYPE)) {
					DiscreteMapping dm = (DiscreteMapping)m;
					if (dm.getMapValue(Model.Properties.TYPE_CYTOKINE) == null) {
						dm.putMapValue(Model.Properties.TYPE_CYTOKINE, 50.0f);
					}
					if (dm.getMapValue(Model.Properties.TYPE_RECEPTOR) == null) {
						dm.putMapValue(Model.Properties.TYPE_RECEPTOR, 65.0f);
					}
					if (dm.getMapValue(Model.Properties.TYPE_KINASE) == null) {
						dm.putMapValue(Model.Properties.TYPE_KINASE, 55.0f);
					}
					if (dm.getMapValue(Model.Properties.TYPE_PHOSPHATASE) == null) {
						dm.putMapValue(Model.Properties.TYPE_PHOSPHATASE, 55.0f);
					}
					if (dm.getMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR) == null) {
						dm.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, 40.0f);
					}
					if (dm.getMapValue(Model.Properties.TYPE_GENE) == null) {
						dm.putMapValue(Model.Properties.TYPE_GENE, 50.0f);
					}
					if (dm.getMapValue(Model.Properties.TYPE_MRNA) == null) {
						dm.putMapValue(Model.Properties.TYPE_MRNA, 50.0f);
					}
					if (dm.getMapValue(Model.Properties.TYPE_DUMMY) == null) {
						dm.putMapValue(Model.Properties.TYPE_DUMMY, 40.0f);
					}
					if (dm.getMapValue(Model.Properties.TYPE_OTHER) == null) {
						dm.putMapValue(Model.Properties.TYPE_OTHER, 35.0f);
					}
					break;
				}
			}
		}
		
		if (redoHeightMappings) {
			mapping = new DiscreteMapping(Float.class, Model.Properties.MOLECULE_TYPE);
			mapping.putMapValue(Model.Properties.TYPE_CYTOKINE, 50.0f);
			mapping.putMapValue(Model.Properties.TYPE_RECEPTOR, 65.0f);
			mapping.putMapValue(Model.Properties.TYPE_KINASE, 55.0f);
			mapping.putMapValue(Model.Properties.TYPE_PHOSPHATASE, 55.0f);
			mapping.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, 40.0f);
			mapping.putMapValue(Model.Properties.TYPE_GENE, 50.0f);
			mapping.putMapValue(Model.Properties.TYPE_MRNA, 50.0f);
			mapping.putMapValue(Model.Properties.TYPE_DUMMY, 40.0f);
			mapping.putMapValue(Model.Properties.TYPE_OTHER, 35.0f);
			calco = new BasicCalculator(myVisualStyleName + "Mapping_node_shape_height_from_molecule_type", mapping, VisualPropertyType.NODE_HEIGHT);
			nac.setCalculator(calco);
			heightsMap = mapping;
		} else {
			heightsMap = null;
		}
		
		PassThroughMapping mp;
		
		//Looks like Cytoscape normally shows the ID of a node as its label, so we force this to be used anyway.
		//if (nac.getCalculator(VisualPropertyType.NODE_LABEL) == null) {
			mp = new PassThroughMapping(String.class, Model.Properties.CANONICAL_NAME);
			calco = new BasicCalculator(myVisualStyleName + "Mapping_for_node_label", mp, VisualPropertyType.NODE_LABEL);
			nac.setCalculator(calco);
		//}
		
		if (nac.getCalculator(VisualPropertyType.NODE_TOOLTIP) == null) {
			mp = new PassThroughMapping(String.class, Model.Properties.DESCRIPTION);
			calco = new BasicCalculator(myVisualStyleName + "Mapping_for_node_tooltip", mp, VisualPropertyType.NODE_TOOLTIP);
			nac.setCalculator(calco);
		}
		if (eac.getCalculator(VisualPropertyType.EDGE_TOOLTIP) == null) {
			mp = new PassThroughMapping(String.class, Model.Properties.DESCRIPTION);
			calco = new BasicCalculator(myVisualStyleName + "Mapping_for_edge_tooltip", mp, VisualPropertyType.EDGE_TOOLTIP);
			eac.setCalculator(calco);
		}
		
		if (nac.getCalculator(VisualPropertyType.NODE_FILL_COLOR) == null) {
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
			nac.setCalculator(calco);
			//legendColors.setParameters(new float[]{1 - 1, 1 - 2/3.0f, 1 - 1/3.0f, 1 - 0}, new Color[]{Color.WHITE, new Color(255, 255, 0), new Color(204, 0, 0), Color.BLACK});
			legendColors.setParameters(new float[]{1 - 1, 1 - 0.5f, 1 - 0/*1 - 1, 1 - 0*/}, new Color[]{new Color(0, 204, 0), new Color(255, 204, 0), new Color(204, 0, 0)/*new Color(255, 204, 0), new Color(204, 0, 0)*/});
		} else {
			legendColors.updateFromSettings();
		}
		
		List<String> orderedNames = Arrays.asList(new String[]{Model.Properties.TYPE_CYTOKINE, Model.Properties.TYPE_RECEPTOR, Model.Properties.TYPE_KINASE,
				   Model.Properties.TYPE_PHOSPHATASE, Model.Properties.TYPE_TRANSCRIPTION_FACTOR, Model.Properties.TYPE_GENE,
				   Model.Properties.TYPE_MRNA, Model.Properties.TYPE_DUMMY, Model.Properties.TYPE_OTHER});
		legendShapes.setNameOrder(orderedNames);
		
		if (shapesMap != null && widthsMap != null && heightsMap != null) {
			legendShapes.setParameters(shapesMap, widthsMap, heightsMap);
		} else {
			legendShapes.updateFromSettings();
		}
		
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
		if (network != null && !network.equals(Cytoscape.getNullNetwork())) {
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
		//System.err.println("Il mapping per " + myVisualStyleName + " esiste di sicuro ora, quindi lo uso");
		VisualStyle myVisualStyle = visualStyleCatalog.getVisualStyle(myVisualStyleName);
		currentNetworkView.setVisualStyle(myVisualStyleName);
		vizMap.setVisualStyle(myVisualStyleName);
		currentNetworkView.redrawGraph(true, true);
		visualStyleCatalog.removeVisualStyle("default");
		visualStyleCatalog.addVisualStyle(new VisualStyle(myVisualStyle, "default"));
		
		legendColors.updateFromSettings();
		legendShapes.updateFromSettings();
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
					if (Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST).getSelectedIndex() != InatPlugin.TAB_INDEX) {
						drawingEdge = false;
						firstNode = null;
						return;
					}
					if (drawingEdge) { //Ctrl is not important, we just make sure that there was only one click
						DGraphView view = (DGraphView)Cytoscape.getCurrentNetworkView();
						NodeView nv = view.getPickedNodeView(e.getPoint());
						if (nv != null) { //We have a valid second point for the edge, so we create the edge
							CytoscapeEditor editor = CytoscapeEditorManager.getEditorForView(Cytoscape.getCurrentNetworkView());
							if (editor ==  null) {
								editor = CytoscapeEditorManager.getCurrentEditor();
							}
							editor.addEdge(firstNode, nv.getNode(), CytoscapeEditorManager.EDGE_TYPE, "DefaultEdge");
							Cytoscape.firePropertyChange(Cytoscape.NETWORK_MODIFIED, null, null);
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
							Cytoscape.firePropertyChange(Cytoscape.NETWORK_MODIFIED, null, null);
							drawingEdge = false; //If we started an edge and added a node, we don't want to continue the edge
							firstNode = null;
						} else if (nv != null && ev == null) { //we are adding an edge (a Ctrl-click on an edge should add one of those handles..)
							drawingEdge = true;
							firstNode = nv.getNode();
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
			currentEdgeNumber.put(network.getIdentifier(), network.getEdgeCount());
			currentNodeNumber.put(network.getIdentifier(), network.getNodeCount());
			currentEdgesArray.put(network.getIdentifier(), network.edgesList().toArray());
			//System.err.println("Network viewa creatata per la rete \"" + network.getTitle() + "\"!! Numero iniziale di nodi: " + currentNodeNumber.get(network.getIdentifier()) + ", edgi " + currentEdgeNumber.get(network.getIdentifier()));
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
			//System.err.println("Creato una nuova rete: \"" + Cytoscape.getCurrentNetwork().getTitle() + "\"! Ora ho n edgi = " + currentEdgeNumber + " e nodi " + currentNodeNumber);
			//addVisualMappings();
		}
		if (evt.getPropertyName().equalsIgnoreCase(Cytoscape.NETWORK_MODIFIED)) {
			//System.err.println("Rete modificata!");
			CyNetwork network = Cytoscape.getCurrentNetwork();
			if (currentEdgeNumber.get(network.getIdentifier()) != null && currentNodeNumber.get(network.getIdentifier()) != null) {
				int newEdgeNumber = network.getEdgeCount(),
					newNodeNumber = network.getNodeCount();
				Object[] edgesArray = network.edgesList().toArray();
				//System.err.println("Cambiamento alla rete \"" + network.getTitle() + "\": n. nodi = " + newNodeNumber + " (precedente: " + currentNodeNumber.get(network.getIdentifier()) + "), n. edgi = " + newEdgeNumber + " (precedente: " + currentEdgeNumber.get(network.getIdentifier()) + ")");
				if (newEdgeNumber > currentEdgeNumber.get(network.getIdentifier())) {
					//JOptionPane.showMessageDialog(null, "Nuovo arco inserito");
					//System.err.println("\tNuovo arco inserito nella rete \"" + network.getTitle() + "\": ora ne hai " + newEdgeNumber + "!");
					List<Object> //oldEdges = new Vector<Object>(),
						 newEdges;
					edgesArray = currentEdgesArray.get(network.getIdentifier());
					/*boolean presente = false;
					for (Object o : edgesArray) {
						oldEdges.add(o);
						CyEdge e = (CyEdge)o;
						if (e.getIdentifier().contains("node24_765 (DefaultEdge) node34")) {
							presente = true;
						}
					}
					if (presente) {
						System.err.println("Nella lista di edge gia' presenti esiste gia' l'edge node24_765 (DefaultEdge) node34.");
					} else {
						System.err.println("L'edge node24_765 (DefaultEdge) node34 NON c'e' gia' nella lista di quelli presenti!");
					}*/
					newEdges = network.edgesList();
					CyEdge edge = null;
					for (Object o : newEdges) {
						CyEdge e = (CyEdge)o;
						boolean contained = false;
						for (Object o2: edgesArray) {
							CyEdge e2 = (CyEdge)o2;
							if (e2.getIdentifier().equals(e.getIdentifier())) { //Looks like we can't use equals (so, just use oldEdges.contains(o)). We must check for the edge IDs
								contained = true;
								break;
							}
						}
						if (!contained) { //oldEdges.contains(o)) {
							//System.err.println("Risulta che l'edge " + o + " non fosse prima nella rete e ora ci sia: come mai?");
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
				} else if (newEdgeNumber < currentEdgeNumber.get(network.getIdentifier())) {
					//JOptionPane.showMessageDialog(null, "Arco rimosso");
					edgesArray = network.edgesList().toArray();
				}
				if (newNodeNumber > currentNodeNumber.get(network.getIdentifier())) {
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
				} else if(newNodeNumber < currentNodeNumber.get(network.getIdentifier())) {
					//JOptionPane.showMessageDialog(null, "Nodo rimosso");
					//System.err.println("\tNodo rimosso!");
				}
				currentEdgeNumber.put(network.getIdentifier(), newEdgeNumber);
				currentNodeNumber.put(network.getIdentifier(), newNodeNumber);
				currentEdgesArray.put(network.getIdentifier(), edgesArray);
				//System.err.println("Ecco i nodi ora nella rete \"" + network.getTitle() + "\": " + currentNodeNumber.get(network.getIdentifier()));
				//System.err.println("Ecco i edgi ora nella rete \"" + network.getTitle() + "\": " + currentEdgeNumber.get(network.getIdentifier()));
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
				legendColors.updateFromSettings();
			} catch (Exception ex) {
				
			}
		}
	}
	
	
	public class ShapesListener implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent arg0) {
			try {
				legendShapes.updateFromSettings();
			} catch (Exception ex) {
				
			}
		}
	}
}
