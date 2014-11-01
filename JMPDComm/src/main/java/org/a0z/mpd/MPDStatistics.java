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

package org.a0z.mpd;

import java.util.Collection;
import java.util.Date;

import static org.a0z.mpd.Tools.KEY;
import static org.a0z.mpd.Tools.VALUE;

/**
 * Class representing MPD Server statistics.
 *
 * @author Felipe Gustavo de Almeida
 */
public class MPDStatistics {

    private static final long MILLI_TO_SEC = 1000L;

    private static final String TAG = "MPDStatistics";

    private long mAlbums = -1L;

    private long mArtists = -1L;

    private long mDBPlaytime = -1L;

    private Date mDbUpdate = null;

    private long mPlayTime = -1L;

    private long mSongs = -1L;

    private long mUpTime = -1L;

    MPDStatistics() {
        super();
    }

    /**
     * Retrieves total number of albums in the connected media server's database.
     *
     * @return total number of albums in the connected media server's database.
     */
    public long getAlbums() {
        return mAlbums;
    }

    /**
     * Retrieves total number of artists.
     *
     * @return total number of artists.
     */
    public long getArtists() {
        return mArtists;
    }

    /**
     * Retrieves the amount of time the media server would take to play every song in the db
     * once.
     *
     * @return Retrieves the amount of time (in seconds) mpd would take to play
     * every song in the db once.
     */
    public long getDBPlaytime() {
        return mDBPlaytime;
    }

    /**
     * Retrieves last database update time.
     *
     * @return last database update time.
     */
    public Date getDbUpdate() {
        Date dbUpdate = null;

        if (mDbUpdate != null) {
            dbUpdate = (Date) mDbUpdate.clone();
        }

        return dbUpdate;
    }

    /**
     * Retrieves time the media server has been playing audio.
     *
     * @return how long the media server has been actually playing audio in seconds.
     */
    public long getPlayTime() {
        return mPlayTime;
    }

    /**
     * Retrieves total number of songs.
     *
     * @return total number of songs.
     */
    public long getSongs() {
        return mSongs;
    }

    /**
     * Retrieves server up time.
     *
     * @return server up time.
     */
    public long getUpTime() {
        return mUpTime;
    }

    /**
     * Retrieves a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
        return "artists: " + mArtists +
                ", albums: " + mAlbums +
                ", last db update: " + mDbUpdate +
                ", database playtime: " + mDBPlaytime +
                ", playtime: " + mPlayTime +
                ", songs: " + mSongs +
                ", up time: " + mUpTime;
    }

    /**
     * Updates the state of the MPD Server...
     *
     * @param response The response from the server.
     */
    public final void update(final Collection<String> response) {
        for (final String[] pair : Tools.splitResponse(response)) {

            switch (pair[KEY]) {
                case "albums":
                    mAlbums = Long.parseLong(pair[VALUE]);
                    break;
                case "artists":
                    mArtists = Long.parseLong(pair[VALUE]);
                    break;
                case "db_playtime":
                    mDBPlaytime = Long.parseLong(pair[VALUE]);
                    break;
                case "db_update":
                    mDbUpdate = new Date(Long.parseLong(pair[VALUE]) * MILLI_TO_SEC);
                    break;
                case "playtime":
                    mPlayTime = Long.parseLong(pair[VALUE]);
                    break;
                case "songs":
                    mSongs = Long.parseLong(pair[VALUE]);
                    break;
                case "uptime":
                    mUpTime = Long.parseLong(pair[VALUE]);
                    break;
                default:
                    Log.warning(TAG,
                            "Undocumented statistic: Key: " + pair[KEY] + " Value: " + pair[VALUE]);
                    break;
            }
        }
    }
}
