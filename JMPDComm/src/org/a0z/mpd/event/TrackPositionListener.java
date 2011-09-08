package org.a0z.mpd.event;

import org.a0z.mpd.MPDStatus;

/**
 * Represents a change in current playing track position on MPD server.
 * 
 * @version $Id: TrackPositionListener.java 2595 2004-11-11 00:21:36Z galmeida $
 */
public interface TrackPositionListener {
	/**
	 * Called when track position changes on server.
	 * 
	 * @param status New MPD status, containing current track position
	 */
	void trackPositionChanged(MPDStatus status);
}
