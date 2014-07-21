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
import com.namelessdev.mpdroid.tools.SettingsHelper;
import com.namelessdev.mpdroid.tools.WeakLinkedList;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.event.TrackPositionListener;

import android.os.Handler;
import android.os.Message;

import java.util.Collection;

/**
 * This Class implements the whole MPD Communication as a thread. It also
 * "translates" the monitor event of the JMPDComm Library back to the
 * GUI-Thread, and allows to execute custom commands asynchronously.
 *
 * @author sag
 */
public class MPDAsyncHelper implements Handler.Callback {

    private static final String TAG = "MPDAsyncHelper";

    private static final int LOCAL_UID = 600;

    static final int EVENT_CONNECTFAILED = LOCAL_UID + 2;

    static final int EVENT_CONNECTSUCCEEDED = LOCAL_UID + 3;

    static final int EVENT_CONNECTIONSTATE = LOCAL_UID + 4;

    static final int EVENT_PLAYLIST = LOCAL_UID + 5;

    static final int EVENT_RANDOM = LOCAL_UID + 6;

    static final int EVENT_REPEAT = LOCAL_UID + 7;

    static final int EVENT_STATE = LOCAL_UID + 8;

    static final int EVENT_TRACK = LOCAL_UID + 9;

    static final int EVENT_TRACKPOSITION = LOCAL_UID + 10;

    static final int EVENT_UPDATESTATE = LOCAL_UID + 11;

    static final int EVENT_VOLUME = LOCAL_UID + 12;

    private static int iJobID = 0;

    private final Handler mHelperHandler;

    public MPD oMPD;

    private MPDAsyncWorker oMPDAsyncWorker;

    // Listener Collections
    private Collection<ConnectionListener> connectionListeners;

    private Collection<ConnectionInfoListener> mConnectionInfoListeners;

    private Collection<StatusChangeListener> statusChangedListeners;

    private Collection<TrackPositionListener> trackPositionListeners;

    private Collection<AsyncExecListener> asyncExecListeners;

    private Handler mWorkerHandler;

    private ConnectionInfo mConnectionInfo;

    public MPDAsyncHelper() {
        this(true);
    }

    /**
     * Private constructor for static class
     */
    public MPDAsyncHelper(boolean cached) {
        oMPD = new CachedMPD(cached);

        mHelperHandler = new Handler(this);
        oMPDAsyncWorker = new MPDAsyncWorker(mHelperHandler, oMPD);
        mWorkerHandler = oMPDAsyncWorker.startThread();
        new SettingsHelper(this).updateConnectionSettings();

        connectionListeners = new WeakLinkedList<ConnectionListener>("ConnectionListener");
        mConnectionInfoListeners = new WeakLinkedList<>("ConnectionInfoListener");
        statusChangedListeners = new WeakLinkedList<StatusChangeListener>("StatusChangeListener");
        trackPositionListeners = new WeakLinkedList<TrackPositionListener>("TrackPositionListener");
        asyncExecListeners = new WeakLinkedList<AsyncExecListener>("AsyncExecListener");
    }

    public void addAsyncExecListener(AsyncExecListener listener) {
        if (!asyncExecListeners.contains(listener)) {
            asyncExecListeners.add(listener);
        }
    }

    public void addConnectionListener(ConnectionListener listener) {
        if (!connectionListeners.contains(listener)) {
            connectionListeners.add(listener);
        }
    }

    public void addConnectionInfoListener(final ConnectionInfoListener listener) {
        if (!mConnectionInfoListeners.contains(listener)) {
            mConnectionInfoListeners.add(listener);
        }
    }

    public void addStatusChangeListener(StatusChangeListener listener) {
        if (!statusChangedListeners.contains(listener)) {
            statusChangedListeners.add(listener);
        }
    }

    public void addTrackPositionListener(TrackPositionListener listener) {
        if (!trackPositionListeners.contains(listener)) {
            trackPositionListeners.add(listener);
        }
    }

    public void connect() {
        mWorkerHandler.sendEmptyMessage(MPDAsyncWorker.EVENT_CONNECT);
    }

    public void disconnect() {
        mWorkerHandler.sendEmptyMessage(MPDAsyncWorker.EVENT_DISCONNECT);
    }

    /**
     * Executes a Runnable Asynchronous. Meant to use for individual long during
     * operations on JMPDComm. Use this method only, when the code to execute is
     * only used once in the project. If its use more than once, implement
     * individual events and listener in this class.
     *
     * @param run Runnable to execute async
     * @return JobID, which is brought back with the AsyncExecListener
     * interface...
     */
    public int execAsync(Runnable run) {
        int actjobid = iJobID++;
        mWorkerHandler.obtainMessage(MPDAsyncWorker.EVENT_EXECASYNC, actjobid, 0, run)
                .sendToTarget();
        return actjobid;
    }

