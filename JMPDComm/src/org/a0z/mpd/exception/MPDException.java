package org.a0z.mpd.exception;

/**
 * @author Felipe Gustavo de Almeida - galmeida
 * @version $Id$
 */
public class MPDException extends Exception {

	private static final long serialVersionUID = -8576503491266801543L;

	/**
	 * Constructor.
	 */
	public MPDException() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param message
	 *           exception message.
	 */
	public MPDException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 * 
	 * @param message
	 *           exception message.
	 * @param cause
	 *           cause of this exception.
	 */
	public MPDException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor.
	 * 
	 * @param cause
	 *           cause of this exception.
	 */
	public MPDException(Throwable cause) {
		super(cause);
	}

}
