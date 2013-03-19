package inat.cytoscape;

import java.awt.event.ActionEvent;

import javax.swing.AbstractButton;
import javax.swing.JOptionPane;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.util.CytoscapeAction;

/**
 * The button to change the number of real-life seconds represented
 * by a single UPPAAL time unit in the model.
 */
public class ChangeSecondsAction extends CytoscapeAction {

	private static final long serialVersionUID = -9023326560269020342L;

	private static final String SECONDS_PER_POINT = "seconds per point"; //The name of the network property to which the number of real-life seconds per UPPAAL time unit is associated
	
	@SuppressWarnings("unused")
	private AbstractButton associatedButton;
	
	public ChangeSecondsAction(InatPlugin plugin, AbstractButton associatedButton) {
		this.associatedButton = associatedButton;
		associatedButton.setAction(this);
		CyNetwork network = Cytoscape.getCurrentNetwork();
		CyAttributes networkAttributes = Cytoscape.getNetworkAttributes();

		//If we did not find a value for the property, we invite the user to set it. It a value was found, we directly display it on the button caption
		if (!networkAttributes.hasAttribute(network.getIdentifier(), SECONDS_PER_POINT)) {
			associatedButton.setText("Choose seconds/step");
		} else {
			associatedButton.setText("" + networkAttributes.getDoubleAttribute(network.getIdentifier(), SECONDS_PER_POINT) + " seconds/step");
		}
	}

	/**
	 * Depending on whether the number of seconds per time unit was already set,
	 * we ask a slightly different question to the user. Apart from that, we
	 * simply set the property to what the user has chosen.
	 */
	@Override
	public void actionPerformed(ActionEvent arg0) {
		CyNetwork network = Cytoscape.getCurrentNetwork();
		CyAttributes networkAttributes = Cytoscape.getNetworkAttributes();
		
		String message;
		double currentSecondsPerPoint;
		if (!networkAttributes.hasAttribute(network.getIdentifier(), SECONDS_PER_POINT)) {
			message = "Missing number of seconds per point for the network.\nPlease insert the number of real-life seconds a simulation point will represent";
			currentSecondsPerPoint = 1;
		} else {
			message = "Please insert the number of real-life seconds a simulation point will represent";
			currentSecondsPerPoint = networkAttributes.getDoubleAttribute(network.getIdentifier(), SECONDS_PER_POINT);
		}
		String inputSecs = JOptionPane.showInputDialog(message, currentSecondsPerPoint);
		Double nSecPerPoint;
		if (inputSecs != null) {
			try {
				nSecPerPoint = new Double(inputSecs);
			} catch (Exception ex) {
				nSecPerPoint = currentSecondsPerPoint;
			}
		} else {
			nSecPerPoint = currentSecondsPerPoint;
		}
		networkAttributes.setAttribute(network.getIdentifier(), SECONDS_PER_POINT, nSecPerPoint);
	}

}
