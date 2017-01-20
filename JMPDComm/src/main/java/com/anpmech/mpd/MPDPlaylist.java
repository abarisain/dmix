/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2016 The MPDroid Project
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

package com.anpmech.mpd;

import com.anpmech.mpd.commandresponse.MusicResponse;
import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.connection.MPDConnection;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.FilesystemTreeEntry;
import com.anpmech.mpd.item.Music;
import com.anpmech.mpd.subsystem.status.MPDStatus;
import com.anpmech.mpd.subsystem.status.MPDStatusMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * The frontend for {@link MusicList} and MPD protocol
 * <A HREF="http://www.musicpd.org/doc/protocol/queue.html">playlist queue controller</A>.
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

    private final Semaphore mPlaylistValidity = new Semaphore(0);

    private int mLastPlaylistVersion = -1;

    /**
     * The sole constructor.
     *
     * @param mpdConnection The connection to use to retrieve playlist queue information.
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
        return MPDCommand.create(MPD_CMD_PLAYLIST_ADD, fullPath);
    }

    static MPDCommand clearCommand() {
        return MPDCommand.create(MPD_CMD_PLAYLIST_CLEAR);
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
        if (mpd.getIdleConnection().isProtocolVersionSupported(0, 16)) {
            commandQueue.add(MPD_CMD_PLAYLIST_REMOVE, "1:" + playlistLength);
        } else {
            for (int i = playlistLength - 1; i >= 1; i--) {
                commandQueue.add(MPD_CMD_PLAYLIST_REMOVE, Integer.toString(i));
            }
        }

        return commandQueue;
    }

    static MPDCommand loadCommand(final String file) {
        return MPDCommand.create(MPD_CMD_PLAYLIST_LOAD, file);
    }

    /**
     * Adds a music to playlist.
     *
     * @param entry music/directory/playlist to be added.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void add(final FilesystemTreeEntry entry) throws IOException, MPDException {
        mConnection.submit(addCommand(entry.getFullPath()));
    }

    /**
     * Adds a {@code collection} of {@code Music} to playlist.
     *
     * @param collection {@code collection} of {@code Music} to be added to playlist.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     * @see Music
     */
    public void addAll(final Iterable<Music> collection) throws IOException, MPDException {
        mConnection.submit(addAllCommand(collection));
    }

    /**
     * Adds a stream to playlist.
     *
     * @param url stream URL
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void addStream(final String url) throws IOException, MPDException {
        mConnection.submit(MPD_CMD_PLAYLIST_ADD, url);
    }

    /**
     * Clears playlist content.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void clear() throws IOException, MPDException {
        mConnection.submit(clearCommand());
    }

    /**
     * Retrieves music by the playlist id. Operates on local copy of playlist, may not reflect
     * server's current playlist.
     *
     * @param id The playlist id to get the {@link Music} entry for.
     * @return The {@link Music} entry.
     */
    public Music getById(final int id) {
        return mList.getById(id);
    }

    /**
     * Retrieves music at position index in playlist. Operates on local copy of playlist, may not
     * reflect server's current playlist.
     *
     * Use of this method is discouraged, use {@link #getById(int)} to prevent races.
     *
     * @param index position.
     * @return music at position index.
     */
    public Music getByIndex(final int index) {
        return mList.getByIndex(index);
    }

    /**
     * This replaces the entire {@code MusicList} with a full playlist response from the media
     * server.
     *
     * @return A {@code Collection} of all items in the playlist queue.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    private Collection<Music> getFullPlaylist() throws IOException, MPDException {
        final CommandResult result = mConnection.submit(MPD_CMD_PLAYLIST_LIST).get();
        return new MusicResponse(result);
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
     * Invalidate this {@code MPDPlaylist}.
     */
    public void invalidate() {
        mList.clear();
        mPlaylistValidity.drainPermits();
    }

    /**
     * This method checks the playlist for validation.
     *
     * @return True if the playlist is valid, false otherwise.
     */
    public boolean isValid() {
        final boolean isAcquired = mPlaylistValidity.tryAcquire();

        if (isAcquired) {
            mPlaylistValidity.release();
        }

        return isAcquired;
    }

    /**
     * Load playlist file.
     *
     * @param file playlist filename without .m3u extension.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void load(final String file) throws IOException, MPDException {
        mConnection.submit(loadCommand(file));
    }

    /**
     * Moves song with specified id to position {@code to}.
     *
     * @param songId Id of the song to be moved.
     * @param to     target position of the song to be moved.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void move(final int songId, final int to) throws IOException, MPDException {
        mConnection.submit(MPD_CMD_PLAYLIST_MOVE_ID, Integer.toString(songId),
                Integer.toString(to));
    }

    /**
     * Moves song at position {@code from} to position {@code to}.
     *
     * @param from current position of the song to be moved.
     * @param to   target position of the song to be moved.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     * @see #move(int, int)
     */
    public void moveByPosition(final int from, final int to) throws IOException, MPDException {
        mConnection.submit(MPD_CMD_PLAYLIST_MOVE, Integer.toString(from),
                Integer.toString(to));
    }

    /**
     * Moves {@code number} songs starting at position {@code start} to position {@code to}.
     *
     * @param start  current position of the first of the songs to be moved.
     * @param number number of songs to be moved.
     * @param to     first target position of the songs to be moved to.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     * @see #moveByPosition(int, int)
     */
    public void moveByPosition(final int start, final int number, final int to)
            throws IOException, MPDException {
        if (start != to && number > 0) {
            final String beginRange = Integer.toString(start);
            final String endRange = Integer.toString(start + number);
            final String target = Integer.toString(to);
            mConnection
                    .submit(MPD_CMD_PLAYLIST_MOVE, beginRange + ':' + endRange, target);
        }
    }

    /**
     * Removes album of given ID from playlist. Album and artist name must be available for this
     * method.
     *
     * @param songId entries positions.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     * @see #removeById(int[])
     */
    public void removeAlbumById(final int songId) throws IOException, MPDException {
        final List<Integer> matchingPositions = new ArrayList<>();
        final Music currentTrack = mList.getById(songId);
        final String albumName = currentTrack.getAlbumName();
        final boolean usingAlbumArtist;
        String artistName = currentTrack.getAlbumArtistName();

        if (artistName == null || artistName.isEmpty()) {
            artistName = currentTrack.getArtistName();
            usingAlbumArtist = false;
        } else {
            usingAlbumArtist = true;
        }

        if (albumName == null && artistName == null) {
            throw new IllegalStateException(
                    "Cannot call removeAlbumById() without either a artist name or album name.");
        }

        if (DEBUG) {
            Log.debug(TAG, "Remove album " + albumName + " of " + artistName);
        }

        /** Don't allow the list to change before we've computed the CommandList. */
        synchronized (mList) {
            for (final Music track : mList) {
                if (Tools.equals(albumName, track.getAlbumName())) {
                    final boolean songIsAlbumArtist = usingAlbumArtist &&
                            Tools.equals(artistName, track.getAlbumArtistName());
                    final boolean songIsArtist = !usingAlbumArtist &&
                            Tools.equals(artistName, track.getArtistName());

                    if (songIsArtist || songIsAlbumArtist) {
                        matchingPositions.add(track.getPos());
                    }
                }
            }
        }

        removeByIndex(matchingPositions);
        if (DEBUG) {
            Log.debug(TAG, "Removed " + (matchingPositions.size() + 1) + " songs");
        }
    }

    /**
     * Removes entries from playlist.
     *
     * @param songIds entries IDs.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void removeById(final int... songIds) throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue(songIds.length);

        for (final int id : songIds) {
            commandQueue.add(MPD_CMD_PLAYLIST_REMOVE_ID, Integer.toString(id));
        }
        mConnection.submit(commandQueue);
    }

    /**
     * Removes entries from playlist.
     *
     * @param songIds Playlist songIDs to remove.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void removeById(final Collection<Integer> songIds) throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue(songIds.size());

        for (final Integer id : songIds) {
            commandQueue.add(MPD_CMD_PLAYLIST_REMOVE_ID, id.toString());
        }

        mConnection.submit(commandQueue);
    }

    /**
     * Removes entries from playlist.
     *
     * @param songs entries positions.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    void removeByIndex(final int... songs) throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue();
        if (mConnection.isProtocolVersionSupported(0, 16)) {
            commandQueue.add(MPD_CMD_PLAYLIST_REMOVE, Tools.sequentialToRange(songs));
        } else {
            Arrays.sort(songs);

            for (int i = songs.length - 1; i >= 0; i--) {
                commandQueue.add(MPD_CMD_PLAYLIST_REMOVE, Integer.toString(i));
            }
        }
        mConnection.submit(commandQueue);
    }

    /**
     * Removes entries from playlist.
     *
     * @param songs entries positions.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    void removeByIndex(final List<Integer> songs) throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue();

        if (mConnection.isProtocolVersionSupported(0, 16)) {
            commandQueue.add(MPD_CMD_PLAYLIST_REMOVE, Tools.sequentialToRange(songs));
        } else {
            Collections.sort(songs, Collections.reverseOrder());
            for (final Integer song : songs) {
                commandQueue.add(MPD_CMD_PLAYLIST_REMOVE, song.toString());
            }
        }

        mConnection.submit(commandQueue);
    }

    /**
     * Removes playlist file.
     *
     * @param file playlist filename without .m3u extension.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void removePlaylist(final String file) throws IOException, MPDException {
        mConnection.submit(MPD_CMD_PLAYLIST_DELETE, file);
    }

    /**
     * Save playlist file.
     *
     * @param file playlist filename without .m3u extension.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void savePlaylist(final String file) throws IOException, MPDException {
        // If the playlist already exists, save will fail. So, just remove it first!
        try {
            removePlaylist(file);
        } catch (final MPDException ignored) {
            /** We're removing it just in case it exists. */
        }
        mConnection.submit(MPD_CMD_PLAYLIST_SAVE, file);
    }

    /**
     * Shuffles playlist content.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void shuffle() throws IOException, MPDException {
        mConnection.submit(MPD_CMD_PLAYLIST_SHUFFLE);
    }

    /**
     * Retrieves playlist size. Operates on local copy of playlist, may not reflect server's
     * current playlist.
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
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void swap(final int song1Id, final int song2Id) throws IOException, MPDException {
        mConnection.submit(MPD_CMD_PLAYLIST_SWAP_ID,
                Integer.toString(song1Id), Integer.toString(song2Id));
    }

    /**
     * Swap positions of song1 and song2.
     *
     * @param song1 position of song1 in playlist.
     * @param song2 position of song2 in playlist
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     * @see #swap(int, int)
     */
    public void swapByPosition(final int song1, final int song2) throws IOException, MPDException {
        mConnection.submit(MPD_CMD_PLAYLIST_SWAP, Integer.toString(song1),
                Integer.toString(song2));
    }

    /**
     * Retrieves a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder();
        synchronized (mList) {
            for (final Music music : mList) {
                stringBuilder.append(music);
                stringBuilder.append(MPDCommand.MPD_CMD_NEWLINE);
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Reloads the playlist content. This is the only place the {@link MusicList} should be
     * modified, don't call this method unless you know exactly what you're doing.
     *
     * @param mpdStatus A current {@code MPDStatus} object.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void update(final MPDStatus mpdStatus) throws IOException, MPDException {
        /** Synchronize this block to make sure the playlist version stays coherent. */
        synchronized (mList) {
            final int newPlaylistVersion = mpdStatus.getPlaylistVersion();

            if (mLastPlaylistVersion == MPDStatusMap.DEFAULT_INTEGER || mList.size() == 0) {
                mList.replace(getFullPlaylist());

                if (mPlaylistValidity.availablePermits() == 0) {
                    mPlaylistValidity.release();
                }
            } else if (mLastPlaylistVersion != newPlaylistVersion) {
                final CommandResult result =
                        mConnection.submit(MPD_CMD_PLAYLIST_CHANGES,
                                Integer.toString(mLastPlaylistVersion)).get();
                final Iterable<Music> changes = new MusicResponse(result);

                try {
                    mList.manipulate(changes, mpdStatus.getPlaylistLength());
                } catch (final IllegalStateException e) {
                    Log.error(TAG, "Partial update failed, running full update.", e);
                    mList.replace(getFullPlaylist());
                }
            }

            mLastPlaylistVersion = newPlaylistVersion;
        }
    }

    /**
     * Blocks indefinitely until this object is valid.
     *
     * @throws InterruptedException If the current thread is {@link Thread#interrupted()}.
     */
    public void waitForValidity() throws InterruptedException {
        Tools.waitForValidity(mPlaylistValidity);
    }

    /**
     * Blocks for the given waiting time.
     *
     * @param timeout The time to wait for a valid object.
     * @param unit    The time unit of the {@code timeout} argument.
     * @return {@code true} if a the {@code ResponseMap} was valid by the time of return, false
     * otherwise.
     * @throws InterruptedException If the current thread is {@link Thread#interrupted()}.
     */
    public boolean waitForValidity(final long timeout, final TimeUnit unit)
            throws InterruptedException {
        return Tools.waitForValidity(mPlaylistValidity, timeout, unit);
    }
}
