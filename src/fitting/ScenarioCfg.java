package fitting;
import java.io.Serializable;
import java.util.HashMap;

public class ScenarioCfg implements Serializable {
	private static final long serialVersionUID = -6396468134355425874L;
	private int index = 0;
	private HashMap<String, Double> parameters = null;
	
	public ScenarioCfg(int index, HashMap<String, Double> parameters) {
		this.index = index;
		this.parameters = parameters;
	}
	
	@SuppressWarnings("unchecked")
	public ScenarioCfg(ScenarioCfg cfg) {
		this.index = cfg.index;
		this.parameters = (HashMap<String, Double>)cfg.parameters.clone();
	}
	
	public ScenarioCfg() {
	}

	public int getIndex() {
		return this.index;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	public HashMap<String, Double> getParameters() {
		return this.parameters;
	}
	
	public void setParameters(HashMap<String, Double> parameters) {
		this.parameters = parameters;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder("Scenario " + index + ", parameters: ");
		for (String parName : parameters.keySet()) {
			builder.append(parName + " = " + parameters.get(parName) + ", ");
		}
		return builder.substring(0, builder.length() - 2);
	}
}
