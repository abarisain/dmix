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

package com.namelessdev.mpdroid.helpers;

import com.namelessdev.mpdroid.MPDApplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class MPDConnectionHandler extends BroadcastReceiver {

    private static MPDConnectionHandler instance;

    public static MPDConnectionHandler getInstance() {
        if (instance == null)
            instance = new MPDConnectionHandler();
        return instance;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // MPDApplication app = (MPDApplication)
        // context.getApplicationContext();
        String action = intent.getAction();
        if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            Log.d(MPDApplication.TAG, "WIFI-STATE:" + intent.getAction());
            Log.d(MPDApplication.TAG, "WIFI-STATE:"
                    + (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                            WifiManager.WIFI_STATE_UNKNOWN)));
        } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            Log.d(MPDApplication.TAG, "NETW-STATE:" + intent.getAction());
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            Log.d(MPDApplication.TAG, "NETW-STATE: Connected: " + networkInfo.isConnected());
            Log.d(MPDApplication.TAG, "NETW-STATE: Connected: " + networkInfo.getState().toString());
        }
    }
}
