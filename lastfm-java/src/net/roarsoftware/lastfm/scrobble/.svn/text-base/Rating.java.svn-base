package net.roarsoftware.lastfm.scrobble;

/**
 * The rating of the track. See <a href="http://www.last.fm/api/submissions#subs">http://www.last.fm/api/submissions#subs</a>
 * for more information.
 *
 * @author Lukasz Wisniewski
 */
public enum Rating {

	/**
	 * Love (on any mode if the user has manually loved the track). This implies a listen.
	 */
	LOVE("L"),

	/**
	 * Ban (only if source=L). This implies a skip, and the client should skip to the next track when a ban happens
	 */
	BAN("B"),

	/**
	 * Skip (only if source=L)
	 */
	SKIP("S");

	private String code;

	Rating(String code) {
		this.code = code;
	}

	/**
	 * Returns the corresponding code for this rating.
	 *
	 * @return the code
	 */
	public String getCode() {
		return code;
	}
}