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
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * MPD Server controller.
 * 
 * @version $Id: MPD.java 2716 2004-11-20 17:37:20Z galmeida $
 */
public class MPD {

    private final static String TAG = "org.a0z.mpd.MPD";

    protected MPDConnection mpdConnection;
    protected MPDConnection mpdIdleConnection;

    protected MPDConnection mpdStatusConnection;
    protected MPDStatus mpdStatus;
    protected MPDPlaylist playlist;

    protected Directory rootDirectory;
    static protected boolean sortByTrackNumber = true;
    static protected boolean sortAlbumsByYear = false;
    static protected boolean showArtistAlbumCount = false;

    static protected boolean showAlbumTrackCount = true;

    static protected Context applicationContext = null;

    static public Context getApplicationContext() {
        return applicationContext;
    }
    static public void setApplicationContext(Context context) {
        applicationContext = context;
    }

    static public void setShowAlbumTrackCount(boolean v) {
        showAlbumTrackCount = v;
    }

    static public void setShowArtistAlbumCount(boolean v) {
        showArtistAlbumCount = v;
    }

    static public void setSortAlbumsByYear(boolean v) {
        sortAlbumsByYear = v;
    }

    static public void setSortByTrackNumber(boolean v) {
        sortByTrackNumber = v;
    }

    static public boolean showAlbumTrackCount() {
        return showAlbumTrackCount;
    }

    static public boolean showArtistAlbumCount() {
        return showArtistAlbumCount;
    }

    static public boolean sortAlbumsByYear() {
        return sortAlbumsByYear;
    }

