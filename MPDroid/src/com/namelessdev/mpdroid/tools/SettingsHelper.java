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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import com.namelessdev.mpdroid.cover.GracenoteCover;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper;

import org.a0z.mpd.MPD;

public class SettingsHelper implements OnSharedPreferenceChangeListener {
    private static final int DEFAULT_MPD_PORT = 6600;
    private static final int DEFAULT_STREAMING_PORT = 8000;

    private WifiManager mWifiManager;
    private SharedPreferences settings;
    private MPDAsyncHelper oMPDAsyncHelper;

    public SettingsHelper(Context parent, MPDAsyncHelper MPDAsyncHelper) {
        // Get Settings and register ourself for updates
        settings = PreferenceManager.getDefaultSharedPreferences(parent);// getSharedPreferences("org.pmix",
                                                                         // MODE_PRIVATE);
        settings.registerOnSharedPreferenceChangeListener(this);

        // get reference on WiFi service
        mWifiManager = (WifiManager) parent.getSystemService(Context.WIFI_SERVICE);

        oMPDAsyncHelper = MPDAsyncHelper;
    }

    private boolean getBooleanSetting(String name) {
        return settings.getBoolean(name, false);
    }

    private String getCurrentSSID() {
        WifiInfo info = mWifiManager.getConnectionInfo();
        final String ssid = info.getSSID();
        return ssid == null ? null : ssid.replace("\"", "");
    }

    private int getIntegerSetting(String name, int defaultValue) {
        try {
            return Integer
                    .parseInt(settings.getString(name, Integer.toString(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return DEFAULT_MPD_PORT;
        }
    }

    private String getStringSetting(String name) {
        String value = settings.getString(name, "").trim();

        if (value.equals(""))
            return null;
        else
            return value;
    }

    private String getStringWithSSID(String param, String wifiSSID) {
        if (wifiSSID == null)
            return param;
        else
            return wifiSSID + param;
    }

    public void onSharedPreferenceChanged(SharedPreferences settings, String key) {
        // Reset the Grace note authentication if a custom client ID has been
        // sets
        if (key.equals(GracenoteCover.CUSTOM_CLIENT_ID_KEY)) {
            removeProperty(GracenoteCover.USER_ID);
        }
        updateSettings();
    }

    private void removeProperty(String property) {
        SharedPreferences.Editor editor;
        editor = settings.edit();
        editor.remove(property);
        editor.commit();
    }

    public boolean updateConnectionSettings() {
        String wifiSSID = getCurrentSSID();
        if (getStringSetting(getStringWithSSID("hostname", wifiSSID)) != null) {
            updateConnectionSettings(wifiSSID);
            return true;
        } else if (getStringSetting("hostname") != null) {
            updateConnectionSettings(null);
            return true;
        } else {
            return false;
        }
    }

    private void updateConnectionSettings(String wifiSSID) {
        // an empty SSID should be null
        if (wifiSSID != null)
            if (wifiSSID.trim().equals(""))
                wifiSSID = null;

        oMPDAsyncHelper.getConnectionSettings().sServer = getStringSetting(getStringWithSSID(
                "hostname", wifiSSID));
        oMPDAsyncHelper.getConnectionSettings().iPort = getIntegerSetting(
                getStringWithSSID("port", wifiSSID), DEFAULT_MPD_PORT);
        oMPDAsyncHelper.getConnectionSettings().sPassword = getStringSetting(getStringWithSSID(
                "password", wifiSSID));
        oMPDAsyncHelper.getConnectionSettings().sServerStreaming = getStringSetting(getStringWithSSID(
                "hostnameStreaming", wifiSSID));
        oMPDAsyncHelper.getConnectionSettings().iPortStreaming = getIntegerSetting(
                getStringWithSSID("portStreaming", wifiSSID), DEFAULT_STREAMING_PORT);
        oMPDAsyncHelper.getConnectionSettings().sSuffixStreaming = getStringSetting(getStringWithSSID(
                "suffixStreaming", wifiSSID));
        if (oMPDAsyncHelper.getConnectionSettings().sSuffixStreaming == null)
            oMPDAsyncHelper.getConnectionSettings().sSuffixStreaming = "";
    }

    public boolean updateSettings() {

        MPD.setSortByTrackNumber(settings.getBoolean("albumTrackSort", MPD.sortByTrackNumber()));
        MPD.setSortAlbumsByYear(settings.getBoolean("sortAlbumsByYear", MPD.sortAlbumsByYear()));
        MPD.setShowAlbumTrackCount(settings.getBoolean("showAlbumTrackCount",
                MPD.showAlbumTrackCount()));
        // MPD.setShowArtistAlbumCount(settings.getBoolean("showArtistAlbumCount",
        // MPD.showArtistAlbumCount()));

        oMPDAsyncHelper.setUseCache(settings.getBoolean("useLocalAlbumCache", false));
        return updateConnectionSettings();
    }

    public boolean warningShown() {
        return getBooleanSetting("newWarningShown");
    }
}
