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

package org.a0z.mpd;

import org.a0z.mpd.connection.MPDConnection;
import org.a0z.mpd.exception.MPDServerException;
import org.a0z.mpd.item.FilesystemTreeEntry;
import org.a0z.mpd.item.Music;

import java.util.Arrays;
import java.util.List;

/**
 * MPD Playlist controller.
 */
public class MPDPlaylist {

    public static final String MPD_CMD_PLAYLIST_ADD = "add";

    public static final String MPD_CMD_PLAYLIST_CHANGES = "plchanges";

    public static final String MPD_CMD_PLAYLIST_CLEAR = "clear";

    public static final String MPD_CMD_PLAYLIST_DELETE = "rm";

    public static final String MPD_CMD_PLAYLIST_LIST = "playlistid";

    public static final String MPD_CMD_PLAYLIST_LOAD = "load";

    public static final String MPD_CMD_PLAYLIST_MOVE = "move";

    public static final String MPD_CMD_PLAYLIST_MOVE_ID = "moveid";

    public static final String MPD_CMD_PLAYLIST_REMOVE = "delete";

    public static final String MPD_CMD_PLAYLIST_REMOVE_ID = "deleteid";

    public static final String MPD_CMD_PLAYLIST_SAVE = "save";

    public static final String MPD_CMD_PLAYLIST_SHUFFLE = "shuffle";

    public static final String MPD_CMD_PLAYLIST_SWAP = "swap";

    public static final String MPD_CMD_PLAYLIST_SWAP_ID = "swapid";

    private static final boolean DEBUG = false;

    private static final String TAG = "MPDPlaylist";

    private final MPDConnection mConnection;

    private final MusicList mList;

    private int mLastPlaylistVersion = -1;

    /**
     * Creates a new playlist.
     */
    MPDPlaylist(final MPDConnection mpdConnection) {
        super();

        mList = new MusicList();
        mConnection = mpdConnection;
    }

    static CommandQueue addAllCommand(final Iterable<Music> collection) {
        final CommandQueue commandQueue = new CommandQueue();

        for (final Music music : collection) {
            commandQueue.add(MPD_CMD_PLAYLIST_ADD, music.getFullPath());
        }

        return commandQueue;
    }

    static MPDCommand addCommand(final String fullPath) {
        return new MPDCommand(MPD_CMD_PLAYLIST_ADD, fullPath);
    }

    static MPDCommand clearCommand() {
        return new MPDCommand(MPD_CMD_PLAYLIST_CLEAR);
    }

    static CommandQueue cropCommand(final MPD mpd) {
        final CommandQueue commandQueue = new CommandQueue();
        final int currentTrackID = mpd.getStatus().getSongId();
        /** Null range ends are broken in MPD-0.18 on 32-bit arch, see bug #4080. */
        final int playlistLength = mpd.getStatus().getPlaylistLength();

        if (currentTrackID < 0) {
            throw new IllegalStateException("Cannot crop when media server is inactive.");
        }

        if (playlistLength == 1) {
            throw new IllegalStateException("Cannot crop when media server playlist length is 1.");
        }

        commandQueue.add(MPD_CMD_PLAYLIST_MOVE_ID, Integer.toString(currentTrackID), "0");
        commandQueue.add(MPD_CMD_PLAYLIST_REMOVE, "1:" + playlistLength);

        return commandQueue;
    }

    static MPDCommand loadCommand(final String file) {
        return new MPDCommand(MPD_CMD_PLAYLIST_LOAD, file);
    }

    static CommandQueue removeByIndexCommand(final int... songs) {
        Arrays.sort(songs);
        final CommandQueue commandQueue = new CommandQueue();

        for (int i = songs.length - 1; i >= 0; i--) {
            commandQueue.add(MPD_CMD_PLAYLIST_REMOVE, Integer.toString(songs[i]));
        }

        return commandQueue;
    }

    /**
     * Adds a music to playlist.
     *
     * @param entry music/directory/playlist to be added.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void add(final FilesystemTreeEntry entry) throws MPDServerException {
        mConnection.sendCommand(addCommand(entry.getFullPath()));
    }

    /**
     * Adds a {@code collection} of {@code Music} to playlist.
     *
     * @param collection {@code collection} of {@code Music} to be added to
     *                   playlist.
     * @throws MPDServerException if an error occur while contacting server.
     * @see Music
     */
    public void addAll(final Iterable<Music> collection) throws MPDServerException {
        addAllCommand(collection).send(mConnection);
    }

