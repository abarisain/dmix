package org.a0z.mpd.exception;

/**
 * @author Felipe Gustavo de Almeida
 * @version $Id$
 */
public class InvalidParameterException extends RuntimeException {

	private static final long serialVersionUID = 1335085123017300139L;

	/**
	 * Constructor.
	 */
	public InvalidParameterException() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param message
	 *           exception message.
	 */
	public InvalidParameterException(String message) {
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
	public InvalidParameterException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor.
	 * 
	 * @param cause
	 *           cause of this exception.
	 */
	public InvalidParameterException(Throwable cause) {
		super(cause);
	}

}