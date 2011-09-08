package org.a0z.mpd.exception;


/**
 * Represents an MPD Server error.
 * 
 * @author Felipe Gustavo de Almeida
 * @version $Id: MPDServerException.java 2716 2004-11-20 17:37:20Z galmeida $
 */
public class MPDServerException extends MPDException {

	private static final long serialVersionUID = 5986199004785561712L;

	/**
	 * Constructor.
	 */
	public MPDServerException() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param message
	 *           exception message.
	 */
	public MPDServerException(String message) {
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
	public MPDServerException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor.
	 * 
	 * @param cause
	 *           cause of this exception.
	 */
	public MPDServerException(Throwable cause) {
		super(cause);
	}

}
