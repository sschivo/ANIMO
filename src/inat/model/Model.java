package inat.model;

import fitting.ScenarioCfg;
import giny.model.Edge;
import giny.model.Node;
import inat.InatBackend;
import inat.analyser.uppaal.VariablesModel;
import inat.exceptions.InatException;
import inat.util.Table;
import inat.util.XmlConfiguration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTask;

/**
 * A model. This model keeps itself consistent, as long as both {@link Reactant}
 * and {@link Reaction} implementations keep their {@link #equals(Object)}
 * method based on identity the model is automatically consistent.
 * 
 * @author B. Wanders
 */
public class Model implements Serializable {
	public static class Properties {
		public static final String NUMBER_OF_LEVELS = "levels", //Property that can belong to a node or to a network. If related to a single node, it represents the maximum number of levels for that single reactant. If related to a complete network, it is the maximum value of the NUMBER_OF_LEVELS property among all nodes in the network. Expressed as integer number in [0, 100] (chosen by the user).
								   INITIAL_LEVEL = "initialConcentration", //Property belonging to a node. The initial activity level for a node. Expressed as an integer number in [0, NUMBER_OF_LEVELS for that node]
								   SHOWN_LEVEL = "activityRatio", //Property belonging to a node. The current activity level of a node. Expressed as a relative number representing INITIAL_LEVEL / NUMBER_OF_LEVELS, so it is a double number in [0, 1]
								   SECONDS_PER_POINT = "seconds per point", //Property belonging to a network. The number of real-life seconds represented by a single UPPAAL time unit.
								   SECS_POINT_SCALE_FACTOR = "time scale factor", //This value is multiplied to the time bounds as a counterbalance to any change in seconds per point. This allows us to avoid having to directly modify the parameters of scenarios.
								   LEVELS_SCALE_FACTOR = "levels scale factor", //Also this value is multiplied to the time bounds, and it counterbalances the changes in the number of levels for the reactants. It is specific for every reaction.
								   SCENARIO = "scenario", //Property belonging to an edge. The id of the scenario on which the reaction corresponding to the edge computes its time tables.
								   ALIAS = "alias", //The property used to indicate the user-chosen name of a node
								   CANONICAL_NAME = "canonicalName", //The same, but in the Cytoscape model instead of the Model
								   DESCRIPTION = "description", //The verbose description of a node/edge, possibly provided with references to papers and/or various IDs
								   ENABLED = "enabled", //Tells us whether a node/edge is enabled
								   PLOTTED = "plotted", //Tells us whether to plot a node or not
								   GROUP = "group", //A group of nodes identifies alternative phosphorylation sites (can be useless)
								   TIMES_UPPER = "timesU", //Upper time bound
								   TIMES = "times", //Time bound (no upper nor lower: it is possible that it is never be used in practice)
								   TIMES_LOWER = "timesL", //Lower time bound
								   MINIMUM_DURATION = "minTime", //The minimum amount of time a reaction can take
								   MAXIMUM_DURATION = "maxTime", //The maximum amount of time a reaction can take (not infinite)
								   INCREMENT = "increment", //Increment in substrate as effect of the reaction (+1, -1, etc)
								   BI_REACTION = "reaction2", //Reaction between two reactants (substrate/reactant and catalyst)
								   MONO_REACTION = "reaction1", //Reaction with only one reactant
								   UNCERTAINTY = "uncertainty", //The percentage of uncertainty about the reaction parameter settings
								   REACTANT = "reactant", //The reactant for a mono-reaction or the substrate for a bi-reaction
								   REACTANT_ID = "_REACTANT_", //(prefix for a parameter of a reaction) The Cytoscape ID for a reactant. The actual name of this property is for example _REACTANT_S, _REACTANT_E, _REACTANT_E1, _REACTANT_E2
								   REACTANT_IS_ACTIVE_INPUT = "_REACTANT_ACT_", //(prefix for a parameter of a reaction) tells whether the input for a reaction considers the active or inactive part of the given reactant. The actual name of this property is for example _REACTANT_ACT_S, _REACTANT_ACT_E, _REACTANT_ACT_E1, _REACTANT_ACT_E2
								   R1_IS_DOWNSTREAM = "r1IsDownstream", //whether the reactant known as r1 is a downstream reactant (i.e., its activity influences the reaction, but the reaction influences its activity too)
								   R2_IS_DOWNSTREAM = "r2IsDownstream", //see R1_IS_DOWNSTREAM
								   OUTPUT_REACTANT = "output reactant", //The ID of the output reactant of a reaction
								   CYTOSCAPE_ID = "cytoscape id", //The ID assigned to the node/edge by Cytoscape
								   MOLECULE_TYPE = "moleculeType", //The type of the reactant (kinase, phosphatase, receptor, cytokine, ...)
								   TYPE_CYTOKINE = "Cytokine", //The following TYPE_* keys are the possible values for the property MOLECULE_TYPE
								   TYPE_RECEPTOR = "Receptor",
								   TYPE_KINASE = "Kinase",
								   TYPE_PHOSPHATASE = "Phosphatase",
								   TYPE_TRANSCRIPTION_FACTOR = "Transcription factor",
								   TYPE_OTHER = "Other",
								   TYPE_MRNA = "mRNA",
								   TYPE_GENE = "Gene",
								   TYPE_DUMMY = "Dummy",
								   REACTANT_NAME = "name", //The name of the reactant (possibly outdated property name)
								   REACTION_TYPE = "type", //Type of reaction (mono, bi)
								   SCENARIO_CFG = "SCENARIO_CFG", //The current scenario configuration (scenario index + parameter values) of the reaction
								   CATALYST = "catalyst", //Catalyst in a bi-reaction
								   REACTANT_INDEX = "index", //The index of the reactant (sometimes we need to assign them an index)
								   SCENARIO_PARAMETER_KM = "km", //The following are all scenario parameters
								   SCENARIO_PARAMETER_K2 = "k2",
								   SCENARIO_PARAMETER_STOT = "Stot",
								   SCENARIO_PARAMETER_K2_KM = "k2/km",
								   SCENARIO_ONLY_PARAMETER = "parameter",
								   SCENARIO_PARAMETER_K = "k",
								   MODEL_CHECKING_TYPE = "model checking type";
		public static final int STATISTICAL_MODEL_CHECKING = 1,
								NORMAL_MODEL_CHECKING = 2;
	}

	
	private static final long serialVersionUID = 9078409933212069999L;
	/**
	 * The vertices in the model.
	 */
	private Map<String, Reactant> reactants;
	/**
	 * The edges in the model.
	 */
	private Map<String, Reaction> reactions;
	private Map<String, String> mapCytoscapeIDtoReactantID = null;
	
	/**
	 * The global properties on the model.
	 */
	private PropertyBag properties;

	/**
	 * Constructor.
	 */
	public Model() {
		this.reactants = new HashMap<String, Reactant>();
		this.reactions = new HashMap<String, Reaction>();
		this.properties = new PropertyBag();
	}
	
	public void setReactants(Map<String, Reactant> reactants) { //The next 7 methods are to keep compatibility with java beans and XML encoder/decoder
		this.reactants = reactants;
	}
	
	public void setReactions(Map<String, Reaction> reactions) {
		this.reactions = reactions;
	}
	
	public void setProperties(PropertyBag properties) {
		this.properties = properties;
	}
	
	public void setMapCytoscapeIDtoReactantID(Map<String, String> mapCytoscapeIDtoReactantID) {
		this.mapCytoscapeIDtoReactantID = mapCytoscapeIDtoReactantID;
	}
	
	public Map<String, Reactant> getReactants() {
		return this.reactants;
	}
	
	public Map<String, Reaction> getReactions() {
		return this.reactions;
	}
	
	public Map<String, String> getMapCytoscapeIDtoReactantID() {
		return this.mapCytoscapeIDtoReactantID;
	}

	/**
	 * Puts (adds or replaces) a vertex into the model.
	 * 
	 * @param v the vertex to add
	 */
	public void add(Reactant v) {
		assert v.getModel() == null : "Can't add a reactant that is already part of a model.";

		this.reactants.put(v.getId(), v);
		v.setModel(this);
	}

	/**
	 * Puts (adds or replaces) an edge into the model.
	 * 
	 * @param e the edge to remove
	 */
	public void add(Reaction e) {
		assert e.getModel() == null : "Can't add a reaction that is already part of a model.";

		this.reactions.put(e.getId(), e);
		e.setModel(this);
	}

	/**
	 * Removes an edge.
	 * 
	 * @param e the edge to remove
	 */
	public void remove(Reaction e) {
		assert e.getModel() == this : "Can't remove a reaction that is not part of this model.";
		this.reactions.remove(e.getId());
		e.setModel(null);
	}

	/**
	 * Removes a vertex, this method also cleans all edges connecting to this
	 * vertex.
	 * 
	 * @param v the vertex to remove
	 */
	public void remove(Reactant v) {
		assert v.getModel() == this : "Can't remove a reactant that is not part of this model.";
		this.reactants.remove(v.getId());
		v.setModel(null);
	}

	/**
	 * Returns the edge with the given identifier, or {@code null}.
	 * 
	 * @param id the identifier we are looking for
	 * @return the found {@link Reaction}, or {@code null}
	 */
	public Reaction getReaction(String id) {
		return this.reactions.get(id);
	}

	/**
	 * Returns the vertex with the given identifier, or {@code null}.
	 * 
	 * @param id the identifier we are looking for
	 * @return the found {@link Reactant}, or {@code null}
	 */
	public Reactant getReactant(String id) {
		return this.reactants.get(id);
	}
	
