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

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.tools.WeakLinkedList;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.MPDStatusMonitor;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.event.TrackPositionListener;
import org.a0z.mpd.exception.MPDServerException;

import java.net.UnknownHostException;
import java.util.Collection;

/**
 * This Class implements the whole MPD Communication as a thread. It also
 * "translates" the monitor event of the JMPDComm Library back to the
 * GUI-Thread, and allows to execute custom commands asynchronously.
 * 
 * @author sag
 */
public class MPDAsyncHelper extends Handler {

    public static final String TAG = "MPDAsyncHelper";

    // Interface for callback when Asynchronous operations are finished
    public interface AsyncExecListener {
        public void asyncExecSucceeded(int jobID);
    }

    // PMix internal ConnectionListener interface
    public interface ConnectionListener {
        public void connectionFailed(String message);

        public void connectionSucceeded(String message);
    }

    /**
     * Asynchronous worker thread-class for long during operations on JMPDComm
     */
    public class MPDAsyncWorker extends Handler implements StatusChangeListener,
            TrackPositionListener {
        public MPDAsyncWorker(Looper looper) {
            super(looper);
        }

        @Override
        public void connectionStateChanged(boolean connected, boolean connectionLost) {
            MPDAsyncHelper.this.obtainMessage(EVENT_CONNECTIONSTATE,
                    Tools.toObjectArray(connected, connectionLost)).sendToTarget();
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_CONNECT:
                    try {
                        MPDConnectionInfo conInfo = (MPDConnectionInfo) msg.obj;
                        if (oMPD != null) {
                            oMPD.connect(conInfo.sServer, conInfo.iPort, conInfo.sPassword);
                            MPDAsyncHelper.this.obtainMessage(EVENT_CONNECTSUCCEEDED)
                                    .sendToTarget();
                        }
                    } catch (MPDServerException | UnknownHostException e) {
                        Log.e(TAG, "Error while connecting to the server", e);
                        MPDAsyncHelper.this.obtainMessage(EVENT_CONNECTFAILED,
                                Tools.toObjectArray(e.getMessage())).sendToTarget();
                    }
                    break;
                case EVENT_STARTMONITOR:
                    oMonitor = new MPDStatusMonitor(oMPD, 500);
                    oMonitor.addStatusChangeListener(this);
                    oMonitor.addTrackPositionListener(this);
                    oMonitor.start();
                    break;
                case EVENT_STOPMONITOR:
                    if (oMonitor != null)
                        oMonitor.giveup();
                    break;
                case EVENT_DISCONNECT:
                    try {
                        if (oMPD != null)
                            oMPD.disconnect();
                        Log.d(MPDApplication.TAG, "Disconnected");
                    } catch (MPDServerException e) {
                        Log.e(MPDApplication.TAG, "Error on disconnect", e);// Silent
                                                                            // exception
                                                                            // are
                                                                            // dangerous
                    }
                    // Should not happen anymore
                    // catch (NullPointerException ex) {
                    // }
                    break;
                case EVENT_EXECASYNC:
                    Runnable run = (Runnable) msg.obj;
                    run.run();
                    MPDAsyncHelper.this.obtainMessage(EVENT_EXECASYNCFINISHED, msg.arg1, 0)
                            .sendToTarget();
                default:
                    break;
            }
        }

        @Override
        public void libraryStateChanged(boolean updating) {
            MPDAsyncHelper.this.obtainMessage(EVENT_UPDATESTATE, Tools.toObjectArray(updating))
                    .sendToTarget();
        }

