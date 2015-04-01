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
import com.anpmech.mpd.concurrent.MPDFuture;
import com.anpmech.mpd.concurrent.ResponseFuture;
import com.anpmech.mpd.connection.MPDConnectionStatus;
import com.anpmech.mpd.connection.MonoIOMPDConnection;
import com.anpmech.mpd.event.StatusChangeListener;
import com.anpmech.mpd.event.TrackPositionListener;
import com.anpmech.mpd.exception.MPDException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

/**
 * A monitoring class representing the <A HREF="http://www.musicpd.org/doc/protocol/command_reference.html#command_idle"
 * target="_top"> idle</A> command response of the <A HREF="http://www.musicpd.org/doc/protocol"
 * target="_top">MPD protocol</A>.
 */
public class IdleSubsystemMonitor implements Runnable {

    /**
     * The song database has been modified after update.
     *
     * @see #IDLE_UPDATE
     */
    public static final String IDLE_DATABASE = "database";

    /**
     * A message was received on a channel this client is subscribed to.
     */
    public static final String IDLE_MESSAGE = "message";

    /**
     * Emitted after the mixer volume has been modified.
     */
    public static final String IDLE_MIXER = "mixer";

    /**
     * Emitted after an option (repeat, random, cross fade, Replay Gain) modification.
     */
    public static final String IDLE_OPTIONS = "options";

    /**
     * Emitted after a output has been enabled or disabled.
     */
    public static final String IDLE_OUTPUT = "output";

    /**
     * Emitted after upon current playing status change.
     */
    public static final String IDLE_PLAYER = "player";

    /**
     * Emitted after the playlist queue has been modified.
     */
    public static final String IDLE_PLAYLIST = "playlist";

    /**
     * Emitted after the sticker database has been modified.
     */
    public static final String IDLE_STICKER = "sticker";

    /**
     * Emitted after a server side stored playlist has been added, removed or modified.
     */
    public static final String IDLE_STORED_PLAYLIST = "stored_playlist";

    /**
     * Emitted after a client has added or removed subscription to a channel.
     */
    public static final String IDLE_SUBSCRIPTION = "subscription";

    /**
     * Emitted after a database update has started or finished.
     *
     * @see #IDLE_DATABASE
     */
    public static final String IDLE_UPDATE = "update";

    /**
     * The debug boolean flag.
     */
    private static final boolean DEBUG = false;

    /**
     * The general error message.
     */
    private static final String GENERAL_ERROR = "Exception caught while looping.";

    /**
     * This is the length of time (in milliseconds) between failure retries of critical commands.
     */
    private static final long PENALIZATION_TIMEOUT = TimeUnit.SECONDS.toMillis(10L);

    /**
     * The class log identifier.
     */
    private static final String TAG = "IdleStatusMonitor";

    /**
     * The MPD object to keep updated.
     */
    private final MPD mMPD;

    /**
     * The status change listeners to keep updated with this tracker.
     */
    private final List<StatusChangeListener> mStatusChangeListeners;

    /**
     * The track position change listener.
     */
    private final List<TrackPositionListener> mTrackPositionListeners;

    /**
     * This Future tracks the status of the Idle command.
     */
    private ResponseFuture mIdleTracker;

    /**
     * This Future tracks the status of this monitor.
     */
    private MPDFuture mMonitorTracker;

    /**
     * This is the loop terminator.
     */
    private volatile boolean mStop;

    /**
     * The supported idle subsystems from IDLE_*.
     */
    private String[] mSupportedSubsystems;

    /**
     * Sole constructor.
     *
     * @param mpd MPD server to monitor.
     */
    public IdleSubsystemMonitor(final MPD mpd) {
        super();

        mMPD = mpd;
        mStop = false;
        mStatusChangeListeners = new ArrayList<>();
        mTrackPositionListeners = new ArrayList<>();
    }

    /**
     * Adds a {@code StatusChangeListener}.
     *
     * @param listener a {@code StatusChangeListener}.
     */
    public void addStatusChangeListener(final StatusChangeListener listener) {
        if (!mStatusChangeListeners.contains(listener)) {
            mStatusChangeListeners.add(listener);
        }
    }

    /**
     * Adds a {@code TrackPositionListener}.
     *
     * @param listener a {@code TrackPositionListener}.
     */
    public void addTrackPositionListener(final TrackPositionListener listener) {
        if (!mTrackPositionListeners.contains(listener)) {
            mTrackPositionListeners.add(listener);
        }
    }

    /**
     * An IdleStatusMonitor status tracker, tracking started or stopped status.
     *
     * @return True if the IdleStatusMonitor is monitoring, false otherwise.
     */
    public boolean isStopped() {
        return mMonitorTracker == null || mMonitorTracker.isDone();
    }

    /**
     * An IdleStatusMonitor status tracker, tracking only whether the tracker is starting.
     *
     * @return True if the IdleStatusMonitor is in the process of stopping.
     */
    public boolean isStopping() {
        return mStop;
    }

    /**
     * Removes a {@code StatusChangeListener}.
     *
     * @param listener a {@code StatusChangeListener}.
     */
    public void removeStatusChangeListener(final StatusChangeListener listener) {
        mStatusChangeListeners.remove(listener);
    }

    /**
     * Removes a {@code TrackPositionListener}.
     *
     * @param listener a {@code TrackPositionListener}.
     */
    public void removeTrackPositionListener(final TrackPositionListener listener) {
        mTrackPositionListeners.remove(listener);
    }

