/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2016 The MPDroid Project
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

import com.anpmech.mpd.TestTools;
import com.anpmech.mpd.Tools;
import com.anpmech.mpd.commandresponse.GenreResponse;

import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * This class tests elements of the {@link Music} and {@link AbstractMusic} classes.
 */
public final class MusicTest {

    /**
     * The class log identifier.
     */
    private static final String TAG = "MusicTest";

    /**
     * This array contains all resource files.
     */
    private static final String[] TEST_FILE_PATHS = {
            TestTools.FILE_SINGULAR_TRACK_FILE,
            TestTools.FILE_SINGULAR_TRACK_STREAM,
            TestTools.FILE_SINGULAR_PLAYLISTINFO,
            TestTools.FILE_MULTIPLE_PLAYLISTINFO,
            TestTools.FILE_ROOT_LSINFO
    };

    private final Map<String, Music> mMusicList = new HashMap<>();

    private final Map<String, String> mRawMusic = new HashMap<>();

    private static String getUnknownResource(final Class<? extends Item<?>> clazz) {
        final String key = AbstractItem.UNKNOWN_METADATA + clazz.getSimpleName();
        final String mainText;

        if (AbstractItem.RESOURCE.containsKey(key)) {
            mainText = AbstractItem.RESOURCE.getString(key);
        } else {
            mainText = null;
        }

        return mainText;
    }

    public static long parseDate(final CharSequence dateResponse) {
        final int length = dateResponse.length();
        final StringBuilder sb = new StringBuilder(length);
        long resultDate = Long.MIN_VALUE;

        for (int i = 0; i < length; i++) {
            final char c = dateResponse.charAt(i);

            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }

        try {
            resultDate = Long.parseLong(sb.toString());
        } catch (final NumberFormatException ignored) {
        }

        return resultDate;
    }

    private String getURIFragment(final String filePath) {
        String URIFragment = null;
        final String value = getValue(filePath, AbstractMusic.RESPONSE_FILE);

        if (value != null) {
            final int pos = value.indexOf('#');

            if (pos > 1) {
                URIFragment = value.substring(pos + 1, value.length());
            }
        }

        return URIFragment;
    }

    private String getValue(final String filePath, final String key) {
        String value = null;
        final String haystack = mRawMusic.get(filePath);

        final int keyIndex = haystack.indexOf(key + ": ");

        if (keyIndex != -1) {
            final int valueIndex = keyIndex + key.length() + 2;
            final int valueEndIndex = haystack.indexOf('\n', valueIndex);

            if (valueEndIndex != -1) {
                value = haystack.substring(valueIndex, valueEndIndex);
            }
        }

        return value;
    }

    private boolean isStream(final String filePath) {
        final String value = getValue(filePath, AbstractMusic.RESPONSE_FILE);

        return value != null && value.contains("://");
    }

    @Before
    public void loadFiles() throws IOException {
        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();

        for (final String filePath : TEST_FILE_PATHS) {
            final URL path = classLoader.getResource(filePath);
            if (path == null) {
                throw new FileNotFoundException("File not found: " + filePath);
            }

            final String resourcePath = path.getFile();

            mRawMusic.put(resourcePath, TestTools.readFile(filePath));
            mMusicList.put(resourcePath, new Music(mRawMusic.get(resourcePath)));
        }
    }

    @Test
    public void testAlbumArtistNameMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final String expectedValue = getValue(filePath,
                    AbstractMusic.RESPONSE_ALBUM_ARTIST);
            final Music music = mMusicList.get(filePath);
            final String msg = TestTools.getMatchMsg("AlbumArtistName", filePath);

