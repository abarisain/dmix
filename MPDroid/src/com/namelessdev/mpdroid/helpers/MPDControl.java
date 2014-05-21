/*
 * Copyright (C) 2010-2014 The MPDroid Project
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

package com.namelessdev.mpdroid.helpers;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.SettingsHelper;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.exception.MPDServerException;

import android.util.Log;

import java.net.UnknownHostException;

/**
 * This class contains simple server control methods.
 */
public final class MPDControl {

    /** If these are sent to the run() class, the volume will not change. */
    public static final int INVALID_INT = -5;

    public static final long INVALID_LONG = -5L;

    private static final String TAG = "MPDControl";

    private static final String FULLY_QUALIFIED_NAME = "com.namelessdev.mpdroid.helpers" + '.'
            + TAG;

    /**
     * The following are server action commands.
     */
    public static final String ACTION_CONSUME = FULLY_QUALIFIED_NAME + "CONSUME";

    public static final String ACTION_MUTE = FULLY_QUALIFIED_NAME + "MUTE";

    public static final String ACTION_NEXT = FULLY_QUALIFIED_NAME + "NEXT";

    public static final String ACTION_PAUSE = FULLY_QUALIFIED_NAME + "PAUSE";

    public static final String ACTION_PLAY = FULLY_QUALIFIED_NAME + "PLAY";

    public static final String ACTION_PREVIOUS = FULLY_QUALIFIED_NAME + "PREVIOUS";

    public static final String ACTION_SEEK = FULLY_QUALIFIED_NAME + "SEEK";

    public static final String ACTION_SET_VOLUME = FULLY_QUALIFIED_NAME + "SET_VOLUME";

    public static final String ACTION_SINGLE = FULLY_QUALIFIED_NAME + "SINGLE";

    public static final String ACTION_STOP = FULLY_QUALIFIED_NAME + "STOP";

    public static final String ACTION_TOGGLE_PLAYBACK = FULLY_QUALIFIED_NAME + "PLAY_PAUSE";

    public static final String ACTION_TOGGLE_RANDOM = FULLY_QUALIFIED_NAME + "RANDOM";

    public static final String ACTION_TOGGLE_REPEAT = FULLY_QUALIFIED_NAME + "REPEAT";

    private static final MPDApplication app = MPDApplication.getInstance();

    private static boolean manualConnection = false;

    static {
        /* This is strictly required for manual connection */
        if (app.oMPDAsyncHelper.getConnectionSettings().sServer == null) {
            final SettingsHelper sh = new SettingsHelper(app.oMPDAsyncHelper);
            sh.updateConnectionSettings();
            manualConnection = true;
        }
    }

    /**
     * An overload method to use the standard {@code MPD}
     * object and no {@code userCommand} argument.
     *
     * @param userCommand The command to be run.
     */
    public static void run(final String userCommand) {
        run(MPDApplication.getInstance().oMPDAsyncHelper.oMPD, userCommand, INVALID_LONG);
    }

    /**
     * An overload for the  {@code run(mpd, userCommand, long)} method which uses
     * the standard {@code MPD} object.
     *
     * @param userCommand The command to be run.
     * @param i           An integer which will be cast to long for run for userCommand argument.
     */
    public static void run(final String userCommand, final int i) {
        run(MPDApplication.getInstance().oMPDAsyncHelper.oMPD, userCommand, (long) i);
    }

    /**
     * An overload for the  {@code run(mpd, userCommand, long)} method which
     * translates resource id into a native command.
     *
     * @param resId A resource id.
     */
    public static void run(final int resId) {
        switch (resId) {
            case R.id.next:
                run(ACTION_NEXT);
                break;
            case R.id.prev:
                run(ACTION_PREVIOUS);
                break;
            case R.id.playpause:
                run(ACTION_TOGGLE_PLAYBACK);
                break;
            case R.id.repeat:
                run(ACTION_TOGGLE_REPEAT);
                break;
            case R.id.shuffle:
                run(ACTION_TOGGLE_RANDOM);
                break;
            case R.id.stop:
                run(ACTION_STOP);
                break;
            default:
                break;
        }
    }

