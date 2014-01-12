/*
 * Copyright 2014 Arnaud Barisain Monrose (The MPDroid Project)
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.namelessdev.mpdroid.helpers.MPDAsyncHelper;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper.MPDConnectionInfo;
import com.namelessdev.mpdroid.tools.NetworkHelper;
import com.namelessdev.mpdroid.tools.SettingsHelper;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.exception.MPDServerException;

import java.net.UnknownHostException;

public class PhoneStateReceiver extends BroadcastReceiver {
    // Used to trace when the app pauses / resumes playback
    private static final String PAUSED_MARKER = "wasPausedInCall";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (!NetworkHelper.isLocalNetworkConnected(context)) {
            Log.d(MPDApplication.TAG, "No local network available.");
            return;
        }

        Log.d(MPDApplication.TAG, "Phonestate received");
        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(context);
        // Get config vars
        boolean pauseOnCall = settings.getBoolean("pauseOnPhoneStateChange",
                false);
        boolean playOnCallStop = pauseOnCall
                && settings.getBoolean("playOnPhoneStateChange", false);

        Log.d(MPDApplication.TAG, "Pause on call " + pauseOnCall);
        if (pauseOnCall == false) {
            return;
        }
        Bundle bundle = intent.getExtras();
        if (null == bundle) {
            Log.e(MPDApplication.TAG, "Bundle was null");
            return;
        }
        String state = bundle.getString(TelephonyManager.EXTRA_STATE);

        final boolean shouldPause = pauseOnCall
                && (state
                        .equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING) || state
                        .equalsIgnoreCase(TelephonyManager.EXTRA_STATE_OFFHOOK));
        Log.d(MPDApplication.TAG, "Should pause " + shouldPause);

        final boolean shouldPlay = (playOnCallStop
                && settings.getBoolean(PAUSED_MARKER, false) && state
                .equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE));
        Log.d(MPDApplication.TAG, "Should play " + shouldPlay);

        if (shouldPause == false && shouldPlay == false) {
            return;
        }
        // get configured MPD connection
        final MPDAsyncHelper oMPDAsyncHelper = new MPDAsyncHelper();
        SettingsHelper settingsHelper = new SettingsHelper(
                (ContextWrapper) context.getApplicationContext(),
                oMPDAsyncHelper);
        settingsHelper.updateConnectionSettings();

        // schedule real work
        oMPDAsyncHelper.execAsync(new Runnable() {
            @Override
            public void run() {
                Log.d(MPDApplication.TAG, "Runnable started");

                try {
                    MPD mpd = oMPDAsyncHelper.oMPD;
                    if (!mpd.isConnected()) {
                        Log.d(MPDApplication.TAG, "Trying to connect");
                        // MPD connection has to be done synchronously
                        MPDConnectionInfo conInfo = (MPDConnectionInfo) oMPDAsyncHelper
                                .getConnectionSettings();
                        mpd.connect(conInfo.sServer, conInfo.iPort, conInfo.sPassword);

                        if (mpd.isConnected()) {
                            Log.d(MPDApplication.TAG, "Connected");
                        } else {
                            Log.e(MPDApplication.TAG, "Not connected");
                        }
                    }
                    if (shouldPause) {
                        Log.d(MPDApplication.TAG, "Trying to pause");
                        if (mpd.getStatus().getState()
                                .equals(MPDStatus.MPD_STATE_PLAYING)) {
                            mpd.pause();
                            settings.edit()
                                    .putBoolean(PAUSED_MARKER, true)
                                    .commit();
                            Log.d(MPDApplication.TAG, "Playback paused");
                        }
                    } else if (shouldPlay) {
                        Log.d(MPDApplication.TAG, "Trying to play");
                        mpd.play();
                        settings.edit()
                                .putBoolean(PAUSED_MARKER, false)
                                .commit();
                        Log.d(MPDApplication.TAG, "Playback resumed");
                    }
                    mpd.disconnect();
                } catch (MPDServerException e) {
                    e.printStackTrace();
                    Log.d(MPDApplication.TAG, "MPD Error", e);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    Log.d(MPDApplication.TAG, "MPD Error", e);
                }
            }
        });
    }
}
