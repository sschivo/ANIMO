package inat.cytoscape;

import java.awt.event.ActionEvent;

import cytoscape.util.CytoscapeAction;

public class InatActionTask extends CytoscapeAction {
	
	private static final long serialVersionUID = 7601367319473988438L;
	protected boolean needToStop = false; //Whether the user has pressed the Cancel button on the TaskMonitor while we were running an analysis process

	public InatActionTask(String init) {
		super(init);
	}
	

	public static String timeDifferenceFormat(long startTime, long endTime) {
		long diffInSeconds = (endTime - startTime) / 1000;
		return timeDifferenceFormat(diffInSeconds);
	}
	
	public static String timeDifferenceFormat(long diffInSeconds) {
	    long diff[] = new long[] { 0, 0, 0, 0 };
	    /* sec */diff[3] = (diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds);
	    /* min */diff[2] = (diffInSeconds = (diffInSeconds / 60)) >= 60 ? diffInSeconds % 60 : diffInSeconds;
	    /* hours */diff[1] = (diffInSeconds = (diffInSeconds / 60)) >= 24 ? diffInSeconds % 24 : diffInSeconds;
	    /* days */diff[0] = (diffInSeconds = (diffInSeconds / 24));
	    
	    return String.format(
		        "%d day%s, %d hour%s, %d minute%s, %d second%s",
		        diff[0],
		        diff[0] != 1 ? "s" : "",
		        diff[1],
		        diff[1] != 1 ? "s" : "",
		        diff[2],
		        diff[2] != 1 ? "s" : "",
		        diff[3],
		        diff[3] != 1 ? "s" : "");
	}
	
	public boolean needToStop() {
		return this.needToStop;
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		
	}
	

}
