/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2015 The MPDroid Project
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

import com.anpmech.mpd.connection.MPDConnection;
import com.anpmech.mpd.connection.MPDConnectionMonoSocket;
import com.anpmech.mpd.connection.MPDConnectionMultiSocket;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Album;
import com.anpmech.mpd.item.AlbumBuilder;
import com.anpmech.mpd.item.Artist;
import com.anpmech.mpd.item.Directory;
import com.anpmech.mpd.item.FilesystemTreeEntry;
import com.anpmech.mpd.item.Genre;
import com.anpmech.mpd.item.Item;
import com.anpmech.mpd.item.Music;
import com.anpmech.mpd.item.MusicBuilder;
import com.anpmech.mpd.item.PlaylistFile;
import com.anpmech.mpd.item.Stream;
import com.anpmech.mpd.subsystem.Sticker;
import com.anpmech.mpd.subsystem.status.MPDStatisticsMap;
import com.anpmech.mpd.subsystem.status.MPDStatusMap;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import static com.anpmech.mpd.Tools.KEY;
import static com.anpmech.mpd.Tools.VALUE;

/**
 * MPD Server controller.
 */
public class MPD {

    public static final String STREAMS_PLAYLIST = "[Radio Streams]";

    private static final String TAG = "MPD";

    protected final MPDPlaylist mPlaylist;

    private final MPDConnection mConnection;

    private final MPDConnection mIdleConnection;

    private final MPDStatisticsMap mStatistics;

    private final MPDStatusMap mStatus;

    /**
     * Constructs a new MPD server controller without connection.
     */
    public MPD() {
        super();
        mConnection = new MPDConnectionMultiSocket(5000, 2);
        mIdleConnection = new MPDConnectionMonoSocket(0);

        mPlaylist = new MPDPlaylist(mConnection);
        mStatistics = new MPDStatisticsMap(mConnection);
        mStatus = new MPDStatusMap(mConnection);
    }

    /**
     * Constructs a new MPD server controller.
     *
     * @param server server address or host name
     * @param port   server port
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public MPD(final InetAddress server, final int port, final String password)
            throws MPDException, IOException {
        this();
        connect(server, port, password);
    }

    /**
     * Constructs a new MPD server controller.
     *
     * @param server server address or host name
     * @param port   server port
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public MPD(final String server, final int port, final String password)
            throws IOException, MPDException {
        this();
        connect(server, port, password);
    }

    private static String[] getAlbumArtistPair(final Album album) {
        final Artist artist = album.getArtist();
        final String[] artistPair;

        if (artist == null) {
            artistPair = new String[]{null, null};
        } else {
            if (album.hasAlbumArtist()) {
                artistPair = new String[]{Music.TAG_ALBUM_ARTIST, artist.getName()};
            } else {
                artistPair = new String[]{Music.TAG_ARTIST, artist.getName()};
            }
        }

        return artistPair;
    }

    private static MPDCommand getAlbumDetailsCommand(final Album album) {
        final String[] artistPair = getAlbumArtistPair(album);

        return MPDCommand.create(MPDCommand.MPD_CMD_COUNT,
                Music.TAG_ALBUM, album.getName(),
                artistPair[0], artistPair[1]);
    }

    private static MPDCommand getSongsCommand(final Album album) {
        final String[] artistPair = getAlbumArtistPair(album);

        return MPDCommand.create(MPDCommand.MPD_CMD_FIND, Music.TAG_ALBUM, album.getName(),
                artistPair[0], artistPair[1]);
    }

    /*
     * get raw command String for listAlbums
     */
    private static MPDCommand listAlbumsCommand(final String artist, final boolean useAlbumArtist) {
        String albumArtist = null;

        if (artist != null) {
            if (useAlbumArtist) {
                albumArtist = Music.TAG_ALBUM_ARTIST;
            } else {
                albumArtist = Music.TAG_ARTIST;
            }
        }

        return MPDCommand.create(MPDCommand.MPD_CMD_LIST_TAG, Music.TAG_ALBUM,
                albumArtist, artist);
    }

    /*
     * get raw command String for listAllAlbumsGrouped
     */
    private static MPDCommand listAllAlbumsGroupedCommand(final boolean useAlbumArtist) {
        final String artistTag;

        if (useAlbumArtist) {
            artistTag = Music.TAG_ALBUM_ARTIST;
        } else {
            artistTag = Music.TAG_ARTIST;
        }

        return MPDCommand.create(MPDCommand.MPD_CMD_LIST_TAG, Music.TAG_ALBUM,
                MPDCommand.MPD_CMD_GROUP, artistTag);
    }

    private static MPDCommand nextCommand() {
        return MPDCommand.create(MPDCommand.MPD_CMD_NEXT);
    }

    private static MPDCommand skipToPositionCommand(final int position) {
        return MPDCommand.create(MPDCommand.MPD_CMD_PLAY, Integer.toString(position));
    }

    private static void sort(final List<String> list, final boolean sensitive) {
        if (sensitive) {
            Collections.sort(list);
        } else {
            Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
        }
    }

