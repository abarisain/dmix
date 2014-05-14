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
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

public class WifiConnectionSettings extends PreferenceActivity {

    private static final int MAIN = 0;
    private static final String KEY_WIFI_BASED_CATEGORY = "wifibasedCategory";
    private static final String KEY_WIFI_BASED_SCREEN = "wifibasedScreen";

    private PreferenceCategory mWifibasedCategory;

    private List<WifiConfiguration> mWifiList;
    private WifiManager mWifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.wificonnectionsettings);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();

        mWifibasedCategory = (PreferenceCategory) preferenceScreen
                .findPreference(KEY_WIFI_BASED_CATEGORY);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add(0, MAIN, 0, R.string.mainMenu).setIcon(android.R.drawable.ic_menu_revert);

        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent i;

        switch (item.getItemId()) {

            case MAIN:
                i = new Intent(this, MainMenuActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
        }
        return false;
    }

    /**
     * Method is beeing called on any click of an preference...
     */

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals(KEY_WIFI_BASED_SCREEN)) {
            mWifiList = mWifiManager.getConfiguredNetworks();
            if (mWifiList == null) {
                return false;
            }

            for (WifiConfiguration wifi : mWifiList) {
                // Friendly SSID-Name
                String ssid = wifi.SSID.replaceAll("\"", "");
                // Add PreferenceScreen for each network
                PreferenceScreen pref = getPreferenceManager().createPreferenceScreen(this);
                pref.setPersistent(false);
                pref.setKey("wifiNetwork" + ssid);
                pref.setTitle(ssid);

                Intent intent = new Intent(this, ConnectionSettings.class);
                intent.putExtra("SSID", ssid);
                pref.setIntent(intent);
                if (WifiConfiguration.Status.CURRENT == wifi.status)
                    pref.setSummary(R.string.connected);
                else
                    pref.setSummary(R.string.notInRange);
                mWifibasedCategory.addPreference(pref);
            }
        }

        return false;
    }
}
