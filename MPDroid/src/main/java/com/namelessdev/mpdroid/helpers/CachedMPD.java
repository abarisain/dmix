/*
 * Copyright (C) 2010-2015 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.helpers;

import com.anpmech.mpd.MPD;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Album;
import com.anpmech.mpd.item.AlbumBuilder;
import com.anpmech.mpd.item.Artist;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/*
 * Cached Version of MPD
 *
 * Certain methods of MPD are overridden to call the cache.
 *
 * All public methods should call cache.refresh() to see whether the cache is up to date.
 */
public class CachedMPD extends MPD {

    private final AlbumCache mCache;

    public CachedMPD() {
        super();

        mCache = AlbumCache.getInstance(this);
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
     * Add detail information to all albums.
     *
     * @param albums The albums to add detail information to.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    @Override
    protected void addAlbumDetails(final List<Album> albums)
            throws IOException, MPDException {
        final ListIterator<Album> iterator = albums.listIterator();
        final AlbumBuilder albumBuilder = new AlbumBuilder();

        mCache.refresh();
        while (iterator.hasNext()) {
            final Album album = iterator.next();
            albumBuilder.setAlbum(album);
            final Artist artist = album.getArtist();
            final String artistName = getArtistName(artist);
            final AlbumCache.AlbumDetails details;

            details =
                    mCache.getAlbumDetails(artistName, album.getName(), album.hasAlbumArtist());

            if (details != null) {
                albumBuilder.setAlbumDetails(details.mNumTracks, details.mTotalTime);
                albumBuilder.setSongDetails(details.mDate, details.mPath);
                iterator.set(albumBuilder.build());
            }
        }
        Log.d("MPD CACHED", "Details of " + albums.size());
    }

    /**
     * Adds path information to all album objects in a list.
     *
     * @param albums List of Album objects to add path information.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    @Override
    protected void addAlbumSongDetails(final List<Album> albums) throws IOException, MPDException {
        final ListIterator<Album> iterator = albums.listIterator();
        final AlbumBuilder albumBuilder = new AlbumBuilder();

        mCache.refresh();
        while (iterator.hasNext()) {
            final Album album = iterator.next();
            final Artist artist = album.getArtist();
            final String artistName = getArtistName(artist);

            final AlbumCache.AlbumDetails details =
                    mCache.getAlbumDetails(artistName, album.getName(), album.hasAlbumArtist());
            if (details != null) {
                albumBuilder.setAlbum(album);
                albumBuilder.setSongDetails(details.mDate, details.mPath);
                iterator.set(albumBuilder.build());
            }
        }
        Log.d("MPD CACHED", "addAlbumSongDetails " + albums.size());
    }

    /**
     * Forced cache refresh.
     */
    public void clearCache() {
        mCache.refresh(true);
    }

    /**
     * Get all albums.
     *
     * @return A list of Album objects.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    @Override
    public List<Album> getAllAlbums()
            throws IOException, MPDException {
        final List<Album> allAlbums;
        mCache.refresh();
        final Set<List<String>> albumListSet = mCache.getUniqueAlbumSet();
        final Set<Album> albums = new HashSet<>(albumListSet.size());
        final AlbumBuilder albumBuilder = new AlbumBuilder();

        for (final List<String> ai : albumListSet) {
            final String thirdList = ai.get(2);
            albumBuilder.setName(ai.get(0));

            if (thirdList != null && thirdList.isEmpty()) { // no album artist
                albumBuilder.setArtist(ai.get(1));
            } else {
                albumBuilder.setAlbumArtist(ai.get(2));
            }

            albums.add(albumBuilder.build());
        }

        if (albums.isEmpty()) {
            allAlbums = Collections.emptyList();
        } else {
            allAlbums = new ArrayList<>(albums);
            Collections.sort(allAlbums);
            addAlbumDetails(allAlbums);
        }

        return allAlbums;
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
    public List<List<String>> listAlbumArtists(final List<Album> albums)
            throws IOException, MPDException {
        final List<List<String>> albumArtists = new ArrayList<>(albums.size());

        mCache.refresh();
        for (final Album album : albums) {
            final Artist artist = album.getArtist();
            final Set<String> albumArtist;
            final String artistName = getArtistName(artist);

            albumArtist = mCache.getAlbumArtists(album.getName(), artistName);
            albumArtists.add(new ArrayList<>(albumArtist));
        }

        return albumArtists;
    }

    /**
     * List all albums of given artist from database.
     *
     * @param artist         artist to list albums
     * @param useAlbumArtist use AlbumArtist instead of Artist
     * @return List of albums.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    @Override
    public List<String> listAlbums(final Artist artist, final boolean useAlbumArtist)
            throws IOException, MPDException {
        mCache.refresh();
        return new ArrayList(mCache.getAlbums(artist, useAlbumArtist));
    }
}
