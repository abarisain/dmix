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

import org.a0z.mpd.MPDStatus;

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

    private static final MPDApplication APP = MPDApplication.getInstance();

    private static final SharedPreferences SETTINGS = PreferenceManager
            .getDefaultSharedPreferences(APP);

    private static final boolean DEBUG = false;

    // Used to trace when the app pauses / resumes playback
    private static final String PAUSED_MARKER = "wasPausedInCall";

    private static final String TAG = "PhoneStateReceiver";

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

    private static void setPausedMarker(final boolean value) {
        SETTINGS.edit()
                .putBoolean(PAUSED_MARKER, value)
                .commit();
    }

    private static boolean shouldPauseForCall() {
        boolean result = false;
        final boolean isPlaying =
                APP.oMPDAsyncHelper.oMPD.getStatus().isState(MPDStatus.STATE_PLAYING);

        if (isPlaying) {
            if (APP.isLocalAudible()) {
                if (DEBUG) {
                    Log.d(TAG, "App is local audible.");
                }
                result = true;
            } else {
                result = SETTINGS.getBoolean("pauseOnPhoneStateChange", false);
                if (DEBUG) {
                    Log.d(TAG, "pauseOnPhoneStateChange: " + result);
                }
            }
        }

        return result;
    }

    @Override
    public final void onReceive(final Context context, final Intent intent) {
        final String telephonyState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        if (isLocalNetworkConnected() || APP.isLocalAudible() && telephonyState != null) {
            if (DEBUG) {
                Log.d(TAG, "Telephony State: " + telephonyState);
            }
            if ((telephonyState.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING) ||
                    telephonyState.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_OFFHOOK)) &&
                    shouldPauseForCall()) {
                if (DEBUG) {
                    Log.d(TAG, "Pausing for incoming call.");
                }
                MPDControl.run(MPDControl.ACTION_PAUSE);
                setPausedMarker(true);
            } else if (telephonyState.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE)) {
                final boolean playOnCallStop = SETTINGS.getBoolean("playOnPhoneStateChange", false);
                if (playOnCallStop && SETTINGS.getBoolean(PAUSED_MARKER, false)) {
                    if (DEBUG) {
                        Log.d(TAG, "Resuming play after call.");
                    }
                    MPDControl.run(MPDControl.ACTION_PLAY);
                }
                setPausedMarker(false);
            }
        }
    }
}