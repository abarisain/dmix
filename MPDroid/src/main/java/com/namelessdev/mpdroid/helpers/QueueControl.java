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

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDPlaylist;
import org.a0z.mpd.exception.MPDException;

import android.util.Log;

import java.io.IOException;

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

    private static final MPDApplication APP = MPDApplication.getInstance();

    private static final int INVALID_INT = -1;

    private static final MPD MPD = APP.oMPDAsyncHelper.oMPD;

    private static final MPDPlaylist PLAYLIST = MPD.getPlaylist();

    private static final String TAG = "QueueControl";

    private QueueControl() {
        super();
    }

    /**
     * A method to send simple playlist controls with a integer
     * array argument which requires no result processing.
     *
     * @param command  The playlist command to send.
     * @param intArray The int array argument for the command.
     */
    public static void run(final int command, final int[] intArray) {
        APP.oMPDAsyncHelper.execAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    if (command == REMOVE_BY_ID) {
                        PLAYLIST.removeById(intArray);
                    }
                } catch (final IOException | MPDException e) {
                    Log.e(TAG, "Failed to remove by playlist id. intArray: " + intArray, e);
                }
            }
        });
    }

    /**
     * A method to send simple playlist controls with a
     * string argument which requires no result processing.
     *
     * @param command The playlist command to send.
     * @param s       The string argument for the command.
     */
    public static void run(final int command, final String s) {
        APP.oMPDAsyncHelper.execAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    if (command == SAVE_PLAYLIST) {
                        PLAYLIST.savePlaylist(s);
                    }
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
    public static void run(final int command) {
        run(command, INVALID_INT, INVALID_INT);
    }

    /**
     * A simple overload for run(command, int, int)
     *
     * @param command The playlist command to send.
     * @param i       The integer argument for the command.
     */
    public static void run(final int command, final int i) {
        run(command, i, INVALID_INT);
    }

    /**
     * A method to send simple playlist controls which requires no result processing.
     *
     * @param command The playlist command to send.
     * @param arg1    The first integer argument for the command.
     * @param arg2    The second integer argument for the command.
     */
    public static void run(final int command, final int arg1, final int arg2) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int workingCommand = command;
                final int i = arg1;
                int j = arg2;

                try {
                    switch (command) {
                        case MOVE_TO_LAST:
                            j = APP.oMPDAsyncHelper.oMPD.getStatus().getPlaylistLength() - 1;
                            workingCommand = MOVE;
                            break;
                        case MOVE_TO_NEXT:
                            j = APP.oMPDAsyncHelper.oMPD.getStatus().getSongPos();

                            if (i >= j) {
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
                            PLAYLIST.move(i, j);
                            break;
                        case REMOVE_ALBUM_BY_ID:
                            PLAYLIST.removeAlbumById(i);
                            break;
                        case REMOVE_BY_ID:
                            PLAYLIST.removeById(i);
                            break;
                        case SKIP_TO_ID:
                            MPD.skipToId(i);
                            break;
                        default:
                            break;
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
    public static void run(final int command, final int arg1, final int arg2, final int arg3) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int i = arg1;
                final int j = arg2;
                final int k = arg3;
                try {
                    switch (command) {
                        case MOVE:
                            PLAYLIST.moveByPosition(i, j, k);
                            break;
                        default:
                            break;
                    }
                } catch (final IOException | MPDException e) {
                    Log.e(TAG, "Failed to run simple playlist command. Argument 1: " + arg1
                            + " Argument 2: " + arg2 + " Argument 3: " + arg3, e);
                }
            }
        }).start();
    }

}
