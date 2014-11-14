/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.helpers;

import com.namelessdev.mpdroid.ConnectionInfo;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.tools.SettingsHelper;
import com.namelessdev.mpdroid.tools.WeakLinkedList;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.event.TrackPositionListener;

import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;

import java.util.Collection;

/**
 * This Class implements the whole MPD Communication as a thread. It also "translates" the monitor
 * event of the JMPDComm Library back to the GUI-Thread, and allows to execute custom commands,
 * asynchronously.
 *
 * @author sag
 */
public class MPDAsyncHelper implements Handler.Callback {

    private static final int LOCAL_UID = 600;

    static final int EVENT_CONNECT_FAILED = LOCAL_UID + 2;

    static final int EVENT_CONNECT_SUCCEEDED = LOCAL_UID + 3;

    static final int EVENT_CONNECTION_STATE = LOCAL_UID + 4;

    static final int EVENT_PLAYLIST = LOCAL_UID + 5;

    static final int EVENT_RANDOM = LOCAL_UID + 6;

    static final int EVENT_REPEAT = LOCAL_UID + 7;

    static final int EVENT_SET_USE_CACHE = LOCAL_UID + 8;

    static final int EVENT_STATE = LOCAL_UID + 9;

    static final int EVENT_TRACK = LOCAL_UID + 10;

    static final int EVENT_TRACK_POSITION = LOCAL_UID + 11;

    static final int EVENT_UPDATE_STATE = LOCAL_UID + 12;

    static final int EVENT_VOLUME = LOCAL_UID + 13;

    static final int EVENT_STICKER_CHANGED = LOCAL_UID + 14;

    private static final String TAG = "MPDAsyncHelper";

    private static int sJobID = 0;

    public final MPD oMPD;

    private final Collection<AsyncExecListener> mAsyncExecListeners;

    private final Collection<ConnectionInfoListener> mConnectionInfoListeners;

    private final Collection<ConnectionListener> mConnectionListeners;

    private final Collection<StatusChangeListener> mStatusChangeListeners;

    private final Collection<TrackPositionListener> mTrackPositionListeners;

    private final Handler mWorkerHandler;

    private final MPDAsyncWorker oMPDAsyncWorker;

    private ConnectionInfo mConnectionInfo = new ConnectionInfo();

    public MPDAsyncHelper() {
        this(PreferenceManager.getDefaultSharedPreferences(MPDApplication.getInstance())
                .getBoolean(MPDAsyncWorker.USE_LOCAL_ALBUM_CACHE_KEY, false));
    }

    /**
     * Private constructor for static class
     */
    public MPDAsyncHelper(final boolean cached) {
        super();
        oMPD = new CachedMPD(cached);

        oMPDAsyncWorker = new MPDAsyncWorker(new Handler(this), oMPD);
        mWorkerHandler = oMPDAsyncWorker.startThread();
        new SettingsHelper(this).updateConnectionSettings();

        mAsyncExecListeners = new WeakLinkedList<>("AsyncExecListener");
        mConnectionListeners = new WeakLinkedList<>("ConnectionListener");
        mConnectionInfoListeners = new WeakLinkedList<>("ConnectionInfoListener");
        mStatusChangeListeners = new WeakLinkedList<>("StatusChangeListener");
        mTrackPositionListeners = new WeakLinkedList<>("TrackPositionListener");
    }

    public void addAsyncExecListener(final AsyncExecListener listener) {
        if (!mAsyncExecListeners.contains(listener)) {
            mAsyncExecListeners.add(listener);
        }
    }

    public void addConnectionInfoListener(final ConnectionInfoListener listener) {
        if (!mConnectionInfoListeners.contains(listener)) {
            mConnectionInfoListeners.add(listener);
        }
    }

    public void addConnectionListener(final ConnectionListener listener) {
        if (!mConnectionListeners.contains(listener)) {
            mConnectionListeners.add(listener);
        }
    }

    public void addStatusChangeListener(final StatusChangeListener listener) {
        if (!mStatusChangeListeners.contains(listener)) {
            mStatusChangeListeners.add(listener);
        }
    }

    public void addTrackPositionListener(final TrackPositionListener listener) {
        if (!mTrackPositionListeners.contains(listener)) {
            mTrackPositionListeners.add(listener);
        }
    }

    public void connect() {
        mWorkerHandler.sendEmptyMessage(MPDAsyncWorker.EVENT_CONNECT);
    }

    public void disconnect() {
        mWorkerHandler.sendEmptyMessage(MPDAsyncWorker.EVENT_DISCONNECT);
    }

    /**
     * Executes a Runnable Asynchronous. Meant to use for individual long during operations on
     * JMPDComm. Use this method only, when the code to execute is only used once in the project.
     * If its use more than once, implement individual events and listener in this class.
     *
     * @param run Runnable to execute in background thread.
     * @return JobID, which is brought back with the AsyncExecListener interface.
     */
    public int execAsync(final Runnable run) {
        final int activeJobID = sJobID;
        sJobID++;
        mWorkerHandler.obtainMessage(MPDAsyncWorker.EVENT_EXEC_ASYNC, activeJobID, 0, run)
                .sendToTarget();
        return activeJobID;
    }

    /**
     * Get the current {@code ConnectionInfo} object.
     *
     * @return A current {@code ConnectionInfo} object.
     */
    public ConnectionInfo getConnectionSettings() {
        return mConnectionInfo;
    }

