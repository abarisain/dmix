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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class MPDConnectionHandler extends BroadcastReceiver {

    private static final String TAG = "MPDConnectionHandler";

    private static MPDConnectionHandler sInstance;

    public static MPDConnectionHandler getInstance() {
        if (sInstance == null) {
            sInstance = new MPDConnectionHandler();
        }
        return sInstance;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        switch (action) {
            case WifiManager.WIFI_STATE_CHANGED_ACTION:
                final int wifiState = intent
                        .getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

                Log.d(TAG, "WIFI-STATE:" + action + " state: " + wifiState);
                break;
            case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                final NetworkInfo networkInfo =
                        intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                Log.d(TAG, "NETW-STATE:" + action);
                Log.d(TAG, "NETW-STATE: Connected: " + networkInfo.isConnected()
                        + ", state: " + networkInfo.getState());
                break;
            default:
                break;
        }
    }
}
