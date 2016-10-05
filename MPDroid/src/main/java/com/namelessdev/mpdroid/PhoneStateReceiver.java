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

package com.namelessdev.mpdroid;

import com.anpmech.mpd.MPD;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.subsystem.status.MPDStatus;
import com.anpmech.mpd.subsystem.status.MPDStatusMap;
import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.tools.Tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;

/**
 * This class handles any telephony handling required for the connected server.
 *
 * <p>There are a few things that must be considered when maintaining this class. First, all of
 * this occurs on the UI thread. This is why we instantiate a thread prior to waiting for
 * connection and status.</p>
 *
 * <p>BroadcastReceiver will only last about 10 seconds, so no long running processes.</p>
 *
 * <p>Also, <i>expect</i> races. Devices may send <b>multiple</b> broadcasts during each phone
 * state changes. There may be multiple MPDroid devices around the house telling MPD to do the same
 * thing (no toggling!). There will always be possible races here, but attempt to minimize.</p>
 *
 * <p>State persistence is a requirement. To keep things persistent, we use the Android
 * {@link PreferenceManager} infrastructure. This comes with a large downside: If a marker is set
 * but not unset, due to conditionals, make sure it has a chance to be unset in the future; a bad
 * situation would be for it to <b>remain</b> unset.</p>
 */
public class PhoneStateReceiver extends BroadcastReceiver {

    /** A key to hold the pause during phone call user configuration setting. */
    public static final String PAUSE_DURING_CALL = "pauseOnPhoneStateChange";

    /** The debug flag, if set to true, debug output will emit in the logcat. */
    private static final boolean DEBUG = false;

    /** A marker used when the app pauses / resumes playback */
    private static final String PAUSED_MARKER = "PausedMarker";

    /** A marker used to prevent races from causing more than one pause to be sent. */
    private static final String PAUSING_MARKER = "PausingMarker";

    /** A key to hold the play after phone call user configuration setting. */
    private static final String PLAY_AFTER_CALL = "playOnPhoneStateChange";

    /** The class log identifier. */
    private static final String TAG = "PhoneStateReceiver";

    /** The settings to store the persistent markers in. */
    private SharedPreferences mSettings;

    /**
     * This method is used to output to the log.
     *
     * @param line The line to output to the log.
     */
    private static void debug(final String line) {
        if (DEBUG) {
            Log.d(TAG, line);
        }
    }

    /**
     * Checks to see if the local network is connected.
     *
     * @param context The Context in which the receiver is running.
     * @return True if the local network is connected, false otherwise.
     */
    private static boolean isLocalNetworkConnected(final Context context) {
        final ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isLocalNetwork = false;

        if (cm != null) {
            final NetworkInfo networkInfo = cm.getActiveNetworkInfo();

            if (networkInfo != null) {
                final int networkType = networkInfo.getType();

                if (networkInfo.isConnected() && networkType == ConnectivityManager.TYPE_WIFI ||
                        networkType == ConnectivityManager.TYPE_ETHERNET) {
                    isLocalNetwork = true;
                }
            }
        }

        return isLocalNetwork;
    }

    /**
     * This is a simple shortening settings retrieval method.
     *
     * @param key The key to get, if it doesn't exist, defaulting to {@code false}.
     * @return True if the key exists and is true, false otherwise.
     */
    private boolean get(final String key) {
        return mSettings.getBoolean(key, false);
    }

    /**
     * This method handles any incoming call actions.
     *
     * @param context The Context in which the receiver is running.
     */
    private void handleCall(final Context context) {
        debug("Telephony active, attempting to pause call.");

        /**
         * If we have to wait for a connection or status validity, open a thread to
         * prevent UI blocking.
         */
        final MPD mpd = ((MPDApplicationBase) context.getApplicationContext()).getMPD();
        if (mpd.isConnected() && mpd.getStatus().isValid()) {
            debug("Running runnable.");
            new PauseForCall(context).run();
        } else {
            debug("Running threaded.");
            new Thread(new PauseForCall(context, goAsync())).start();
        }
    }

