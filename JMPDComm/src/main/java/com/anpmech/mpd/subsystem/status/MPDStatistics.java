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

import java.util.Date;

/**
 * An interface to the {@link MPDStatisticsMap} class for the methods which are not required for
 * MPDStatisticsMap modification.
 */
public interface MPDStatistics extends Response {

    /**
     * Retrieves total number of albums in the connected media server's database.
     *
     * @return total number of albums in the connected media server's database.
     */
    long getAlbums();

    /**
     * Retrieves total number of artists in the server database.
     *
     * @return The total number of artists in the server database.
     */
    long getArtists();

    /**
     * Retrieves the amount of time the media server would take to play every song in the database
     * once.
     *
     * @return Retrieves the amount of time (in seconds) mpd would take to play every song in the db
     * once.
     */
    long getDBPlaytime();

    /**
     * Retrieves last database update time.
     *
     * @return The last database update time.
     */
    Date getDBUpdateTime();

    /**
     * Retrieves time the media server has been playing audio.
     *
     * @return How long the media server has been actually playing audio in seconds.
     */
    long getPlayTime();

    /**
     * Retrieves total number of songs.
     *
     * @return The total number of songs.
     */
    long getSongs();

    /**
     * Retrieves server up time.
     *
     * @return The server up time.
     */
    long getUpTime();
}
