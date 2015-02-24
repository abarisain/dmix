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

package com.namelessdev.mpdroid;

import com.anpmech.mpd.subsystem.status.IdleSubsystemMonitor;
import com.namelessdev.mpdroid.closedbits.CrashlyticsWrapper;
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
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class MPDApplication extends Application implements
        Handler.Callback,
        MPDAsyncHelper.ConnectionInfoListener {

    private static final boolean DEBUG = false;

    private static final long DISCONNECT_TIMER = 15000L;

    private static final String TAG = "MPDApplication";

    private static MPDApplication sInstance;

    private final Collection<Object> mConnectionLocks =
            Collections.synchronizedCollection(new LinkedList<>());

    public MPDAsyncHelper oMPDAsyncHelper;

    public UpdateTrackInfo updateTrackInfo;

    private Timer mDisconnectScheduler;

    private boolean mIsNotificationActive;

    private boolean mIsNotificationOverridden;

    private boolean mIsStreamActive;

    private ServiceBinder mServiceBinder;

    private SharedPreferences mSettings;

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

    public static MPDApplication getInstance() {
        return sInstance;
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

    void cancelDisconnectScheduler() {
        mDisconnectScheduler.cancel();
        mDisconnectScheduler.purge();
        mDisconnectScheduler = new Timer();
    }

    private void checkConnectionNeeded() {
        if (mConnectionLocks.isEmpty()) {
            disconnect();
        } else {
            if (!oMPDAsyncHelper.isStatusMonitorAlive()) {
                oMPDAsyncHelper.startStatusMonitor(new String[]{
                        IdleSubsystemMonitor.IDLE_DATABASE,
                        IdleSubsystemMonitor.IDLE_MIXER,
                        IdleSubsystemMonitor.IDLE_OPTIONS,
                        IdleSubsystemMonitor.IDLE_OUTPUT,
                        IdleSubsystemMonitor.IDLE_PLAYER,
                        IdleSubsystemMonitor.IDLE_PLAYLIST,
                        IdleSubsystemMonitor.IDLE_STICKER,
                        IdleSubsystemMonitor.IDLE_UPDATE
                });
            }
            if (!oMPDAsyncHelper.oMPD.isConnected()) {
                oMPDAsyncHelper.connect();
            }
        }
    }

    final void disconnect() {
        cancelDisconnectScheduler();
        startDisconnectScheduler();
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
                oMPDAsyncHelper.addConnectionInfoListener(this);
                break;
            case ServiceBinder.DISCONNECTED:
                oMPDAsyncHelper.removeConnectionInfoListener(this);
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

    public final boolean hasGooglePlayDeathWarningBeenDisplayed() {
        return mSettings.getBoolean("googlePlayDeathWarningShown", false);
    }

    public final boolean hasGooglePlayThankYouBeenDisplayed() {
        return mSettings.getBoolean("googlePlayThankYouShown", false);
    }

    public final boolean isInSimpleMode() {
        return mSettings.getBoolean("simpleMode", false);
    }

    public final boolean isLightThemeSelected() {
        return mSettings.getBoolean("lightTheme", false);
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
     * Checks the MPDroid scheduling service and the persistent override to
     */
    public final boolean isNotificationPersistent() {
        final boolean result;

        if (oMPDAsyncHelper.getConnectionSettings().isNotificationPersistent &&
                !mIsNotificationOverridden) {
            result = true;
        } else {
            result = false;
        }

        debug("Notification is persistent: " + result);
        return result;
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
        if (mServiceBinder != null && mServiceBinder.isServiceBound()) {
            final Bundle bundle = new Bundle();
            bundle.setClassLoader(ConnectionInfo.class.getClassLoader());
            bundle.putParcelable(ConnectionInfo.BUNDLE_KEY, connectionInfo);
            mServiceBinder.sendMessageToService(MPDroidService.CONNECTION_INFO_CHANGED, bundle);
        }
    }

    @Override
    public final void onCreate() {
        super.onCreate();
        sInstance = this;
        debug("onCreate Application");

        // Don't worry FOSS guys, crashlytics is not included in the "foss" flavour
        CrashlyticsWrapper.start(this);

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);

        // Init the default preferences (meaning we won't have different defaults between code/xml)
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        oMPDAsyncHelper = new MPDAsyncHelper();

        mDisconnectScheduler = new Timer();
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
                    oMPDAsyncHelper.stopStatusMonitor();
                    oMPDAsyncHelper.disconnect();
                }
            }, DISCONNECT_TIMER);
        } catch (final IllegalStateException e) {
            Log.d(TAG, "Disconnection timer interrupted.", e);
        }
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
