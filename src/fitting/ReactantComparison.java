package fitting;
import java.util.Vector;

public class ReactantComparison {
	private String csvFile = null;
	private Vector<String> seriesNames = null;
	private Double maxError = null;
	
	public ReactantComparison() {
		this.csvFile = "";
		this.seriesNames = new Vector<String>();
		this.maxError = 0.0;
	}
	
	public ReactantComparison(String csvFile, Vector<String> seriesNames, Double maxError) {
		this.csvFile = csvFile;
		this.seriesNames = seriesNames;
		this.maxError = maxError;
	}
	
	public void setCsvFile(String csvFile) {
		this.csvFile = csvFile;
	}
	
	public String getCsvFile() {
		return this.csvFile;
	}
	
	public void setSeriesNames(Vector<String> seriesNames) {
		this.seriesNames = seriesNames;
	}
	
	public Vector<String> getSeriesNames() {
		return this.seriesNames;
	}
	
	public void setMaxError(Double maxError) {
		this.maxError = maxError;
	}
	
	public Double getMaxError() {
		return this.maxError;
	}
}