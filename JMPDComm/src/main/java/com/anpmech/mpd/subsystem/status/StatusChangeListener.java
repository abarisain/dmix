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

/**
 * This interface is used in conjunction with the {@link IdleSubsystemMonitor} to listen to
 * <A HREF="http://www.musicpd.org/doc/protocol/command_reference.html#command_idle">Idle
 * subsystem</A> changes.
 */
public interface StatusChangeListener {

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
     * @param oldPlaylistVersion old playlist version.
     */
    void playlistChanged(int oldPlaylistVersion);

    /**
     * Called when MPD server random feature changes state.
     */
    void randomChanged();

    /**
     * Called when MPD server repeat feature changes state.
     */
    void repeatChanged();

    /**
     * Called when MPD state changes on server.
     *
     * @param oldState previous state.
     */
    void stateChanged(int oldState);

    /**
     * Called when any sticker of any track has been changed on server.
     */
    void stickerChanged();

    /**
     * Called when a stored playlist has been modified, renamed, created or deleted.
     */
    void storedPlaylistChanged();

    /**
     * Called when playing track is changed on server.
     *
     * @param oldTrack track number before event.
     */
    void trackChanged(int oldTrack);

    /**
     * Called when volume changes on MPD server.
     *
     * @param oldVolume volume before event
     */
    void volumeChanged(int oldVolume);
}
