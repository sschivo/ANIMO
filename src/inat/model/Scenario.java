package inat.model;
import inat.util.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a scenario for a reaction in the model.
 * There are 3 predefined scenarios, each one with its set of parameters.
 */
public class Scenario {
	private static final String SCENARIO_PARAMETER_K = Model.Properties.SCENARIO_PARAMETER_K;
//	SCENARIO_PARAMETER_KM = Model.Properties.SCENARIO_PARAMETER_KM,
//	SCENARIO_PARAMETER_K2 = Model.Properties.SCENARIO_PARAMETER_K2,
//	SCENARIO_PARAMETER_STOT = Model.Properties.SCENARIO_PARAMETER_STOT,
	protected HashMap<String, Double> parameters = new HashMap<String, Double>(); //Parameter name -> value
	protected HashMap<String, Double> defaultParameterValues = new HashMap<String, Double>(); //The defaul values of parameters
	protected HashMap<String, Pair<String, Boolean>> reactants = new HashMap<String, Pair<String, Boolean>>(); //Reactant name (i.e., E, S, E1, E2) --> <reactant ID, active/inactive> 
	public static final int INFINITE_TIME = -1; //The constant to mean that the reaction will not happen
	
	public static final Scenario[] sixScenarios = new Scenario[3]; //The default predefined scenarios
	
	public Scenario() {
	}
	
	static {
		sixScenarios[0] = new Scenario() {
			@Override
			public double computeRate(int r1Level, int nLevelsR1, boolean activeR1, int r2Level, int nLevelsR2, boolean activeR2) {
				double par = parameters.get(SCENARIO_PARAMETER_K),
					   E;
				if (activeR1) { //If we depend on active R1, the level of activity is the value of E
					E =  r1Level;
				} else { //otherwise we find the inactivity level via the total number of levels
					E = nLevelsR1 - r1Level;
				}
				double rate = par * E;
				return rate;
			}
			
			@Override
			public String[] listVariableParameters() {
				return new String[]{SCENARIO_PARAMETER_K};
			}
			
			@Override
			public String[] getReactantNames() {
				return new String[]{"E"};
			}
			
			@Override
			public String toString() {
				return "Scenario 1: k * [E]";
			}
		};
		//sixScenarios[0].setParameter(SCENARIO_ONLY_PARAMETER, 0.01);
		sixScenarios[0].setDefaultParameterValue(SCENARIO_PARAMETER_K, 0.004);
		
		sixScenarios[1] = new Scenario() {
			@Override
			public double computeRate(int r1Level, int nLevelsR1, boolean activeR1, int r2Level, int nLevelsR2, boolean activeR2) {
				double par = parameters.get(SCENARIO_PARAMETER_K),
					   S,
					   E;
				if (activeR1) { //If we depend on active R1, the activity level is the value of the parameter
					E = r1Level;
				} else { //If we depend on inactive R1, we must find the inactive part based on the total amount (n. of levels)
					E = nLevelsR1 - r1Level;
				}
				if (activeR2) { //Same reasoning here
					S = r2Level;
				} else {
					S = nLevelsR2 - r2Level;
				}
				double rate = par * E * S;
				return rate;
			}
			
			@Override
			public String[] listVariableParameters() {
				return new String[]{SCENARIO_PARAMETER_K};
			}
			
			@Override
			public String[] getReactantNames() {
				return new String[]{"E", "S"};
			}
			
			@Override
			public String toString() {
				return "Scenario 2: k * [E] * [S]";
			}
		};
		sixScenarios[1].setDefaultParameterValue(SCENARIO_PARAMETER_K, 0.004);
		
		sixScenarios[2] = new Scenario() {
			@Override
			public double computeRate(int r1Level, int nLevelsR1, boolean activeR1, int r2Level, int nLevelsR2, boolean activeR2) {
				double par = parameters.get(SCENARIO_PARAMETER_K),
					   E1,
					   E2;
				if (activeR1) { //If we depend on active R1, the activity level is the value of the parameter
					E1 = r1Level;
				} else { //If we depend on inactive R1, we must find the inactive part based on the total amount (n. of levels)
					E1 = nLevelsR1 - r1Level;
				}
				if (activeR2) { //Same reasoning here
					E2 = r2Level;
				} else {
					E2 = nLevelsR2 - r2Level;
				}
				double rate = par * E1 * E2;
				return rate;
			}
			
			@Override
			public String[] listVariableParameters() {
				return new String[]{SCENARIO_PARAMETER_K};
			}
			
			@Override
			public String[] getReactantNames() {
				return new String[]{"E1", "E2"};
			}
			
			@Override
			public String toString() {
				return "Scenario 3: k * [E1] * [E2]";
			}
		};
		sixScenarios[2].setDefaultParameterValue(SCENARIO_PARAMETER_K, 0.004);
		
	}
	
	
	@SuppressWarnings("unchecked")
	public Scenario(Scenario source) {
		this.parameters = (HashMap<String, Double>)source.parameters.clone();
	}
	
