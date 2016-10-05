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

package com.anpmech.mpd.subsystem.status;

import com.anpmech.mpd.commandresponse.KeyValueResponse;
import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.connection.MPDConnection;
import com.anpmech.mpd.exception.MPDException;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A class representing a <A HREF="http://www.musicpd.org/doc/protocol/command_reference.html#command_stats">stats</A>
 * command response of the <A HREF="http://www.musicpd.org/doc/protocol">MPD protocol</A>.
 */
public class MPDStatisticsMap extends ResponseMap implements MPDStatistics {

    /**
     * Command text required to generate a command to retrieve the statistics for this map.
     */
    public static final String CMD_ACTION_STATISTICS = "stats";

    /**
     * The default number of statistic response entries.
     *
     * <p>The statistics command responds with 7 entries as of standard MPD implementation 0.19.
     * Unlike the status command, the statistics entry response count is much less likely to
     * fluctuate.</p>
     */
    public static final int DEFAULT_ENTRY_COUNT = 7;

    /**
     * This is the value given if there was no long in the map with the given key.
     */
    public static final long DEFAULT_LONG = ResponseMap.LONG_DEFAULT;

    /**
     * The key from the statistics command for the value of a count of all albums in the database.
     */
    public static final String RESPONSE_ALBUM_COUNT = "albums";

    /**
     * The key from the statistics command for the value of a count of all artists in the database.
     */
    public static final String RESPONSE_ARTIST_COUNT = "artists";

    /**
     * The key from the statistics command for the value of the time required to play all tracks in
     * the database.
     */
    public static final String RESPONSE_DATABASE_PLAY_TIME = "db_playtime";

    /**
     * The key from the statistics command for the value of last database update in UNIX time.
     */
    public static final String RESPONSE_LAST_UPDATE_TIME = "db_update";

    /**
     * The key from the statistics command for the value of length of time the connected media
     * player has been playing.
     */
    public static final String RESPONSE_PLAYTIME = "playtime";

    /**
     * The key from the statistics command for the value of the uptime in seconds.
     */
    public static final String RESPONSE_SERVER_UPTIME = "uptime";

    /**
     * The key from the statistics command for the value of the total number of songs in the
     * database.
     */
    public static final String RESPONSE_SONG_COUNT = "songs";

    /**
     * The class log identifier.
     */
    private static final String TAG = "MPDStatistics";

    /**
     * The connection to update the statistics.
     */
    private final MPDConnection mConnection;

    /**
     * This constructor initializes the backend storage for the stat response.
     *
     * @param connection The connection to the statistics updated.
     */
    public MPDStatisticsMap(final MPDConnection connection) {
        super(DEFAULT_ENTRY_COUNT);

        mConnection = connection;
    }

    /**
     * This constructor is used to create a immutable copy of this class.
     *
     * @param responseMap The response map backend storage map.
     * @see #getImmutable()
     */
    private MPDStatisticsMap(final Map<String, String> responseMap) {
        super(responseMap);

        mConnection = null;
    }

    /**
     * This method returns a {@link MPDStatistics} Object constructed by response.
     *
     * @param response The response used to create the MPDStatus.
     * @return The MPDStatus created by response.
     */
    public static MPDStatistics getImmutable(final KeyValueResponse response) {
        return new MPDStatisticsMap(response.getKeyValueMap());
    }

    /**
     * Retrieves total number of albums in the connected media server's database.
     *
     * @return total number of albums in the connected media server's database.
     */
    @Override
    public long getAlbums() {
        return parseMapLong(RESPONSE_ALBUM_COUNT);
    }

    /**
     * Retrieves total number of artists in the server database.
     *
     * @return The total number of artists in the server database.
     */
    @Override
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
    @Override
    public long getDBPlaytime() {
        return parseMapLong(RESPONSE_DATABASE_PLAY_TIME);
    }

    /**
     * Retrieves last database update time.
     *
     * @return The last database update time.
     */
    @Override
    public Date getDBUpdateTime() {
        final long date = TimeUnit.SECONDS.toMillis(parseMapLong(RESPONSE_LAST_UPDATE_TIME));

        return new Date(date);
    }

    /**
     * Gets an immutable copy of this object.
     *
     * @return An immutable copy of this object.
     */
    public final MPDStatistics getImmutable() {
        return new MPDStatisticsMap(getMap());
    }

    /**
     * Retrieves time the media server has been playing audio.
     *
     * @return How long the media server has been actually playing audio in seconds.
     */
    @Override
    public long getPlayTime() {
        return parseMapLong(RESPONSE_PLAYTIME);
    }

    /**
     * Retrieves total number of songs.
     *
     * @return The total number of songs.
     */
    @Override
    public long getSongs() {
        return parseMapLong(RESPONSE_SONG_COUNT);
    }

    /**
     * Retrieves server up time.
     *
     * @return The server up time.
     */
    @Override
    public long getUpTime() {
        return parseMapLong(RESPONSE_SERVER_UPTIME);
    }

    /**
     * Retrieves a string representation of the {@link ResponseMap} and this object.
     *
     * @return A string representation of the ResponseMap and this resulting object.
     */
    @Override
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

    /**
     * Updates the statistics object stored in this object. Do not call this method
     * directly unless you absolutely know what you are doing. If a long running application needs
     * a status update, use the {@code MPDStatusMonitor} instead.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     * @see IdleSubsystemMonitor
     */
    public void update() throws IOException, MPDException {
        final CommandResult result = mConnection.submit(CMD_ACTION_STATISTICS).get();

        update(new KeyValueResponse(result));
    }
}
