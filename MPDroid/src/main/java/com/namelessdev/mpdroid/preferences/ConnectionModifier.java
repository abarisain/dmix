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

package com.namelessdev.mpdroid.preferences;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.StringRes;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;

import com.cafbit.multicasttest.NetThread;
import com.example.android.nsdchat.NsdHelper;
import com.namelessdev.mpdroid.R;

import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class is the Fragment used to configure a specific connection.
 */
public class ConnectionModifier extends PreferenceFragment {

    /**
     * This is the default streaming port for the default MPD implementation.
     */
    public static final CharSequence DEFAULT_STREAMING_PORT = "8000";

    /**
     * This is the Bundle extra used to start this Fragment.
     */
    public static final String EXTRA_SERVICE_SET_ID = "SSID";

    /**
     * This is the settings key used to store the MPD hostname or IP address.
     */
    public static final String KEY_HOSTNAME = "hostname";

    /**
     * This settings key used to store the stream hostname.
     *
     * @deprecated Use {@link #KEY_STREAM_URL}
     */
    @Deprecated
    public static final String KEY_HOSTNAME_STREAMING = "hostnameStreaming";

    /**
     * This is the settings key used to store the MPD host password.
     */
    public static final String KEY_PASSWORD = "password";

    /**
     * This is the settings key used to store whether a persistent notification is required for
     * this connection.
     */
    public static final String KEY_PERSISTENT_NOTIFICATION = "persistentNotification";

    /**
     * This is the settings key used to store the MPD host port.
     */
    public static final String KEY_PORT = "port";

    /**
     * This settings key used to store the stream host port.
     *
     * @deprecated Use {@link #KEY_STREAM_URL}
     */
    @Deprecated
    public static final String KEY_PORT_STREAMING = "portStreaming";

    /**
     * This settings key stores the stream URL.
     */
    public static final String KEY_STREAM_URL = "streamUrl";

    /**
     * This settings key used to store the stream host path.
     *
     * @deprecated Use {@link #KEY_STREAM_URL}
     */
    @Deprecated
    public static final String KEY_SUFFIX_STREAMING = "suffixStreaming";

    private static final String KEY_CONNECTION_CATEGORY = "connectionCategory";

    /**
     * The class log identifier.
     */
    private static final String TAG = "ConnectionSettings";

    /**
     * This method converts the older streaming settings to the new key.
     *
     * @param context   The current context.
     * @param keyPrefix The key prefix used for these stream settings.
     * @return A string conversion to the new Stream URL style.
     */
    @SuppressWarnings("deprecation")
    @Nullable
    private static String convertStreamSettings(final Context context, final String keyPrefix) {
        final String result;
        final SharedPreferences settings =
                PreferenceManager.getDefaultSharedPreferences(context);
        final String host = settings.getString(keyPrefix + KEY_HOSTNAME_STREAMING, null);

        if (host == null || host.isEmpty()) {
            result = null;
        } else {
            final StringBuilder stringBuilder = new StringBuilder();
            final String port = settings.getString(keyPrefix + KEY_PORT_STREAMING, null);
            final String suffix = settings.getString(keyPrefix + KEY_SUFFIX_STREAMING, null);

            /**
             * Assume not rtsp://
             */
            stringBuilder.append("http://");

            stringBuilder.append(host);
            stringBuilder.append(':');
            if (port != null && !port.isEmpty()) {
                stringBuilder.append(port);
            } else {
                stringBuilder.append(DEFAULT_STREAMING_PORT);
            }

            if (suffix != null && !suffix.isEmpty()) {
                stringBuilder.append('/');
                stringBuilder.append(suffix);
            }

            result = stringBuilder.toString();
        }

        /**
         * The results of this method are only retrieved one time. If the user doesn't use them
         * at that time, they'll reinput after.
         */
        if (result != null) {
            final SharedPreferences.Editor editor = settings.edit();

            editor.remove(keyPrefix + KEY_HOSTNAME_STREAMING);
            editor.remove(keyPrefix + KEY_PORT_STREAMING);
            editor.remove(keyPrefix + KEY_SUFFIX_STREAMING);

            editor.apply();
        }

        return result;
    }


