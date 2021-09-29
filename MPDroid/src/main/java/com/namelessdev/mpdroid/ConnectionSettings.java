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

import com.cafbit.multicasttest.NetThread;
import com.example.android.nsdchat.NsdHelper;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

@SuppressWarnings("deprecation")
public class ConnectionSettings extends PreferenceActivity {

    public static final int MAIN = 0;

    private static final String TAG = "ConnectionSettings";

    private static final String KEY_CONNECTION_CATEGORY = "connectionCategory";

    private NsdHelper mNsdHelper = null;
    private EditTextPreference prefHost;
    private EditTextPreference prefPort;

    /**
     * This method is the Preference for getting MPD server info via NSD
     *
     * @param context   The current context.
     * @param keyPrefix The Wi-Fi Set Service ID.
     * @return The host name Preference.
     */
    private ListPreference getMPDServer(final Context context, final String keyPrefix) {
        // try to prevent the Nsd stack from returning IPv6 addresses
        java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
        java.lang.System.setProperty("java.net.preferIPv4Stack", "true");

        final ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        final ArrayList<CharSequence> entriesValues = new ArrayList<CharSequence>();

        //entries.add("localhost:6600");
        //entriesValues.add("127.0.0.1:5500");

        Preference.OnPreferenceChangeListener mChangeListener = new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d(TAG, "set new value to "+newValue);
                String splitstr[] = newValue.toString().split(":");
                prefHost.setText(splitstr[0]);
                prefPort.setText(splitstr[1]);
                return true;
            }
        };

        final ListPreference prefMPDServer = new ListPreference(context);
        prefMPDServer.setTitle("Searching for MPD Servers");
        prefMPDServer.setSummary("");
        prefMPDServer.setDialogTitle("NO MPD Servers Found");
        prefMPDServer.setEntries(entries.toArray(new CharSequence[]{}));
        prefMPDServer.setEntryValues(entriesValues.toArray(new CharSequence[]{}));
        prefMPDServer.setKey(keyPrefix + "nsdhostandport");

        prefMPDServer.setOnPreferenceChangeListener(mChangeListener);

        Handler mUpdateHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                try {
                    InetAddress serviceaddress = InetAddress.getByAddress(msg.getData().getByteArray("address"));
                    Integer serviceport = msg.getData().getInt("port");
                    String servicename = msg.getData().getString("name") + ":" + serviceport.toString();
                    Log.d(TAG, "found mpd server "+servicename+" at "+serviceaddress);
                    for (int i = 0; i < entries.size(); ) {
                        if (servicename.equals(entries.get(i))) {
                            entries.remove(i);
                            entriesValues.remove(i);
                        }
                        else {
                            i++;
                        }
                    }
                    prefMPDServer.setTitle("Found MPD Servers on Local Network");
                    prefMPDServer.setSummary("Touch to see MPD servers");
                    prefMPDServer.setDialogTitle("MPD Servers");
                    entries.add(servicename);
                    entriesValues.add(serviceaddress.getHostAddress() + ":" + serviceport.toString());
                    prefMPDServer.setEntries(entries.toArray(new CharSequence[]{}));
                    prefMPDServer.setEntryValues(entriesValues.toArray(new CharSequence[]{}));
                } catch (UnknownHostException e) {
                    Log.d(TAG, "resolving mpd server failed: "+e);
                }
            }
        };

        mNsdHelper = new NsdHelper(context, mUpdateHandler);
        mNsdHelper.initializeNsd();
        mNsdHelper.discoverServices("_mpd._tcp");

        // This sends a low-level DNS query to the mDNS daemon on the Android device.
        // Unfortunately, the Android NsdManager doesn't seem to wake up properly
        // (at least not up through 8.0.0), so a low-level query to the system's
        // mDNS daemon is in order.
        final NetThread mnetThread = new NetThread(context, "_mpd._tcp.local");
        mnetThread.start();

        return prefMPDServer;
    }

    private void createDynamicSettings(final String keyPrefix,
            final PreferenceCategory toCategory) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final ListPreference prefMPDServer = getMPDServer(this, keyPrefix);
            toCategory.addPreference(prefMPDServer);
        }

        prefHost = new EditTextPreference(this);
        prefHost.getEditText().setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        prefHost.setDialogTitle(R.string.host);
        prefHost.setTitle(R.string.host);
        prefHost.setSummary(R.string.hostDescription);
        prefHost.setDefaultValue("127.0.0.1");
        prefHost.setKey(keyPrefix + "hostname");
        toCategory.addPreference(prefHost);

        prefPort = new EditTextPreference(this);
        prefPort.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        prefPort.setDialogTitle(R.string.port);
        prefPort.setTitle(R.string.port);
        prefPort.setSummary(R.string.portDescription);
        prefPort.setDefaultValue("6600");
        prefPort.setKey(keyPrefix + "port");
        toCategory.addPreference(prefPort);

        final EditTextPreference prefPassword = new EditTextPreference(this);
        prefPassword.getEditText().setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        prefPassword.setDialogTitle(R.string.password);
        prefPassword.setTitle(R.string.password);
        prefPassword.setSummary(R.string.passwordDescription);
        prefPassword.setDefaultValue("");
        prefPassword.setKey(keyPrefix + "password");
        toCategory.addPreference(prefPassword);

        final EditTextPreference prefHostStreaming = new EditTextPreference(this);
        prefHostStreaming.getEditText().setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        prefHostStreaming.setDialogTitle(R.string.hostStreaming);
        prefHostStreaming.setTitle(R.string.hostStreaming);
        prefHostStreaming.setSummary(R.string.hostStreamingDescription);
        prefHostStreaming.setDefaultValue("");
        prefHostStreaming.setKey(keyPrefix + "hostnameStreaming");
        toCategory.addPreference(prefHostStreaming);

        // Meh.
        final EditTextPreference prefStreamingPort = new EditTextPreference(this);
        prefStreamingPort.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        prefStreamingPort.setDialogTitle(R.string.portStreaming);
        prefStreamingPort.setTitle(R.string.portStreaming);
        prefStreamingPort.setSummary(R.string.portStreamingDescription);
        prefStreamingPort.setDefaultValue("8000");
        prefStreamingPort.setKey(keyPrefix + "portStreaming");
        toCategory.addPreference(prefStreamingPort);

        final EditTextPreference suffixStreamingPort = new EditTextPreference(this);
        suffixStreamingPort.getEditText().setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        suffixStreamingPort.setDialogTitle(R.string.suffixStreaming);
        suffixStreamingPort.setTitle(R.string.suffixStreaming);
        suffixStreamingPort.setSummary(R.string.suffixStreamingDescription);
        suffixStreamingPort.setDefaultValue("");
        suffixStreamingPort.setKey(keyPrefix + "suffixStreaming");
        toCategory.addPreference(suffixStreamingPort);

        final CheckBoxPreference persistentNotification = new CheckBoxPreference(this);
        persistentNotification.setDefaultValue(false);
        persistentNotification.setTitle(R.string.persistentNotification);
        persistentNotification.setSummary(R.string.persistentNotificationDescription);
        persistentNotification.setKey(keyPrefix + "persistentNotification");
        toCategory.addPreference(persistentNotification);

        onContentChanged();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.connectionsettings);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();

        final PreferenceCategory masterCategory = (PreferenceCategory) preferenceScreen
                .findPreference(KEY_CONNECTION_CATEGORY);

        if (getIntent().getStringExtra("SSID") != null) {
            // WiFi-Based Settings
            final String SSID = getIntent().getStringExtra("SSID");
            createDynamicSettings(SSID, masterCategory);
        } else {
            // Default settings
            createDynamicSettings("", masterCategory);
            masterCategory.setTitle(R.string.defaultSettings);

        }
    }

    @Override
    protected void onDestroy() {
        if (mNsdHelper != null) {
            mNsdHelper.stopDiscovery();
            mNsdHelper.tearDown();
            mNsdHelper = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final boolean result = super.onCreateOptionsMenu(menu);
        menu.add(0, MAIN, 0, R.string.mainMenu).setIcon(android.R.drawable.ic_menu_revert);

        return result;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final boolean result;

        if (item.getItemId() == MAIN) {
            final Intent intent = new Intent(this, MainMenuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            result = true;
        } else {
            result = super.onOptionsItemSelected(item);
        }

        return result;
    }
}
