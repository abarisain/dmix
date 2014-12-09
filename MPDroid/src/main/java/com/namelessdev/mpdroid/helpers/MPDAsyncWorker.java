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
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.cover.GracenoteCover;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.MPDStatusMonitor;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.event.TrackPositionListener;
import org.a0z.mpd.exception.MPDException;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

import java.io.IOException;

/**
 * Asynchronous worker thread-class for long during operations on JMPDComm.
 */
public class MPDAsyncWorker implements Handler.Callback,
        SharedPreferences.OnSharedPreferenceChangeListener,
        StatusChangeListener,
        TrackPositionListener {

    static final String USE_LOCAL_ALBUM_CACHE_KEY = "useLocalAlbumCache";

    private static final int LOCAL_UID = 500;

    static final int EVENT_CONNECT = LOCAL_UID + 1;

    static final int EVENT_CONNECTION_CONFIG = LOCAL_UID + 2;

    static final int EVENT_DISCONNECT = LOCAL_UID + 3;

    static final int EVENT_EXEC_ASYNC = LOCAL_UID + 4;

    static final int EVENT_EXEC_ASYNC_FINISHED = LOCAL_UID + 5;

    static final int EVENT_START_STATUS_MONITOR = LOCAL_UID + 6;

    static final int EVENT_STOP_STATUS_MONITOR = LOCAL_UID + 7;

    private static final String TAG = "MPDAsyncWorker";

    /** A handler for the MPDAsyncHelper object. */
    private final Handler mHelperHandler;

    private final MPD mMPD;

    private final SharedPreferences mSettings;

    /** A store for the current connection information. */
    private ConnectionInfo mConInfo = new ConnectionInfo();

    private String[] mIdleSubsystems;

    private MPDStatusMonitor mStatusMonitor;

    private Handler mWorkerHandler;

    MPDAsyncWorker(final Handler helperHandler, final MPD mpd) {
        super();

        mSettings = PreferenceManager.getDefaultSharedPreferences(MPDApplication.getInstance());
        mSettings.registerOnSharedPreferenceChangeListener(this);

        mHelperHandler = helperHandler;
        mMPD = mpd;
    }

    /** Connects the {@code MPD} object to the media server. */
    private void connect() {
        try {
            mMPD.connect(mConInfo.server, mConInfo.port, mConInfo.password);
            mHelperHandler.sendEmptyMessage(MPDAsyncHelper.EVENT_CONNECT_SUCCEEDED);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Error while connecting to the server.", e);
            mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_CONNECT_FAILED,
                    Tools.toObjectArray(e.getMessage())).sendToTarget();
        }
    }

    @Override
    public void connectionStateChanged(final boolean connected, final boolean connectionLost) {
        mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_CONNECTION_STATE,
                Tools.toObjectArray(connected, connectionLost)).sendToTarget();
    }

    /** Disconnects the {@code MPD} object from the media server. */
    private void disconnect() {
        try {
            mMPD.disconnect();
            Log.d(TAG, "Disconnected.");
        } catch (final IOException e) {
            Log.e(TAG, "Error on disconnect.", e);
        }
    }

    /**
     * Handles messages to the off UI thread {@code Handler}/{@code Looper}.
     *
     * @param msg The incoming message to handle.
     * @return True if message was handled, false otherwise.
     */
    @Override
    public final boolean handleMessage(final Message msg) {
        boolean result = true;

        switch (msg.what) {
            case EVENT_CONNECT:
                connect();
                break;
            case EVENT_START_STATUS_MONITOR:
                mIdleSubsystems = (String[]) msg.obj;
                startStatusMonitor();
                break;
            case EVENT_STOP_STATUS_MONITOR:
                stopStatusMonitor();
                break;
            case EVENT_DISCONNECT:
                disconnect();
                break;
            case EVENT_EXEC_ASYNC:
                final Runnable run = (Runnable) msg.obj;
                run.run();
                mHelperHandler.obtainMessage(EVENT_EXEC_ASYNC_FINISHED, msg.arg1, 0).sendToTarget();
                break;
            default:
                result = false;
                break;
        }

        return result;
    }

    /**
     * Checks the JMPDComm MPD Status Monitor for activity.
     *
     * @return True if the JMPDComm MPD Status Monitor is active, false otherwise.
     */
    public boolean isStatusMonitorAlive() {
        final boolean isMonitorAlive;

        if (mStatusMonitor != null && mStatusMonitor.isAlive() && !mStatusMonitor.isGivingUp()) {
            isMonitorAlive = true;
        } else {
            isMonitorAlive = false;
        }

        return isMonitorAlive;
    }

    @Override
    public void libraryStateChanged(final boolean updating, final boolean dbChanged) {
        mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_UPDATE_STATE,
                Tools.toObjectArray(updating, dbChanged)).sendToTarget();
    }

    /**
     * Called when a shared preference is changed, added, or removed. This
     * may be called even if a preference is set to its existing value.
     *
     * <p>This callback will be run on your main thread.
     *
     * @param sharedPreferences The {@link android.content.SharedPreferences} that received
     *                          the change.
     * @param key               The key of the preference that was changed, added, or
     */
    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
            final String key) {
        switch (key) {
            case USE_LOCAL_ALBUM_CACHE_KEY:
                final boolean useAlbumCache = sharedPreferences.getBoolean(key, false);

                mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_SET_USE_CACHE, useAlbumCache);
                break;
            case GracenoteCover.CUSTOM_CLIENT_ID_KEY:
                final SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove(GracenoteCover.USER_ID);
                editor.commit();
                break;
            default:
                break;
        }
    }

    @Override
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {
        mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_PLAYLIST,
                Tools.toObjectArray(mpdStatus, oldPlaylistVersion)).sendToTarget();
    }

    @Override
    public void randomChanged(final boolean random) {
        mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_RANDOM, Tools.toObjectArray(random))
                .sendToTarget();
    }

    @Override
    public void repeatChanged(final boolean repeating) {
        mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_REPEAT, Tools.toObjectArray(repeating))
                .sendToTarget();
    }

    /**
     * Sets the connection settings.
     *
     * @param connectionInfo A current {@code ConnectionInfo} object.
     */
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
        }
    }

    /** Starts the JMPDComm MPD Status Monitor. */
    private void startStatusMonitor() {
        mStatusMonitor =
                new MPDStatusMonitor(mMPD, DateUtils.SECOND_IN_MILLIS / 2L, mIdleSubsystems);
        mStatusMonitor.addStatusChangeListener(this);
        mStatusMonitor.addTrackPositionListener(this);
        mStatusMonitor.start();
    }

    /**
     * Initiates the worker thread {@code Handler} in an off UI thread {@code Looper}.
     *
     * @return A {@code Handler} for this object.
     */
    final Handler startThread() {
        final HandlerThread handlerThread = new HandlerThread("MPDAsyncWorker");

        handlerThread.start();
        mWorkerHandler = new Handler(handlerThread.getLooper(), this);

        return mWorkerHandler;
    }

    @Override
    public void stateChanged(final MPDStatus mpdStatus, final int oldState) {
        mHelperHandler
                .obtainMessage(MPDAsyncHelper.EVENT_STATE, Tools.toObjectArray(mpdStatus, oldState))
                .sendToTarget();
    }

    @Override
    public void stickerChanged(final MPDStatus mpdStatus) {
        mHelperHandler
                .obtainMessage(MPDAsyncHelper.EVENT_STICKER_CHANGED, Tools.toObjectArray(mpdStatus))
                .sendToTarget();
    }

    /** Stops the JMPDComm MPD Status Monitor */
    private void stopStatusMonitor() {
        if (mStatusMonitor != null) {
            mStatusMonitor.giveup();
        }
    }

    @Override
    public void trackChanged(final MPDStatus mpdStatus, final int oldTrack) {
        mHelperHandler
                .obtainMessage(MPDAsyncHelper.EVENT_TRACK, Tools.toObjectArray(mpdStatus, oldTrack))
                .sendToTarget();
    }

    @Override
    public void trackPositionChanged(final MPDStatus status) {
        mHelperHandler
                .obtainMessage(MPDAsyncHelper.EVENT_TRACK_POSITION, Tools.toObjectArray(status))
                .sendToTarget();
    }

    @Override
    public void volumeChanged(final MPDStatus mpdStatus, final int oldVolume) {
        mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_VOLUME,
                Tools.toObjectArray(mpdStatus, oldVolume)).sendToTarget();
    }
}
