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

package com.anpmech.mpd.item;

import org.jetbrains.annotations.NotNull;

/**
 * This is the builder for {@code Album} objects.
 */
public final class AlbumBuilder {

    /**
     * Storage for the artist object used, to build an album.
     */
    private Artist mBuilderArtist;

    /**
     * Storage for the date of the album, used to build an album.
     */
    private long mBuilderDate;

    /**
     * Storage for the duration used, to build an album.
     */
    private long mBuilderDuration;

    /**
     * Storage for the album artist used, to build an album.
     */
    private boolean mBuilderHasAlbumArtist;

    /**
     * Storage for the name of the album, used to build an album.
     */
    private String mBuilderName;

    /**
     * Storage for the path of the album on the filesystem of the media server, used to build an
     * album.
     */
    private String mBuilderPath;

    /**
     * Storage for the song count of the album, used to build an album.
     */
    private long mBuilderSongCount;

    /**
     * This method builds a {@code Album} object based on the settings set by the methods.
     *
     * @param clearAfter Will clear the information in {@code this} prior to returning.
     * @return An {@code Album} object based off the settings given in this class.
     */
    public Album build(final boolean clearAfter) {
        final Album album = new Album(mBuilderName, mBuilderArtist, mBuilderHasAlbumArtist,
                mBuilderSongCount, mBuilderDuration, mBuilderDate, mBuilderPath);

        if (clearAfter) {
            clear();
        }

        return album;
    }

    /**
     * This method builds a {@code Album} object based on the settings set by the methods. By
     * default, this class will clear the settings prior to returning the built object.
     *
     * @return An {@code Album} object based off the settings given in this class.
     */
    public Album build() {
        return build(true);
    }

    /**
     * Clears all the information in this instance.
     */
    public void clear() {
        mBuilderArtist = null;
        mBuilderDuration = 0L;
        mBuilderHasAlbumArtist = false;
        mBuilderName = null;
        mBuilderPath = null;
        mBuilderSongCount = 0L;
        mBuilderDate = 0L;
    }

    /**
     * Adds all information from another album object to this album object.
     *
     * @param album The album object with which to base this builder's settings.
     */
    public void setAlbum(final Album album) {
        mBuilderArtist = album.getArtist();
        mBuilderDuration = album.getDuration();
        mBuilderHasAlbumArtist = album.hasAlbumArtist();
        mBuilderName = album.getName();
        mBuilderPath = album.getPath();
        mBuilderSongCount = album.getSongCount();
        mBuilderDate = album.getDate();
    }

    /**
     * This sets the album artist information for this album. This is identical to {@link
     * #setAlbumArtist(Artist)} but with a {@code String} parameter for convenience.
     *
     * @param albumArtistName The name of the album artist of this album.
     * @see #setAlbumArtist(Artist)
     */
    public void setAlbumArtist(final String albumArtistName) {
        setArtist(Artist.byName(albumArtistName), true);
    }

    /**
     * This sets the album artist information for this album.
     *
     * @param albumArtist The album artist of this album.
     * @see #setAlbumArtist(String)
     */
    public void setAlbumArtist(final Artist albumArtist) {
        setArtist(albumArtist, true);
    }

    /**
     * This sets details that would come from the actual album itself.
     *
     * @param songCount The song count of the album.
     * @param duration  The duration of the album.
     * @see #setDuration(long)
     * @see #setSongCount(long)
     */
    public void setAlbumDetails(final long songCount, final long duration) {
        mBuilderDuration = duration;
        mBuilderSongCount = songCount;
    }

    /**
     * This sets the artist information for this album. This is identical to {@link
     * #setArtist(Artist)} but with a {@code String} parameter for convenience.
     *
     * @param artistName The name of the artist of this album.
     */
    public void setArtist(@NotNull final String artistName) {
        setArtist(Artist.byName(artistName), false);
    }

    /**
     * This sets the artist information for this album.
     *
     * @param artist The artist of this album.
     * @see #setArtist(String)
     */
    public void setArtist(final Artist artist) {
        setArtist(artist, false);
    }

    /**
     * This sets the artist information for this album.
     *
     * @param artist        The artist of this album.
     * @param isAlbumArtist Whether the {@code artist} signifies an album artist.
     */
    private void setArtist(final Artist artist, final boolean isAlbumArtist) {
        mBuilderArtist = artist;
        mBuilderHasAlbumArtist = isAlbumArtist;
    }

    /**
     * This sets some of the basics of an album to setup. This is identical to {@link
     * #setBase(String, Artist, boolean)} but with a String parameter with which to build an {@code
     * Artist} object, for convenience.
     *
     * @param albumName     The name of the album to build.
     * @param artistName    The name of the artist of this album.
     * @param isAlbumArtist Whether the {@code artistName} signifies an album artist.
     * @see #setBase(String, Artist, boolean)
     */
    public void setBase(final String albumName, final String artistName,
            final boolean isAlbumArtist) {
        setBase(albumName, Artist.byName(artistName), isAlbumArtist);
    }

    /**
     * This sets some of the basics of an album to setup. This is identical to {@link
     * #setBase(String, String, boolean)} but with a artist parameter for convenience.
     *
     * @param albumName     The name of the album to build.
     * @param artist        The artist of this album.
     * @param isAlbumArtist Whether the {@code artistName} signifies an album artist.
     * @see #setBase(String, String, boolean)
     */
    public void setBase(final String albumName, final Artist artist,
            final boolean isAlbumArtist) {
        setArtist(artist, isAlbumArtist);
        mBuilderName = albumName;
    }

    /**
     * Sets the date of this album.
     *
     * @param date The date the album was published.
     * @see #setSongDetails(long, String)
     */
    public void setDate(final long date) {
        mBuilderDate = date;
    }

    /**
     * This sets the duration of the album.
     *
     * @param duration The duration of the album.
     * @see #setAlbumDetails(long, long)
     */
    public void setDuration(final long duration) {
        mBuilderDuration = duration;
    }

    /**
     * This sets the album name.
     *
     * @param albumName The name of the album to build.
     * @see #setBase(String, String, boolean)
     * @see #setBase(String, Artist, boolean)
     */
    public void setName(final String albumName) {
        mBuilderName = albumName;
    }

    /**
     * Sets the path of this album.
     *
     * @param path The path with which the album can be found.
     * @see #setSongDetails(long, String)
     */
    public void setPath(final String path) {
        mBuilderPath = path;
    }

    /**
     * This sets the song count of the album.
     *
     * @param songCount The song count of the album.
     * @see #setAlbumDetails(long, long)
     */
    public void setSongCount(final long songCount) {
        mBuilderSongCount = songCount;
    }

    /**
     * This sets some album information based off the {@code Music} objects belonging to this
     * album.
     *
     * @param date The Date the album was published.
     * @param path The path with which the album can be found.
     * @see #setPath(String)
     * @see #setDate(long)
     */
    public void setSongDetails(final long date, final String path) {
        mBuilderPath = path;
        mBuilderDate = date;
    }
}
