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

package com.namelessdev.mpdroid.preferences;

import com.namelessdev.mpdroid.R;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class lists all connections for choosing to modify a connection settings.
 */
public class ConnectionChooser extends PreferenceFragment {

    /**
     * This is used to clean up Wi-Fi Set Service IDs.
     */
    private static final Pattern QUOTATION_DELIMITER = Pattern.compile("\"");

    /**
     * The class log identifier.
     */
    private static final String TAG = "ConnectionChooser";

    /**
     * This is the category for the default connection.
     *
     * @param context The current context.
     * @return The category for the default connection.
     */
    private static Preference getDefaultCategory(final Context context) {
        final Preference category = new PreferenceCategory(context);
        category.setKey("defaultCategory");
        category.setTitle(R.string.defaultConnection);

        return category;
    }

    /**
     * This is the default Preference item which is used when not connected to Wi-Fi.
     *
     * @param context The current context.
     * @return A Preference entry.
     */
    private static Preference getDefaultItem(final Context context) {
        final Preference preference = new Preference(context);

        preference.setKey("defaultScreen");
        preference.setSummary(R.string.defaultSettingsDescription);
        preference.setTitle(R.string.defaultSettings);
        preference.getExtras().putString(ConnectionModifier.EXTRA_SERVICE_SET_ID, "");
        preference.setFragment(ConnectionSettings.FRAGMENT_MODIFIER_NAME);

        return preference;
    }

    /**
     * This is the category for the Wi-Fi connection list.
     *
     * @param context The current context.
     * @return The category for the Wi-Fi connection list.
     */
    private static Preference getWifiCategory(final Context context) {
        final Preference category = new PreferenceCategory(context);
        category.setTitle(R.string.preferredConnection);

        return category;
    }

    /**
     * This method returns a current Collection of Wi-Fi entries.
     *
     * @param context The current context.
     * @return A Collection of Wi-Fi entries.
     */
    private static Collection<WifiConfiguration> getWifiCollection(final Context context) {
        final List<WifiConfiguration> wifiList;
        final WifiManager wifiManager =
                (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (wifiManager == null) {
            Log.e(TAG, "Failed to retrieve the WifiManager service.");
            wifiList = Collections.emptyList();
        } else {
            final Collection<WifiConfiguration> networks = wifiManager.getConfiguredNetworks();

            if (networks == null) {
                Log.e(TAG, "Failed to retrieve a list of configured networks.");
                wifiList = Collections.emptyList();
            } else {
                wifiList = new ArrayList<>(networks);
                Collections.sort(wifiList, new WifiComparator());
            }
        }

        return wifiList;
    }

    /**
     * This method fills the PreferenceScreen parameter with Wi-Fi entries.
     *
     * @param screen   The PreferenceScreen to modify.
     * @param wifiList The Wi-Fi entries to add to the PreferenceScreen parameter.
     */
    private static void getWifiEntries(final PreferenceScreen screen,
            final Iterable<WifiConfiguration> wifiList) {
        for (final WifiConfiguration wifi : wifiList) {
            if (wifi != null && wifi.SSID != null) {
                // Friendly SSID-Name
                final Matcher matcher = QUOTATION_DELIMITER.matcher(wifi.SSID);
                final String ssid = matcher.replaceAll("");

                final Preference ssidItem = new Preference(screen.getContext());

                ssidItem.setPersistent(false);
                ssidItem.setKey("wifiNetwork" + ssid);
                ssidItem.setTitle(ssid);
                ssidItem.getExtras().putString(ConnectionModifier.EXTRA_SERVICE_SET_ID, ssid);
                ssidItem.setFragment(ConnectionSettings.FRAGMENT_MODIFIER_NAME);

                if (WifiConfiguration.Status.CURRENT == wifi.status) {
                    ssidItem.setSummary(R.string.connected);
                } else {
                    ssidItem.setSummary(R.string.notInRange);
                }

                screen.addPreference(ssidItem);
            }
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final PreferenceScreen screen =
                getPreferenceManager().createPreferenceScreen(getActivity());
        final Context context = screen.getContext();
        final Collection<WifiConfiguration> wifiList = getWifiCollection(context);

        screen.addPreference(getDefaultCategory(context));
        screen.addPreference(getDefaultItem(context));

        if (!wifiList.isEmpty()) {
            screen.addPreference(getWifiCategory(context));
            getWifiEntries(screen, wifiList);
        }

        setPreferenceScreen(screen);
    }

    /**
     * This comparator sorts {@link WifiConfiguration} entries.
     *
     * The WifiConfiguration entries are sorted in order of:
     * <ol>
     * <li>If Wi-Fi network is connected.</li>
     * <li>If Wi-Fi network has the ability to connect.</li>
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
         * @return an integer &lt; 0 if {@code lhs} is less than {@code rhs}, 0 if they are
         * equal, and &gt; 0 if {@code lhs} is greater than {@code rhs}.
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