    /**
     * Overload method for the {@code run(mpd, userCommand, long)} method which
     * allows an integer as the final parameter.
     *
     * @param mpd         An {@code MPD} object.
     * @param userCommand The command to be run.
     * @param i           An integer which will be cast to long for run.
     */
    public static void run(final MPD mpd, final String userCommand, final int i) {
        run(mpd, userCommand, (long) i);
    }

    /**
     * The parent method which runs the command (above) in a thread.
     *
     * @param mpd         An {@code MPD} object.
     * @param userCommand The command to be run.
     * @param l           A long primitive argument for the {@code userCommand}.
     */
    public static void run(final MPD mpd, final String userCommand, final long l) {
        new Thread(new Runnable() {

            /**
             * A simple status retrieval method.
             *
             * @return An {@code MPDStatus} state string.
             */
            private String getStatus() {
                String state = null;
                try {
                    if (manualConnection) {
                        state = mpd.getStatus(true).getState();
                    } else {
                        state = mpd.getStatus().getState();
                    }
                } catch (final MPDServerException e) {
                    Log.e(TAG, "Failed to receive a current status", e);
                }

                return state;
            }

            private boolean isPaused() {
                return MPDStatus.MPD_STATE_PAUSED.equals(getStatus());
            }

            private boolean isPlaying() {
                return MPDStatus.MPD_STATE_PLAYING.equals(getStatus());
            }

            private void getManualConnection() {
                try {
                    final MPDAsyncHelper.MPDConnectionInfo conInfo = app.oMPDAsyncHelper
                            .getConnectionSettings();
                    mpd.connect(conInfo.sServer, conInfo.iPort, conInfo.sPassword);
                    manualConnection = true;
                } catch (final MPDServerException | UnknownHostException e) {
                    Log.d(TAG, "Failed to get a connection.", e);
                }
            }

            @Override
            public final void run() {
                String command = userCommand;
                if (!mpd.isConnected()) {
                    getManualConnection();
                }

                /** This switch translates for the next switch. */
                switch (command) {
                    case ACTION_TOGGLE_PLAYBACK:
                        if (isPlaying()) {
                            command = ACTION_PAUSE;
                        } else {
                            command = ACTION_PLAY;
                        }
                        break;
                    default:
                        break;
                }

                /**
                 * The main switch for running the command.
                 */
                try {
                    switch (command) {
                        case ACTION_CONSUME:
                            mpd.setConsume(!mpd.getStatus().isConsume());
                            break;
                        case ACTION_MUTE:
                            mpd.setVolume(0);
                            break;
                        case ACTION_NEXT:
                            mpd.next();
                            break;
                        case ACTION_PAUSE:
                            if (!isPaused()) {
                                mpd.pause();
                            }
                            break;
                        case ACTION_PLAY:
                            mpd.play();
                            break;
                        case ACTION_PREVIOUS:
                            mpd.previous();
                            break;
                        case ACTION_SEEK:
                            long li = l;
                            if (li == INVALID_LONG) {
                                li = 0L;
                            }
                            mpd.seek(li);
                            break;
                        case ACTION_SET_VOLUME:
                            if (l != INVALID_LONG) {
                                mpd.setVolume((int) l);
                            }
                            break;
                        case ACTION_STOP:
                            mpd.stop();
                            break;
                        case ACTION_SINGLE:
                            mpd.setSingle(!mpd.getStatus().isSingle());
                            break;
                        case ACTION_TOGGLE_RANDOM:
                            mpd.setRandom(!mpd.getStatus().isRandom());
                            break;
                        case ACTION_TOGGLE_REPEAT:
                            mpd.setRepeat(!mpd.getStatus().isRepeat());
                            break;
                        default:
                            break;
                    }
                } catch (final MPDServerException e) {
                    Log.w(TAG, "Failed to send a simple MPD command.", e);
                } /** Let MPD disconnect itself. */
            }
        }
        ).start();
    }
}
