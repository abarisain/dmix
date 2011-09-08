package org.a0z.mpd.exception;

/**
 * Thrown when server sends an unexpected or invalid message.
 * 
 * @author Felipe Gustavo de Almeida
 * @version $Id$
 */
public class InvalidResponseException extends RuntimeException {
	
	private static final long serialVersionUID = 2105442123614116620L;

	/**
	 * Constructor.
	 */
	public InvalidResponseException() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param message
	 *           exception message.
	 */
	public InvalidResponseException(String message) {
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
	public InvalidResponseException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor.
	 * 
	 * @param cause
	 *           cause of this exception.
	 */
	public InvalidResponseException(Throwable cause) {
		super(cause);
	}

}