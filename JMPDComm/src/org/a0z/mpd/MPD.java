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

import org.a0z.mpd.exception.MPDClientException;
import org.a0z.mpd.exception.MPDConnectionException;
import org.a0z.mpd.exception.MPDServerException;

import android.content.Context;
import android.util.Log;

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

    private static final String TAG = "org.a0z.mpd.MPD";

    public static final String STREAMS_PLAYLIST = "[Radio Streams]";

    protected MPDConnection mpdConnection;

    protected MPDConnection mpdIdleConnection;

    protected MPDConnection mpdStatusConnection;

    protected MPDStatus mpdStatus;

    protected MPDPlaylist playlist;

    protected Directory rootDirectory;

    protected static boolean sortByTrackNumber = true;

    protected static boolean sortAlbumsByYear = false;

    protected static boolean showArtistAlbumCount = false;

    protected static boolean showAlbumTrackCount = true;

    protected static Context applicationContext = null;

    public static Context getApplicationContext() {
        return applicationContext;
    }

    public static void setApplicationContext(Context context) {
        applicationContext = context;
    }

    public static void setShowAlbumTrackCount(boolean v) {
        showAlbumTrackCount = v;
    }

    public static void setShowArtistAlbumCount(boolean v) {
        showArtistAlbumCount = v;
    }

    public static void setSortAlbumsByYear(boolean v) {
        sortAlbumsByYear = v;
    }

    public static void setSortByTrackNumber(boolean v) {
        sortByTrackNumber = v;
    }

    public static boolean showAlbumTrackCount() {
        return showAlbumTrackCount;
    }

    public static boolean showArtistAlbumCount() {
        return showArtistAlbumCount;
    }

    public static boolean sortAlbumsByYear() {
        return sortAlbumsByYear;
    }

    public static boolean sortByTrackNumber() {
        return sortByTrackNumber;
    }

    /**
     * Constructs a new MPD server controller without connection.
     */
    public MPD() {
        this.playlist = new MPDPlaylist(this);
        this.mpdStatus = new MPDStatus();
        this.rootDirectory = Directory.makeRootDirectory(this);
    }

    /**
     * Constructs a new MPD server controller.
     *
     * @param server server address or host name
     * @param port   server port
     * @throws MPDServerException if an error occur while contacting server
     */
    public MPD(InetAddress server, int port, String password) throws MPDServerException {
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
    public MPD(String server, int port, String password) throws MPDServerException,
            UnknownHostException {
        this();
        connect(server, port, password);
    }

    public void add(Album album) throws MPDServerException {
        add(album, false, false);
    }

    public void add(final Album album, boolean replace, boolean play) throws MPDServerException {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    final ArrayList<Music> songs = new ArrayList<>(getSongs(album));
                    getPlaylist().addAll(songs);
                } catch (final MPDServerException e) {
                    Log.e(TAG, "Failed to add.", e);
                }
            }
        };
        add(r, replace, play);
    }

    public void add(Artist artist) throws MPDServerException {
        add(artist, false, false);
    }

    public void add(final Artist artist, boolean replace, boolean play) throws MPDServerException {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    final ArrayList<Music> songs = new ArrayList<>(getSongs(artist));
                    getPlaylist().addAll(songs);
                } catch (final MPDServerException e) {
                    Log.e(TAG, "Failed to add.", e);
                }
            }
        };
        add(r, replace, play);
    }

    public void add(final Directory directory, boolean replace, boolean play)
            throws MPDServerException {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    getPlaylist().add(directory);
                } catch (final MPDServerException e) {
                    Log.e(TAG, "Failed to add.", e);
                }
            }
        };
        add(r, replace, play);
    }

    public void add(final FilesystemTreeEntry music, boolean replace, boolean play)
            throws MPDServerException {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    if (music instanceof Music) {
                        getPlaylist().add(music);

                    } else if (music instanceof PlaylistFile) {
                        getPlaylist().load(music.getFullpath());
                    }
                } catch (final MPDServerException e) {
                    Log.e(TAG, "Failed to add.", e);
                }
            }
        };
        add(r, replace, play);
    }

    public void add(Music music) throws MPDServerException {
        add(music, false, false);
    }

    /**
     * Adds songs to the queue. Optionally, clears the queue prior to the addition. Optionally,
     * play the added songs afterward.
     *
     * @param runnable The runnable that will be responsible of inserting the
     *                 songs into the queue.
     * @param replace  If true, replaces the entire playlist queue with the added files.
     * @param playAfterAdd     If true, starts playing once added.
     */
    public void add(final Runnable runnable, final boolean replace, final boolean playAfterAdd)
            throws MPDServerException {
        int playPos = 0;
        final MPDStatus status = getStatus();
        final boolean isPlaying = MPDStatus.MPD_STATE_PLAYING.equals(status.getState());
        final boolean isConsume = status.isConsume();
        final boolean isRandom = status.isRandom();

        /** Replace */
        if (replace) {
            if (isPlaying) {
                playlist.crop();
            } else {
                playlist.clear();
            }
        } else if (playAfterAdd && !isRandom) {
            /** Since we didn't clear the playlist queue, we need to play the (current queue+1) */
            playPos = playlist.size();
        }

        /** Add */
        runnable.run();

        if (replace) {
            if (isPlaying) {
                next();
            } else if (playAfterAdd) {
                skipToPosition(playPos);
            }
        } else if (playAfterAdd) {
            skipToPosition(playPos);
        }

        /** Finally, clean up the last playing song. */
        if (replace && isPlaying && !isConsume) {
            try {
                playlist.removeByIndex(0);
            } catch (final MPDServerException e) {
                Log.d(TAG, "Remove by index failed.", e);
            }
        }
    }

    public void add(String playlist) throws MPDServerException {
        add(playlist, false, false);
    }

    public void add(final String playlist, boolean replace, boolean play)
            throws MPDServerException {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    getPlaylist().load(playlist);
                } catch (final MPDServerException e) {
                    Log.e(TAG, "Failed to add playlist.", e);
                }
            }
        };
        add(r, replace, play);
    }

    public void addStream(final String stream, boolean replace, boolean play) throws MPDServerException {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    getPlaylist().addStream(stream);
                } catch (final MPDServerException | MPDClientException e) {
                    Log.e(TAG, "Failed to add stream.", e);
                }
            }
        };
        add(r, replace, play);
    }

    protected void addAlbumPaths(List<Album> albums) {
        if (albums == null || albums.isEmpty()) {
            return;
        }

        for (Album a : albums) {
            try {
                List<Music> songs = getFirstTrack(a);
                if (!songs.isEmpty()) {
                    a.setPath(songs.get(0).getPath());
                }
            } catch (MPDServerException e) {
            }
        }
    }

    // Returns a pattern where all punctuation characters are escaped.

    public void addToPlaylist(String playlistName, Album album) throws MPDServerException {
        addToPlaylist(playlistName, new ArrayList<Music>(getSongs(album)));
    }

    public void addToPlaylist(String playlistName, Artist artist) throws MPDServerException {
        addToPlaylist(playlistName, new ArrayList<Music>(getSongs(artist)));
    }

    public void addToPlaylist(String playlistName, Collection<Music> c) throws MPDServerException {
        if (null == c || c.size() < 1) {
            return;
        }
        for (Music m : c) {
            getMpdConnection().queueCommand(MPDCommand.MPD_CMD_PLAYLIST_ADD, playlistName,
                    m.getFullpath());
        }
        getMpdConnection().sendCommandQueue();
    }

    public void addToPlaylist(String playlistName, FilesystemTreeEntry entry)
            throws MPDServerException {
        getMpdConnection().sendCommand(MPDCommand.MPD_CMD_PLAYLIST_ADD, playlistName,
                entry.getFullpath());
    }

    public void addToPlaylist(String playlistName, Music music) throws MPDServerException {
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
    public void adjustVolume(int modifier) throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        // calculate final volume (clip value with [0, 100])
        int vol = getVolume() + modifier;
        vol = Math.max(MPDCommand.MIN_VOLUME, Math.min(MPDCommand.MAX_VOLUME, vol));

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_SET_VOLUME, Integer.toString(vol));
    }

    /*
     * test whether given album is in given genre
     */
    public boolean albumInGenre(Album album, Genre genre) throws MPDServerException {
        List<String> response;
        Artist artist = album.getArtist();
        response = mpdConnection.sendCommand
                (new MPDCommand(MPDCommand.MPD_CMD_LIST_TAG,
                        MPDCommand.MPD_TAG_ALBUM,
                        MPDCommand.MPD_TAG_ALBUM, album.getName(),
                        album.hasAlbumArtist() ? MPDCommand.MPD_TAG_ALBUM_ARTIST
                                : MPDCommand.MPD_TAG_ARTIST,
                        (artist == null ? "" : artist.getName()),
                        MPDCommand.MPD_TAG_GENRE, genre.getName()
                ));
        return (!response.isEmpty());
    }

    /**
     * Clears error message.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void clearError() throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_CLEARERROR);
    }

    /**
     * Connects to a MPD server.
     *
     * @param server server address or host name
     * @param port   server port
     */
    public final synchronized void connect(InetAddress server, int port, String password)
            throws MPDServerException {
        if (!isConnected()) {
            this.mpdConnection = new MPDConnectionMultiSocket(server, port, password, 5000, 3);
            this.mpdIdleConnection = new MPDConnectionMonoSocket(server, port, password, 0);
            this.mpdStatusConnection = new MPDConnectionMonoSocket(server, port, password, 5000);
        }
    }

    /**
     * Connects to a MPD server.
     *
     * @param server server address or host name
     * @param port   server port
     * @throws MPDServerException if an error occur while contacting server
     */
    public final void connect(String server, int port, String password) throws MPDServerException,
            UnknownHostException {
        InetAddress address = InetAddress.getByName(server);
        connect(address, port, password);
    }

    /**
     * Connects to a MPD server.
     *
     * @param server server address or host name and port (server:port)
     * @throws MPDServerException if an error occur while contacting server
     */
    public final void connect(String server, String password) throws MPDServerException,
            UnknownHostException {
        int port = MPDCommand.DEFAULT_MPD_PORT;
        String host;
        if (server.indexOf(':') != -1) {
            host = server.substring(0, server.lastIndexOf(':'));
            port = Integer.parseInt(server.substring(server.lastIndexOf(':') + 1));
        } else {
            host = server;
        }
        connect(host, port, password);
    }

    public void disableOutput(int id) throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_OUTPUTDISABLE, Integer.toString(id));
    }

    /**
     * Disconnects from server.
     *
     * @throws MPDServerException if an error occur while closing connection
     */
    public synchronized void disconnect() throws MPDServerException {
        MPDServerException ex = null;
        if (mpdConnection != null && mpdConnection.isConnected()) {
            try {
                mpdConnection.sendCommand(MPDCommand.MPD_CMD_CLOSE);
            } catch (MPDServerException e) {
                ex = e;
            }
        }
        if (mpdConnection != null && mpdConnection.isConnected()) {
            try {
                mpdConnection.disconnect();
            } catch (MPDServerException e) {
                ex = (ex != null) ? ex : e;// Always keep first non null
                // exception
            }
        }
        if (mpdIdleConnection != null && mpdIdleConnection.isConnected()) {
            try {
                mpdIdleConnection.disconnect();
            } catch (MPDServerException e) {
                ex = (ex != null) ? ex : e;// Always keep non null first
                // exception
            }
        }

        if (mpdStatusConnection != null && mpdStatusConnection.isConnected()) {
            try {
                mpdStatusConnection.disconnect();
            } catch (MPDServerException e) {
                ex = (ex != null) ? ex : e;// Always keep non null first
                // exception
            }
        }

        // TODO: Throw ex
    }

    public void enableOutput(int id) throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_OUTPUTENABLE, Integer.toString(id));
    }

    /**
     * Similar to {@code search},{@code find} looks for exact matches
     * in the MPD database.
     *
     * @param type   type of search. Should be one of the following constants:
     *               MPD_FIND_ARTIST, MPD_FIND_ALBUM
     * @param string case-insensitive locator string. Anything that exactly
     *               matches {@code string} will be returned in the results.
     * @return a Collection of {@code Music}
     * @throws MPDServerException if an error occur while contacting server
     * @see org.a0z.mpd.Music
     */
    public List<Music> find(String type, String string) throws MPDServerException {
        return genericSearch(MPDCommand.MPD_CMD_FIND, type, string);
    }

    public List<Music> find(String[] args) throws MPDServerException {
        return genericSearch(MPDCommand.MPD_CMD_FIND, args, true);
    }

    /*
     * For all given albums, look for albumartists and create as many albums as
     * there are albumartists, including "" The server call can be slow for long
     * album lists
     */
    protected void fixAlbumArtists(List<Album> albums) {
        if (albums == null || albums.isEmpty()) {
            return;
        }
        List<String[]> albumartists = null;
        try {
            albumartists = listAlbumArtists(albums);
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to fix album artists.", e);
        }
        if (albumartists == null || albumartists.size() != albums.size()) {
            return;
        }

        /** Split albums are rare, let it allocate as needed. */
        @SuppressWarnings("CollectionWithoutInitialCapacity")
        final Collection<Album> splitalbums = new ArrayList<>();
        int i = 0;
        for (Album a : albums) {
            String[] aartists = albumartists.get(i);
            if (aartists.length > 0) {
                Arrays.sort(aartists); // make sure "" is the first one
                if (aartists[0] != null && !aartists[0].isEmpty()) { // one albumartist, fix this
                    // album
                    a.setArtist(new Artist(aartists[0]));
                    a.setHasAlbumArtist(true);
                } // do nothing if albumartist is ""
                if (aartists.length > 1) { // it's more than one album, insert
                    for (int n = 1; n < aartists.length; n++) {
                        Album newalbum =
                                new Album(a.getName(), new Artist(aartists[n]), true);
                        splitalbums.add(newalbum);
                    }
                }
            }
            i++;
        }
        albums.addAll(splitalbums);
    }

    protected List<Music> genericSearch(String searchCommand, String args[], boolean sort)
            throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        return Music.getMusicFromList(mpdConnection.sendCommand(searchCommand, args), sort);
    }

    protected List<Music> genericSearch(String searchCommand, String type, String strToFind)
            throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        List<String> response = mpdConnection.sendCommand(searchCommand, type, strToFind);
        return Music.getMusicFromList(response, true);
    }

    public int getAlbumCount(Artist artist, boolean useAlbumArtistTag) throws MPDServerException {
        return listAlbums(artist.getName(), useAlbumArtistTag).size();
    }

    public int getAlbumCount(String artist, boolean useAlbumArtistTag) throws MPDServerException {
        if (mpdConnection == null) {
            throw new MPDServerException("MPD Connection is not established");
        }
        return listAlbums(artist, useAlbumArtistTag).size();
    }

    protected void getAlbumDetails(List<Album> albums,
            boolean findYear) throws MPDServerException {
        for (Album a : albums) {
            mpdConnection.queueCommand(getAlbumDetailsCommand(a));
        }
        List<String[]> response = mpdConnection.sendCommandQueueSeparated();
        if (response.size() != albums.size()) {
            // Log.d("MPD AlbumDetails", "non matching results "+
            // response.size()+" != "+ albums.size());
            return;
        }
        for (int i = 0; i < response.size(); i++) {
            String[] list = response.get(i);
            Album a = albums.get(i);
            for (String line : list) {
                if (line.startsWith("songs: ")) {
                    a.setSongCount(Long.parseLong(line.substring("songs: ".length())));
                } else if (line.startsWith("playtime: ")) {
                    a.setDuration(Long.parseLong(line.substring("playtime: ".length())));
                }
            }
            if (findYear) {
                List<Music> songs = getFirstTrack(a);
                if (null != songs && !songs.isEmpty()) {
                    a.setYear(songs.get(0).getDate());
                    a.setPath(songs.get(0).getPath());
                }
            }
        }
    }

    protected MPDCommand getAlbumDetailsCommand(Album album) throws MPDServerException {
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

    public List<Album> getAlbums(Artist artist, boolean trackCountNeeded)
            throws MPDServerException {
        List<Album> a_albums = getAlbums(artist, trackCountNeeded, false);
        // 1. the null artist list already contains all albums
        // 2. the "unknown artist" should not list unknown albumartists
        if (artist != null && !artist.isUnknown()) {
            return Item.merged(a_albums, getAlbums(artist, trackCountNeeded, true));
        }
        return a_albums;
    }

    public List<Album> getAlbums(Artist artist, boolean trackCountNeeded,
            boolean useAlbumArtist) throws MPDServerException {
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
            if (MPD.showAlbumTrackCount() && trackCountNeeded || MPD.sortAlbumsByYear()) {
                getAlbumDetails(albums, MPD.sortAlbumsByYear());
            }
            if (!MPD.sortAlbumsByYear()) {
                addAlbumPaths(albums);
            }

            Collections.sort(albums);
        }
        return albums;
    }

    /**
     * Get all albums (if there is no artist specified for filtering)
     * @param trackCountNeeded Do we need the track count ?
     * @return all Albums
     */
    public List<Album> getAllAlbums(final boolean trackCountNeeded) throws MPDServerException {
        final List<Album> albums;
        // Use MPD 0.19's album grouping feature if available.
        if (mpdConnection.isAlbumGroupingSupported()) {
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

    public List<Artist> getArtists(boolean useAlbumArtist) throws MPDServerException {
        List<String> artistNames = useAlbumArtist ? listAlbumArtists() : listArtists(true);
        final List<Artist> artists = new ArrayList<>(artistNames.size());

        if (null != artistNames && !artistNames.isEmpty()) {
            for (String artist : artistNames) {
                artists.add(new Artist(artist,
                        MPD.showArtistAlbumCount() ?
                                getAlbumCount(artist, useAlbumArtist) : 0
                ));
            }
        }
        Collections.sort(artists);
        return artists;
    }

    public List<Artist> getArtists(Genre genre) throws MPDServerException {
        return Item.merged(getArtists(genre, false), getArtists(genre, true));
    }

    public List<Artist> getArtists(Genre genre, boolean useAlbumArtist) throws MPDServerException {
        List<String> artistNames = useAlbumArtist ? listAlbumArtists(genre) : listArtists(
                genre.getName(), true);
        final List<Artist> artists = new ArrayList<>(artistNames.size());

        if (null != artistNames && !artistNames.isEmpty()) {
            for (String artist : artistNames) {
                artists.add(new Artist(artist,
                        MPD.showArtistAlbumCount() ?
                                getAlbumCount(artist, useAlbumArtist) : 0
                ));
            }
        }
        Collections.sort(artists);
        return artists;
    }

    /**
     * Retrieves a database directory listing of the base of the database
     * directory path.
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
     * @return a {@code Collection} of {@code Music} and
     * {@code Directory} representing directory entries.
     * @throws MPDServerException if an error occur while contacting server.
     * @see Music
     * @see Directory
     */
    public List<FilesystemTreeEntry> getDir(final String path) throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        final List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LSDIR, path);

        final LinkedList<String> lineCache = new LinkedList<>();
        final LinkedList<FilesystemTreeEntry> result = new LinkedList<>();

        // Read the response backwards so it is easier to parse
        for (int i = response.size() - 1; i >= 0; i--) {

            // If we hit anything we know is an item, consume the linecache
            final String line = response.get(i);
            final String[] lines = StringsUtils.MPD_DELIMITER.split(line, 2);
            
            switch (lines[0]) {
                case "directory":
                    result.add(rootDirectory.makeDirectory(lines[1]));
                    lineCache.clear();
                    break;
                case "file":
                    // Music requires this line to be cached too.
                    // It could be done every time but it would be a waste to add and
                    // clear immediately when we're parsing a playlist or a directory
                    lineCache.add(line);
                    result.add(new Music(lineCache));
                    lineCache.clear();
                    break;
                case "playlist":
                    result.add(new PlaylistFile(lines[1]));
                    lineCache.clear();
                    break;
                default:
                    // We're in something unsupported or in an item description, cache the lines
                    lineCache.add(line);
                    break;
            }
        }

        // Since we read the list backwards, reverse the results ordering.
        Collections.reverse(result);
        return result;
    }

    public InetAddress getHostAddress() throws MPDConnectionException {
        if (mpdConnection == null) {
            throw new MPDConnectionException("MPD Connection is null.");
        }

        return mpdConnection.getHostAddress();
    }

    public int getHostPort() throws MPDConnectionException {
        if (mpdConnection == null) {
            throw new MPDConnectionException("MPD Connection is null.");
        }

        return mpdConnection.getHostPort();
    }

    protected List<Music> getFirstTrack(Album album) throws MPDServerException {
        Artist artist = album.getArtist();
        String[] args = new String[6];
        args[0] = (artist == null ? "" : album.hasAlbumArtist() ? MPDCommand.MPD_TAG_ALBUM_ARTIST
                : MPDCommand.MPD_TAG_ARTIST);
        args[1] = (artist == null ? "" : artist.getName());
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
            String[] args2 = Arrays.copyOf(args, 4); // find all tracks
            songs = find(args2);
        }
        return songs;
    }

    public List<Genre> getGenres() throws MPDServerException {
        List<String> genreNames = listGenres();
        List<Genre> genres = null;

        if (null != genreNames && !genreNames.isEmpty()) {
            genres = new ArrayList<>(genreNames.size());
            for (String genre : genreNames) {
                genres.add(new Genre(genre));
            }
        }
        if (null != genres) {
            Collections.sort(genres);
        }
        return genres;
    }

    /**
     * Retrieves {@code MPDConnection}.
     *
     * @return {@code MPDConnection}.
     */
    MPDConnection getMpdConnection() {
        return this.mpdConnection;
    }

    MPDConnection getMpdIdleConnection() {
        return this.mpdIdleConnection;
    }

    /**
     * Returns MPD server version.
     *
     * @return MPD Server version.
     */
    public String getMpdVersion() throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        int[] version = mpdIdleConnection.getMPDVersion();

        StringBuffer sb = new StringBuffer(version.length);
        for (int i = 0; i < version.length; i++) {
            sb.append(version[i]);
            if (i < (version.length - 1)) {
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
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        List<MPDOutput> result = new LinkedList<>();
        List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_OUTPUTS);

        LinkedList<String> lineCache = new LinkedList<>();
        for (String line : response) {
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
        return this.playlist;
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
    public List<Item> getPlaylists(boolean sort) throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LISTPLAYLISTS);
        final List<Item> result = new ArrayList<>(response.size());
        for (String line : response) {
            if (line.startsWith("playlist")) {
                String name = line.substring("playlist: ".length());
                if ( null != name && !name.equals(STREAMS_PLAYLIST)) {
                    result.add(new Playlist(name));
                }
            }
        }
        if (sort) {
            Collections.sort(result);
        }

        return result;
    }

    public List<Music> getPlaylistSongs(String playlistName) throws MPDServerException {
        String args[] = new String[1];
        args[0] = playlistName;
        List<Music> music = genericSearch(MPDCommand.MPD_CMD_PLAYLIST_INFO, args, false);

        for (int i = 0; i < music.size(); ++i) {
            music.get(i).setSongId(i);
        }

        return music;
    }

    public List<Music> getSavedStreams() throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LISTPLAYLISTS);
        for (String line : response) {
            if (line.startsWith("playlist")) {
                String name = line.substring("playlist: ".length());
                if (null!=name && name.equals(STREAMS_PLAYLIST)) {
                    String args[] = new String[1];
                    args[0] = STREAMS_PLAYLIST;
                    List<Music> music = genericSearch(MPDCommand.MPD_CMD_PLAYLIST_INFO, args, false);

                    for (int i = 0; i < music.size(); ++i) {
                        music.get(i).setSongId(i);
                    }

                    return music;
                }
            }
        }
        return null;
    }

    public void saveStream(String url, String name) throws MPDServerException {
        getMpdConnection().sendCommand(MPDCommand.MPD_CMD_PLAYLIST_ADD, STREAMS_PLAYLIST,
                Music.addStreamName(url, name));
    }

    public void removeSavedStream(Integer pos) throws MPDServerException {
        getMpdConnection().sendCommand(MPDCommand.MPD_CMD_PLAYLIST_DEL, STREAMS_PLAYLIST,
                Integer.toString(pos));
    }

    public void editSavedStream(String url, String name, Integer pos) throws MPDServerException {
        removeSavedStream(pos);
        saveStream(url, name);
    }

    /**
     * Retrieves root directory.
     *
     * @return root directory.
     */
    public Directory getRootDirectory() {
        return rootDirectory;
    }

    public List<Music> getSongs(Album album) throws MPDServerException {
        List<Music> songs = Music.getMusicFromList
                (getMpdConnection().sendCommand(getSongsCommand(album)), true);
        if (album.hasAlbumArtist()) {
            // remove songs that don't have this albumartist
            // (mpd >=0.18 puts them in)
            String artistname = album.getArtist().getName();
            for (int i = songs.size() - 1; i >= 0; i--) {
                final String albumartist = songs.get(i).getAlbumArtist();
                if (albumartist != null && !albumartist.isEmpty()
                        && !(artistname.equals(albumartist))) {
                    songs.remove(i);
                }
            }
        }
        if (null != songs) {
            Collections.sort(songs);
        }
        return songs;
    }

    public List<Music> getSongs(Artist artist) throws MPDServerException {
        List<Album> albums = getAlbums(artist, false);
        final List<Music> songs = new ArrayList<>(albums.size());
        for (Album a : albums) {
            songs.addAll(getSongs(a));
        }
        return songs;
    }

    public MPDCommand getSongsCommand(Album album) {
        String albumname = album.getName();
        Artist artist = album.getArtist();
        if (null == artist) { // get songs for ANY artist
            return new MPDCommand(MPDCommand.MPD_CMD_FIND,
                    MPDCommand.MPD_TAG_ALBUM, albumname);
        }
        String artistname = artist.getName();
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

    /**
     * Retrieves statistics for the connected server.
     *
     * @return statistics for the connected server.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public MPDStatistics getStatistics() throws MPDServerException {
        if (!isConnected()) {
            throw new MPDConnectionException("MPD Connection is not established");
        }

        List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_STATISTICS);
        return new MPDStatistics(response);
    }

    /**
     * Retrieves status of the connected server.
     *
     * @return status of the connected server.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public MPDStatus getStatus() throws MPDServerException {
        return getStatus(false);
    }

    /**
     * Retrieves status of the connected server.
     *
     * @return status of the connected server.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public MPDStatus getStatus(boolean forceRefresh) throws MPDServerException {
        if (forceRefresh || mpdStatus == null || mpdStatus.getState() == null) {
            if (!isConnected()) {
                throw new MPDConnectionException("MPD Connection is not established");
            }
            List<String> response = mpdStatusConnection.sendCommand(MPDCommand.MPD_CMD_STATUS);
            if (response == null) {
                Log.w(TAG, "No status response from the MPD server.");
            } else {
                mpdStatus.updateStatus(response);
            }
        }
        return mpdStatus;
    }

    /**
     * Retrieves current volume.
     *
     * @return current volume.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public int getVolume() throws MPDServerException {
        return this.getStatus().getVolume();
    }

    /**
     * Returns true when connected and false when not connected.
     *
     * @return true when connected and false when not connected
     */
    public boolean isConnected() {
        return mpdIdleConnection != null && mpdStatusConnection != null && mpdConnection != null
                && mpdIdleConnection.isConnected();
    }

    public boolean isMpdConnectionNull() {
        return (this.mpdConnection == null);
    }

    public List<String> listAlbumArtists() throws MPDServerException {
        return listAlbumArtists(true);
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

    /**
     * List all album artist names from database.
     *
     * @return album artist names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listAlbumArtists(final boolean sortInsensitive) throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        final List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG,
                MPDCommand.MPD_TAG_ALBUM_ARTIST);

        return parseResponse(response, "albumartist", sortInsensitive);
    }

    public List<String> listAlbumArtists(Genre genre) throws MPDServerException {
        return listAlbumArtists(genre, true);
    }

    /**
     * List all album artist names from database.
     *
     * @return album artist names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listAlbumArtists(Genre genre, boolean sortInsensitive)
            throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        final List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG,
                MPDCommand.MPD_TAG_ALBUM_ARTIST, MPDCommand.MPD_TAG_GENRE,
                genre.getName());

        return parseResponse(response, "albumartist", sortInsensitive);
    }

    public List<String[]> listAlbumArtists(List<Album> albums) throws MPDServerException {
        for (Album a : albums) {
            mpdConnection.queueCommand(new MPDCommand(MPDCommand.MPD_CMD_LIST_TAG,
                    MPDCommand.MPD_TAG_ALBUM_ARTIST,
                    MPDCommand.MPD_TAG_ARTIST,
                    a.getArtist().getName(),
                    MPDCommand.MPD_TAG_ALBUM,
                    a.getName()));
        }
        List<String[]> response = mpdConnection.sendCommandQueueSeparated();
        if (response.size() != albums.size()) {
            Log.d("MPD listAlbumArtists", "ERROR");
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
    public List<String> listAlbums(boolean useAlbumArtist) throws MPDServerException {
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
    public List<String> listAlbums(String artist, boolean useAlbumArtist)
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
    public List<String> listAlbums(String artist, boolean useAlbumArtist,
            boolean includeUnknownAlbum) throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        boolean foundSongWithoutAlbum = false;

        List<String> response =
                mpdConnection.sendCommand
                        (listAlbumsCommand(artist, useAlbumArtist));

        final List<String> result = new ArrayList<>(response.size());
        for (String line : response) {
            String name = line.substring("Album: ".length());
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

    /*
     * get raw command String for listAlbums
     */
    public MPDCommand listAlbumsCommand(String artist, boolean useAlbumArtist) {
        if (useAlbumArtist) {
            return new MPDCommand(MPDCommand.MPD_CMD_LIST_TAG, MPDCommand.MPD_TAG_ALBUM,
                    MPDCommand.MPD_TAG_ALBUM_ARTIST, artist);
        } else {
            return new MPDCommand(MPDCommand.MPD_CMD_LIST_TAG, MPDCommand.MPD_TAG_ALBUM, artist);
        }
    }

    /**
     * List all albums grouped by Artist/AlbumArtist
     * This method queries both Artist/AlbumArtist and tries to detect if the artist is an artist
     * or an album artist. Only the AlbumArtist query will be displayed so that the list is not
     * cluttered.
     *
     * @param includeUnknownAlbum include an entry for albums with no artists
     * @return <code>Collection</code> with all albums present in database, with their artist.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<Album> listAllAlbumsGrouped(boolean includeUnknownAlbum) throws MPDServerException {
        List<Album> artistAlbums = listAllAlbumsGrouped(false, includeUnknownAlbum);
        List<Album> albumArtistAlbums = listAllAlbumsGrouped(true, includeUnknownAlbum);

        for (Album artistAlbum : artistAlbums) {
            for (Album albumArtistAlbum : albumArtistAlbums) {
                if (artistAlbum.getArtist() != null && artistAlbum.nameEquals(albumArtistAlbum)) {
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
     * @param useAlbumArtist use AlbumArtist instead of Artist
     * @param includeUnknownAlbum include an entry for albums with no artists
     * @return {@code Collection} with all albums present in database, with their artist.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<Album> listAllAlbumsGrouped(boolean useAlbumArtist,
            boolean includeUnknownAlbum) throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        List<String> response =
                mpdConnection.sendCommand
                        (listAllAlbumsGroupedCommand(useAlbumArtist));

        final String artistResponse = useAlbumArtist ? "AlbumArtist: " : "Artist: ";
        final String albumResponse = "Album: ";

        ArrayList<Album> result = new ArrayList<>();
        Album currentAlbum = null;
        for (String line : response) {
            if (line.startsWith(artistResponse)) {
                // Don't make the check with the other so we don't waste time doing string
                // comparisons for nothing.
                if (currentAlbum != null) {
                    currentAlbum.setArtist(new Artist(line.substring(artistResponse.length())));
                }
            } else if (line.startsWith(albumResponse)) {
                String name = line.substring(albumResponse.length());
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

    /*
     * get raw command String for listAllAlbumsGrouped
     */
    public MPDCommand listAllAlbumsGroupedCommand(boolean useAlbumArtist) {
        final String artistTag = useAlbumArtist ? MPDCommand.MPD_TAG_ALBUM_ARTIST :
                MPDCommand.MPD_TAG_ARTIST;
        return new MPDCommand(MPDCommand.MPD_CMD_LIST_TAG, MPDCommand.MPD_TAG_ALBUM,
                MPDCommand.MPD_CMD_GROUP, artistTag);
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
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        final List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG,
                MPDCommand.MPD_TAG_ARTIST);

        return parseResponse(response, "Artist", sortInsensitive);
    }

    /*
     * List all albumartist or artist names of all given albums from database.
     * @return list of array of artist names for each album.
     * @throws MPDServerException if an error occurs while contacting server.
     */
    public List<String[]> listArtists(List<Album> albums, boolean albumArtist)
            throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        if (albums == null) {
            return new ArrayList<>(0);
        }
        for (Album a : albums) {
            // When adding album artist to existing artist check that the artist
            // matches
            if (albumArtist && a.getArtist() != null && !a.getArtist().isUnknown()) {
                mpdConnection.queueCommand
                        (new MPDCommand(MPDCommand.MPD_CMD_LIST_TAG,
                                MPDCommand.MPD_TAG_ALBUM_ARTIST,
                                MPDCommand.MPD_TAG_ALBUM, a.getName(),
                                MPDCommand.MPD_TAG_ARTIST, a.getArtist().getName()));
            } else {
                mpdConnection.queueCommand
                        (new MPDCommand(MPDCommand.MPD_CMD_LIST_TAG,
                                (albumArtist ? MPDCommand.MPD_TAG_ALBUM_ARTIST :
                                        MPDCommand.MPD_TAG_ARTIST), MPDCommand.MPD_TAG_ALBUM, a
                                .getName()
                        ));
            }
        }

        List<String[]> responses = mpdConnection.sendCommandQueueSeparated();
        final List<String[]> result = new ArrayList<>(responses.size());

        for (String[] r : responses) {
            final ArrayList<String> albumResult = new ArrayList<>(r.length);
            for (String s : r) {
                String name = s.substring((albumArtist ? "AlbumArtist: " : "Artist: ").length());
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
    public List<String> listArtists(String genre) throws MPDServerException {
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
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        final List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG,
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
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        final List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG,
                MPDCommand.MPD_TAG_GENRE);

        return parseResponse(response, "Genre", sortInsensitive);
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
        if (!isConnected()) {
            throw new MPDConnectionException("MPD Connection is not established.");
        }

        final List<String> allInfo = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LISTALLINFO);
        return Music.getMusicFromList(allInfo, false);
    }

    public void movePlaylistSong(String playlistName, int from, int to) throws MPDServerException {
        getMpdConnection().sendCommand(MPDCommand.MPD_CMD_PLAYLIST_MOVE, playlistName,
                Integer.toString(from), Integer.toString(to));
    }

    /**
     * Jumps to next playlist track.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void next() throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_NEXT);
    }

    /**
     * Pauses/Resumes music playing.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void pause() throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_PAUSE);
    }

    /**
     * Starts playing music.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void play() throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_PLAY);
    }

    /**
     * Plays previous playlist music.
     *
     * @throws MPDServerException if an error occur while contacting server..
     */
    public void previous() throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_PREV);
    }

    /**
     * Tells server to refresh database.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void refreshDatabase() throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_REFRESH);
    }

    /**
     * Tells server to refresh database.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void refreshDatabase(String folder) throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_REFRESH, folder);
    }

    public void removeFromPlaylist(String playlistName, Integer pos) throws MPDServerException {
        getMpdConnection().sendCommand(MPDCommand.MPD_CMD_PLAYLIST_DEL, playlistName,
                Integer.toString(pos));
    }

    /**
     * Similar to {@code find},{@code search} looks for partial
     * matches in the MPD database.
     *
     * @param type   type of search. Should be one of the following constants:
     *               MPD_SEARCH_ARTIST, MPD_SEARCH_TITLE, MPD_SEARCH_ALBUM,
     *               MPD_SEARCH_FILENAME
     * @param string case-insensitive locator string. Anything that contains
     *               {@code string} will be returned in the results.
     * @return a Collection of {@code Music}.
     * @throws MPDServerException if an error occur while contacting server.
     * @see org.a0z.mpd.Music
     */
    public Collection<Music> search(String type, String string) throws MPDServerException {
        return genericSearch(MPDCommand.MPD_CMD_SEARCH, type, string);
    }

    public List<Music> search(String[] args) throws MPDServerException {
        return genericSearch(MPDCommand.MPD_CMD_SEARCH, args, true);
    }

    /**
     * Seeks current music to the position.
     *
     * @param position song position in seconds
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void seek(long position) throws MPDServerException {
        seekById(this.getStatus().getSongId(), position);
    }

    /**
     * Seeks music to the position.
     *
     * @param songId   music id in playlist.
     * @param position song position in seconds.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void seekById(int songId, long position) throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_SEEK_ID, Integer.toString(songId),
                Long.toString(position));
    }

    /**
     * Seeks music to the position.
     *
     * @param index    music position in playlist.
     * @param position song position in seconds.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void seekByIndex(int index, long position) throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_SEEK, Integer.toString(index),
                Long.toString(position));
    }

    /**
     * Enabled or disable consuming.
     *
     * @param consume if true song consuming will be enabled, if false song
     *                consuming will be disabled.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setConsume(boolean consume) throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_CONSUME, consume ? "1" : "0");
    }

    /**
     * Sets cross-fade.
     *
     * @param time cross-fade time in seconds. 0 to disable cross-fade.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setCrossfade(int time) throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection
                .sendCommand(MPDCommand.MPD_CMD_CROSSFADE, Integer.toString(Math.max(0, time)));
    }

    /**
     * Enabled or disable random.
     *
     * @param random if true random will be enabled, if false random will be
     *               disabled.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setRandom(boolean random) throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_RANDOM, random ? "1" : "0");
    }

    /**
     * Enabled or disable repeating.
     *
     * @param repeat if true repeating will be enabled, if false repeating will
     *               be disabled.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setRepeat(boolean repeat) throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_REPEAT, repeat ? "1" : "0");
    }

    /**
     * Enabled or disable single mode.
     *
     * @param single if true single mode will be enabled, if false single mode
     *               will be disabled.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setSingle(boolean single) throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_SINGLE, single ? "1" : "0");
    }

    /**
     * Sets volume to {@code volume}.
     *
     * @param volume new volume value, must be in 0-100 range.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setVolume(int volume) throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        int vol = Math.max(MPDCommand.MIN_VOLUME, Math.min(MPDCommand.MAX_VOLUME, volume));
        mpdConnection.sendCommand(MPDCommand.MPD_CMD_SET_VOLUME, Integer.toString(vol));
    }

    /**
     * Kills server.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void shutdown() throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_KILL);
    }

    /**
     * Skip to song with specified {@code id}.
     *
     * @param id song id.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void skipToId(int id) throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_PLAY_ID, Integer.toString(id));
    }

    /**
     * Jumps to track {@code position} from playlist.
     *
     * @param position track number.
     * @throws MPDServerException if an error occur while contacting server.
     * @see #skipToId(int)
     */
    public void skipToPosition(int position) throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_PLAY, Integer.toString(position));
    }

    /**
     * Stops music playing.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void stop() throws MPDServerException {
        if (!isConnected()) {
            throw new MPDServerException("MPD Connection is not established");
        }

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_STOP);
    }

    /**
     * Wait for server changes using "idle" command on the dedicated connection.
     *
     * @return Data read from the server.
     * @throws MPDServerException if an error occur while contacting server
     */
    public List<String> waitForChanges() throws MPDServerException {

        while (mpdIdleConnection != null && mpdIdleConnection.isConnected()) {
            List<String> data = mpdIdleConnection
                    .sendAsyncCommand(MPDCommand.MPD_CMD_IDLE);
            if (data.isEmpty()) {
                continue;
            }
            return data;
        }
        throw new MPDConnectionException("IDLE connection lost");
    }

}
