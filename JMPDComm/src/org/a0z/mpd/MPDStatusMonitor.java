/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice,this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.a0z.mpd;

import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.event.TrackPositionListener;
import org.a0z.mpd.exception.MPDConnectionException;
import org.a0z.mpd.exception.MPDServerException;

import java.util.LinkedList;
import java.util.List;

/**
 * Monitors MPD Server and sends events on status changes.
 * 
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
     * 
     * @param mpd MPD server to monitor.
     * @param delay status query interval.
     */
    public MPDStatusMonitor(MPD mpd, int delay) {
        super("MPDStatusMonitor");

        this.mpd = mpd;
        this.delay = delay;
        this.giveup = false;
        this.statusChangedListeners = new LinkedList<StatusChangeListener>();
        this.trackPositionChangedListeners = new LinkedList<TrackPositionListener>();

        // integrate MPD stuff into listener lists
        addStatusChangeListener(mpd.getPlaylist());
    }

    /**
     * Adds a <code>StatusChangeListener</code>.
     * 
     * @param listener a <code>StatusChangeListener</code>.
     */
    public void addStatusChangeListener(StatusChangeListener listener) {
        statusChangedListeners.add(listener);
    }

    /**
     * Adds a <code>TrackPositionListener</code>.
     * 
     * @param listener a <code>TrackPositionListener</code>.
     */
    public void addTrackPositionListener(TrackPositionListener listener) {
        trackPositionChangedListeners.add(listener);
    }

    /**
     * Gracefully terminate tread.
     */
    public void giveup() {
        this.giveup = true;
    }

    public boolean isGivingUp() {
        return this.giveup;
    }

    /**
     * Main thread method
     */
    public void run() {
        // initialize value cache
        int oldSong = -1;
        int oldSongId = -1;
        int oldPlaylistVersion = -1;
        long oldElapsedTime = -1;
        String oldState = "";
        int oldVolume = -1;
        boolean oldUpdating = false;
        boolean oldRepeat = false;
        boolean oldRandom = false;
        boolean oldConnectionState = false;
        boolean connectionLost = false;

        while (!giveup) {
            Boolean connectionState = Boolean.valueOf(mpd.isConnected());
            boolean connectionStateChanged = false;

            if (connectionLost || oldConnectionState != connectionState) {
                for (StatusChangeListener listener : statusChangedListeners) {
                    listener.connectionStateChanged(connectionState.booleanValue(), connectionLost);
                }
                connectionLost = false;
                oldConnectionState = connectionState;
                connectionStateChanged = true;
            }

            if (connectionState == Boolean.TRUE) {
                // playlist
                try {
                    boolean dbChanged = false;
                    boolean statusChanged = false;

                    if (connectionStateChanged) {
                        dbChanged = statusChanged = true;
                    } else {
                        List<String> changes = mpd.waitForChanges();

                        if (null == changes || changes.isEmpty()) {
                            continue;
                        }

                        for (String change : changes) {
                            if (change.startsWith("changed: database")) {
                                dbChanged = true;
                                statusChanged = true;
                                break;
                            } else if (change.startsWith("changed: update")) {
                                dbChanged = true;
                            } else if (change.startsWith("changed: playlist")
                                    || change.startsWith("changed: player") ||
                                    change.startsWith("changed: mixer")
                                    || change.startsWith("changed: output")
                                    || change.startsWith("changed: options")) {
                                statusChanged = true;
                            }
                            if (dbChanged && statusChanged) {
                                break;
                            }
                        }
                    }

                    if (dbChanged) {
                        mpd.getStatistics();
                    }
                    if (statusChanged) {
                        MPDStatus status = mpd.getStatus(true);

                        // playlist
                        if (connectionStateChanged
                                || (oldPlaylistVersion != status.getPlaylistVersion() && status
                                        .getPlaylistVersion() != -1)) {
                            // Lets update our own copy
                            for (StatusChangeListener listener : statusChangedListeners)
                                listener.playlistChanged(status, oldPlaylistVersion);
                            oldPlaylistVersion = status.getPlaylistVersion();
                        }

                        // song
                        /**
                         * songId is used here, otherwise, once consume mode is enabled getSongPos
                         * would never iterate without manual user playlist queue intervention and
                         * trackChanged() would never be called.
                         */
                        if (connectionStateChanged || oldSongId != status.getSongId()) {
                            for (StatusChangeListener listener : statusChangedListeners) {
                                listener.trackChanged(status, oldSong);
                            }
                            oldSong = status.getSongPos();
                            oldSongId = status.getSongId();
                        }

                        // time
                        if (connectionStateChanged || oldElapsedTime != status.getElapsedTime()) {
                            for (TrackPositionListener listener : trackPositionChangedListeners) {
                                listener.trackPositionChanged(status);
                            }
                            oldElapsedTime = status.getElapsedTime();
                        }

                        // state
                        if (connectionStateChanged || !oldState.equals(status.getState())) {
                            for (StatusChangeListener listener : statusChangedListeners) {
                                listener.stateChanged(status, oldState);
                            }
                            oldState = status.getState();
                        }

                        // volume
                        if (connectionStateChanged || oldVolume != status.getVolume()) {
                            for (StatusChangeListener listener : statusChangedListeners) {
                                listener.volumeChanged(status, oldVolume);
                            }
                            oldVolume = status.getVolume();
                        }

                        // repeat
                        if (connectionStateChanged || oldRepeat != status.isRepeat()) {
                            for (StatusChangeListener listener : statusChangedListeners) {
                                listener.repeatChanged(status.isRepeat());
                            }
                            oldRepeat = status.isRepeat();
                        }

                        // volume
                        if (connectionStateChanged || oldRandom != status.isRandom()) {
                            for (StatusChangeListener listener : statusChangedListeners) {
                                listener.randomChanged(status.isRandom());
                            }
                            oldRandom = status.isRandom();
                        }

                        // update database
                        if (connectionStateChanged || oldUpdating != status.isUpdating()) {
                            for (StatusChangeListener listener : statusChangedListeners) {
                                listener.libraryStateChanged(status.isUpdating());
                            }
                            oldUpdating = status.isUpdating();
                        }
                    }
                } catch (MPDConnectionException e) {
                    // connection lost
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
}