    public ConnectionInfo getConnectionSettings() {
        return mConnectionInfo;
    }

    public void setConnectionSettings(final ConnectionInfo connectionInfo) {
        mConnectionInfo = connectionInfo;
        oMPDAsyncWorker.setConnectionSettings(connectionInfo);
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
            Object[] args = (Object[]) msg.obj;
            switch (msg.what) {
                case EVENT_CONNECTIONSTATE:
                    for (StatusChangeListener listener : statusChangedListeners) {
                        listener.connectionStateChanged((Boolean) args[0], (Boolean) args[1]);
                    }
                    // Also notify Connection Listener...
                    if ((Boolean) args[0]) {
                        for (ConnectionListener listener : connectionListeners) {
                            listener.connectionSucceeded("");
                        }
                    }
                    if ((Boolean) args[1]) {
                        for (ConnectionListener listener : connectionListeners) {
                            listener.connectionFailed("Connection Lost");
                        }
                    }
                    break;
                case MPDAsyncWorker.EVENT_CONNECTION_CHANGED:
                    mConnectionInfo = (ConnectionInfo) args[0];
                    for (final ConnectionInfoListener listener : mConnectionInfoListeners) {
                        listener.onConnectionConfigChange(mConnectionInfo);
                    }
                    break;
                case EVENT_PLAYLIST:
                    for (StatusChangeListener listener : statusChangedListeners) {
                        listener.playlistChanged((MPDStatus) args[0], (Integer) args[1]);
                    }
                    break;
                case EVENT_RANDOM:
                    for (StatusChangeListener listener : statusChangedListeners) {
                        listener.randomChanged((Boolean) args[0]);
                    }
                    break;
                case EVENT_REPEAT:
                    for (StatusChangeListener listener : statusChangedListeners) {
                        listener.repeatChanged((Boolean) args[0]);
                    }
                    break;
                case EVENT_STATE:
                    for (StatusChangeListener listener : statusChangedListeners) {
                        listener.stateChanged((MPDStatus) args[0], (String) args[1]);
                    }
                    break;
                case EVENT_TRACK:
                    for (StatusChangeListener listener : statusChangedListeners) {
                        listener.trackChanged((MPDStatus) args[0], (Integer) args[1]);
                    }
                    break;
                case EVENT_UPDATESTATE:
                    for (StatusChangeListener listener : statusChangedListeners) {
                        listener.libraryStateChanged((Boolean) args[0], (Boolean) args[1]);
                    }
                    break;
                case EVENT_VOLUME:
                    for (StatusChangeListener listener : statusChangedListeners) {
                        listener.volumeChanged((MPDStatus) args[0], ((Integer) args[1]));
                    }
                    break;
                case EVENT_TRACKPOSITION:
                    for (TrackPositionListener listener : trackPositionListeners) {
                        listener.trackPositionChanged((MPDStatus) args[0]);
                    }
                    break;
                case EVENT_CONNECTFAILED:
                    for (ConnectionListener listener : connectionListeners) {
                        listener.connectionFailed((String) args[0]);
                    }
                    break;
                case EVENT_CONNECTSUCCEEDED:
                    for (ConnectionListener listener : connectionListeners) {
                        listener.connectionSucceeded(null);
                    }
                    break;
                case MPDAsyncWorker.EVENT_EXECASYNCFINISHED:
                    // Asynchronous operation finished, call the listeners and
                    // supply the JobID...
                    for (AsyncExecListener listener : asyncExecListeners) {
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

    public boolean isMonitorAlive() {
        return oMPDAsyncWorker.isMonitorAlive();
    }

    public void removeAsyncExecListener(AsyncExecListener listener) {
        asyncExecListeners.remove(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    public void removeConnectionInfoListener(final ConnectionInfoListener listener) {
        mConnectionInfoListeners.remove(listener);
    }

    public void removeStatusChangeListener(StatusChangeListener listener) {
        statusChangedListeners.remove(listener);
    }

    public void removeTrackPositionListener(TrackPositionListener listener) {
        trackPositionListeners.remove(listener);
    }

    public void setUseCache(boolean useCache) {
        ((CachedMPD) oMPD).setUseCache(useCache);
    }

    public void startMonitor() {
        mWorkerHandler.sendEmptyMessage(MPDAsyncWorker.EVENT_STARTMONITOR);
    }

    public void stopMonitor() {
        mWorkerHandler.sendEmptyMessage(MPDAsyncWorker.EVENT_STOPMONITOR);
    }

    // Interface for callback when Asynchronous operations are finished
    public interface AsyncExecListener {

        public void asyncExecSucceeded(int jobID);
    }

    // PMix internal ConnectionListener interface
    public interface ConnectionListener {

        public void connectionFailed(String message);

        public void connectionSucceeded(String message);
    }

    public interface ConnectionInfoListener {

        void onConnectionConfigChange(ConnectionInfo connectionInfo);
    }

}
