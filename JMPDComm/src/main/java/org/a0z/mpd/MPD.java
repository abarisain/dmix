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
import org.a0z.mpd.connection.MPDConnectionMonoSocket;
import org.a0z.mpd.connection.MPDConnectionMultiSocket;
import org.a0z.mpd.exception.MPDServerException;
import org.a0z.mpd.item.Album;
import org.a0z.mpd.item.Artist;
import org.a0z.mpd.item.Directory;
import org.a0z.mpd.item.FilesystemTreeEntry;
import org.a0z.mpd.item.Genre;
import org.a0z.mpd.item.Item;
import org.a0z.mpd.item.Music;
import org.a0z.mpd.item.PlaylistFile;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * MPD Server controller.
 */
public class MPD {

    public static final String STREAMS_PLAYLIST = "[Radio Streams]";

    private static final String TAG = "MPD";

    protected static boolean sortAlbumsByYear = false;

    protected static boolean sortByTrackNumber = true;

    private static String sUnknownAlbum = "";

    private static String sUnknownArtist = "";

    protected final MPDPlaylist mPlaylist;

    protected final Directory mRootDirectory;

    private final MPDConnection mConnection;

    private final MPDConnection mIdleConnection;

    private final MPDStatistics mStatistics;

    private final MPDStatus mStatus;

    /**
     * Constructs a new MPD server controller without connection.
     */
    public MPD() {
        super();
        mConnection = new MPDConnectionMultiSocket(5000, 2);
        mIdleConnection = new MPDConnectionMonoSocket(0);
        mStatistics = new MPDStatistics();

        mPlaylist = new MPDPlaylist(mConnection);
        mStatus = new MPDStatus();
        mRootDirectory = Directory.makeRootDirectory(this);
    }

    /**
     * Constructs a new MPD server controller.
     *
     * @param server server address or host name
     * @param port   server port
     * @throws MPDServerException if an error occur while contacting server
     */
    public MPD(final InetAddress server, final int port, final String password)
            throws MPDServerException {
        this();
        connect(server, port, password);
    }

    /**
     * Constructs a new MPD server controller.
     *
     * @param server server address or host name
     * @param port   server port
     * @throws MPDServerException if an error occur while contacting server
     */
    public MPD(final String server, final int port, final String password)
            throws MPDServerException,
            UnknownHostException {
        this();
        connect(server, port, password);
    }

    protected static MPDCommand getAlbumDetailsCommand(final Album album)
            throws MPDServerException {
        if (album.hasAlbumArtist()) {
            return new MPDCommand(MPDCommand.MPD_CMD_COUNT,
                    MPDCommand.MPD_TAG_ALBUM, album.getName(),
                    MPDCommand.MPD_TAG_ALBUM_ARTIST, album.getArtist().getName());
        } else { // only get albums without albumartist
            return new MPDCommand(MPDCommand.MPD_CMD_COUNT,
                    MPDCommand.MPD_TAG_ALBUM, album.getName(),
                    MPDCommand.MPD_TAG_ARTIST, album.getArtist().getName());
        }
    }

    public static MPDCommand getSongsCommand(final Album album) {
        final String albumname = album.getName();
        final Artist artist = album.getArtist();
        if (null == artist) { // get songs for ANY artist
            return new MPDCommand(MPDCommand.MPD_CMD_FIND,
                    MPDCommand.MPD_TAG_ALBUM, albumname);
        }
        final String artistname = artist.getName();
        if (album.hasAlbumArtist()) {
            return new MPDCommand(MPDCommand.MPD_CMD_FIND,
                    MPDCommand.MPD_TAG_ALBUM, albumname,
                    MPDCommand.MPD_TAG_ALBUM_ARTIST, artistname);
        } else {
            return new MPDCommand(MPDCommand.MPD_CMD_FIND,
                    MPDCommand.MPD_TAG_ALBUM, albumname,
                    MPDCommand.MPD_TAG_ARTIST, artistname);
        }
    }

    public static String getUnknownAlbum() {
        return sUnknownAlbum;
    }

    public static String getUnknownArtist() {
        return sUnknownArtist;
    }

    /*
     * get raw command String for listAlbums
     */
    public static MPDCommand listAlbumsCommand(final String artist, final boolean useAlbumArtist) {
        if (useAlbumArtist) {
            return new MPDCommand(MPDCommand.MPD_CMD_LIST_TAG, MPDCommand.MPD_TAG_ALBUM,
                    MPDCommand.MPD_TAG_ALBUM_ARTIST, artist);
        } else {
            return new MPDCommand(MPDCommand.MPD_CMD_LIST_TAG, MPDCommand.MPD_TAG_ALBUM, artist);
        }
    }

    /*
     * get raw command String for listAllAlbumsGrouped
     */
    public static MPDCommand listAllAlbumsGroupedCommand(final boolean useAlbumArtist) {
        final String artistTag = useAlbumArtist ? MPDCommand.MPD_TAG_ALBUM_ARTIST :
                MPDCommand.MPD_TAG_ARTIST;
        return new MPDCommand(MPDCommand.MPD_CMD_LIST_TAG, MPDCommand.MPD_TAG_ALBUM,
                MPDCommand.MPD_CMD_GROUP, artistTag);
    }

    static MPDCommand nextCommand() {
        return new MPDCommand(MPDCommand.MPD_CMD_NEXT);
    }

    /**
     * Parse the response from tag list command for album artists.
     *
     * @param response        The album artist list response from the MPD server database.
     * @param substring       The substring from the response to remove.
     * @param sortInsensitive Whether to sort insensitively.
     * @return Returns a parsed album artist list.
     */
    private static List<String> parseResponse(final Collection<String> response,
            final String substring, final boolean sortInsensitive) {
        final List<String> result = new ArrayList<>(response.size());
        for (final String line : response) {
            result.add(line.substring((substring + ": ").length()));
        }
        if (sortInsensitive) {
            Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        } else {
            Collections.sort(result);
        }
        return result;
    }

    public static void setSortAlbumsByYear(final boolean v) {
        sortAlbumsByYear = v;
    }

