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

import com.namelessdev.mpdroid.tools.SettingsHelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * This is a class which detects an available network connection, checks it against the current
 * preference and if it matches up sends out a callback for other objects to take action.
 */
public class NetworkActivityHandler extends BroadcastReceiver implements Runnable {

    private static final boolean DEBUG = false;

    private static final String TAG = "NetworkActivityHandler";

    /** The handler for the MPDAsyncHelper. */
    private final Handler mHelperHandler;

    /** The MPDAsyncHelper object which this will be processed in. */
    private final MPDAsyncHelper mMPDAsyncHelper;

    /** Necessary to keep the MPD connection up to date after a network change. */
    private final SettingsHelper mSettingsHelper;

    /** The intent received by the BroadcastReceiver. */
    private Intent mIntent;

    /** The SharedPreferences from the current context. */
    private SharedPreferences mPreferences;

    NetworkActivityHandler(final MPDAsyncHelper mpdAsyncHelper) {
        super();

        mMPDAsyncHelper = mpdAsyncHelper;
        mHelperHandler = new Handler(mpdAsyncHelper);
        mSettingsHelper = new SettingsHelper(mpdAsyncHelper);
    }

    /**
     * Get the default shared preferences with Context MODE_MULTI_PROCESS so we always have an
     * updated copy from the main process. Because we update the preferences object per the context
     * in this object, this could be overkill.
     *
     * @param context The current context.
     * @return The {@code SharedPreference} object.
     */
    public static SharedPreferences getPreferences(final Context context) {
        return context.getSharedPreferences(
                context.getPackageName() + "_preferences",
                Context.MODE_MULTI_PROCESS);
    }

    /**
     * A method to display the intent, formatted for debugging.
     *
     * @param intent The incoming ConnectivityManager.CONNECTIVITY_ACTION intent.
     */
    private static void visualizeIntent(final Intent intent) {
        final Bundle extras = intent.getExtras();

        Log.v(TAG, "action: " + intent.getAction());
        Log.v(TAG, "component: " + intent.getComponent());
        if (extras != null) {
            for (final String key : extras.keySet()) {
                Log.v(TAG, "key [" + key + "]: " + extras.get(key));
            }
        } else {
            Log.v(TAG, "no extras");
        }
    }

    /**
     * This checks for localhost MPD connectivity.
     *
     * @return True if a localhost MPD is available, false otherwise.
     */
    private boolean canConnectToLocalhost() {
        final int timeout = 1000; // Should easily take less than a second.
        final int port = Integer.parseInt(mPreferences.getString("port", "6600"));
        final InetSocketAddress endPoint = new InetSocketAddress("127.0.0.1", port);

        boolean isLocalHostAvailable = false;

        if (endPoint.isUnresolved()) {
            if (DEBUG) {
                Log.d(TAG, "Failure " + endPoint);
            }
        } else {
            final Socket socket = new Socket();

            try {
                socket.connect(endPoint, timeout);
                isLocalHostAvailable = true;
                if (DEBUG) {
                    Log.d(TAG, "Success:" + endPoint);
                }
            } catch (final IOException e) {
                if (DEBUG) {
                    Log.d(TAG, "Failed to connect to 127.0.0.1", e);
                }
            } finally {
                try {
                    socket.close();
                } catch (final IOException ignored) {
                }
            }
        }

        return isLocalHostAvailable;
    }

    /**
     * Checks if the MPD object connection configuration is current
     * to the current WIFI/SSID network configuration.
     *
     * @param bundle The incoming ConnectivityManager intent bundle.
     * @return True if the MPD object connection configuration is current to the current
     * WIFI/SSID network configuration, false otherwise.
     */
    private boolean isConnectedToWIFI(final Bundle bundle) {
        final String potentialSSID = bundle.getString(ConnectivityManager.EXTRA_EXTRA_INFO);
        /** Remove quotes */
        final String hostPreferenceName =
                potentialSSID.substring(1, potentialSSID.length() - 1) + "hostname";

        final String hostname = mPreferences.getString(hostPreferenceName, null);

        return isLinkedToHostname(hostname);
    }

