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

import org.a0z.mpd.event.AbstractStatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MPD Playlist controller.
 */
public class MPDPlaylist extends AbstractStatusChangeListener {

    public static final String MPD_CMD_PLAYLIST_ADD = "add";

    public static final String MPD_CMD_PLAYLIST_CLEAR = "clear";

    public static final String MPD_CMD_PLAYLIST_DELETE = "rm";

    public static final String MPD_CMD_PLAYLIST_LIST = "playlistid";

    public static final String MPD_CMD_PLAYLIST_CHANGES = "plchanges";

    public static final String MPD_CMD_PLAYLIST_LOAD = "load";

    public static final String MPD_CMD_PLAYLIST_MOVE = "move";

    public static final String MPD_CMD_PLAYLIST_MOVE_ID = "moveid";

    public static final String MPD_CMD_PLAYLIST_REMOVE = "delete";

    public static final String MPD_CMD_PLAYLIST_REMOVE_ID = "deleteid";

    public static final String MPD_CMD_PLAYLIST_SAVE = "save";

    public static final String MPD_CMD_PLAYLIST_SHUFFLE = "shuffle";

    public static final String MPD_CMD_PLAYLIST_SWAP = "swap";

    public static final String MPD_CMD_PLAYLIST_SWAP_ID = "swapid";

    private static final String TAG = "MPDPlaylist";

    private static final boolean DEBUG = false;

    private final MPD mMPD;

    private final MusicList mList;

    private boolean mFirstRefresh = true;

    private int mLastPlaylistVersion = -1;

    /**
     * Creates a new playlist.
     */
    MPDPlaylist(final MPD mpd) {
        super();
        mMPD = mpd;

        mList = new MusicList();
    }

    /**
     * Adds a music to playlist.
     *
     * @param entry music/directory/playlist to be added.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void add(final FilesystemTreeEntry entry) throws MPDServerException {
        mMPD.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_ADD, entry.getFullpath());
        refresh();
    }

    /**
     * Adds a stream to playlist.
     *
     * @param url stream URL
     */
    public void addStream(final String url) throws MPDServerException {
        mMPD.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_ADD, url);
        refresh();
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
        final CommandQueue commandQueue = new CommandQueue();

        for (final Music music : collection) {
            commandQueue.add(MPD_CMD_PLAYLIST_ADD, music.getFullpath());
        }

