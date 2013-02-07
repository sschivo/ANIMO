package inat.network;
import inat.analyser.SMCResult;
import inat.analyser.uppaal.SimpleLevelResult;
import inat.model.Model;

import java.rmi.Remote;

/**
 * Remotely accessible features: simulation run or SMC analysis
 */
public interface iUPPAALServer extends Remote {
	
	public SimpleLevelResult analyze(Model m, int timeTo, int nSimulationRuns, boolean computeAvgStdDev, boolean overlayPlot) throws Exception;
	
	public SMCResult analyze(Model m, String smcQuery) throws Exception;
}