    /**
     * Adds a stream to playlist.
     *
     * @param url stream URL
     */
    public void addStream(final String url) throws MPDServerException {
        mConnection.sendCommand(MPD_CMD_PLAYLIST_ADD, url);
    }

    /**
     * Clears playlist content.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void clear() throws MPDServerException {
        mConnection.sendCommand(clearCommand());
    }

    /**
     * Retrieves music at position index in playlist. Operates on local copy of
     * playlist, may not reflect server's current playlist.
     *
     * @param index position.
     * @return music at position index.
     */
    public Music getByIndex(final int index) {
        return mList.getByIndex(index);
    }

    /**
     * Retrieves all songs as an {@code List} of {@code Music}.
     *
     * @return all songs as an {@code List} of {@code Music}.
     * @see Music
     */
    public List<Music> getMusicList() {
        return mList.getMusic();
    }

    /**
     * Load playlist file.
     *
     * @param file playlist filename without .m3u extension.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void load(final String file) throws MPDServerException {
        mConnection.sendCommand(loadCommand(file));
    }

    /**
     * Moves song with specified id to position {@code to}.
     *
     * @param songId Id of the song to be moved.
     * @param to     target position of the song to be moved.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void move(final int songId, final int to) throws MPDServerException {
        mConnection.sendCommand(MPD_CMD_PLAYLIST_MOVE_ID, Integer.toString(songId),
                Integer.toString(to));
    }

    /**
     * Moves song at position {@code from} to position {@code to}.
     *
     * @param from current position of the song to be moved.
     * @param to   target position of the song to be moved.
     * @throws MPDServerException if an error occur while contacting server.
     * @see #move(int, int)
     */
    public void moveByPosition(final int from, final int to) throws MPDServerException {
        mConnection.sendCommand(MPD_CMD_PLAYLIST_MOVE, Integer.toString(from),
                Integer.toString(to));
    }

    /**
     * Moves {@code number} songs starting at position {@code start}
     * to position {@code to}.
     *
     * @param start  current position of the first of the songs to be moved.
     * @param number number of songs to be moved.
     * @param to     first target position of the songs to be moved to.
     * @throws MPDServerException if an error occur while contacting server.
     * @see #moveByPosition(int, int)
     */
    public void moveByPosition(final int start, final int number, final int to)
            throws MPDServerException {
        if (start != to && number > 0) {
            final String beginRange = Integer.toString(start);
            final String endRange = Integer.toString(start + number);
            final String target = Integer.toString(to);
            mConnection
                    .sendCommand(MPD_CMD_PLAYLIST_MOVE, beginRange + ':' + endRange, target);
        }
    }

    /**
     * Reloads the playlist content.
     *
     * @param mpdStatus A current {@code MPDStatus} object.
     * @throws MPDServerException Thrown if an error occurred while communicating with the media
     *                            server.
     */
    void refresh(final MPDStatus mpdStatus) throws MPDServerException {
        /** Synchronize this block to make sure the playlist version stays coherent. */
        synchronized (mList) {
            final int newPlaylistVersion = mpdStatus.getPlaylistVersion();

            if (mLastPlaylistVersion == -1 || mList.size() == 0) {
                final List<String> response = mConnection
                        .sendCommand(MPD_CMD_PLAYLIST_LIST);
                final List<Music> playlist = Music.getMusicFromList(response, false);

                mList.replace(playlist);
            } else if (mLastPlaylistVersion != newPlaylistVersion) {
                final List<String> response =
                        mConnection.sendCommand(MPD_CMD_PLAYLIST_CHANGES,
                                Integer.toString(mLastPlaylistVersion));
                final List<Music> changes = Music.getMusicFromList(response, false);

                for (final Music song : changes) {
                    mList.manipulate(song);
                }

                mList.removeByRange(mpdStatus.getPlaylistLength(), mList.size());
            }

            mLastPlaylistVersion = newPlaylistVersion;
        }
    }