    public static void setSortByTrackNumber(final boolean v) {
        sortByTrackNumber = v;
    }

    public static void setUnknownAlbum(final String unknownAlbum) {
        sUnknownAlbum = unknownAlbum;
    }

    public static void setUnknownArtist(final String unknownArtist) {
        sUnknownArtist = unknownArtist;
    }

    static MPDCommand skipToPositionCommand(final int position) {
        return new MPDCommand(MPDCommand.MPD_CMD_PLAY, Integer.toString(position));
    }

    public static boolean sortAlbumsByYear() {
        return sortAlbumsByYear;
    }

    public static boolean sortByTrackNumber() {
        return sortByTrackNumber;
    }

    /**
     * Adds a {@code Album} item object to the playlist queue.
     *
     * @param album {@code Album} item object to be added to the media server playlist queue.
     * @throws MPDServerException On media server command parsing or connection error.
     */
    public void add(final Album album) throws MPDServerException {
        add(album, false, false);
    }

    /**
     * Adds a {@code Album} item object to the playlist queue.
     *
     * @param album   {@code Album} item object to be added to the media server playlist queue.
     * @param replace Whether to clear the playlist queue prior to adding the item(s).
     * @param play    Whether to play the playlist queue after adding the item(s).
     * @throws MPDServerException On media server command parsing or connection error.
     */
    public void add(final Album album, final boolean replace, final boolean play)
            throws MPDServerException {
        final List<Music> songs = getSongs(album);
        final CommandQueue commandQueue = MPDPlaylist.addAllCommand(songs);

        add(commandQueue, replace, play);
    }

    /**
     * Adds a {@code Artist} item object to the playlist queue.
     *
     * @param artist {@code Artist} item object to be added to the media server playlist queue.
     * @throws MPDServerException On media server command parsing or connection error.
     */
    public void add(final Artist artist) throws MPDServerException {
        add(artist, false, false);
    }

    /**
     * Adds a {@code Artist} item object to the playlist queue.
     *
     * @param artist  {@code Artist} item object to be added to the media server playlist queue.
     * @param replace Whether to clear the playlist queue prior to adding the item(s).
     * @param play    Whether to play the playlist queue after adding the item(s).
     * @throws MPDServerException On media server command parsing or connection error.
     */
    public void add(final Artist artist, final boolean replace, final boolean play)
            throws MPDServerException {
        final List<Music> songs = getSongs(artist);
        final CommandQueue commandQueue = MPDPlaylist.addAllCommand(songs);

        add(commandQueue, replace, play);
    }

    /**
     * Add a {@code Music} item object to the playlist queue.
     *
     * @param music   {@code Music} item object to be added to the media server playlist queue.
     * @param replace Whether to clear the playlist queue prior to adding the item(s).
     * @param play    Whether to play the playlist queue after adding the item(s).
     * @throws MPDServerException On media server command parsing or connection error.
     */
    public void add(final FilesystemTreeEntry music, final boolean replace, final boolean play)
            throws MPDServerException {
        final CommandQueue commandQueue = new CommandQueue();

        commandQueue.add(MPDPlaylist.addCommand(music.getFullPath()));

        add(commandQueue, replace, play);
    }

    /**
     * Add a {@code Music} item object to the playlist queue.
     *
     * @param music {@code Music} item object to be added to the playlist queue.
     * @throws MPDServerException On media server command parsing or connection error.
     */
    public void add(final FilesystemTreeEntry music) throws MPDServerException {
        add(music, false, false);
    }