    /**
     * Checks if the MPD object connection configuration is current
     * to the network type shared preference configuration.
     *
     * @param bundle The incoming ConnectivityManager intent bundle.
     * @return True if the shared preference configuration is the same as the configured MPD
     * object
     * configuration, false otherwise.
     */
    private boolean isHostnameLinked(final Bundle bundle) {
        final boolean result;

        if (bundle.getInt(ConnectivityManager.EXTRA_NETWORK_TYPE) ==
                ConnectivityManager.TYPE_WIFI) {
            result = isConnectedToWIFI(bundle);
        } else {
            /** "Default" connection */
            result = isLinkedToHostname(mPreferences.getString("hostname", null));
        }

        return result;
    }

    /**
     * Checks that the MPD object connection configuration is current
     * to the network type shared preference configuration hostname.
     *
     * @param hostname The shared preference hostname.
     * @return True if the shared preference hostname configuration is the same
     * as the MPD object connection hostname configuration, false otherwise.
     */
    private boolean isLinkedToHostname(final String hostname) {
        final boolean isConnectedTo;

        if (hostname == null) {
            isConnectedTo = false;
        } else {
            InetAddress hostAddress = null;

            hostAddress = mMPDAsyncHelper.oMPD.getHostAddress();

            if (hostAddress == null) {
                Log.e(TAG, "This should not happen.");
                isConnectedTo = false;
            } else {
                if (DEBUG) {
                    Log.d(TAG, "Shared preference hostname: " + hostname
                            + " Canonical host name: " + hostAddress.getCanonicalHostName()
                            + " host address: " + hostAddress.getHostAddress()
                            + " host name: " + hostAddress.getHostName());
                }

                if (hostname.equals(hostAddress.getHostAddress()) ||
                        hostname.equals(hostAddress.getHostName()) ||
                        hostname.equals(hostAddress.getCanonicalHostName())) {
                    if (DEBUG) {
                        Log.d(TAG, "Connected to this address.");
                    }
                    isConnectedTo = true;
                } else {
                    if (DEBUG) {
                        Log.d(TAG, "Not connected to this address.");
                    }
                    isConnectedTo = false;
                }
            }
        }

        return isConnectedTo;
    }

    /**
     * This is the BroadcastReceiver receiver; this method sets
     * up this object for runnable processing off the UI thread.
     */
    @Override
    public final void onReceive(final Context context, final Intent intent) {
        if (intent != null && ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            mPreferences = getPreferences(context);
            mIntent = intent;
            mMPDAsyncHelper.execAsync(this);
        }
    }

    /**
     * The runnable method for execution off the UI thread, then calls back if appropriate.
     */
    @Override
    public final void run() {
        final Bundle extras = mIntent.getExtras();

        if (DEBUG) {
            visualizeIntent(mIntent);
        }

        /**
         * !mIntent.getBooleanExtra("noConnectivity") == is connected
         * !mIntent.getBooleanExtra("noConnectivity", false) if doesn't exist is connected
         */
        final boolean isNetworkConnected = !mIntent.getBooleanExtra("noConnectivity", false);
        boolean resolved = false;

        if (isNetworkConnected) {
            if (DEBUG) {
                Log.d(TAG, "Connected.");
            }

            if (!isHostnameLinked(extras)) {
                mSettingsHelper.updateConnectionSettings();
                mMPDAsyncHelper.reconnect();
            }

            if (isHostnameLinked(extras)) {
                resolved = true;
                if (mMPDAsyncHelper.oMPD.isConnected()) {
                    if (DEBUG) {
                        Log.d(TAG, "Media player is already linked and connected.");
                    }
                } else {
                    if (DEBUG) {
                        Log.d(TAG, "Linked, but not connected, sending callback.");
                    }
                    mHelperHandler.sendEmptyMessage(MPDAsyncHelper.EVENT_NETWORK_CONNECTED);
                }
            } else if (DEBUG) {
                Log.w(TAG, "Host not linked to the current MPD object.");
            }
        } else if (DEBUG) {
            Log.d(TAG, "Not connected to network.");
        }

        /** Specific to a localhost MPD server. */
        if (!resolved) {
            /**
             * If network is connected, SettingsHelper has already
             * updated ConnectionInfo has already been updated.
             */
            if (!isNetworkConnected) {
                mSettingsHelper.updateConnectionSettings();
                mMPDAsyncHelper.reconnect();
            }

            if ("127.0.0.1".equals(mMPDAsyncHelper.getConnectionSettings().server) &&
                    canConnectToLocalhost()) {
                mHelperHandler.sendEmptyMessage(MPDAsyncHelper.EVENT_NETWORK_CONNECTED);
            }
        }
    }
}
