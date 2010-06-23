/*
 * Created on 12/02/2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.a0z.mpd;

import java.util.Iterator;
import java.util.LinkedList;

import org.a0z.mpd.event.MPDConnectionStateChangedEvent;
import org.a0z.mpd.event.MPDPlaylistChangedEvent;
import org.a0z.mpd.event.MPDRandomChangedEvent;
import org.a0z.mpd.event.MPDRepeatChangedEvent;
import org.a0z.mpd.event.MPDStateChangedEvent;
import org.a0z.mpd.event.MPDTrackChangedEvent;
import org.a0z.mpd.event.MPDTrackPositionChangedEvent;
import org.a0z.mpd.event.MPDUpdateStateChangedEvent;
import org.a0z.mpd.event.MPDVolumeChangedEvent;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.event.TrackPositionListener;

/**
 * Monitors MPD Server and sends events on status changes.
 * @author Felipe Gustavo de Almeida
 * @version $Id: MPDStatusMonitor.java 2941 2005-02-09 02:34:21Z galmeida $
 */
public class MPDStatusMonitor extends Thread {
    private int delay;

    private MPD mpd;

    private boolean giveup;

    private LinkedList<StatusChangeListener> statusChangedListeners;

    private LinkedList<TrackPositionListener> trackPositionChangedListeners;

    /**
     * Constructs a MPDStatusMonitor.
     * @param mpd MPD server to monitor.
     * @param delay status query interval.
     */
    public MPDStatusMonitor(MPD mpd, int delay) {
        super("MPDStatusMonitor");
        this.mpd = mpd;
        this.giveup = false;
        statusChangedListeners = new LinkedList<StatusChangeListener>();
        trackPositionChangedListeners = new LinkedList<TrackPositionListener>();
        this.delay = delay;
    }

    /**
     * Main thread method.
     */
    public void run() {
        int oldSong = -1;
        int oldPlaylistVersion = -1;
        long oldElapsedTime = -1;
        String oldState = "";
        int oldVolume = -1;
        Boolean oldUpdating = null;
        Boolean oldRepeat = null;
        Boolean oldRandom = null;
        Boolean oldConnectionState = null;
        boolean connectionLost = false;

        while (!giveup) {
            Boolean connectionState = Boolean.valueOf(mpd.isConnected());
            
            if (connectionLost || oldConnectionState != connectionState) {
                for (StatusChangeListener listener : statusChangedListeners) {
                	listener.connectionStateChanged(new MPDConnectionStateChangedEvent(connectionState.booleanValue(), connectionLost));
                }
                connectionLost = false;
                oldConnectionState = connectionState;
            }

            if (connectionState == Boolean.TRUE) {
                //playlist
                try {
                    MPDStatus status = mpd.getStatus();
                    int song = status.getSongPos();
                    int playlistVersion = status.getPlaylistVersion();
                    long elapsedTime = status.getElapsedTime();
                    String state = status.getState();
                    int volume = status.getVolume();
                    Boolean updating = Boolean.valueOf(status.getUpdating());
                    Boolean repeat = Boolean.valueOf(status.isRepeat());
                    Boolean random = Boolean.valueOf(status.isRandom());

                    //playlist
                    if (oldPlaylistVersion != playlistVersion && playlistVersion != -1) {
                        for (StatusChangeListener listener : statusChangedListeners) {
                            listener.playlistChanged(new MPDPlaylistChangedEvent(status, oldPlaylistVersion));
                        }
                        oldPlaylistVersion = playlistVersion;
                    }

                    //song
                    if (oldSong != song) {
                        for (StatusChangeListener listener : statusChangedListeners) {
                            listener.trackChanged(new MPDTrackChangedEvent(oldSong, status));
                        }
                        oldSong = song;
                    }

                    //time
                    if (oldElapsedTime != elapsedTime) {
                        for (TrackPositionListener listener : trackPositionChangedListeners) {
                            listener.trackPositionChanged(new MPDTrackPositionChangedEvent(status));
                        }
                        oldElapsedTime = elapsedTime;
                    }

                    //state
                    if (oldState != state) {
                        for (StatusChangeListener listener : statusChangedListeners) {
                            listener.stateChanged(new MPDStateChangedEvent(oldState, status));
                        }
                        oldState = state;
                    }

                    //volume
                    if (oldVolume != volume) {
                        for (StatusChangeListener listener : statusChangedListeners) {
                            listener.volumeChanged(new MPDVolumeChangedEvent(oldVolume, status));
                        }
                        oldVolume = volume;
                    }

                    //repeat
                    if (oldRepeat != repeat) {
                        Iterator it = statusChangedListeners.iterator();
                        while (it.hasNext()) {
                            ((StatusChangeListener) it.next()).repeatChanged(new MPDRepeatChangedEvent(repeat.booleanValue()));
                        }
                        oldRepeat = repeat;
                    }

                    //volume
                    if (oldRandom != random) {
                        for (StatusChangeListener listener : statusChangedListeners) {
                            listener.randomChanged(new MPDRandomChangedEvent(random.booleanValue()));
                        }
                        oldRandom = random;
                    }

                    //update database
                    if (oldUpdating != updating) {
                        for (StatusChangeListener listener : statusChangedListeners) {
                            listener.updateStateChanged(new MPDUpdateStateChangedEvent(updating.booleanValue()));
                        }
                        oldUpdating = updating;
                    }
                } catch (MPDConnectionException e) { 
                	//connection lost
                    connectionState = Boolean.FALSE;
                    connectionLost = true;
                } catch (MPDServerException e) {
                    e.printStackTrace();
                }
            }
            try {
                synchronized (this) {
                    this.wait(this.delay);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * Adds a <code>StatusChangeListener</code>.
     * @param listener a <code>StatusChangeListener</code>.
     */
    public void addStatusChangeListener(StatusChangeListener listener) {
        statusChangedListeners.add(listener);
    }

    /**
     * Adds a <code>TrackPositionListener</code>.
     * @param listener a <code>TrackPositionListener</code>.
     */
    public void addTrackPositionListener(TrackPositionListener listener) {
        trackPositionChangedListeners.add(listener);
    }

    /**
     * Gracefull terminate tread.
     */
    public void giveup() {
        this.giveup = true;
    }
    
    public boolean isGivingUp()
    {
    	return this.giveup;
    }
}