    /**
     * This method is called when telephony becomes active.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public final void onReceive(final Context context, final Intent intent) {
        final String telephonyState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        mSettings = PreferenceManager.getDefaultSharedPreferences(context);

        if (telephonyState == null) {
            debug("Received broadcast for telephony state change with no change information.");
        } else {
            final boolean isLocalAudible =
                    ((MPDApplicationBase) context.getApplicationContext()).isLocalAudible();

            /**
             * If we're connected to a local network or it's audible on the local device
             * (we force the call stop).
             */
            if (isLocalNetworkConnected(context) && get(PAUSE_DURING_CALL) || isLocalAudible) {
                final boolean alreadyActive = get(PAUSED_MARKER) || get(PAUSING_MARKER);

                debug("Telephony State: " + telephonyState + " Already active: " + alreadyActive);

                if ((telephonyState.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING) ||
                        telephonyState.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_OFFHOOK)) &&
                        !alreadyActive) {
                    handleCall(context);
                } else if (telephonyState.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE)) {
                    final boolean playAfterCall = get(PLAY_AFTER_CALL);

                    if (playAfterCall && (get(PAUSED_MARKER) || !get(PAUSE_DURING_CALL))) {
                        debug("Resuming play after call.");
                        Tools.runCommand(MPDControl.ACTION_PLAY);
                    } else {
                        debug("No need to resume play after call.");
                    }
                }
            }

            /**
             * If the paused marker is true, we need to do this, even if we need to remove the
             * marker regardless.
             */
            if (telephonyState.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE)) {
                unsetMarker(PAUSED_MARKER);

                /** This shouldn't be strictly necessary, but just in case. */
                unsetMarker(PAUSING_MARKER);
            }
        }
    }

    /**
     * Unsets a persistent marker.
     *
     * @param marker The marker to unset.
     */
    private void unsetMarker(final String marker) {
        if (get(marker)) {
            mSettings.edit().remove(marker).commit();
        }
    }

    /**
     * This class is called upon a device telephony state change to ringing or off hook.
     */
    private static final class PauseForCall implements Runnable {

        /**
         * The current context.
         */
        private final Context mContext;

        /**
         * The PendingResult, required for the new thread.
         */
        private final BroadcastReceiver.PendingResult mResult;

        /**
         * This field are the default shared preferences.
         */
        private final SharedPreferences mSettings;

        /**
         * This constructor is used when running in a runnable.
         *
         * @param context The Context in which the receiver is running.
         */
        private PauseForCall(final Context context) {
            this(context, null);
        }

        /**
         * This constructor is used when calling from a BroadcastReceiver and running in another
         * thread.
         *
         * @param context The Context in which the receiver is running.
         * @param result  The PendingResult used to finalize the parent BroadcastReceiver.
         */
        private PauseForCall(final Context context, final PendingResult result) {
            super();

            mContext = context;
            mResult = result;
            mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        }

        /**
         * This is a simple shortening settings retrieval method.
         *
         * @param key The key to get, if it doesn't exist, defaulting to {@code false}.
         * @return True if the key exists and is true, false otherwise.
         */
        private boolean get(final String key) {
            return mSettings.getBoolean(key, false);
        }

        @Override
        public void run() {
            final MPDApplication app = (MPDApplication) mContext;

            try {
                setMarker(PAUSING_MARKER);
                app.addConnectionLock(this);

                if (shouldPauseForCall(mContext)) {
                    app.getMPD().getPlayback().pause().get();
                    setMarker(PAUSED_MARKER);
                }
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to send a simple MPD command.", e);
            } catch (final InterruptedException e) {
                Log.e(TAG, "Interrupted by other thread.", e);
            } finally {
                app.removeConnectionLock(this);
                unsetMarker(PAUSING_MARKER);

                if (mResult != null) {
                    mResult.finish();
                }
            }
        }

        /**
         * Sets a persistent marker.
         *
         * @param marker The marker to set.
         */
        private void setMarker(final String marker) {
            if (!get(marker)) {
                mSettings.edit().putBoolean(marker, true).commit();
            }
        }

        /**
         * This method factors in several circumstances to whether
         * or not to pause the media server for a telephony activity.
         *
         * @param context The Context in which the receiver is running.
         * @return True if the media server should be paused, false otherwise.
         * @throws InterruptedException If the current thread is interrupted.
         */
        private boolean shouldPauseForCall(final Context context) throws InterruptedException {
            /**
             * No need to worry about setting a timeout for the wait stuff, the
             * {@link BroadcastReceiver} times out after 10s anyhow.
             */
            final MPDApplication app = (MPDApplication) context.getApplicationContext();
            final MPDStatus status = app.getMPD().getStatus();
            app.getMPD().getConnectionStatus().waitForConnection();
            status.waitForValidity();

            /**
             * We need to double check the telephony state, the connection
             * may have taken longer than the telephony is active.
             */
            final boolean result;
            final TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (status.isState(MPDStatusMap.STATE_PLAYING) &&
                    telephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                if (app.isLocalAudible()) {
                    debug("App is local audible.");

                    result = true;
                } else {
                    result = get(PAUSE_DURING_CALL);

                    debug(PAUSE_DURING_CALL + ": " + result);
                }
            } else {
                result = false;
            }

            return result;
        }

        /**
         * Unsets a persistent marker.
         *
         * @param marker The marker to unset.
         */
        private void unsetMarker(final String marker) {
            if (get(marker)) {
                mSettings.edit().remove(marker).commit();
            }
        }
    }
}