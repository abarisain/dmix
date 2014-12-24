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

import com.namelessdev.mpdroid.helpers.MPDControl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PhoneStateReceiver extends BroadcastReceiver {

    /** Used to trace when the app pauses / resumes playback */
    public static final String PAUSED_MARKER = "wasPausedInCall";

    /** A key to hold the pause during phone call user configuration setting. */
    public static final String PAUSE_DURING_CALL = "pauseOnPhoneStateChange";

    /** Our app instance. */
    private static final MPDApplication APP = MPDApplication.getInstance();

    private static final SharedPreferences SETTINGS = PreferenceManager
            .getDefaultSharedPreferences(APP);

    private static final boolean DEBUG = false;

    /** A key to hold the play after phone call user configuration setting. */
    private static final String PLAY_AFTER_CALL = "playOnPhoneStateChange";

    /** The class log identifier. */
    private static final String TAG = "PhoneStateReceiver";

    /** A setting to prevent races from causing more than one pause to be sent. */
    public static final String PAUSING_MARKER = TAG + "PausingMarker";

    private static boolean isLocalNetworkConnected() {
        final ConnectivityManager cm =
                (ConnectivityManager) APP.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isLocalNetwork = false;

        if (cm != null) {
            final NetworkInfo networkInfo = cm.getActiveNetworkInfo();

            if (networkInfo != null) {
                final int networkType = networkInfo.getType();

                if (networkInfo.isConnected() && networkType == ConnectivityManager.TYPE_WIFI ||
                        networkType == ConnectivityManager.TYPE_ETHERNET) {
                    isLocalNetwork = true;
                }
            }
        }

        return isLocalNetwork;
    }

    /**
     * This method is called when telephony becomes active.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public final void onReceive(final Context context, final Intent intent) {
        final String telephonyState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        if (isLocalNetworkConnected() || APP.isLocalAudible() && telephonyState != null) {
            final boolean pauseDuringCall = SETTINGS.getBoolean(PAUSE_DURING_CALL, false);
            final boolean playAfterCall = SETTINGS.getBoolean(PLAY_AFTER_CALL, false);

            if (DEBUG) {
                Log.d(TAG, "Telephony State: " + telephonyState);
            }

            if ((telephonyState.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING) ||
                    telephonyState.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_OFFHOOK)) &&
                    !SETTINGS.getBoolean(PAUSING_MARKER, false)) {
                SETTINGS.edit().putBoolean(PAUSING_MARKER, true).commit();

                if (playAfterCall || pauseDuringCall) {
                    if (DEBUG) {
                        Log.d(TAG, "Telephony active, attempting to pause call.");
                    }

                    /**
                     * The remainder is required to be in MPDControl. In cases where this program
                     * is not loaded in memory, and a connection isn't easy to access, we need to
                     * wait for a connection first, query the status then pause, then set the
                     * PAUSED_MARKER as true.
                     */
                    MPDControl.run(MPDControl.ACTION_PAUSE_FOR_CALL);
                }
            } else if (telephonyState.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE)) {
                final boolean playAfterCallMarked =
                        playAfterCall && SETTINGS.getBoolean(PAUSED_MARKER, false);
                final boolean playAfterCallOnly = playAfterCall && !pauseDuringCall;

                if (playAfterCallMarked || playAfterCallOnly) {
                    if (DEBUG) {
                        Log.d(TAG, "Resuming play after call.");
                    }
                    MPDControl.run(MPDControl.ACTION_PLAY);
                }

                if (SETTINGS.getBoolean(PAUSED_MARKER, false)) {
                    SETTINGS.edit().putBoolean(PAUSED_MARKER, false).commit();
                }
            }
        }
    }
}