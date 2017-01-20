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

import com.anpmech.mpd.commandresponse.AlbumArtistResponse;
import com.anpmech.mpd.commandresponse.ArtistResponse;
import com.anpmech.mpd.commandresponse.AudioOutputResponse;
import com.anpmech.mpd.commandresponse.CommandResponse;
import com.anpmech.mpd.commandresponse.GenreResponse;
import com.anpmech.mpd.commandresponse.KeyValueResponse;
import com.anpmech.mpd.commandresponse.MusicResponse;
import com.anpmech.mpd.commandresponse.PlaylistFileResponse;
import com.anpmech.mpd.commandresponse.SeparatedResponse;
import com.anpmech.mpd.commandresponse.StreamResponse;
import com.anpmech.mpd.concurrent.ResultFuture;
import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.connection.MPDConnection;
import com.anpmech.mpd.connection.MPDConnectionStatus;
import com.anpmech.mpd.connection.MonoIOMPDConnection;
import com.anpmech.mpd.connection.MultiIOMPDConnection;
import com.anpmech.mpd.connection.ThreadSafeMonoConnection;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Album;
import com.anpmech.mpd.item.AlbumBuilder;
import com.anpmech.mpd.item.Artist;
import com.anpmech.mpd.item.FilesystemTreeEntry;
import com.anpmech.mpd.item.Genre;
import com.anpmech.mpd.item.Item;
import com.anpmech.mpd.item.Music;
import com.anpmech.mpd.item.PlaylistFile;
import com.anpmech.mpd.item.RefreshableItem;
import com.anpmech.mpd.item.Stream;
import com.anpmech.mpd.subsystem.AudioOutput;
import com.anpmech.mpd.subsystem.Playback;
import com.anpmech.mpd.subsystem.Sticker;
import com.anpmech.mpd.subsystem.status.MPDStatisticsMap;
import com.anpmech.mpd.subsystem.status.MPDStatusMap;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * This is a class containing instantiations of commonly used objects and uses for convenience of
 * a MPD client.
 */
public class MPD {

    private static final String TAG = "MPD";

    protected final MPDPlaylist mPlaylist;

    private final MPDConnection mConnection;

    private final ThreadSafeMonoConnection mIdleConnection;

    private final Playback mPlayback;

    private final MPDStatisticsMap mStatistics;

    private final MPDStatusMap mStatus;

    /**
     * Constructs a new MPD server controller without connection.
     */
    public MPD() {
        super();
        mConnection = new MultiIOMPDConnection(5000);
        mIdleConnection = new MonoIOMPDConnection(0);

        mPlaylist = new MPDPlaylist(mConnection);
        mStatistics = new MPDStatisticsMap(mConnection);
        mStatus = new MPDStatusMap(mConnection);
        mPlayback = new Playback(mStatus, mConnection);
    }

    /**
     * Constructs a new MPD server controller, with a connection in the construction.
     *
     * @param server   The server address or host name to connect to.
     * @param port     The server port to connect to.
     * @param password The default password to use for this connection.
     */
    public MPD(final InetAddress server, final int port, final CharSequence password) {
        this();

        setDefaultPassword(password);
        connect(server, port);
    }

