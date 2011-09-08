package org.a0z.mpd.exception;


/**
 * Represents an MPD Client error.
 * 
 * @author Felipe Gustavo de Almeida
 * @version $Id: MPDServerException.java 2595 2004-11-11 00:21:36Z galmeida $
 */
public class MPDClientException extends MPDException {

	private static final long serialVersionUID = -7525157411640713109L;

	/**
	 * Constructor.
	 */
	public MPDClientException() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param message
	 *           exception message.
	 */
	public MPDClientException(String message) {
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
	public MPDClientException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor.
	 * 
	 * @param cause
	 *           cause of this exception.
	 */
	public MPDClientException(Throwable cause) {
		super(cause);
	}

}
