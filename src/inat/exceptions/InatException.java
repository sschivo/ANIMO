/**
 * 
 */
package inat.exceptions;

/**
 * This excpetion is thrown to indicate that something within the ANIMO framework
 * went wrong.
 * 
 * @author B. Wanders
 */
public class InatException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 315015035048039668L;

	/**
	 * Constructor with detail message and cause.
	 * 
	 * @param message the detail message
	 * @param cause the cause of this exception
	 */
	public InatException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor with detail message.
	 * 
	 * @param message the detail message
	 */
	public InatException(String message) {
		super(message);
	}
}
