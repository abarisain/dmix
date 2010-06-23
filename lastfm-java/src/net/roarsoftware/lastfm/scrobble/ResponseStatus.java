package net.roarsoftware.lastfm.scrobble;

/**
 * Contains information about the result of a scrobbling operation and an optional error message.
 *
 * @author Janni Kovacs
 */
public class ResponseStatus {

	public static final int OK = 0;
	public static final int BANNED = 1;
	public static final int BADAUTH = 2;
	public static final int BADTIME = 3;
	public static final int BADSESSION = 4;
	public static final int FAILED = 5;

	private int status;
	private String message;

	public ResponseStatus(int status) {
		this(status, null);
	}

	public ResponseStatus(int status, String message) {
		this.status = status;
		this.message = message;
	}

	/**
	 * Returns the optional error message, which is only available if <code>status</code> is <code>FAILED</code>, or
	 * <code>null</code>, if no message is available.
	 *
	 * @return the error message or <code>null</code>
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Returns the result status code of the operation, which is one of the integer constants defined in this class.
	 *
	 * @return the status code
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Returns <code>true</code> if the operation was successful. Same as <code>getStatus() == ResponseStatus.OK</code>.
	 *
	 * @return <code>true</code> if status is OK
	 */
	public boolean ok() {
		return status == OK;
	}

	static int codeForStatus(String status) {
		if ("OK".equals(status))
			return OK;
		if (status.startsWith("FAILED"))
			return FAILED;
		if ("BADAUTH".equals(status))
			return BADAUTH;
		if ("BADSESSION".equals(status))
			return BADSESSION;
		if ("BANNED".equals(status))
			return BANNED;
		if ("BADTIME".equals(status))
			return BADTIME;
		return -1;
	}
}
