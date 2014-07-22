/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.helpers;

import com.namelessdev.mpdroid.ConnectionInfo;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.MPDStatusMonitor;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.event.TrackPositionListener;
import org.a0z.mpd.exception.MPDServerException;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.format.DateUtils;
import android.util.Log;

import java.net.UnknownHostException;

/**
 * Asynchronous worker thread-class for long during operations on JMPDComm.
 */
public class MPDAsyncWorker implements Handler.Callback,
        StatusChangeListener,
        TrackPositionListener {

    private static final int LOCAL_UID = 500;

    static final int EVENT_CONNECT = LOCAL_UID + 1;

    static final int EVENT_CONNECTION_CONFIG = LOCAL_UID + 2;

    static final int EVENT_DISCONNECT = LOCAL_UID + 3;

    static final int EVENT_EXECASYNC = LOCAL_UID + 4;

    static final int EVENT_EXECASYNCFINISHED = LOCAL_UID + 5;

    static final int EVENT_RECONNECT = LOCAL_UID + 6;

    static final int EVENT_STARTMONITOR = LOCAL_UID + 7;

    static final int EVENT_STOPMONITOR = LOCAL_UID + 8;

    private static final String TAG = "MPDAsyncWorker";

    static Handler mWorkerHandler;

    private final Handler mHelperHandler;

    private final MPD mMPD;

    private ConnectionInfo mConInfo;

    private MPDStatusMonitor mStatusMonitor;

    MPDAsyncWorker(final Handler helperHandler, final MPD mpd) {
        super();

        mHelperHandler = helperHandler;
        mMPD = mpd;
    }

    private void connect() {
        try {
            if (mMPD != null) {
                mMPD.connect(mConInfo.server, mConInfo.port, mConInfo.password);
                mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_CONNECTSUCCEEDED)
                        .sendToTarget();
            }
        } catch (final MPDServerException | UnknownHostException e) {
            Log.e(TAG, "Error while connecting to the server.", e);
            mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_CONNECTFAILED,
                    Tools.toObjectArray(e.getMessage())).sendToTarget();
        }
    }

    @Override
    public void connectionStateChanged(boolean connected, boolean connectionLost) {
        mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_CONNECTIONSTATE,
                Tools.toObjectArray(connected, connectionLost)).sendToTarget();
    }

    private void disconnect() {
        try {
            if (mMPD != null) {
                mMPD.disconnect();
            }
            Log.d(TAG, "Disconnected.");
        } catch (final MPDServerException e) {
            Log.e(TAG, "Error on disconnect.", e);
        }
    }

    @Override
    public final boolean handleMessage(final Message msg) {
        boolean result = true;

        switch (msg.what) {
            case EVENT_CONNECT:
                connect();
                break;
            case EVENT_RECONNECT:
                reconnect();
                break;
            case EVENT_STARTMONITOR:
                startStatusMonitor();
                break;
            case EVENT_STOPMONITOR:
                stopMonitor();
                break;
            case EVENT_DISCONNECT:
                disconnect();
                break;
            case EVENT_EXECASYNC:
                Runnable run = (Runnable) msg.obj;
                run.run();
                mHelperHandler.obtainMessage(EVENT_EXECASYNCFINISHED, msg.arg1, 0)
                        .sendToTarget();
                break;
            default:
                result = false;
                break;
        }

        return result;
    }

    public boolean isMonitorAlive() {
        if (mStatusMonitor == null) {
            return false;
        } else {
            return mStatusMonitor.isAlive() & !mStatusMonitor.isGivingUp();
        }
    }

    @Override
    public void libraryStateChanged(boolean updating, boolean dbChanged) {
        mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_UPDATESTATE, Tools.toObjectArray(updating,
                dbChanged)).sendToTarget();
    }

    @Override
    public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
        mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_PLAYLIST,
                Tools.toObjectArray(mpdStatus, oldPlaylistVersion)).sendToTarget();
    }

    @Override
    public void randomChanged(boolean random) {
        mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_RANDOM, Tools.toObjectArray(random))
                .sendToTarget();
    }

    private void reconnect() {
        final boolean isMonitorAlive = isMonitorAlive();

        /** Don't continue before the monitor is stopped. */
        if (isMonitorAlive) {
            stopMonitor();

            try {
                /** Give up waiting after a couple of seconds. */
                mStatusMonitor.join(2L * DateUtils.SECOND_IN_MILLIS);
            } catch (final InterruptedException ignored) {
            }
        }

        if (mMPD != null && mMPD.isConnected()) {
            disconnect();
            connect();
        }

        if (isMonitorAlive) {
            startStatusMonitor();
        }
    }

    @Override
    public void repeatChanged(boolean repeating) {
        mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_REPEAT, Tools.toObjectArray(repeating))
                .sendToTarget();
    }

    public final void setConnectionSettings(final ConnectionInfo connectionInfo) {
        if (mConInfo == null) {
            if (connectionInfo != null) {
                mConInfo = connectionInfo;
            }
        } else if (connectionInfo.serverInfoChanged || connectionInfo.streamingServerInfoChanged
                || connectionInfo.wasNotificationPersistent !=
                connectionInfo.isNotificationPersistent) {
            mHelperHandler.obtainMessage(EVENT_CONNECTION_CONFIG, mConInfo).sendToTarget();
            mConInfo = connectionInfo;

            if (mConInfo.serverInfoChanged) {
                Log.d(TAG, "Connection changed, connecting to " + mConInfo.server);
                mWorkerHandler.sendEmptyMessage(EVENT_RECONNECT);
            } else {
                Log.d(TAG, "Server info had not changed!");
            }
        }
    }

    private void startStatusMonitor() {
        mStatusMonitor = new MPDStatusMonitor(mMPD, 500L);
        mStatusMonitor.addStatusChangeListener(this);
        mStatusMonitor.addTrackPositionListener(this);
        mStatusMonitor.start();
    }

    final Handler startThread() {
        final HandlerThread handlerThread = new HandlerThread("MPDAsyncWorker");

        handlerThread.start();
        mWorkerHandler = new Handler(handlerThread.getLooper(), this);

        return mWorkerHandler;
    }

    @Override
    public void stateChanged(MPDStatus mpdStatus, String oldState) {
        mHelperHandler
                .obtainMessage(MPDAsyncHelper.EVENT_STATE, Tools.toObjectArray(mpdStatus, oldState))
                .sendToTarget();
    }

    private void stopMonitor() {
        if (mStatusMonitor != null) {
            mStatusMonitor.giveup();
        }
    }

    @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
        mHelperHandler
                .obtainMessage(MPDAsyncHelper.EVENT_TRACK, Tools.toObjectArray(mpdStatus, oldTrack))
                .sendToTarget();
    }

    @Override
    public void trackPositionChanged(MPDStatus status) {
        mHelperHandler
                .obtainMessage(MPDAsyncHelper.EVENT_TRACKPOSITION, Tools.toObjectArray(status))
                .sendToTarget();
    }

    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
        mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_VOLUME,
                Tools.toObjectArray(mpdStatus, oldVolume)).sendToTarget();
    }
}
