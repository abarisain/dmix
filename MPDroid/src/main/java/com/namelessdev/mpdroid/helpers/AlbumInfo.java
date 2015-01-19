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

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * This class should be thread-safe.
 */
public class AlbumInfo {

    private static final String INVALID_ALBUM_CHECKSUM = "INVALID_ALBUM_CHECKSUM";

    private static final String TAG = "AlbumInfo";

    private final String mAlbum;

    private final String mArtist;

    private final String mFilename;

    private final String mPath;

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
        mFilename = null;
    }

    public AlbumInfo(final AlbumInfo albumInfo) {
        this(albumInfo.mArtist, albumInfo.mAlbum, albumInfo.mPath, albumInfo.mFilename);
    }

    public AlbumInfo(final String artist, final String album) {
        this(artist, album, null, null);
    }

    private AlbumInfo(final String artist, final String album, final String path,
            final String filename) {
        super();
        mArtist = artist;
        mAlbum = album;
        mPath = path;
        mFilename = filename;
    }

    /**
     * Convert byte array to hex string.
     *
     * @param data Target data array.
     * @return Hex string.
     */
    private static String convertToHex(final byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }

        final StringBuilder buffer = new StringBuilder(data.length);
        for (int byteIndex = 0; byteIndex < data.length; byteIndex++) {
            int halfbyte = (data[byteIndex] >>> 4) & 0x0F;
            int twoHalves = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buffer.append((char) ('0' + halfbyte));
                } else {
                    buffer.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[byteIndex] & 0x0F;
            } while (twoHalves++ < 1);
        }

        return buffer.toString();
    }

    /**
     * Gets the hash value from the specified string.
     *
     * @param value Target string value to get hash from.
     * @return the hash from string.
     */
    private static String getHashFromString(final String value) {
        String hash = null;

        if (value != null && !value.isEmpty()) {
            try {
                final MessageDigest hashEngine = MessageDigest.getInstance("MD5");
                hashEngine.update(value.getBytes("iso-8859-1"), 0, value.length());
                hash = convertToHex(hashEngine.digest());
            } catch (final NoSuchAlgorithmException | UnsupportedEncodingException e) {
                Log.e(TAG, "Failed to get hash.", e);
            }
        }

        return hash;
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
        final String value;

        if (isValid()) {
            value = getHashFromString(mArtist + mAlbum);
        } else {
            value = INVALID_ALBUM_CHECKSUM;
        }

        return value;
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
