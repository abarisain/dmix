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

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Class representing MPD Server statistics.
 *
 * @author Felipe Gustavo de Almeida
 * @version $Id: MPDStatistics.java 2614 2004-11-11 18:46:31Z galmeida $
 */
public class MPDStatistics {

    private static final long MILLI_TO_SEC = 1000L;

    private long artists = -1L;

    private long albums = -1L;

    private long songs = -1L;

    private long uptime = -1L;

    private Date dbUpdate = null;

    private long playtime = -1L;

    private long dbPlaytime = -1L;

    /**
     * This is a regular expression pattern matcher
     * for the MPD protocol delimiter ": ".
     */
    private static final Pattern mpdDelimiter = Pattern.compile(": ");

    MPDStatistics(final List<String> response) {
        super();

        for (final String line : response) {
            final String[] lines = mpdDelimiter.split(line);

            switch (lines[0]) {
                case "albums":
                    albums = Long.parseLong(lines[1]);
                    break;
                case "artists":
                    artists = Long.parseLong(lines[1]);
                    break;
                case "db_playtime":
                    dbPlaytime = Long.parseLong(lines[1]);
                    break;
                case "db_update":
                    dbUpdate = new Date(Long.parseLong(lines[1]) * MILLI_TO_SEC);
                    break;
                case "playtime:":
                    playtime = Long.parseLong(lines[1]);
                    break;
                case "songs":
                    songs = Long.parseLong(lines[1]);
                    break;
                case "uptime":
                    uptime = Long.parseLong(lines[1]);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Retrieves total number of albums.
     *
     * @return total number of albums.
     */
    public long getAlbums() {
        return albums;
    }

    /**
     * Retrieves total number of artists.
     *
     * @return total number of artists.
     */
    public long getArtists() {
        return artists;
    }

    /**
     * Retrieves the amount of time mpd would take to play every song in the db
     * once.
     *
     * @return Retrieves the amount of time (in seconds) mpd would take to play
     * every song in the db once.
     */
    public long getDbPlaytime() {
        return dbPlaytime;
    }

    /**
     * Retrieves last database update time.
     *
     * @return last database update time.
     */
    public Date getDbUpdate() {
        return (Date) dbUpdate.clone();
    }

    /**
     * Retrieves time mpd has been playing music.
     *
     * @return how long mpd has been actually playing music in seconds.
     */
    public long getPlaytime() {
        return playtime;
    }

    /**
     * Retrieves total number of songs.
     *
     * @return total number of songs.
     */
    public long getSongs() {
        return songs;
    }

    /**
     * Retrieves server uptime.
     *
     * @return server uptime.
     */
    public long getUptime() {
        return uptime;
    }

    /**
     * Retrieves a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
        return "artists: " + artists +
                ", albums: " + albums +
                ", last db update: " + dbUpdate +
                ", songs: " + songs +
                ", uptime: " + uptime;
    }
}
