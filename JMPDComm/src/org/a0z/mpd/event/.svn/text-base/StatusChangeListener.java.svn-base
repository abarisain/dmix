/*
 * Created on 14/02/2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.a0z.mpd.event;


/**
 * @author Felipe Gustavo de Almeida
 * @version $Id: StatusChangeListener.java 2941 2005-02-09 02:34:21Z galmeida $
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface StatusChangeListener {
    /**
     * Called when volume changes on MPD server.
     * @param event event.
     */
    void volumeChanged(MPDVolumeChangedEvent event);

    /**
     * Called when playlist changes on MPD server.
     * @param event event.
     */
    void playlistChanged(MPDPlaylistChangedEvent event);
    /**
     * Called when playing track is changed on server.
     * @param event event.
     */
    void trackChanged(MPDTrackChangedEvent event);
    /**
     * Called when MPD state changes on server.
     * @param event event.
     */
    void stateChanged(MPDStateChangedEvent event);
    /**
     * Called when MPD server repeat feature changes state.
     * @param event event.
     */
    void repeatChanged(MPDRepeatChangedEvent event);
    /**
     * Called when MPD server random feature changes state.
     * @param event event.
     */
    void randomChanged(MPDRandomChangedEvent event);
    /**
     * Called when MPD server connection changes state. (connected/disconnected)
     * @param event event.
     */
    void connectionStateChanged(MPDConnectionStateChangedEvent event);
    /**
     * Called when the MPD server update database starts and stops.
     * @param event event.
     */
    void updateStateChanged(MPDUpdateStateChangedEvent event);
}
