package net.roarsoftware.lastfm.cache;

import java.util.Map;

/**
 * The <code>ExpirationPolicy</code> decides if and how long a request should be cached.
 *
 * @author Janni Kovacs
 */
public interface ExpirationPolicy {

	/**
	 * Returns the time in milliseconds a request of the given method should be cached. Returns -1 if this
	 * method should not be cached.
	 *
	 * @param method The method called
	 * @param params The parameters sent
	 * @return the time the request should be cached in milliseconds
	 */
	public long getExpirationTime(String method, Map<String, String> params);

}
