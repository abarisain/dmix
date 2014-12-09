/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice,this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.a0z.mpd.subsystem;

import org.a0z.mpd.CommandQueue;
import org.a0z.mpd.Log;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.Tools;
import org.a0z.mpd.connection.MPDConnection;
import org.a0z.mpd.exception.MPDException;
import org.a0z.mpd.item.FilesystemTreeEntry;
import org.a0z.mpd.item.Music;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.a0z.mpd.Tools.KEY;
import static org.a0z.mpd.Tools.VALUE;

/**
 * A class to manage the <A HREF="http://www.musicpd.org/doc/protocol/stickers.html">sticker</A>
 * subsystem of the <A HREF="http://www.musicpd.org/doc/protocol">MPD protocol</A>. This will query
 * the sticker database on the connected media server.
 */
public class Sticker {

    /** A response returned from a {@link #CMD_ACTION_FIND}. */
    private static final String CMD_RESPONSE_FILE = "file";

    /** The base command for this subsystem. */
    private static final String CMD_STICKER = "sticker";

    /** A response returned from any sticker command. */
    private static final String CMD_RESPONSE_STICKER = CMD_STICKER;

    /**
     * Command text required to generate a command to retrieve a sticker.
     * <BR><BR>
     * <B>Protocol command syntax:</B><BR> {@code sticker get {TYPE} {URI} {NAME}}.
     * <BR><BR>
     * <BR><B>Sample protocol output:</B><BR>
     * {@code sticker get song track.wav key}<BR>
     * {@code sticker: key=value}<BR>
     * {@code OK}
     */
    private static final String CMD_ACTION_GET = CMD_STICKER + ' ' + "get";

    /**
     * Command text required to generate a command to set a sticker.
     * <BR><BR>
     * <B>Protocol command syntax:</B> {@code sticker set {TYPE} {URI} {NAME} {VALUE}}.
     * <BR><BR>
     * <B>Sample protocol output:</B><BR>
     * {@code sticker set song track.wav key value}<BR>
     * {@code OK}
     */
    private static final String CMD_ACTION_SET = CMD_STICKER + ' ' + "set";

    /**
     * Command text required to generate a command to delete a sticker.
     * <BR><BR>
     * <B>Protocol command syntax:</B> {@code sticker delete {TYPE} {URI} [NAME]}.
     * <BR><BR>
     * <B>Sample protocol output:</B><BR>
     * {@code sticker delete song track.wav}<BR>
     * {@code OK}<BR>
     */
    private static final String CMD_ACTION_DELETE = CMD_STICKER + ' ' + "delete";

    /**
     * Command text required to generate a command to list stickers in a track.
     * <BR><BR>
     * <B>Protocol command syntax:</B> {@code sticker list {TYPE} {URI}}.
     * <BR><BR>
     * <B>Sample protocol output:</B><BR>
     * {@code sticker list song track.wav}<BR>
     * {@code sticker: key=value}<BR>
     * {@code OK}
     */
    private static final String CMD_ACTION_LIST = CMD_STICKER + ' ' + "list";

    /**
     * Command text required to generate a command to find stickers, recursively.
     * <BR><BR>
     * <B>Protocol command syntax:</B> {@code sticker find {TYPE} {URI} {NAME}}.
     * <BR><BR>
     * <B>Sample protocol output:</B><BR>
     * {@code sticker find song /dir key}<BR>
     * {@code file: track.wav}<BR>
     * {@code sticker: key=value}<BR>
     * {@code OK}
     */
    private static final String CMD_ACTION_FIND = CMD_STICKER + ' ' + "find";

    /** This is a {@code {TYPE}} argument for sticker commands. */
    private static final String CMD_STICKER_TYPE_SONG = "song";

    /** The debug flag, enable to get debug information in the log output. */
    private static final boolean DEBUG = true;

    /** The maximum rating used for the home grown rating system. */
    private static final int MAX_RATING = 100;

    /** The minimum rating used for the home grown rating system. */
    private static final int MIN_RATING = 0;

    /** This is a sticker {@code NAME} argument for the home grown ratings. */
    private static final String RATING_STICKER = "rating";

    private static final String STICKERS_NOT_AVAILABLE =
            "Stickers are not available on this server.";

    private static final String TAG = "Sticker";

    /** The connection to the server. */
    private final MPDConnection mConnection;

    /**
     * The constructor to get a sticker manager.
     *
     * @param connection The connection to use to query the media server for sticker handling.
     */
    public Sticker(final MPDConnection connection) {
        super();

        mConnection = connection;
    }

