package org.a0z.mpd.event;

import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.exception.MPDServerException;

/**
 * @version $Id: StatusChangeListener.java 2941 2005-02-09 02:34:21Z galmeida $
 */
public interface StatusChangeListener {
	/**
	 * Called when volume changes on MPD server.
	 * 
	 * @param mpdStatus
	 *           <code>MPDStatus</code> after event
	 * 
	 * @param oldVolume
	 *           volume before event
	 */
	void volumeChanged(MPDStatus mpdStatus, int oldVolume);

	/**
	 * Called when playlist changes on MPD server.
	 * 
	 * @param mpdStatus
	 *           MPDStatus after playlist change.
	 * 
	 * @param oldPlaylistVersion
	 *           old playlist version.
	 * @throws MPDServerException 
	 */
	void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion);

	/**
	 * Called when playing track is changed on server.
	 * 
	 * @param mpdStatus
	 *           <code>MPDStatus</code> after event.
	 * 
	 * @param oldTrack
	 *           track number before event.
	 */
	void trackChanged(MPDStatus mpdStatus, int oldTrack);

	/**
	 * Called when MPD state changes on server.
	 * 
	 * @param mpdStatus
	 *           MPDStatus after event.
	 * 
	 * @param oldState
	 *           previous state.
	 */
	void stateChanged(MPDStatus mpdStatus, String oldState);

	/**
	 * Called when MPD server repeat feature changes state.
	 * 
	 * @param repeating
	 *           new repeat state: true, on; false, off.
	 */
	void repeatChanged(boolean repeating);

	/**
	 * Called when MPD server random feature changes state.
	 * 
	 * @param random
	 *           new random state: true, on; false, off
	 */
	void randomChanged(boolean random);

	/**
	 * Called when MPD server connection changes state. (connected/disconnected)
	 * 
	 * @param connected
	 *           new connection state: true, connected; false, disconnected.
	 * @param connectionLost
	 *           true when connection was lost, false otherwise.
	 */
	void connectionStateChanged(boolean connected, boolean connectionLost);

	/**
	 * Called when the MPD server update database starts and stops.
	 * 
	 * @param updating
	 *           true when updating, false when not updating.
	 */
	void libraryStateChanged(boolean updating);
}
