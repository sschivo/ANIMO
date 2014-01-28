package fitting;
import inat.analyser.LevelResult;

import java.util.HashMap;

public class AcceptableConfiguration {
	private HashMap<String, ScenarioCfg> scenarioConfigurations = null;
	private LevelResult result = null;
	private String errorEstimation = null;
	
	public AcceptableConfiguration(HashMap<String, ScenarioCfg> scenarioConfigurations, LevelResult result, String errorEstimation) {
		this.scenarioConfigurations = scenarioConfigurations;
		this.result = result;
		this.errorEstimation = errorEstimation;
	}
	
	public void setScenarioConfigurations(HashMap<String, ScenarioCfg> scenarioConfigurations) {
		this.scenarioConfigurations = scenarioConfigurations;
	}
	
	public HashMap<String, ScenarioCfg> getScenarioConfigurations() {
		return this.scenarioConfigurations;
	}
	
	public void setResult(LevelResult result) {
		this.result = result;
	}
	
	public LevelResult getResult() {
		return this.result;
	}
	
	public void setErrorEstimation(String errorEstimation) {
		this.errorEstimation = errorEstimation;
	}
	
	public String getErrorEstimation() {
		return this.errorEstimation;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (String r : scenarioConfigurations.keySet()) {
			builder.append(r + " uses ");
			ScenarioCfg cfg = scenarioConfigurations.get(r);
			for (String parName : cfg.getParameters().keySet()) {
				builder.append(parName + "=" + cfg.getParameters().get(parName) + ", ");
			}
			builder.append(System.getProperty("line.separator"));
		}
		builder.append(". Error estimation: " + errorEstimation);
		return builder.toString();
	}
}
