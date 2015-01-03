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

package com.anpmech.mpd.subsystem.status;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * A class representing a <A HREF="http://www.musicpd.org/doc/protocol/command_reference.html#command_stats">stats</A>
 * command response of the <A HREF="http://www.musicpd.org/doc/protocol">MPD protocol</A>.
 */
public class MPDStatistics extends ResponseMap {

    /**
     * This is the value given if there was no long in the map with the given key.
     */
    public static final long DEFAULT_LONG = ResponseMap.LONG_DEFAULT;

    /**
     * The default number of statistic response entries.
     * <p/>
     * The statistics command responds with 7 entries as of standard MPD implementation 0.19. Unlike
     * the status command, the statistics entry response count is much less likely to fluctuate.
     */
    private static final int DEFAULT_ENTRY_COUNT = 7;

    /**
     * The key from the statistics command for the value of a count of all albums in the database.
     */
    private static final CharSequence RESPONSE_ALBUM_COUNT = "albums";

    /**
     * The key from the statistics command for the value of a count of all artists in the database.
     */
    private static final CharSequence RESPONSE_ARTIST_COUNT = "artists";

    /**
     * The key from the statistics command for the value of the time required to play all tracks in
     * the database.
     */
    private static final CharSequence RESPONSE_DATABASE_PLAY_TIME = "db_playtime";

    /**
     * The key from the statistics command for the value of last database update in UNIX time.
     */
    private static final CharSequence RESPONSE_LAST_UPDATE_TIME = "db_update";

    /**
     * The key from the statistics command for the value of length of time the connected media
     * player has been playing.
     */
    private static final CharSequence RESPONSE_PLAYTIME = "playtime";

    /**
     * The key from the statistics command for the value of the uptime in seconds.
     */
    private static final CharSequence RESPONSE_SERVER_UPTIME = "uptime";

    /**
     * The key from the statistics command for the value of the total number of songs in the
     * database.
     */
    private static final CharSequence RESPONSE_SONG_COUNT = "songs";

    /**
     * The class log identifier.
     */
    private static final String TAG = "MPDStatistics";

    /**
     * This constructor initializes the backend storage for the stat response.
     */
    public MPDStatistics() {
        super(DEFAULT_ENTRY_COUNT);
    }

    /**
     * Retrieves total number of albums in the connected media server's database.
     *
     * @return total number of albums in the connected media server's database.
     */
    public long getAlbums() {
        return parseMapLong(RESPONSE_ALBUM_COUNT);
    }

    /**
     * Retrieves total number of artists in the server database.
     *
     * @return The total number of artists in the server database.
     */
    public long getArtists() {
        return parseMapLong(RESPONSE_ARTIST_COUNT);
    }

    /**
     * Retrieves the amount of time the media server would take to play every song in the database
     * once.
     *
     * @return Retrieves the amount of time (in seconds) mpd would take to play every song in the db
     * once.
     */
    public long getDBPlaytime() {
        return parseMapLong(RESPONSE_DATABASE_PLAY_TIME);
    }

    /**
     * Retrieves last database update time.
     *
     * @return The last database update time.
     */
    public Date getDBUpdateTime() {
        final long date = TimeUnit.SECONDS.toMillis(parseMapLong(RESPONSE_LAST_UPDATE_TIME));

        return new Date(date);
    }

    /**
     * Retrieves time the media server has been playing audio.
     *
     * @return How long the media server has been actually playing audio in seconds.
     */
    public long getPlayTime() {
        return parseMapLong(RESPONSE_PLAYTIME);
    }

    /**
     * Retrieves total number of songs.
     *
     * @return The total number of songs.
     */
    public long getSongs() {
        return parseMapLong(RESPONSE_SONG_COUNT);
    }

    /**
     * Retrieves server up time.
     *
     * @return The server up time.
     */
    public long getUpTime() {
        return parseMapLong(RESPONSE_SERVER_UPTIME);
    }

    /**
     * Retrieves a string representation of the {@link ResponseMap} and this object.
     *
     * @return A string representation of the ResponseMap and this resulting object.
     */
    public String toString() {
        return super.toString() +
                "MPDStatistics: {" +
                "getAlbums(): " + getAlbums() +
                ", getArtists(): " + getArtists() +
                ", getDBPlaytime(): " + getDBPlaytime() +
                ", getDBUpdateTime(): " + getDBUpdateTime() +
                ", getPlayTime(): " + getPlayTime() +
                ", getSongs(): " + getSongs() +
                ", getUpTime(): " + getUpTime() + '}';
    }
}
