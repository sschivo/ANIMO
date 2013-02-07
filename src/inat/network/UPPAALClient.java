package inat.network;

import inat.analyser.SMCResult;
import inat.analyser.uppaal.SimpleLevelResult;
import inat.model.Model;

import java.rmi.Naming;

/**
 * The class used to access the remote server.
 */
public class UPPAALClient {
	private iUPPAALServer server = null;
	
	public UPPAALClient(String serverHost, Integer serverPort) throws Exception {
		//System.setProperty("java.rmi.server.hostname", "130.89.14.18");
		System.setSecurityManager(new java.rmi.RMISecurityManager());
		server = (iUPPAALServer) Naming.lookup("rmi://" + serverHost + ":" + serverPort + "/UPPAALServer");
	}
	
	public SimpleLevelResult analyze(Model m, int timeTo, int nSimulationRuns, boolean computeAvgStdDev, boolean overlayPlot) throws Exception {
		return server.analyze(m, timeTo, nSimulationRuns, computeAvgStdDev, overlayPlot);
	}
	
	public SMCResult analyzeSMC(Model m, String smcQuery) throws Exception {
		return server.analyze(m, smcQuery);
	}
}