    /**
     * Adds a {@code Album} item object to the playlist queue.
     *
     * @param album {@code Album} item object to be added to the media server playlist queue.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void add(final Album album) throws IOException, MPDException {
        add(album, false, false);
    }

    /**
     * Adds a {@code Album} item object to the playlist queue.
     *
     * @param album   {@code Album} item object to be added to the media server playlist queue.
     * @param replace Whether to clear the playlist queue prior to adding the item(s).
     * @param play    Whether to play the playlist queue after adding the item(s).
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void add(final Album album, final boolean replace, final boolean play)
            throws IOException, MPDException {
        final CommandQueue commandQueue;

        if (isCommandAvailable(MPDCommand.MPD_CMD_FIND_ADD)) {
            final String[] artistPair = getAlbumArtistPair(album);

            commandQueue = new CommandQueue();

            commandQueue
                    .add(MPDCommand.MPD_CMD_FIND_ADD, Music.TAG_ALBUM, album.getName(),
                            artistPair[0], artistPair[1]);
        } else {
            final List<Music> songs = getSongs(album);
            commandQueue = MPDPlaylist.addAllCommand(songs);
        }

        add(commandQueue, replace, play);
    }

    /**
     * Adds a {@code Artist} item object to the playlist queue.
     *
     * @param artist {@code Artist} item object to be added to the media server playlist queue.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void add(final Artist artist) throws IOException, MPDException {
        add(artist, false, false);
    }

    /**
     * Adds a {@code Artist} item object to the playlist queue.
     *
     * @param artist  {@code Artist} item object to be added to the media server playlist queue.
     * @param replace Whether to clear the playlist queue prior to adding the item(s).
     * @param play    Whether to play the playlist queue after adding the item(s).
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void add(final Artist artist, final boolean replace, final boolean play)
            throws IOException, MPDException {
        final CommandQueue commandQueue;

        if (isCommandAvailable(MPDCommand.MPD_CMD_FIND_ADD)) {
            commandQueue = new CommandQueue();

            commandQueue
                    .add(MPDCommand.MPD_CMD_FIND_ADD, Music.TAG_ARTIST, artist.getName());
        } else {
            final List<Music> songs = getSongs(artist);
            commandQueue = MPDPlaylist.addAllCommand(songs);
        }

        add(commandQueue, replace, play);
    }

    /**
     * Add a {@code Music} or {@code Directory} item object to the playlist queue. {@code
     * PlaylistFile} items are added in it's own method.
     *
     * @param music   {@code Music} item object to be added to the media server playlist queue.
     * @param replace Whether to clear the playlist queue prior to adding the item(s).
     * @param play    Whether to play the playlist queue after adding the item(s).
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void add(final FilesystemTreeEntry music, final boolean replace, final boolean play)
            throws IOException, MPDException {
        if (music instanceof PlaylistFile) {
            add((PlaylistFile) music, replace, play);
        } else {
            final CommandQueue commandQueue = new CommandQueue();

            commandQueue.add(MPDPlaylist.addCommand(music.getFullPath()));

            add(commandQueue, replace, play);
        }
    }

    /**
     * Add a {@code Music} item object to the playlist queue.
     *
     * @param music {@code Music} item object to be added to the playlist queue.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void add(final FilesystemTreeEntry music) throws IOException, MPDException {
        add(music, false, false);
    }

    public void add(final Genre genre, final boolean replace, final boolean play)
            throws IOException, MPDException {
        final CommandQueue commandQueue;

        if (isCommandAvailable(MPDCommand.MPD_CMD_FIND_ADD)) {
            commandQueue = new CommandQueue();

            commandQueue
                    .add(MPDCommand.MPD_CMD_FIND_ADD, Music.TAG_GENRE, genre.getName());
        } else {
            final Collection<Music> music = find(Music.TAG_GENRE, genre.getName());

            commandQueue = MPDPlaylist.addAllCommand(music);
        }

        add(commandQueue, replace, play);
    }

    /**
     * Adds songs to the queue. Optionally, clears the queue prior to the addition. Optionally,
     * play the added songs afterward.
     *
     * @param commandQueue The commandQueue that will be responsible of inserting the songs into
     *                     the queue.
     * @param replace      If true, replaces the entire playlist queue with the added files.
     * @param playAfterAdd If true, starts playing once added.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    private void add(final CommandQueue commandQueue, final boolean replace,
            final boolean playAfterAdd) throws IOException, MPDException {
        int playPos = 0;
        final boolean isPlaying = mStatus.isState(MPDStatusMap.STATE_PLAYING);
        final boolean isConsume = mStatus.isConsume();
        final boolean isRandom = mStatus.isRandom();
        final int playlistLength = mStatus.getPlaylistLength();

        /** Replace */
        if (replace) {
            if (isPlaying) {
                if (playlistLength > 1) {
                    try {
                        commandQueue.addAll(0, MPDPlaylist.cropCommand(this));
                    } catch (final IllegalStateException ignored) {
                        /** Shouldn't occur, we already checked for playing. */
                    }
                }
            } else {
                commandQueue.add(0, MPDPlaylist.clearCommand());
            }
        } else if (playAfterAdd && !isRandom) {
            /** Since we didn't clear the playlist queue, we need to play the (current queue+1) */
            playPos = mPlaylist.size();
        }

        if (replace) {
            if (isPlaying) {
                commandQueue.add(nextCommand());
            } else if (playAfterAdd) {
                commandQueue.add(skipToPositionCommand(playPos));
            }
        } else if (playAfterAdd) {
            commandQueue.add(skipToPositionCommand(playPos));
        }

        /** Finally, clean up the last playing song. */
        if (replace && isPlaying && !isConsume) {
            commandQueue.add(MPDPlaylist.MPD_CMD_PLAYLIST_REMOVE, "0");

        }