    /**
     * Adds songs to the queue. Optionally, clears the queue prior to the addition. Optionally,
     * play the added songs afterward.
     *
     * @param commandQueue The commandQueue that will be responsible of inserting the
     *                     songs into the queue.
     * @param replace      If true, replaces the entire playlist queue with the added files.
     * @param playAfterAdd If true, starts playing once added.
     */
    public void add(final CommandQueue commandQueue, final boolean replace,
            final boolean playAfterAdd) throws MPDServerException {
        int playPos = 0;
        final boolean isPlaying = mStatus.isState(MPDStatus.STATE_PLAYING);
        final boolean isConsume = mStatus.isConsume();
        final boolean isRandom = mStatus.isRandom();
        final int playlistLength = mStatus.getPlaylistLength();

        /** Replace */
        if (replace) {
            if (isPlaying) {
                if (playlistLength > 1) {
                    try {
                        commandQueue.add(0, MPDPlaylist.cropCommand(this));
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
            commandQueue.add(MPDPlaylist.removeByIndexCommand(0));

        }

        commandQueue.send(mConnection);
    }

    /**
     * Add a {@code Playlist} item object to the playlist queue.
     *
     * @param databasePlaylist A playlist item stored on the media server to add to the
     *                         playlist queue.
     * @throws MPDServerException On media server command parsing or connection error.
     */
    public void add(final PlaylistFile databasePlaylist) throws MPDServerException {
        add(databasePlaylist, false, false);
    }

    /**
     * Add a {@code Playlist} item object to the playlist queue.
     *
     * @param databasePlaylist A playlist item stored on the media server to add to the
     *                         playlist queue.
     * @param replace          Whether to clear the playlist queue prior to adding the
     *                         databasePlaylist string.
     * @param play             Whether to play the playlist queue prior after adding the
     *                         databasePlaylist string.
     * @throws MPDServerException On media server command parsing or connection error.
     */
    public void add(final PlaylistFile databasePlaylist, final boolean replace, final boolean play)
            throws MPDServerException {
        final CommandQueue commandQueue = new CommandQueue();

        commandQueue.add(MPDPlaylist.loadCommand(databasePlaylist.getName()));

        add(commandQueue, replace, play);
    }

    protected void addAlbumPaths(final List<Album> albums) {
        if (albums == null || albums.isEmpty()) {
            return;
        }

        for (final Album a : albums) {
            try {
                final List<Music> songs = getFirstTrack(a);
                if (!songs.isEmpty()) {
                    a.setPath(songs.get(0).getPath());
                }
            } catch (final MPDServerException e) {
            }
        }
    }

    /** TODO: This needs to be an add(Stream, ...) method. */
    public void addStream(final String stream, final boolean replace, final boolean play)
            throws MPDServerException {
        final CommandQueue commandQueue = new CommandQueue();
        commandQueue.add(MPDPlaylist.addCommand(stream));

        add(commandQueue, replace, play);
    }

    public void addToPlaylist(final String playlistName, final Album album)
            throws MPDServerException {
        addToPlaylist(playlistName, new ArrayList<>(getSongs(album)));
    }

    public void addToPlaylist(final String playlistName, final Artist artist)
            throws MPDServerException {
        addToPlaylist(playlistName, new ArrayList<>(getSongs(artist)));
    }

    public void addToPlaylist(final String playlistName, final Collection<Music> musicCollection)
            throws MPDServerException {
        if (null == musicCollection || musicCollection.size() < 1) {
            return;
        }
        final CommandQueue commandQueue = new CommandQueue();

        for (final Music music : musicCollection) {
            commandQueue.add(MPDCommand.MPD_CMD_PLAYLIST_ADD, playlistName, music.getFullPath());
        }
        commandQueue.send(mConnection);
    }

    public void addToPlaylist(final String playlistName, final FilesystemTreeEntry entry)
            throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_PLAYLIST_ADD, playlistName,
                entry.getFullPath());
    }

    public void addToPlaylist(final String playlistName, final Music music)
            throws MPDServerException {
        final Collection<Music> songs = new ArrayList<>(1);
        songs.add(music);
        addToPlaylist(playlistName, songs);
    }

    /**
     * Increases or decreases volume by {@code modifier} amount.
     *
     * @param modifier volume adjustment
     * @throws MPDServerException if an error occur while contacting server
     */
    public void adjustVolume(final int modifier) throws MPDServerException {
        // calculate final volume (clip value with [0, 100])
        int vol = mStatus.getVolume() + modifier;
        vol = Math.max(MPDCommand.MIN_VOLUME, Math.min(MPDCommand.MAX_VOLUME, vol));

        mConnection.sendCommand(MPDCommand.MPD_CMD_SET_VOLUME, Integer.toString(vol));
    }

    /*
     * test whether given album is in given genre
     */
    public boolean albumInGenre(final Album album, final Genre genre) throws MPDServerException {
        final List<String> response;
        final Artist artist = album.getArtist();
        response = mConnection.sendCommand
                (new MPDCommand(MPDCommand.MPD_CMD_LIST_TAG,
                        MPDCommand.MPD_TAG_ALBUM,
                        MPDCommand.MPD_TAG_ALBUM, album.getName(),
                        album.hasAlbumArtist() ? MPDCommand.MPD_TAG_ALBUM_ARTIST
                                : MPDCommand.MPD_TAG_ARTIST,
                        (artist == null ? "" : artist.getName()),
                        MPDCommand.MPD_TAG_GENRE, genre.getName()
                ));
        return !response.isEmpty();
    }

    /**
     * Clears error message.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void clearError() throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_CLEARERROR);
    }

    /**
     * Connects to a MPD server.
     *
     * @param server server address or host name
     * @param port   server port
     */
    public final synchronized void connect(final InetAddress server, final int port,
            final String password)
            throws MPDServerException {
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
     * @throws MPDServerException if an error occur while contacting server
     */
    public final void connect(final String server, final int port, final String password)
            throws MPDServerException,
            UnknownHostException {
        final InetAddress address = InetAddress.getByName(server);
        connect(address, port, password);
    }

    /**
     * Connects to a MPD server.
     *
     * @param server server address or host name and port (server:port)
     * @throws MPDServerException if an error occur while contacting server
     */
    public final void connect(final String server, final String password) throws MPDServerException,
            UnknownHostException {
        int port = MPDCommand.DEFAULT_MPD_PORT;
        final String host;
        if (server.indexOf(':') != -1) {
            host = server.substring(0, server.lastIndexOf(':'));
            port = Integer.parseInt(server.substring(server.lastIndexOf(':') + 1));
        } else {
            host = server;
        }
        connect(host, port, password);
    }

    public void disableOutput(final int id) throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_OUTPUTDISABLE, Integer.toString(id));
    }

    /**
     * Disconnects from server.
     *
     * @throws MPDServerException if an error occur while closing connection
     */
    public synchronized void disconnect() throws MPDServerException {
        MPDServerException ex = null;
        if (mConnection != null && mConnection.isConnected()) {
            try {
                mConnection.disconnect();
            } catch (final MPDServerException e) {
                ex = (ex != null) ? ex : e;// Always keep first non null
                // exception
            }
        }
        if (mIdleConnection != null && mIdleConnection.isConnected()) {
            try {
                mIdleConnection.disconnect();
            } catch (final MPDServerException e) {
                ex = (ex != null) ? ex : e;// Always keep non null first
                // exception
            }
        }
    }

    public void editSavedStream(final String url, final String name, final Integer pos)
            throws MPDServerException {
        removeSavedStream(pos);
        saveStream(url, name);
    }

