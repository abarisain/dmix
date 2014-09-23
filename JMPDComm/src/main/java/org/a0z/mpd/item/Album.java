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

package org.a0z.mpd.item;

import org.a0z.mpd.AlbumInfo;
import org.a0z.mpd.MPD;
import org.a0z.mpd.Tools;

import java.util.Arrays;

public class Album extends Item {

    private final Artist mArtist;

    private final String mName;

    private long mDuration;

    private boolean mHasAlbumArtist;

    private String mPath;

    private long mSongCount;

    private long mYear;

    public Album(final Album otherAlbum) {
        this(otherAlbum.mName,
                new Artist(otherAlbum.mArtist),
                otherAlbum.mHasAlbumArtist,
                otherAlbum.mSongCount,
                otherAlbum.mDuration,
                otherAlbum.mYear,
                otherAlbum.mPath);
    }

    public Album(final String name, final Artist artist) {
        this(name, artist, false, 0L, 0L, 0L, null);
    }

    public Album(final String name, final Artist artist, final boolean hasAlbumArtist) {
        this(name, artist, hasAlbumArtist, 0L, 0L, 0L, null);
    }

    public Album(final String name, final Artist artist, final boolean hasAlbumArtist,
            final long songCount, final long duration,
            final long year, final String path) {
        super();
        mName = name;
        mSongCount = songCount;
        mDuration = duration;
        mYear = year;
        mArtist = artist;
        mHasAlbumArtist = hasAlbumArtist;
        mPath = path;
    }

    @Override
    public int compareTo(final Item another) {
        Integer i = null;

        if (another instanceof Album) {
            final Album oa = (Album) another;
            if (MPD.sortAlbumsByYear()) {
                if (mYear < oa.mYear) {
                    i = Integer.valueOf(-1);
                } else if (mYear > oa.mYear) {
                    i = Integer.valueOf(1);
                }
            }
        }

        if (i == null) {
            i = Integer.valueOf(super.compareTo(another));
        }

        return i.intValue();
    }

    @Override
    public boolean doesNameExist(final Item o) {
        final boolean result;

        if (o instanceof Album) {
            final Album a = (Album) o;
            result = mName.equals(a.mName) && mArtist.doesNameExist(a.mArtist);
        } else {
            result = false;
        }

        return result;
    }

    /**
     * Compares an Artist object with a general contract of
     * comparison that is reflexive, symmetric and transitive.
     *
     * @param o The object to compare this instance with.
     * @return True if the objects are equal with regard to te general contract, false otherwise.
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(final Object o) {
        Boolean isEqual = null;

        if (this == o) {
            isEqual = Boolean.TRUE;
        } else if (o == null || getClass() != o.getClass()) {
            isEqual = Boolean.FALSE;
        }

        if (isEqual == null || isEqual.equals(Boolean.TRUE)) {
            final Album album = (Album) o;

            if (Tools.isNotEqual(mName, album.mName) || Tools.isNotEqual(mArtist, album.mArtist)) {
                isEqual = Boolean.FALSE;
            }
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual.booleanValue();
    }

    public AlbumInfo getAlbumInfo() {
        return new AlbumInfo(this);
    }

    public Artist getArtist() {
        return mArtist;
    }

    public long getDuration() {
        return mDuration;
    }

    public String getName() {
        return mName;
    }

    public String getPath() {
        return mPath;
    }

    public long getSongCount() {
        return mSongCount;
    }

    public long getYear() {
        return mYear;
    }

    public boolean hasAlbumArtist() {
        return mHasAlbumArtist;
    }

    /**
     * Returns an integer hash code for this Artist. By contract, any two objects for which
     * {@link #equals} returns {@code true} must return the same hash code value. This means that
     * subclasses of {@code Object} usually override both methods or neither method.
     *
     * @return This Artist hash code.
     * @see Object#equals(Object)
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{mName, mArtist});
    }

    /*
     * text for display
     */
    @Override
    public String mainText() {
        final String result;

        if (mName.isEmpty()) {
            result = MPD.getUnknownAlbum();
        } else {
            result = mName;
        }

        return result;
    }

    /**
     * This sets the artist in a new object, due to the required immutability of name
     * and artist to satisfy the requirement that the hash code not change over time.
     *
     * @param artist The new artist.
     * @return A new Album object based off this Album object.
     */
    public Album setArtist(final Artist artist) {
        return new Album(mName, artist, mHasAlbumArtist, mSongCount, mDuration, mYear, mPath);
    }

    public void setDuration(final long duration) {
        mDuration = duration;
    }

    public void setHasAlbumArtist(final boolean hasAlbumArtist) {
        mHasAlbumArtist = hasAlbumArtist;
    }

    public void setPath(final String p) {
        mPath = p;
    }

    public void setSongCount(final long sc) {
        mSongCount = sc;
    }

    public void setYear(final long y) {
        mYear = y;
    }

}