    /**
     * Constructs a new MPD server controller, with a connection in the construction.
     *
     * @param server   The server address or host name to connect to.
     * @param port     The server port to connect to.
     * @param password The default password to use for this connection.
     */
    public MPD(final String server, final int port, final CharSequence password) {
        this();

        setDefaultPassword(password);
        connect(server, port);
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

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static List<Artist> getMergedArtists(final MPDConnection connection,
            final CommandQueue commandQueue) throws IOException, MPDException {
        final CommandResult result = connection.submit(commandQueue).get();

        final ArtistResponse artists = new ArtistResponse(result);
        final AlbumArtistResponse albumArtists = new AlbumArtistResponse(result);

        final List<Artist> artistList = new ArrayList<>(artists);
        final List<Artist> albumArtistList = new ArrayList<>(albumArtists);

        Item.merge(artistList, albumArtistList);

        return artistList;
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

            Collections.sort(songs);

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

            Collections.sort(songs);

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
        add(Collections.singleton(genre), replace, play);
    }

    public void add(final Set<Genre> genres, final boolean replace, final boolean play)
            throws IOException, MPDException {
        final CommandQueue commandQueue;

        if (isCommandAvailable(MPDCommand.MPD_CMD_FIND_ADD)) {
            commandQueue = new CommandQueue();

            for (final Genre genre : genres) {
                commandQueue
                        .add(MPDCommand.MPD_CMD_FIND_ADD, Music.TAG_GENRE, genre.getName());
            }
        } else {
            final Set<Music> music = new HashSet<>();
            for (final Genre genre : genres) {
                music.addAll(find(Music.TAG_GENRE, genre.getName()));
            }
            final List<Music> musicList = new ArrayList<>(music);
            Collections.sort(musicList);

            commandQueue = MPDPlaylist.addAllCommand(musicList);
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
                commandQueue.add(Playback.CMD_ACTION_NEXT);
            } else if (playAfterAdd) {
                commandQueue.add(Playback.CMD_ACTION_PLAY, Integer.toString(playPos));
            }
        } else if (playAfterAdd) {
            commandQueue.add(Playback.CMD_ACTION_PLAY, Integer.toString(playPos));
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
            mConnection.submit(commandQueue);
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
            final boolean play) throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue();

        commandQueue.add(MPDPlaylist.loadCommand(databasePlaylist.getName()));

        add(commandQueue, replace, play);
    }

    protected void addAlbumDetails(final List<Album> albums) throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue(albums.size());
        for (final Album album : albums) {
            commandQueue.add(getAlbumDetailsCommand(album));
        }

        final SeparatedResponse responses = mConnection.submitSeparated(commandQueue).get();

