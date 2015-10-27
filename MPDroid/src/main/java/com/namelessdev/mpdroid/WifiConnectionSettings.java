/*
 * Copyright (C) 2010-2015 The MPDroid Project
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

package com.namelessdev.mpdroid;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WifiConnectionSettings extends PreferenceActivity {

    private static final String KEY_WIFI_BASED_CATEGORY = "wifibasedCategory";

    private static final String KEY_WIFI_BASED_SCREEN = "wifibasedScreen";

    private static final int MAIN = 0;

    private static final Pattern QUOTATION_DELIMITER = Pattern.compile("\"");

    private static final String TAG = "WifiConnectionSettings";

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
        final List<WifiConfiguration> wifiList = new ArrayList<>();

        if (preferenceCategory == null) {
            Log.e(TAG, "Failed to find PreferenceCategory: " + KEY_WIFI_BASED_CATEGORY);
        } else {
            if (preference.getKey().equals(KEY_WIFI_BASED_SCREEN)) {
                /** Clear the wifi list. */
                preferenceCategory.removeAll();

                final WifiManager wifiManager =
                        (WifiManager) getSystemService(Context.WIFI_SERVICE);

                if (wifiManager == null) {
                    Log.e(TAG, "Failed to retrieve the WifiManager service.");
                } else {
                    final Collection<WifiConfiguration> networks =
                            wifiManager.getConfiguredNetworks();

                    if (networks == null) {
                        Log.e(TAG, "Failed to retrieve a list of configured networks.");
                    } else {
                        wifiList.addAll(networks);
                    }
                }
            }

            Collections.sort(wifiList, new WifiComparator());
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

                    if (WifiConfiguration.Status.CURRENT == wifi.status) {
                        ssidItem.setSummary(R.string.connected);
                    } else {
                        ssidItem.setSummary(R.string.notInRange);
                    }

                    preferenceCategory.addPreference(ssidItem);
                }
            }
        }

        return false;
    }

    /**
     * This comparator sorts {@link WifiConfiguration} entries.
     *
     * The WifiConfiguration entries are sorted in order of:
     * <ol>
     * <li>If wifi network is connected.</li>
     * <li>If wifi network has the ability to connect.</li>
     * <li>By Set Service Identifier.</li>
     * </ol>
     */
    private static final class WifiComparator implements Comparator<WifiConfiguration> {

        /**
         * Sole constructor.
         */
        private WifiComparator() {
            super();
        }

        /**
         * Compares the two specified objects to determine their relative ordering. The ordering
         * implied by the return value of this method for all possible pairs of
         * {@code (lhs, rhs)} should form an <i>equivalence relation</i>.
         * This means that
         * <ul>
         * <li>{@code compare(a,a)} returns zero for all {@code a}</li>
         * <li>the sign of {@code compare(a,b)} must be the opposite of the sign of {@code
         * compare(b,a)} for all pairs of (a,b)</li>
         * <li>From {@code compare(a,b) > 0} and {@code compare(b,c) > 0} it must
         * follow {@code compare(a,c) > 0} for all possible combinations of {@code
         * (a,b,c)}</li>
         * </ul>
         *
         * @param lhs an {@code Object}.
         * @param rhs a second {@code Object} to compare with {@code lhs}.
         * @return an integer < 0 if {@code lhs} is less than {@code rhs}, 0 if they are
         * equal, and > 0 if {@code lhs} is greater than {@code rhs}.
         * @throws ClassCastException if objects are not of the correct type.
         */
        @Override
        public int compare(final WifiConfiguration lhs, final WifiConfiguration rhs) {
            int result = 0;

            if (lhs.status != rhs.status) {
                if (lhs.status == WifiConfiguration.Status.CURRENT) {
                    result--;
                } else if (rhs.status == WifiConfiguration.Status.CURRENT) {
                    result++;
                }

                if (result == 0) {
                    if (lhs.status == WifiConfiguration.Status.ENABLED) {
                        result--;
                    } else if (rhs.status == WifiConfiguration.Status.ENABLED) {
                        result++;
                    }
                }
            }

            if (result == 0) {
                result = String.CASE_INSENSITIVE_ORDER.compare(lhs.SSID, rhs.SSID);
            }

            return result;
        }
    }
}