	public void setParameter(String name, Double value) {
		parameters.put(name, value);
	}
	
	public Double getParameter(String name) {
		return parameters.get(name);
	}
	
	@SuppressWarnings("unchecked")
	public HashMap<String, Double> getParameters() {
		return (HashMap<String, Double>)parameters.clone();
	}
	
	public void setDefaultParameterValue(String name, Double value) {
		defaultParameterValues.put(name, value);
		parameters.put(name, value);
	}
	
	public Double getDefaultParameterValue(String name) {
		return defaultParameterValues.get(name);
	}
	
	@SuppressWarnings("unchecked")
	public HashMap<String, Double> getDefaultParameterValues() {
		return (HashMap<String, Double>)defaultParameterValues.clone();
	}
	
	//linkedTo contains <Cytoscape reactant ID, whether we consider the active or inactive part of that reactant as parameter for this scenario>
	public void setReactant(String reactantName, Pair<String, Boolean> linkedTo) {
		reactants.put(reactantName, linkedTo);
	}
	
	public HashMap<String, Pair<String, Boolean>> getReactants() {
		return reactants; //No clone! We want to set them!
	}
	
	public String[] getReactantNames() {
		return new String[]{};
	}
	
	public double computeRate(int r1Level, int nLevelsR1, boolean activeR1, int r2Level, int nLevelsR2, boolean activeR2) {
//		double k2 = parameters.get(SCENARIO_PARAMETER_K2),
//		   E = r1Level,
//		   km = parameters.get(SCENARIO_PARAMETER_KM),
//		   Stot = parameters.get(SCENARIO_PARAMETER_STOT),
//		   S;
//		r2Level = (int)Math.round(r2Level * Stot / nLevelsR2);
//		if (activatingReaction) { //The quantity of unreacted substrate is different depending on the point of view of the reaction: if we are activating, the unreacted substrate is inactive. But if we are inhibiting the unreacted substrate is ACTIVE.
//			S = Stot - r2Level;
//		} else {
//			S = r2Level;
//		}
//		double rate = k2 * E * S / (km + S);
//		return rate;
		return 0; //We are not supposed to compute anything: the basic Scenario is an abstract class
	}
	
	public Double computeFormula(int r1Level, int nLevelsR1, boolean activeR1, int r2Level, int nLevelsR2, boolean activeR2) {
		double rate = computeRate(r1Level, nLevelsR1, activeR1, r2Level, nLevelsR2, activeR2);
		if (rate > 1e-8) {
			//return Math.max(0, (int)Math.round(1 / rate)); //We need to put at least 1 because otherwise the reaction will keep happening forever (it is not very nice not to let time pass..)
			return 1.0 / rate;
		} else {
			//return INFINITE_TIME;
			return Double.POSITIVE_INFINITY;
		}
	}

