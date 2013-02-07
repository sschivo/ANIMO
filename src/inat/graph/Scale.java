package inat.graph;


import java.awt.Rectangle;

/**
 * The class used to contain the scaling informations for a graph.
 * When a new series is added to the graph, we make sure that the
 * update the minimum and maximum values for the graph, so that it will
 * automatically display the data in the best possible zoom level.
 */
public class Scale {
	private double maxX = Double.NaN;
	private double maxY = Double.NaN;
	private double minX = Double.NaN;
	private double minY = Double.NaN;
	private double scaleX = 1;
	private double scaleY = 1;
	
	public Scale() {
		//nothing
	}
	
	//reset the scales
	public void reset() {
		maxX = maxY = minX = minY = Double.NaN;
		scaleX = scaleY = 1;
	}

	public double getXScale() {
		return scaleX;
	}
	
	public double getYScale() {
		return scaleY;
	}
	
	public double getMaxX() {
		return maxX;
	}
	
	public void setMaxX(double maxX) {
		this.maxX = maxX;
	}
	
	public double getMinX() {
		return minX;
	}
	
	public void setMinX(double minX) {
		this.minX = minX;
	}
	
	public double getMaxY() {
		return maxY;
	}
	
	public void setMaxY(double maxY) {
		this.maxY = maxY;
	}
	
	public double getMinY() {
		return minY;
	}
	
	public void setMinY(double minY) {
		this.minY = minY;
	}

	public void addData(P[] data) {
		for (int i=0; i < data.length; i++) {
			if (Double.isNaN(maxX) || maxX < data[i].x){
				maxX = data[i].x;
			}
			if (Double.isNaN(maxY) || maxY < data[i].y) {
				maxY = data[i].y;
			}
			if (Double.isNaN(minX) || minX > data[i].x) {
				minX = data[i].x;
			}
			if (Double.isNaN(minY) || minY > data[i].y) {
				minY = data[i].y;
			}
		}
	}

	public void computeScale(Rectangle bounds) {
		scaleX = bounds.width / Math.abs(maxX - minX);
		scaleY = bounds.height / Math.abs(maxY - minY);
		if (scaleX < 1e-8) {
			scaleX = 1e-8;
		}
		if (scaleY < 1e-8) {
			scaleY = 1e-8;
		}
	}
	
}
