
package org.a0z.mpd.exception;

/**
 * Represents an MPD Connection error : the server does not answer to the client
 * command
 */
public class MPDNoResponseException extends MPDConnectionException {

    private static final long serialVersionUID = 3680916889263961898L;

    /**
     * Constructor.
     */
    public MPDNoResponseException() {
        super();
    }

    /**
     * Constructor.
     * 
     * @param message exception message.
     */
    public MPDNoResponseException(String message) {
        super(message);
    }

    /**
     * Constructor.
     * 
     * @param message exception message.
     * @param cause cause of this exception.
     */
    public MPDNoResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor.
     * 
     * @param cause cause of this exception.
     */
    public MPDNoResponseException(Throwable cause) {
        super(cause);
    }

}
