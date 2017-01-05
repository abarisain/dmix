/*
 * Copyright (C) 2010-2016 The MPDroid Project
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

package com.namelessdev.mpdroid;

import com.anpmech.mpd.MPD;
import com.anpmech.mpd.subsystem.status.IdleSubsystemMonitor;
import com.anpmech.mpd.subsystem.status.StatusChangeListener;
import com.anpmech.mpd.subsystem.status.TrackPositionListener;
import com.namelessdev.mpdroid.favorites.Favorites;
import com.namelessdev.mpdroid.helpers.CachedMPD;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper;
import com.namelessdev.mpdroid.helpers.UpdateTrackInfo;
import com.namelessdev.mpdroid.service.MPDroidService;
import com.namelessdev.mpdroid.service.NotificationHandler;
import com.namelessdev.mpdroid.service.ServiceBinder;
import com.namelessdev.mpdroid.service.StreamHandler;
import com.namelessdev.mpdroid.tools.Tools;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

class MPDApplicationBase extends Application implements
        Handler.Callback,
        MPDAsyncHelper.ConnectionInfoListener {

    public static final String INTENT_ACTION_REFRESH = "com.namelessdev.mpdroid.action.ui.refresh";

    public static final String USE_LOCAL_ALBUM_CACHE_KEY = "useLocalAlbumCache";

    private static final boolean DEBUG = false;

    private static final long DISCONNECT_TIMER = 15000L;

    private static final String TAG = "MPDApplication";

    private final Collection<Object> mConnectionLocks =
            Collections.synchronizedCollection(new LinkedList<>());

    private final Object mDisconnectLock = new Object();

    public UpdateTrackInfo updateTrackInfo;

    private ConnectionInfo mConnectionInfo;

    private Timer mDisconnectScheduler = new Timer();

    private IdleSubsystemMonitor mIdleSubsystemMonitor;

    private boolean mIsNotificationActive;

    private boolean mIsNotificationOverridden;

    private boolean mIsStreamActive;

    private MPD mMPD;

    private Favorites mFavorites;

    private MPDAsyncHelper mMPDAsyncHelper;

    private ServiceBinder mServiceBinder;

    private SharedPreferences mSettings;

    /**
     * Detect and log all found faults.
     */
    static {
        final StrictMode.ThreadPolicy policy =
                new StrictMode.ThreadPolicy.Builder().detectAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    /**
     * A simple method used to suppress or emit debug log output, depending on the {@link #DEBUG}
     * boolean.
     *
     * @param line The log output.
     */
    private static void debug(final String line) {
        if (DEBUG) {
            Log.d(TAG, line);
        }
    }

    /**
     * This adds the connection lock which disallows the service to disconnect after timeout.
     *
     * @param lockOwner The instance declaring itself as an owner of the connection lock.
     */
    public final void addConnectionLock(final Object lockOwner) {
        if (!mConnectionLocks.contains(lockOwner)) {
            mConnectionLocks.add(lockOwner);
            checkConnectionNeeded();
            cancelDisconnectScheduler();
            debug("Added lock owner: " + lockOwner + ", " + mConnectionLocks.size() + " remain.");
        }
    }

    /**
     * Adds a {@link IdleSubsystemMonitor.Error} listener.
     *
     * @param listener A IdleSubsystemMonitor error listener.
     */
    public void addIdleSubsystemErrorListener(final IdleSubsystemMonitor.Error listener) {
        mIdleSubsystemMonitor.addIdleSubsystemErrorListener(listener);
    }

    /**
     * Adds a {@link StatusChangeListener} from the associated {@link IdleSubsystemMonitor}.
     *
     * @param listener The {@link StatusChangeListener} to add for notification for the
     *                 {@link IdleSubsystemMonitor}.
     */
    public void addStatusChangeListener(final StatusChangeListener listener) {
        mIdleSubsystemMonitor.addStatusChangeListener(listener);
    }

    /**
     * Adds a {@link TrackPositionListener} from the associated {@link IdleSubsystemMonitor}.
     *
     * @param listener The {@link TrackPositionListener} to add for notification for the
     *                 {@link IdleSubsystemMonitor}.
     */
    public void addTrackPositionListener(final TrackPositionListener listener) {
        mIdleSubsystemMonitor.addTrackPositionListener(listener);
    }

    void cancelDisconnectScheduler() {
        mDisconnectScheduler.cancel();
        mDisconnectScheduler.purge();
        mDisconnectScheduler = new Timer();
    }

    private void checkConnectionNeeded() {
        if (mConnectionLocks.isEmpty()) {
            disconnect();
        } else {
            if (mIdleSubsystemMonitor.isStopped()) {
                mIdleSubsystemMonitor.setSupportedSubsystems(
                        IdleSubsystemMonitor.IDLE_DATABASE,
                        IdleSubsystemMonitor.IDLE_MIXER,
                        IdleSubsystemMonitor.IDLE_OPTIONS,
                        IdleSubsystemMonitor.IDLE_OUTPUT,
                        IdleSubsystemMonitor.IDLE_PLAYER,
                        IdleSubsystemMonitor.IDLE_PLAYLIST,
                        IdleSubsystemMonitor.IDLE_STICKER,
                        IdleSubsystemMonitor.IDLE_STORED_PLAYLIST,
                        IdleSubsystemMonitor.IDLE_UPDATE);
                mIdleSubsystemMonitor.start();
            }
            if (!mMPD.isConnected()) {
                try {
                    connect();
                } catch (final UnknownHostException e) {
                    Log.e(TAG, "Failed to connect due to unknown host.");
                }
            }
        }
    }

    /**
     * This method manually connects the global MPD instance using default connection information.
     *
     * <p>This method intentionally blocks the thread, do not use in the UI thread. Instead, use
     * {@link #addConnectionLock(Object)}.</p>
     *
     * @throws UnknownHostException Thrown when a hostname can not be resolved.
     */
    public void connect() throws UnknownHostException {
        mConnectionInfo = mMPDAsyncHelper.updateConnectionSettings();
        mMPD.setDefaultPassword(mConnectionInfo.getPassword());
        mMPD.connect(mConnectionInfo.getServer(), mConnectionInfo.getPort());
    }

    public final void disconnect() {
        /**
         * Synchronization is required in this block due to the possibility of a race during
         * already starting timer while cancelling/purging.
         */
        synchronized (mDisconnectLock) {
            cancelDisconnectScheduler();
            startDisconnectScheduler();
        }
    }

    public MPDAsyncHelper getAsyncHelper() {
        return mMPDAsyncHelper;
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
     * Get the Application MPD instance.
     *
     * @return The Application class MPD instance.
     */
    public MPD getMPD() {
        return mMPD;
    }

    public Favorites getFavorites(){
        return mFavorites;
    }

    /**
     * Called upon receiving messages from any handler, in this case most often the Service.
     *
     * @param msg The incoming message.
     * @return Whether the message was acted upon.
     */
    @Override
    public final boolean handleMessage(final Message msg) {
        boolean result = true;

        debug("Message received: " + ServiceBinder.getHandlerValue(msg.what) +
                " with value: " + ServiceBinder.getHandlerValue(msg.arg1));

        switch (msg.what) {
            case MPDroidService.REQUEST_UNBIND:
                debug("Service requested unbind, complying.");
                mServiceBinder.doUnbindService();
                break;
            case NotificationHandler.IS_ACTIVE:
                mIsNotificationActive = ServiceBinder.TRUE == msg.arg1;
                mServiceBinder.setServicePersistent(true);
                break;
            case ServiceBinder.CONNECTED:
                debug("MPDApplication is bound to the service.");
                mMPDAsyncHelper.addConnectionInfoListener(this);
                break;
            case ServiceBinder.DISCONNECTED:
                mMPDAsyncHelper.removeConnectionInfoListener(this);
                break;
            case StreamHandler.IS_ACTIVE:
                mIsStreamActive = ServiceBinder.TRUE == msg.arg1;
                mServiceBinder.setServicePersistent(true);
                break;
            case ServiceBinder.SET_PERSISTENT:
                if (!isNotificationPersistent() || ServiceBinder.TRUE == msg.arg1) {
                    mServiceBinder.setServicePersistent(ServiceBinder.TRUE == msg.arg1);
                }
                break;
            default:
                result = false;
                break;
        }

        return result;
    }

    public final boolean isInSimpleMode() {
        return mSettings.getBoolean("simpleMode", false);
    }

    /**
     * This method returns if the light theme has been selected in the preferences.
     *
     * @return True if the light theme has been selected, false otherwise.
     */
    public boolean isLightThemeSelected() {
        return Tools.isLightThemeSelected(this);
    }

    /**
     * isLocalAudible()
     *
     * @return Returns whether it is probable that the local audio system will be playing audio
     * controlled by this application.
     */
    public final boolean isLocalAudible() {
        return isStreamActive() || Tools.isServerLocalhost();
    }

    /**
     * Checks to see if the MPDroid scheduling service is active.
     *
     * @return True if MPDroid scheduling service running, false otherwise.
     */
    public final boolean isNotificationActive() {
        return !mIsNotificationOverridden && mIsNotificationActive;
    }

    /**
     * Checks the MPDroid scheduling service and the persistent override to query the current
     * status of notification persistence.
     *
     * @return {@code true} if the notification has been set as persistent, {@code false}
     * otherwise.
     */
    public final boolean isNotificationPersistent() {
        final boolean result;

        if (mConnectionInfo.isNotificationPersistent() && !mIsNotificationOverridden) {
            result = true;
        } else {
            result = false;
        }

        debug("Notification is persistent: " + result);
        return result;
    }

    /**
     * Checks to see if the {@link IdleSubsystemMonitor} is active.
     *
     * @return True if the {@link IdleSubsystemMonitor} is active, false otherwise.
     */
    public boolean isStatusMonitorAlive() {
        return !mIdleSubsystemMonitor.isStopped();
    }

    /**
     * Checks for a running Streaming service.
     *
     * @return True if streaming service is running, false otherwise.
     */
    public final boolean isStreamActive() {
        if (mServiceBinder != null) {
            debug("ServiceBound: " + mServiceBinder.isServiceBound() + " isStreamActive: " +
                    mIsStreamActive);
        }
        return mServiceBinder != null && mServiceBinder.isServiceBound() && mIsStreamActive;
    }

    public final boolean isTabletUiEnabled() {
        return getResources().getBoolean(R.bool.isTablet)
                && mSettings.getBoolean("tabletUI", true);
    }

    @SuppressLint("CommitPrefEdits")
    public final void markGooglePlayThankYouAsRead() {
        mSettings.edit().putBoolean("googlePlayThankYouShown", true).commit();
    }

    /**
     * Called upon connection configuration change.
     *
     * @param connectionInfo The new connection configuration information object.
     */
    @Override
    public final void onConnectionConfigChange(final ConnectionInfo connectionInfo) {
        mConnectionInfo = connectionInfo;

        if (mServiceBinder != null && mServiceBinder.isServiceBound()) {
            final Bundle bundle = new Bundle();
            bundle.setClassLoader(ConnectionInfo.class.getClassLoader());
            bundle.putParcelable(ConnectionInfo.EXTRA, connectionInfo);
            mServiceBinder.sendMessageToService(MPDroidService.CONNECTION_INFO_CHANGED, bundle);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        debug("onCreate Application");

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);

        mMPDAsyncHelper = new MPDAsyncHelper();
        mConnectionInfo = mMPDAsyncHelper.updateConnectionSettings();

        if (mSettings.getBoolean(USE_LOCAL_ALBUM_CACHE_KEY, false)) {
            mMPD = new CachedMPD();
        } else {
            mMPD = new MPD();
        }

        mFavorites = new Favorites(mMPD);

        mIdleSubsystemMonitor = new IdleSubsystemMonitor(mMPD);
    }

    /**
     * This removes the connection lock which allows the service to disconnect after timeout.
     *
     * @param lockOwner The instance declaring itself as an owner of the connection lock.
     */
    public final void removeConnectionLock(final Object lockOwner) {
        mConnectionLocks.remove(lockOwner);
        checkConnectionNeeded();
        debug("Removing lock owner: " + lockOwner + ", " + mConnectionLocks.size() + " remain.");
    }

    /**
     * Removes a {@link IdleSubsystemMonitor.Error} listener.
     *
     * @param listener A IdleSubsystemMonitor error listener.
     */
    public void removeIdleSubsystemErrorListener(final IdleSubsystemMonitor.Error listener) {
        mIdleSubsystemMonitor.removeIdleSubsystemErrorListener(listener);
    }

    /**
     * Removes a {@link StatusChangeListener} from the associated
     * {@link IdleSubsystemMonitor}.
     *
     * @param listener The {@link StatusChangeListener} to remove from
     *                 notification for the
     *                 {@link IdleSubsystemMonitor}.
     */
    public void removeStatusChangeListener(final StatusChangeListener listener) {
        mIdleSubsystemMonitor.removeStatusChangeListener(listener);
    }

    /**
     * Removes a {@link TrackPositionListener} from the associated {@link IdleSubsystemMonitor}.
     *
     * @param listener The {@link TrackPositionListener} to remove from notification for the
     *                 {@link IdleSubsystemMonitor}.
     */
    public void removeTrackPositionListener(final TrackPositionListener listener) {
        mIdleSubsystemMonitor.removeTrackPositionListener(listener);
    }

    /**
     * Set this to override the persistent notification for the current session.
     *
     * @param override True to override persistent notification, false otherwise.
     */
    public final void setPersistentOverride(final boolean override) {
        if (mIsNotificationOverridden != override) {
            mIsNotificationOverridden = override;

            setupServiceBinder();
            mServiceBinder.sendMessageToService(NotificationHandler.PERSISTENT_OVERRIDDEN,
                    mIsNotificationOverridden);
        }
    }

    /**
     * Sets up the service binder class. This needs to run the initial running Activity, not in the
     * Application, to prevent the service process from spawning it's own service binder class.
     */
    public final void setupServiceBinder() {
        if (mServiceBinder == null) {
            final Handler handler = new Handler(this);
            mServiceBinder = new ServiceBinder(this, handler);
            mServiceBinder.sendMessageToService(MPDroidService.UPDATE_CLIENT_STATUS);
        }
    }

    private void startDisconnectScheduler() {
        try {
            debug("Scheduling disconnection.");
            mDisconnectScheduler.schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.w(TAG, "Disconnecting (" + DISCONNECT_TIMER + " ms timeout)");
                    mIdleSubsystemMonitor.stop();
                    try {
                        mMPD.disconnect();
                    } catch (final IOException e) {
                        Log.e(TAG, "Failed to disconnect");
                    }
                }
            }, DISCONNECT_TIMER);
        } catch (final IllegalStateException e) {
            Log.d(TAG, "Disconnection timer interrupted.", e);
        }
    }

    /**
     * Starts the associated {@link IdleSubsystemMonitor}.
     *
     * @param idleSubsystems The subsystems to track in the associated
     *                       {@link IdleSubsystemMonitor}.
     */
    public void startIdleMonitor(final String... idleSubsystems) {
        mIdleSubsystemMonitor.setSupportedSubsystems(idleSubsystems);
        mIdleSubsystemMonitor.start();
    }

    public final void startNotification() {
        if (!mIsNotificationActive) {
            debug("Starting notification.");
            setupServiceBinder();
            mServiceBinder
                    .sendMessageToService(NotificationHandler.START, isNotificationPersistent());
        }
    }

    public final void startStreaming() {
        if (!mIsStreamActive) {
            debug("Starting stream.");
            setupServiceBinder();
            mServiceBinder.sendMessageToService(StreamHandler.START);
        }
    }

    /**
     * Stops the associated {@link IdleSubsystemMonitor}.
     */
    public void stopIdleMonitor() {
        mIdleSubsystemMonitor.stop();
    }

    public final void stopNotification() {
        debug("Stop notification.");
        setupServiceBinder();
        mServiceBinder.sendMessageToService(NotificationHandler.STOP);
    }

    public final void stopStreaming() {
        debug("Stop streaming.");
        setupServiceBinder();
        mServiceBinder.sendMessageToService(StreamHandler.STOP);
    }
}
