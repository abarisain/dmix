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

import com.anpmech.mpd.connection.MPDConnectionListener;
import com.anpmech.mpd.connection.MPDConnectionStatus;
import com.anpmech.mpd.exception.MPDException;
import com.namelessdev.mpdroid.tools.SettingsHelper;
import com.namelessdev.mpdroid.tools.Tools;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import java.net.UnknownHostException;
import java.util.List;

public class ErrorHandler implements MPDConnectionListener {

    private static final boolean DEBUG = false;

    private static final String MPD_PACKAGE_NAME = "org.musicpd";

    private static final int SETTINGS = 5;

    private static final String TAG = "ErrorHandler";

    private static AlertDialog sAlertDialog;

    private final Activity mActivity;

    private final MPDApplication mApp;

    private final Resources mResources;

    public ErrorHandler(final Activity activity) {
        super();

        debug("Starting ErrorHandler");
        mActivity = activity;
        mResources = activity.getResources();
        /**
         * Initialize this as non-null; big problems can happen if this is ever null.
         */
        if (sAlertDialog == null) {
            sAlertDialog = new AlertDialog.Builder(activity).create();
        }
        mApp = (MPDApplication) mActivity.getApplication();

        if (SettingsHelper.getConnectionSettings(null) == null) {
            final Intent intent = new Intent(mActivity, WifiConnectionSettings.class);

            // Absolutely no settings defined! Open Settings!
            mActivity.startActivityForResult(intent, SETTINGS);
        }

        mApp.addConnectionLock(mActivity);
        final MPDConnectionStatus connectionStatus = mApp.getMPD().getConnectionStatus();
        connectionStatus.addListener(this);

        if (connectionStatus.isConnected()) {
            connectionConnected(0);
        } else {
            connectionConnecting();
        }
    }

    private static void debug(final String line) {
        if (DEBUG) {
            Log.d(TAG, line);
        }
    }

    /**
     * Builds the Connection Failed dialog box for anything other than the settings activity.
     *
     * @param message The reason the connection failed.
     * @return The built {@code AlertDialog} object.
     */
    private AlertDialog.Builder buildConnectionFailedMessage(final String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        final DialogInterface.OnClickListener oDialogClickListener =
                new DialogClickListener(mActivity);

        builder.setTitle(R.string.connectionFailed);
        builder.setMessage(mResources.getString(R.string.connectionFailedMessage, message));
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
    private AlertDialog.Builder buildConnectionFailedSettings(final CharSequence message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

        builder.setCancelable(false);
        builder.setMessage(mResources.getString(R.string.connectionFailedMessageSetting, message));
        builder.setPositiveButton(R.string.ok, Tools.NOOP_CLICK_LISTENER);
        return builder;
    }

    /**
     * Called upon connection.
     *
     * @param commandErrorCode If this number is non-zero, the number will correspond to a
     *                         {@link MPDException} error code. If this number is zero, the
     *                         connection MPD protocol commands were successful.
     */
    @Override
    public void connectionConnected(final int commandErrorCode) {
        debug("Connected");
        dismissAlertDialog();
    }

    /**
     * Called when connecting.
     */
    @Override
    public final void connectionConnecting() {
        debug("Connecting...");
        launchMPD();
        // dismiss possible dialog
        dismissAlertDialog();

        // show connecting to server dialog, only on the main thread.
        sAlertDialog = new ProgressDialog(mActivity);
        sAlertDialog.setTitle(R.string.connecting);
        sAlertDialog.setMessage(mResources.getString(R.string.connectingToServer));
        sAlertDialog.setCancelable(false);
        sAlertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(final DialogInterface dialog, final int keyCode,
                    final KeyEvent event) {
                // Handle all keys!
                return true;
            }
        });

        try {
            sAlertDialog.show();
        } catch (final WindowManager.BadTokenException ignored) {
            // Can't display it. Don't care.
        }

