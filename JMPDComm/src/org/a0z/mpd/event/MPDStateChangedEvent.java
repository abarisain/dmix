package org.a0z.mpd.event;

import org.a0z.mpd.MPDStatus;
/**
 * Represents a state change event on MPD Server.
 * @author Felipe Gustavo de Almeida
 * @version $Id: MPDStateChangedEvent.java 2595 2004-11-11 00:21:36Z galmeida $
 */
public class MPDStateChangedEvent {
    private String oldState;
    private MPDStatus mpdStatus;

    /**
     * Represents a state change event.
     * @param oldState previous state.
     * @param mpdStatus MPDStatus after event.
     */
    public MPDStateChangedEvent(String oldState, MPDStatus mpdStatus) {
        this.oldState = oldState;
        this.mpdStatus = mpdStatus;
    }

    /**
     * Retrieves MPDStatus after event.
     * @return Retrieves MPDStatus after event.
     */
    public MPDStatus getMpdStatus() {
        return mpdStatus;
    }

    /**
     * Retrieves state before event.
     * @return state before event.
     */
    public String getOldState() {
        return oldState;
    }
}
