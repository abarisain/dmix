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

package com.namelessdev.mpdroid;

import com.namelessdev.mpdroid.helpers.MPDAsyncHelper;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper.ConnectionListener;
import com.namelessdev.mpdroid.helpers.UpdateTrackInfo;
import com.namelessdev.mpdroid.service.MPDroidService;
import com.namelessdev.mpdroid.service.StreamHandler;
import com.namelessdev.mpdroid.tools.SettingsHelper;

import org.a0z.mpd.MPD;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager.BadTokenException;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MPDApplication extends Application implements ConnectionListener {

    private static final long DISCONNECT_TIMER = 15000L;

    private static final int SETTINGS = 5;

    private static final String TAG = "MPDApplication";

    private static MPDApplication sInstance;

    private final Collection<Object> mConnectionLocks = new LinkedList<>();

    public MPDAsyncHelper oMPDAsyncHelper = null;

    public UpdateTrackInfo updateTrackInfo = null;

    private AlertDialog mAlertDialog = null;

    private Activity mCurrentActivity = null;

    private Timer mDisconnectScheduler = null;

    private SettingsHelper mSettingsHelper = null;

    private boolean mSettingsShown = false;

    private SharedPreferences mSharedPreferences = null;

    private boolean mWarningShown = false;

    public static MPDApplication getInstance() {
        return sInstance;
    }

    /**
     * Checks against a list of running service classes for the needle parameter. This method
     * (ab)uses getRunningServices() due to no other clear cut way whether our own services are
     * active. We could use static boolean, but this method is more fullproof in the case of
     * process instability. Please replace if you know a better way.
     *
     * @param serviceClass The class to search for.
     * @return True if {@code serviceClass} was found, false otherwise.
     */
    private static boolean isServiceRunning(final Class<?> serviceClass) {
        final int maxServices = 1000;
        final ActivityManager activityManager =
                (ActivityManager) sInstance.getSystemService(sInstance.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services =
                activityManager.getRunningServices(maxServices);
        boolean isServiceRunning = false;

        for (final ActivityManager.RunningServiceInfo serviceInfo : services) {
            if (serviceClass.getName().equals(serviceInfo.service.getClassName())) {
                isServiceRunning = true;
                break;
            }
        }

        return isServiceRunning;
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
        }
    }

    private void cancelDisconnectScheduler() {
        mDisconnectScheduler.cancel();
        mDisconnectScheduler.purge();
        mDisconnectScheduler = new Timer();
    }

    private void checkConnectionNeeded() {
        if (mConnectionLocks.isEmpty()) {
            disconnect();
        } else {
            if (!oMPDAsyncHelper.isMonitorAlive()) {
                oMPDAsyncHelper.startMonitor();
            }
            if (!oMPDAsyncHelper.oMPD.isConnected() && (mCurrentActivity == null
                    || !mCurrentActivity.getClass().equals(WifiConnectionSettings.class))) {
                connect();
            }
        }
    }

    public final void connect() {
        if (!mSettingsHelper.updateSettings()) {
            // Absolutely no settings defined! Open Settings!
            if (mCurrentActivity != null && !mSettingsShown) {
                mCurrentActivity.startActivityForResult(new Intent(mCurrentActivity,
                        WifiConnectionSettings.class), SETTINGS);
                mSettingsShown = true;
            }
        }

        if (mCurrentActivity != null && !mSettingsHelper.warningShown() && !mWarningShown) {
            mCurrentActivity.startActivity(new Intent(mCurrentActivity, WarningActivity.class));
            mWarningShown = true;
        }
        connectMPD();
    }

    /**
     * Builds the Connection Failed dialog box for anything other than the settings activity.
     *
     * @param message The reason the connection failed.
     * @return The built {@code AlertDialog} object.
     */
    private AlertDialog.Builder buildConnectionFailedMessage(final String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mCurrentActivity);
        final OnClickListener oDialogClickListener = new DialogClickListener();

        builder.setTitle(R.string.connectionFailed);
        builder.setMessage(
                getResources().getString(R.string.connectionFailedMessage, message));
        builder.setCancelable(false);

        builder.setNegativeButton(R.string.quit, oDialogClickListener);
        builder.setNeutralButton(R.string.settings, oDialogClickListener);
        builder.setPositiveButton(R.string.retry, oDialogClickListener);

        return builder;
    }

    /**
     * Builds the Connection Failed dialog box for the Settings activity.
     *
     * @param message The reason the connection failed.
     * @return The built {@code AlertDialog} object.
     */
    private AlertDialog.Builder buildConnectionFailedSettings(final String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mCurrentActivity);

        builder.setCancelable(false);
        builder.setMessage(
                getResources().getString(R.string.connectionFailedMessageSetting, message));
        builder.setPositiveButton("OK", new OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
            }
        });
        return builder;
    }

    /**
     * Handles the connection failure with a {@code AlertDialog} user facing message box.
     *
     * @param message The reason the connection failed.
     */
    @Override
    public final synchronized void connectionFailed(final String message) {

        if (mAlertDialog == null || mAlertDialog instanceof ProgressDialog ||
                !mAlertDialog.isShowing()) {

            // dismiss possible dialog
            dismissAlertDialog();

            oMPDAsyncHelper.disconnect();

            if (mCurrentActivity != null && !mConnectionLocks.isEmpty()) {
                try {
                    // are we in the settings activity?
                    if (mCurrentActivity.getClass().equals(SettingsActivity.class)) {
                        mAlertDialog = buildConnectionFailedSettings(message).show();
                    } else {
                        mAlertDialog = buildConnectionFailedMessage(message).show();
                    }
                } catch (final BadTokenException ignored) {
                }
            }
        }
    }

    @Override
    public final synchronized void connectionSucceeded(final String message) {
        dismissAlertDialog();
    }

    private void connectMPD() {
        // dismiss possible dialog
        dismissAlertDialog();

        // show connecting to server dialog, only on the main thread.
        if (mCurrentActivity != null && Looper.myLooper().equals(Looper.getMainLooper())) {
            mAlertDialog = new ProgressDialog(mCurrentActivity);
            mAlertDialog.setTitle(R.string.connecting);
            mAlertDialog.setMessage(getResources().getString(R.string.connectingToServer));
            mAlertDialog.setCancelable(false);
            mAlertDialog.setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(final DialogInterface dialog, final int keyCode,
                        final KeyEvent event) {
                    // Handle all keys!
                    return true;
                }
            });
            try {
                mAlertDialog.show();
            } catch (final BadTokenException ignored) {
                // Can't display it. Don't care.
            }
        }

        cancelDisconnectScheduler();

        // really connect
        oMPDAsyncHelper.connect();
    }

    final void disconnect() {
        cancelDisconnectScheduler();
        startDisconnectScheduler();
    }

    private void dismissAlertDialog() {
        if (mAlertDialog != null) {
            if (mAlertDialog.isShowing()) {
                try {
                    mAlertDialog.dismiss();
                } catch (final IllegalArgumentException ignored) {
                    // We don't care, it has already been destroyed
                }
            }
        }
    }

    public final boolean isInSimpleMode() {
        return mSharedPreferences.getBoolean("simpleMode", false);
    }

    public final boolean isLightThemeSelected() {
        return mSharedPreferences.getBoolean("lightTheme", false);
    }

    /**
     * isLocalAudible()
     *
     * @return Returns whether it is probable that the local audio
     * system will be playing audio controlled by this application.
     */
    public final boolean isLocalAudible() {
        return isStreamingServiceRunning() ||
                "127.0.0.1".equals(oMPDAsyncHelper.getConnectionSettings().sServer);
    }

    /**
     * Checks to see if the MPDroid scheduling service is active.
     *
     * @return True if MPDroid scheduling service running, false otherwise.
     */
    public final boolean isMPDroidServiceRunning() {
        return isServiceRunning(MPDroidService.class);
    }

    /**
     * Checks the MPDroid scheduling service and the persistent override to
     */
    public final boolean isNotificationPersistent() {
        final boolean result;

        if (oMPDAsyncHelper.getConnectionSettings().persistentNotification &&
                !mSharedPreferences.getBoolean("notificationOverride", false)) {
            result = true;
        } else {
            result = false;
        }

        return result;
    }

    /**
     * Checks for a running Streaming service.
     *
     * @return True if streaming service is running, false otherwise.
     */
    public final boolean isStreamingServiceRunning() {
        return isServiceRunning(StreamHandler.class);
    }

    public final boolean isTabletUiEnabled() {
        return getResources().getBoolean(R.bool.isTablet)
                && mSharedPreferences.getBoolean("tabletUI", true);
    }

    @Override
    public final void onCreate() {
        super.onCreate();
        sInstance = this;
        Log.d(TAG, "onCreate Application");

        MPD.setApplicationContext(this);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        final StrictMode.VmPolicy vmPolicy = new StrictMode.VmPolicy.Builder().penaltyLog().build();
        final StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll()
                .build();
        StrictMode.setThreadPolicy(policy);
        StrictMode.setVmPolicy(vmPolicy);

        // Init the default preferences (meaning we won't have different defaults between code/xml)
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        oMPDAsyncHelper = new MPDAsyncHelper();
        oMPDAsyncHelper.addConnectionListener(this);

        mSettingsHelper = new SettingsHelper(oMPDAsyncHelper);

        mDisconnectScheduler = new Timer();

        if (!mSharedPreferences.contains("albumTrackSort")) {
            mSharedPreferences.edit().putBoolean("albumTrackSort", true).commit();
        }
    }

    /**
     * This removes the connection lock which allows the service to disconnect after timeout.
     *
     * @param lockOwner The instance declaring itself as an owner of the connection lock.
     */
    public final void removeConnectionLock(final Object lockOwner) {
        mConnectionLocks.remove(lockOwner);
        checkConnectionNeeded();
    }

    public final void setActivity(final Object activity) {
        if (activity instanceof Activity) {
            mCurrentActivity = (Activity) activity;
        }

        addConnectionLock(activity);
    }

    /**
     * Set this to override the persistent notification for the current session.
     *
     * @param override True to override persistent notification, false otherwise.
     */
    public final void setPersistentOverride(final boolean override) {
        mSharedPreferences.edit()
                .putBoolean("notificationOverride", override)
                .apply();
    }

    private void startDisconnectScheduler() {
        mDisconnectScheduler.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.w(TAG, "Disconnecting (" + DISCONNECT_TIMER + " ms timeout)");
                oMPDAsyncHelper.stopMonitor();
                oMPDAsyncHelper.disconnect();
            }
        }, DISCONNECT_TIMER);

    }

    public final void unsetActivity(final Object activity) {
        removeConnectionLock(activity);

        if (mCurrentActivity != null && mCurrentActivity.equals(activity)) {
            mCurrentActivity = null;
        }
    }

    private class DialogClickListener implements OnClickListener {

        @Override
        public final void onClick(final DialogInterface dialog, final int which) {
            switch (which) {
                case DialogInterface.BUTTON_NEUTRAL:
                    final Intent intent =
                            new Intent(mCurrentActivity, WifiConnectionSettings.class);
                    // Show Settings
                    mCurrentActivity.startActivityForResult(intent, SETTINGS);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    mCurrentActivity.finish();
                    break;
                case DialogInterface.BUTTON_POSITIVE:
                    connectMPD();
                    break;
                default:
                    break;
            }
        }
    }
}
