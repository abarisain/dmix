package org.a0z.mpd.event;

import org.a0z.mpd.MPDStatus;

/**
 * Represents playlist change event.
 *
 * @author Felipe Gustavo de Almeida
 * @version $Id: MPDPlaylistChangedEvent.java 2693 2004-11-16 23:40:07Z galmeida $
 */
public class MPDPlaylistChangedEvent {
    private MPDStatus mpdStatus;
    private int oldPlaylistVersion;

    /**
     * Contructs a new playlist change event.
     * @param mpdStatus MPDStatus after playlist change.
     * @param oldPlaylistVersion old playlist version.
     */
    public MPDPlaylistChangedEvent(MPDStatus mpdStatus, int oldPlaylistVersion) {
        this.mpdStatus = mpdStatus;
        this.oldPlaylistVersion = oldPlaylistVersion;
    }

    /**
     * Retrieves MPDStatus after playlist change.
     * @return MPDStatus after playlist change.
     */
    public MPDStatus getMpdStatus() {
        return this.mpdStatus;
    }

    /**
     * Retrieves old playlist version.
     * @return old playlist version.
     */
    public int getOldPlaylistVersion() {
        return this.oldPlaylistVersion;
    }
}
