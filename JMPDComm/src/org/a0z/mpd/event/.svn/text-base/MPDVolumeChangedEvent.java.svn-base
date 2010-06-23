package org.a0z.mpd.event;

import org.a0z.mpd.MPDStatus;

/**
 * Represents a volume change on MPD server.
 * @author Felipe Gustavo de Almeida
 * @version $Id: MPDVolumeChangedEvent.java 2614 2004-11-11 18:46:31Z galmeida $
 */
public class MPDVolumeChangedEvent {
    private int oldVolume;
    private MPDStatus mpdStatus;

    /**
     * Constructs a new <code>MPDVolumeChangedEvent</code>.
     * @param oldVolume volume before event.
     * @param mpdStatus <code>MPDStatus</code> after event.
     */
    public MPDVolumeChangedEvent(int oldVolume, MPDStatus mpdStatus) {
        this.oldVolume = oldVolume;
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
     * Retrieves volume before event.
     * @return volume befor event.
     */
    public int getOldVolume() {
        return oldVolume;
    }
}
