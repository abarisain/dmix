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

import com.anpmech.mpd.Tools;
import com.anpmech.mpd.item.Album;
import com.anpmech.mpd.item.Artist;
import com.anpmech.mpd.item.Music;

import java.util.Arrays;

import static com.anpmech.mpd.Tools.getHashFromString;

public class AlbumInfo {

    private static final String INVALID_ALBUM_KEY = "INVALID_ALBUM_KEY";

    protected final String mAlbum;

    protected final String mArtist;

    protected String mFilename;

    protected String mPath;

    public AlbumInfo(final Music music) {
        super();
        String artist = music.getAlbumArtistName();
        if (artist == null) {
            artist = music.getArtistName();
        }
        mArtist = artist;
        mAlbum = music.getAlbumName();
        mPath = music.getPath();
        mFilename = music.getFilename();
    }

    public AlbumInfo(final Album album) {
        super();

        final Artist artistName = album.getArtist();
        if (artistName != null) {
            mArtist = artistName.getName();
        } else {
            mArtist = null;
        }

        mAlbum = album.getName();
        mPath = album.getPath();
    }

    public AlbumInfo(final AlbumInfo albumInfo) {
        this(albumInfo.mArtist, albumInfo.mAlbum, albumInfo.mPath, albumInfo.mFilename);
    }

    public AlbumInfo(final String artist, final String album) {
        super();
        mArtist = artist;
        mAlbum = album;
    }

    public AlbumInfo(final String artist, final String album, final String path,
            final String filename) {
        super();
        mArtist = artist;
        mAlbum = album;
        mPath = path;
        mFilename = filename;
    }

    @Override
    public boolean equals(final Object o) {
        Boolean isEqual = null;

        if (this == o) {
            isEqual = Boolean.TRUE;
        } else if (o == null || getClass() != o.getClass()) {
            isEqual = Boolean.FALSE;
        }

        if (isEqual == null || isEqual.equals(Boolean.TRUE)) {
            final AlbumInfo albumInfo = (AlbumInfo) o;

            if (Tools.isNotEqual(mAlbum, albumInfo.mAlbum)) {
                isEqual = Boolean.FALSE;
            }

            if (Tools.isNotEqual(mArtist, albumInfo.mArtist)) {
                isEqual = Boolean.FALSE;
            }
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual.booleanValue();
    }

    public String getAlbum() {
        return mAlbum;
    }

    public String getArtist() {
        return mArtist;
    }

    public String getFilename() {
        return mFilename;
    }

    public String getKey() {
        return isValid() ? getHashFromString(mArtist + mAlbum) : INVALID_ALBUM_KEY;
    }

    public String getPath() {
        return mPath;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{mArtist, mAlbum});
    }

    public boolean isValid() {
        final boolean isArtistEmpty = mArtist == null || mArtist.isEmpty();
        final boolean isAlbumEmpty = mAlbum == null || mAlbum.isEmpty();
        return !isAlbumEmpty && !isArtistEmpty;
    }

    public void setFilename(final String filename) {
        mFilename = filename;
    }

    public void setPath(final String path) {
        mPath = path;
    }

    @Override
    public String toString() {
        return "AlbumInfo{" +
                "artist='" + mArtist + '\'' +
                ", album='" + mAlbum + '\'' +
                ", path='" + mPath + '\'' +
                ", filename='" + mFilename + '\'' +
                '}';
    }
}