    public void enableOutput(final int id) throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_OUTPUTENABLE, Integer.toString(id));
    }

    /**
     * Similar to {@code search},{@code find} looks for exact matches
     * in the MPD database.
     *
     * @param type          type of search. Should be one of the following constants:
     *                      MPD_FIND_ARTIST, MPD_FIND_ALBUM
     * @param locatorString case-insensitive locator locatorString. Anything that exactly
     *                      matches {@code locatorString} will be returned in the results.
     * @return a Collection of {@code Music}
     * @throws MPDServerException if an error occur while contacting server
     * @see org.a0z.mpd.item.Music
     */
    public Collection<Music> find(final String type, final String locatorString)
            throws MPDServerException {
        return genericSearch(MPDCommand.MPD_CMD_FIND, type, locatorString);
    }

    public List<Music> find(final String[] args) throws MPDServerException {
        return genericSearch(MPDCommand.MPD_CMD_FIND, args, true);
    }

    /*
     * For all given albums, look for albumartists and create as many albums as
     * there are albumartists, including "" The server call can be slow for long
     * album lists
     */
    protected void fixAlbumArtists(final List<Album> albums) {
        if (albums == null || albums.isEmpty()) {
            return;
        }
        List<String[]> albumartists = null;
        try {
            albumartists = listAlbumArtists(albums);
        } catch (final MPDServerException e) {
            Log.error(TAG, "Failed to fix album artists.", e);
        }
        if (albumartists == null || albumartists.size() != albums.size()) {
            return;
        }

        /** Split albums are rare, let it allocate as needed. */
        @SuppressWarnings("CollectionWithoutInitialCapacity")
        final Collection<Album> splitalbums = new ArrayList<>();
        int i = 0;
        for (Album a : albums) {
            final String[] aartists = albumartists.get(i);
            if (aartists.length > 0) {
                Arrays.sort(aartists); // make sure "" is the first one
                if (aartists[0] != null && !aartists[0].isEmpty()) { // one albumartist, fix this
                    // album
                    final Artist artist = new Artist(aartists[0]);
                    a = a.setArtist(artist);
                } // do nothing if albumartist is ""
                if (aartists.length > 1) { // it's more than one album, insert
                    for (int n = 1; n < aartists.length; n++) {
                        final Album newalbum =
                                new Album(a.getName(), new Artist(aartists[n]), true);
                        splitalbums.add(newalbum);
                    }
                }
            }
            i++;
        }
        albums.addAll(splitalbums);
    }

    protected List<Music> genericSearch(
            final String searchCommand, final String[] args, final boolean sort)
            throws MPDServerException {
        return Music.getMusicFromList(mConnection.sendCommand(searchCommand, args), sort);
    }

    protected Collection<Music> genericSearch(final String searchCommand, final String type,
            final String strToFind)
            throws MPDServerException {
        final List<String> response = mConnection.sendCommand(searchCommand, type, strToFind);
        return Music.getMusicFromList(response, true);
    }

    public int getAlbumCount(final Artist artist, final boolean useAlbumArtistTag)
            throws MPDServerException {
        return listAlbums(artist.getName(), useAlbumArtistTag).size();
    }

    public int getAlbumCount(final String artist, final boolean useAlbumArtistTag)
            throws MPDServerException {
        return listAlbums(artist, useAlbumArtistTag).size();
    }

    protected void getAlbumDetails(final List<Album> albums,
            final boolean findYear) throws MPDServerException {
        final CommandQueue commandQueue = new CommandQueue();
        for (final Album a : albums) {
            commandQueue.add(getAlbumDetailsCommand(a));
        }
        final List<String[]> response = commandQueue.sendSeparated(mConnection);
        if (response.size() != albums.size()) {
            // Log.debug("MPD AlbumDetails", "non matching results "+
            // response.size()+" != "+ albums.size());
            return;
        }
        for (int i = 0; i < response.size(); i++) {
            final String[] list = response.get(i);
            final Album a = albums.get(i);
            for (final String line : list) {
                if (line.startsWith("songs: ")) {
                    a.setSongCount(Long.parseLong(line.substring("songs: ".length())));
                } else if (line.startsWith("playtime: ")) {
                    a.setDuration(Long.parseLong(line.substring("playtime: ".length())));
                }
            }
            if (findYear) {
                final List<Music> songs = getFirstTrack(a);
                if (null != songs && !songs.isEmpty()) {
                    a.setYear(songs.get(0).getDate());
                    a.setPath(songs.get(0).getPath());
                }
            }
        }
    }

    public List<Album> getAlbums(final Artist artist, final boolean trackCountNeeded)
            throws MPDServerException {
        final List<Album> albums = getAlbums(artist, trackCountNeeded, false);
        // 1. the null artist list already contains all albums
        // 2. the "unknown artist" should not list unknown albumartists
        if (artist != null && !artist.isUnknown()) {
            return Item.merged(albums, getAlbums(artist, trackCountNeeded, true));
        }
        return albums;
    }

    public List<Album> getAlbums(final Artist artist, final boolean trackCountNeeded,
            final boolean useAlbumArtist) throws MPDServerException {
        if (artist == null) {
            return getAllAlbums(trackCountNeeded);
        }
        final List<String> albumNames = listAlbums(artist.getName(), useAlbumArtist);
        final List<Album> albums = new ArrayList<>(albumNames.size());

        if (!albumNames.isEmpty()) {
            for (final String album : albumNames) {
                albums.add(new Album(album, artist, useAlbumArtist));
            }
            if (!useAlbumArtist) {
                fixAlbumArtists(albums);
            }

            // after fixing albumartists
            if (trackCountNeeded || sortAlbumsByYear()) {
                getAlbumDetails(albums, sortAlbumsByYear());
            }
            if (!sortAlbumsByYear()) {
                addAlbumPaths(albums);
            }

            Collections.sort(albums);
        }
        return albums;
    }

    /**
     * Get all albums (if there is no artist specified for filtering)
     *
     * @param trackCountNeeded Do we need the track count ?
     * @return all Albums
     */
    public List<Album> getAllAlbums(final boolean trackCountNeeded) throws MPDServerException {
        final List<Album> albums;
        // Use MPD 0.19's album grouping feature if available.
        if (mConnection.isProtocolVersionSupported(0, 19)) {
            albums = listAllAlbumsGrouped(false);
        } else {
            final List<String> albumNames = listAlbums();
            if (null != albumNames && !albumNames.isEmpty()) {
                albums = new ArrayList<>(albumNames.size());
                for (final String album : albumNames) {
                    albums.add(new Album(album, null));
                }
            } else {
                albums = new ArrayList<>(0);
            }
        }

        Collections.sort(albums);
        return albums;
    }

    public List<Artist> getArtists() throws MPDServerException {
        return Item.merged(getArtists(true), getArtists(false));
    }

    public List<Artist> getArtists(final boolean useAlbumArtist) throws MPDServerException {
        final List<String> artistNames = useAlbumArtist ? listAlbumArtists() : listArtists(true);
        final List<Artist> artists = new ArrayList<>(artistNames.size());

        if (null != artistNames && !artistNames.isEmpty()) {
            for (final String artist : artistNames) {
                artists.add(new Artist(artist));
            }
        }
        Collections.sort(artists);
        return artists;
    }

    public List<Artist> getArtists(final Genre genre) throws MPDServerException {
        return Item.merged(getArtists(genre, false), getArtists(genre, true));
    }

    public List<Artist> getArtists(final Genre genre, final boolean useAlbumArtist)
            throws MPDServerException {
        final List<String> artistNames = useAlbumArtist ? listAlbumArtists(genre) : listArtists(
                genre.getName(), true);
        final List<Artist> artists = new ArrayList<>(artistNames.size());

        if (null != artistNames && !artistNames.isEmpty()) {
            for (final String artist : artistNames) {
                artists.add(new Artist(artist));
            }
        }
        Collections.sort(artists);
        return artists;
    }

    /**
     * Retrieves a database directory listing of the base of the database directory path.
     *
     * @return a {@code Collection} of {@code Music} and
     * {@code Directory} representing directory entries.
     * @throws MPDServerException if an error occur while contacting server.
     * @see Music
     * @see Directory
     */
    public List<FilesystemTreeEntry> getDir() throws MPDServerException {
        return getDir(null);
    }

    /**
     * Retrieves a database directory listing of {@code path} directory.
     *
     * @param path Directory to be listed.
     * @return a {@code Collection} of {@code Music} and {@code Directory} representing directory
     * entries.
     * @throws MPDServerException if an error occur while contacting server.
     * @see Music
     * @see Directory
     */
    public List<FilesystemTreeEntry> getDir(final String path) throws MPDServerException {
        final List<String> response = mConnection.sendCommand(MPDCommand.MPD_CMD_LSDIR, path);
        return Directory.getDir(response, this);
    }

    protected List<Music> getFirstTrack(final Album album) throws MPDServerException {
        final Artist artist = album.getArtist();
        final String[] args = new String[6];

        if (artist == null) {
            args[0] = "";
            args[1] = "";
        } else if (album.hasAlbumArtist()) {
            args[0] = MPDCommand.MPD_TAG_ALBUM_ARTIST;
        } else {
            args[0] = MPDCommand.MPD_TAG_ARTIST;
        }

        if (artist != null) {
            args[1] = artist.getName();
        }

        args[2] = MPDCommand.MPD_TAG_ALBUM;
        args[3] = album.getName();
        args[4] = "track";
        args[5] = "1";
        List<Music> songs = find(args);
        if (null == songs || songs.isEmpty()) {
            args[5] = "01";
            songs = find(args);
        }
        if (null == songs || songs.isEmpty()) {
            args[5] = "1";
            songs = search(args);
        }
        if (null == songs || songs.isEmpty()) {
            final String[] args2 = Arrays.copyOf(args, 4); // find all tracks
            songs = find(args2);
        }
        return songs;
    }

    public List<Genre> getGenres() throws MPDServerException {
        final List<String> genreNames = listGenres();
        List<Genre> genres = null;

        if (null != genreNames && !genreNames.isEmpty()) {
            genres = new ArrayList<>(genreNames.size());
            for (final String genre : genreNames) {
                genres.add(new Genre(genre));
            }
        }
        if (null != genres) {
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

    MPDConnection getIdleConnection() {
        return mIdleConnection;
    }

    /**
     * Returns MPD server version.
     *
     * @return MPD Server version.
     */
    public String getMpdVersion() throws MPDServerException {
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
     */
    public List<MPDOutput> getOutputs() throws MPDServerException {
        final List<MPDOutput> result = new LinkedList<>();
        final List<String> response = mConnection.sendCommand(MPDCommand.MPD_CMD_OUTPUTS);

        final LinkedList<String> lineCache = new LinkedList<>();
        for (final String line : response) {
            if (line.startsWith("outputid: ")) {
                if (!lineCache.isEmpty()) {
                    result.add(new MPDOutput(lineCache));
                    lineCache.clear();
                }
            }
            lineCache.add(line);
        }

        if (!lineCache.isEmpty()) {
            result.add(new MPDOutput(lineCache));
        }

        return result;
    }

    /**
     * Retrieves {@code playlist}.
     *
     * @return playlist.
     */
    public MPDPlaylist getPlaylist() {
        return mPlaylist;
    }

    public List<Music> getPlaylistSongs(final String playlistName) throws MPDServerException {
        final String[] args = new String[1];
        args[0] = playlistName;

        return genericSearch(MPDCommand.MPD_CMD_PLAYLIST_INFO, args, false);
    }

    /**
     * Returns a list of all available playlists
     */
    public List<Item> getPlaylists() throws MPDServerException {
        return getPlaylists(false);
    }

    /**
     * Returns a list of all available playlists
     *
     * @param sort whether the return list should be sorted
     */
    public List<Item> getPlaylists(final boolean sort) throws MPDServerException {
        final List<String> response = mConnection.sendCommand(MPDCommand.MPD_CMD_LISTPLAYLISTS);
        final List<Item> result = new ArrayList<>(response.size());
        for (final String line : response) {
            if (line.startsWith("playlist")) {
                final String name = line.substring("playlist: ".length());
                if (null != name && !name.equals(STREAMS_PLAYLIST)) {
                    result.add(new PlaylistFile(name));
                }
            }
        }
        if (sort) {
            Collections.sort(result);
        }

        return result;
    }

    /**
     * Retrieves root directory.
     *
     * @return root directory.
     */
    public Directory getRootDirectory() {
        return mRootDirectory;
    }

    public List<Music> getSavedStreams() throws MPDServerException {
        final List<String> response = mConnection.sendCommand(MPDCommand.MPD_CMD_LISTPLAYLISTS);
        for (final String line : response) {
            if (line.startsWith("playlist")) {
                final String name = line.substring("playlist: ".length());
                if (STREAMS_PLAYLIST.equals(name)) {
                    final String[] args = new String[1];
                    args[0] = STREAMS_PLAYLIST;

                    return genericSearch(MPDCommand.MPD_CMD_PLAYLIST_INFO, args, false);
                }
            }
        }
        return null;
    }

    public List<Music> getSongs(final Album album) throws MPDServerException {
        final List<Music> songs = Music.getMusicFromList
                (mConnection.sendCommand(getSongsCommand(album)), true);
        if (album.hasAlbumArtist()) {
            // remove songs that don't have this albumartist
            // (mpd >=0.18 puts them in)
            final String artistname = album.getArtist().getName();
            for (int i = songs.size() - 1; i >= 0; i--) {
                final String albumartist = songs.get(i).getAlbumArtist();
                if (albumartist != null && !albumartist.isEmpty()
                        && !artistname.equals(albumartist)) {
                    songs.remove(i);
                }
            }
        }
        if (null != songs) {
            Collections.sort(songs);
        }
        return songs;
    }

    public List<Music> getSongs(final Artist artist) throws MPDServerException {
        final List<Album> albums = getAlbums(artist, false);
        final List<Music> songs = new ArrayList<>(albums.size());
        for (final Album a : albums) {
            songs.addAll(getSongs(a));
        }
        return songs;
    }

    /**
     * Retrieves the current statistics for the connected server.
     *
     * @return statistics for the connected server.
     */
    public MPDStatistics getStatistics() {
        return mStatistics;
    }

    /**
     * Retrieves status of the connected server.
     *
     * @return status of the connected server.
     */
    public MPDStatus getStatus() {
        return mStatus;
    }

    /**
     * Returns true when connected and false when not connected.
     *
     * @return true when connected and false when not connected
     */
    public boolean isConnected() {
        return mIdleConnection.isConnected();
    }

    public List<String> listAlbumArtists() throws MPDServerException {
        return listAlbumArtists(true);
    }

    /**
     * List all album artist names from database.
     *
     * @return album artist names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listAlbumArtists(final boolean sortInsensitive) throws MPDServerException {
        final List<String> response = mConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG,
                MPDCommand.MPD_TAG_ALBUM_ARTIST);

        return parseResponse(response, "albumartist", sortInsensitive);
    }

    public List<String> listAlbumArtists(final Genre genre) throws MPDServerException {
        return listAlbumArtists(genre, true);
    }

    /**
     * List all album artist names from database.
     *
     * @return album artist names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listAlbumArtists(final Genre genre, final boolean sortInsensitive)
            throws MPDServerException {
        final List<String> response = mConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG,
                MPDCommand.MPD_TAG_ALBUM_ARTIST, MPDCommand.MPD_TAG_GENRE,
                genre.getName());

        return parseResponse(response, "albumartist", sortInsensitive);
    }

    public List<String[]> listAlbumArtists(final List<Album> albums) throws MPDServerException {
        final CommandQueue commandQueue = new CommandQueue();

        for (final Album a : albums) {
            commandQueue.add(MPDCommand.MPD_CMD_LIST_TAG,
                    MPDCommand.MPD_TAG_ALBUM_ARTIST,
                    MPDCommand.MPD_TAG_ARTIST,
                    a.getArtist().getName(),
                    MPDCommand.MPD_TAG_ALBUM,
                    a.getName());
        }
        final List<String[]> response = commandQueue.sendSeparated(mConnection);
        if (response.size() != albums.size()) {
            Log.debug("MPD listAlbumArtists", "ERROR");
            return null;
        }
        for (int i = 0; i < response.size(); i++) {
            for (int j = 0; j < response.get(i).length; j++) {
                response.get(i)[j] = response.get(i)[j].substring("AlbumArtist: ".length());
            }
        }
        return response;
    }

    /**
     * List all albums from database.
     *
     * @return {@code Collection} with all album names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listAlbums() throws MPDServerException {
        return listAlbums(null, false, true);
    }

    /**
     * List all albums from database.
     *
     * @param useAlbumArtist use AlbumArtist instead of Artist
     * @return {@code Collection} with all album names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listAlbums(final boolean useAlbumArtist) throws MPDServerException {
        return listAlbums(null, useAlbumArtist, true);
    }

    /**
     * List all albums from a given artist, including an entry for songs with no
     * album tag.
     *
     * @param artist         artist to list albums
     * @param useAlbumArtist use AlbumArtist instead of Artist
     * @return {@code Collection} with all album names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listAlbums(final String artist, final boolean useAlbumArtist)
            throws MPDServerException {
        return listAlbums(artist, useAlbumArtist, true);
    }

    /**
     * List all albums from a given artist.
     *
     * @param artist              artist to list albums
     * @param useAlbumArtist      use AlbumArtist instead of Artist
     * @param includeUnknownAlbum include an entry for songs with no album tag
     * @return {@code Collection} with all album names from the given
     * artist present in database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listAlbums(final String artist, final boolean useAlbumArtist,
            final boolean includeUnknownAlbum) throws MPDServerException {
        boolean foundSongWithoutAlbum = false;

        final List<String> response =
                mConnection.sendCommand
                        (listAlbumsCommand(artist, useAlbumArtist));

        final List<String> result = new ArrayList<>(response.size());
        for (final String line : response) {
            final String name = line.substring("Album: ".length());
            if (!name.isEmpty()) {
                result.add(name);
            } else {
                foundSongWithoutAlbum = true;
            }
        }

        // add a single blank entry to host all songs without an album set
        if (includeUnknownAlbum && foundSongWithoutAlbum) {
            result.add("");
        }

        Collections.sort(result);
        return result;
    }

    /**
     * List all albums grouped by Artist/AlbumArtist
     * This method queries both Artist/AlbumArtist and tries to detect if the artist is an artist
     * or an album artist. Only the AlbumArtist query will be displayed so that the list is not
     * cluttered.
     *
     * @param includeUnknownAlbum include an entry for albums with no artists
     * @return {@code Collection} with all albums present in database, with their artist.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<Album> listAllAlbumsGrouped(final boolean includeUnknownAlbum)
            throws MPDServerException {
        final List<Album> artistAlbums = listAllAlbumsGrouped(false, includeUnknownAlbum);
        final List<Album> albumArtistAlbums = listAllAlbumsGrouped(true, includeUnknownAlbum);

        for (final Album artistAlbum : artistAlbums) {
            for (final Album albumArtistAlbum : albumArtistAlbums) {
                if (artistAlbum.getArtist() != null && artistAlbum
                        .doesNameExist(albumArtistAlbum)) {
                    albumArtistAlbum.setHasAlbumArtist(false);
                    break;
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
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<Album> listAllAlbumsGrouped(final boolean useAlbumArtist,
            final boolean includeUnknownAlbum) throws MPDServerException {
        final List<String> response =
                mConnection.sendCommand
                        (listAllAlbumsGroupedCommand(useAlbumArtist));

        final String artistResponse = useAlbumArtist ? "AlbumArtist: " : "Artist: ";
        final String albumResponse = "Album: ";

        final List<Album> result = new ArrayList<>();
        Album currentAlbum = null;
        for (final String line : response) {
            if (line.startsWith(artistResponse)) {
                // Don't make the check with the other so we don't waste time doing string
                // comparisons for nothing.
                if (currentAlbum != null) {
                    currentAlbum.setArtist(new Artist(line.substring(artistResponse.length())));
                }
            } else if (line.startsWith(albumResponse)) {
                final String name = line.substring(albumResponse.length());
                if (!name.isEmpty() || includeUnknownAlbum) {
                    currentAlbum = new Album(name, null);
                    currentAlbum.setHasAlbumArtist(useAlbumArtist);
                    result.add(currentAlbum);
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
     * discouraged, as it can retrieve so much information the server max_output_buffer_size may
     * be exceeded, which will, in turn, truncate the output to this method.
     *
     * @return List of all available music information.
     * @throws MPDServerException on no connection or failure to send command.
     */
    public List<Music> listAllInfo() throws MPDServerException {
        final List<String> allInfo = mConnection.sendCommand(MPDCommand.MPD_CMD_LISTALLINFO);
        return Music.getMusicFromList(allInfo, false);
    }

    /**
     * List all artist names from database.
     *
     * @return artist names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listArtists() throws MPDServerException {
        return listArtists(true);
    }

    /**
     * List all artist names from database.
     *
     * @param sortInsensitive boolean for insensitive sort when true
     * @return artist names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listArtists(final boolean sortInsensitive) throws MPDServerException {
        final List<String> response = mConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG,
                MPDCommand.MPD_TAG_ARTIST);

        return parseResponse(response, "Artist", sortInsensitive);
    }

    /*
     * List all albumartist or artist names of all given albums from database.
     * @return list of array of artist names for each album.
     * @throws MPDServerException if an error occurs while contacting server.
     */
    public List<String[]> listArtists(final List<Album> albums, final boolean useAlbumArtist)
            throws MPDServerException {
        if (albums == null) {
            return new ArrayList<>(0);
        }

        final CommandQueue commandQueue = new CommandQueue();
        for (final Album a : albums) {
            // When adding album artist to existing artist check that the artist
            // matches
            if (useAlbumArtist && a.getArtist() != null && !a.getArtist().isUnknown()) {
                commandQueue.add(MPDCommand.MPD_CMD_LIST_TAG,
                        MPDCommand.MPD_TAG_ALBUM_ARTIST,
                        MPDCommand.MPD_TAG_ALBUM, a.getName(),
                        MPDCommand.MPD_TAG_ARTIST, a.getArtist().getName());
            } else {
                commandQueue.add(MPDCommand.MPD_CMD_LIST_TAG,
                        (useAlbumArtist ? MPDCommand.MPD_TAG_ALBUM_ARTIST :
                                MPDCommand.MPD_TAG_ARTIST),
                        MPDCommand.MPD_TAG_ALBUM, a.getName()
                );
            }
        }

        final List<String[]> responses = commandQueue.sendSeparated(mConnection);
        final List<String[]> result = new ArrayList<>(responses.size());

        for (final String[] r : responses) {
            final ArrayList<String> albumResult = new ArrayList<>(r.length);
            for (final String s : r) {
                final String name = s
                        .substring((useAlbumArtist ? "AlbumArtist: " : "Artist: ").length());
                albumResult.add(name);
            }
            result.add(albumResult.toArray(new String[albumResult.size()]));
        }
        return result;
    }

    /**
     * List all artist names from database.
     *
     * @return artist names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listArtists(final String genre) throws MPDServerException {
        return listArtists(genre, true);
    }

    /**
     * List all artist names from database.
     *
     * @param sortInsensitive boolean for insensitive sort when true
     * @return artist names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listArtists(final String genre, final boolean sortInsensitive)
            throws MPDServerException {
        final List<String> response = mConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG,
                MPDCommand.MPD_TAG_ARTIST, MPDCommand.MPD_TAG_GENRE, genre);

        return parseResponse(response, "Artist", sortInsensitive);
    }

    /**
     * List all genre names from database.
     *
     * @return artist names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listGenres() throws MPDServerException {
        return listGenres(true);
    }

    /**
     * List all genre names from database.
     *
     * @param sortInsensitive boolean for insensitive sort when true
     * @return artist names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listGenres(final boolean sortInsensitive) throws MPDServerException {
        final List<String> response = mConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG,
                MPDCommand.MPD_TAG_GENRE);

        return parseResponse(response, "Genre", sortInsensitive);
    }

    public void movePlaylistSong(final String playlistName, final int from, final int to)
            throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_PLAYLIST_MOVE, playlistName,
                Integer.toString(from), Integer.toString(to));
    }

    /**
     * Jumps to next playlist track.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void next() throws MPDServerException {
        mConnection.sendCommand(nextCommand());
    }

    /**
     * Pauses/Resumes music playing.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void pause() throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_PAUSE);
    }

    /**
     * Starts playing music.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void play() throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_PLAY);
    }

    /**
     * Plays previous playlist music.
     *
     * @throws MPDServerException if an error occur while contacting server..
     */
    public void previous() throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_PREV);
    }

    /**
     * Tells server to refresh database.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void refreshDatabase() throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_REFRESH);
    }

    /**
     * Tells server to refresh database.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void refreshDatabase(final String folder) throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_REFRESH, folder);
    }

    public void removeFromPlaylist(final String playlistName, final Integer pos)
            throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_PLAYLIST_DEL, playlistName,
                Integer.toString(pos));
    }

    public void removeSavedStream(final Integer pos) throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_PLAYLIST_DEL, STREAMS_PLAYLIST,
                Integer.toString(pos));
    }

    public void saveStream(final String url, final String name) throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_PLAYLIST_ADD, STREAMS_PLAYLIST,
                Music.addStreamName(url, name));
    }

    /**
     * Similar to {@code find},{@code search} looks for partial
     * matches in the MPD database.
     *
     * @param type          type of search. Should be one of the following constants:
     *                      MPD_SEARCH_ARTIST, MPD_SEARCH_TITLE, MPD_SEARCH_ALBUM,
     *                      MPD_SEARCH_FILENAME
     * @param locatorString case-insensitive locator locatorString. Anything that contains
     *                      {@code locatorString} will be returned in the results.
     * @return a Collection of {@code Music}.
     * @throws MPDServerException if an error occur while contacting server.
     * @see org.a0z.mpd.item.Music
     */
    public Collection<Music> search(final String type, final String locatorString)
            throws MPDServerException {
        return genericSearch(MPDCommand.MPD_CMD_SEARCH, type, locatorString);
    }

    public List<Music> search(final String[] args) throws MPDServerException {
        return genericSearch(MPDCommand.MPD_CMD_SEARCH, args, true);
    }

    /**
     * Seeks current music to the position.
     *
     * @param position song position in seconds
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void seek(final long position) throws MPDServerException {
        seekById(mStatus.getSongId(), position);
    }

    /**
     * Seeks music to the position.
     *
     * @param songId   music id in playlist.
     * @param position song position in seconds.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void seekById(final int songId, final long position) throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_SEEK_ID, Integer.toString(songId),
                Long.toString(position));
    }

    /**
     * Seeks music to the position.
     *
     * @param index    music position in playlist.
     * @param position song position in seconds.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void seekByIndex(final int index, final long position) throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_SEEK, Integer.toString(index),
                Long.toString(position));
    }

    /**
     * Enabled or disable consuming.
     *
     * @param consume if true song consuming will be enabled, if false song
     *                consuming will be disabled.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setConsume(final boolean consume) throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_CONSUME, consume ? "1" : "0");
    }

    /**
     * Sets cross-fade.
     *
     * @param time cross-fade time in seconds. 0 to disable cross-fade.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setCrossfade(final int time) throws MPDServerException {
        mConnection
                .sendCommand(MPDCommand.MPD_CMD_CROSSFADE, Integer.toString(Math.max(0, time)));
    }

    /**
     * Enabled or disable random.
     *
     * @param random if true random will be enabled, if false random will be
     *               disabled.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setRandom(final boolean random) throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_RANDOM, random ? "1" : "0");
    }

    /**
     * Enabled or disable repeating.
     *
     * @param repeat if true repeating will be enabled, if false repeating will
     *               be disabled.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setRepeat(final boolean repeat) throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_REPEAT, repeat ? "1" : "0");
    }

    /**
     * Enabled or disable single mode.
     *
     * @param single if true single mode will be enabled, if false single mode
     *               will be disabled.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setSingle(final boolean single) throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_SINGLE, single ? "1" : "0");
    }

    /**
     * Sets volume to {@code volume}.
     *
     * @param volume new volume value, must be in 0-100 range.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setVolume(final int volume) throws MPDServerException {
        final int vol = Math.max(MPDCommand.MIN_VOLUME, Math.min(MPDCommand.MAX_VOLUME, volume));
        mConnection.sendCommand(MPDCommand.MPD_CMD_SET_VOLUME, Integer.toString(vol));
    }

    /**
     * Kills server.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void shutdown() throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_KILL);
    }

    /**
     * Skip to song with specified {@code id}.
     *
     * @param id song id.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void skipToId(final int id) throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_PLAY_ID, Integer.toString(id));
    }

    /**
     * Jumps to track {@code position} from playlist.
     *
     * @param position track number.
     * @throws MPDServerException if an error occur while contacting server.
     * @see #skipToId(int)
     */
    public void skipToPosition(final int position) throws MPDServerException {
        mConnection.sendCommand(skipToPositionCommand(position));
    }

    /**
     * Stops music playing.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void stop() throws MPDServerException {
        mConnection.sendCommand(MPDCommand.MPD_CMD_STOP);
    }

    /**
     * Updates the {@link MPDStatistics} object stored in this object. Do not call this
     * method directly unless you absolutely know what you are doing. If a long running application
     * needs a status update, use the {@code MPDStatusMonitor} instead.
     *
     * @throws MPDServerException if an error occurred while contacting the server.
     * @see MPDStatusMonitor
     */
    public void updateStatistics() throws MPDServerException {
        final List<String> response = mConnection.sendCommand(MPDCommand.MPD_CMD_STATISTICS);

        mStatistics.update(response);
    }

    /**
     * Retrieves status of the connected server. Do not call this method directly unless you
     * absolutely know what you are doing. If a long running application needs a status update, use
     * the {@code MPDStatusMonitor} instead.
     *
     * @throws MPDServerException if an error occur while contacting server.
     * @see MPDStatusMonitor
     */
    void updateStatus() throws MPDServerException {
        final List<String> response = mConnection.sendCommand(MPDCommand.MPD_CMD_STATUS);

        if (response == null) {
            Log.error(TAG, "No status response from the MPD server.");
        } else {
            mStatus.updateStatus(response);
        }
    }
}