            assertEquals(msg, expectedValue, music.getAlbumArtistName());
        }
    }

    @Test
    public void testAlbumArtistOrArtist() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final String albumArtistName = getValue(filePath,
                    AbstractMusic.RESPONSE_ALBUM_ARTIST);
            final String artistName = getValue(filePath, AbstractMusic.RESPONSE_ARTIST);
            final Music music = mMusicList.get(filePath);
            final String msg = TestTools.getMatchMsg("Album Artist or Artist", filePath);

            final String expectedValue;
            if (!Tools.isEmpty(albumArtistName)) {
                expectedValue = albumArtistName;
            } else if (!Tools.isEmpty(artistName)) {
                expectedValue = artistName;
            } else {
                expectedValue = getUnknownResource(Artist.class);
            }

            assertEquals(msg, expectedValue, music.getAlbumArtistOrArtist());
        }
    }

    @Test
    public void testAlbumMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final Album expectedValue = mMusicList.get(filePath).getAlbum();
            final String albumArtistName = getValue(filePath,
                    AbstractMusic.RESPONSE_ALBUM_ARTIST);
            final String msg = TestTools.getMatchMsg(AbstractMusic.RESPONSE_ALBUM, filePath);
            final String albumName = getValue(filePath, AbstractMusic.RESPONSE_ALBUM);

            final AlbumBuilder albumBuilder = new AlbumBuilder();
            albumBuilder.setName(albumName);
            if (Tools.isEmpty(albumArtistName)) {
                albumBuilder.setArtist(getValue(filePath, AbstractMusic.RESPONSE_ARTIST));
            } else {
                albumBuilder.setAlbumArtist(albumArtistName);
            }
            final String fullPath = mMusicList.get(filePath).getParentDirectory();
            final String date = getValue(filePath, AbstractMusic.RESPONSE_DATE);

            final long parsedDate;
            if (date == null) {
                parsedDate = Long.MIN_VALUE;
            } else {
                parsedDate = parseDate(date);
            }
            albumBuilder.setSongDetails(parsedDate, fullPath);

            assertEquals(msg, albumBuilder.build(), expectedValue);
        }
    }

    @Test
    public void testArtistNameMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            // getValue() has a bug where it will match, either AlbumArtist or Artist.
            final String expectedValue = getValue(filePath, '\n' + AbstractMusic.RESPONSE_ARTIST);
            final Music music = mMusicList.get(filePath);
            final String msg = TestTools.getMatchMsg("Artist Name", filePath);

            assertEquals(msg, expectedValue, music.getArtistName());
        }
    }

    @Test
    public void testComposerNameMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final String expectedValue = getValue(filePath,
                    AbstractMusic.RESPONSE_COMPOSER);
            final Music music = mMusicList.get(filePath);
            final String msg = TestTools.getMatchMsg("Composer Name", filePath);

            assertEquals(msg, expectedValue, music.getComposerName());
        }
    }

    @Test
    public void testDateMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final CharSequence value = getValue(filePath, AbstractMusic.RESPONSE_DATE);
            final long expectedValue;
            final Music music = mMusicList.get(filePath);
            final String msg = TestTools.getMatchMsg("Date", filePath);

            if (value == null) {
                expectedValue = Long.MIN_VALUE;
            } else {
                expectedValue = parseDate(value);
            }

            assertEquals(msg, expectedValue, music.getDate());
        }
    }

    @Test
    public void testDiscMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final String value = getValue(filePath, AbstractMusic.RESPONSE_DISC);
            final Music music = mMusicList.get(filePath);
            final String msg = TestTools.getMatchMsg("Disc", filePath);
            int expectedValue = Integer.MIN_VALUE;

            if (value != null) {
                final int discIndex = value.indexOf('/');
                try {
                    if (discIndex == -1) {
                        expectedValue = Integer.parseInt(value);
                    } else {
                        expectedValue = Integer.parseInt(value.substring(0, discIndex));
                    }
                } catch (final NumberFormatException ignored) {
                }
            }

            assertEquals(msg, expectedValue, music.getDisc());
        }
    }

    @Test
    public void testFormattedTimeMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final String value = getValue(filePath, AbstractMusic.RESPONSE_TIME);

            final Music music = mMusicList.get(filePath);
            final String msg = TestTools.getMatchMsg("Formatted Time", filePath);
            long time = Long.MIN_VALUE;

            try {
                time = Long.parseLong(value);
            } catch (final NumberFormatException ignore) {
            }

            // CharSequence comparison fails.
            final String expectedValue = Tools.timeToString(time).toString();
            assertEquals(msg, expectedValue, music.getFormattedTime().toString());
        }
    }

    @Test
    public void testFullPathMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final Music music = mMusicList.get(filePath);
            final String msg = TestTools.getMatchMsg("Full Path", filePath);
            String expectedValue = getValue(filePath, AbstractMusic.RESPONSE_FILE);

            if (isStream(filePath)) {
                final int pos = expectedValue.indexOf('#');

                if (pos != Integer.MIN_VALUE) {
                    expectedValue = expectedValue.substring(0, pos);
                }
            }

            assertEquals(msg, expectedValue, music.getFullPath());
        }
    }

    @Test
    public void testGenreNameMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final String expectedValue = getValue(filePath, AbstractMusic.RESPONSE_GENRE);
            final Music music = mMusicList.get(filePath);
            final GenreResponse genres = music.getGenres();
            final String msg = TestTools.getMatchMsg("Genre Name", filePath);
            final String genreName;

            if (genres.isEmpty()) {
                genreName = null;
            } else {
                genreName = genres.get(0).getName();
            }

            assertEquals(msg, expectedValue, genreName);
        }
    }

    @Test
    public void testIsStreamMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final Music music = mMusicList.get(filePath);
            final String msg = TestTools.getMatchMsg("isStream", filePath);

            assertEquals(msg, isStream(filePath), music.isStream());
        }
    }

    @Test
    public void testNameMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final String value = getValue(filePath, AbstractMusic.RESPONSE_NAME);
            final Music music = mMusicList.get(filePath);
            final String msg = TestTools.getMatchMsg("name", filePath);

            String expectedValue = null;
            if (isStream(filePath)) {
                expectedValue = getURIFragment(filePath);
            }

            if (expectedValue == null) {
                if (value == null || value.isEmpty()) {
                    expectedValue = getValue(filePath, AbstractMusic.RESPONSE_FILE);
                } else {
                    expectedValue = value;
                }
            }

            assertEquals(msg, expectedValue, music.getName());
        }
    }

    @Test
    public void testNameTagMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final String expectedValue = getValue(filePath, AbstractMusic.RESPONSE_NAME);
            final Music music = mMusicList.get(filePath);
            final String msg = TestTools.getMatchMsg("NameTag", filePath);

            assertEquals(msg, expectedValue, music.getNameTag());
        }
    }

    @Test
    public void testParentDirectoryMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final Music music = mMusicList.get(filePath);
            final String msg = TestTools.getMatchMsg("Parent Directory", filePath);
            String expectedValue = getValue(filePath, AbstractMusic.RESPONSE_FILE);

            if (expectedValue != null) {
                int index = expectedValue.lastIndexOf('/');

                /** If it ends with a backslash, try again. */
                if (index == expectedValue.length() - 1) {
                    index = expectedValue.lastIndexOf('/', index - 1);
                }

                if (index != -1) {
                    expectedValue = expectedValue.substring(0, index);
                }
            }
            assertEquals(msg, expectedValue, music.getParentDirectory());
        }
    }

    @Test
    public void testQueueIDMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final String value = getValue(filePath, AbstractMusic.RESPONSE_SONG_ID);
            final Music music = mMusicList.get(filePath);
            final String msg = TestTools.getMatchMsg("Playlist ID", filePath);

            int expectedValue = Integer.MIN_VALUE;
            if (value != null) {
                try {
                    expectedValue = Integer.parseInt(value);
                } catch (final NumberFormatException ignored) {
                }
            }

            assertEquals(msg, expectedValue, music.getSongId());
        }
    }

    @Test
    public void testQueuePositionMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final String value = getValue(filePath, AbstractMusic.RESPONSE_SONG_POS);
            final Music music = mMusicList.get(filePath);
            final String msg = TestTools.getMatchMsg("Playlist position", filePath);

            int expectedValue = Integer.MIN_VALUE;
            if (value != null) {
                try {
                    expectedValue = Integer.parseInt(value);
                } catch (final NumberFormatException ignored) {
                }
            }

            assertEquals(msg, expectedValue, music.getPos());
        }
    }

    @Test
    public void testTimeMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final String value = getValue(filePath, AbstractMusic.RESPONSE_TIME);
            final Music music = mMusicList.get(filePath);
            long expectedValue = Long.MIN_VALUE;
            final String msg = TestTools.getMatchMsg("Time", filePath);

            try {
                expectedValue = Long.parseLong(value);
            } catch (final NumberFormatException ignored) {
            }

            assertEquals(msg, expectedValue, music.getTime());
        }
    }

    @Test
    public void testTitleMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final String value = getValue(filePath, AbstractMusic.RESPONSE_TITLE);
            final Music music = mMusicList.get(filePath);
            final String msg = TestTools.getMatchMsg("Title", filePath);

            final String expectedValue;
            if (value == null || value.isEmpty()) {
                expectedValue = getValue(filePath, AbstractMusic.RESPONSE_FILE);
            } else {
                expectedValue = value;
            }

            assertEquals(msg, expectedValue, music.getTitle());
        }
    }

    @Test
    public void testTotalTrackMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final String value = getValue(filePath, AbstractMusic.RESPONSE_TRACK);
            final Music music = mMusicList.get(filePath);
            final String msg = TestTools.getMatchMsg("Total Track", filePath);

            final int expectedValue;
            if (value == null) {
                expectedValue = Integer.MIN_VALUE;
            } else {
                final int trackIndex = value.indexOf('/');

                if (trackIndex == -1) {
                    expectedValue = Integer.MIN_VALUE;
                } else {
                    expectedValue = Integer.parseInt(value.substring(trackIndex + 1));
                }
            }

            assertEquals(msg, expectedValue, music.getTotalTracks());
        }
    }

    @Test
    public void testTrackMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final String value = getValue(filePath, AbstractMusic.RESPONSE_TRACK);
            final Music music = mMusicList.get(filePath);
            final String msg = TestTools.getMatchMsg("Track", filePath);

            final int expectedValue;
            if (value == null) {
                expectedValue = Integer.MIN_VALUE;
            } else {
                final int trackIndex = value.indexOf('/');

                if (trackIndex == -1) {
                    expectedValue = Integer.parseInt(value);
                } else {
                    expectedValue = Integer.parseInt(value.substring(0, trackIndex));
                }
            }

            assertEquals(msg, expectedValue, music.getTrack());
        }
    }

    @Test
    public void testURIFragmentMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final Music music = mMusicList.get(filePath);
            final String expectedValue = getURIFragment(filePath);
            final String msg = TestTools.getMatchMsg("URIFragment", filePath);

            assertEquals(msg, expectedValue, music.getURIFragment());
        }
    }
}
