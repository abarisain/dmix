package net.roarsoftware.lastfm.cache;

import java.io.IOException;
import java.util.Collection;

import net.roarsoftware.lastfm.scrobble.Scrobbler;
import net.roarsoftware.lastfm.scrobble.SubmissionData;

/**
 * A <code>ScrobbleCache</code> is able to cache {@link SubmissionData} instances for later submission
 * to the Last.fm servers.
 *
 * @author Janni Kovacs
 */
public interface ScrobbleCache {

	/**
	 * Caches one or more {@link net.roarsoftware.lastfm.scrobble.SubmissionData}.
	 *
	 * @param submissions The submissions
	 */
	public void cacheScrobble(SubmissionData... submissions);

	/**
	 * Caches a collection of {@link SubmissionData}.
	 *
	 * @param submissions The submissions
	 */
	public void cacheScrobble(Collection<SubmissionData> submissions);

	/**
	 * Checks if the cache contains any scrobbles.
	 *
	 * @return <code>true</code> if this cache is empty
	 */
	public boolean isEmpty();

	/**
	 * Tries to scrobble all cached scrobbles. If it succeeds the cache will be empty afterwards.
	 * If this method fails an IOException is thrown and no entries are removed from the cache.
	 *
	 * @param scrobbler A {@link Scrobbler} instance
	 * @throws java.io.IOException on I/O errors
	 * @throws IllegalStateException if the {@link Scrobbler} is not fully initialized (i.e. no handshake performed)
	 */
	public void scrobble(Scrobbler scrobbler) throws IOException;

	/**
	 * Clears all cached scrobbles from this cache.
	 */
	public void clearScrobbleCache();
}
