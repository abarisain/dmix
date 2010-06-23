package net.roarsoftware.lastfm.scrobble;

/**
 * The source of the track. See <a href="http://www.last.fm/api/submissions#subs">http://www.last.fm/api/submissions#subs</a>
 * for more information.
 *
 * @author Janni Kovacs
 */
public enum Source {

	/**
	 * Chosen by the user (the most common value, unless you have a reason for choosing otherwise, use this).
	 */
	USER("P"),

	/**
	 * Non-personalised broadcast (e.g. Shoutcast, BBC Radio 1).
	 */
	NON_PERSONALIZED_BROADCAST("R"),

	/**
	 * Personalised recommendation except Last.fm (e.g. Pandora, Launchcast).
	 */
	PERSONALIZED_BROADCAST("E"),

	/**
	 * Last.fm (any mode). In this case, the 5-digit Last.fm recommendation key must be appended to this source ID
	 * to prove the validity of the submission (for example, "o[0]=L1b48a").
	 */
	LAST_FM("L"),

	/**
	 * Source unknown.
	 */
	UNKNOWN("U");

	private String code;

	Source(String code) {
		this.code = code;
	}

	/**
	 * Returns the corresponding code for this source.
	 *
	 * @return the code
	 */
	public String getCode() {
		return code;
	}
}