        /**
         * It's rare, but possible to make it through the add()
         * methods without adding to the command queue.
         */
        if (!commandQueue.isEmpty()) {
            mConnection.send(commandQueue);
        }
    }

    /**
     * Add a {@code Playlist} item object to the playlist queue.
     *
     * @param databasePlaylist A playlist item stored on the media server to add to the playlist
     *                         queue.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void add(final PlaylistFile databasePlaylist) throws IOException, MPDException {
        add(databasePlaylist, false, false);
    }

    /**
     * Add a {@code Playlist} item object to the playlist queue.
     *
     * @param databasePlaylist A playlist item stored on the media server to add to the playlist
     *                         queue.
     * @param replace          Whether to clear the playlist queue prior to adding the
     *                         databasePlaylist string.
     * @param play             Whether to play the playlist queue prior after adding the
     *                         databasePlaylist string.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void add(final PlaylistFile databasePlaylist, final boolean replace,
            final boolean play)
            throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue();

        commandQueue.add(MPDPlaylist.loadCommand(databasePlaylist.getName()));

        add(commandQueue, replace, play);
    }

    protected void addAlbumDetails(final List<Album> albums)
            throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue(albums.size());
        for (final Album album : albums) {
            commandQueue.add(getAlbumDetailsCommand(album));
        }
        final List<List<String>> response = mConnection.sendSeparated(commandQueue);

        if (response.size() == albums.size()) {
            final AlbumBuilder albumBuilder = new AlbumBuilder();

            for (int i = 0; i < response.size(); i++) {
                final List<String> list = response.get(i);
                final Album album = albums.get(i);
                long duration = 0L;
                long songCount = 0L;

                albumBuilder.setAlbum(albums.get(i));

                /** First, extract the album specifics from the response. */
                for (final String[] pair : Tools.splitResponse(list)) {
                    if ("songs".equals(pair[KEY])) {
                        songCount = Long.parseLong(pair[VALUE]);
                    } else if ("playtime".equals(pair[KEY])) {
                        duration = Long.parseLong(pair[VALUE]);
                    }
                }

                albumBuilder.setAlbumDetails(songCount, duration);

                /** Then extract the date and path from a song of the album. */
                final List<Music> songs = getFirstTrack(album);
                if (!songs.isEmpty()) {
                    albumBuilder.setSongDetails(songs.get(0).getDate(),
                            songs.get(0).getParentDirectory());
                }
                albums.set(i, albumBuilder.build());
            }
        }
    }

    protected void addAlbumSongDetails(final List<Album> albums) throws IOException, MPDException {
        if (!albums.isEmpty()) {
            final ListIterator<Album> iterator = albums.listIterator();
            final AlbumBuilder albumBuilder = new AlbumBuilder();

            while (iterator.hasNext()) {
                final Album album = iterator.next();
                final List<Music> songs = getFirstTrack(album);

                if (!songs.isEmpty()) {
                    albumBuilder.setAlbum(album);
                    albumBuilder.setSongDetails(songs.get(0).getDate(),
                            songs.get(0).getParentDirectory());
                    iterator.set(albumBuilder.build());
                }
            }
        }
    }

    /** TODO: This needs to be an add(Stream, ...) method. */
    public void addStream(final String stream, final boolean replace, final boolean play)
            throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue();
        commandQueue.add(MPDPlaylist.addCommand(stream));

        add(commandQueue, replace, play);
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public void addToPlaylist(final PlaylistFile playlist, final Album album)
            throws IOException, MPDException {
        if (mIdleConnection.isCommandAvailable(MPDCommand.MPD_CMD_SEARCH_ADD_PLAYLIST)) {
            final String[] artistPair = getAlbumArtistPair(album);

            mConnection.send(MPDCommand.MPD_CMD_SEARCH_ADD_PLAYLIST, playlist.getFullPath(),
                    Music.TAG_ALBUM, album.getName(), artistPair[0], artistPair[1]);
        } else {
            addToPlaylist(playlist, getSongs(album));
        }
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public void addToPlaylist(final PlaylistFile playlist, final Artist artist)
            throws IOException, MPDException {
        if (mIdleConnection.isCommandAvailable(MPDCommand.MPD_CMD_SEARCH_ADD_PLAYLIST)) {
            mConnection.send(MPDCommand.MPD_CMD_SEARCH_ADD_PLAYLIST, playlist.getFullPath(),
                    Music.TAG_ARTIST, artist.getName());
        } else {
            addToPlaylist(playlist, getSongs(artist));
        }
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public void addToPlaylist(final PlaylistFile playlist, final Collection<Music> musicCollection)
            throws IOException, MPDException {
        if (!musicCollection.isEmpty()) {
            final CommandQueue commandQueue = new CommandQueue();

            for (final Music music : musicCollection) {
                commandQueue
                        .add(MPDCommand.MPD_CMD_PLAYLIST_ADD, playlist.getFullPath(),
                                music.getFullPath());
            }
            mConnection.send(commandQueue);
        }
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public void addToPlaylist(final PlaylistFile playlist, final FilesystemTreeEntry entry)
            throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_PLAYLIST_ADD, playlist.getFullPath(),
                entry.getFullPath());
    }

    public void addToPlaylist(final PlaylistFile playlist, final Genre genre)
            throws IOException, MPDException {
        if (mIdleConnection.isCommandAvailable(MPDCommand.MPD_CMD_SEARCH_ADD_PLAYLIST)) {
            mConnection.send(MPDCommand.MPD_CMD_SEARCH_ADD_PLAYLIST, playlist.getFullPath(),
                    Music.TAG_GENRE, genre.getName());
        } else {
            final Collection<Music> music = find(Music.TAG_GENRE, genre.getName());

            addToPlaylist(playlist, music);
        }
    }

    public void addToPlaylist(final PlaylistFile playlist, final Music music)
            throws IOException, MPDException {
        addToPlaylist(playlist, Collections.singletonList(music));
    }

    /**
     * Increases or decreases volume by {@code modifier} amount.
     *
     * @param modifier volume adjustment
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void adjustVolume(final int modifier) throws IOException, MPDException {
        // calculate final volume (clip value with [0, 100])
        int vol = mStatus.getVolume() + modifier;
        vol = Math.max(MPDStatusMap.VOLUME_MIN, Math.min(MPDStatusMap.VOLUME_MAX, vol));

        mConnection.send(MPDCommand.MPD_CMD_SET_VOLUME, Integer.toString(vol));
    }

    /**
     * Clears error message.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void clearError() throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_CLEARERROR);
    }

    /**
     * Connects to the default MPD server.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public synchronized void connect() throws IOException, MPDException {
        mConnection.connect();
        mIdleConnection.connect();
    }

    /**
     * Connects to a MPD server.
     *
     * @param server server address or host name
     * @param port   server port
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public final synchronized void connect(final InetAddress server, final int port,
            final String password) throws IOException, MPDException {
        if (!isConnected()) {
            mConnection.connect(server, port, password);
            mIdleConnection.connect(server, port, password);
        }
    }

    /**
     * Connects to a MPD server.
     *
     * @param server server address or host name
     * @param port   server port
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public final void connect(final String server, final int port, final String password)
            throws IOException, MPDException {
        final InetAddress address = InetAddress.getByName(server);
        connect(address, port, password);
    }

    /**
     * Connects to a MPD server.
     *
     * @param server server address or host name and port (server:port)
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public final void connect(final String server, final String password) throws IOException,
            MPDException {
        int port = MPDCommand.DEFAULT_MPD_PORT;
        final String host;
        if (server.indexOf(':') == -1) {
            host = server;
        } else {
            host = server.substring(0, server.lastIndexOf(':'));
            port = Integer.parseInt(server.substring(server.lastIndexOf(':') + 1));
        }
        connect(host, port, password);
    }

    public void disableOutput(final int id) throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_OUTPUTDISABLE, Integer.toString(id));
    }

    /**
     * Disconnects from server.
     *
     * @throws IOException if an error occur while closing connection
     */
    public synchronized void disconnect() throws IOException {
        IOException ex = null;
        if (mConnection != null && mConnection.isConnected()) {
            try {
                mConnection.disconnect();
            } catch (final IOException e) {
                ex = (ex != null) ? ex : e;// Always keep first non null
                // exception
            }
        }
        if (mIdleConnection != null && mIdleConnection.isConnected()) {
            try {
                mIdleConnection.disconnect();
            } catch (final IOException e) {
                ex = (ex != null) ? ex : e;// Always keep non null first
                // exception
            }
        }
    }

    public void editSavedStream(final String url, final String name, final Integer pos)
            throws IOException, MPDException {
        removeSavedStream(pos);
        saveStream(url, name);
    }

    public void enableOutput(final int id) throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_OUTPUTENABLE, Integer.toString(id));
    }

    /**
     * Similar to {@code search},{@code find} looks for exact matches in the MPD database.
     *
     * @param type          type of search. Should be one of the following constants:
     *                      MPD_FIND_ARTIST, MPD_FIND_ALBUM
     * @param locatorString case-insensitive locator locatorString. Anything that exactly matches
     *                      {@code locatorString} will be returned in the results.
     * @return a Collection of {@code Music}
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     * @see com.anpmech.mpd.item.Music
     */
    public Collection<Music> find(final String type, final String locatorString)
            throws IOException, MPDException {
        return genericSearch(MPDCommand.MPD_CMD_FIND, type, locatorString,true);
    }

    public List<Music> find(final String[] args) throws IOException, MPDException {
        return genericSearch(MPDCommand.MPD_CMD_FIND, args, true);
    }

    /*
     * For all given albums, look for album artists and create as many albums as
     * there are album artists, including "" The server call can be slow for long
     * album lists
     */
    protected void fixAlbumArtists(final List<Album> albums) throws IOException, MPDException {
        if (!albums.isEmpty()) {
            final List<List<String>> albumArtists = listAlbumArtists(albums);

            if (albumArtists.size() == albums.size()) {
                /** Split albums are rare, let it allocate as needed. */
                @SuppressWarnings("CollectionWithoutInitialCapacity")
                final Collection<Album> splitAlbums = new ArrayList<>();
                final AlbumBuilder albumBuilder = new AlbumBuilder();
                int i = 0;
                for (final Album album : albums) {
                    final List<String> aartists = albumArtists.get(i);
                    final int aartistsSize = aartists.size();
                    String firstArtist;

                    if (aartistsSize > 0) {
                        albumBuilder.setAlbum(album);
                        Collections.sort(aartists); // make sure "" is the first one
                        firstArtist = aartists.get(0);

                        if (!firstArtist.isEmpty()) {
                            // album
                            albumBuilder.setAlbumArtist(firstArtist);
                            albums.set(i, albumBuilder.build(false));
                        } // do nothing if albumartist is ""

                        if (aartists.size() > 1) { // it's more than one album, insert
                            final ListIterator<String> iterator = aartists.listIterator(1);

                            while (iterator.hasNext()) {
                                albumBuilder.setAlbumArtist(iterator.next());
                                splitAlbums.add(albumBuilder.build(false));
                            }
                        }
                    }
                    i++;
                }
                albums.addAll(splitAlbums);
            }
        }
    }

    protected List<Music> genericSearch(final String searchCommand, final String[] args,
            final boolean sort) throws IOException, MPDException {
        return MusicBuilder.buildMusicFromList(mConnection.send(searchCommand, args), sort);
    }

    protected List<Music> genericSearch(final String searchCommand, final String type,
            final String strToFind, final boolean sort) throws IOException, MPDException {
        final List<String> response = mConnection.send(searchCommand, type, strToFind);

        return MusicBuilder.buildMusicFromList(response, sort);
    }

    public int getAlbumCount(final Artist artist, final boolean useAlbumArtistTag)
            throws IOException, MPDException {
        return listAlbums(artist.getName(), useAlbumArtistTag).size();
    }

    public int getAlbumCount(final String artist, final boolean useAlbumArtistTag)
            throws IOException, MPDException {
        return listAlbums(artist, useAlbumArtistTag).size();
    }

    public List<Album> getAlbums(final Artist artist, final boolean sortByYear,
            final boolean trackCountNeeded) throws IOException, MPDException {
        List<Album> albums = getAlbums(artist, sortByYear, trackCountNeeded, false);

        // 1. the null artist list already contains all albums
        // 2. the "unknown artist" should not list unknown album artists
        if (artist != null && !artist.isUnknown()) {
            albums = Item.merged(getAlbums(artist, sortByYear, trackCountNeeded, true), albums);
        }

        return albums;
    }

    public List<Album> getAlbums(final Artist artist, final boolean sortByYear,
            final boolean trackCountNeeded, final boolean useAlbumArtist)
            throws IOException, MPDException {
        final List<Album> albums;

        if (artist == null) {
            albums = getAllAlbums();
        } else {
            final List<String> albumNames = listAlbums(artist.getName(), useAlbumArtist);
            albums = new ArrayList<>(albumNames.size());

            if (!albumNames.isEmpty()) {
                final AlbumBuilder albumBuilder = new AlbumBuilder();

                for (final String album : albumNames) {
                    albumBuilder.setBase(album, artist, useAlbumArtist);
                    albums.add(albumBuilder.build());
                }

                if (!useAlbumArtist) {
                    fixAlbumArtists(albums);
                }

                // after fixing album artists
                if (trackCountNeeded || sortByYear) {
                    addAlbumDetails(albums);
                }

                if (!sortByYear) {
                    addAlbumSongDetails(albums);
                }

                Collections.sort(albums);
            }
        }

        return albums;
    }

    /**
     * Get all albums (if there is no artist specified for filtering)
     *
     * @return A list of all albums on the connected media server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<Album> getAllAlbums() throws IOException, MPDException {
        final List<Album> albums;
        // Use MPD 0.19's album grouping feature if available.
        if (mConnection.isProtocolVersionSupported(0, 19)) {
            albums = listAllAlbumsGrouped(false);
        } else {
            final List<String> albumNames = listAlbums();
            if (albumNames.isEmpty()) {
                albums = Collections.emptyList();
            } else {
                final AlbumBuilder albumBuilder = new AlbumBuilder();

                albums = new ArrayList<>(albumNames.size());
                for (final String album : albumNames) {
                    albumBuilder.setName(album);
                    albums.add(albumBuilder.build());
                }
            }
        }

        Collections.sort(albums);
        return albums;
    }

    public List<Artist> getArtists() throws IOException, MPDException {
        return Item.merged(getArtists(true), getArtists(false));
    }

    public List<Artist> getArtists(final boolean useAlbumArtist) throws IOException, MPDException {
        final List<String> artistNames;
        final List<Artist> artists;

        if (useAlbumArtist) {
            artistNames = listAlbumArtists();
        } else {
            artistNames = listArtists(true);
        }

        artists = new ArrayList<>(artistNames.size());
        if (!artistNames.isEmpty()) {
            for (final String artist : artistNames) {
                artists.add(new Artist(artist));
            }
        }
        Collections.sort(artists);
        return artists;
    }

    public List<Artist> getArtists(final Genre genre) throws IOException, MPDException {
        return Item.merged(getArtists(genre, true), getArtists(genre, false));
    }

    public List<Artist> getArtists(final Genre genre, final boolean useAlbumArtist)
            throws IOException, MPDException {
        final List<String> artistNames;
        final List<Artist> artists;

        if (useAlbumArtist) {
            artistNames = listAlbumArtists(genre);
        } else {
            artistNames = listArtists(genre.getName(), true);
        }

        artists = new ArrayList<>(artistNames.size());
        if (!artistNames.isEmpty()) {
            for (final String artist : artistNames) {
                artists.add(new Artist(artist));
            }
        }
        Collections.sort(artists);
        return artists;
    }

    protected List<Music> getFirstTrack(final Album album) throws IOException, MPDException {
        final Artist artist = album.getArtist();
        final String[] args = new String[6];

        if (artist == null) {
            args[0] = "";
            args[1] = "";
        } else if (album.hasAlbumArtist()) {
            args[0] = Music.TAG_ALBUM_ARTIST;
        } else {
            args[0] = Music.TAG_ARTIST;
        }

        if (artist != null) {
            args[1] = artist.getName();
        }

        args[2] = Music.TAG_ALBUM;
        args[3] = album.getName();
        args[4] = Music.TAG_TRACK;
        args[5] = "1";
        List<Music> songs = find(args);
        if (songs.isEmpty()) {
            args[5] = "01";
            songs = find(args);
        }
        if (songs.isEmpty()) {
            args[5] = "1";
            songs = search(args);
        }
        if (songs.isEmpty()) {
            final String[] args2 = Arrays.copyOf(args, 4); // find all tracks
            songs = find(args2);
        }
        return songs;
    }

    public List<Genre> getGenres() throws IOException, MPDException {
        final List<String> genreNames = listGenres();
        List<Genre> genres;

        if (genreNames.isEmpty()) {
            genres = Collections.emptyList();
        } else {
            genres = new ArrayList<>(genreNames.size());

            for (final String genre : genreNames) {
                genres.add(new Genre(genre));
            }

            Collections.sort(genres);
        }

        return genres;
    }

    public InetAddress getHostAddress() {
        return mConnection.getHostAddress();
    }

    public int getHostPort() {
        return mConnection.getHostPort();
    }

    public MPDConnection getIdleConnection() {
        return mIdleConnection;
    }

    /**
     * Returns MPD server version.
     *
     * @return MPD Server version.
     */
    public String getMpdVersion() {
        final int[] version = mIdleConnection.getMPDVersion();

        final StringBuilder sb = new StringBuilder(version.length);
        for (int i = 0; i < version.length; i++) {
            sb.append(version[i]);
            if (i < version.length - 1) {
                sb.append('.');
            }
        }
        return sb.toString();
    }

    /**
     * Returns the available outputs
     *
     * @return List of available outputs
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public Collection<MPDOutput> getOutputs() throws IOException, MPDException {
        final List<String> response = mConnection.send(MPDCommand.MPD_CMD_OUTPUTS);

        return MPDOutput.buildOutputsFromList(response);
    }

    /**
     * Retrieves {@code playlist}.
     *
     * @return playlist.
     */
    public MPDPlaylist getPlaylist() {
        return mPlaylist;
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public List<Music> getPlaylistSongs(final PlaylistFile playlist)
            throws IOException, MPDException {
        final String[] args = new String[1];
        args[0] = playlist.getFullPath();

        return genericSearch(MPDCommand.MPD_CMD_PLAYLIST_INFO, args, false);
    }

    /**
     * Returns a list of all available playlists
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<PlaylistFile> getPlaylists() throws IOException, MPDException {
        return getPlaylists(false);
    }

    /**
     * Returns a list of all available playlists
     *
     * @param sort whether the return list should be sorted
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<PlaylistFile> getPlaylists(final boolean sort)
            throws IOException, MPDException {
        final List<String> response = mConnection.send(MPDCommand.MPD_CMD_LISTPLAYLISTS);
        final List<PlaylistFile> result = new ArrayList<>(response.size());
        for (final String[] pair : Tools.splitResponse(response)) {
            if ("playlist".equals(pair[KEY])) {
                if (null != pair[VALUE] && !STREAMS_PLAYLIST.equals(pair[VALUE])) {
                    result.add(new PlaylistFile(pair[VALUE]));
                }
            }
        }
        if (sort) {
            Collections.sort(result);
        }

        return result;
    }

    public List<Music> getSavedStreams() throws IOException, MPDException {
        final List<String> response = mConnection.send(MPDCommand.MPD_CMD_LISTPLAYLISTS);
        List<Music> savedStreams = Collections.emptyList();

        for (final String[] pair : Tools.splitResponse(response)) {
            if ("playlist".equals(pair[KEY])) {
                if (STREAMS_PLAYLIST.equals(pair[VALUE])) {
                    final String[] args = {pair[VALUE]};

                    savedStreams = genericSearch(MPDCommand.MPD_CMD_PLAYLIST_INFO, args, false);
                    break;
                }
            }
        }

        return savedStreams;
    }

    public List<Music> getSongs(final Album album) throws IOException, MPDException {
        final List<Music> songs = MusicBuilder
                .buildMusicFromList(mConnection.send(getSongsCommand(album)), true);

        if (album.hasAlbumArtist()) {
            // remove songs that don't have this album artist (mpd >=0.18 puts them in)
            final Artist artist = album.getArtist();
            String artistName = null;

            if (artist != null) {
                artistName = artist.getName();
            }

            for (int i = songs.size() - 1; i >= 0; i--) {
                final String albumArtist = songs.get(i).getAlbumArtistName();
                if (albumArtist != null && !albumArtist.isEmpty()
                        && !albumArtist.equals(artistName)) {
                    songs.remove(i);
                }
            }
        }
        if (null != songs) {
            Collections.sort(songs);
        }
        return songs;
    }

    public List<Music> getSongs(final Artist artist) throws IOException, MPDException {
        final List<Album> albums = getAlbums(artist, false, false);
        final List<Music> songs = new ArrayList<>(albums.size());
        for (final Album album : albums) {
            songs.addAll(getSongs(album));
        }
        return songs;
    }

    /**
     * Retrieves the current statistics for the connected server.
     *
     * @return statistics for the connected server.
     */
    public MPDStatisticsMap getStatistics() {
        return mStatistics;
    }

    /**
     * Retrieves status of the connected server.
     *
     * @return status of the connected server.
     */
    public MPDStatusMap getStatus() {
        return mStatus;
    }

    public Sticker getStickerManager() {
        return new Sticker(mConnection);
    }

    /*
     * test whether given album is in given genre
     */
    public boolean isAlbumInGenre(final Album album, final Genre genre)
            throws IOException, MPDException {
        final List<String> response;
        final Artist artist = album.getArtist();
        String artistName = null;
        String artistType = null;

        if (artist != null) {
            artistName = artist.getName();

            if (album.hasAlbumArtist()) {
                artistType = Music.TAG_ALBUM_ARTIST;
            } else {
                artistType = Music.TAG_ARTIST;
            }
        }

        response = mConnection.send(MPDCommand.create(
                MPDCommand.MPD_CMD_LIST_TAG, Music.TAG_ALBUM,
                Music.TAG_ALBUM, album.getName(),
                artistType, artistName,
                Music.TAG_GENRE, genre.getName()));

        return !response.isEmpty();
    }

    /**
     * Checks for command validity against a list of available commands generated on connection.
     *
     * @param command A MPD protocol command.
     * @return True if the {@code command} is available for use, false otherwise.
     */
    public boolean isCommandAvailable(final String command) {
        return mConnection.isCommandAvailable(command);
    }

    /**
     * Returns true when connected and false when not connected.
     *
     * @return true when connected and false when not connected
     */
    public boolean isConnected() {
        return mIdleConnection.isConnected();
    }

    public List<String> listAlbumArtists() throws IOException, MPDException {
        return listAlbumArtists(true);
    }

    /**
     * List all album artist names from database.
     *
     * @return album artist names from database.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<String> listAlbumArtists(final boolean sortInsensitive)
            throws IOException, MPDException {
        final List<String> response = mConnection.send(MPDCommand.MPD_CMD_LIST_TAG,
                Music.TAG_ALBUM_ARTIST);

        Tools.parseResponse(response, Music.RESPONSE_ALBUM_ARTIST);
        sort(response, sortInsensitive);

        return response;
    }

    public List<String> listAlbumArtists(final Genre genre) throws IOException, MPDException {
        return listAlbumArtists(genre, true);
    }

    /**
     * List all album artist names from database.
     *
     * @return album artist names from database.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<String> listAlbumArtists(final Genre genre, final boolean sortInsensitive)
            throws IOException, MPDException {
        final List<String> response = mConnection.send(
                MPDCommand.MPD_CMD_LIST_TAG, Music.TAG_ALBUM_ARTIST,
                Music.TAG_GENRE, genre.getName());

        Tools.parseResponse(response, Music.RESPONSE_ALBUM_ARTIST);
        sort(response, sortInsensitive);

        return response;
    }

    public List<List<String>> listAlbumArtists(final List<Album> albums)
            throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue(albums.size());
        final List<List<String>> responses;

        for (final Album album : albums) {
            final Artist artist = album.getArtist();
            String artistCommand = null;
            String artistName = null;

            if (artist != null) {
                artistCommand = Music.TAG_ARTIST;
                artistName = artist.getName();
            }

            commandQueue.add(MPDCommand.MPD_CMD_LIST_TAG, Music.TAG_ALBUM_ARTIST,
                    artistCommand, artistName,
                    Music.TAG_ALBUM, album.getName());
        }

        responses = mConnection.sendSeparated(commandQueue);
        if (responses.size() == albums.size()) {
            final ListIterator<List<String>> iterator = responses.listIterator();

            while (iterator.hasNext()) {
                final List<String> response = iterator.next();
                Tools.parseResponse(response, Music.RESPONSE_ALBUM_ARTIST);
                iterator.set(response);
            }
        } else {
            Log.warning(TAG, "Response and album size differ when listing album artists.");
        }

        return responses;
    }

    /**
     * List all albums from database.
     *
     * @return {@code Collection} with all album names from database.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<String> listAlbums() throws IOException, MPDException {
        return listAlbums(null, false);
    }

    /**
     * List all albums from a given artist.
     *
     * @param artist         artist to list albums
     * @param useAlbumArtist use AlbumArtist instead of Artist
     * @return {@code Collection} with all album names from the given artist present in database.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<String> listAlbums(final String artist, final boolean useAlbumArtist)
            throws IOException, MPDException {
        final List<String> response =
                mConnection.send(listAlbumsCommand(artist, useAlbumArtist));

        Tools.parseResponse(response, Music.RESPONSE_ALBUM);
        sort(response, false);

        return response;
    }

    /**
     * List all albums grouped by Artist/AlbumArtist This method queries both Artist/AlbumArtist
     * and tries to detect if the artist is an artist or an album artist. Only the AlbumArtist
     * query will be displayed so that the list is not cluttered.
     *
     * @param includeUnknownAlbum include an entry for albums with no artists
     * @return {@code Collection} with all albums present in database, with their artist.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<Album> listAllAlbumsGrouped(final boolean includeUnknownAlbum)
            throws IOException, MPDException {
        final List<Album> artistAlbums = listAllAlbumsGrouped(false, includeUnknownAlbum);
        final List<Album> albumArtistAlbums = listAllAlbumsGrouped(true, includeUnknownAlbum);

        for (final Album artistAlbum : artistAlbums) {
            final ListIterator<Album> iterator = albumArtistAlbums.listIterator();

            while (iterator.hasNext()) {
                final Album albumArtistAlbum = iterator.next();

                if (artistAlbum.getArtist() != null &&
                        artistAlbum.doesNameExist(albumArtistAlbum)) {
                    iterator.set(artistAlbum);
                }
            }
        }

        return albumArtistAlbums;
    }

    /**
     * List all albums grouped by Artist/AlbumArtist
     *
     * @param useAlbumArtist      use AlbumArtist instead of Artist
     * @param includeUnknownAlbum include an entry for albums with no artists
     * @return {@code Collection} with all albums present in database, with their artist.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<Album> listAllAlbumsGrouped(final boolean useAlbumArtist,
            final boolean includeUnknownAlbum) throws IOException, MPDException {
        final AlbumBuilder albumBuilder = new AlbumBuilder();
        final String albumResponse = Music.RESPONSE_ALBUM;
        final String artistResponse;
        final List<String> response =
                mConnection.send(listAllAlbumsGroupedCommand(useAlbumArtist));
        final List<Album> result = new ArrayList<>(response.size() / 2);
        String currentAlbum = null;

        if (useAlbumArtist) {
            artistResponse = Music.RESPONSE_ALBUM_ARTIST;
        } else {
            artistResponse = Music.RESPONSE_ARTIST;
        }

        for (final String[] pair : Tools.splitResponse(response)) {

            if (artistResponse.equals(pair[KEY])) {
                if (currentAlbum != null) {
                    albumBuilder.setBase(currentAlbum, pair[VALUE], useAlbumArtist);
                    result.add(albumBuilder.build());

                    currentAlbum = null;
                }
            } else if (albumResponse.equals(pair[KEY])) {
                if (currentAlbum != null) {
                    albumBuilder.setName(currentAlbum);
                    /** There was no artist in this response, add the album alone */
                    result.add(albumBuilder.build());
                }

                if (!pair[VALUE].isEmpty() || includeUnknownAlbum) {
                    currentAlbum = pair[VALUE];
                } else {
                    currentAlbum = null;
                }
            }
        }

        Collections.sort(result);

        return result;
    }

    /**
     * Returns a sorted listallinfo command from the media server. Use of this command is highly
     * discouraged, as it can retrieve so much information the server max_output_buffer_size may be
     * exceeded, which will, in turn, truncate the output to this method.
     *
     * @return List of all available music information.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<Music> listAllInfo() throws IOException, MPDException {
        final List<String> allInfo = mConnection.send(MPDCommand.MPD_CMD_LISTALLINFO);

        return MusicBuilder.buildMusicFromList(allInfo, false);
    }

    /**
     * List all artist names from database.
     *
     * @return artist names from database.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<String> listArtists() throws IOException, MPDException {
        return listArtists(true);
    }

    /**
     * List all artist names from database.
     *
     * @param sortInsensitive boolean for insensitive sort when true
     * @return artist names from database.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<String> listArtists(final boolean sortInsensitive)
            throws IOException, MPDException {
        final List<String> response = mConnection.send(MPDCommand.MPD_CMD_LIST_TAG,
                Music.TAG_ARTIST);

        Tools.parseResponse(response, Music.RESPONSE_ARTIST);
        sort(response, sortInsensitive);

        return response;
    }

    /**
     * List all album artist or artist names of all given albums from database.
     *
     * @return list of array of artist names for each album.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<List<String>> listArtists(final List<Album> albums, final boolean useAlbumArtist)
            throws IOException, MPDException {
        final List<List<String>> responses = listArtistsCommand(albums, useAlbumArtist);
        final List<List<String>> result = new ArrayList<>(responses.size());
        final int artistLength;
        List<String> albumResult = Collections.emptyList();

        if (useAlbumArtist) {
            artistLength = Music.RESPONSE_ALBUM_ARTIST.length() + 2;
        } else {
            artistLength = Music.RESPONSE_ARTIST.length() + 2;
        }

        for (final List<String> response : responses) {
            albumResult.clear();

            for (final String album : response) {
                final String name = album.substring(artistLength);

                /** Give the array list an approximate size. */
                albumResult = new ArrayList<>(album.length() * response.size());

                albumResult.add(name);
            }

            result.add(albumResult);
        }

        return result;
    }

    /**
     * List all artist names from database.
     *
     * @return artist names from database.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<String> listArtists(final String genre) throws IOException, MPDException {
        return listArtists(genre, true);
    }

    /**
     * List all artist names from database.
     *
     * @param sortInsensitive boolean for insensitive sort when true
     * @return artist names from database.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<String> listArtists(final String genre, final boolean sortInsensitive)
            throws IOException, MPDException {
        final List<String> response = mConnection.send(MPDCommand.MPD_CMD_LIST_TAG,
                Music.TAG_ARTIST, Music.TAG_GENRE, genre);

        Tools.parseResponse(response, Music.RESPONSE_ARTIST);
        sort(response, sortInsensitive);

        return response;
    }

    private List<List<String>> listArtistsCommand(final Iterable<Album> albums,
            final boolean useAlbumArtist) throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue();

        for (final Album album : albums) {
            final Artist artist = album.getArtist();

            // When adding album artist to existing artist check that the artist matches
            if (useAlbumArtist && artist != null && !artist.isUnknown()) {
                commandQueue.add(MPDCommand.MPD_CMD_LIST_TAG, Music.TAG_ALBUM_ARTIST,
                        Music.TAG_ALBUM, album.getName(),
                        Music.TAG_ARTIST, artist.getName());
            } else {
                final String artistCommand;
                if (useAlbumArtist) {
                    artistCommand = Music.TAG_ALBUM_ARTIST;
                } else {
                    artistCommand = Music.TAG_ARTIST;
                }

                commandQueue.add(MPDCommand.MPD_CMD_LIST_TAG, artistCommand,
                        Music.TAG_ALBUM, album.getName());
            }
        }

        return mConnection.sendSeparated(commandQueue);
    }

    /**
     * List all genre names from database.
     *
     * @return artist names from database.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<String> listGenres() throws IOException, MPDException {
        return listGenres(true);
    }

    /**
     * List all genre names from database.
     *
     * @param sortInsensitive boolean for insensitive sort when true
     * @return artist names from database.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<String> listGenres(final boolean sortInsensitive) throws IOException, MPDException {
        final List<String> response = mConnection.send(MPDCommand.MPD_CMD_LIST_TAG,
                Music.TAG_GENRE);

        Tools.parseResponse(response, Music.RESPONSE_GENRE);
        sort(response, sortInsensitive);

        return response;
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public void movePlaylistSong(final PlaylistFile playlist, final int from, final int to)
            throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_PLAYLIST_MOVE, playlist.getFullPath(),
                Integer.toString(from), Integer.toString(to));
    }

    /**
     * Jumps to next playlist track.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void next() throws IOException, MPDException {
        mConnection.send(nextCommand());
    }

    /**
     * Pauses/Resumes music playing.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void pause() throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_PAUSE);
    }

    /**
     * Starts playing music.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void play() throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_PLAY);
    }

    /**
     * Plays previous playlist music.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void previous() throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_PREV);
    }

    /**
     * Tells server to refresh database.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void refreshDatabase() throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_REFRESH);
    }

    /**
     * Tells server to refresh database.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void refreshDatabase(final String folder) throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_REFRESH, folder);
    }

    public void refreshDirectory(final Directory directory)
            throws IOException, MPDException {
        directory.refresh(mConnection);
    }

    /**
     * Removes a list of tracks from a playlist file, by position.
     *
     * @param playlist  The playlist file to remove tracks from.
     * @param positions The positions of the tracks to remove from the playlist file.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public void removeFromPlaylist(final PlaylistFile playlist, final List<Integer> positions)
            throws IOException, MPDException {
        Collections.sort(positions, Collections.reverseOrder());
        final CommandQueue commandQueue = new CommandQueue(positions.size());

        for (final Integer position : positions) {
            commandQueue.add(MPDCommand.MPD_CMD_PLAYLIST_DEL, playlist.getFullPath(),
                    position.toString());
        }

        mConnection.send(commandQueue);
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public void removeFromPlaylist(final PlaylistFile playlist, final Integer pos)
            throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_PLAYLIST_DEL, playlist.getFullPath(),
                Integer.toString(pos));
    }

    public void removeSavedStream(final Integer pos) throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_PLAYLIST_DEL, STREAMS_PLAYLIST,
                Integer.toString(pos));
    }

    public void saveStream(final String url, final String name) throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_PLAYLIST_ADD, STREAMS_PLAYLIST,
                Stream.addStreamName(url, name));
    }

    /**
     * Similar to {@code find},{@code search} looks for partial matches in the MPD database.
     *
     * @param type          type of search. Should be one of the following constants:
     *                      MPD_SEARCH_ARTIST, MPD_SEARCH_TITLE, MPD_SEARCH_ALBUM,
     *                      MPD_SEARCH_FILENAME
     * @param locatorString case-insensitive locator locatorString. Anything that contains {@code
     *                      locatorString} will be returned in the results.
     * @param sort          Sort search results alphabetically
     * @return a Collection of {@code Music}.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     * @see com.anpmech.mpd.item.Music
     */
    public List<Music> search(final String type, final String locatorString, boolean sort)
            throws IOException, MPDException {
        return genericSearch(MPDCommand.MPD_CMD_SEARCH, type, locatorString,sort);
    }

    public List<Music> search(final String[] args) throws IOException, MPDException {
        return genericSearch(MPDCommand.MPD_CMD_SEARCH, args, true);
    }

    /**
     * Seeks current music to the position.
     *
     * @param position song position in seconds
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void seek(final long position) throws IOException, MPDException {
        seekById(mStatus.getSongId(), position);
    }

    /**
     * Seeks music to the position.
     *
     * @param songId   music id in playlist.
     * @param position song position in seconds.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void seekById(final int songId, final long position) throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_SEEK_ID, Integer.toString(songId),
                Long.toString(position));
    }

    /**
     * Seeks music to the position.
     *
     * @param index    music position in playlist.
     * @param position song position in seconds.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void seekByIndex(final int index, final long position) throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_SEEK, Integer.toString(index),
                Long.toString(position));
    }

    /**
     * Enabled or disable consuming.
     *
     * @param consume if true song consuming will be enabled, if false song consuming will be
     *                disabled.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void setConsume(final boolean consume) throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_CONSUME, MPDCommand.booleanValue(consume));
    }

    /**
     * Sets cross-fade.
     *
     * @param time cross-fade time in seconds. 0 to disable cross-fade.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void setCrossFade(final int time) throws IOException, MPDException {
        mConnection
                .send(MPDCommand.MPD_CMD_CROSSFADE, Integer.toString(Math.max(0, time)));
    }

    /**
     * Enabled or disable random.
     *
     * @param random if true random will be enabled, if false random will be disabled.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void setRandom(final boolean random) throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_RANDOM, MPDCommand.booleanValue(random));
    }

    /**
     * Enabled or disable repeating.
     *
     * @param repeat if true repeating will be enabled, if false repeating will be disabled.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void setRepeat(final boolean repeat) throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_REPEAT, MPDCommand.booleanValue(repeat));
    }

    /**
     * Enabled or disable single mode.
     *
     * @param single if true single mode will be enabled, if false single mode will be disabled.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void setSingle(final boolean single) throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_SINGLE, MPDCommand.booleanValue(single));
    }

    /**
     * Sets volume to {@code volume}.
     *
     * @param volume new volume value, must be in 0-100 range.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void setVolume(final int volume) throws IOException, MPDException {
        final int vol = Math.max(MPDStatusMap.VOLUME_MIN,
                Math.min(MPDStatusMap.VOLUME_MAX, volume));
        mConnection.send(MPDCommand.MPD_CMD_SET_VOLUME, Integer.toString(vol));
    }

    /**
     * Kills server.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void shutdown() throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_KILL);
    }

    /**
     * This method returns a thread which will shut down all running threads. This should be added
     * to the client's shutdown hooks, if applicable.
     *
     * @return A thread containing all executors to shutdown.
     */
    public Thread shutdownExecutors() {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                mConnection.shutdown().run();
                mIdleConnection.shutdown().run();
            }
        });
    }

    /**
     * Skip to song with specified {@code id}.
     *
     * @param id song id.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void skipToId(final int id) throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_PLAY_ID, Integer.toString(id));
    }

    /**
     * Jumps to track {@code position} from playlist.
     *
     * @param position track number.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     * @see #skipToId(int)
     */
    public void skipToPosition(final int position) throws IOException, MPDException {
        mConnection.send(skipToPositionCommand(position));
    }

    /**
     * Stops music playing.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void stop() throws IOException, MPDException {
        mConnection.send(MPDCommand.MPD_CMD_STOP);
    }
}
