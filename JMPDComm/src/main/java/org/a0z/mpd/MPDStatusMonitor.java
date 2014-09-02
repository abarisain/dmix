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

import android.util.Log;

import java.util.LinkedList;
import java.util.List;

/**
 * Monitors MPD Server and sends events on status changes.
 */
public class MPDStatusMonitor extends Thread {

    /** The song database has been modified after update. */
    public static final String IDLE_DATABASE = "database";

    /** A message was received on a channel this client is subscribed to. */
    public static final String IDLE_MESSAGE = "message";

    /** Emitted after the mixer volume has been modified. */
    public static final String IDLE_MIXER = "mixer";

    /** Emitted after an option (repeat, random, cross fade, Replay Gain) modification. */
    public static final String IDLE_OPTIONS = "options";

    /** Emitted after a output has been enabled or disabled. */
    public static final String IDLE_OUTPUT = "output";

    /** Emitted after upon current playing status change. */
    public static final String IDLE_PLAYER = "player";

    /** Emitted after the playlist queue has been modified. */
    public static final String IDLE_PLAYLIST = "playlist";

    /** Emitted after the sticker database has been modified. */
    public static final String IDLE_STICKER = "sticker";

    /** Emitted after a server side stored playlist has been added, removed or modified. */
    public static final String IDLE_STORED_PLAYLIST = "stored_playlist";

    /** Emitted after a client has added or removed subscription to a channel. */
    public static final String IDLE_SUBSCRIPTION = "subscription";

    /** Emitted after a database update has started or finished. See IDLE_DATABASE */
    public static final String IDLE_UPDATE = "update";

    private static final String TAG = "MPDStatusMonitor";

    private final long delay;

    private final MPD mpd;

    private volatile boolean giveup;

    private final LinkedList<StatusChangeListener> statusChangedListeners;

    private final LinkedList<TrackPositionListener> trackPositionChangedListeners;

    private final MPDCommand mIdleCommand;

    /**
     * Constructs a MPDStatusMonitor.
     *
     * @param mpd           MPD server to monitor.
     * @param delay         status query interval.
     * @param supportedIdle Idle subsystems to support, see IDLE fields in this class.
     */
    public MPDStatusMonitor(MPD mpd, long delay, final String[] supportedIdle) {
        super("MPDStatusMonitor");

        this.mpd = mpd;
        this.delay = delay;
        this.giveup = false;
        this.statusChangedListeners = new LinkedList<>();
        this.trackPositionChangedListeners = new LinkedList<>();
        mIdleCommand = new MPDCommand(MPDCommand.MPD_CMD_IDLE, supportedIdle);
    }

    /**
     * Adds a {@code StatusChangeListener}.
     *
     * @param listener a {@code StatusChangeListener}.
     */
    public void addStatusChangeListener(StatusChangeListener listener) {
        statusChangedListeners.add(listener);
    }

    /**
     * Adds a {@code TrackPositionListener}.
     *
     * @param listener a {@code TrackPositionListener}.
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
        final MPDStatus status = mpd.getStatus();
        int oldSong = -1;
        int oldSongId = -1;
        int oldPlaylistVersion = -1;
        long oldElapsedTime = -1L;
        int oldState = MPDStatus.STATE_UNKNOWN;
        int oldVolume = -1;
        boolean oldUpdating = false;
        boolean oldRepeat = false;
        boolean oldRandom = false;
        boolean oldConnectionState = false;
        boolean connectionLost = false;
        final MPDPlaylist playlist = mpd.getPlaylist();

        while (!giveup) {
            Boolean connectionState = Boolean.valueOf(mpd.isConnected());
            boolean connectionStateChanged = false;

            if (connectionLost || oldConnectionState != connectionState) {
                for (StatusChangeListener listener : statusChangedListeners) {
                    listener.connectionStateChanged(connectionState.booleanValue(), connectionLost);
                }

                if (mpd.isConnected()) {
                    try {
                        mpd.updateStatus();
                        playlist.refresh(status);
                    } catch (final MPDServerException e) {
                        Log.e(TAG, "Failed to force a status update.", e);
                    }
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
                        final List<String> changes = waitForChanges();

                        mpd.updateStatus();

                        for (final String change : changes) {
                            switch(change.substring("changed: ".length())) {
                                case "database":
                                    dbChanged = true;
                                    statusChanged = true;
                                    break;
                                case "playlist":
                                    playlist.refresh(status);
                                    statusChanged = true;
                                    break;
                                default:
                                    statusChanged = true;
                                    break;
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
                        // playlist
                        if (connectionStateChanged
                                || (oldPlaylistVersion != status.getPlaylistVersion() && status
                                .getPlaylistVersion() != -1)) {
                            // Lets update our own copy
                            for (StatusChangeListener listener : statusChangedListeners) {
                                listener.playlistChanged(status, oldPlaylistVersion);
                            }
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
                        if (connectionStateChanged || !status.isState(oldState)) {
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
                                listener.libraryStateChanged(status.isUpdating(), dbChanged);
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
                    this.wait(delay);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * Wait for server changes using "idle" command on the dedicated connection.
     *
     * @return Data read from the server.
     * @throws MPDServerException if an error occur while contacting server
     */
    private List<String> waitForChanges() throws MPDServerException {
        final MPDConnection mpdIdleConnection = mpd.getMpdIdleConnection();

        while (mpdIdleConnection != null && mpdIdleConnection.isConnected()) {
            final List<String> data = mpdIdleConnection.sendCommand(mIdleCommand);

            if (data.isEmpty()) {
                continue;
            }

            return data;
        }
        throw new MPDConnectionException("IDLE connection lost");
    }
}
