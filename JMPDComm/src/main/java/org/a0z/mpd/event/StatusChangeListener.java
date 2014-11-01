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

package org.a0z.mpd.event;

import org.a0z.mpd.MPDStatus;

/**
 * Implement this to get idle subsystem status updates.
 */
public interface StatusChangeListener {

    /**
     * Called when MPD server connection changes state. (connected/disconnected)
     *
     * @param connected      new connection state: true, connected; false,
     *                       disconnected.
     * @param connectionLost true when connection was lost, false otherwise.
     */
    void connectionStateChanged(boolean connected, boolean connectionLost);

    /**
     * Called when the MPD server update database starts and stops.
     *
     * @param updating  true when updating, false when not updating.
     * @param dbChanged After update, if the database has changed, this will be true else false.
     */
    void libraryStateChanged(boolean updating, boolean dbChanged);

    /**
     * Called when playlist changes on MPD server.
     *
     * @param mpdStatus          MPDStatus after playlist change.
     * @param oldPlaylistVersion old playlist version.
     */
    void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion);

    /**
     * Called when MPD server random feature changes state.
     *
     * @param random new random state: true, on; false, off
     */
    void randomChanged(boolean random);

    /**
     * Called when MPD server repeat feature changes state.
     *
     * @param repeating new repeat state: true, on; false, off.
     */
    void repeatChanged(boolean repeating);

    /**
     * Called when MPD state changes on server.
     *
     * @param mpdStatus MPDStatus after event.
     * @param oldState  previous state.
     */
    void stateChanged(MPDStatus mpdStatus, int oldState);

    /**
     * Called when any sticker of any track has been changed on server.
     *
     * @param mpdStatus {@code MPDStatus} after event.
     */
    void stickerChanged(MPDStatus mpdStatus);

    /**
     * Called when playing track is changed on server.
     *
     * @param mpdStatus {@code MPDStatus} after event.
     * @param oldTrack  track number before event.
     */
    void trackChanged(MPDStatus mpdStatus, int oldTrack);

    /**
     * Called when volume changes on MPD server.
     *
     * @param mpdStatus {@code MPDStatus} after event
     * @param oldVolume volume before event
     */
    void volumeChanged(MPDStatus mpdStatus, int oldVolume);
}