        @Override
        public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
            MPDAsyncHelper.this.obtainMessage(EVENT_PLAYLIST,
                    Tools.toObjectArray(mpdStatus, oldPlaylistVersion)).sendToTarget();
        }

        @Override
        public void randomChanged(boolean random) {
            MPDAsyncHelper.this.obtainMessage(EVENT_RANDOM, Tools.toObjectArray(random))
                    .sendToTarget();
        }

        @Override
        public void repeatChanged(boolean repeating) {
            MPDAsyncHelper.this.obtainMessage(EVENT_REPEAT, Tools.toObjectArray(repeating))
                    .sendToTarget();
        }

        @Override
        public void stateChanged(MPDStatus mpdStatus, String oldState) {
            MPDAsyncHelper.this
                    .obtainMessage(EVENT_STATE, Tools.toObjectArray(mpdStatus, oldState))
                    .sendToTarget();
        }

        @Override
        public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
            MPDAsyncHelper.this
                    .obtainMessage(EVENT_TRACK, Tools.toObjectArray(mpdStatus, oldTrack))
                    .sendToTarget();
        }

        // Send all events as Messages back to the GUI-Thread
        @Override
        public void trackPositionChanged(MPDStatus status) {
            MPDAsyncHelper.this.obtainMessage(EVENT_TRACKPOSITION, Tools.toObjectArray(status))
                    .sendToTarget();
        }

        @Override
        public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
            MPDAsyncHelper.this.obtainMessage(EVENT_VOLUME,
                    Tools.toObjectArray(mpdStatus, oldVolume)).sendToTarget();
        }
    }

    public class MPDConnectionInfo {
        public String sServer;
        public int iPort;
        public String sPassword;
        public String sServerStreaming;
        public int iPortStreaming;
        public String sSuffixStreaming = "";

        public String getConnectionStreamingServer() {
            return conInfo.sServerStreaming == null ? sServer : sServerStreaming;
        }
    }

    // Event-ID's for PMix internal events...
    private static final int EVENT_CONNECT = 0;
    private static final int EVENT_DISCONNECT = 1;
    private static final int EVENT_CONNECTFAILED = 2;
    private static final int EVENT_CONNECTSUCCEEDED = 3;

    private static final int EVENT_STARTMONITOR = 4;
    private static final int EVENT_STOPMONITOR = 5;
    private static final int EVENT_EXECASYNC = 6;
    private static final int EVENT_EXECASYNCFINISHED = 7;
    // Event-ID's for JMPDComm events (from the listener)...
    private static final int EVENT_CONNECTIONSTATE = 11;
    private static final int EVENT_PLAYLIST = 12;
    private static final int EVENT_RANDOM = 13;
    private static final int EVENT_REPEAT = 14;
    private static final int EVENT_STATE = 15;

    private static final int EVENT_TRACK = 16;
    private static final int EVENT_UPDATESTATE = 17;
    private static final int EVENT_VOLUME = 18;
    private static final int EVENT_TRACKPOSITION = 19;
    private MPDAsyncWorker oMPDAsyncWorker;

    private HandlerThread oMPDAsyncWorkerThread;

    private MPDStatusMonitor oMonitor;

    public MPD oMPD;
    private static int iJobID = 0;
    // Listener Collections
    private Collection<ConnectionListener> connectionListeners;
    private Collection<StatusChangeListener> statusChangedListeners;

    private Collection<TrackPositionListener> trackPositionListeners;

    private Collection<AsyncExecListener> asyncExecListeners;

    // Current connection Information
    private MPDConnectionInfo conInfo;

    public MPDAsyncHelper() {
        this(true);
    }

    /**
     * Private constructor for static class
     */
    public MPDAsyncHelper(boolean cached) {
        oMPD = new CachedMPD(cached);
        oMPDAsyncWorkerThread = new HandlerThread("MPDAsyncWorker");
        oMPDAsyncWorkerThread.start();
        oMPDAsyncWorker = new MPDAsyncWorker(oMPDAsyncWorkerThread.getLooper());

        connectionListeners = new WeakLinkedList<ConnectionListener>("ConnectionListener");
        statusChangedListeners = new WeakLinkedList<StatusChangeListener>("StatusChangeListener");
        trackPositionListeners = new WeakLinkedList<TrackPositionListener>("TrackPositionListener");
        asyncExecListeners = new WeakLinkedList<AsyncExecListener>("AsyncExecListener");

        conInfo = new MPDConnectionInfo();
    }

    public void addAsyncExecListener(AsyncExecListener listener) {
        asyncExecListeners.add(listener);
    }

    public void addConnectionListener(ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    public void addStatusChangeListener(StatusChangeListener listener) {
        statusChangedListeners.add(listener);
    }

    public void addTrackPositionListener(TrackPositionListener listener) {
        trackPositionListeners.add(listener);
    }

    public void connect() {
        oMPDAsyncWorker.obtainMessage(EVENT_CONNECT, conInfo).sendToTarget();
    }

    public void disconnect() {
        oMPDAsyncWorker.obtainMessage(EVENT_DISCONNECT).sendToTarget();
    }

    /**
     * Executes a Runnable Asynchronous. Meant to use for individual long during
     * operations on JMPDComm. Use this method only, when the code to execute is
     * only used once in the project. If its use more than once, implement
     * individual events and listener in this class.
     * 
     * @param run Runnable to execute async
     * @return JobID, which is brought back with the AsyncExecListener
     *         interface...
     */
    public int execAsync(Runnable run) {
        int actjobid = iJobID++;
        oMPDAsyncWorker.obtainMessage(EVENT_EXECASYNC, actjobid, 0, run).sendToTarget();
        return actjobid;
    }

    public MPDConnectionInfo getConnectionSettings() {
        return conInfo;
    }

    /**
     * This method handles Messages, which comes from the AsyncWorker. This
     * Message handler runs in the UI-Thread, and can therefore send the
     * information back to the listeners of the matching events...
     */
    public void handleMessage(Message msg) {
        try {
            Object[] args = (Object[]) msg.obj;
            switch (msg.what) {
                case EVENT_CONNECTIONSTATE:
                    for (StatusChangeListener listener : statusChangedListeners)
                        listener.connectionStateChanged((Boolean) args[0], (Boolean) args[1]);
                    // Also notify Connection Listener...
                    if ((Boolean) args[0])
                        for (ConnectionListener listener : connectionListeners)
                            listener.connectionSucceeded("");
                    if ((Boolean) args[1])
                        for (ConnectionListener listener : connectionListeners)
                            listener.connectionFailed("Connection Lost");
                    break;
                case EVENT_PLAYLIST:
                    for (StatusChangeListener listener : statusChangedListeners)
                        listener.playlistChanged((MPDStatus) args[0], (Integer) args[1]);
                    break;
                case EVENT_RANDOM:
                    for (StatusChangeListener listener : statusChangedListeners)
                        listener.randomChanged((Boolean) args[0]);
                    break;
                case EVENT_REPEAT:
                    for (StatusChangeListener listener : statusChangedListeners)
                        listener.repeatChanged((Boolean) args[0]);
                    break;
                case EVENT_STATE:
                    for (StatusChangeListener listener : statusChangedListeners)
                        listener.stateChanged((MPDStatus) args[0], (String) args[1]);
                    break;
                case EVENT_TRACK:
                    for (StatusChangeListener listener : statusChangedListeners)
                        listener.trackChanged((MPDStatus) args[0], (Integer) args[1]);
                    break;
                case EVENT_UPDATESTATE:
                    for (StatusChangeListener listener : statusChangedListeners)
                        listener.libraryStateChanged((Boolean) args[0]);
                    break;
                case EVENT_VOLUME:
                    for (StatusChangeListener listener : statusChangedListeners)
                        listener.volumeChanged((MPDStatus) args[0], ((Integer) args[1]));
                    break;
                case EVENT_TRACKPOSITION:
                    for (TrackPositionListener listener : trackPositionListeners)
                        listener.trackPositionChanged((MPDStatus) args[0]);
                    break;
                case EVENT_CONNECTFAILED:
                    for (ConnectionListener listener : connectionListeners)
                        listener.connectionFailed((String) args[0]);
                    break;
                case EVENT_CONNECTSUCCEEDED:
                    for (ConnectionListener listener : connectionListeners)
                        listener.connectionSucceeded(null);
                    break;
                case EVENT_EXECASYNCFINISHED:
                    // Asynchronous operation finished, call the listeners and
                    // supply the JobID...
                    for (AsyncExecListener listener : asyncExecListeners)
                        if (listener != null)
                            listener.asyncExecSucceeded(msg.arg1);
                    break;
            }
        } catch (ClassCastException e) {
            // happens when unknown message type is received
        }
    }

    public boolean isMonitorAlive() {
        if (oMonitor == null)
            return false;
        else
            return oMonitor.isAlive() & !oMonitor.isGivingUp();
    }

    public void removeAsyncExecListener(AsyncExecListener listener) {
        asyncExecListeners.remove(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
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
        oMPDAsyncWorker.obtainMessage(EVENT_STARTMONITOR).sendToTarget();
    }

    public void stopMonitor() {
        oMPDAsyncWorker.obtainMessage(EVENT_STOPMONITOR).sendToTarget();
    }

}