	/**
	 * Generate the times table based on the scenario formula and parameters.
	 * @param nLevelsReactant1 The total number of levels of reactant1 (the enzyme or catalyst) + 1 (!! Note the +1! Basically, this is the number of columns of the time table, so when using it as the number of activity levels of the enzyme we must subtract 1)
	 * @param activeR1 true if the R1 input to the formula is the concentration of active reactant 1
	 * @param reactant1IsDownstream true when the reactant 1 is a downstream reactant (the reaction time depends on its activity, but we treat limit cases as their nearest neighbours to ensure that boolean networks behave correctly) 
	 * @param nLevelsReactant2 The total number of levels of reactant2 (the substrate) + 1 (!! Note the +1)
	 * @param activeR2 true if the R2 input to the formula is the concentration of active reactant 2
	 * @param reactant2IsDownstream see reactant1IsDownstream
	 * @return Output is a list for no particular reason anymore. It used to be
	 * set as a Cytoscape property, but these tables can become clumsy to be stored
	 * inside the model, slowing down the loading/saving processes in the Cytoscape
	 * interface. We now compute the tables on the fly and output them directly as
	 * reference constants in the UPPPAAL models, saving also memory.
	 */
	public List<Double> generateTimes(int nLevelsReactant1, boolean activeR1, boolean reactant1IsDownstream, int nLevelsReactant2, boolean activeR2, boolean reactant2IsDownstream) {
		List<Double> times = new LinkedList<Double>();
		int i, limitI;
		if (!activeR2) { //We depend on the inactivity of R2, which is completely inactive (first row --> R2 = 0), so the first row will have the smallest values
			i = 0;
			limitI = nLevelsReactant2 - 1; //the last row will be all infinite
		} else { //We depend on the activity of R2, which is completely inactive, so the first row should be all infinite
			if (reactant2IsDownstream) {
				for (int k=0;k<nLevelsReactant1;k++) {
					times.add(computeFormula(k, nLevelsReactant1 - 1, activeR1, 1, nLevelsReactant2 - 1, activeR2));
				}
			} else {
				for (int k=0;k<nLevelsReactant1;k++) {
					times.add(Double.POSITIVE_INFINITY); //all reactant2 already reacted (inactive) = no reaction
				}
			}
			i = 1; //the first row was already done here, with all infinites
			limitI = nLevelsReactant2; //the last row will have the smallest values
		}
		for (;i<limitI;i++) {
			int j, limitJ;
			if (!activeR1) { //We depend on the inactivity of R1, which when j = 0 is completely inactive (first column --> R1 = 0), so the first column will have the smallest values
				j = 0;
				limitJ = nLevelsReactant1 - 1; //the last column will be all infinite
			} else {
				if (reactant1IsDownstream) {
					times.add(computeFormula(1, nLevelsReactant1 - 1, activeR1, i, nLevelsReactant2 - 1, activeR2));
				} else {
					times.add(Double.POSITIVE_INFINITY); //no reactant1 = no reaction
				}
				j = 1;
				limitJ = nLevelsReactant1; //the last column will have the smallest values
			}
			for (;j<limitJ;j++) {
				times.add(computeFormula(j, nLevelsReactant1 - 1, activeR1, i, nLevelsReactant2 - 1, activeR2));
			}
			if (!activeR1) { //We depend on the inactivity of R1, which in the last column is completely active. So the last column has all infinite
				if (reactant1IsDownstream) {
					times.add(computeFormula(nLevelsReactant1 - 2, nLevelsReactant1 - 1, activeR1, i, nLevelsReactant2 - 1, activeR2));
				} else {
					times.add(Double.POSITIVE_INFINITY);
				}
			}
		}
		if (!activeR2) { //We depend on the inactivity of R2, which in the last row is completely active. So the last row has all infinite
			for (int j=0;j<nLevelsReactant1;j++) {
				if (reactant2IsDownstream) {
					times.add(computeFormula(j, nLevelsReactant1 - 1, activeR1, nLevelsReactant2 - 2, nLevelsReactant2 - 1, activeR2));
				} else {
					times.add(Double.POSITIVE_INFINITY); //all reactant2 already reacted (active) = no reaction
				}
			}
		}
		return times;
	}
	
	public String[] listVariableParameters() {
		return new String[]{};
	}
	
}