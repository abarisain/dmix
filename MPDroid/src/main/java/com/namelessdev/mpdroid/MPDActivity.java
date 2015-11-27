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
import com.anpmech.mpd.subsystem.status.IdleSubsystemMonitor;
import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.preferences.ConnectionSettings;
import com.namelessdev.mpdroid.preferences.SettingsActivity;
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
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.WindowManager;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * This is a common {@link AppCompatActivity} class used for subclassing with commonly used values
 * and methods.
 */
public abstract class MPDActivity extends AppCompatActivity implements IdleSubsystemMonitor.Error,
        MPDConnectionListener {

    /**
     * The Android musicpd app package name.
     */
    private static final String MPD_PACKAGE_NAME = "org.musicpd";

    private static final int SETTINGS = 5;

    /**
     * The class log identifier.
     */
    private static final String TAG = "MPDActivity";

    /**
     * This is the AlertDialog used for this class. It must be static as it must persist Activity
     * changes.
     */
    private static AlertDialog sAlertDialog;

    /**
     * This is the shared MPDApplication instance.
     */
    protected MPDApplication mApp;

    /**
     * This is the initial theme resource.
     */
    private int mInitialThemeRes;

    /**
     * This method sends the line parameter to the debug log.
     *
     * @param line The string to send to the debug log.
     */
    private static void debug(final String line) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, line);
        }
    }

    /**
     * This method simply dismisses the {@link #sAlertDialog}.
     */
    private static void dismissAlertDialog() {
        debug("Dismissing alert dialog");
        try {
            sAlertDialog.dismiss();
        } catch (final IllegalArgumentException | IllegalStateException e) {
            Log.e(TAG, "General error while dismissing alert dialog", e);
        }
    }

    /**
     * Builds the Connection Failed dialog box for anything other than the settings activity.
     *
     * @param message The reason the connection failed.
     * @return The built {@code AlertDialog} object.
     */
    private AlertDialog.Builder buildConnectionFailedMessage(final String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final DialogInterface.OnClickListener oDialogClickListener = new DialogClickListener(this);
        final Resources resources = getResources();

        builder.setTitle(R.string.connectionFailed);
        builder.setMessage(resources.getString(R.string.connectionFailedMessage, message));
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
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final String buildMessage =
                getResources().getString(R.string.connectionFailedMessageSetting, message);

        builder.setCancelable(false);
        builder.setMessage(buildMessage);
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
    public void connectionConnecting() {
        debug("Connecting...");
        launchMPD();
        // dismiss possible dialog
        dismissAlertDialog();

        // show connecting to server dialog, only on the main thread.
        sAlertDialog = new ProgressDialog(this);
        sAlertDialog.setTitle(R.string.connecting);
        sAlertDialog.setMessage(getResources().getString(R.string.connectingToServer));
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
    public void connectionDisconnected(final String reason) {
        debug("connectionDisconnected()");
        if (sAlertDialog instanceof ProgressDialog) {

            // dismiss possible dialog
            debug("Dismissing in connectionDisconnected");
            dismissAlertDialog();

            try {
                // are we in the settings activity?
                if (getClass().equals(SettingsActivity.class)) {
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

    /**
     * This method returns the current theme resource ID.
     *
     * @return The current theme resource ID.
     */
    @StyleRes
    protected int getThemeResId() {
        final int themeResId;

        if (isLightThemeSelected()) {
            themeResId = R.style.AppTheme_Light;
        } else {
            themeResId = R.style.AppTheme;
        }

        return themeResId;
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
     * This method checks if MPD on localhost is running.
     *
     * @return True if MPD on localhost is running, false otherwise.
     */
    private boolean isMPDRunning() {
        final ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final Collection<ActivityManager.RunningAppProcessInfo> apps = new ArrayList<>();
        apps.addAll(activityManager.getRunningAppProcesses());

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
        final boolean shouldLaunch = "127.0.0.1".equals(mApp.getConnectionSettings().getServer())
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1;

        if (shouldLaunch && !isMPDRunning() && Tools.isPackageInstalled(MPD_PACKAGE_NAME)) {
            /**
             * No delay; no matter the time given, this takes a bit.
             */
            final long relaunchTime = SystemClock.elapsedRealtime() +
                    TimeUnit.SECONDS.toMillis(1L);
            final PackageManager packageManager = getPackageManager();

            if (packageManager != null) {
                final AlarmManager alarmService =
                        (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                final Intent restartActivity =
                        packageManager.getLaunchIntentForPackage(getPackageName());
                final PendingIntent relaunchMPDroid = PendingIntent.getActivity(this, 1,
                        restartActivity, PendingIntent.FLAG_ONE_SHOT);

                /**
                 * Ready an alarm to restart MPDroid after MPD is launched.
                 */
                alarmService.set(AlarmManager.ELAPSED_REALTIME, relaunchTime, relaunchMPDroid);

                Tools.notifyUser(R.string.launchingLocalhostMPD);
                final Intent mpdIntent = packageManager.getLaunchIntentForPackage(MPD_PACKAGE_NAME);
                startActivityIfNeeded(mpdIntent, 0);
            }
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        mApp = (MPDApplication) getApplicationContext();
        mInitialThemeRes = getThemeResId();

        super.onCreate(savedInstanceState);

        /**
         * Initialize this as non-null; big problems can happen if this is ever null.
         */
        if (sAlertDialog == null) {
            sAlertDialog = new AlertDialog.Builder(this).create();
        }
    }

    /**
     * Listeners of this interface method will be called upon IdleSubsystemMonitor IOException
     * error.
     *
     * @param e The {@link IOException} which caused this callback.
     */
    @Override
    public void onIOError(final IOException e) {
    }

    @Override
    public boolean onKeyLongPress(final int keyCode, final KeyEvent event) {
        boolean result = true;

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                Tools.runCommand(MPDControl.ACTION_NEXT);
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                Tools.runCommand(MPDControl.ACTION_PREVIOUS);
                break;
            default:
                result = super.onKeyLongPress(keyCode, event);
                break;
        }

        return result;
    }

    @Override
    public boolean onKeyUp(final int keyCode, @NonNull final KeyEvent event) {
        boolean result = true;

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (event.isTracking() && !event.isCanceled() && !mApp.isLocalAudible()) {
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        Tools.runCommand(MPDControl.ACTION_VOLUME_STEP_UP);
                    } else {
                        Tools.runCommand(MPDControl.ACTION_VOLUME_STEP_DOWN);
                    }
                }
                break;
            default:
                result = super.onKeyUp(keyCode, event);
                break;
        }

        return result;
    }

    /**
     * Listeners of this interface method will be called upon IdleSubsystemMonitor IOException
     * error.
     *
     * @param e The {@link MPDException} which caused this callback.
     */
    @Override
    public void onMPDError(final MPDException e) {
        final Intent intent = new Intent(this, ConnectionSettings.class);

        switch (e.mErrorCode) {
            case MPDException.ACK_ERROR_PASSWORD:
                // TODO: This needs a better UI.
                Tools.notifyUser(R.string.invalidPassword);
                startActivityIfNeeded(intent, SETTINGS);
                break;
            case MPDException.ACK_ERROR_PERMISSION:
                // TODO: This needs a better UI.
                Tools.notifyUser(R.string.corePermissionDenied, e.mErrorMessage);
                startActivityIfNeeded(intent, SETTINGS);
                break;
            default:
                break;
        }
    }

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        dismissAlertDialog();
        mApp.getMPD().getConnectionStatus().removeListener(this);
        mApp.removeConnectionLock(this);
        mApp.removeIdleSubsystemErrorListener(this);

        super.onPause();
    }

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are <em>not</em> resumed.  This means
     * that in some cases the previous state may still be saved, not allowing
     * fragment transactions that modify the state.  To correctly interact
     * with fragments in their proper state, you should instead override
     * {@link #onResumeFragments()}.
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (mInitialThemeRes != getThemeResId()) {
            Tools.notifyUser(R.string.activityReload);
            Tools.resetActivity(this);
        }

        mApp.addConnectionLock(this);

        final MPDConnectionStatus connectionStatus = mApp.getMPD().getConnectionStatus();
        connectionStatus.addListener(this);

        if (connectionStatus.isConnected()) {
            connectionConnected(0);
        } else {
            connectionConnecting();
        }

        mApp.addIdleSubsystemErrorListener(this);
    }

    /**
     * This method overrides {@link ContextThemeWrapper#setTheme(int)} to use
     * {@link #getThemeResId()}.
     *
     * @param resid The resource ID for the current theme.
     */
    @Override
    public void setTheme(final int resid) {
        super.setTheme(getThemeResId());
    }

    /**
     * This class is used as a {@link DialogInterface.OnClickListener} for the
     */
    private static final class DialogClickListener implements DialogInterface.OnClickListener {

        private final Activity mActivity;

        /**
         * Sole constructor.
         *
         * @param activity The current activity.
         */
        DialogClickListener(final Activity activity) {
            super();

            mActivity = activity;
        }

        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            switch (which) {
                case DialogInterface.BUTTON_NEUTRAL:
                    final Intent intent = new Intent(mActivity, ConnectionSettings.class);
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
                    throw new IllegalArgumentException("Undefined button click: " + which);
            }
        }
    }
}
