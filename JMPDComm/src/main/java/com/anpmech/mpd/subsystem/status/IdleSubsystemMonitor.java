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
import com.anpmech.mpd.concurrent.MPDExecutor;
import com.anpmech.mpd.connection.MPDConnectionStatus;
import com.anpmech.mpd.connection.MonoIOMPDConnection;
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

    private final MPD mMPD;

    private final Queue<StatusChangeListener> mStatusChangeListeners;

    private final String[] mSupportedSubsystems;

    private final Queue<TrackPositionListener> mTrackPositionListeners;

    private volatile boolean mGiveup;

    /**
     * Constructs an IdleStatusMonitor.
     *
     * @param mpd                 MPD server to monitor.
     * @param supportedSubsystems Idle subsystems to support, see IDLE fields in this class.
     */
    public IdleSubsystemMonitor(final MPD mpd, final String[] supportedSubsystems) {
        super(TAG);

        mMPD = mpd;
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
        /** Objects to keep cached in {@link MPD} */
        final MPDStatusMap status = mMPD.getStatus();
        final MPDPlaylist playlist = mMPD.getPlaylist();
        final MPDStatisticsMap statistics = mMPD.getStatistics();
        final MonoIOMPDConnection connection = mMPD.getIdleConnection().getThreadUnsafeConnection();
        final MPDConnectionStatus connectionStatus = connection.getConnectionStatus();

        /** Just for initialization purposes */
        MPDStatus oldStatus = status;
        long lastConnected = Long.MIN_VALUE;

        while (!mGiveup) {
            final long statusChangeTime = connectionStatus.getChangeTime();
            final boolean connectionReset = lastConnected != statusChangeTime;

            if (connectionReset) {
                lastConnected = statusChangeTime;

                if (connectionStatus.isConnected()) {
                    try {
                        oldStatus = status.getImmutableStatus();
                        statistics.update();
                        status.update();
                        playlist.refresh(status);
                    } catch (final IOException | MPDException e) {
                        Log.error(TAG, "Failed to force a status update.", e);
                    }
                } else {
                    try {
                        connectionStatus.waitForConnection();
                    } catch (final InterruptedException ignored) {
                    }
                    continue;
                }
            }

            // playlist
            try {
                boolean dbChanged = false;
                boolean statusChanged = false;
                boolean stickerChanged = false;

                if (connectionReset) {
                    dbChanged = true;
                    statusChanged = true;
                } else {
                    final List<String> changes = connection.send(MPDCommand.MPD_CMD_IDLE,
                            mSupportedSubsystems);

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
                    if (connectionReset || oldPlaylistVersion != status.getPlaylistVersion()) {
                        playlist.refresh(status);

                        for (final StatusChangeListener listener : mStatusChangeListeners) {
                            MPDExecutor.submitCallback(new Runnable() {
                                @Override
                                public void run() {
                                    listener.playlistChanged(status, oldPlaylistVersion);
                                }
                            });
                        }
                    }

                    // song
                    /**
                     * songId is used here, otherwise, once consume mode is enabled getSongPos
                     * would never iterate without manual user playlist queue intervention and
                     * trackChanged() would never be called.
                     */
                    if (connectionReset || oldStatus.getSongId() != status.getSongId()) {
                        final int oldSongPos = oldStatus.getSongPos();
                        for (final StatusChangeListener listener : mStatusChangeListeners) {
                            MPDExecutor.submitCallback(new Runnable() {
                                @Override
                                public void run() {
                                    listener.trackChanged(status, oldSongPos);
                                }
                            });
                        }
                    }

                    // time
                    if (connectionReset ||
                            oldStatus.getElapsedTime() != status.getElapsedTime()) {
                        for (final TrackPositionListener listener : mTrackPositionListeners) {
                            MPDExecutor.submitCallback(new Runnable() {
                                @Override
                                public void run() {
                                    listener.trackPositionChanged(status);
                                }
                            });
                        }
                    }

                    // state
                    final int oldState = oldStatus.getState();
                    if (connectionReset || !status.isState(oldState)) {
                        for (final StatusChangeListener listener : mStatusChangeListeners) {
                            MPDExecutor.submitCallback(new Runnable() {
                                @Override
                                public void run() {
                                    listener.stateChanged(status, oldState);
                                }
                            });
                        }
                    }

                    // volume
                    final int oldVolume = oldStatus.getVolume();
                    if (connectionReset || oldVolume != status.getVolume()) {
                        for (final StatusChangeListener listener : mStatusChangeListeners) {
                            MPDExecutor.submitCallback(new Runnable() {
                                @Override
                                public void run() {
                                    listener.volumeChanged(status, oldVolume);
                                }
                            });
                        }
                    }

                    // repeat
                    final boolean oldRepeat = oldStatus.isRepeat();
                    if (connectionReset || oldRepeat != status.isRepeat()) {
                        for (final StatusChangeListener listener : mStatusChangeListeners) {
                            MPDExecutor.submitCallback(new Runnable() {
                                @Override
                                public void run() {
                                    listener.repeatChanged(status.isRepeat());
                                }
                            });
                        }
                    }

                    // random
                    if (connectionReset || oldStatus.isRandom() != status.isRandom()) {
                        for (final StatusChangeListener listener : mStatusChangeListeners) {
                            MPDExecutor.submitCallback(new Runnable() {
                                @Override
                                public void run() {
                                    listener.randomChanged(status.isRandom());
                                }
                            });
                        }
                    }

                    // update database
                    if (connectionReset ||
                            oldStatus.isUpdating() != status.isUpdating()) {
                        final boolean myDbChanged = dbChanged;
                        for (final StatusChangeListener listener : mStatusChangeListeners) {
                            MPDExecutor.submitCallback(new Runnable() {
                                @Override
                                public void run() {
                                    listener.libraryStateChanged(status.isUpdating(),
                                            myDbChanged);
                                }
                            });
                        }
                    }
                }

                if (stickerChanged) {
                    if (DEBUG) {
                        Log.debug(TAG, "Sticker changed");
                    }
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        MPDExecutor.submitCallback(new Runnable() {
                            @Override
                            public void run() {
                                listener.stickerChanged(status);
                            }
                        });
                    }
                }
            } catch (final IOException e) {
                if (mMPD.isConnected()) {
                    Log.error(TAG, "Exception caught while looping.", e);
                }
            } catch (final MPDException e) {
                Log.error(TAG, "Exception caught while looping.", e);
            }
        }
    }
}
