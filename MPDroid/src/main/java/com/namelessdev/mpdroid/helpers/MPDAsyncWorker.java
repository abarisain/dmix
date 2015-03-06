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

import com.anpmech.mpd.exception.MPDException;
import com.namelessdev.mpdroid.ConnectionInfo;
import com.namelessdev.mpdroid.ConnectionSettings;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.cover.GracenoteCover;
import com.namelessdev.mpdroid.tools.SettingsHelper;

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

    private static final int LOCAL_UID = 500;

    static final int EVENT_CONNECT = LOCAL_UID + 1;

    static final int EVENT_CONNECTION_CONFIG = LOCAL_UID + 2;

    static final int EVENT_EXEC_ASYNC = LOCAL_UID + 3;

    static final int EVENT_EXEC_ASYNC_FINISHED = LOCAL_UID + 4;

    private static final String TAG = "MPDAsyncWorker";

    private static final int UPDATE_CONNECTION_INFO = LOCAL_UID + 5;

    /** A handler for the MPDAsyncHelper object. */
    private final Handler mHelperHandler;

    private ConnectionInfo mConnectionInfo = new ConnectionInfo();

    MPDAsyncWorker(final Handler helperHandler) {
        super();

        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(MPDApplication.getInstance());
        settings.registerOnSharedPreferenceChangeListener(this);

        mHelperHandler = helperHandler;
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
                try {
                    MPDApplication.getInstance().connect();
                } catch (final MPDException | IOException e) {
                    Log.e(TAG, "Failed to connect.", e);
                }
                break;
            case EVENT_EXEC_ASYNC:
                final Runnable run = (Runnable) msg.obj;
                run.run();
                mHelperHandler.obtainMessage(EVENT_EXEC_ASYNC_FINISHED, msg.arg1, 0).sendToTarget();
                break;
            case UPDATE_CONNECTION_INFO:
                setConnectionSettings(SettingsHelper.getConnectionSettings(mConnectionInfo));
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
        String modKey = key;

        final String currentSSID = SettingsHelper.getCurrentSSID();
        if (key.startsWith(currentSSID)) {
            modKey = key.substring(currentSSID.length());
        }

        switch (modKey) {
            case ConnectionSettings.KEY_HOSTNAME:
            case ConnectionSettings.KEY_HOSTNAME_STREAMING:
            case ConnectionSettings.KEY_PASSWORD:
            case ConnectionSettings.KEY_PERSISTENT_NOTIFICATION:
            case ConnectionSettings.KEY_PORT:
            case ConnectionSettings.KEY_PORT_STREAMING:
            case ConnectionSettings.KEY_SUFFIX_STREAMING:
                mHelperHandler.sendEmptyMessage(UPDATE_CONNECTION_INFO);
                break;
            case MPDApplication.USE_LOCAL_ALBUM_CACHE_KEY:
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
    private final void setConnectionSettings(final ConnectionInfo connectionInfo) {
        if (connectionInfo.serverInfoChanged || connectionInfo.streamingServerInfoChanged
                || connectionInfo.wasNotificationPersistent !=
                connectionInfo.isNotificationPersistent) {
            mConnectionInfo = connectionInfo;
            mHelperHandler.obtainMessage(EVENT_CONNECTION_CONFIG, connectionInfo).sendToTarget();
        }
    }

    /**
     * Initiates the worker thread {@code Handler} in an off UI thread {@code Looper}.
     *
     * @return A {@code Handler} for this object.
     */
    final Handler startThread() {
        final HandlerThread handlerThread = new HandlerThread(TAG);

        handlerThread.start();

        return new Handler(handlerThread.getLooper(), this);
    }

    public ConnectionInfo updateConnectionSettings() {
        final ConnectionInfo connectionInfo =
                SettingsHelper.getConnectionSettings(mConnectionInfo);

        if (connectionInfo == null) {
            setConnectionSettings(mConnectionInfo);
        } else {
            setConnectionSettings(connectionInfo);
        }

        return mConnectionInfo;
    }
}