	/**
	 * Given the Cytoscape node id, return the corresponding Reactant in the model
	 * @param id The Cytoscape node identificator (e.g. node102 etc)
	 * @return The Reactant as constructed in the current model
	 */
	public Reactant getReactantByCytoscapeID(String id) {
		return this.reactants.get(this.mapCytoscapeIDtoReactantID.get(id));
	}

	/**
	 * Returns the properties for this model.
	 * 
	 * @return the properties of this model
	 */
	public PropertyBag getProperties() {
		return this.properties;
	}

	/**
	 * Returns an unmodifiable view of all vertices in this model.
	 * 
	 * @return all vertices
	 */
	public Collection<Reactant> getReactantCollection() {
		return Collections.unmodifiableCollection(this.reactants.values());
	}
	
	public List<Reactant> getSortedReactantList() {
		List<Reactant> result = new ArrayList<Reactant>();
		result.addAll(this.reactants.values());
		Collections.sort(result, new Comparator<Reactant>() {
			@Override
			public int compare(Reactant r1, Reactant r2) {
				return r1.getName().compareTo(r2.getName());
			}
		});
		return result;
	}

	/**
	 * returns an unmodifiable view of all edges in this model.
	 * 
	 * @return all edges
	 */
	public Collection<Reaction> getReactionCollection() {
		return Collections.unmodifiableCollection(this.reactions.values());
	}
	
	
	/**
	 * returns a (deep) copy of this model
	 */
	public Model copy() {
		Model model = new Model();
		model.properties = this.properties.copy();
		for (String k : reactants.keySet()) {
			model.reactants.put(k, (Reactant)reactants.get(k).copy());
		}
		for (String k : reactions.keySet()) {
			model.reactions.put(k, (Reaction)reactions.get(k).copy());
		}
		return model;
	}
	
	

