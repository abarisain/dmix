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

package com.namelessdev.mpdroid.tools;

import com.namelessdev.mpdroid.ConnectionInfo;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper;

import org.a0z.mpd.MPDCommand;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

public class SettingsHelper {

    private static final int DEFAULT_STREAMING_PORT = 8000;

    private final MPDAsyncHelper mMPDAsyncHelper;

    private final SharedPreferences mSettings;

    private final WifiManager mWifiManager;

    public SettingsHelper(final MPDAsyncHelper mpdAsyncHelper) {
        super();

        // Get Settings and register ourself for updates
        final MPDApplication app = MPDApplication.getInstance();
        mSettings = PreferenceManager.getDefaultSharedPreferences(app);

        // get reference on WiFi service
        mWifiManager = (WifiManager) app.getSystemService(Context.WIFI_SERVICE);

        mMPDAsyncHelper = mpdAsyncHelper;
    }

    private static String getStringWithSSID(final String param, final String wifiSSID) {
        if (wifiSSID == null) {
            return param;
        } else {
            return wifiSSID + param;
        }
    }

    private boolean getBooleanSetting(final String name) {
        return mSettings.getBoolean(name, false);
    }

    private String getCurrentSSID() {
        final WifiInfo info = mWifiManager.getConnectionInfo();
        final String ssid = info.getSSID();
        return ssid == null ? null : ssid.replace("\"", "");
    }

    private int getIntegerSetting(final String name, final int defaultValue) {
        try {
            return Integer
                    .parseInt(mSettings.getString(name, Integer.toString(defaultValue)).trim());
        } catch (final NumberFormatException ignored) {
            return MPDCommand.DEFAULT_MPD_PORT;
        }
    }

    private String getStringSetting(final String name) {
        final String value = mSettings.getString(name, "").trim();
        final String result;

        if (value.isEmpty()) {
            result = null;
        } else {
            result = value;
        }

        return result;
    }

    public final boolean updateConnectionSettings() {
        final String wifiSSID = getCurrentSSID();
        boolean result = true;

        if (getStringSetting(getStringWithSSID("hostname", wifiSSID)) != null) {
            // an empty SSID should be null
            if (wifiSSID != null && wifiSSID.isEmpty()) {
                updateConnectionSettings(null);
            } else {
                updateConnectionSettings(wifiSSID);
            }
        } else if (getStringSetting("hostname") != null) {
            updateConnectionSettings(null);
        } else {
            result = false;
        }

        return result;
    }

    private void updateConnectionSettings(final String wifiSSID) {
        final String server = getStringSetting(getStringWithSSID("hostname", wifiSSID));
        final int port = getIntegerSetting(getStringWithSSID("port", wifiSSID),
                MPDCommand.DEFAULT_MPD_PORT);
        final String password = getStringSetting(getStringWithSSID("password", wifiSSID));
        final ConnectionInfo.Builder connectionInfo =
                new ConnectionInfo.Builder(server, port, password);

        final String streamServer =
                getStringSetting(getStringWithSSID("hostnameStreaming", wifiSSID));
        final int streamPort = getIntegerSetting(
                getStringWithSSID("portStreaming", wifiSSID), DEFAULT_STREAMING_PORT);
        String streamSuffix =
                getStringSetting(getStringWithSSID("suffixStreaming", wifiSSID));
        if (streamSuffix == null) {
            streamSuffix = "";
        }
        connectionInfo.setStreamingServer(streamServer, streamPort, streamSuffix);

        final boolean persistentNotification =
                getBooleanSetting(getStringWithSSID("persistentNotification", wifiSSID));
        connectionInfo.setPersistentNotification(persistentNotification);

        connectionInfo.setPreviousConnectionInfo(mMPDAsyncHelper.getConnectionSettings());

        mMPDAsyncHelper.setConnectionSettings(connectionInfo.build());
    }
}