    /**
     * This method handles Messages, which comes from the AsyncWorker. This
     * Message handler runs in the UI-Thread, and can therefore send the
     * information back to the listeners of the matching events...
     */
    @Override
    public final boolean handleMessage(final Message msg) {
        boolean result = true;

        try {
            final Object[] args = (Object[]) msg.obj;
            switch (msg.what) {
                case EVENT_CONNECTION_STATE:
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        listener.connectionStateChanged((Boolean) args[0], (Boolean) args[1]);
                    }
                    // Also notify Connection Listener...
                    if ((Boolean) args[0]) {
                        for (final ConnectionListener listener : mConnectionListeners) {
                            listener.connectionSucceeded("");
                        }
                    }
                    if ((Boolean) args[1]) {
                        for (final ConnectionListener listener : mConnectionListeners) {
                            listener.connectionFailed("Connection Lost");
                        }
                    }
                    break;
                case MPDAsyncWorker.EVENT_CONNECTION_CONFIG:
                    mConnectionInfo = (ConnectionInfo) args[0];
                    for (final ConnectionInfoListener listener : mConnectionInfoListeners) {
                        listener.onConnectionConfigChange(mConnectionInfo);
                    }
                    break;
                case EVENT_PLAYLIST:
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        listener.playlistChanged((MPDStatus) args[0], (Integer) args[1]);
                    }
                    break;
                case EVENT_RANDOM:
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        listener.randomChanged((Boolean) args[0]);
                    }
                    break;
                case EVENT_REPEAT:
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        listener.repeatChanged((Boolean) args[0]);
                    }
                    break;
                case EVENT_SET_USE_CACHE:
                    ((CachedMPD) oMPD).setUseCache((Boolean) args[0]);
                    break;
                case EVENT_STATE:
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        listener.stateChanged((MPDStatus) args[0], (int) args[1]);
                    }
                    break;
                case EVENT_TRACK:
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        listener.trackChanged((MPDStatus) args[0], (int) args[1]);
                    }
                    break;
                case EVENT_UPDATE_STATE:
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        listener.libraryStateChanged((Boolean) args[0], (Boolean) args[1]);
                    }
                    break;
                case EVENT_VOLUME:
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        listener.volumeChanged((MPDStatus) args[0], (Integer) args[1]);
                    }
                    break;
                case EVENT_TRACK_POSITION:
                    for (final TrackPositionListener listener : mTrackPositionListeners) {
                        listener.trackPositionChanged((MPDStatus) args[0]);
                    }
                    break;
                case EVENT_STICKER_CHANGED:
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        listener.stickerChanged((MPDStatus) args[0]);
                    }
                    break;
                case EVENT_CONNECT_FAILED:
                    for (final ConnectionListener listener : mConnectionListeners) {
                        listener.connectionFailed((String) args[0]);
                    }
                    break;
                case EVENT_CONNECT_SUCCEEDED:
                    for (final ConnectionListener listener : mConnectionListeners) {
                        listener.connectionSucceeded(null);
                    }
                    break;
                case MPDAsyncWorker.EVENT_EXEC_ASYNC_FINISHED:
                    // Asynchronous operation finished, call the listeners and supply the JobID...
                    for (final AsyncExecListener listener : mAsyncExecListeners) {
                        if (listener != null) {
                            listener.asyncExecSucceeded(msg.arg1);
                        }
                    }
                    break;
                default:
                    result = false;
                    break;
            }
        } catch (final ClassCastException ignored) {
            // happens when unknown message type is received
        }

        return result;
    }

    public boolean isStatusMonitorAlive() {
        return oMPDAsyncWorker.isStatusMonitorAlive();
    }

    public void removeAsyncExecListener(final AsyncExecListener listener) {
        mAsyncExecListeners.remove(listener);
    }

    public void removeConnectionInfoListener(final ConnectionInfoListener listener) {
        mConnectionInfoListeners.remove(listener);
    }

    public void removeConnectionListener(final ConnectionListener listener) {
        mConnectionListeners.remove(listener);
    }

    public void removeStatusChangeListener(final StatusChangeListener listener) {
        mStatusChangeListeners.remove(listener);
    }

    public void removeTrackPositionListener(final TrackPositionListener listener) {
        mTrackPositionListeners.remove(listener);
    }

    /**
     * Stores the {@code ConnectionInfo} object and sends it to the worker.
     *
     * @param connectionInfo A current {@code ConnectionInfo} object.
     */
    public void setConnectionSettings(final ConnectionInfo connectionInfo) {
        mConnectionInfo = connectionInfo;
        oMPDAsyncWorker.setConnectionSettings(connectionInfo);
    }

    public void setUseCache(final boolean useCache) {
        ((CachedMPD) oMPD).setUseCache(useCache);
    }

    public void startStatusMonitor(final String[] idleSubsystems) {
        Message.obtain(mWorkerHandler, MPDAsyncWorker.EVENT_START_STATUS_MONITOR, idleSubsystems)
                .sendToTarget();
    }

    public void stopStatusMonitor() {
        mWorkerHandler.sendEmptyMessage(MPDAsyncWorker.EVENT_STOP_STATUS_MONITOR);
    }

    // Interface for callback when Asynchronous operations are finished
    public interface AsyncExecListener {

        void asyncExecSucceeded(int jobID);
    }

    public interface ConnectionInfoListener {

        void onConnectionConfigChange(ConnectionInfo connectionInfo);
    }

    // PMix internal ConnectionListener interface
    public interface ConnectionListener {

        void connectionFailed(String message);

        void connectionSucceeded(String message);
    }
}
