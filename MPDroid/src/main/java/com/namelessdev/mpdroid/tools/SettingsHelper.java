/*
 * Copyright (C) 2010-2016 The MPDroid Project
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

package com.namelessdev.mpdroid.tools;

import com.anpmech.mpd.MPDCommand;
import com.namelessdev.mpdroid.ConnectionInfo;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.preferences.ConnectionModifier;

import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.regex.Pattern;

public final class SettingsHelper {

    private static final MPDApplication APP = MPDApplication.getInstance();

    private static final Pattern COMPILE = Pattern.compile("\"", Pattern.LITERAL);

    /**
     * This is the code used when there is no SSID, from WifiSsid, which cannot be linked.
     */
    private static final String NONE = "<unknown ssid>";

    private static final SharedPreferences SETTINGS =
            PreferenceManager.getDefaultSharedPreferences(APP);

    private static final String TAG = "SettingsHelper";

    private SettingsHelper() {
        super();
    }

    private static ConnectionInfo getConnectionSettings(final String wifiSSID,
            final ConnectionInfo previousInfo) {
        final String server = getStringSetting(
                getStringWithSSID(ConnectionModifier.KEY_HOSTNAME, wifiSSID));
        final int port = getIntegerSetting(getStringWithSSID(ConnectionModifier.KEY_PORT, wifiSSID),
                MPDCommand.DEFAULT_MPD_PORT);
        final String password = getStringSetting(
                getStringWithSSID(ConnectionModifier.KEY_PASSWORD, wifiSSID));
        final ConnectionInfo.Builder connectionInfo =
                new ConnectionInfo.Builder(server, port, password);
        connectionInfo.setStreamingServer(getStream(server, wifiSSID));

        final boolean persistentNotification =
                SETTINGS.getBoolean(
                        getStringWithSSID(ConnectionModifier.KEY_PERSISTENT_NOTIFICATION, wifiSSID),
                        false);

        if (persistentNotification) {
            connectionInfo.setNotificationPersistent();
        } else {
            connectionInfo.setNotificationNotPersistent();
        }

        connectionInfo.setPreviousConnectionInfo(previousInfo);

        return connectionInfo.build();
    }

    @NotNull
    public static ConnectionInfo getConnectionSettings(final ConnectionInfo previousInfo) {
        final String wifiSSID = getCurrentSSID();
        final ConnectionInfo connectionInfo;

        if (getStringSetting(getStringWithSSID(ConnectionModifier.KEY_HOSTNAME, wifiSSID))
                != null) {
            // an empty SSID should be null
            if (wifiSSID != null && wifiSSID.isEmpty()) {
                connectionInfo = getConnectionSettings(null, previousInfo);
            } else {
                connectionInfo = getConnectionSettings(wifiSSID, previousInfo);
            }
        } else if (getStringSetting(ConnectionModifier.KEY_HOSTNAME) != null) {
            connectionInfo = getConnectionSettings(null, previousInfo);
        } else {
            connectionInfo = ConnectionInfo.EMPTY;
        }

        return connectionInfo;
    }

    public static String getCurrentSSID() {
        final WifiManager wifiManager = (WifiManager) APP.getSystemService(Context.WIFI_SERVICE);
        String result = null;

        if (wifiManager != null) {
            final WifiInfo info = wifiManager.getConnectionInfo();

            if (info != null) {
                final String ssid = info.getSSID();

                if (ssid != null && !ssid.equals(NONE)) {
                    result = COMPILE.matcher(ssid).replaceAll("");
                }
            }
        }

        return result;
    }

    private static int getIntegerSetting(final String name, final int defaultValue) {
        int setting = defaultValue;
        final String settingString =
                SETTINGS.getString(name, Integer.toString(defaultValue).trim());

        if (!settingString.isEmpty()) {
            try {
                setting = Integer.parseInt(settingString);
            } catch (final NumberFormatException e) {
                Log.e(TAG, "Received a bad integer during processing", e);

            }
        }

        return setting;
    }

    private static String getStream(final String server, final String ssid) {
        String streamServer =
                getStringSetting(getStringWithSSID(ConnectionModifier.KEY_STREAM_URL, ssid));

        /**
         * If the stream server is not available, try to choose a sane default.
         */
        if (streamServer == null) {
            streamServer = "http://" + server + ':' + ConnectionModifier.DEFAULT_STREAMING_PORT;
        }

        return streamServer;
    }

    private static String getStringSetting(final String name) {
        final String value = SETTINGS.getString(name, "").trim();
        final String result;

        if (value.isEmpty()) {
            result = null;
        } else {
            result = value;
        }

        return result;
    }

    private static String getStringWithSSID(final String param, final String wifiSSID) {
        final String stringWithSSID;

        if (wifiSSID == null) {
            stringWithSSID = param;
        } else {
            stringWithSSID = wifiSSID + param;
        }

        return stringWithSSID;
    }
}
