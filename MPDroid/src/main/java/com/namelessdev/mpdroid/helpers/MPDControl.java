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

package com.namelessdev.mpdroid.helpers;

import com.anpmech.mpd.MPD;
import com.anpmech.mpd.concurrent.MPDFuture;
import com.anpmech.mpd.subsystem.Playback;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;

import android.support.annotation.IdRes;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * This class contains simple server control methods.
 */
public final class MPDControl {

    /** If these are sent to the run() class, the volume will not change. */
    public static final int INVALID_INT = Integer.MIN_VALUE;

    public static final long INVALID_LONG = Long.MIN_VALUE;

    private static final MPDApplication APP = MPDApplication.getInstance();

    private static final String TAG = "MPDControl";

    private static final String FULLY_QUALIFIED_NAME = "com.namelessdev.mpdroid.helpers" + '.'
            + TAG + '.';

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

    public static final String ACTION_VOLUME_SET = FULLY_QUALIFIED_NAME + "SET_VOLUME";

    public static final String ACTION_SINGLE = FULLY_QUALIFIED_NAME + "SINGLE";

    public static final String ACTION_STOP = FULLY_QUALIFIED_NAME + "STOP";

    public static final String ACTION_TOGGLE_PLAYBACK = FULLY_QUALIFIED_NAME + "PLAY_PAUSE";

    public static final String ACTION_TOGGLE_RANDOM = FULLY_QUALIFIED_NAME + "RANDOM";

    public static final String ACTION_TOGGLE_REPEAT = FULLY_QUALIFIED_NAME + "REPEAT";

    public static final String ACTION_VOLUME_STEP_DOWN = FULLY_QUALIFIED_NAME + "VOLUME_STEP_DOWN";

    public static final String ACTION_VOLUME_STEP_UP = FULLY_QUALIFIED_NAME + "VOLUME_STEP_UP";

    private static final int VOLUME_STEP = 5;

    private MPDControl() {
        super();
    }

    /**
     * An overload for the {@code run(userCommand, long)} method which translates resource id
     * into a native command.
     *
     * @param resId A resource id.
     */
    public static void run(@IdRes final int resId) {
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
     * Overload method for the {@code run(userCommand, long)} method which removes the third
     * parameter.
     *
     * @param userCommand The command to be run.
     */
    public static void run(final String userCommand) {
        run(userCommand, INVALID_LONG);
    }

    /**
     * Overload method for the {@code run(userCommand, long)} method which allows an integer
     * as the final parameter.
     *
     * @param userCommand The command to be run.
     * @param i           An integer which will be cast to long for run.
     */
    public static void run(final String userCommand, final int i) {
        run(userCommand, (long) i);
    }

    /**
     * The parent method which runs the command (above) in a thread.
     *
     * @param userCommand The command to be run.
     * @param l           A long primitive argument for the {@code userCommand}.
     */
    public static MPDFuture run(final String userCommand, final long l) {
        final Playback playback = APP.getMPD().getPlayback();
        MPDFuture future = null;

        switch (userCommand) {
            case ACTION_CONSUME:
                future = playback.consume();
                break;
            case ACTION_MUTE:
                future = playback.setVolume(0);
                break;
            case ACTION_NEXT:
                future = playback.next();
                break;
            case ACTION_PAUSE:
                future = playback.pause();
                break;
            case ACTION_PLAY:
                future = playback.play();
                break;
            case ACTION_PREVIOUS:
                future = playback.previous();
                break;
            case ACTION_SEEK:
                long li = l;
                if (li == INVALID_LONG) {
                    li = 0L;
                }
                future = playback.seek(li);
                break;
            case ACTION_STOP:
                future = playback.stop();
                break;
            case ACTION_SINGLE:
                future = playback.single();
                break;
            case ACTION_TOGGLE_PLAYBACK:
                future = playback.togglePlayback();
                break;
            case ACTION_TOGGLE_RANDOM:
                future = playback.random();
                break;
            case ACTION_TOGGLE_REPEAT:
                future = playback.repeat();
                break;
            case ACTION_VOLUME_SET:
                if (l != INVALID_LONG) {
                    future = playback.setVolume((int) l);
                }
                break;
            case ACTION_VOLUME_STEP_DOWN:
                future = playback.stepVolume(-VOLUME_STEP);
                break;
            case ACTION_VOLUME_STEP_UP:
                future = playback.stepVolume(VOLUME_STEP);
                break;
            default:
                future = null;
                break;
        }

        return future;
    }

    /**
     * This method sets up the connection.
     *
     * <p>This should be used, as required, prior to run().</p>
     *
     * @param timeout The maximum time to wait for both a connection and/or status.
     * @param unit    The time unit of the {@code timeout} argument.
     * @return A token to use with {@link MPDApplication#removeConnectionLock(Object)}. If
     * {@code null}, a connection could not be established.
     */
    public static Object setupConnection(final long timeout, final TimeUnit unit) {
        final MPD mpd = APP.getMPD();

        if (mpd.getStatus().isValid()) {
            throw new IllegalStateException("setupConnection must be called with invalid status.");
        }

        final Object token = Integer.valueOf(new Random().nextInt());

        APP.addConnectionLock(token);
        boolean success = true;
        try {
            success = mpd.getConnectionStatus().waitForConnection(timeout, unit);

            if (success) {
                success = mpd.getStatus().waitForValidity(timeout, unit);
            }
        } catch (final InterruptedException ignored) {
        }

        final Object lockToken;
        if (success) {
            lockToken = token;
        } else {
            lockToken = null;
            APP.removeConnectionLock(token);
        }

        return lockToken;
    }
}
