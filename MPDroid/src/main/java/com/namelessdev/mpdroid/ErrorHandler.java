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
import com.namelessdev.mpdroid.tools.SettingsHelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

public class ErrorHandler implements MPDConnectionListener {

    private static final boolean DEBUG = false;

    private static final int SETTINGS = 5;

    private static final String TAG = "ErrorHandler";

    private final Activity mActivity;

    private final MPDApplication mApp;

    private final Resources mResources;

    private AlertDialog mAlertDialog;

    public ErrorHandler(final Activity activity) {
        super();

        debug("Starting ErrorHandler");
        mActivity = activity;
        mResources = activity.getResources();
        /**
         * Initialize this as non-null; big problems can happen if this is ever null.
         */
        mAlertDialog = new AlertDialog.Builder(activity).create();
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
            connectionConnected();
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
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
            }
        });
        return builder;
    }

    /**
     * Called upon connection.
     */
    @Override
    public final void connectionConnected() {
        debug("Connected");
        dismissAlertDialog();
    }

    /**
     * Called when connecting.
     */
    @Override
    public final void connectionConnecting() {
        debug("Connecting...");
        // dismiss possible dialog
        dismissAlertDialog();

        // show connecting to server dialog, only on the main thread.
        mAlertDialog = new ProgressDialog(mActivity);
        mAlertDialog.setTitle(R.string.connecting);
        mAlertDialog.setMessage(mResources.getString(R.string.connectingToServer));
        mAlertDialog.setCancelable(false);
        mAlertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(final DialogInterface dialog, final int keyCode,
                    final KeyEvent event) {
                // Handle all keys!
                return true;
            }
        });

        try {
            mAlertDialog.show();
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
        if (mAlertDialog instanceof ProgressDialog) {

            // dismiss possible dialog
            debug("Dismissing in connectionDisconnected");
            dismissAlertDialog();

            try {
                // are we in the settings activity?
                if (mActivity.getClass().equals(SettingsActivity.class)) {
                    mAlertDialog = buildConnectionFailedSettings(reason).show();
                } else {
                    debug("Showing alert dialog.");
                    mAlertDialog = buildConnectionFailedMessage(reason).show();
                }
            } catch (final RuntimeException e) {
                /**
                 * Unfortunately, AlertDialog can throw RuntimeException if not initialized
                 * and we're given no meaningful way to fix this, so, we'll have to catch it.
                 */
                Log.e(TAG, "Failed to show the connection failed alert dialog.", e);
            }
        }
    }

    private void dismissAlertDialog() {
        debug("Dismissing alert dialog");
        try {
            mAlertDialog.dismiss();
        } catch (final IllegalArgumentException | IllegalStateException e) {
            Log.e(TAG, "General error while dismissing alert dialog", e);
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
                    ((MPDApplication) mActivity.getApplication()).oMPDAsyncHelper.connect();
                    break;
                default:
                    break;
            }
        }
    }
}