    /**
     * Removes album of given ID from playlist.
     *
     * @param songId entries positions.
     * @throws MPDServerException if an error occur while contacting server.
     * @see #removeById(int[])
     */
    public void removeAlbumById(final int songId) throws MPDServerException {
        // Better way to get artist of given songId?
        final List<Music> songs = mList.getMusic();
        String artist = "";
        String album = "";
        int num = 0;
        boolean usingAlbumArtist = true;

        for (final Music song : songs) {
            if (song.getSongId() == songId) {
                artist = song.getAlbumArtist();
                if (artist == null || artist.isEmpty()) {
                    usingAlbumArtist = false;
                    artist = song.getArtist();
                }
                album = song.getAlbum();
                break;
            }
        }

        if (artist != null && album != null) {
            if (DEBUG) {
                Log.debug(TAG, "Remove album " + album + " of " + artist);
            }
            final CommandQueue commandQueue = new CommandQueue();

            /** Don't allow the list to change before we've computed the CommandList. */
            synchronized (mList) {
                final List<Music> tracks = mList.getMusic();

                for (final Music track : tracks) {
                    if (album.equals(track.getAlbum())) {
                        final boolean songIsAlbumArtist =
                                usingAlbumArtist && artist.equals(track.getAlbumArtist());
                        final boolean songIsArtist =
                                !usingAlbumArtist && artist.equals(track.getArtist());

                        if (songIsArtist || songIsAlbumArtist) {
                            final String songID = Integer.toString(track.getSongId());
                            commandQueue.add(MPD_CMD_PLAYLIST_REMOVE_ID, songID);
                            num++;
                        }
                    }
                }
            }

            commandQueue.send(mConnection);
        }
        if (DEBUG) {
            Log.debug(TAG, "Removed " + num + " songs");
        }
    }

    /**
     * Removes entries from playlist.
     *
     * @param songIds entries IDs.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void removeById(final int... songIds) throws MPDServerException {
        final CommandQueue commandQueue = new CommandQueue();

        for (final int id : songIds) {
            commandQueue.add(MPD_CMD_PLAYLIST_REMOVE_ID, Integer.toString(id));
        }
        commandQueue.send(mConnection);
    }

    /**
     * Removes entries from playlist.
     *
     * @param songs entries positions.
     * @throws MPDServerException if an error occur while contacting server.
     * @see #removeById(int[])
     */
    void removeByIndex(final int... songs) throws MPDServerException {
        removeByIndexCommand(songs).send(mConnection);
    }

    /**
     * Removes playlist file.
     *
     * @param file playlist filename without .m3u extension.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void removePlaylist(final String file) throws MPDServerException {
        mConnection.sendCommand(MPD_CMD_PLAYLIST_DELETE, file);
    }

    /**
     * Save playlist file.
     *
     * @param file playlist filename without .m3u extension.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void savePlaylist(final String file) throws MPDServerException {
        // If the playlist already exists, save will fail. So, just remove it first!
        try {
            removePlaylist(file);
        } catch (final MPDServerException ignored) {
            /** We're removing it just in case it exists. */
        }
        mConnection.sendCommand(MPD_CMD_PLAYLIST_SAVE, file);
    }

    /**
     * Shuffles playlist content.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void shuffle() throws MPDServerException {
        mConnection.sendCommand(MPD_CMD_PLAYLIST_SHUFFLE);
    }

    /**
     * Retrieves playlist size. Operates on local copy of playlist, may not
     * reflect server's current playlist.
     *
     * @return playlist size.
     */
    public int size() {
        return mList.size();
    }

    /**
     * Swap positions of song1 and song2.
     *
     * @param song1Id id of song1 in playlist.
     * @param song2Id id of song2 in playlist.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void swap(final int song1Id, final int song2Id) throws MPDServerException {
        mConnection.sendCommand(MPD_CMD_PLAYLIST_SWAP_ID,
                Integer.toString(song1Id), Integer.toString(song2Id));
    }

    /**
     * Swap positions of song1 and song2.
     *
     * @param song1 position of song1 in playlist.
     * @param song2 position of song2 in playlist
     * @throws MPDServerException if an error occur while contacting server.
     * @see #swap(int, int)
     */
    public void swapByPosition(final int song1, final int song2) throws MPDServerException {
        mConnection.sendCommand(MPD_CMD_PLAYLIST_SWAP, Integer.toString(song1),
                Integer.toString(song2));
    }

    /**
     * Retrieves a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder(mList.toString().length());
        synchronized (mList) {
            for (final Music music : mList.getMusic()) {
                stringBuilder.append(music);
                stringBuilder.append(MPDCommand.MPD_CMD_NEWLINE);
            }
        }
        return stringBuilder.toString();
    }

}