    static public boolean sortByTrackNumber() {
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
     * @param port server port
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
     * @param port server port
     * @throws MPDServerException if an error occur while contacting server
     * @throws UnknownHostException
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
                    final ArrayList<Music> songs = new ArrayList<Music>(getSongs(album));
                    getPlaylist().addAll(songs);
                } catch (MPDServerException e) {
                    e.printStackTrace();
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
                    final ArrayList<Music> songs = new ArrayList<Music>(getSongs(artist));
                    getPlaylist().addAll(songs);
                } catch (MPDServerException e) {
                    e.printStackTrace();
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
                } catch (MPDServerException e) {
                    e.printStackTrace();
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
                } catch (MPDServerException e) {
                    e.printStackTrace();
                }
            }
        };
        add(r, replace, play);
    }

    public void add(Music music) throws MPDServerException {
        add(music, false, false);
    }

    /**
     * Adds songs to the queue. It is possible to request a clear of the current
     * one, and to start the playback once done.
     *
     * @param runnable The runnable that will be responsible of inserting the
     *                 songs into the queue
     * @param replace  If true, replaces the entire playlist queue with the added files
     * @param play     If true, starts playing once added
     */
    public void add(Runnable runnable, boolean replace, boolean play) throws MPDServerException {
        int playPos = 0;
        final boolean isPlaying = MPDStatus.MPD_STATE_PLAYING.equals(getStatus().getState());

        if (replace) {
            if (isPlaying) {
                playPos = 1;
                getPlaylist().crop();
            } else {
                getPlaylist().clear();
            }
        } else if (play) {
            playPos = getPlaylist().size();
        }

        runnable.run();

        if (play || (replace && isPlaying)) {
            skipToPosition(playPos);
        }

        /** Finally, clean up the last playing song. */
        if (replace && isPlaying) {
            try {
                getPlaylist().removeByIndex(0);
            } catch (MPDServerException e) {
                Log.d(TAG, "Remove by index failed.", e);
            }
        }
    }

    public void add(String playlist) throws MPDServerException {
        add(playlist, false, false);
    }

    public void add(final String playlist, boolean replace, boolean play) throws MPDServerException {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    getPlaylist().load(playlist);
                } catch (MPDServerException e) {
                    e.printStackTrace();
                }
            }
        };
        add(r, replace, play);
    }

    public void add(final URL stream, boolean replace, boolean play) throws MPDServerException {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    getPlaylist().add(stream);
                } catch (MPDServerException | MPDClientException e) {
                    e.printStackTrace();
                }
            }
        };
        add(r, replace, play);
    }

    protected void addAlbumPaths(List<Album> albums) {
        if (albums == null || albums.size() == 0) {
            return;
        }

        for (Album a : albums) {
            try {
                List<Music> songs = getFirstTrack(a);
                if (songs.size() > 0) {
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
        final ArrayList<Music> songs = new ArrayList<Music>();
        songs.add(music);
        addToPlaylist(playlistName, songs);
    }

    /**
     * Increases or decreases volume by <code>modifier</code> amount.
     * 
     * @param modifier volume adjustment
     * @throws MPDServerException if an error occur while contacting server
     */
    public void adjustVolume(int modifier) throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

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
                        MPDCommand.MPD_TAG_GENRE, genre.getName()));
        return (response.size() > 0);
    }

    /**
     * Clears error message.
     * 
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void clearError() throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_CLEARERROR);
    }

    /**
     * Connects to a MPD server.
     * 
     * @param server server address or host name
     * @param port server port
     */
    public synchronized final void connect(InetAddress server, int port, String password)
            throws MPDServerException {
        if (!isConnected()) {
            this.mpdConnection = new MPDConnectionMultiSocket(server, port, 3, password, 5000);
            this.mpdIdleConnection = new MPDConnectionMonoSocket(server, port, password, 0);
            this.mpdStatusConnection = new MPDConnectionMonoSocket(server, port, password, 5000);
        }
    }

    /**
     * Connects to a MPD server.
     * 
     * @param server server address or host name
     * @param port server port
     * @throws MPDServerException if an error occur while contacting server
     * @throws UnknownHostException
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
     * @throws UnknownHostException
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
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

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
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_OUTPUTENABLE, Integer.toString(id));
    }

    /**
     * Similar to <code>search</code>,<code>find</code> looks for exact matches
     * in the MPD database.
     * 
     * @param type type of search. Should be one of the following constants:
     *            MPD_FIND_ARTIST, MPD_FIND_ALBUM
     * @param string case-insensitive locator string. Anything that exactly
     *            matches <code>string</code> will be returned in the results.
     * @return a Collection of <code>Music</code>
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
        if (albums == null || albums.size() == 0) {
            return;
        }
        List<String[]> albumartists = null;
        try {
            albumartists = listAlbumArtists(albums);
        } catch (MPDServerException e) {
        }
        if (albumartists == null || albumartists.size() != albums.size()) {
            return;
        }
        List<Album> splitalbums = new ArrayList<Album>();
        int i = 0;
        for (Album a : albums) {
            String[] aartists = albumartists.get(i);
            if (aartists.length > 0) {
                Arrays.sort(aartists); // make sure "" is the first one
                if (!"".equals(aartists[0])) { // one albumartist, fix this
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
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        return Music.getMusicFromList(mpdConnection.sendCommand(searchCommand, args), sort);
    }

    protected List<Music> genericSearch(String searchCommand, String type, String strToFind)
            throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

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
                    MPDCommand.MPD_TAG_ARTIST, album.getArtist().getName(),
                    MPDCommand.MPD_TAG_ALBUM_ARTIST, "");
        }
    }

    public List<Album> getAlbums(Artist artist, boolean trackCountNeeded) throws MPDServerException {
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
        List<String> albumNames = listAlbums(artist.getName(), useAlbumArtist);
        List<Album> albums = new ArrayList<Album>();

        if (null == albumNames || albumNames.isEmpty()) {
            return albums;
        }

        for (String album : albumNames) {
            albums.add(new Album(album, artist, useAlbumArtist));
        }
        if (!useAlbumArtist) {
            fixAlbumArtists(albums);
        }

        // after fixing albumartists
        if (((MPD.showAlbumTrackCount() && trackCountNeeded) || MPD.sortAlbumsByYear())) {
            getAlbumDetails(albums, MPD.sortAlbumsByYear());
        }
        if (!MPD.sortAlbumsByYear()) {
            addAlbumPaths(albums);
        }

        Collections.sort(albums);
        return albums;
    }

    /**
     * @return all Albums
     */
    public List<Album> getAllAlbums(boolean trackCountNeeded) throws MPDServerException {
        List<String> albumNames = listAlbums();
        List<Album> albums = new ArrayList<Album>();
        if (null == albumNames || albumNames.isEmpty()) {
            return albums; // empty list
        }
        for (String album : albumNames) {
            albums.add(new Album(album, null));
        }
        Collections.sort(albums);
        return albums;
    }

    public List<Artist> getArtists() throws MPDServerException {
        return Item.merged(getArtists(true), getArtists(false));
    }

    public List<Artist> getArtists(boolean useAlbumArtist) throws MPDServerException {
        List<String> artistNames = useAlbumArtist ? listAlbumArtists() : listArtists(true);
        List<Artist> artists = new ArrayList<Artist>();

        if (null != artistNames && !artistNames.isEmpty()) {
            for (String artist : artistNames) {
                artists.add(new Artist(artist,
                        MPD.showArtistAlbumCount() ?
                                getAlbumCount(artist, useAlbumArtist) : 0));
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
        List<Artist> artists = new ArrayList<Artist>();

        if (null != artistNames && !artistNames.isEmpty()) {
            for (String artist : artistNames) {
                artists.add(new Artist(artist,
                        MPD.showArtistAlbumCount() ?
                                getAlbumCount(artist, useAlbumArtist) : 0));
            }
        }
        Collections.sort(artists);
        return artists;
    }

    /**
     * Retrieves a database directory listing of the base of the database
     * directory path.
     * 
     * @return a <code>Collection</code> of <code>Music</code> and
     *         <code>Directory</code> representing directory entries.
     * @throws MPDServerException if an error occur while contacting server.
     * @see Music
     * @see Directory
     */
    public List<FilesystemTreeEntry> getDir() throws MPDServerException {
        return getDir(null);
    }

    /**
     * Retrieves a database directory listing of <code>path</code> directory.
     * 
     * @param path Directory to be listed.
     * @return a <code>Collection</code> of <code>Music</code> and
     *         <code>Directory</code> representing directory entries.
     * @throws MPDServerException if an error occur while contacting server.
     * @see Music
     * @see Directory
     */
    public List<FilesystemTreeEntry> getDir(String path) throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LSDIR, path);

        LinkedList<String> lineCache = new LinkedList<String>();
        LinkedList<FilesystemTreeEntry> result = new LinkedList<FilesystemTreeEntry>();
        for (String line : response) {

            // If we detect a new file element and the line cache isn't empty
            // dump the linecache into a music item
            if (line.startsWith("file: ") && lineCache.size() > 0) {
                result.add(new Music(lineCache));
                lineCache.clear();
            }

            if (line.startsWith("playlist: ")) {
                lineCache.clear();
                line = line.substring("playlist: ".length());
                result.add(new PlaylistFile(line));
            } else if (line.startsWith("directory: ")) {
                lineCache.clear();
                line = line.substring("directory: ".length());
                result.add(rootDirectory.makeDirectory(line));
            } else {
                lineCache.add(line);
            }

        }
        if (lineCache.size() > 0) {
            // Don't create a music object if the line cache does not contain any
            // It can happen for playlist and directory items with supplementary information
            for (String line : lineCache) {
                if (line.startsWith("file: ")) {
                    result.add(new Music(lineCache));
                    break;
                }
            }
        }

        return result;
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
            genres = new ArrayList<Genre>();
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
     * Retrieves <code>MPDConnection</code>.
     * 
     * @return <code>MPDConnection</code>.
     */
    public MPDConnection getMpdConnection() {
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
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        int[] version = mpdIdleConnection.getMpdVersion();

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < version.length; i++) {
            sb.append(version[i]);
            if (i < (version.length - 1))
                sb.append(".");
        }
        return sb.toString();
    }

    /**
     * Returns the available outputs
     * 
     * @return List of available outputs
     */
    public List<MPDOutput> getOutputs() throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        List<MPDOutput> result = new LinkedList<MPDOutput>();
        List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_OUTPUTS);

        LinkedList<String> lineCache = new LinkedList<String>();
        for (String line : response) {
            if (line.startsWith("outputid: ")) {
                if (lineCache.size() != 0) {
                    result.add(new MPDOutput(lineCache));
                    lineCache.clear();
                }
            }
            lineCache.add(line);
        }

        if (lineCache.size() != 0) {
            result.add(new MPDOutput(lineCache));
        }

        return result;
    }

    /**
     * Retrieves <code>playlist</code>.
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
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        List<Item> result = new ArrayList<Item>();
        List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LISTPLAYLISTS);
        for (String line : response) {
            if (line.startsWith("playlist"))
                result.add(new Playlist(line.substring("playlist: ".length())));
        }
        if (sort)
            Collections.sort(result);

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
                if (!(artistname.equals(songs.get(i).getAlbumArtist()))) {
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
        List<Music> songs = new ArrayList<Music>();
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
                    MPDCommand.MPD_TAG_ARTIST, artistname,
                    MPDCommand.MPD_TAG_ALBUM_ARTIST, "");
        }
    }

    /**
     * Retrieves statistics for the connected server.
     * 
     * @return statistics for the connected server.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public MPDStatistics getStatistics() throws MPDServerException {
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
            if(response == null) {
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
     * List all album artist names from database.
     * 
     * @return album artist names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listAlbumArtists(boolean sortInsensitive) throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG,
                MPDCommand.MPD_TAG_ALBUM_ARTIST);

        ArrayList<String> result = new ArrayList<String>();
        for (String s : response) {
            String name = s.substring("albumartist: ".length());
            result.add(name);
        }
        if (sortInsensitive)
            Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        else
            Collections.sort(result);
        return result;
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
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG,
                MPDCommand.MPD_TAG_ALBUM_ARTIST, MPDCommand.MPD_TAG_GENRE,
                genre.getName());

        ArrayList<String> result = new ArrayList<String>();
        for (String s : response) {
            String name = s.substring("albumartist: ".length());
            result.add(name);
        }
        if (sortInsensitive)
            Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        else
            Collections.sort(result);
        return result;
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
     * @return <code>Collection</code> with all album names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listAlbums() throws MPDServerException {
        return listAlbums(null, false, true);
    }

    /**
     * List all albums from database.
     * 
     * @param useAlbumArtist use AlbumArtist instead of Artist
     * @return <code>Collection</code> with all album names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listAlbums(boolean useAlbumArtist) throws MPDServerException {
        return listAlbums(null, useAlbumArtist, true);
    }

    /**
     * List all albums from a given artist, including an entry for songs with no
     * album tag.
     * 
     * @param artist artist to list albums
     * @param useAlbumArtist use AlbumArtist instead of Artist
     * @return <code>Collection</code> with all album names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listAlbums(String artist, boolean useAlbumArtist) throws MPDServerException {
        return listAlbums(artist, useAlbumArtist, true);
    }

    /**
     * List all albums from a given artist.
     * 
     * @param artist artist to list albums
     * @param useAlbumArtist use AlbumArtist instead of Artist
     * @param includeUnknownAlbum include an entry for songs with no album tag
     * @return <code>Collection</code> with all album names from the given
     *         artist present in database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public List<String> listAlbums(String artist, boolean useAlbumArtist,
            boolean includeUnknownAlbum) throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        boolean foundSongWithoutAlbum = false;

        List<String> response =
                mpdConnection.sendCommand
                        (listAlbumsCommand(artist, useAlbumArtist));

        ArrayList<String> result = new ArrayList<String>();
        for (String line : response) {
            String name = line.substring("Album: ".length());
            if (name.length() > 0) {
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
    public List<String> listArtists(boolean sortInsensitive) throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG,
                MPDCommand.MPD_TAG_ARTIST);

        ArrayList<String> result = new ArrayList<String>();
        for (String s : response) {
            result.add(s.substring("Artist: ".length()));
        }
        if (sortInsensitive)
            Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        else
            Collections.sort(result);

        return result;
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
        ArrayList<String[]> result = new ArrayList<String[]>();
        if (albums == null) {
            return result;
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
                                        .getName()));
            }
        }

        List<String[]> responses = mpdConnection.sendCommandQueueSeparated();

        for (String[] r : responses) {
            ArrayList<String> albumresult = new ArrayList<String>();
            for (String s : r) {
                String name = s.substring((albumArtist ? "AlbumArtist: " : "Artist: ").length());
                albumresult.add(name);
            }
            result.add(albumresult.toArray(new String[albumresult.size()]));
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
    public List<String> listArtists(String genre, boolean sortInsensitive)
            throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG,
                MPDCommand.MPD_TAG_ARTIST, MPDCommand.MPD_TAG_GENRE, genre);

        ArrayList<String> result = new ArrayList<String>();
        for (String s : response) {
            String name = s.substring("Artist: ".length());
            result.add(name);
        }
        if (sortInsensitive)
            Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        else
            Collections.sort(result);

        return result;
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
    public List<String> listGenres(boolean sortInsensitive) throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG,
                MPDCommand.MPD_TAG_GENRE);

        ArrayList<String> result = new ArrayList<String>();
        for (String s : response) {
            String name = s.substring("Genre: ".length());
            result.add(name);
        }
        if (sortInsensitive)
            Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        else
            Collections.sort(result);

        return result;
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
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_NEXT);
    }

    /**
     * Pauses/Resumes music playing.
     * 
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void pause() throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_PAUSE);
    }

    /**
     * Starts playing music.
     * 
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void play() throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_PLAY);
    }

    /**
     * Plays previous playlist music.
     * 
     * @throws MPDServerException if an error occur while contacting server..
     */
    public void previous() throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_PREV);
    }

    /**
     * Tells server to refresh database.
     * 
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void refreshDatabase() throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_REFRESH);
    }

    /**
     * Tells server to refresh database.
     *
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void refreshDatabase(String folder) throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_REFRESH, folder);
    }

    public void removeFromPlaylist(String playlistName, Integer pos) throws MPDServerException {
        getMpdConnection().sendCommand(MPDCommand.MPD_CMD_PLAYLIST_DEL, playlistName,
                Integer.toString(pos));
    }

    /**
     * Similar to <code>find</code>,<code>search</code> looks for partial
     * matches in the MPD database.
     * 
     * @param type type of search. Should be one of the following constants:
     *            MPD_SEARCH_ARTIST, MPD_SEARCH_TITLE, MPD_SEARCH_ALBUM,
     *            MPD_SEARCH_FILENAME
     * @param string case-insensitive locator string. Anything that contains
     *            <code>string</code> will be returned in the results.
     * @return a Collection of <code>Music</code>.
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
     * @param songId music id in playlist.
     * @param position song position in seconds.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void seekById(int songId, long position) throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_SEEK_ID, Integer.toString(songId),
                Long.toString(position));
    }

    /**
     * Seeks music to the position.
     * 
     * @param index music position in playlist.
     * @param position song position in seconds.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void seekByIndex(int index, long position) throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_SEEK, Integer.toString(index),
                Long.toString(position));
    }

    /**
     * Enabled or disable consuming.
     * 
     * @param consume if true song consuming will be enabled, if false song
     *            consuming will be disabled.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setConsume(boolean consume) throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_CONSUME, consume ? "1" : "0");
    }

    /**
     * Sets cross-fade.
     * 
     * @param time cross-fade time in seconds. 0 to disable cross-fade.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setCrossfade(int time) throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        mpdConnection
                .sendCommand(MPDCommand.MPD_CMD_CROSSFADE, Integer.toString(Math.max(0, time)));
    }

    /**
     * Enabled or disable random.
     * 
     * @param random if true random will be enabled, if false random will be
     *            disabled.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setRandom(boolean random) throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_RANDOM, random ? "1" : "0");
    }

    /**
     * Enabled or disable repeating.
     * 
     * @param repeat if true repeating will be enabled, if false repeating will
     *            be disabled.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setRepeat(boolean repeat) throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_REPEAT, repeat ? "1" : "0");
    }

    /**
     * Enabled or disable single mode.
     * 
     * @param single if true single mode will be enabled, if false single mode
     *            will be disabled.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setSingle(boolean single) throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_SINGLE, single ? "1" : "0");
    }

    /**
     * Sets volume to <code>volume</code>.
     * 
     * @param volume new volume value, must be in 0-100 range.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setVolume(int volume) throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        int vol = Math.max(MPDCommand.MIN_VOLUME, Math.min(MPDCommand.MAX_VOLUME, volume));
        mpdConnection.sendCommand(MPDCommand.MPD_CMD_SET_VOLUME, Integer.toString(vol));
    }

    /**
     * Kills server.
     * 
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void shutdown() throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_KILL);
    }

    /**
     * Skip to song with specified <code>id</code>.
     * 
     * @param id song id.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void skipToId(int id) throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_PLAY_ID, Integer.toString(id));
    }

    /**
     * Jumps to track <code>position</code> from playlist.
     * 
     * @param position track number.
     * @throws MPDServerException if an error occur while contacting server.
     * @see #skipToId(int)
     */
    public void skipToPosition(int position) throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

        mpdConnection.sendCommand(MPDCommand.MPD_CMD_PLAY, Integer.toString(position));
    }

    /**
     * Stops music playing.
     * 
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void stop() throws MPDServerException {
        if (!isConnected())
            throw new MPDServerException("MPD Connection is not established");

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
