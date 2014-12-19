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

import org.a0z.mpd.Log;
import org.a0z.mpd.Tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.a0z.mpd.Tools.KEY;
import static org.a0z.mpd.Tools.VALUE;

public final class MusicBuilder {

    private MusicBuilder() {
        super();
    }

    /**
     * The maximum number of key/value pairs for a music item response.
     */
    private static final int MUSIC_ATTRIBUTES = 30;

    /**
     * The class log identifier.
     */
    private static final String TAG = "MusicBuilder";

    /**
     * Builds a {@code Music} object from a subset of the
     * media server response to a music listing command.
     *
     * @param response A music listing command response.
     * @return A Music object.
     * @see #buildMusicFromList(java.util.Collection, boolean)
     */
    static Music build(final Collection<String> response) {
        String albumName = null;
        String artistName = null;
        String albumArtistName = null;
        String composerName = null;
        String fullPath = null;
        int disc = AbstractMusic.UNDEFINED_INT;
        long date = -1L;
        String genreName = null;
        long time = -1L;
        String title = null;
        int totalTracks = AbstractMusic.UNDEFINED_INT;
        int track = AbstractMusic.UNDEFINED_INT;
        int songId = AbstractMusic.UNDEFINED_INT;
        int songPos = AbstractMusic.UNDEFINED_INT;
        String name = null;

        for (final String[] pair : Tools.splitResponse(response)) {

            switch (pair[KEY]) {
                case AbstractMusic.RESPONSE_ALBUM:
                    albumName = pair[VALUE];
                    break;
                case AbstractMusic.RESPONSE_ALBUM_ARTIST:
                    albumArtistName = pair[VALUE];
                    break;
                case AbstractMusic.RESPONSE_ARTIST:
                    artistName = pair[VALUE];
                    break;
                case AbstractMusic.RESPONSE_COMPOSER:
                    composerName = pair[VALUE];
                    break;
                case AbstractMusic.RESPONSE_DATE:
                    date = parseDate(pair[VALUE]);
                    break;
                case AbstractMusic.RESPONSE_DISC:
                    final int discIndex = pair[VALUE].indexOf('/');

                    try {
                        if (discIndex == -1) {
                            disc = Integer.parseInt(pair[VALUE]);
                        } else {
                            disc = Integer.parseInt(pair[VALUE].substring(0, discIndex));
                        }
                    } catch (final NumberFormatException e) {
                        Log.warning(TAG, "Not a valid disc number.", e);
                    }
                    break;
                case AbstractMusic.RESPONSE_FILE:
                    fullPath = pair[VALUE];
                    if (!fullPath.isEmpty() && fullPath.contains("://")) {
                        final int pos = fullPath.indexOf('#');
                        if (pos > 1) {
                            name = fullPath.substring(pos + 1, fullPath.length());
                            fullPath = fullPath.substring(0, pos);
                        }
                    }
                    break;
                case AbstractMusic.RESPONSE_GENRE:
                    genreName = pair[VALUE];
                    break;
                case AbstractMusic.RESPONSE_NAME:
                    /**
                     * This name might already be assigned to the URL fragment identifier.
                     */
                    if (name == null) {
                        name = pair[VALUE];
                    }
                    break;
                case AbstractMusic.RESPONSE_SONG_ID:
                    try {
                        songId = Integer.parseInt(pair[VALUE]);
                    } catch (final NumberFormatException e) {
                        Log.error(TAG, "Not a valid song ID.", e);
                    }
                    break;
                case AbstractMusic.RESPONSE_SONG_POS:
                    try {
                        songPos = Integer.parseInt(pair[VALUE]);
                    } catch (final NumberFormatException e) {
                        Log.error(TAG, "Not a valid song position.", e);
                    }
                    break;
                case AbstractMusic.RESPONSE_TIME:
                    try {
                        time = Long.parseLong(pair[VALUE]);
                    } catch (final NumberFormatException e) {
                        Log.error(TAG, "Not a valid time number.", e);
                    }
                    break;
                case AbstractMusic.RESPONSE_TITLE:
                    title = pair[VALUE];
                    break;
                case AbstractMusic.RESPONSE_TRACK:
                    final int trackIndex = pair[VALUE].indexOf('/');

                    try {
                        if (trackIndex == -1) {
                            track = Integer.parseInt(pair[VALUE]);
                        } else {
                            track = Integer.parseInt(pair[VALUE].substring(0, trackIndex));
                            totalTracks = Integer.parseInt(pair[VALUE].substring(trackIndex + 1));
                        }
                    } catch (final NumberFormatException e) {
                        Log.warning(TAG, "Not a valid track number.", e);
                    }
                    break;
                default:
                    /**
                     * Ignore everything else, there are a lot of
                     * uninteresting blocks the server might send.
                     */
                    break;
            }
        }

        return new Music(albumName, albumArtistName, artistName, composerName, date, disc, fullPath,
                genreName, name, songId, songPos, time, title, totalTracks, track);
    }

    /**
     * Builds a {@code Music} object from a media
     * server response to a music listing command.
     *
     * @param response A music listing command response.
     * @return A Music object.
     * @see #build(java.util.Collection)
     */
    public static List<Music> buildMusicFromList(final Collection<String> response,
            final boolean sort) {
        final Collection<String> lineCache = new ArrayList<>(MUSIC_ATTRIBUTES);
        final int size = response.size();
        final List<Music> result;

        /** This list can be pretty sizable, it's good to give a low estimate of it's size. */
        if (size > MUSIC_ATTRIBUTES) {
            result = new ArrayList<>(size / MUSIC_ATTRIBUTES);
        } else {
            result = new ArrayList<>(0);
        }

        for (final String line : response) {
            if (line.startsWith(AbstractMusic.RESPONSE_FILE)) {
                if (!lineCache.isEmpty()) {
                    result.add(build(lineCache));
                    lineCache.clear();
                }
            }
            lineCache.add(line);
        }

        if (!lineCache.isEmpty()) {
            result.add(build(lineCache));
        }

        if (sort) {
            Collections.sort(result);
        }

        return result;
    }

    /**
     * This method parses the date MPD protocol response by removing all non-digit characters then
     * parsing it as a long.
     *
     * @param dateResponse The date MPD protocol response.
     * @return The parsed date.
     */
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
}
