package net.roarsoftware.lastfm;

import net.roarsoftware.xml.DomElement;

/**
 * Contains Session data relevant for making API calls which require authentication.
 * A <code>Session</code> instance is passed to all methods requiring previous authentication.
 *
 * @author Janni Kovacs
 * @see net.roarsoftware.lastfm.Authenticator
 */
public class Session {

	private String apiKey;
	private String secret;
	private String username;
	private String key;
	private boolean subscriber;

	/**
	 * Restores a Session instance with the given session key.
	 *
	 * @param apiKey An api key
	 * @param secret A secret
	 * @param sessionKey The previously obtained session key
	 * @return a Session instance
	 */
	public static Session createSession(String apiKey, String secret, String sessionKey) {
		return createSession(apiKey, secret, sessionKey, null, false);
	}

	/**
	 * Restores a Session instance with the given session key.
	 *
	 * @param apiKey An api key
	 * @param secret A secret
	 * @param sessionKey The previously obtained session key
	 * @param username A Last.fm username
	 * @param subscriber Subscriber status
	 * @return a Session instance
	 */
	public static Session createSession(String apiKey, String secret, String sessionKey, String username,
										boolean subscriber) {
		Session s = new Session();
		s.apiKey = apiKey;
		s.secret = secret;
		s.key = sessionKey;
		s.username = username;
		s.subscriber = subscriber;
		return s;
	}

	public String getSecret() {
		return secret;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getKey() {
		return key;
	}

	public boolean isSubscriber() {
		return subscriber;
	}

	public String getUsername() {
		return username;
	}

	static Session sessionFromElement(DomElement element, String apiKey, String secret) {
		if (element == null)
			return null;
		String user = element.getChildText("name");
		String key = element.getChildText("key");
		boolean subsc = element.getChildText("subscriber").equals("1");
		return createSession(apiKey, secret, key, user, subsc);
	}
}
