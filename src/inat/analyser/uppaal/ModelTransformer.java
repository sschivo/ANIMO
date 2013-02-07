/**
 * 
 */
package inat.analyser.uppaal;

import inat.model.Model;

/**
 * This interface is responsible for converting the {@link Model} to an UPPAAL
 * representation.
 * 
 * @author B. Wanders
 */
public interface ModelTransformer {
	/**
	 * Converts the model to UPPAAL representation.
	 * 
	 * @param m the model to transform
	 * @return the UPPAAL model
	 */
	public String transform(Model m);
}
