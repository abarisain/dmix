package org.a0z.mpd.event;

import org.a0z.mpd.MPDStatus;

/**
 * Represents a change in current playing track position.
 * @author Felipe Gustavo de Almeida
 * @version $Id: MPDTrackPositionChangedEvent.java 2614 2004-11-11 18:46:31Z galmeida $
 */
public class MPDTrackPositionChangedEvent {
    private MPDStatus mpdStatus;

    /**
     * Constructs a new <code>MPDTrackPositionChangedEvent</code>.
     * @param mpdStatus <code>MPDStatus</code> after event.
     */
    public MPDTrackPositionChangedEvent(MPDStatus mpdStatus) {
        this.mpdStatus = mpdStatus;
    }

    /**
     * Retrieves <code>MPDStatus</code> after event.
     * @return <code>MPDStatus</code> after event.
     */
    public MPDStatus getMpdStatus() {
        return this.mpdStatus;
    }
}
