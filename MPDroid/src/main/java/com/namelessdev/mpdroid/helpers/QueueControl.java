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
import com.anpmech.mpd.MPDPlaylist;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Music;
import com.namelessdev.mpdroid.MPDApplication;

import android.support.annotation.IntDef;
import android.util.Log;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Playlist control implements simple playlist controls which require no result processing.
 */
public final class QueueControl {

    public static final int CLEAR = 0;

    public static final int MOVE = 1;

    public static final int MOVE_TO_LAST = 2;

    public static final int MOVE_TO_NEXT = 3;

    public static final int REMOVE_ALBUM_BY_ID = 4;

    public static final int REMOVE_BY_ID = 5;

    public static final int SAVE_PLAYLIST = 6;

    public static final int SKIP_TO_ID = 7;

    public static final int SHUFFLE = 8;

    private static final MPDApplication APP = MPDApplication.getInstance();

    private static final int INVALID_INT = -1;

    private static final MPD MPD = APP.getMPD();

    private static final MPDPlaylist PLAYLIST = MPD.getPlaylist();

    private static final String TAG = "QueueControl";


    private QueueControl() {
        super();
    }

    /**
     * This method is a utility to throw an exception when an unexpected command is given to the
     * calling method.
     *
     * @param command The unexpected given command.
     */
    private static void argumentNotSupported(@ControlType final int command) {
        throw new IllegalArgumentException("QueueControl not setup: " + command);
    }

    /**
     * A method to send simple playlist controls with a integer array argument which requires no
     * result processing.
     *
     * @param command  The playlist command to send.
     * @param intArray The int array argument for the command.
     */
    public static void run(@ControlType final int command, final int[] intArray) {
        APP.getAsyncHelper().execAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    if (command != REMOVE_BY_ID) {
                        argumentNotSupported(command);
                    }
                    PLAYLIST.removeById(intArray);
                } catch (final IOException | MPDException e) {
                    Log.e(TAG, "Failed to remove by playlist id. intArray: " + intArray, e);
                }
            }
        });
    }

    /**
     * A method to send simple playlist controls with a string argument which requires no result
     * processing.
     *
     * @param command The playlist command to send.
     * @param s       The string argument for the command.
     */
    public static void run(@ControlType final int command, final String s) {
        APP.getAsyncHelper().execAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    if (command != SAVE_PLAYLIST) {
                        argumentNotSupported(command);
                    }
                    PLAYLIST.savePlaylist(s);
                } catch (final IOException | MPDException e) {
                    Log.e(TAG, "Failed to save the playlist. String: " + s, e);
                }
            }
        });
    }

    /**
     * A simple overload for run(command, int, int)
     *
     * @param command The playlist command to send.
     */
    public static void run(@ControlType final int command) {
        run(command, INVALID_INT, INVALID_INT);
    }

    /**
     * A simple overload for run(command, int, int)
     *
     * @param command The playlist command to send.
     * @param i       The integer argument for the command.
     */
    public static void run(@ControlType final int command, final int i) {
        run(command, i, INVALID_INT);
    }

    public static void run(@ControlType final int command, final Music track) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (command != SKIP_TO_ID) {
                    argumentNotSupported(command);
                }

                try {
                    MPD.getPlayback().play(track).get();
                } catch (final IOException | MPDException e) {
                    Log.e(TAG, "Failed to add track to play.", e);
                }
            }
        }).start();
    }

    /**
     * A method to send simple playlist controls which requires no result processing.
     *
     * @param command The playlist command to send.
     * @param arg1    The first integer argument for the command.
     * @param arg2    The second integer argument for the command.
     */
    public static void run(@ControlType final int command, final int arg1, final int arg2) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int workingCommand = command;
                int j = arg2;

                try {
                    switch (command) {
                        case MOVE_TO_LAST:
                            j = MPD.getStatus().getPlaylistLength() - 1;
                            workingCommand = MOVE;
                            break;
                        case MOVE_TO_NEXT:
                            j = MPD.getStatus().getSongPos();

                            if (arg1 >= j) {
                                j += 1;
                            }

                            workingCommand = MOVE;
                            break;
                        default:
                            break;
                    }

                    switch (workingCommand) {
                        case CLEAR:
                            PLAYLIST.clear();
                            break;
                        case MOVE:
                            PLAYLIST.move(arg1, j);
                            break;
                        case REMOVE_ALBUM_BY_ID:
                            PLAYLIST.removeAlbumById(arg1);
                            break;
                        case REMOVE_BY_ID:
                            PLAYLIST.removeById(arg1);
                            break;
                        case SHUFFLE:
                            PLAYLIST.shuffle();
                            break;
                        default:
                            argumentNotSupported(command);
                    }
                } catch (final IOException | MPDException e) {
                    Log.e(TAG, "Failed to run simple playlist command. Argument 1: " + arg1 +
                            " Argument 2: " + arg2, e);
                }
            }
        }).start();
    }

    /**
     * A method to send simple playlist controls which requires no result processing.
     *
     * @param command The playlist command to send.
     * @param arg1    An integer argument for the command.
     * @param arg2    An integer argument for the command.
     * @param arg3    An integer argument for the command.
     */
    public static void run(@ControlType final int command, final int arg1, final int arg2,
            final int arg3) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (command != MOVE) {
                        argumentNotSupported(command);
                    }
                    PLAYLIST.moveByPosition(arg1, arg2, arg3);
                } catch (final IOException | MPDException e) {
                    Log.e(TAG, "Failed to run simple playlist command. Argument 1: " + arg1
                            + " Argument 2: " + arg2 + " Argument 3: " + arg3, e);
                }
            }
        }).start();
    }

    /**
     * This annotation is used to give a hint if a possible invalid value is fed to run().
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CLEAR, MOVE, MOVE_TO_LAST, MOVE_TO_NEXT, REMOVE_ALBUM_BY_ID, REMOVE_BY_ID,
            SAVE_PLAYLIST, SKIP_TO_ID})
    private @interface ControlType {

    }
}
