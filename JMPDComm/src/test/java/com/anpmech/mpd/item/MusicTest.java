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

import com.anpmech.mpd.Log;
import com.anpmech.mpd.TestTools;
import com.anpmech.mpd.Tools;
import com.anpmech.mpd.exception.InvalidResponseException;

import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;

public final class MusicTest {

    private static final String TAG = "MusicTest";

    private final Map<String, Music> mMusicList = new HashMap<>();

    private final Map<String, List<String>> mRawMusic = new HashMap<>();

    private static String getMatchMsg(final String message, final String filePath) {
        return message + " failed to match for filepath: " + filePath + '.';
    }

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
        long resultDate = -1L;

        for (int i = 0; i < length; i++) {
            final char c = dateResponse.charAt(i);

            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }

        try {
            resultDate = Long.parseLong(sb.toString());
        } catch (final NumberFormatException e) {
            Log.warning(TAG, "Not a valid date.", e);
        }

        return resultDate;
    }

    private static String readFile(final String pathname) throws IOException {
        final File file = new File(pathname);
        final StringBuilder fileContents = new StringBuilder((int) file.length());
        final Scanner scanner = new Scanner(file, "UTF-8");
        final String lineSeparator = System.getProperty("line.separator");

        try {
            while (scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine());
                fileContents.append(lineSeparator);
            }
        } finally {
            scanner.close();
        }

        return fileContents.toString();
    }

    private static List<String> readFileToList(final String file) throws IOException {
        final List<String> list = new ArrayList<>();
        BufferedReader br = null;
        try {
            final FileInputStream fis = new FileInputStream(new File(file));
            br = new BufferedReader(new InputStreamReader(fis));
            String buffer;

            while (true) {
                buffer = br.readLine();
                if (buffer != null) {
                    list.add(buffer);
                } else {
                    break;
                }
            }
        } finally {
            final int toRemove = list.size() - 1;

            if (toRemove > 0) {
                list.remove(toRemove);
            }

            if (br != null) {
                br.close();
            }
        }

        return list;
    }

    public static String[] splitResponse(final String line) {
        final int delimiterIndex = line.indexOf(':');
        final String[] result = new String[2];

        if (delimiterIndex == -1) {
            throw new InvalidResponseException("Failed to parse server response key for line: " +
                    line);
        }

        result[0] = line.substring(0, delimiterIndex);

        /** Skip ': ' */
        result[1] = line.substring(delimiterIndex + 2);

        return result;
    }

    private String getURIFragment(final String filePath) {
        String URIFragment = null;
        final String value = getValueFromList(filePath, AbstractMusic.RESPONSE_FILE);

        if (value != null) {
            final int pos = value.indexOf('#');

            if (pos > 1) {
                URIFragment = value.substring(pos + 1, value.length());
            }
        }

        return URIFragment;
    }

    private String getValueFromList(final String filePath, final String key) {
        String value = null;
        String[] pair;

        for (final String line : mRawMusic.get(filePath)) {
            pair = splitResponse(line);

            if (pair[0].equals(key)) {
                value = pair[1];
                break;
            }
        }

        return value;
    }

    private boolean isStream(final String filePath) {
        final String value = getValueFromList(filePath, AbstractMusic.RESPONSE_FILE);

        return value != null && value.contains("://");
    }

    @Before
    public void loadSingleNonstreamLsinfo() throws Exception {
        final ClassLoader classLoader = getClass().getClassLoader();

        for (final String filePath : TestTools.TEST_FILE_PATHS) {
            final String resourcePath = classLoader.getResource(filePath).getFile();

            mRawMusic.put(resourcePath, readFileToList(resourcePath));
            mMusicList.put(resourcePath, MusicBuilder.build(mRawMusic.get(resourcePath)));
        }
    }

    @Test
    public void testAlbumArtistNameMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final String expectedValue = getValueFromList(filePath,
                    AbstractMusic.RESPONSE_ALBUM_ARTIST);
            final Music music = mMusicList.get(filePath);
            final String msg = getMatchMsg("AlbumArtistName", filePath);

            assertEquals(msg, expectedValue, music.getAlbumArtistName());
        }
    }

    @Test
    public void testAlbumArtistOrArtist() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final String albumArtistName = getValueFromList(filePath,
                    AbstractMusic.RESPONSE_ALBUM_ARTIST);
            final String artistName = getValueFromList(filePath, AbstractMusic.RESPONSE_ARTIST);
            final Music music = mMusicList.get(filePath);
            final String msg = getMatchMsg("Album Artist or Artist", filePath);

            final String expectedValue;
            if (albumArtistName != null && !albumArtistName.isEmpty()) {
                expectedValue = albumArtistName;
            } else if (artistName != null && !artistName.isEmpty()) {
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
            final String albumArtistName = getValueFromList(filePath,
                    AbstractMusic.RESPONSE_ALBUM_ARTIST);
            final String msg = getMatchMsg(AbstractMusic.RESPONSE_ALBUM, filePath);
            final boolean isAlbumArtist = albumArtistName != null && !albumArtistName.isEmpty();
            final String albumName = getValueFromList(filePath, AbstractMusic.RESPONSE_ALBUM);

            final AlbumBuilder albumBuilder = new AlbumBuilder();
            albumBuilder.setName(albumName);
            if (isAlbumArtist) {
                albumBuilder.setAlbumArtist(albumArtistName);
            } else {
                albumBuilder.setArtist(getValueFromList(filePath, AbstractMusic.RESPONSE_ARTIST));
            }
            final String fullPath = getValueFromList(filePath, AbstractMusic.RESPONSE_FILE);
            final String date = getValueFromList(filePath, AbstractMusic.RESPONSE_DATE);

            final long parsedDate;
            if (date == null) {
                parsedDate = -1L;
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
            final String expectedValue = getValueFromList(filePath, AbstractMusic.RESPONSE_ARTIST);
            final Music music = mMusicList.get(filePath);
            final String msg = getMatchMsg("Artist Name", filePath);

            assertEquals(msg, expectedValue, music.getArtistName());
        }
    }

    @Test
    public void testComposerNameMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final String expectedValue = getValueFromList(filePath,
                    AbstractMusic.RESPONSE_COMPOSER);
            final Music music = mMusicList.get(filePath);
            final String msg = getMatchMsg("Composer Name", filePath);

            assertEquals(msg, expectedValue, music.getComposerName());
        }
    }

    @Test
    public void testDateMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final CharSequence value = getValueFromList(filePath, AbstractMusic.RESPONSE_DATE);
            final long expectedValue;
            final Music music = mMusicList.get(filePath);
            final String msg = getMatchMsg("Date", filePath);

            if (value == null) {
                expectedValue = -1L;
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
            final String value = getValueFromList(filePath, AbstractMusic.RESPONSE_DISC);
            final Music music = mMusicList.get(filePath);
            final String msg = getMatchMsg("Disc", filePath);
            int expectedValue = -1;

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
            final String value = getValueFromList(filePath, AbstractMusic.RESPONSE_TIME);

            final Music music = mMusicList.get(filePath);
            final String msg = getMatchMsg("Formatted Time", filePath);
            long time = -1L;

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
            final String msg = getMatchMsg("Full Path", filePath);
            String expectedValue = getValueFromList(filePath, AbstractMusic.RESPONSE_FILE);

            if (isStream(filePath)) {
                final int pos = expectedValue.indexOf('#');

                if (pos != -1) {
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
            final String expectedValue = getValueFromList(filePath, AbstractMusic.RESPONSE_GENRE);
            final Music music = mMusicList.get(filePath);
            final String msg = getMatchMsg("Genre Name", filePath);

            assertEquals(msg, expectedValue, music.getGenreName());
        }
    }

    @Test
    public void testIsStreamMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final Music music = mMusicList.get(filePath);
            final String msg = getMatchMsg("isStream", filePath);

            assertEquals(msg, isStream(filePath), music.isStream());
        }
    }

    @Test
    public void testNameMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final String value = getValueFromList(filePath, AbstractMusic.RESPONSE_NAME);
            final Music music = mMusicList.get(filePath);
            final String msg = getMatchMsg("name", filePath);

            String expectedValue = null;
            if (isStream(filePath)) {
                expectedValue = getURIFragment(filePath);
            }

            if (expectedValue == null) {
                if (value == null || value.isEmpty()) {
                    expectedValue = getValueFromList(filePath, AbstractMusic.RESPONSE_FILE);
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
            final String expectedValue = getValueFromList(filePath, AbstractMusic.RESPONSE_NAME);
            final Music music = mMusicList.get(filePath);
            final String msg = getMatchMsg("NameTag", filePath);

            assertEquals(msg, expectedValue, music.getNameTag());
        }
    }

    @Test
    public void testParentDirectoryMatch() {
        for (final Map.Entry<String, Music> entry : mMusicList.entrySet()) {
            final String filePath = entry.getKey();
            final Music music = mMusicList.get(filePath);
            final String msg = getMatchMsg("Parent Directory", filePath);
            String expectedValue = getValueFromList(filePath, AbstractMusic.RESPONSE_FILE);

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
            final String value = getValueFromList(filePath, AbstractMusic.RESPONSE_SONG_ID);
            final Music music = mMusicList.get(filePath);
            final String msg = getMatchMsg("Playlist ID", filePath);

            int expectedValue = -1;
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
            final String value = getValueFromList(filePath, AbstractMusic.RESPONSE_SONG_POS);
            final Music music = mMusicList.get(filePath);
            final String msg = getMatchMsg("Playlist position", filePath);

            int expectedValue = -1;
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
            final String value = getValueFromList(filePath, AbstractMusic.RESPONSE_TIME);
            final Music music = mMusicList.get(filePath);
            long expectedValue = -1L;
            final String msg = getMatchMsg("Time", filePath);

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
            final String value = getValueFromList(filePath, AbstractMusic.RESPONSE_TITLE);
            final Music music = mMusicList.get(filePath);
            final String msg = getMatchMsg("Title", filePath);

            final String expectedValue;
            if (value == null || value.isEmpty()) {
                expectedValue = getValueFromList(filePath, AbstractMusic.RESPONSE_FILE);
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
            final String value = getValueFromList(filePath, AbstractMusic.RESPONSE_TRACK);
            final Music music = mMusicList.get(filePath);
            final String msg = getMatchMsg("Total Track", filePath);

            final int expectedValue;
            if (value == null) {
                expectedValue = -1;
            } else {
                final int trackIndex = value.indexOf('/');

                if (trackIndex == -1) {
                    expectedValue = -1;
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
            final String value = getValueFromList(filePath, AbstractMusic.RESPONSE_TRACK);
            final Music music = mMusicList.get(filePath);
            final String msg = getMatchMsg("Track", filePath);

            final int expectedValue;
            if (value == null) {
                expectedValue = -1;
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
            final String msg = getMatchMsg("URIFragment", filePath);

            assertEquals(msg, expectedValue, music.getURIFragment());
        }
    }
}
