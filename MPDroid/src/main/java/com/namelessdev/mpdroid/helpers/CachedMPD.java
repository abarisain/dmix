/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.helpers;

import org.a0z.mpd.MPD;
import org.a0z.mpd.exception.MPDException;
import org.a0z.mpd.item.Album;
import org.a0z.mpd.item.Artist;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * Cached Version of org.a0z.mpd.MPD
 *
 * Certain methods of MPD are overridden to call the cache.
 *
 * All public methods should call cache.refresh() to see whether the cache is up to date.
 */
public class CachedMPD extends MPD {

    private final AlbumCache mCache;

    private boolean mIsEnabled = true;

    public CachedMPD() {
        this(true);
    }

    public CachedMPD(final boolean isEnabled) {
        super();
        mCache = AlbumCache.getInstance(this);
        mIsEnabled = isEnabled;
    }

    /**
     * Returns the artist name if it's available, blank otherwise.
     *
     * @param artist The Artist object to extract the artist name from.
     * @return The artist name, if it's available, blank otherwise.
     */
    private static String getArtistName(final Artist artist) {
        final String artistName;

        if (artist == null) {
            artistName = "";
        } else {
            artistName = artist.getName();
        }

        return artistName;
    }

    /*
     * add path info to all albums
     */

    /**
     * Adds path information to all album objects in a list.
     *
     * @param albums List of Album objects to add path information.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    @Override
    protected void addAlbumPaths(final List<Album> albums) throws IOException, MPDException {
        if (!isCached()) {
            super.addAlbumPaths(albums);
            return;
        }
        for (final Album album : albums) {
            final Artist artist = album.getArtist();
            final String artistName = getArtistName(artist);

            final AlbumCache.AlbumDetails details =
                    mCache.getAlbumDetails(artistName, album.getName(), album.hasAlbumArtist());
            if (details != null) {
                album.setPath(details.mPath);
            }
        }
        Log.d("MPD CACHED", "addAlbumPaths " + albums.size());
    }

    /**
     * Forced cache refresh.
     */
    public void clearCache() {
        if (mIsEnabled) {
            mCache.refresh(true);
        }
    }

    /**
     * Add detail information to all albums.
     *
     * @param albums   The albums to add detail information to.
     * @param findYear Not applicable to this class extension.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    @Override
    protected void getAlbumDetails(final List<Album> albums, final boolean findYear/* ignored */)
            throws IOException, MPDException {
        if (isCached()) {
            for (final Album album : albums) {
                final Artist artist = album.getArtist();
                final String artistName = getArtistName(artist);
                final AlbumCache.AlbumDetails details;

                details =
                        mCache.getAlbumDetails(artistName, album.getName(), album.hasAlbumArtist());

                if (null != details) {
                    album.setSongCount(details.mNumTracks);
                    album.setDuration(details.mTotalTime);
                    album.setYear(details.mDate);
                    album.setPath(details.mPath);
                }
            }
            Log.d("MPD CACHED", "Details of " + albums.size());
        } else {
            super.getAlbumDetails(albums, findYear);
        }
    }

    /**
     * Get all albums.
     *
     * @param trackCountNeeded Do we need the track count ?
     * @return A list of Album objects.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    @Override
    public List<Album> getAllAlbums(final boolean trackCountNeeded)
            throws IOException, MPDException {
        final List<Album> allAlbums;

        if (isCached()) {
            final Set<List<String>> albumListSet = mCache.getUniqueAlbumSet();
            final Set<Album> albums = new HashSet<>(albumListSet.size());

            for (final List<String> ai : albumListSet) {
                final Album album;
                final String thirdList = ai.get(2);

                if (thirdList != null && thirdList.isEmpty()) { // no album artist
                    album = new Album(ai.get(0), new Artist(ai.get(1)), false);
                } else {
                    album = new Album(ai.get(0), new Artist(ai.get(2)), true);
                }

                albums.add(album);
            }

            if (albums.isEmpty()) {
                allAlbums = Collections.emptyList();
            } else {
                allAlbums = new ArrayList<>(albums);
                Collections.sort(allAlbums);
                getAlbumDetails(allAlbums, true);
            }
        } else {
            allAlbums = super.getAllAlbums(trackCountNeeded);
        }

        return allAlbums;
    }

    /**
     * Check whether the AlbumCache is enabled and ready for use.
     *
     * @return True if enabled and ready for use, false otherwise.
     */
    protected boolean isCached() {
        return mIsEnabled && mCache.refresh();
    }

    /**
     * Gets a list of all album artists in the database.
     *
     * @param albums List of Album objects to get Album Artists from.
     * @return A list of album artists.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    @Override
    public List<String[]> listAlbumArtists(final List<Album> albums)
            throws IOException, MPDException {
        final List<String[]> albumArtists;

        if (isCached()) {
            albumArtists = new ArrayList<>(albums.size());
            for (final Album album : albums) {
                final Artist artist = album.getArtist();
                final Set<String> albumArtist;
                final String artistName = getArtistName(artist);

                albumArtist = mCache.getAlbumArtists(album.getName(), artistName);
                albumArtists.add(albumArtist.toArray(new String[albumArtist.size()]));
            }
        } else {
            albumArtists = super.listAlbumArtists(albums);
        }

        return albumArtists;
    }

    /**
     * List all albums of given artist from database.
     *
     * @param artist              artist to list albums
     * @param useAlbumArtist      use AlbumArtist instead of Artist
     * @return List of albums.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    @Override
    public List<String> listAlbums(final String artist, final boolean useAlbumArtist)
            throws IOException, MPDException {
        final List<String> albums;

        if (isCached()) {
            albums = new ArrayList(mCache.getAlbums(artist, useAlbumArtist));
        } else {
            albums = super.listAlbums(artist, useAlbumArtist);
        }

        return albums;
    }

    /**
     * List all album artist or artist names of all given albums from database.
     *
     * @param albums         List of Album objects to get Artists or Album Artists from.
     * @param useAlbumArtist If true use album artist, false otherwise.
     * @return list of arrays of artist names for each album.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    @Override
    public List<String[]> listArtists(final List<Album> albums, final boolean useAlbumArtist)
            throws IOException, MPDException {
        final List<String[]> artists;

        if (isCached()) {
            artists = new ArrayList<>(albums.size());
            for (final Album album : albums) {
                final List<String> aba = mCache.getArtistsByAlbum(album.getName(), useAlbumArtist);
                artists.add(aba.toArray(new String[aba.size()]));
            }
        } else {
            artists = super.listArtists(albums, useAlbumArtist);
        }

        return artists;
    }

    /**
     * Set whether to use the cache.
     *
     * @param useCache True to use the AlbumCache, false otherwise.
     */
    public void setUseCache(final boolean useCache) {
        mIsEnabled = useCache;
    }

}
