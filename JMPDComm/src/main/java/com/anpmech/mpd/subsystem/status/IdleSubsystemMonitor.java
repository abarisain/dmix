/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2015 The MPDroid Project
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

package com.anpmech.mpd.subsystem.status;

import com.anpmech.mpd.Log;
import com.anpmech.mpd.MPD;
import com.anpmech.mpd.MPDCommand;
import com.anpmech.mpd.MPDPlaylist;
import com.anpmech.mpd.connection.MPDConnection;
import com.anpmech.mpd.event.StatusChangeListener;
import com.anpmech.mpd.event.TrackPositionListener;
import com.anpmech.mpd.exception.MPDException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * A monitoring class representing the <A HREF="http://www.musicpd.org/doc/protocol/command_reference.html#command_idle"
 * target="_top"> idle</A> command response of the <A HREF="http://www.musicpd.org/doc/protocol"
 * target="_top">MPD protocol</A>.
 */
public class IdleSubsystemMonitor extends Thread {

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

    private static final boolean DEBUG = false;

    private static final String TAG = "IdleStatusMonitor";

    private final long mDelay;

    private final MPD mMPD;

    private final Queue<StatusChangeListener> mStatusChangeListeners;

    private final String[] mSupportedSubsystems;

    private final Queue<TrackPositionListener> mTrackPositionListeners;

    private volatile boolean mGiveup;

    /**
     * Constructs an IdleStatusMonitor.
     *
     * @param mpd                 MPD server to monitor.
     * @param delay               status query interval.
     * @param supportedSubsystems Idle subsystems to support, see IDLE fields in this class.
     */
    public IdleSubsystemMonitor(final MPD mpd, final long delay,
            final String[] supportedSubsystems) {
        super(TAG);

        mMPD = mpd;
        mDelay = delay;
        mGiveup = false;
        mStatusChangeListeners = new LinkedList<>();
        mTrackPositionListeners = new LinkedList<>();
        mSupportedSubsystems = supportedSubsystems.clone();
    }

    /**
     * Adds a {@code StatusChangeListener}.
     *
     * @param listener a {@code StatusChangeListener}.
     */
    public void addStatusChangeListener(final StatusChangeListener listener) {
        mStatusChangeListeners.add(listener);
    }

    /**
     * Adds a {@code TrackPositionListener}.
     *
     * @param listener a {@code TrackPositionListener}.
     */
    public void addTrackPositionListener(final TrackPositionListener listener) {
        mTrackPositionListeners.add(listener);
    }

    /**
     * Gracefully terminate tread.
     */
    public void giveup() {
        mGiveup = true;
    }

    public boolean isGivingUp() {
        return mGiveup;
    }

