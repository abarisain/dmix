package org.a0z.mpd.url;

import java.io.IOException;

/**
 * Thrown when Mime-Type is not supported.
 * 
 * @author galmeida
 */
public class UnsupportedMimeTypeException extends IOException {

	private static final long serialVersionUID = 2035178718331445615L;

	/**
	 * Constructor.
	 * 
	 * @param message
	 *           message.
	 */
	public UnsupportedMimeTypeException(String message) {
		super(message);
	}
}