        mApp.cancelDisconnectScheduler();
    }

    /**
     * Called upon disconnection.
     *
     * @param reason The reason given for disconnection.
     */
    @Override
    public final void connectionDisconnected(final String reason) {
        debug("connectionDisconnected()");
        if (sAlertDialog instanceof ProgressDialog) {

            // dismiss possible dialog
            debug("Dismissing in connectionDisconnected");
            dismissAlertDialog();

            try {
                // are we in the settings activity?
                if (mActivity.getClass().equals(SettingsActivity.class)) {
                    sAlertDialog = buildConnectionFailedSettings(reason).show();
                } else {
                    debug("Showing alert dialog.");
                    sAlertDialog = buildConnectionFailedMessage(reason).show();
                }
            } catch (final RuntimeException e) {
                /**
                 * Unfortunately, AlertDialog can throw RuntimeException if not initialized
                 * and we're given no meaningful way to fix this, so, we'll have to catch it.
                 */
                Log.e(TAG, "Failed to show the connection failed alert dialog.", e);
            }
        }
        launchMPD();
    }

    private void dismissAlertDialog() {
        debug("Dismissing alert dialog");
        try {
            sAlertDialog.dismiss();
        } catch (final IllegalArgumentException | IllegalStateException e) {
            Log.e(TAG, "General error while dismissing alert dialog", e);
        }
    }

    /**
     * This method checks if MPD on localhost is running.
     *
     * @return True if MPD on localhost is running, false otherwise.
     */
    private boolean isMPDRunning() {
        final ActivityManager activityManager =
                (ActivityManager) mActivity.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> apps =
                activityManager.getRunningAppProcesses();

        boolean isRunning = false;
        for (final ActivityManager.RunningAppProcessInfo app : apps) {
            if (!isRunning && app.processName.equals(MPD_PACKAGE_NAME)) {
                isRunning = true;
            }
        }

        if (!isRunning) {
            debug("MPD is installed and not running, attempting launch.");
        }

        return isRunning;
    }

    /**
     * This method launches MPD if a MPD server is setup for the localhost.
     */
    private void launchMPD() {
        if ("127.0.0.1".equals(mApp.getConnectionSettings().server) && !isMPDRunning() &&
                Tools.isPackageInstalled(MPD_PACKAGE_NAME)) {
            /**
             * No delay; no matter the time given, this takes a bit.
             */
            final long relaunchTime = SystemClock.elapsedRealtime();
            final PackageManager packageManager = mActivity.getPackageManager();
            final AlarmManager alarmService =
                    (AlarmManager) mActivity.getSystemService(Context.ALARM_SERVICE);
            final Intent restartActivity =
                    packageManager.getLaunchIntentForPackage(mActivity.getPackageName());
            final PendingIntent relaunchMPDroid = PendingIntent.getActivity(mActivity, 1,
                    restartActivity, PendingIntent.FLAG_ONE_SHOT);

            /**
             * Ready an alarm to restart MPDroid after MPD is launched.
             */
            alarmService.set(AlarmManager.ELAPSED_REALTIME, relaunchTime, relaunchMPDroid);

            Tools.notifyUser(R.string.launchingLocalhostMPD);
            final Intent mpdIntent = packageManager.getLaunchIntentForPackage(MPD_PACKAGE_NAME);
            mActivity.startActivityIfNeeded(mpdIntent, 0);
        }
    }

    public void stop() {
        dismissAlertDialog();
        mApp.getMPD().getConnectionStatus().removeListener(this);
        mApp.removeConnectionLock(mActivity);
    }

    private static class DialogClickListener implements DialogInterface.OnClickListener {

        private final Activity mActivity;

        DialogClickListener(final Activity activity) {
            super();

            mActivity = activity;
        }

        @Override
        public final void onClick(final DialogInterface dialog, final int which) {
            switch (which) {
                case DialogInterface.BUTTON_NEUTRAL:
                    final Intent intent = new Intent(mActivity, WifiConnectionSettings.class);
                    // Show Settings
                    mActivity.startActivityForResult(intent, SETTINGS);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    mActivity.finish();
                    break;
                case DialogInterface.BUTTON_POSITIVE:
                    try {
                        ((MPDApplication) mActivity.getApplication()).connect();
                    } catch (final UnknownHostException e) {
                        Log.e(TAG, "Failed to connect due to unknown host.", e);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