    /**
     * Starts executing the active part of the class' code. This method is
     * called when a thread is started that has been created with a class which
     * implements {@code Runnable}.
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

        while (!mStop) {
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

            boolean dbChanged = false;
            boolean statusChanged = false;
            boolean stickerChanged = false;
            boolean storedPlaylistChanged = false;

            if (connectionReset) {
                dbChanged = true;
                statusChanged = true;
            } else {
                mIdleTracker = connection.submit(MPDCommand.MPD_CMD_IDLE,
                        mSupportedSubsystems);

                /**
                 * We block here until the idle command response returns or
                 * {@link #mIdleTracker} is cancelled by another thread.
                 */
                try {
                    final Iterator<Map.Entry<String, String>> changes =
                            mIdleTracker.get().splitListIterator();

                    oldStatus = status.getImmutableStatus();
                    status.update();

                    while (changes.hasNext()) {
                        switch (changes.next().getValue()) {
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
                            case IDLE_STORED_PLAYLIST:
                                storedPlaylistChanged = true;
                                break;
                            default:
                                statusChanged = true;
                                break;
                        }

                        if (dbChanged && statusChanged) {
                            break;
                        }
                    }
                } catch (final IOException | MPDException e) {
                    Log.error(TAG, GENERAL_ERROR, e);
                    synchronized (this) {
                        try {
                            Log.error(TAG, "Sleeping for " +
                                    TimeUnit.MILLISECONDS.toSeconds(PENALIZATION_TIMEOUT) +
                                    " seconds due to error.");
                            wait(PENALIZATION_TIMEOUT);
                        } catch (final InterruptedException ignored) {
                        }
                    }
                    continue;
                }
            }

            try {
                if (statusChanged) {
                    // playlist
                    final int oldPlaylistVersion = oldStatus.getPlaylistVersion();
                    if (connectionReset || oldPlaylistVersion != status.getPlaylistVersion()) {
                        playlist.refresh(status);

                        for (final StatusChangeListener listener : mStatusChangeListeners) {
                            MPDExecutor.submitCallback(new Runnable() {
                                @Override
                                public void run() {
                                    listener.playlistChanged(oldPlaylistVersion);
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
                                    listener.trackChanged(oldSongPos);
                                }
                            });
                        }
                    }

                    // time
                    if (connectionReset || oldStatus.getElapsedTime() != status.getElapsedTime()) {
                        for (final TrackPositionListener listener : mTrackPositionListeners) {
                            MPDExecutor.submitCallback(new Runnable() {
                                @Override
                                public void run() {
                                    listener.trackPositionChanged();
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
                                    listener.stateChanged(oldState);
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
                                    listener.volumeChanged(oldVolume);
                                }
                            });
                        }
                    }

                    // repeat
                    if (connectionReset || oldStatus.isRepeat() != status.isRepeat()) {
                        for (final StatusChangeListener listener : mStatusChangeListeners) {
                            MPDExecutor.submitCallback(new Runnable() {
                                @Override
                                public void run() {
                                    listener.repeatChanged();
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
                                    listener.randomChanged();
                                }
                            });
                        }
                    }

                    // update database
                    if (connectionReset || oldStatus.isUpdating() != status.isUpdating()) {
                        final boolean myDbChanged = dbChanged;
                        for (final StatusChangeListener listener : mStatusChangeListeners) {
                            MPDExecutor.submitCallback(new Runnable() {
                                @Override
                                public void run() {
                                    listener.libraryStateChanged(status.isUpdating(), myDbChanged);
                                }
                            });
                        }
                    }

                    if (connectionReset || storedPlaylistChanged) {
                        for (final StatusChangeListener listener : mStatusChangeListeners) {
                            MPDExecutor.submitCallback(new Runnable() {
                                @Override
                                public void run() {
                                    listener.storedPlaylistChanged();
                                }
                            });
                        }
                    }
                }

                if (stickerChanged) {
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        MPDExecutor.submitCallback(new Runnable() {
                            @Override
                            public void run() {
                                listener.stickerChanged();
                            }
                        });
                    }
                }
            } catch (final CancellationException ignored) {
                /** If a FutureTask is cancelled, just continue. */
                continue;
            } catch (final IOException e) {
                if (mMPD.isConnected()) {
                    Log.error(TAG, GENERAL_ERROR, e);
                }
            } catch (final MPDException e) {
                Log.error(TAG, GENERAL_ERROR, e);
            }
        }
    }

    /**
     * Sets the idle subsystems to track, if empty or not set defaulting to all subsystems.
     *
     * @param supportedSubsystems Idle subsystems to support, see IDLE fields in this class.
     */
    public void setSupportedSubsystems(final String... supportedSubsystems) {
        mSupportedSubsystems = supportedSubsystems;
    }

    /**
     * Stops the IdleStatusMonitor, if running, then starts.
     *
     * <p>This method does not stop a running IdleSubsystemMonitor, so stop first, if
     * applicable.</p>
     */
    public void start() {
        mStop = false;

        /** We don't need to store the FutureTask for this one, we stop in a gentler way. */
        mMonitorTracker = MPDExecutor.submit(this);
    }

    /**
     * Stops the IdleStatusMonitor.
     */
    public void stop() {
        mStop = true;

        if (mIdleTracker != null) {
            mIdleTracker.cancel(true);
        }
    }
}
