package org.a0z.mpd.event;

import org.a0z.mpd.MPDStatus;

/**
 * Represents a track change evente on MPD server.
 * @author Felipe Gustavo de Almeida
 * @version $Id: MPDTrackChangedEvent.java 2595 2004-11-11 00:21:36Z galmeida $
 */
public class MPDTrackChangedEvent {
    private int oldTrack;
    private MPDStatus mpdStatus;

    /**
     * Constructs a new <code>MPDTrackChangedEvent</code>.
     * @param oldTrack track number before event.
     * @param mpdStatus <code>MPDStatus</code> after event.
     */
    public MPDTrackChangedEvent(int oldTrack, MPDStatus mpdStatus) {
        this.oldTrack = oldTrack;
        this.mpdStatus = mpdStatus;
    }

    /**
     * Retrieves <code>MPDStatus</code> after event.
     * @return <code>MPDStatus</code> after event.
     */
    public MPDStatus getMpdStatus() {
        return mpdStatus;
    }

    /**
     * Retrieves track number before event.
     * @return track number before event.
     */
    public int getOldTrack() {
        return oldTrack;
    }
}