    /**
     * This is the settings key used to store the MPD IP address and port.
     */
    private static final String KEY_NSD_HOSTANDPORT = "nsdhostandport";

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
    private Preference getMPDServer(final Context context, final String keyPrefix) {
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
        prefMPDServer.setKey(keyPrefix + KEY_NSD_HOSTANDPORT);

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

    /**
     * This method is the Preference for modifying the MPD hostname.
     *
     * @param context   The current context.
     * @param keyPrefix The Wi-Fi Set Service ID.
     * @return The host name Preference.
     */
    private Preference getHost(final Context context, final String keyPrefix) {
        prefHost = new EditTextPreference(context);
        prefHost.getEditText().setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        prefHost.setDialogTitle(R.string.host);
        prefHost.setTitle(R.string.host);
        prefHost.setSummary(R.string.hostDescription);
        prefHost.setDefaultValue("127.0.0.1");
        prefHost.setKey(keyPrefix + KEY_HOSTNAME);

        return prefHost;
    }

    /**
     * This method creates a Preference for the master category of this class.
     *
     * @param context   The current context.
     * @param keyPrefix The Wi-Fi Set Service ID.
     * @return The EditTextPreference for this preference.
     */
    private static Preference getMasterCategory(final Context context, final String keyPrefix) {
        final PreferenceCategory masterCategory = new PreferenceCategory(context);

        if (keyPrefix.isEmpty()) {
            masterCategory.setTitle(R.string.defaultSettings);
        } else {
            masterCategory.setTitle(R.string.wlanBasedSettings);
        }

        return masterCategory;
    }

    /**
     * This method is the Preference for modifying the MPD host password.
     *
     * @param context   The current context.
     * @param keyPrefix The Wi-Fi Set Service ID.
     * @return The password Preference.
     */
    private static Preference getPassword(final Context context, final String keyPrefix) {
        final EditTextPreference prefPassword = new EditTextPreference(context);
        prefPassword.getEditText().setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        prefPassword.setDialogTitle(R.string.password);
        prefPassword.setTitle(R.string.password);
        prefPassword.setSummary(R.string.passwordDescription);
        prefPassword.setDefaultValue("");
        prefPassword.setKey(keyPrefix + KEY_PASSWORD);

        return prefPassword;
    }

    /**
     * This method is the Preference for modifying notification persistence for this connection.
     *
     * @param context   The current context.
     * @param keyPrefix The Wi-Fi Set Service ID.
     * @return The notification persistence Preference.
     */
    private static Preference getPersistentNotification(final Context context,
            final String keyPrefix) {
        final Preference preference = new CheckBoxPreference(context);
        preference.setDefaultValue(Boolean.FALSE);
        preference.setTitle(R.string.persistentNotification);
        preference.setSummary(R.string.persistentNotificationDescription);
        preference.setKey(keyPrefix + KEY_PERSISTENT_NOTIFICATION);

        return preference;
    }

    /**
     * This method creates a Preference to create a MPD host port preference input.
     *
     * @param context   The current context.
     * @param keyPrefix The Wi-Fi Set Service ID.
     * @return The EditTextPreference for this preference.
     */
    private Preference getPort(final Context context, final String keyPrefix) {
        prefPort = new EditTextPreference(context);
        final EditText editText = prefPort.getEditText();

        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.addTextChangedListener(new ValidatePort(prefPort));

        prefPort.setDialogTitle(R.string.port);
        prefPort.setTitle(R.string.port);
        prefPort.setSummary(R.string.portDescription);
        prefPort.setDefaultValue("6600");
        prefPort.setKey(keyPrefix + KEY_PORT);

        return prefPort;
    }

    /**
     * This method creates a Preference to create a Stream URL preference input.
     *
     * @param context   The current context.
     * @param keyPrefix The Wi-Fi Set Service ID.
     * @return The EditTextPreference for this preference.
     */
    private static Preference getStreamURL(final Context context, final String keyPrefix) {
        final EditTextPreference result = new EditTextPreference(context);
        final EditText editText = result.getEditText();

        editText.addTextChangedListener(new ValidateStreamURL(result));
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        editText.setHint(R.string.streamingUrlHint);

        result.setDialogTitle(R.string.streamingUrlTitle);
        result.setDialogMessage(R.string.streamingUrlDialogMessage);
        result.setTitle(R.string.streamingUrlTitle);
        result.setSummary(R.string.streamingUrlDescription);
        result.setKey(keyPrefix + KEY_STREAM_URL);

        final String currentValue = result.getText();
        if (currentValue == null) {
            result.setDefaultValue(convertStreamSettings(context, keyPrefix));
        }

        return result;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final PreferenceScreen screen =
                getPreferenceManager().createPreferenceScreen(getActivity());
        final Context context = screen.getContext();
        final String serviceSetId = getArguments().getString(EXTRA_SERVICE_SET_ID);

        if (serviceSetId == null) {
            throw new IllegalStateException("Set service ID must not be null.");
        }

        screen.setKey(KEY_CONNECTION_CATEGORY);
        screen.addPreference(getMasterCategory(context, serviceSetId));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            screen.addPreference(getMPDServer(context, serviceSetId));
        screen.addPreference(getHost(context, serviceSetId));
        screen.addPreference(getPort(context, serviceSetId));
        screen.addPreference(getPassword(context, serviceSetId));
        screen.addPreference(getStreamURL(context, serviceSetId));
        screen.addPreference(getPersistentNotification(context, serviceSetId));
        setPreferenceScreen(screen);
    }

    @Override
    public void onDestroy() {
        if (mNsdHelper != null) {
            mNsdHelper.stopDiscovery();
            mNsdHelper.tearDown();
            mNsdHelper = null;
        }
        super.onDestroy();
    }

    /**
     * This class includes common code for validating user input for preferences.
     */
    private abstract static class CommonValidator implements TextWatcher {

        /**
         * The maximum host port for IPv4 / IPv6
         */
        private static final int MAX_PORT = 65535;

        /**
         * The Stream URL EditTextPreference to validate.
         */
        protected final EditTextPreference mPreferenceTextEdit;

        /**
         * Sole constructor.
         *
         * @param editTextPreference The stream URL EditTextPreference to validate.
         */
        protected CommonValidator(final EditTextPreference editTextPreference) {
            super();

            mPreferenceTextEdit = editTextPreference;
        }

        /**
         * This method enables or disables the positive button.
         *
         * @param editText   The edit text of the button to enable.
         * @param setEnabled If true, this will enable the positive button, false disables the
         *                   positive button.
         */
        private static void enablePositiveButton(final EditTextPreference editText,
                final boolean setEnabled) {
            final Dialog dialog = editText.getDialog();

            if (dialog instanceof AlertDialog) {
                final AlertDialog alertDlg = (AlertDialog) dialog;
                final Button button = alertDlg.getButton(DialogInterface.BUTTON_POSITIVE);

                button.setEnabled(setEnabled);
            }
        }

        /**
         * This method validate the user input port.
         *
         * @param hostPort The hostPort from the user input.
         * @return An error message upon error, null otherwise.
         */
        @StringRes
        protected static int validatePort(final String hostPort) {
            int error = Integer.MIN_VALUE;

            try {
                final int port = Integer.parseInt(hostPort);

                if (port > MAX_PORT) {
                    error = R.string.portIntegerAboveRange;
                } else if (port < 0) {
                    error = R.string.portMustBePositive;
                }
            } catch (final NumberFormatException ignored) {
                error = R.string.portIntegerUndefined;
            }

            return error;
        }

        /**
         * This method handles errors.
         *
         * @param error The error message.
         */
        protected void setError(final int error) {
            if (error == Integer.MIN_VALUE) {
                setError(null);
            } else {
                setError(mPreferenceTextEdit.getContext().getResources().getString(error));
            }
        }

        /**
         * This method handles errors.
         *
         * @param error The error message.
         */
        protected void setError(final CharSequence error) {
            enablePositiveButton(mPreferenceTextEdit, error == null);
            mPreferenceTextEdit.getEditText().setError(error);
        }
    }

    /**
     * This class implements a {@link TextWatcher} to validate the MPD host port user input.
     */
    private static final class ValidatePort extends CommonValidator {

        /**
         * Sole constructor.
         *
         * @param editTextPreference The stream URL EditTextPreference to validate.
         */
        private ValidatePort(final EditTextPreference editTextPreference) {
            super(editTextPreference);
        }

        /**
         * This method is called to notify you that, somewhere within {@code s}, the text has been
         * changed.
         *
         * <p>It is legitimate to make further changes to {@code s} from this callback, but be
         * careful not to get yourself into an infinite loop, because any changes you make will
         * cause this method to be called again recursively.</p>
         */
        @Override
        public void afterTextChanged(final Editable s) {
            int error = Integer.MIN_VALUE;

            if (s.length() > 0) {
                error = validatePort(s.toString());
            }

            setError(error);
        }

        /**
         * This method is called to notify you that, within {@code s}, the {@code count} characters
         * beginning at {@code start} are about to be replaced by new text with length {@code
         * after}.
         *
         * <p>It is an error to attempt to make changes to {@code s} from this callback.</p>
         */
        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count,
                final int after) {

        }

        /**
         * This method is called to notify you that, within {@code s}, the {@code count} characters
         * beginning at {@code start} have just replaced old text that had length {@code before}.
         *
         * <p>It is an error to attempt to make changes to {@code s} from this callback.</p>
         */
        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before,
                final int count) {

        }
    }

    /**
     * This class implements a {@link TextWatcher} to validate the media stream URL user input.
     */
    private static final class ValidateStreamURL extends CommonValidator {

        /**
         * These are valid URL schemes for Android {@link MediaPlayer}.
         */
        private static final String[] VALID_SCHEMES = {"http", "rtsp"};

        /**
         * This field flags whether the port has been previously applied.
         *
         * If the user removes it from here, they will be warned, but it won't be reinserted.
         */
        private boolean mPortInserted;

        /**
         * Sole constructor.
         *
         * @param editTextPreference The stream URL EditTextPreference to validate.
         */
        private ValidateStreamURL(final EditTextPreference editTextPreference) {
            super(editTextPreference);
        }

        /**
         * This method validates the port.
         *
         * @param text                The text to extract the port from.
         * @param authorityColonIndex The index of the URI authority.
         * @param portColonIndex      The index of the URI port colon.
         * @return The error output while trying to validate the port.
         */
        private static int getPort(final String text, final int authorityColonIndex,
                final int portColonIndex) {
            final int authorityEndIndex = text.indexOf('/', authorityColonIndex + 3);
            final String intString;

            if (authorityEndIndex == -1) {
                intString = text.substring(portColonIndex + 1);
            } else {
                intString = text.substring(portColonIndex + 1, authorityEndIndex);
            }

            final int error;

            if (intString.isEmpty()) {
                error = Integer.MIN_VALUE;
            } else {
                error = validatePort(intString);
            }

            return error;
        }

        /**
         * This method inserts the default port into the editable during typing.
         *
         * @param s                   The editable to insert the default port into.
         * @param text                The text from the editable.
         * @param authorityColonIndex The authority colon index.
         * @throws ParseException If the URL fails validation after default port insertion.
         */
        private static void insertDefaultPort(final Editable s, final String text,
                final int authorityColonIndex) throws ParseException {
            final int authorityEndIndex = text.indexOf('/', authorityColonIndex + 3);

            if (authorityEndIndex == -1) {
                s.append(':');
                s.append(DEFAULT_STREAMING_PORT);
            } else {
                s.insert(authorityEndIndex, ":" + DEFAULT_STREAMING_PORT);
            }

            /**
             * This should not be invalid, this is a double-check.
             */
            if (!Patterns.WEB_URL.matcher(s).matches()) {
                throw new ParseException("Failed to parse after insertion.", -1);
            }
        }

        /**
         * This checks for validity of the URL scheme.
         *
         * @param text The URL as text.
         * @return True if the scheme is one of the schemes listed in {@link #VALID_SCHEMES}, false
         * otherwise.
         */
        private static boolean isValidScheme(final String text) {
            final int colonIndex = text.indexOf(':');
            final boolean isValidScheme;

            /**
             * Don't bother the user until they've completed the scheme.
             */
            if (colonIndex == -1) {
                isValidScheme = true;
            } else {
                final String scheme = text.substring(0, colonIndex);
                isValidScheme = Arrays.binarySearch(VALID_SCHEMES, scheme) >= 0;
            }

            return isValidScheme;
        }

        /**
         * This method is called to notify you that, somewhere within {@code s}, the text has been
         * changed.
         *
         * <p>It is legitimate to make further changes to {@code s} from this callback, but be
         * careful not to get yourself into an infinite loop, because any changes you make will
         * cause this method to be called again recursively.</p>
         */
        @Override
        public void afterTextChanged(final Editable s) {
            int error = Integer.MIN_VALUE;

            if (s.length() != 0) {
                final String text = s.toString();

                if (isValidScheme(text)) {
                    if (Patterns.WEB_URL.matcher(s).matches()) {
                        final int authorityColonIndex = text.indexOf(':');
                        final int httpAuthIndex = text.indexOf('@');
                        final int portColonIndex;

                        if (httpAuthIndex == -1) {
                            portColonIndex = text.indexOf(':', authorityColonIndex + 1);
                        } else {
                            portColonIndex = text.indexOf(':', httpAuthIndex + 1);
                        }

                        if (portColonIndex == -1 && !mPortInserted) {
                            try {
                                insertDefaultPort(s, text, authorityColonIndex);
                            } catch (final ParseException ignored) {
                                error = R.string.errorParsingURL;
                            }
                            mPortInserted = true;
                        } else {
                            error = getPort(text, authorityColonIndex, portColonIndex);
                        }
                    } else {
                        error = R.string.invalidUrl;
                    }
                } else {
                    error = R.string.invalidStreamScheme;
                }
            }

            setError(error);
        }

        /**
         * This method is called to notify you that, within {@code s}, the {@code count} characters
         * beginning at {@code start} are about to be replaced by new text with length {@code
         * after}.
         *
         * <p>It is an error to attempt to make changes to {@code s} from this callback.</p>
         */
        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count,
                final int after) {
        }

        /**
         * This method is called to notify you that, within {@code s}, the {@code count} characters
         * beginning at {@code start} have just replaced old text that had length {@code before}.
         *
         * <p>It is an error to attempt to make changes to {@code s} from this callback.</p>
         */
        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before,
                final int count) {
        }
    }
}
