package org.a0z.mpd.exception;


/**
 * Represents an MPD Connection error.
 * 
 * @author Felipe Gustavo de Almeida
 * @version $Id: MPDConnectionException.java 2595 2004-11-11 00:21:36Z galmeida $
 */
public class MPDConnectionException extends MPDServerException {

	private static final long serialVersionUID = 7522398560164238646L;

	/**
	 * Constructor.
	 */
	public MPDConnectionException() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param message
	 *           exception message.
	 */
	public MPDConnectionException(String message) {
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
	public MPDConnectionException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor.
	 * 
	 * @param cause
	 *           cause of this exception.
	 */
	public MPDConnectionException(Throwable cause) {
		super(cause);
	}

}