    /**
     * Generates a {@code CommandQueue} to retrieve information used to generate {@code Music}
     * objects.
     *
     * @param response The media server response from which to retrieve the music list.
     * @return A command queue to retrieve a media server response for a list of music.
     */
    private static CommandQueue getMusicCommand(final Collection<String> response) {
        final CommandQueue commandQueue = new CommandQueue();

        for (final String[] pair : Tools.splitResponse(response)) {
            if (CMD_RESPONSE_FILE.equals(pair[KEY])) {
                commandQueue.add(MPDCommand.MPD_CMD_LISTALL, pair[VALUE]);
            }
        }

        return commandQueue;
    }

    /**
     * Sanity check for the sticker manager methods.
     *
     * @param entry The entry to process.
     */
    private static void onlyMusicSupported(final FilesystemTreeEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Failed on a null argument.");
        }

        if (!(entry instanceof Music)) {
            throw new IllegalArgumentException("Stickers are only supported with Music objects: " +
                    "Class: " + entry.getClass() + " Entry: " + entry.getFullPath());
        }
    }

    /**
     * Deletes a sticker.
     *
     * @param entry   The entry to delete.
     * @param sticker The sticker key to delete. If null, all stickers associated with this entry
     *                will be removed.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void delete(final FilesystemTreeEntry entry, final String sticker)
            throws IOException, MPDException {
        onlyMusicSupported(entry);

        if (isAvailable()) {
            mConnection.sendCommand(CMD_ACTION_DELETE, CMD_STICKER_TYPE_SONG, entry.getFullPath(),
                    sticker);
        } else {
            Log.debug(TAG, STICKERS_NOT_AVAILABLE);
        }
    }

    /**
     * Searches the media server sticker database for matching stickers below the entry given.
     * For each matching track, it prints the URI and that one sticker's value.
     *
     * @param entry The entry to search below in the entry's hierarchy.
     * @param name  The name to search the stickers for.
     * @return A map of entries from the media server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public Map<Music, Map<String, String>> find(final FilesystemTreeEntry entry,
            final String name) throws IOException, MPDException {
        onlyMusicSupported(entry);

        final Map<Music, Map<String, String>> foundStickers;
        if (isAvailable()) {
            final List<String> response =
                    mConnection.sendCommand(CMD_ACTION_FIND, entry.getFullPath(), name);

            /** Generate a map used to create the result. */
            final Map<String, Music> musicPair = getMusicPair(response);
            foundStickers = new HashMap<>(musicPair.size());
            final Map<String, String> currentTrackStickers = new HashMap<>();
            Music currentMusic = null;

            for (final String[] sticker : Tools.splitResponse(response)) {
                if (CMD_RESPONSE_FILE.equals(sticker[KEY])) {
                    /** Clear the old map, start new! */
                    if (!foundStickers.isEmpty()) {
                        foundStickers.put(currentMusic, currentTrackStickers);
                        currentTrackStickers.clear();
                    }

                    currentMusic = musicPair.get(sticker[VALUE]);
                } else if (CMD_RESPONSE_STICKER.equals(sticker[KEY])) {
                    final int delimiterIndex = sticker[VALUE].indexOf('=');
                    final String stickerKey = sticker[VALUE].substring(0, delimiterIndex);
                    final String stickerValue = sticker[VALUE].substring(delimiterIndex + 1);

                    currentTrackStickers.put(stickerKey, stickerValue);
                }
            }
        } else {
            Log.debug(TAG, STICKERS_NOT_AVAILABLE);
            foundStickers = Collections.emptyMap();
        }

        return foundStickers;
    }

    /**
     * Reads the sticker value for this entry.
     *
     * @param entry The entry to retrieve.
     * @param name  The optional name to sticker key to receive. If null all names will be
     *              retrieved.
     * @return A map of entries from the media server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public String get(final FilesystemTreeEntry entry, final String name)
            throws IOException, MPDException {
        onlyMusicSupported(entry);

        String foundSticker = null;

        if (isAvailable()) {
            /** Do not throw exception when attempting to retrieve a non-existant sticker. */
            final int[] nonfatalErrors = {MPDException.ACK_ERROR_NO_EXIST};
            final List<String> response = mConnection.sendCommand(CMD_ACTION_GET, nonfatalErrors,
                    CMD_STICKER_TYPE_SONG, entry.getFullPath(), name);

            if (response == null) {
                if (DEBUG) {
                    Log.debug(TAG, "No responses received from sticker get query. FullPath: " +
                            entry.getFullPath());
                }
            } else {
                for (final String[] sticker : Tools.splitResponse(response)) {
                    if (CMD_RESPONSE_STICKER.equals(sticker[KEY])) {
                        final int index = sticker[VALUE].indexOf('=');

                        foundSticker = sticker[VALUE].substring(index + 1);
                    }
                }
            }
        }

        return foundSticker;
    }

    /**
     * This returns a map of FullPath (from Music.getFullPath()) and Music.
     *
     * @param response The media server response from which to retrieve the music map.
     * @return A {@code Map\<FullPath, Music\>} map.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    private Map<String, Music> getMusicPair(final Collection<String> response)
            throws IOException, MPDException {
        final List<String> musicResponse = getMusicCommand(response).send(mConnection);
        final List<Music> musicList = Music.getMusicFromList(musicResponse, false);
        final Map<String, Music> musicPair = new HashMap<>(musicList.size());

        for (final Music music : musicList) {
            musicPair.put(music.getFullPath(), music);
        }

        return musicPair;
    }

    /**
     * Retrieves rating of a entry.
     *
     * @return rating of entry.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public int getRating(final FilesystemTreeEntry entry) throws IOException, MPDException {
        final String rating = get(entry, RATING_STICKER);
        int resultRating;

        try {
            /** This will not throw an NullPointerException, only a NumberFormatException. */
            resultRating = Integer.parseInt(rating);
        } catch (final NumberFormatException e) {
            if (DEBUG) {
                if (rating == null) {
                    Log.debug(TAG, "Rating doesn't exist for this entry: " + entry.getFullPath());
                } else {
                    Log.debug(TAG, "Failed to parse rating by sticker.", e);
                }
            }

            resultRating = 0;
        }

        return resultRating;
    }

    /**
     * Checks to see if stickers are available on the current media server.
     *
     * @return True if stickers are available, false otherwise.
     */
    public final boolean isAvailable() {
        return mConnection.isCommandAvailable(CMD_STICKER);
    }

    /**
     * Lists the stickers for the specified object.
     *
     * @param entry The entry to list stickers from.
     * @return A map of entries from the media server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public Map<String, String> list(final FilesystemTreeEntry entry)
            throws IOException, MPDException {
        onlyMusicSupported(entry);

        final Map<String, String> stickers;
        final boolean isAvailable = isAvailable();

        if (isAvailable) {
            final List<String> response = mConnection.sendCommand(CMD_ACTION_LIST,
                    CMD_STICKER_TYPE_SONG, entry.getFullPath());

            if (response == null) {
                if (DEBUG) {
                    Log.debug(TAG, "No responses received from sticker list query. FullPath: " +
                            entry.getFullPath());
                }
                stickers = Collections.emptyMap();
            } else {
                stickers = new HashMap<>(response.size());

                for (final String[] sticker : Tools.splitResponse(response)) {
                    if (CMD_RESPONSE_STICKER.equals(sticker[KEY])) {
                        final int delimiterIndex = sticker[VALUE].indexOf('=');
                        final String stickerKey = sticker[VALUE].substring(0, delimiterIndex);
                        final String stickerValue = sticker[VALUE].substring(delimiterIndex + 1);

                        stickers.put(stickerKey, stickerValue);
                    }
                }
            }
        } else {
            Log.debug(TAG, STICKERS_NOT_AVAILABLE);
            stickers = Collections.emptyMap();
        }

        return stickers;
    }

    /**
     * Add a sticker key-value pair.
     *
     * @param entry   The entry with which to associate the sticker key-value pair.
     * @param sticker The sticker key.
     * @param value   The sticker value.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void set(final FilesystemTreeEntry entry, final String sticker, final String value)
            throws IOException, MPDException {
        onlyMusicSupported(entry);

        if (isAvailable()) {
            mConnection.sendCommand(CMD_ACTION_SET, CMD_STICKER_TYPE_SONG, entry.getFullPath(),
                    sticker, value);
        } else {
            Log.debug(TAG, STICKERS_NOT_AVAILABLE);
        }
    }

    /**
     * Sets the rating for the given entry.
     *
     * @param entry  The entry to rate.
     * @param rating The rating to set the entry to, from {@code MIN_RATING} to
     *               {@code MAX_RATING}.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void setRating(final FilesystemTreeEntry entry, final int rating)
            throws IOException, MPDException {
        final int maximumRating = Math.min(MAX_RATING, rating);
        final int boundedRating = Math.max(MIN_RATING, maximumRating);

        set(entry, RATING_STICKER, Integer.toString(boundedRating));
    }
}
