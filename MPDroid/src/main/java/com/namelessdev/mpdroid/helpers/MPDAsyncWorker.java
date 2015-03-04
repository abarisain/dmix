/*
 * Copyright (C) 2010-2015 The MPDroid Project
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

import com.anpmech.mpd.MPD;
import com.anpmech.mpd.exception.MPDException;
import com.namelessdev.mpdroid.ConnectionInfo;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.cover.GracenoteCover;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;

/**
 * Asynchronous worker thread-class for long during operations on JMPDComm.
 */
public class MPDAsyncWorker implements Handler.Callback,
        SharedPreferences.OnSharedPreferenceChangeListener {

    static final String USE_LOCAL_ALBUM_CACHE_KEY = "useLocalAlbumCache";

    private static final int LOCAL_UID = 500;

    static final int EVENT_CONNECT = LOCAL_UID + 1;

    static final int EVENT_CONNECTION_CONFIG = LOCAL_UID + 2;

    static final int EVENT_DISCONNECT = LOCAL_UID + 3;

    static final int EVENT_EXEC_ASYNC = LOCAL_UID + 4;

    static final int EVENT_EXEC_ASYNC_FINISHED = LOCAL_UID + 5;

    private static final String TAG = "MPDAsyncWorker";

    /** A handler for the MPDAsyncHelper object. */
    private final Handler mHelperHandler;

    private final MPD mMPD;

    /** A store for the current connection information. */
    private ConnectionInfo mConInfo = new ConnectionInfo();

    MPDAsyncWorker(final Handler helperHandler, final MPD mpd) {
        super();

        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(MPDApplication.getInstance());
        settings.registerOnSharedPreferenceChangeListener(this);

        mHelperHandler = helperHandler;
        mMPD = mpd;
    }

    /** Connects the {@code MPD} object to the media server. */
    private void connect() {
        try {
            mMPD.setDefaultPassword(mConInfo.password);
            mMPD.connect(mConInfo.server, mConInfo.port);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Error while connecting to the server.", e);
        }
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
     * Called when a shared preference is changed, added, or removed. This may be called even if a
     * preference is set to its existing value.
     * <p/>
     * <p>This callback will be run on your main thread.
     *
     * @param sharedPreferences The {@link android.content.SharedPreferences} that received the
     *                          change.
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

    /**
     * Initiates the worker thread {@code Handler} in an off UI thread {@code Looper}.
     *
     * @return A {@code Handler} for this object.
     */
    final Handler startThread() {
        final HandlerThread handlerThread = new HandlerThread("MPDAsyncWorker");

        handlerThread.start();

        return new Handler(handlerThread.getLooper(), this);
    }
}