        final AlbumBuilder albumBuilder = new AlbumBuilder();
        int i = 0;
        for (final CommandResponse response : responses) {
            final Album album = albums.get(i);

            albumBuilder.setAlbum(albums.get(i));

            /** First, extract the album specifics from the response. */
            for (final Map.Entry<String, String> entry : new KeyValueResponse(response)) {
                switch (entry.getKey()) {
                    case "songs":
                        albumBuilder.setSongCount(Long.parseLong(entry.getValue()));
                        break;
                    case "playtime":
                        albumBuilder.setDuration(Long.parseLong(entry.getValue()));
                        break;
                    default:
                        break;
                }
            }

            /** Then extract the date and path from a song of the album. */
            final Music song = getFirstTrack(album);

            if (song != null) {
                albumBuilder.setSongDetails(song.getDate(), song.getParentDirectory());
            }
            albums.set(i, albumBuilder.build());
            i++;
        }
    }

    protected void addAlbumSongDetails(final List<Album> albums) throws IOException, MPDException {
        if (!albums.isEmpty()) {
            final ListIterator<Album> iterator = albums.listIterator();
            final AlbumBuilder albumBuilder = new AlbumBuilder();

            while (iterator.hasNext()) {
                final Album album = iterator.next();
                final Music song = getFirstTrack(album);

                if (song != null) {
                    albumBuilder.setAlbum(album);
                    albumBuilder.setSongDetails(song.getDate(), song.getParentDirectory());
                    iterator.set(albumBuilder.build());
                }
            }
        }
    }

    /**
     * Adds all songs in the database to the queue. Optionally, clears the queue prior to the
     * addition. Optionally, play the added songs afterward.
     *
     * @param replace      If true, replaces the entire playlist queue with the added files.
     * @param playAfterAdd If true, starts playing once added.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void addAll(final boolean replace, final boolean playAfterAdd)
            throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue();

        commandQueue.add(MPDPlaylist.MPD_CMD_PLAYLIST_ADD, "/");

        add(commandQueue, replace, playAfterAdd);
    }

    /**
     * Adds a stream to the current queue.
     *
     * @param stream       The stream to add to the queue.
     * @param replace      If true, replaces the entire playlist queue with the added files.
     * @param playAfterAdd If true, starts playing once added.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void addStream(final String stream, final boolean replace, final boolean playAfterAdd)
            throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue();
        commandQueue.add(MPDPlaylist.addCommand(stream));

        add(commandQueue, replace, playAfterAdd);
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public void addToPlaylist(final PlaylistFile playlist, final Album album)
            throws IOException, MPDException {
        if (mIdleConnection.isCommandAvailable(MPDCommand.MPD_CMD_SEARCH_ADD_PLAYLIST)) {
            final String[] artistPair = getAlbumArtistPair(album);

            mConnection.submit(MPDCommand.MPD_CMD_SEARCH_ADD_PLAYLIST, playlist.getFullPath(),
                    Music.TAG_ALBUM, album.getName(), artistPair[0], artistPair[1]);
        } else {
            final List<Music> songs = getSongs(album);

            Collections.sort(songs);

            addToPlaylist(playlist, songs);
        }
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public void addToPlaylist(final PlaylistFile playlist, final Artist artist)
            throws IOException, MPDException {
        if (mIdleConnection.isCommandAvailable(MPDCommand.MPD_CMD_SEARCH_ADD_PLAYLIST)) {
            mConnection.submit(MPDCommand.MPD_CMD_SEARCH_ADD_PLAYLIST, playlist.getFullPath(),
                    Music.TAG_ARTIST, artist.getName());
        } else {
            final List<Music> songs = getSongs(artist);

            Collections.sort(songs);

            addToPlaylist(playlist, songs);
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
            mConnection.submit(commandQueue);
        }
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public void addToPlaylist(final PlaylistFile playlist, final FilesystemTreeEntry entry)
            throws IOException, MPDException {
        mConnection.submit(MPDCommand.MPD_CMD_PLAYLIST_ADD, playlist.getFullPath(),
                entry.getFullPath());
    }

    public void addToPlaylist(final PlaylistFile playlist, final Genre genre)
            throws IOException, MPDException {
        addToPlaylist(playlist, Collections.singleton(genre));
    }

    public void addToPlaylist(final PlaylistFile playlist, final Set<Genre> genres)
            throws IOException, MPDException {
        if (mIdleConnection.isCommandAvailable(MPDCommand.MPD_CMD_SEARCH_ADD_PLAYLIST)) {
            final CommandQueue commandQueue = new CommandQueue();
            for (final Genre genre : genres) {
                commandQueue.add(MPDCommand.MPD_CMD_SEARCH_ADD_PLAYLIST, playlist.getFullPath(),
                        Music.TAG_GENRE, genre.getName());
            }
            mConnection.submit(commandQueue);
        } else {
            final Set<Music> music = new HashSet<>();
            for (final Genre genre : genres) {
                music.addAll(find(Music.TAG_GENRE, genre.getName()));
            }
            final List<Music> musicList = new ArrayList<>(music);
            Collections.sort(musicList);

            addToPlaylist(playlist, musicList);
        }
    }

    public void addToPlaylist(final PlaylistFile playlist, final Music music)
            throws IOException, MPDException {
        addToPlaylist(playlist, Collections.singletonList(music));
    }

    /**
     * Clears error message.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void clearError() throws IOException, MPDException {
        mConnection.submit(MPDCommand.MPD_CMD_CLEARERROR);
    }

    /**
     * Connects to the default MPD server.
     * <p>If there is a default password that is not included in the {@code MPD_HOST} environment
     * variable, {@link #setDefaultPassword(CharSequence)} must be called prior to this method.</p>
     *
     * @throws UnknownHostException Thrown when a hostname can not be resolved.
     */
    public synchronized void connect() throws UnknownHostException {
        mConnection.connect();
        mIdleConnection.connect();
    }

    /**
     * Connects to a MPD server.
     * <p>If there is a default password, {@link #setDefaultPassword(CharSequence)} must be called
     * prior to this method.</p>
     *
     * @param server server address or host name
     * @param port   server port
     */
    public final synchronized void connect(final InetAddress server, final int port) {
        if (!isConnected()) {
            mConnection.connect(server, port);
            mIdleConnection.connect(server, port);
        }
    }

    /**
     * Connects to a MPD server.
     * <p>If there is a default password, {@link #setDefaultPassword(CharSequence)} must be called
     * prior to this method.</p>
     *
     * @param server server address or host name
     * @param port   server port
     */
    public final void connect(final String server, final int port) {
        if (!isConnected()) {
            mConnection.connect(server, port);
            mIdleConnection.connect(server, port);
        }
    }

    /**
     * Connects to a MPD server.
     * <p>If there is a default password, {@link #setDefaultPassword(CharSequence)} must be called
     * prior to this method.</p>
     *
     * @param server server address or host name and port (server:port)
     */
    public final void connect(final String server) {
        int port = MPDCommand.DEFAULT_MPD_PORT;
        final String host;
        if (server.indexOf(':') == -1) {
            host = server;
        } else {
            host = server.substring(0, server.lastIndexOf(':'));
            port = Integer.parseInt(server.substring(server.lastIndexOf(':') + 1));
        }
        connect(host, port);
    }

    public void disableOutput(final int id) throws IOException, MPDException {
        mConnection.submit(MPDCommand.MPD_CMD_OUTPUTDISABLE, Integer.toString(id));
    }

    /**
     * Disconnects from server.
     *
     * @throws IOException if an error occur while closing connection
     */
    public synchronized void disconnect() throws IOException {
        mIdleConnection.disconnect();
        mConnection.disconnect();
        mPlaylist.invalidate();
        mStatistics.invalidate();
        mStatus.invalidate();
    }

    public ResultFuture enableOutput(final int id) {
        return mConnection.submit(MPDCommand.MPD_CMD_OUTPUTENABLE, Integer.toString(id));
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
     * @see Music
     */
    public MusicResponse find(final String type, final String locatorString)
            throws IOException, MPDException {
        return genericSearch(MPDCommand.MPD_CMD_FIND, type, locatorString);
    }

    public MusicResponse find(final String[] args) throws IOException, MPDException {
        return genericSearch(MPDCommand.MPD_CMD_FIND, args);
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

    protected MusicResponse genericSearch(final CharSequence searchCommand, final String... args)
            throws IOException, MPDException {
        final CommandResult result = mConnection.submit(searchCommand, args).get();

        return new MusicResponse(result);
    }

    protected MusicResponse genericSearch(final CharSequence searchCommand, final String type,
            final String strToFind) throws IOException, MPDException {
        final CommandResult result = mConnection.submit(searchCommand, type, strToFind).get();

        return new MusicResponse(result);
    }

    public AlbumArtistResponse getAlbumArtists() throws IOException, MPDException {
        final CommandResult result = mConnection.submit(MPDCommand.MPD_CMD_LIST_TAG,
                Music.TAG_ALBUM_ARTIST).get();

        return new AlbumArtistResponse(result);
    }

    public AlbumArtistResponse getAlbumArtists(final Genre genre) throws IOException, MPDException {
        final CommandResult result = mConnection.submit(
                MPDCommand.MPD_CMD_LIST_TAG, Music.TAG_ALBUM_ARTIST,
                Music.TAG_GENRE, genre.getName()).get();
        return new AlbumArtistResponse(result);
    }

    public List<Artist> getAlbumArtists(final Set<Genre> genres) throws IOException, MPDException {
        final CommandQueue commands = new CommandQueue();
        for (final Genre genre : genres) {
            commands.add(MPDCommand.MPD_CMD_LIST_TAG, Music.TAG_ALBUM_ARTIST,
                    Music.TAG_GENRE, genre.getName());
        }
        final CommandResult result = mConnection.submit(commands).get();

        final List<Artist> artists = new ArrayList<>(new AlbumArtistResponse(result));
        Item.merge(artists, Collections.<Artist>emptyList());
        return artists;
    }

    public int getAlbumCount(final Artist artist, final boolean useAlbumArtistTag)
            throws IOException, MPDException {
        return listAlbums(artist, useAlbumArtistTag).size();
    }

    public List<Album> getAlbums(final Artist artist, final boolean sortByYear,
            final boolean trackCountNeeded) throws IOException, MPDException {
        final List<Album> albums = getAlbums(artist, sortByYear, trackCountNeeded, false);

        // 1. the null artist list already contains all albums
        // 2. the "unknown artist" should not list unknown album artists
        if (artist != null && !artist.isUnknown()) {
            Item.merge(albums, getAlbums(artist, sortByYear, trackCountNeeded, true));
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
            final List<String> albumNames = listAlbums(artist, useAlbumArtist);
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

        return albums;
    }

    public ArtistResponse getArtists() throws IOException, MPDException {
        final CommandResult result = mConnection.submit(MPDCommand.MPD_CMD_LIST_TAG,
                Music.TAG_ARTIST).get();

        return new ArtistResponse(result);
    }

    public ArtistResponse getArtists(final Genre genre) throws IOException, MPDException {
        final CommandResult result = mConnection.submit(MPDCommand.MPD_CMD_LIST_TAG,
                Music.TAG_ARTIST, Music.TAG_GENRE, genre.getName()).get();
        return new ArtistResponse(result);
    }

    public List<Artist> getArtists(final Set<Genre> genres) throws IOException, MPDException {
        final CommandQueue commands = new CommandQueue();
        for (final Genre genre : genres) {
            commands.add(MPDCommand.MPD_CMD_LIST_TAG,
                    Music.TAG_ARTIST, Music.TAG_GENRE, genre.getName());
        }
        final CommandResult result = mConnection.submit(commands).get();

        final List<Artist> artists = new ArrayList<>(new ArtistResponse(result));
        Item.merge(artists, Collections.<Artist>emptyList());
        return artists;
    }

    public List<Artist> getArtistsMerged() throws IOException, MPDException {
        final CommandQueue commands = new CommandQueue(2);
        commands.add(MPDCommand.MPD_CMD_LIST_TAG, Music.TAG_ARTIST);
        commands.add(MPDCommand.MPD_CMD_LIST_TAG, Music.TAG_ALBUM_ARTIST);

        return getMergedArtists(mConnection, commands);
    }

    public List<Artist> getArtistsMerged(final Genre genre) throws IOException, MPDException {
        return getArtistsMerged(Collections.singleton(genre));
    }

    public List<Artist> getArtistsMerged(final Set<Genre> genres) throws IOException, MPDException {
        final CommandQueue commands = new CommandQueue();
        for (final Genre genre : genres) {
            final String genreName = genre.getName();
            commands.add(MPDCommand.MPD_CMD_LIST_TAG, Music.TAG_ARTIST, Music.TAG_GENRE, genreName);
            commands.add(MPDCommand.MPD_CMD_LIST_TAG, Music.TAG_ALBUM_ARTIST, Music.TAG_GENRE,
                    genreName);
        }

        return getMergedArtists(mConnection, commands);
    }

    /**
     * This returns the connection status of the mono connection utilized by this class.
     *
     * @return The MPDConnectionStatus class for the mono connection used by this class.
     */
    public MPDConnectionStatus getConnectionStatus() {
        return mIdleConnection.getConnectionStatus();
    }

    /**
     * This retrieves the current track from the {@link MPDPlaylist}/{@link MPDStatusMap} cache.
     *
     * <p>This method <b>depends</b> on a valid MPDStatus and MPDPlaylist prior to being run. The
     * reason it is not run by this method is to wait for validity will block and it is up to the
     * user to take precaution for this prior to running this method.</p>
     *
     * @return The current track.
     */
    public Music getCurrentTrack() {
        final int songID = mStatus.getSongId();
        final Music currentTrack;

        if (songID == MPDStatusMap.DEFAULT_INTEGER) {
            currentTrack = null;
        } else {
            currentTrack = mPlaylist.getById(songID);
        }

        return currentTrack;
    }

    @Nullable
    protected Music getFirstTrack(final Album album) throws IOException, MPDException {
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
        MusicResponse songs = find(args);
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

        final Iterator<Music> iterator = songs.iterator();
        Music song = null;
        if (iterator.hasNext()) {
            song = iterator.next();
        }

        return song;
    }

    /**
     * This method retrieves a {@link GenreResponse} including all available genres.
     *
     * @return A GenreResponse including all available genres.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public GenreResponse getGenreResponse() throws IOException, MPDException {
        final CommandResult result = mConnection.submit(MPDCommand.MPD_CMD_LIST_TAG,
                Music.TAG_GENRE).get();

        return new GenreResponse(result);
    }

    public InetAddress getHostAddress() {
        return mConnection.getHostAddress();
    }

    public int getHostPort() {
        return mConnection.getHostPort();
    }

    /**
     * This returns a thread safe version of the mono socket IO connection.
     *
     * @return A thread safe version of the mono socket IO connection.
     */
    public ThreadSafeMonoConnection getIdleConnection() {
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
    public AudioOutputResponse getOutputs() throws IOException, MPDException {
        final CommandResult result = mConnection.submit(AudioOutput.CMD_ACTION_OUTPUTS).get();

        return new AudioOutputResponse(result);
    }

    /**
     * This returns the local copy of the Playback object.
     *
     * @return A local copy of the Playback object.
     */
    public Playback getPlayback() {
        return mPlayback;
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
    public MusicResponse getPlaylistSongs(final PlaylistFile playlist)
            throws IOException, MPDException {
        return genericSearch(MPDCommand.MPD_CMD_PLAYLIST_INFO, playlist.getFullPath());
    }

    /**
     * Returns a list of all available playlist files.
     *
     * @return Returns a list of all available playlist files.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public PlaylistFileResponse getPlaylists() throws IOException, MPDException {
        final CommandResult result = mConnection.submit(MPDCommand.MPD_CMD_LISTPLAYLISTS).get();

        return new PlaylistFileResponse(result);
    }

    public StreamResponse getSavedStreams() throws IOException, MPDException {
        final CommandResult result =
                mConnection.submit(MPDCommand.MPD_CMD_PLAYLIST_INFO, Stream.PLAYLIST_NAME).get();

        return new StreamResponse(result);
    }

    public List<Music> getSongs(final Album album) throws IOException, MPDException {
        final CommandResult result = mConnection.submit(getSongsCommand(album)).get();
        final MusicResponse response = new MusicResponse(result);
        final List<Music> tracks;

        if (album.hasAlbumArtist()) {
            // remove songs that don't have this album artist (mpd >=0.18 puts them in)
            final Artist artist = album.getArtist();
            String artistName = null;
            tracks = new ArrayList<>();

            if (artist != null) {
                artistName = artist.getName();
            }

            for (final Music track : response) {
                final String albumArtist = track.getAlbumArtistName();

                if (Tools.isEmpty(albumArtist) || albumArtist.equals(artistName)) {
                    tracks.add(track);
                }
            }
        } else {
            tracks = new ArrayList<>(response);
        }

        return tracks;
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

        final CommandResult result = mConnection.submit(MPDCommand.create(
                MPDCommand.MPD_CMD_LIST_TAG, Music.TAG_ALBUM,
                Music.TAG_ALBUM, album.getName(),
                artistType, artistName,
                Music.TAG_GENRE, genre.getName())).get();

        return !result.isEmpty();
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
        return mIdleConnection.getConnectionStatus().isConnected();
    }

    public List<List<String>> listAlbumArtists(final List<Album> albums)
            throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue(albums.size());

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

        List<List<String>> responses = new ArrayList<>();
        for (final CommandResponse response : mConnection.submitSeparated(commandQueue).get()) {
            responses.add(new KeyValueResponse(response).getValues(Music.RESPONSE_ALBUM_ARTIST));
        }

        if (responses.size() != albums.size()) {
            Log.warning(TAG, "Response and album size differ when listing album artists.");
            responses = Collections.emptyList();
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
    public List<String> listAlbums(final Artist artist, final boolean useAlbumArtist)
            throws IOException, MPDException {
        final MPDCommand command;

        if (artist == null) {
            command = listAlbumsCommand(null, useAlbumArtist);
        } else {
            command = listAlbumsCommand(artist.getName(), useAlbumArtist);
        }

        final KeyValueResponse response = new KeyValueResponse(mConnection.submit(command).get());

        return response.getValues(Music.RESPONSE_ALBUM);
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
                        artistAlbum.isNameSame(albumArtistAlbum)) {
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
        final KeyValueResponse response =
                new KeyValueResponse(
                        mConnection.submit(listAllAlbumsGroupedCommand(useAlbumArtist)).get());
        final List<Album> result = new ArrayList<>();
        String currentAlbum = null;

        if (useAlbumArtist) {
            artistResponse = Music.RESPONSE_ALBUM_ARTIST;
        } else {
            artistResponse = Music.RESPONSE_ARTIST;
        }

        for (final Map.Entry<String, String> entry : response) {
            if (artistResponse.equals(entry.getKey())) {
                if (currentAlbum != null) {
                    albumBuilder.setBase(currentAlbum, entry.getValue(), useAlbumArtist);
                    result.add(albumBuilder.build());

                    currentAlbum = null;
                }
            } else if (albumResponse.equals(entry.getKey())) {
                if (currentAlbum != null) {
                    albumBuilder.setName(currentAlbum);
                    /** There was no artist in this response, add the album alone */
                    result.add(albumBuilder.build());
                }

                if (!entry.getValue().isEmpty() || includeUnknownAlbum) {
                    currentAlbum = entry.getValue();
                } else {
                    currentAlbum = null;
                }
            }
        }

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
    public MusicResponse listAllInfo() throws IOException, MPDException {
        final CommandResult result = mConnection.submit(MPDCommand.MPD_CMD_LISTALLINFO).get();

        return new MusicResponse(result);
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public void movePlaylistSong(final PlaylistFile playlist, final int from, final int to)
            throws IOException, MPDException {
        mConnection.submit(MPDCommand.MPD_CMD_PLAYLIST_MOVE, playlist.getFullPath(),
                Integer.toString(from), Integer.toString(to));
    }

    /**
     * Retrieves a database directory listing of all server recognized entries.
     *
     * @param directory The directory to update with a full entry listing.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void refresh(final RefreshableItem directory) throws IOException, MPDException {
        directory.refresh(mConnection);
    }

    /**
     * Tells server to refresh database.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void refreshDatabase() throws IOException, MPDException {
        mConnection.submit(MPDCommand.MPD_CMD_REFRESH);
    }

    /**
     * Tells server to refresh database.
     *
     * @param folder The folder to use as the root for the database refresh.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void refreshDatabase(final String folder) throws IOException, MPDException {
        mConnection.submit(MPDCommand.MPD_CMD_REFRESH, folder);
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

        mConnection.submit(commandQueue);
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public void removeFromPlaylist(final PlaylistFile playlist, final int pos)
            throws IOException, MPDException {
        removeFromPlaylist(playlist.getFullPath(), pos);
    }

    private void removeFromPlaylist(final String playlist, final int pos)
            throws IOException, MPDException {
        mConnection.submit(MPDCommand.MPD_CMD_PLAYLIST_DEL, playlist, Integer.toString(pos));
    }

    public void removeSavedStream(final int pos) throws IOException, MPDException {
        removeFromPlaylist(Stream.PLAYLIST_NAME, pos);
    }

    public void saveStream(final String url, final String name) throws IOException, MPDException {
        mConnection.submit(MPDCommand.MPD_CMD_PLAYLIST_ADD, Stream.PLAYLIST_NAME,
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
     * @return a Collection of {@code Music}.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     * @see Music
     */
    public MusicResponse search(final String type, final String locatorString)
            throws IOException, MPDException {
        return genericSearch(MPDCommand.MPD_CMD_SEARCH, type, locatorString);
    }

    public MusicResponse search(final String... args) throws IOException, MPDException {
        return genericSearch(MPDCommand.MPD_CMD_SEARCH, args);
    }

    /**
     * This sets the default password for the MPD server.
     *
     * @param password The default password for this MPD server.
     */
    public final void setDefaultPassword(final CharSequence password) {
        mConnection.setDefaultPassword(password);
        mIdleConnection.setDefaultPassword(password);
    }

    /**
     * Kills server.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void shutdown() throws IOException, MPDException {
        mConnection.submit(MPDCommand.MPD_CMD_KILL);
    }
}
