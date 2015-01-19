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
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * This class should be thread-safe.
 */
public class AlbumInfo {

    private static final Pattern BLOCK_IN_COMBINING_DIACRITICAL_MARKS =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private static final Map<CharSequence, String> CHECKSUM_CACHE;

    private static final String INVALID_ALBUM_CHECKSUM = "INVALID_ALBUM_CHECKSUM";

    private static final String TAG = "AlbumInfo";

    private static final Pattern TEXT_PATTERN = Pattern.compile("[^\\w .-]+");

    private final String mAlbumName;

    private final String mArtistName;

    private final String mFilename;

    private final String mPath;

    static {
        /**
         * It is unlikely that more than one thread would write to the map at one time.
         */
        final int concurrencyLevel = 1;

        /**
         * The HashMap default capacity.
         */
        final int defaultCapacity = 16;

        /**
         * The ConcurrencyMap default.
         */
        final float loadFactor = 0.75f;

        CHECKSUM_CACHE = new ConcurrentHashMap<>(defaultCapacity, loadFactor, concurrencyLevel);
    }

    public AlbumInfo(final Music music) {
        super();

        String artist = music.getAlbumArtistName();

        if (artist == null) {
            artist = music.getArtistName();
        }

        mArtistName = artist;
        mAlbumName = music.getAlbumName();
        mPath = music.getPath();
        mFilename = music.getFilename();
    }

    public AlbumInfo(final Album album) {
        super();

        final Artist artist = album.getArtist();
        if (artist != null) {
            mArtistName = artist.getName();
        } else {
            mArtistName = null;
        }

        mAlbumName = album.getName();
        mPath = album.getPath();
        mFilename = null;
    }

    public AlbumInfo(final AlbumInfo albumInfo) {
        this(albumInfo.mArtistName, albumInfo.mAlbumName, albumInfo.mPath, albumInfo.mFilename);
    }

    public AlbumInfo(final String artistName, final String albumName) {
        this(artistName, albumName, null, null);
    }

    private AlbumInfo(final String artistName, final String albumName, final String path,
            final String filename) {
        super();

        mArtistName = artistName;
        mAlbumName = albumName;
        mPath = path;
        mFilename = filename;
    }

    private static String cleanGetRequest(final CharSequence text) {
        String processedText = null;

        if (text != null) {
            processedText = TEXT_PATTERN.matcher(text).replaceAll(" ");

            processedText = Normalizer.normalize(processedText, Normalizer.Form.NFD);

            processedText =
                    BLOCK_IN_COMBINING_DIACRITICAL_MARKS.matcher(processedText).replaceAll("");
        }

        return processedText;
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

    // Remove disc references from albums (like CD1, disc02 ...)
    private static String removeDiscReference(final String album) {
        String cleanedAlbum = album.toLowerCase();

        for (final String discReference : new String[]{"cd", "disc", "disque"}) {
            cleanedAlbum = cleanedAlbum.replaceAll(discReference + "\\s*\\d+", " ");
        }
        return cleanedAlbum;
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

            if (Tools.isNotEqual(mAlbumName, albumInfo.mAlbumName)) {
                isEqual = Boolean.FALSE;
            }

            if (Tools.isNotEqual(mArtistName, albumInfo.mArtistName)) {
                isEqual = Boolean.FALSE;
            }
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual.booleanValue();
    }

    public String getAlbumName() {
        return mAlbumName;
    }

    public String getArtistName() {
        return mArtistName;
    }

    public String getFilename() {
        return mFilename;
    }

    public String getKey() {
        final String value;

        if (isValid()) {
            final CharSequence key = mAlbumName + mArtistName;

            if (CHECKSUM_CACHE.containsKey(key)) {
                value = CHECKSUM_CACHE.get(key);
            } else {
                value = getHashFromString(key.toString());
                CHECKSUM_CACHE.put(key, value);
            }
        } else {
            value = INVALID_ALBUM_CHECKSUM;
        }

        return value;
    }

    public AlbumInfo getNormalized() {
        final String artist = cleanGetRequest(mArtistName);
        String album = cleanGetRequest(mAlbumName);
        album = removeDiscReference(album);

        return new AlbumInfo(album, artist, mPath, mFilename);
    }

    public String getPath() {
        return mPath;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{mArtistName, mAlbumName});
    }

    public boolean isValid() {
        final boolean isArtistNameEmpty = mArtistName == null || mArtistName.isEmpty();
        final boolean isAlbumNameEmpty = mAlbumName == null || mAlbumName.isEmpty();
        return !isAlbumNameEmpty && !isArtistNameEmpty;
    }

    @Override
    public String toString() {
        return "AlbumInfo{" +
                "mAlbumName='" + mAlbumName + '\'' +
                ", mArtistName='" + mArtistName + '\'' +
                ", mFilename='" + mFilename + '\'' +
                ", mPath='" + mPath + '\'' +
                '}';
    }
}