    /**
     * Main thread method
     */
    @Override
    public void run() {
        // initialize value cache
        boolean oldConnectionState = false;
        boolean connectionLost = false;

        /** Objects to keep cached in {@link MPD} */
        final MPDStatusMap status = mMPD.getStatus();
        final MPDPlaylist playlist = mMPD.getPlaylist();
        final MPDStatisticsMap statistics = mMPD.getStatistics();

        /** Just for initialization purposes */
        MPDStatus oldStatus = status;

        while (!mGiveup) {
            Boolean connectionState = Boolean.valueOf(mMPD.isConnected());
            boolean connectionStateChanged = false;

            if (connectionLost || oldConnectionState != connectionState) {
                if (mMPD.isConnected()) {
                    try {
                        oldStatus = status.getImmutableStatus();
                        statistics.update();
                        status.update();
                        playlist.refresh(status);
                    } catch (final IOException | MPDException e) {
                        Log.error(TAG, "Failed to force a status update.", e);
                    }
                }

                for (final StatusChangeListener listener : mStatusChangeListeners) {
                    listener.connectionStateChanged(connectionState.booleanValue(), connectionLost);
                }

                connectionLost = false;
                oldConnectionState = connectionState;
                connectionStateChanged = true;
            }

            if (connectionState.equals(Boolean.TRUE)) {
                // playlist
                try {
                    boolean dbChanged = false;
                    boolean statusChanged = false;
                    boolean stickerChanged = false;

                    if (connectionStateChanged) {
                        dbChanged = statusChanged = true;
                    } else {
                        final List<String> changes = waitForChanges();

                        oldStatus = status.getImmutableStatus();
                        status.update();

                        for (final String change : changes) {
                            switch (change.substring("changed: ".length())) {
                                case IDLE_DATABASE:
                                    statistics.update();
                                    dbChanged = true;
                                    statusChanged = true;
                                    break;
                                case IDLE_PLAYLIST:
                                    statusChanged = true;
                                    break;
                                case IDLE_STICKER:
                                    stickerChanged = true;
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

                    if (statusChanged) {
                        // playlist
                        final int oldPlaylistVersion = oldStatus.getPlaylistVersion();
                        if (connectionStateChanged ||
                                oldPlaylistVersion != status.getPlaylistVersion()) {
                            playlist.refresh(status);
                            for (final StatusChangeListener listener : mStatusChangeListeners) {
                                listener.playlistChanged(status, oldPlaylistVersion);
                            }
                        }

                        // song
                        /**
                         * songId is used here, otherwise, once consume mode is enabled getSongPos
                         * would never iterate without manual user playlist queue intervention and
                         * trackChanged() would never be called.
                         */
                        if (connectionStateChanged || oldStatus.getSongId() != status.getSongId()) {
                            for (final StatusChangeListener listener : mStatusChangeListeners) {
                                listener.trackChanged(status, oldStatus.getSongPos());
                            }
                        }

                        // time
                        if (connectionStateChanged ||
                                oldStatus.getElapsedTime() != status.getElapsedTime()) {
                            for (final TrackPositionListener listener : mTrackPositionListeners) {
                                listener.trackPositionChanged(status);
                            }
                        }

                        // state
                        final int oldState = oldStatus.getState();
                        if (connectionStateChanged || !status.isState(oldState)) {
                            for (final StatusChangeListener listener : mStatusChangeListeners) {
                                listener.stateChanged(status, oldState);
                            }
                        }

                        // volume
                        final int oldVolume = oldStatus.getVolume();
                        if (connectionStateChanged || oldVolume != status.getVolume()) {
                            for (final StatusChangeListener listener : mStatusChangeListeners) {
                                listener.volumeChanged(status, oldVolume);
                            }
                        }

                        // repeat
                        final boolean oldRepeat = oldStatus.isRepeat();
                        if (connectionStateChanged || oldRepeat != status.isRepeat()) {
                            for (final StatusChangeListener listener : mStatusChangeListeners) {
                                listener.repeatChanged(status.isRepeat());
                            }
                        }

                        // volume
                        if (connectionStateChanged || oldStatus.isRandom() != status.isRandom()) {
                            for (final StatusChangeListener listener : mStatusChangeListeners) {
                                listener.randomChanged(status.isRandom());
                            }
                        }

                        // update database
                        if (connectionStateChanged ||
                                oldStatus.isUpdating() != status.isUpdating()) {
                            for (final StatusChangeListener listener : mStatusChangeListeners) {
                                listener.libraryStateChanged(status.isUpdating(), dbChanged);
                            }
                        }
                    }

                    if (stickerChanged) {
                        if (DEBUG) {
                            Log.debug(TAG, "Sticker changed");
                        }
                        for (final StatusChangeListener listener : mStatusChangeListeners) {
                            listener.stickerChanged(status);
                        }
                    }
                } catch (final IOException e) {
                    // connection lost
                    connectionState = Boolean.FALSE;
                    connectionLost = true;
                    if (mMPD.isConnected()) {
                        Log.error(TAG, "Exception caught while looping.", e);
                    }
                } catch (final MPDException e) {
                    Log.error(TAG, "Exception caught while looping.", e);
                }
            }

            try {
                synchronized (this) {
                    if (!mMPD.isConnected()) {
                        wait(mDelay);
                    }
                }
            } catch (final InterruptedException e) {
                Log.error(TAG, "Interruption caught during disconnection and wait.", e);
            }
        }

    }

    /**
     * Wait for server changes using "idle" command on the dedicated connection.
     *
     * @return Data read from the server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    private List<String> waitForChanges() throws IOException, MPDException {
        final MPDConnection mpdIdleConnection = mMPD.getIdleConnection();
        final MPDCommand idleCommand = new MPDCommand(MPDCommand.MPD_CMD_IDLE,
                mSupportedSubsystems);

        while (mpdIdleConnection != null && mpdIdleConnection.isConnected()) {
            final List<String> data = mpdIdleConnection.send(idleCommand);

            if (data.isEmpty()) {
                continue;
            }

            return data;
        }
        throw new IOException("IDLE connection lost");
    }
}
