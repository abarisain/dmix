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
import org.a0z.mpd.exception.MPDServerException;

import android.util.Log;

/**
 * Playlist control implements simple playlist controls which require no result processing.
 */
public final class QueueControl {

    private static final String TAG = "QueueControl";

    private static final MPDApplication app = MPDApplication.getInstance();

    private static final MPD mpd = app.oMPDAsyncHelper.oMPD;

    private static final MPDPlaylist playlist = mpd.getPlaylist();

    private static final int INVALID_INT = -1;

    public static final int CLEAR = 0;

    public static final int MOVE = 1;

    public static final int MOVE_TO_LAST = 2;

    public static final int MOVE_TO_NEXT = 3;

    public static final int REMOVE_ALBUM_BY_ID = 4;

    public static final int REMOVE_BY_ID = 5;

    public static final int SAVE_PLAYLIST = 6;

    public static final int SKIP_TO_ID = 7;

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
        app.oMPDAsyncHelper.execAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    if (command == REMOVE_BY_ID) {
                        playlist.removeById(intArray);
                    }
                } catch (final MPDServerException e) {
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
        app.oMPDAsyncHelper.execAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    if (command == SAVE_PLAYLIST) {
                        playlist.savePlaylist(s);
                    }
                } catch (final MPDServerException e) {
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
                int i = arg1;
                int j = arg2;

                try {
                    switch (command) {
                        case MOVE_TO_LAST:
                            j = app.oMPDAsyncHelper.oMPD.getStatus().getPlaylistLength() - 1;
                            workingCommand = MOVE;
                            break;
                        case MOVE_TO_NEXT:
                            j = app.oMPDAsyncHelper.oMPD.getStatus().getSongPos();

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
                            playlist.clear();
                            break;
                        case MOVE:
                            playlist.move(i, j);
                            break;
                        case REMOVE_ALBUM_BY_ID:
                            playlist.removeAlbumById(i);
                            break;
                        case REMOVE_BY_ID:
                            playlist.removeById(i);
                            break;
                        case SKIP_TO_ID:
                            mpd.skipToId(i);
                            break;
                        default:
                            break;
                    }
                } catch (final MPDServerException e) {
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
                int i = arg1;
                int j = arg2;
                int k = arg3;
                try {
                    switch (command) {
                        case MOVE:
                            playlist.moveByPosition(i, j, k);
                            break;
                        default:
                            break;
                    }
                } catch (final MPDServerException e) {
                    Log.e(TAG, "Failed to run simple playlist command. Argument 1: " + arg1 + " Argument 2: " + arg2 + " Argument 3: " + arg3, e);
                }
            }
        }).start();
    }

}