	/**
	 * Translate the Cytoscape network in the internal ANIMO model representation.
	 * This intermediate model will then translated as needed into the proper UPPAAL
	 * model by the analysers. All properties needed from the Cytoscape network are
	 * copied in the resulting model, checking that all are set ok.
	 * @param monitor The TaskMonitor with which to communicate the advancement of the model generation
	 * @param nMinutesToSimulate If a simulation was requested, this parameter is >= 0 and
	 * when checking the presence of all necessary parameters (see checkParameters()) we will also check that
	 * the upper bound is not "too large" for UPPAAL to understand it. The parameter can be < 0 or null to
	 * indicate that no simulation is requested.
	 * @param generateTables Whether to generate the time tables, or just find minimum and maximum to set the
	 * reaction activityRatio when the slider is used
	 * @return The intermediate ANIMO model
	 * @throws InatException
	 */
	@SuppressWarnings("unchecked")
	public static Model generateModelFromCurrentNetwork(TaskMonitor monitor, Integer nMinutesToSimulate, boolean generateTables) throws InatException {
		final String NUMBER_OF_LEVELS = Model.Properties.NUMBER_OF_LEVELS, //The total number of levels for a node (=reactant), or for the whole network (the name of the property is the same)
		SECONDS_PER_POINT = Model.Properties.SECONDS_PER_POINT, //The number of real-life seconds represented by a single UPPAAL time unit
		SECS_POINT_SCALE_FACTOR = Model.Properties.SECS_POINT_SCALE_FACTOR, //The scale factor for the UPPAAL time settings, allowing to keep the same scenario parameters, while varying the "density" of simulation sample points
		LEVELS_SCALE_FACTOR = Model.Properties.LEVELS_SCALE_FACTOR, //The scale factor used by each reaction to counterbalance the change in number of levels for the reactants.
		INCREMENT = Model.Properties.INCREMENT, //The increment in activity caused by a reaction on its downstream reactant
		BI_REACTION = Model.Properties.BI_REACTION, //Identifies a reaction having two reatants
		REACTANT = Model.Properties.REACTANT, //The name of the reactant taking part to the reaction
		CATALYST = Model.Properties.CATALYST, //The name of the catalyst enabling the reaction
		OUTPUT_REACTANT = Model.Properties.OUTPUT_REACTANT, //The reactant that is actually influenced by the reaction
		SCENARIO = Model.Properties.SCENARIO, //The id of the scenario used to set the parameters for an edge (=reaction)
		CYTOSCAPE_ID = Model.Properties.CYTOSCAPE_ID, //The id assigned to the node/edge by Cytoscape
		CANONICAL_NAME = Model.Properties.CANONICAL_NAME, //The name of a reactant displayed to the user
		INITIAL_LEVEL = Model.Properties.INITIAL_LEVEL, //The starting activity level of a reactant
		UNCERTAINTY = Model.Properties.UNCERTAINTY, //The uncertainty about the parameters setting for an edge(=reaction)
		ENABLED = Model.Properties.ENABLED, //Whether the node/edge is enabled. Influences the display of that node/edge thanks to the discrete Visual Mapping defined by AugmentAction
		PLOTTED = Model.Properties.PLOTTED, //Whether the node is plotted in the graph. Default: yes
		GROUP = Model.Properties.GROUP, //Could possibly be never used. All nodes(=reactants) belonging to the same group represent alternative (in the sense of exclusive or) phosphorylation sites of the same protein.
		TIMES_U = Model.Properties.TIMES_UPPER,
		TIMES_L = Model.Properties.TIMES_LOWER,
		MINIMUM_DURATION = Model.Properties.MINIMUM_DURATION,
		MAXIMUM_DURATION = Model.Properties.MAXIMUM_DURATION,
		REACTION_TYPE = Model.Properties.REACTION_TYPE,
		REACTANT_NAME = Model.Properties.REACTANT_NAME,
		REACTANT_ALIAS = Model.Properties.ALIAS,
		REACTION_ALIAS = Model.Properties.ALIAS;
		
		
		checkParameters(monitor, nMinutesToSimulate);
		
		Map<String, String> nodeNameToId = new HashMap<String, String>();
		Map<String, String> edgeNameToId = new HashMap<String, String>();
		
		Model model = new Model();
		
		CyNetwork network = Cytoscape.getCurrentNetwork();
		
		final int totalWork = network.getNodeCount() + network.getEdgeCount();
		int doneWork = 0;
		
		CyAttributes networkAttributes = Cytoscape.getNetworkAttributes();
		
		if (networkAttributes.hasAttribute(network.getIdentifier(), "deltaAlternating")) {
			model.getProperties().let("deltaAlternating").be(networkAttributes.getBooleanAttribute(network.getIdentifier(), "deltaAlternating"));
		}
		if (networkAttributes.hasAttribute(network.getIdentifier(), "useOldResetting")) {
			model.getProperties().let("useOldResetting").be(networkAttributes.getBooleanAttribute(network.getIdentifier(), "useOldResetting"));
		}
		model.getProperties().let(NUMBER_OF_LEVELS).be(networkAttributes.getAttribute(network.getIdentifier(), NUMBER_OF_LEVELS));
		model.getProperties().let(SECONDS_PER_POINT).be(networkAttributes.getAttribute(network.getIdentifier(), SECONDS_PER_POINT));
		double secStepFactor = networkAttributes.getDoubleAttribute(network.getIdentifier(), SECS_POINT_SCALE_FACTOR);
		model.getProperties().let(SECS_POINT_SCALE_FACTOR).be(secStepFactor);
		
		final Integer MaxNLevels = networkAttributes.getIntegerAttribute(network.getIdentifier(), NUMBER_OF_LEVELS);
		final Double nSecondsPerPoint = networkAttributes.getDoubleAttribute(network.getIdentifier(), SECONDS_PER_POINT);
		
		model.getProperties().let(NUMBER_OF_LEVELS).be(MaxNLevels);
		model.getProperties().let(SECONDS_PER_POINT).be(nSecondsPerPoint);
		
		// do nodes first
		final CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
		List<Node> nodesList = (List<Node>) network.nodesList();
		Collections.sort(nodesList, new Comparator<Node>() {
			@Override
			public int compare(Node n1, Node n2) {
				String name1, name2;
				if (nodeAttributes.hasAttribute(n1.getIdentifier(), CANONICAL_NAME)) {
					name1 = nodeAttributes.getStringAttribute(n1.getIdentifier(), CANONICAL_NAME);
				} else {
					name1 = n1.getIdentifier();
				}
				if (nodeAttributes.hasAttribute(n2.getIdentifier(), CANONICAL_NAME)) {
					name2 = nodeAttributes.getStringAttribute(n2.getIdentifier(), CANONICAL_NAME);
				} else {
					name2 = n2.getIdentifier();
				}
				return name1.compareTo(name2);
			}
		});
		final Iterator<Node> nodes = (Iterator<Node>) nodesList.iterator(); //network.nodesIterator();
		for (int i = 0; nodes.hasNext(); i++) {
			if (monitor != null) {
				monitor.setPercentCompleted((100 * doneWork++) / totalWork);
			}
			Node node = nodes.next();
			
			final String reactantId = "R" + i;
			Reactant r = new Reactant(reactantId);
			nodeNameToId.put(node.getIdentifier(), reactantId);
			
			r.let(CYTOSCAPE_ID).be(node.getIdentifier());
			r.let(REACTANT_NAME).be(node.getIdentifier());
			r.let(REACTANT_ALIAS).be(nodeAttributes.getAttribute(node.getIdentifier(), CANONICAL_NAME));
			r.let(NUMBER_OF_LEVELS).be(nodeAttributes.getAttribute(node.getIdentifier(), NUMBER_OF_LEVELS));
			r.let(LEVELS_SCALE_FACTOR).be(nodeAttributes.getAttribute(node.getIdentifier(), LEVELS_SCALE_FACTOR));
			r.let(GROUP).be(nodeAttributes.getAttribute(node.getIdentifier(), GROUP));
			r.let(ENABLED).be(nodeAttributes.getAttribute(node.getIdentifier(), ENABLED));
			r.let(PLOTTED).be(nodeAttributes.getAttribute(node.getIdentifier(), PLOTTED));
			r.let(INITIAL_LEVEL).be(nodeAttributes.getIntegerAttribute(node.getIdentifier(), INITIAL_LEVEL));
			
			model.add(r);
		}
		
		
		// do edges next
		CyAttributes edgeAttributes = Cytoscape.getEdgeAttributes();
		final Iterator<Edge> edges = (Iterator<Edge>) network.edgesIterator();
		int minTimeModel = Integer.MAX_VALUE,
			maxTimeModel = Integer.MIN_VALUE;
		Integer unc = 5; //Uncertainty value is now ANIMO-wide (TODO: is that a bit excessive? One would expect the uncertainty to be connected to the model...)
		XmlConfiguration configuration = InatBackend.get().configuration();
		try {
			unc = new Integer(configuration.get(XmlConfiguration.UNCERTAINTY_KEY));
		} catch (NumberFormatException ex) {
			unc = 0;
		}
		for (int i = 0; edges.hasNext(); i++) {
			if (monitor != null) {
				monitor.setPercentCompleted((100 * doneWork++) / totalWork);
			}
			Edge edge = edges.next();
			if (!edgeAttributes.getBooleanAttribute(edge.getIdentifier(), ENABLED)) continue;
			
			Double levelsScaleFactor;// = nodeAttributes.getDoubleAttribute(edge.getSource().getIdentifier(), LEVELS_SCALE_FACTOR) / nodeAttributes.getDoubleAttribute(edge.getTarget().getIdentifier(), LEVELS_SCALE_FACTOR);
									//edgeAttributes.getDoubleAttribute(edge.getIdentifier(), LEVELS_SCALE_FACTOR); //The scale factor due to the nodes' number of levels is now a property of the nodes themselves, not of the reactions (we take care of retrocompatibility by transferring and deleting attributes found in reactions to their nodes instead in the checkParameters method)
			
			String reactionId = "reaction" + i;
			Reaction r = new Reaction(reactionId);
			edgeNameToId.put(edge.getIdentifier(), reactionId);
			
			r.let(ENABLED).be(edgeAttributes.getAttribute(edge.getIdentifier(), ENABLED));
			r.let(INCREMENT).be(edgeAttributes.getAttribute(edge.getIdentifier(), INCREMENT));
			//r.let(CYTOSCAPE_ID).be("E" + edge.getRootGraphIndex()); //Unfortunately, the so-called "immutable" rootGraphIndex is NOT immutable! If you reload the network, that index will have passed to another edge. Why? Ask Cytoscape
			r.let(CYTOSCAPE_ID).be("E" + edge.getIdentifier());
			
			r.let(REACTION_TYPE).be(BI_REACTION);
			
			final String reactant = nodeNameToId.get(edge.getTarget().getIdentifier());
			r.let(REACTANT).be(reactant);

			final String catalyst = nodeNameToId.get(edge.getSource().getIdentifier());
			r.let(CATALYST).be(catalyst);
			
			int nLevelsR1,
				nLevelsR2;
			
			if (!model.getReactant(catalyst).get(NUMBER_OF_LEVELS).isNull()) {
				nLevelsR1 = model.getReactant(catalyst).get(NUMBER_OF_LEVELS).as(Integer.class);
			} else {
				nLevelsR1 = model.getProperties().get(NUMBER_OF_LEVELS).as(Integer.class);
			}
			if (!model.getReactant(reactant).get(NUMBER_OF_LEVELS).isNull()) {
				nLevelsR2 = model.getReactant(reactant).get(NUMBER_OF_LEVELS).as(Integer.class);
			} else {
				nLevelsR2 = model.getProperties().get(NUMBER_OF_LEVELS).as(Integer.class);
			}

			Scenario[] scenarios = Scenario.sixScenarios;
			Integer scenarioIdx;
			/*if (edgeAttributes.hasAttribute(edge.getIdentifier(), SCENARIO)) {
				scenarioIdx = edgeAttributes.getIntegerAttribute(edge.getIdentifier(), SCENARIO);
			} else {
				//we do this thing in checkParameters
				scenarioIdx = 0;
			}*/
			scenarioIdx = edgeAttributes.getIntegerAttribute(edge.getIdentifier(), SCENARIO);
			Scenario scenario;
			if (scenarioIdx >= 0 && scenarioIdx < scenarios.length) {
				scenario = scenarios[scenarioIdx];
				switch (scenarioIdx) {
					case 0:
						levelsScaleFactor = 1.0 / model.getReactant(reactant).get(NUMBER_OF_LEVELS).as(Integer.class) * model.getReactant(catalyst).get(NUMBER_OF_LEVELS).as(Integer.class);
						break;
					case 1:
						levelsScaleFactor = 1.0 * model.getReactant(catalyst).get(NUMBER_OF_LEVELS).as(Integer.class);
						break;
					case 2:
						String e1, e2;
						e1 = nodeNameToId.get(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1"));
						e2 = nodeNameToId.get(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2"));
						levelsScaleFactor = 1.0 / model.getReactant(reactant).get(NUMBER_OF_LEVELS).as(Integer.class) * model.getReactant(e1).get(NUMBER_OF_LEVELS).as(Integer.class) * model.getReactant(e2).get(NUMBER_OF_LEVELS).as(Integer.class);
						//If we wanted, we could also "simplify" the multiplication above by trying to identify the substrate (called "reactant") with one of the two upstream entities ("e1" and "e2"), but the result would be the same
						break;
					default:
						levelsScaleFactor = 1.0;
						break;
				}
			} else {
				//scenario = scenarios[0];
				edgeAttributes.setAttribute(edge.getIdentifier(), SCENARIO, 0);
				int increment;
				if (edgeAttributes.hasAttribute(edge.getIdentifier(), INCREMENT)) {
					increment = edgeAttributes.getIntegerAttribute(edge.getIdentifier(), INCREMENT);
				} else {
					if (edge.getSource().equals(edge.getTarget())) {
						increment = -1;
					} else {
						increment = 1;
					}
				}
				String res;
				String edgeName;
				StringBuilder reactionName = new StringBuilder();
				if (nodeAttributes.hasAttribute(edge.getSource().getIdentifier(), CANONICAL_NAME)) {
					res = nodeAttributes.getStringAttribute(edge.getSource().getIdentifier(), CANONICAL_NAME);
					reactionName.append(res);
					
					if (increment >= 0) {
						reactionName.append(" --> ");
					} else {
						reactionName.append(" --| ");
					}
					if (nodeAttributes.hasAttribute(edge.getTarget().getIdentifier(), CANONICAL_NAME)) {
						res = nodeAttributes.getStringAttribute(edge.getTarget().getIdentifier(), CANONICAL_NAME);
						reactionName.append(res);
						edgeName = reactionName.toString();
					} else {
						edgeName = "" + edge.getSource() + " --> " + edge.getTarget();
					}
				} else {
					edgeName = "" + edge.getSource() + " --> " + edge.getTarget();
				}
				throw new InatException("The reaction " + edgeName + " has an invalid scenario setting (" + scenarioIdx + "). Now I set it to the first available: please set the correct parameters.");
			}
			//System.err.println("Levels scale factor = " + levelsScaleFactor);
			r.let(Model.Properties.SCENARIO).be(scenarioIdx);
			r.let(LEVELS_SCALE_FACTOR + "_reaction").be(levelsScaleFactor);
			r.let(Model.Properties.SCENARIO_PARAMETER_K).be(edgeAttributes.getDoubleAttribute(edge.getIdentifier(), Model.Properties.SCENARIO_PARAMETER_K));
			
			String[] parameters = scenario.listVariableParameters();
			HashMap<String, Double> scenarioParameterValues = new HashMap<String, Double>();
			for (int j = 0;j < parameters.length;j++) {
				Double parVal = edgeAttributes.getDoubleAttribute(edge.getIdentifier(), parameters[j]);
				if (parVal != null) {
					scenario.setParameter(parameters[j], parVal);
					scenarioParameterValues.put(parameters[j], parVal);
				} else {
					//checkParameters should make sure that each parameter is present, at least with its default value
				}
			}
			r.let(Model.Properties.SCENARIO_CFG).be(new ScenarioCfg(scenarioIdx, scenarioParameterValues));
			
			
			double uncertainty = unc; //edgeAttributes.getIntegerAttribute(edge.getIdentifier(), UNCERTAINTY); //We want to use the uncertainty given under the options, not the uncertainty in the reaction.
			//Also: while we are here, we delete the UNCERTAINTY attribute if we find it
			if (edgeAttributes.hasAttribute(edge.getIdentifier(), UNCERTAINTY)) {
				edgeAttributes.deleteAttribute(edge.getIdentifier(), UNCERTAINTY);
			}
			
			if (scenarioIdx == 2) { //actually, they are both catalysts
				String cata, reac;
				cata = nodeNameToId.get(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1"));
				reac = nodeNameToId.get(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2"));
				r.let(CATALYST).be(cata);
				r.let(REACTANT).be(reac);
				if (!model.getReactant(cata).get(NUMBER_OF_LEVELS).isNull()) {
					nLevelsR1 = model.getReactant(cata).get(NUMBER_OF_LEVELS).as(Integer.class);
				} else {
					nLevelsR1 = model.getProperties().get(NUMBER_OF_LEVELS).as(Integer.class);
				}
				if (!model.getReactant(reac).get(NUMBER_OF_LEVELS).isNull()) {
					nLevelsR2 = model.getReactant(reac).get(NUMBER_OF_LEVELS).as(Integer.class);
				} else {
					nLevelsR2 = model.getProperties().get(NUMBER_OF_LEVELS).as(Integer.class);
				}
				String out = nodeNameToId.get(edge.getTarget().getIdentifier());
				r.let(OUTPUT_REACTANT).be(out);
				//levelsScaleFactor /= 2*nodeAttributes.getDoubleAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2"), LEVELS_SCALE_FACTOR);
			} else {
				r.let(OUTPUT_REACTANT).be(reactant);
			}
			
			/*boolean activatingReaction = true;
			if (edgeAttributes.getIntegerAttribute(edge.getIdentifier(), INCREMENT) > 0) {
				activatingReaction = true;
			} else {
				activatingReaction = false;
			}*/
			
			String reactionAlias;
			reactionAlias = model.getReactant(r.get(CATALYST).as(String.class)).get(REACTANT_ALIAS).as(String.class);
			if (scenarioIdx == 2) {
				reactionAlias += " AND " + model.getReactant(r.get(REACTANT).as(String.class)).get(REACTANT_ALIAS).as(String.class);
			}
			if (r.get(INCREMENT).as(Integer.class) >= 0) {
				reactionAlias += " --> ";
			} else {
				reactionAlias += " --| ";
			}
			if (scenarioIdx == 2) {
				reactionAlias += model.getReactant(r.get(OUTPUT_REACTANT).as(String.class)).get(REACTANT_ALIAS).as(String.class);
			} else {
				reactionAlias += model.getReactant(r.get(REACTANT).as(String.class)).get(REACTANT_ALIAS).as(String.class);
			}
			r.let(REACTION_ALIAS).be(reactionAlias);
			
			
			boolean activeR1 = true, activeR2 = false;
			boolean reactant1IsDownstream = false, reactant2IsDownstream = true;
			
			if (scenarioIdx == 0 || scenarioIdx == 1) {
				activeR1 = true;
				if (edgeAttributes.getIntegerAttribute(edge.getIdentifier(), INCREMENT) >= 0) {
					activeR2 = false;
				} else {
					activeR2 = true;
				}
			} else if (scenarioIdx == 2) {
				activeR1 = edgeAttributes.getBooleanAttribute(edge.getIdentifier(), Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E1");
				activeR2 = edgeAttributes.getBooleanAttribute(edge.getIdentifier(), Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E2");
				r.let(Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E1").be(activeR1);
				r.let(Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E2").be(activeR2);
				reactant1IsDownstream = r.get(CATALYST).as(String.class).equals(r.get(OUTPUT_REACTANT).as(String.class));
				reactant2IsDownstream = r.get(REACTANT).as(String.class).equals(r.get(OUTPUT_REACTANT).as(String.class));
			} else {
				//TODO: this should never happen, because we have already made these checks
				activeR1 = activeR2 = true;
			}
			r.let(Model.Properties.R1_IS_DOWNSTREAM).be(reactant1IsDownstream);
			r.let(Model.Properties.R2_IS_DOWNSTREAM).be(reactant2IsDownstream);
			
			if (generateTables) {
				//System.err.println("Calcolo i tempi per la reazione " + nodeAttributes.getStringAttribute(edge.getSource().getIdentifier(), CANONICAL_NAME) + " -- " + nodeAttributes.getStringAttribute(edge.getTarget().getIdentifier(), CANONICAL_NAME) + ", con activeR1 = " + activeR1 + ", activeR2 = " + activeR2);
				List<Double> times = scenario.generateTimes(1 + nLevelsR1, activeR1, reactant1IsDownstream, 1 + nLevelsR2, activeR2, reactant2IsDownstream);
				Table timesLTable = new Table(nLevelsR2 + 1, nLevelsR1 + 1);
				Table timesUTable = new Table(nLevelsR2 + 1, nLevelsR1 + 1);
				
				int minTime = Integer.MAX_VALUE,
					maxTime = Integer.MIN_VALUE;
				for (int j = 0; j < nLevelsR2 + 1; j++) {
					for (int k = 0; k < nLevelsR1 + 1; k++) {
						Double t = times.get(j * (nLevelsR1 + 1) + k);
						if (Double.isInfinite(t)) {
							timesLTable.set(j, k, VariablesModel.INFINITE_TIME);
							timesUTable.set(j, k, VariablesModel.INFINITE_TIME);
						} else if (uncertainty == 0) {
							timesLTable.set(j, k, (int)Math.round(secStepFactor * levelsScaleFactor * t));
							timesUTable.set(j, k, (int)Math.round(secStepFactor * levelsScaleFactor * t));
							if (timesLTable.get(j, k) < minTime) {
								minTime = timesLTable.get(j, k);
							}
							if (timesUTable.get(j, k) > maxTime) {
								maxTime = timesUTable.get(j, k);
							}
						} else {
							timesLTable.set(j, k, Math.max(0, (int)Math.round(secStepFactor * levelsScaleFactor * t * (1 - uncertainty / 100.0))));
							timesUTable.set(j, k, Math.max(0, (int)Math.round(secStepFactor * levelsScaleFactor * t * (1 + uncertainty / 100.0))));
							if (timesLTable.get(j, k) < minTime) {
								minTime = timesLTable.get(j, k);
							}
							if (timesUTable.get(j, k) > maxTime) {
								maxTime = timesUTable.get(j, k);
							}
						}
					}
				}
				if (minTime == Integer.MAX_VALUE) {
					minTime = VariablesModel.INFINITE_TIME;
				}
				if (maxTime == Integer.MIN_VALUE) {
					maxTime = VariablesModel.INFINITE_TIME;
				}
				r.let(TIMES_L).be(timesLTable);
				r.let(TIMES_U).be(timesUTable);
				r.let(MINIMUM_DURATION).be(minTime);
				r.let(MAXIMUM_DURATION).be(maxTime);
				if (minTime != VariablesModel.INFINITE_TIME && (minTimeModel == Integer.MAX_VALUE || minTime < minTimeModel)) {
					minTimeModel = minTime;
				}
				if (maxTime != VariablesModel.INFINITE_TIME && (maxTimeModel == Integer.MIN_VALUE || maxTime > maxTimeModel)) {
					maxTimeModel = maxTime;
				}
			} else { //No tabels were requested, so find only min and max time to be used in the computation of reaction activityRatio
				Double maxValueFormula = Double.POSITIVE_INFINITY, minValueFormula;
				int maxValueInTables, minValueInTables;
				int colMax, rowMax, incrementColMax, incrementRowMax, colMin, rowMin;
				if (activeR1 && !activeR2) {
					colMax = 0; rowMax = nLevelsR2; //The largest number should be in the lower-left corner (the first not to be considered INFINITE_TIME)
					incrementColMax = 1; incrementRowMax = -1;
					colMin = nLevelsR1; rowMin = 0; //The smallest number should be in the top-right corner
				} else if (activeR1 && activeR2) {
					colMax = 0; rowMax = 0; //The largest number should be in the top-left corner (the first != INF)
					incrementColMax = 1; incrementRowMax = 1;
					colMin = nLevelsR1; rowMin = nLevelsR2; //The smallest number should be in the lower right corner
				} else if (!activeR1 && !activeR2) {
					colMax = nLevelsR1; rowMax = nLevelsR2; //The largest number should be in the lower right corner (the first != INF)
					incrementColMax = -1; incrementRowMax = -1;
					colMin = 0; rowMin = 0; //The smallest number should be in the top-left corner
				} else if (!activeR1 && activeR2) {
					colMax = nLevelsR1; rowMax = 0; //The largest number should be in the top-right corner (the first != INF)
					incrementColMax = -1; incrementRowMax = 1;
					colMin = 0; rowMin = nLevelsR2; //The smallest number should be in the lower-left corner
				} else {
					//TODO: this should never happen, as we have already considered all 4 possibilities for activeR1 and activeR2
					colMax = rowMax = colMin = rowMin = incrementColMax = incrementRowMax = 1;
				}
				minValueFormula = scenario.computeFormula(colMin, nLevelsR1, activeR1, rowMin, nLevelsR2, activeR2);
				while (Double.isInfinite(maxValueFormula) && colMax >= 0 && colMax <= nLevelsR1 && rowMax >= 0 && rowMax <= nLevelsR2) {
					colMax = colMax + incrementColMax;
					rowMax = rowMax + incrementRowMax;
					maxValueFormula = scenario.computeFormula(colMax, nLevelsR1, activeR1, rowMax, nLevelsR2, activeR2);
				}
				
				if (Double.isInfinite(minValueFormula)) {
					minValueInTables = VariablesModel.INFINITE_TIME;
				} else {
					if (uncertainty == 0) {
						minValueInTables = Math.max(0, (int)Math.round(secStepFactor * levelsScaleFactor * minValueFormula));
					} else {
						minValueInTables = Math.max(0, (int)Math.round(secStepFactor * levelsScaleFactor * minValueFormula * (1 - uncertainty / 100.0)));
					}
				}
				if (Double.isInfinite(maxValueFormula)) {
					maxValueInTables = VariablesModel.INFINITE_TIME;
				} else {
					if (uncertainty == 0) {
						maxValueInTables = Math.max(0, (int)Math.round(secStepFactor * levelsScaleFactor * maxValueFormula));
					} else {
						maxValueInTables = Math.max(0, (int)Math.round(secStepFactor * levelsScaleFactor * maxValueFormula * (1 + uncertainty / 100.0)));
					}
				}
				r.let(MINIMUM_DURATION).be(minValueInTables);
				r.let(MAXIMUM_DURATION).be(maxValueInTables);
				if (minValueInTables != VariablesModel.INFINITE_TIME && (minTimeModel == Integer.MAX_VALUE || minValueInTables < minTimeModel)) {
					minTimeModel = minValueInTables;
				}
				if (maxValueInTables != VariablesModel.INFINITE_TIME && (maxTimeModel == Integer.MIN_VALUE || maxValueInTables > maxTimeModel)) {
					maxTimeModel = maxValueInTables;
				}
			}
			
			String r1Id = r.get(CATALYST).as(String.class);
			String r2Id = r.get(REACTANT).as(String.class);
			String rOutput = r.get(OUTPUT_REACTANT).as(String.class);
			r.setId(r1Id + "_" + r2Id + ((rOutput.equals(r2Id))? "" : "_" + rOutput));
			
			model.add(r);
		}
		if (minTimeModel == Integer.MAX_VALUE) {
			minTimeModel = VariablesModel.INFINITE_TIME;
		}
		model.getProperties().let(MINIMUM_DURATION).be(minTimeModel);
		if (maxTimeModel == Integer.MIN_VALUE) {
			maxTimeModel = VariablesModel.INFINITE_TIME;
		}
		model.getProperties().let(MAXIMUM_DURATION).be(maxTimeModel);
		
		/*This should not be necessary any more, as we do that in checkParameters()
		//check that the number of levels is present in each reactant
		Integer defNumberOfLevels = model.getProperties().get(NUMBER_OF_LEVELS).as(Integer.class);
		for (Reactant r : model.getReactants()) {
			Integer nLvl = r.get(NUMBER_OF_LEVELS).as(Integer.class);
			if (nLvl == null) {
				Property nameO = r.get(REACTANT_ALIAS);
				String name;
				if (nameO == null) {
					name = r.getId();
				} else {
					name = nameO.as(String.class);
				}
				String inputLevels = JOptionPane.showInputDialog("Missing number of levels for reactant \"" + name + "\" (" + r.getId() + ").\nPlease insert the max number of levels for \"" + name + "\"", defNumberOfLevels);
				if (inputLevels != null) {
					try {
						nLvl = new Integer(inputLevels);
					} catch (Exception ex) {
						nLvl = defNumberOfLevels;
					}
				} else {
					nLvl = defNumberOfLevels;
				}
				r.let(NUMBER_OF_LEVELS).be(nLvl);
				//System.err.println("Numbero di livelli di " + r.get("cytoscape id").as(String.class) + " = " + nLvl);
				nodeAttributes.setAttribute(r.get(CYTOSCAPE_ID).as(String.class), NUMBER_OF_LEVELS, nLvl);
			}
		}*/
		
		model.mapCytoscapeIDtoReactantID = nodeNameToId;
		
		return model;
	}

	
	/**
	 * Check that all parameters are ok. If possible, ask the user to
	 * input parameters on the fly. If this is not possible, throw an
	 * exception specifying what parameters are missing.
	 */
	@SuppressWarnings("unchecked")
	private static void checkParameters(TaskMonitor monitor, Integer nMinutesToSimulate) throws InatException {
		final String NUMBER_OF_LEVELS = Model.Properties.NUMBER_OF_LEVELS, //The total number of levels for a node (=reactant), or for the whole network (the name of the property is the same)
		SECONDS_PER_POINT = Model.Properties.SECONDS_PER_POINT, //The number of real-life seconds represented by a single UPPAAL time unit
		SECS_POINT_SCALE_FACTOR = Model.Properties.SECS_POINT_SCALE_FACTOR, //The scale factor for the UPPAAL time settings, allowing to keep the same scenario parameters, while varying the "density" of simulation sample points
		LEVELS_SCALE_FACTOR = Model.Properties.LEVELS_SCALE_FACTOR, //The scale factor used by each reaction to counterbalance the change in number of levels for the reactants.
		INCREMENT = Model.Properties.INCREMENT, //The increment in activity caused by a reaction on its downstream reactant
		SCENARIO = Model.Properties.SCENARIO, //The id of the scenario used to set the parameters for an edge (=reaction)
		CANONICAL_NAME = Model.Properties.CANONICAL_NAME, //The name of a reactant displayed to the user
		INITIAL_LEVEL = Model.Properties.INITIAL_LEVEL, //The starting activity level of a reactant
		UNCERTAINTY = Model.Properties.UNCERTAINTY, //The uncertainty about the parameters setting for an edge(=reaction)
		ENABLED = Model.Properties.ENABLED, //Whether the node/edge is enabled. Influences the display of that node/edge thanks to the discrete Visual Mapping defined by AugmentAction
		PLOTTED = Model.Properties.PLOTTED; //Whether the node is plotted in the graph. Default: yes
		
		final int VERY_LARGE_TIME_VALUE = 1073741822; //This is NOT a random number: it is the maximum number that can currently be input to UPPAAL
		
		CyNetwork network = Cytoscape.getCurrentNetwork();
		CyAttributes networkAttributes = Cytoscape.getNetworkAttributes();
		CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
		CyAttributes edgeAttributes = Cytoscape.getEdgeAttributes();
		
		
		//============================== FIRST PART: CHECK THAT ALL PROPERTIES ARE SET =====================================
		//TODO: we could collect the list of all things that were set automatically and show them before continuing with the
		//generation of the model. Alternatively, we could throw exceptions like bullets for any slight misbehavior =)
		//Another alternative is to collect the list of what we want to change, and actually make the changes only after the
		//user has approved them. Otherwise, interrupt the analysis by throwing exception.
		
		if (!networkAttributes.hasAttribute(network.getIdentifier(), NUMBER_OF_LEVELS)) {
			//throw new InatException("Network attribute '" + NUMBER_OF_LEVELS + "' is missing.");
			int defaultNLevels = 15;
			String inputLevels = JOptionPane.showInputDialog((JTask)monitor, "Missing number of levels for the network. Please insert the max number of levels", defaultNLevels);
			Integer nLvl;
			if (inputLevels != null) {
				try {
					nLvl = new Integer(inputLevels);
				} catch (Exception ex) {
					nLvl = defaultNLevels;
				}
			} else {
				nLvl = defaultNLevels;
			}
			networkAttributes.setAttribute(network.getIdentifier(), NUMBER_OF_LEVELS, nLvl);
		}
		
		if (!networkAttributes.hasAttribute(network.getIdentifier(), SECONDS_PER_POINT)) {
			//throw new InatException("Network attribute '" + SECONDS_PER_POINT + "' is missing.");
			double defaultSecondsPerPoint = 1;
			String inputSecs = JOptionPane.showInputDialog((JTask)monitor, "Missing number of seconds per point for the network.\nPlease insert the number of real-life seconds a simulation point will represent", defaultSecondsPerPoint);
			Double nSecPerPoint;
			if (inputSecs != null) {
				try {
					nSecPerPoint = new Double(inputSecs);
				} catch (Exception ex) {
					nSecPerPoint = defaultSecondsPerPoint;
				}
			} else {
				nSecPerPoint = defaultSecondsPerPoint;
			}
			networkAttributes.setAttribute(network.getIdentifier(), SECONDS_PER_POINT, nSecPerPoint);
		}
		
		double secStepFactor;
		if (networkAttributes.hasAttribute(network.getIdentifier(), SECS_POINT_SCALE_FACTOR)) {
			secStepFactor = networkAttributes.getDoubleAttribute(network.getIdentifier(), SECS_POINT_SCALE_FACTOR);
		} else {
			secStepFactor = 1.0;
			networkAttributes.setAttribute(network.getIdentifier(), SECS_POINT_SCALE_FACTOR, secStepFactor);
		}
		
		boolean noReactantsPlotted = true;
		Iterator<Node> nodes = (Iterator<Node>) network.nodesIterator();
		for (@SuppressWarnings("unused")
		int i = 0; nodes.hasNext(); i++) {
			Node node = nodes.next();
			boolean enabled = false;
			if (!nodeAttributes.hasAttribute(node.getIdentifier(), ENABLED)) {
				nodeAttributes.setAttribute(node.getIdentifier(), ENABLED, true);
				enabled = true;
			} else {
				enabled = nodeAttributes.getBooleanAttribute(node.getIdentifier(), ENABLED);
			}
			
			if (!enabled) continue;
			
			if (!nodeAttributes.hasAttribute(node.getIdentifier(), PLOTTED)) {
				nodeAttributes.setAttribute(node.getIdentifier(), PLOTTED, true);
				if (enabled) {
					noReactantsPlotted = false;
				}
			} else if (enabled && nodeAttributes.getBooleanAttribute(node.getIdentifier(), PLOTTED)) {
				noReactantsPlotted = false;
			}
			
			if (!nodeAttributes.hasAttribute(node.getIdentifier(), NUMBER_OF_LEVELS)) {
				nodeAttributes.setAttribute(node.getIdentifier(), NUMBER_OF_LEVELS, networkAttributes.getIntegerAttribute(network.getIdentifier(), NUMBER_OF_LEVELS));
			}
			
			if (!nodeAttributes.hasAttribute(node.getIdentifier(), INITIAL_LEVEL)) {
				//throw new InatException("Node attribute 'initialConcentration' is missing on '" + node.getIdentifier() + "'");
				nodeAttributes.setAttribute(node.getIdentifier(), INITIAL_LEVEL, 0);
			}
			
//			if (!nodeAttributes.hasAttribute(node.getIdentifier(), LEVELS_SCALE_FACTOR)) {
//				nodeAttributes.setAttribute(node.getIdentifier(), LEVELS_SCALE_FACTOR, nodeAttributes.getDoubleAttribute(node.getIdentifier(), NUMBER_OF_LEVELS) / 15.0);
//			}
		}
		
		/*if (noReactantsPlotted && !smcUppaal.isSelected()) {
			JOptionPane.showMessageDialog((JTask)this.monitor, "No reactants selected for plot: select at least one reactant to be plotted in the graph.", "Error", JOptionPane.ERROR_MESSAGE); 
			throw new InatException("No reactants selected for plot: select at least one reactant to be plotted in the graph.");
		}*/
		if (noReactantsPlotted) {
			if (JOptionPane.showConfirmDialog((JTask)monitor, "No graphs will be shown. Do you still want to continue?", "No reactants selected for plotting", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
				throw new InatException("Model generation cancelled by the user");
			}
		}
		
		Iterator<Edge> edges = (Iterator<Edge>) network.edgesIterator();
		for (@SuppressWarnings("unused")
		int i = 0; edges.hasNext(); i++) {
			Edge edge = edges.next();
			boolean enabled;
			if (!edgeAttributes.hasAttribute(edge.getIdentifier(), ENABLED)) {
				edgeAttributes.setAttribute(edge.getIdentifier(), ENABLED, true);
				enabled = true;
			} else {
				enabled = edgeAttributes.getBooleanAttribute(edge.getIdentifier(), ENABLED);
			}
			
			if (!enabled) continue;
			
			int increment;
			if (edgeAttributes.hasAttribute(edge.getIdentifier(), INCREMENT)) {
				increment = edgeAttributes.getIntegerAttribute(edge.getIdentifier(), INCREMENT);
			} else {
				if (edge.getSource().equals(edge.getTarget())) {
					increment = -1;
				} else {
					increment = 1;
				}
			}
			
			String res;
			String edgeName;
			StringBuilder reactionName = new StringBuilder();
			if (nodeAttributes.hasAttribute(edge.getSource().getIdentifier(), CANONICAL_NAME)) {
				res = nodeAttributes.getStringAttribute(edge.getSource().getIdentifier(), CANONICAL_NAME);
				reactionName.append(res);
				
				if (increment >= 0) {
					reactionName.append(" --> ");
				} else {
					reactionName.append(" --| ");
				}
				if (nodeAttributes.hasAttribute(edge.getTarget().getIdentifier(), CANONICAL_NAME)) {
					res = nodeAttributes.getStringAttribute(edge.getTarget().getIdentifier(), CANONICAL_NAME);
					reactionName.append(res);
					edgeName = reactionName.toString();
				} else {
					edgeName = "" + edge.getSource() + " --> " + edge.getTarget();
				}
			} else {
				edgeName = "" + edge.getSource() + " --> " + edge.getTarget();
			}
			
			
			//Check that the edge has a selected scenario
			if (!edgeAttributes.hasAttribute(edge.getIdentifier(), SCENARIO)) {
				edgeAttributes.setAttribute(edge.getIdentifier(), SCENARIO, 0);
			}
			//Check that the edge has the definition of all parameters requested by the selected scenario
			//otherwise set the parameters to their default values
			Scenario scenario;
			int scenarioIdx = edgeAttributes.getIntegerAttribute(edge.getIdentifier(), SCENARIO);
			if (scenarioIdx >= 0 && scenarioIdx < Scenario.sixScenarios.length) {
				scenario = Scenario.sixScenarios[scenarioIdx];
			} else {
				//scenario = Scenario.sixScenarios[0];
				edgeAttributes.setAttribute(edge.getIdentifier(), SCENARIO, 0);
				throw new InatException("The reaction " + edgeName + " has an invalid scenario setting (" + scenarioIdx + "). Now I set it to the first available: please set the correct parameters.");
			}
			String[] paramNames = scenario.listVariableParameters();
			for (String param : paramNames) {
				if (!edgeAttributes.hasAttribute(edge.getIdentifier(), param)) {
					edgeAttributes.setAttribute(edge.getIdentifier(), param, scenario.getDefaultParameterValue(param));
				}
			}
			
			if (!edgeAttributes.hasAttribute(edge.getIdentifier(), UNCERTAINTY)) {
				edgeAttributes.setAttribute(edge.getIdentifier(), UNCERTAINTY, 0);
			}
			
			if (!edgeAttributes.hasAttribute(edge.getIdentifier(), INCREMENT)) {
				edgeAttributes.setAttribute(edge.getIdentifier(), INCREMENT, 1);
			}
			
			if (scenarioIdx == 2) {
				if (!edgeAttributes.hasAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1")) {
					edgeAttributes.setAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1", edge.getSource().getIdentifier());
				}
				if (!edgeAttributes.hasAttribute(edge.getIdentifier(), Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E1")) {
					edgeAttributes.setAttribute(edge.getIdentifier(), Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E1", true);
				}
				if (!edgeAttributes.hasAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2")) {
					edgeAttributes.setAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2", edge.getTarget().getIdentifier());
				}
				if (!edgeAttributes.hasAttribute(edge.getIdentifier(), Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E2")) {
					edgeAttributes.setAttribute(edge.getIdentifier(), Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E2", true);
				}
				StringBuilder nameBuilder = new StringBuilder();
				if (nodeAttributes.hasAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1"), Model.Properties.CANONICAL_NAME)) {
					nameBuilder.append(nodeAttributes.getStringAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1"), Model.Properties.CANONICAL_NAME));
				} else {
					nameBuilder.append(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1"));
				}
				nameBuilder.append(" AND ");
				if (nodeAttributes.hasAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2"), Model.Properties.CANONICAL_NAME)) {
					nameBuilder.append(nodeAttributes.getStringAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2"), Model.Properties.CANONICAL_NAME));
				} else {
					nameBuilder.append(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2"));
				}
				if (increment >= 0) {
					nameBuilder.append(" --> ");
				} else {
					nameBuilder.append(" --| ");
				}
				if (nodeAttributes.hasAttribute(edge.getTarget().getIdentifier(), Model.Properties.CANONICAL_NAME)) {
					nameBuilder.append(nodeAttributes.getStringAttribute(edge.getTarget().getIdentifier(), Model.Properties.CANONICAL_NAME));
				} else {
					nameBuilder.append(edge.getTarget().getIdentifier());
				}
				edgeName = nameBuilder.toString();
			}
			
			switch (scenarioIdx) {
				case 0:
				case 1:
					if (!nodeAttributes.getBooleanAttribute(edge.getSource().getIdentifier(), Model.Properties.ENABLED)
						&& nodeAttributes.getBooleanAttribute(edge.getTarget().getIdentifier(), Model.Properties.ENABLED)) {
						throw new InatException("Please check that reactant \"" + nodeAttributes.getStringAttribute(edge.getSource().getIdentifier(), Model.Properties.CANONICAL_NAME) + "\" is enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
					} else if (!nodeAttributes.getBooleanAttribute(edge.getSource().getIdentifier(), Model.Properties.ENABLED)
							&& !nodeAttributes.getBooleanAttribute(edge.getTarget().getIdentifier(), Model.Properties.ENABLED)) {
						throw new InatException("Please check that both reactants \"" + nodeAttributes.getStringAttribute(edge.getSource().getIdentifier(), Model.Properties.CANONICAL_NAME) + "\" and \"" + nodeAttributes.getStringAttribute(edge.getTarget().getIdentifier(), Model.Properties.CANONICAL_NAME) + "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
					} else if (nodeAttributes.getBooleanAttribute(edge.getSource().getIdentifier(), Model.Properties.ENABLED)
							&& !nodeAttributes.getBooleanAttribute(edge.getTarget().getIdentifier(), Model.Properties.ENABLED)) {
						throw new InatException("Please check that reactant \"" + nodeAttributes.getStringAttribute(edge.getTarget().getIdentifier(), Model.Properties.CANONICAL_NAME) + "\" is enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
					} else {
						//They are both enabled: all is good
					}
					break;
				case 2:
					if (!nodeAttributes.getBooleanAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1"), Model.Properties.ENABLED)
						&& nodeAttributes.getBooleanAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2"), Model.Properties.ENABLED)
						&& nodeAttributes.getBooleanAttribute(edge.getTarget().getIdentifier(), Model.Properties.ENABLED)) {
						throw new InatException("Please check that reactant \"" + nodeAttributes.getStringAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1"), Model.Properties.CANONICAL_NAME) + "\" is enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
					} else if (!nodeAttributes.getBooleanAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1"), Model.Properties.ENABLED)
							&& !nodeAttributes.getBooleanAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2"), Model.Properties.ENABLED)
							&& nodeAttributes.getBooleanAttribute(edge.getTarget().getIdentifier(), Model.Properties.ENABLED)) {
						throw new InatException("Please check that both reactants \"" + nodeAttributes.getStringAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1"), Model.Properties.CANONICAL_NAME) + "\" and \"" + nodeAttributes.getStringAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2"), Model.Properties.CANONICAL_NAME) + "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
					} else if (nodeAttributes.getBooleanAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1"), Model.Properties.ENABLED)
							&& !nodeAttributes.getBooleanAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2"), Model.Properties.ENABLED)
							&& nodeAttributes.getBooleanAttribute(edge.getTarget().getIdentifier(), Model.Properties.ENABLED)) {
						throw new InatException("Please check that reactant \"" + nodeAttributes.getStringAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2"), Model.Properties.CANONICAL_NAME) + "\" is enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
					} else if (!nodeAttributes.getBooleanAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1"), Model.Properties.ENABLED)
						&& nodeAttributes.getBooleanAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2"), Model.Properties.ENABLED)
						&& !nodeAttributes.getBooleanAttribute(edge.getTarget().getIdentifier(), Model.Properties.ENABLED)) {
						throw new InatException("Please check that both reactants \"" + nodeAttributes.getStringAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1"), Model.Properties.CANONICAL_NAME) + "\" and \"" + nodeAttributes.getStringAttribute(edge.getTarget().getIdentifier(), Model.Properties.CANONICAL_NAME) + "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
					} else if (!nodeAttributes.getBooleanAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1"), Model.Properties.ENABLED)
							&& !nodeAttributes.getBooleanAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2"), Model.Properties.ENABLED)
							&& !nodeAttributes.getBooleanAttribute(edge.getTarget().getIdentifier(), Model.Properties.ENABLED)) {
						throw new InatException("Please check that reactants \"" + nodeAttributes.getStringAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1"), Model.Properties.CANONICAL_NAME) + "\", \"" + nodeAttributes.getStringAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2"), Model.Properties.CANONICAL_NAME) + "\" and \"" + nodeAttributes.getStringAttribute(edge.getTarget().getIdentifier(), Model.Properties.CANONICAL_NAME) + "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
					} else if (nodeAttributes.getBooleanAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1"), Model.Properties.ENABLED)
							&& !nodeAttributes.getBooleanAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2"), Model.Properties.ENABLED)
							&& !nodeAttributes.getBooleanAttribute(edge.getTarget().getIdentifier(), Model.Properties.ENABLED)) {
						throw new InatException("Please check that both reactants \"" + nodeAttributes.getStringAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2"), Model.Properties.CANONICAL_NAME) + "\" and \"" + nodeAttributes.getStringAttribute(edge.getTarget().getIdentifier(), Model.Properties.CANONICAL_NAME) + "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
					} else {
						//They are both enabled: all is good
					}
					break;
				default:
					break;
			}
			
			
			if (edgeAttributes.hasAttribute(edge.getIdentifier(), LEVELS_SCALE_FACTOR)) { //Some old models have this property set as a property of the reaction instead of a property of the reactants: we collect it all in the upstream reactant, leaving 1.0 as scale for the downstream, and (important!) remove the property from the reaction
				//Double scale = edgeAttributes.getDoubleAttribute(edge.getIdentifier(), LEVELS_SCALE_FACTOR);
				//nodeAttributes.setAttribute(edge.getSource().getIdentifier(), LEVELS_SCALE_FACTOR, scale / nodeAttributes.getDoubleAttribute(edge.getSource().getIdentifier(), LEVELS_SCALE_FACTOR));
				Double scaleUpstream = nodeAttributes.getIntegerAttribute(edge.getSource().getIdentifier(), NUMBER_OF_LEVELS) / 15.0,
					   scaleDownstream = nodeAttributes.getIntegerAttribute(edge.getTarget().getIdentifier(), NUMBER_OF_LEVELS) / 15.0;
				//String nomeReazione = nodeAttributes.getStringAttribute(edge.getSource().getIdentifier(), CANONICAL_NAME) + " (" + nodeAttributes.getIntegerAttribute(edge.getSource().getIdentifier(), NUMBER_OF_LEVELS) + ") " + ((edgeAttributes.getIntegerAttribute(edge.getIdentifier(), INCREMENT) > 0) ? " --> " : " --| ") + nodeAttributes.getStringAttribute(edge.getTarget().getIdentifier(), CANONICAL_NAME) + " (" + nodeAttributes.getIntegerAttribute(edge.getTarget().getIdentifier(), NUMBER_OF_LEVELS) + ")";
				/*if (Math.abs(scaleUpstream / scaleDownstream - scale) > 1e-6) { //If the components were scaled before the reaction was introduced, then we need to modify the parameters of the reaction in order to keep things working
					//JOptionPane.showMessageDialog(null, "Errore, la scala upstream � " + scaleUpstream + ", la scala downstream � " + scaleDownstream + ",\nil / viene " + (scaleUpstream / scaleDownstream) + ",\nil * viene " + (scaleUpstream * scaleDownstream) + ",\nma la scala attuale della reazione � " + scale, nomeReazione, JOptionPane.WARNING_MESSAGE);
					
					//Counterbalance the scale introduced by the two scales
					double factor = scale * scaleDownstream / scaleUpstream;
					scenarioIdx = edgeAttributes.getIntegerAttribute(edge.getIdentifier(), Model.Properties.SCENARIO);
					if (scenarioIdx == 0) { //Scenario 1-2-3-4
						Double parameter = edgeAttributes.getDoubleAttribute(edge.getIdentifier(), Model.Properties.SCENARIO_ONLY_PARAMETER);
						parameter /= factor;
						edgeAttributes.setAttribute(edge.getIdentifier(), Model.Properties.SCENARIO_ONLY_PARAMETER, parameter);
					} else if (scenarioIdx == 1) { //Scenario 5
						Double k2km = edgeAttributes.getDoubleAttribute(edge.getIdentifier(), Model.Properties.SCENARIO_PARAMETER_K2_KM);
						k2km /= factor;
						edgeAttributes.setAttribute(edge.getIdentifier(), Model.Properties.SCENARIO_PARAMETER_K2_KM, k2km);
					} else if (scenarioIdx == 2) { //Scenario 6
						Double k2 = edgeAttributes.getDoubleAttribute(edge.getIdentifier(), Model.Properties.SCENARIO_PARAMETER_K2);
						k2 /= factor;
						edgeAttributes.setAttribute(edge.getIdentifier(), Model.Properties.SCENARIO_PARAMETER_K2, k2);
					}
				}*/
				/*} else {
					JOptionPane.showMessageDialog(null, "Tutto ok! La scala upstream � " + scaleUpstream + ", la scala downstream � " + scaleDownstream + ",\nil / viene " + (scaleUpstream / scaleDownstream) + ",\nil * viene " + (scaleUpstream * scaleDownstream) + ",\nma la scala attuale della reazione � " + scale, nomeReazione, JOptionPane.INFORMATION_MESSAGE);
				}*/
				nodeAttributes.setAttribute(edge.getSource().getIdentifier(), LEVELS_SCALE_FACTOR, scaleUpstream);
				nodeAttributes.setAttribute(edge.getTarget().getIdentifier(), LEVELS_SCALE_FACTOR, scaleDownstream);
				edgeAttributes.deleteAttribute(edge.getIdentifier(), LEVELS_SCALE_FACTOR);
			}
			/*if (!edgeAttributes.hasAttribute(edge.getIdentifier(), LEVELS_SCALE_FACTOR)) {  //This is commented because edges should not have this property anymore
				edgeAttributes.setAttribute(edge.getIdentifier(), LEVELS_SCALE_FACTOR, 1.0);
			}*/
		}
		
		
		//============ SECOND PART: MAKE SURE THAT REACTION PARAMETERS IN COMBINATION WITH TIME POINTS DENSITY (SECONDS/POINT) DON'T GENERATE BAD PARAMETERS FOR UPPAAL =============
		
		double minSecStep = Double.NEGATIVE_INFINITY, maxSecStep = Double.POSITIVE_INFINITY, //The lower bound of the "valid" interval for secs/step (minSecStep) is the maximum of the lower bounds we find for it, while the upper bound (maxSecStep) is the minimum of all upper bounds. This is why we compute them in this apparently strange way
			   secPerStep = networkAttributes.getDoubleAttribute(network.getIdentifier(), SECONDS_PER_POINT);
		
		
		edges = (Iterator<Edge>) network.edgesIterator();
		while (edges.hasNext()) {
			Edge edge = edges.next();
			if (!edgeAttributes.getBooleanAttribute(edge.getIdentifier(), ENABLED)) continue;
			double levelsScaleFactor;// = nodeAttributes.getDoubleAttribute(edge.getSource().getIdentifier(), LEVELS_SCALE_FACTOR) / nodeAttributes.getDoubleAttribute(edge.getTarget().getIdentifier(), LEVELS_SCALE_FACTOR);
										//edgeAttributes.getDoubleAttribute(edge.getIdentifier(), LEVELS_SCALE_FACTOR); //Now the scale factor due to the nodes' scale is a property of each node
			String r1Id = edge.getSource().getIdentifier(),
				   r2Id = edge.getTarget().getIdentifier();
			double scaleFactorR1 = nodeAttributes.getIntegerAttribute(edge.getSource().getIdentifier(), NUMBER_OF_LEVELS),
				   scaleFactorR2 = nodeAttributes.getIntegerAttribute(edge.getTarget().getIdentifier(), NUMBER_OF_LEVELS);

			Scenario[] scenarios = Scenario.sixScenarios;
			Integer scenarioIdx = edgeAttributes.getIntegerAttribute(edge.getIdentifier(), SCENARIO);
			if (scenarioIdx == null) {
				//TODO: show the editing window
				scenarioIdx = 0;
			}
			
			switch (scenarioIdx) {
				case 0:
					levelsScaleFactor = 1 / scaleFactorR2 * scaleFactorR1;
					break;
				case 1:
					levelsScaleFactor = scaleFactorR1;
					break;
				case 2:
					double scaleFactorE1 = nodeAttributes.getIntegerAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1"), NUMBER_OF_LEVELS),
						   scaleFactorE2 = nodeAttributes.getIntegerAttribute(edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2"), NUMBER_OF_LEVELS);
					levelsScaleFactor = 1 / scaleFactorR2 * scaleFactorE1 * scaleFactorE2;
					//If we wanted, we could also "simplify" the multiplication above by trying to identify the substrate (called "r2Id") with one of the two upstream entities ("e1" and "e2"), but the result would be the same
					break;
				default:
					levelsScaleFactor = 1.0;
					break;
			}
			
			int increment;
			increment = edgeAttributes.getIntegerAttribute(edge.getIdentifier(), INCREMENT);
			Scenario scenario;
			if (scenarioIdx >= 0 && scenarioIdx < scenarios.length) {
				scenario = scenarios[scenarioIdx];
			} else {
				//scenario = scenarios[0];
				edgeAttributes.setAttribute(edge.getIdentifier(), SCENARIO, 0);
				String res;
				String edgeName;
				StringBuilder reactionName = new StringBuilder();
				if (nodeAttributes.hasAttribute(edge.getSource().getIdentifier(), CANONICAL_NAME)) {
					res = nodeAttributes.getStringAttribute(edge.getSource().getIdentifier(), CANONICAL_NAME);
					reactionName.append(res);
					
					if (increment >= 0) {
						reactionName.append(" --> ");
					} else {
						reactionName.append(" --| ");
					}
					if (nodeAttributes.hasAttribute(edge.getTarget().getIdentifier(), CANONICAL_NAME)) {
						res = nodeAttributes.getStringAttribute(edge.getTarget().getIdentifier(), CANONICAL_NAME);
						reactionName.append(res);
						edgeName = reactionName.toString();
					} else {
						edgeName = "" + edge.getSource() + " --> " + edge.getTarget();
					}
				} else {
					edgeName = "" + edge.getSource() + " --> " + edge.getTarget();
				}
				throw new InatException("The reaction " + edgeName + " has an invalid scenario setting (" + scenarioIdx + "). Now I set it to the first available: please set the correct parameters.");
			}

			if (scenarioIdx == 2) {
				r1Id = edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E1");
				r2Id = edgeAttributes.getStringAttribute(edge.getIdentifier(), Model.Properties.REACTANT_ID + "E2");
			}
		
			int nLevelsR1, nLevelsR2;
			if (nodeAttributes.hasAttribute(r1Id, NUMBER_OF_LEVELS)) {
				nLevelsR1 = nodeAttributes.getIntegerAttribute(r1Id, NUMBER_OF_LEVELS);
			} else {
				//TODO: il controllo per la presenza dei livelli non l'ho ancora fatto a questo punto!!
				//suggerisco di fare una funzione apposta per fare tutta la serie di controlli che facciamo all'inizio di getModel
				//a cui aggiungiamo in coda questo controllo sugli uni (e numeri troppo grandi)!
				nLevelsR1 = networkAttributes.getIntegerAttribute(network.getIdentifier(), NUMBER_OF_LEVELS);
			}
			if (nodeAttributes.hasAttribute(r2Id, NUMBER_OF_LEVELS)) {
				nLevelsR2 = nodeAttributes.getIntegerAttribute(r2Id, NUMBER_OF_LEVELS);
			} else {
				nLevelsR2 = networkAttributes.getIntegerAttribute(network.getIdentifier(), NUMBER_OF_LEVELS);
			}
			
			
			boolean activeR1 = true, activeR2 = false;
			if (scenarioIdx == 0) {
				activeR1 = activeR2 = true;
			} else if (scenarioIdx == 1) {
				activeR1 = true;
				if (increment >= 0) {
					activeR2 = false;
				} else {
					activeR2 = true;
				}
			} else if (scenarioIdx == 2) {
				activeR1 = edgeAttributes.getBooleanAttribute(edge.getIdentifier(), Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E1");
				activeR2 = edgeAttributes.getBooleanAttribute(edge.getIdentifier(), Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E2");
			}
			
			String[] parameters = scenario.listVariableParameters();
			for (int j = 0;j < parameters.length;j++) {
				Double parVal = edgeAttributes.getDoubleAttribute(edge.getIdentifier(), parameters[j]);
				if (parVal != null) {
					scenario.setParameter(parameters[j], parVal);
				} else {
					//TODO: show the editing window
				}
			}
			
			double uncertainty;
			if (edgeAttributes.hasAttribute(edge.getIdentifier(), UNCERTAINTY)) {
				uncertainty = edgeAttributes.getIntegerAttribute(edge.getIdentifier(), UNCERTAINTY);
			} else {
				uncertainty = 0;
			}
			
//				boolean activatingReaction = true;
//				if (edgeAttributes.hasAttribute(edge.getIdentifier(), INCREMENT) && edgeAttributes.getIntegerAttribute(edge.getIdentifier(), INCREMENT) > 0) {
//					activatingReaction = true;
//				} else {
//					activatingReaction = false;
//				}
			
			Double maxValueRate = 0.0, minValueRate, maxValueFormula = Double.POSITIVE_INFINITY, minValueFormula;
			int maxValueInTables, minValueInTables;
			int colMax, rowMax, incrementColMax, incrementRowMax, colMin, rowMin;
			if (activeR1 && !activeR2) {
				colMax = 0; rowMax = nLevelsR2; //The largest number should be in the lower-left corner (the first not to be considered INFINITE_TIME)
				incrementColMax = 1; incrementRowMax = -1;
				colMin = nLevelsR1; rowMin = 0; //The smallest number should be in the top-right corner
			} else if (activeR1 && activeR2) {
				colMax = 0; rowMax = 0; //The largest number should be in the top-left corner (the first != INF)
				incrementColMax = 1; incrementRowMax = 1;
				colMin = nLevelsR1; rowMin = nLevelsR2; //The smallest number should be in the lower right corner
			} else if (!activeR1 && !activeR2) {
				colMax = nLevelsR1; rowMax = nLevelsR2; //The largest number should be in the lower right corner (the first != INF)
				incrementColMax = -1; incrementRowMax = -1;
				colMin = 0; rowMin = 0; //The smallest number should be in the top-left corner
			} else if (!activeR1 && activeR2) {
				colMax = nLevelsR1; rowMax = 0; //The largest number should be in the top-right corner (the first != INF)
				incrementColMax = -1; incrementRowMax = 1;
				colMin = 0; rowMin = nLevelsR2; //The smallest number should be in the lower-left corner
			} else {
				//TODO: this should never happen, as we have already considered all 4 possibilities for activeR1 and activeR2
				colMax = rowMax = colMin = rowMin = incrementColMax = incrementRowMax = 1;
			}
			minValueRate = scenario.computeRate(colMin, nLevelsR1, activeR1, rowMin, nLevelsR2, activeR2);
			minValueFormula = scenario.computeFormula(colMin, nLevelsR1, activeR1, rowMin, nLevelsR2, activeR2);
			while (Double.isInfinite(maxValueFormula) && colMax >= 0 && colMax <= nLevelsR1 && rowMax >= 0 && rowMax <= nLevelsR2) {
				colMax = colMax + incrementColMax;
				rowMax = rowMax + incrementRowMax;
				maxValueRate = scenario.computeRate(colMax, nLevelsR1, activeR1, rowMax, nLevelsR2, activeR2);
				maxValueFormula = scenario.computeFormula(colMax, nLevelsR1, activeR1, rowMax, nLevelsR2, activeR2);
			}
			
			if (Double.isInfinite(minValueFormula)) {
				minValueInTables = VariablesModel.INFINITE_TIME;
			} else {
				minValueInTables = Math.max(0, (int)Math.round(secStepFactor * levelsScaleFactor * minValueFormula * (1 - uncertainty / 100.0)));
			}
			if (Double.isInfinite(maxValueFormula)) {
				maxValueInTables = VariablesModel.INFINITE_TIME;
			} else {
				maxValueInTables = Math.max(0, (int)Math.round(secStepFactor * levelsScaleFactor * maxValueFormula * (1 + uncertainty / 100.0)));
			}
			
			if (minValueInTables == 0) {
				double proposedSecStep = secPerStep / (1.1 * minValueRate / (secStepFactor * levelsScaleFactor * (1 - uncertainty / 100.0)));
				if (proposedSecStep < maxSecStep) {
					maxSecStep = proposedSecStep;
				}
			}
			if (maxValueInTables > VERY_LARGE_TIME_VALUE) {
				double proposedSecStep = secPerStep / (VERY_LARGE_TIME_VALUE * maxValueRate / (secStepFactor * levelsScaleFactor * (1 + uncertainty / 100.0)));
				if (proposedSecStep > minSecStep) {
					minSecStep = proposedSecStep;
				}
			}

		}
		
		if (nMinutesToSimulate != null && nMinutesToSimulate >= 0) { //If we were requested to perform a simulation (and not simply translate the model "out of curiosity" or for model checking), we were given also a time limit for the simulation. We need to check whether that time limit is a "large number"
			int myTimeTo = (int)(nMinutesToSimulate * 60.0 / secPerStep);
			if (myTimeTo > VERY_LARGE_TIME_VALUE) {
				double proposedSecStep = nMinutesToSimulate * 60.0 / VERY_LARGE_TIME_VALUE;
				if (proposedSecStep > minSecStep) {
					minSecStep = proposedSecStep;
				}
			}
		}
		
		if (!Double.isInfinite(minSecStep) || !Double.isInfinite(maxSecStep)) {
			System.err.println("As far as I see from the computations, a valid interval for secs/point is [" + minSecStep + ", " + maxSecStep + "]");
		}
		if (!Double.isInfinite(maxSecStep) && secPerStep > maxSecStep) {
			System.err.println("\tThe current setting is over the top: " + secPerStep + " > " + maxSecStep + ", so take " + maxSecStep);
			secPerStep = maxSecStep;
			networkAttributes.setAttribute(network.getIdentifier(), SECONDS_PER_POINT, secPerStep);
		} else {
			//System.err.println("\tNon vado sopra il massimo: " + secPerStep + " <= " + maxSecStep);
		}
		if (!Double.isInfinite(minSecStep) && secPerStep < minSecStep) { //Notice that this check is made last because it is the most important: if we set seconds/point to a value less than the computed minimum, the time values will be so large that UPPAAL will not be able to understand them, thus producing no result
			System.err.println("\tThe current seetting is under the bottom: " + secPerStep + " < " + minSecStep + ", so take " + minSecStep);
			secPerStep = minSecStep;
			networkAttributes.setAttribute(network.getIdentifier(), SECONDS_PER_POINT, secPerStep);
		} else {
			//System.err.println("\tNon vado neanche sotto il minimo: " + secPerStep + " >= " + minSecStep);
		}
		
		Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);

	}
	

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append("Model[\n");
		for (Reactant v : this.reactants.values()) {
			result.append("  " + v + "\n");
		}
		for (Reaction e : this.reactions.values()) {
			result.append("  " + e + "\n");
		}
		result.append("]");

		return result.toString();
	}
}