        commandQueue.send(mMPD.getMpdConnection());
        refresh();
    }

    /**
     * Remove all songs except for the currently playing.
     */
    public void crop() {
        final MPDStatus mpdStatus = mMPD.getStatus();
        if (mpdStatus.isState(MPDStatus.STATE_PLAYING) ||
                mpdStatus.isState(MPDStatus.STATE_PAUSED)) {
            final int currentTrackId = mpdStatus.getSongId();
            final int playlistLength = mList.size();
            final int[] remove = new int[(playlistLength - 1)];

            if (playlistLength > 0) {
                if (currentTrackId != 0) {
                    try {
                        move(currentTrackId, 0);
                    } catch (final MPDServerException e) {
                        Log.d("MPD.java", "Failed to move the current track to 0.", e);
                    }
                }

                for (int i = 0; i < playlistLength - 1; i++) {
                    remove[i] = i + 1;
                }

                try {
                    removeByIndex(remove);
                } catch (final MPDServerException e) {
                    Log.d(TAG, "Failed to remove from the playlist for cropping.", e);
                }
            }
        }
    }

    /**
     * Clears playlist content.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void clear() throws MPDServerException {
        mMPD.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_CLEAR);
        mList.clear();
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
        mMPD.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_LOAD, file);
        refresh();
    }

    /**
     * Moves song with specified id to position {@code to}.
     *
     * @param songId Id of the song to be moved.
     * @param to     target position of the song to be moved.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void move(final int songId, final int to) throws MPDServerException {
        mMPD.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_MOVE_ID, Integer.toString(songId),
                Integer.toString(to));
        refresh();
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
        mMPD.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_MOVE, Integer.toString(from),
                Integer.toString(to));
        refresh();
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
            final CommandQueue commandQueue = new CommandQueue();
            final boolean moveUp = to < start;
            int from = start;
            int target = to;
            for (int i = 0; i < number; i++) {
                commandQueue.add(MPD_CMD_PLAYLIST_MOVE,
                        Integer.toString(from),
                        Integer.toString(target));
                if (moveUp) {
                    from++;
                    target++;
                }
            }
            commandQueue.send(mMPD.getMpdConnection());
            refresh();
        }
    }

    /*
     * React to playlist change on server and refresh the queue
     * @see
     * org.a0z.mpd.event.AbstractStatusChangeListener#playlistChanged(org.a0z
     * .mpd.MPDStatus, int)
     */
    @Override
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {
        super.playlistChanged(mpdStatus, oldPlaylistVersion);

        try {
            refresh(oldPlaylistVersion);
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to refresh.", e);
        }
    }

    /**
     * Reload playlist content. {@code refresh} has better performance and
     * is more server friendly, use it whenever possible.
     *
     * @throws MPDServerException if an error occur while contacting server.
     * @see #refresh(int)
     */
    private void refresh() throws MPDServerException {
        if (mFirstRefresh) {
            // TODO should be atomic
            final MPDStatus status = mMPD.getStatus();
            final List<String> response = mMPD.getMpdConnection()
                    .sendCommand(MPD_CMD_PLAYLIST_LIST);
            final List<Music> playlist = Music.getMusicFromList(response, false);

            mList.replace(playlist);

            mLastPlaylistVersion = status.getPlaylistVersion();
            mFirstRefresh = false;
        } else {
            mLastPlaylistVersion = refresh(mLastPlaylistVersion);
        }
    }

    /**
     * Do incremental update of playlist contents.
     *
     * @param playlistVersion last read playlist version
     * @return current playlist version.
     * @throws MPDServerException if an error occur while contacting server.
     */
    private int refresh(final int playlistVersion) throws MPDServerException {
        // TODO should be atomic
        final MPDStatus status = mMPD.getStatus();
        final List<String> response = mMPD.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_CHANGES,
                Integer.toString(playlistVersion));
        final List<Music> changes = Music.getMusicFromList(response, false);

        final int newLength = status.getPlaylistLength();
        final int oldLength = mList.size();
        final List<Music> newPlaylist = new ArrayList<>(newLength + 1);
        final int newPlaylistLength;

        if (newLength < oldLength) {
            newPlaylistLength = newLength;
        } else {
            newPlaylistLength = oldLength;
        }

        newPlaylist.addAll(mList.subList(0, newPlaylistLength));

        for (int i = newLength - oldLength; i > 0; i--) {
            newPlaylist.add(null);
        }

        for (final Music song : changes) {
            if (newPlaylist.size() > song.getPos() && song.getPos() > -1) {
                newPlaylist.set(song.getPos(), song);
            }
        }

        mList.replace(newPlaylist);

        return status.getPlaylistVersion();
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

        if (artist == null || album == null) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "Remove album " + album + " of " + artist);
        }

        final CommandQueue commandQueue = new CommandQueue();
        int num = 0;
        for (final Music song : songs) {
            if (album.equals(song.getAlbum())) {
                final boolean songIsAlbumArtist =
                        usingAlbumArtist && artist.equals(song.getAlbumArtist());
                final boolean songIsArtist =
                        !usingAlbumArtist && artist.equals(song.getArtist());

                if (songIsArtist || songIsAlbumArtist) {
                    final int id = song.getSongId();
                    commandQueue.add(MPD_CMD_PLAYLIST_REMOVE_ID, Integer.toString(id));
                    mList.removeById(id);
                    num++;
                }
            }
        }
        if (DEBUG) {
            Log.d(TAG, "Removed " + num + " songs");
        }
        commandQueue.send(mMPD.getMpdConnection());
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
        commandQueue.send(mMPD.getMpdConnection());

        for (final int id : songIds) {
            mList.removeById(id);
        }
    }

    /**
     * Removes entries from playlist.
     *
     * @param songs entries positions.
     * @throws MPDServerException if an error occur while contacting server.
     * @see #removeById(int[])
     */
    void removeByIndex(final int... songs) throws MPDServerException {
        Arrays.sort(songs);
        final CommandQueue commandQueue = new CommandQueue();

        for (int i = songs.length - 1; i >= 0; i--) {
            commandQueue.add(MPD_CMD_PLAYLIST_REMOVE, Integer.toString(songs[i]));
        }
        commandQueue.send(mMPD.getMpdConnection());

        for (int i = songs.length - 1; i >= 0; i--) {
            mList.removeByIndex(songs[i]);
        }
    }

    /**
     * Removes playlist file.
     *
     * @param file playlist filename without .m3u extension.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void removePlaylist(final String file) throws MPDServerException {
        mMPD.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_DELETE, file);
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
        mMPD.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_SAVE, file);
    }

    /**
     * Shuffles playlist content.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void shuffle() throws MPDServerException {
        mMPD.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_SHUFFLE);
    }

    /**
     * Retrieves playlist size. Operates on local copy of playlist, may not
     * reflect server's current playlist. You may call refresh() before calling
     * size().
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
        mMPD.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_SWAP_ID,
                Integer.toString(song1Id), Integer.toString(song2Id));
        refresh();
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
        mMPD.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_SWAP, Integer.toString(song1),
                Integer.toString(song2));
        refresh();
    }

    /**
     * Retrieves a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder(mList.toString().length());
        for (final Music music : mList.getMusic()) {
            stringBuilder.append(music);
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }

}
