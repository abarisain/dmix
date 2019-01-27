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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WifiConnectionSettings extends PreferenceActivity {

    private static final String KEY_WIFI_BASED_CATEGORY = "wifibasedCategory";

    private static final String KEY_WIFI_BASED_SCREEN = "wifibasedScreen";

    private static final int MAIN = 0;

    private static final Pattern QUOTATION_DELIMITER = Pattern.compile("\"");

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.wificonnectionsettings);

        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(this);
        /** If the warning has never been shown before, show it. */
        if (!settings.getBoolean("newWarningShown", false)) {
            startActivity(new Intent(this, WarningActivity.class));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final boolean result = super.onCreateOptionsMenu(menu);
        menu.add(0, MAIN, 0, R.string.mainMenu).setIcon(android.R.drawable.ic_menu_revert);

        return result;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);

        if (item.getItemId() == MAIN) {
            final Intent intent = new Intent(this, MainMenuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            result = true;
        }

        return result;
    }

    /** Method is called on any click of a preference... */
    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
            final Preference preference) {
        Intent intent;
        final PreferenceCategory preferenceCategory =
                (PreferenceCategory) preferenceScreen.findPreference(KEY_WIFI_BASED_CATEGORY);
        List<WifiConfiguration> wifiList = null;
        WifiInfo currentWifi = null;

        if (preference.getKey().equals(KEY_WIFI_BASED_SCREEN)) {
            /** Clear the wifi list. */
            preferenceCategory.removeAll();

            final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            wifiList = wifiManager.getConfiguredNetworks();
            currentWifi = wifiManager.getConnectionInfo();
        }

        if (wifiList == null) {
            wifiList = Collections.emptyList();
        }

        for (final WifiConfiguration wifi : wifiList) {
            if (wifi != null && wifi.SSID != null) {
                // Friendly SSID-Name
                final Matcher matcher = QUOTATION_DELIMITER.matcher(wifi.SSID);
                final String ssid = matcher.replaceAll("");

                // Add PreferenceScreen for each network
                final PreferenceScreen ssidItem =
                        getPreferenceManager().createPreferenceScreen(this);

                intent = new Intent(this, ConnectionSettings.class);
                intent.putExtra("SSID", ssid);

                ssidItem.setPersistent(false);
                ssidItem.setKey("wifiNetwork" + ssid);
                ssidItem.setTitle(ssid);
                ssidItem.setIntent(intent);

                if (currentWifi != null && currentWifi.getSSID().equals(wifi.SSID)) {
                    ssidItem.setSummary(R.string.connected);
                } else {
                    ssidItem.setSummary(R.string.notInRange);
                }

                preferenceCategory.addPreference(ssidItem);
            }
        }

        return false;
    }
}
