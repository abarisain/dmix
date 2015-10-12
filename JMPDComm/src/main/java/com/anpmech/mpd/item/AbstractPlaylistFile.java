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

import com.anpmech.mpd.ResponseObject;
import com.anpmech.mpd.exception.InvalidResponseException;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is the generic base for the PlaylistFile items, abstracted for backend.
 *
 * <p>This item is returned from methods of the
 * <A HREF="http://www.musicpd.org/doc/protocol/database.html">database</A>
 * subsystem of the <A HREF="http://www.musicpd.org/doc/protocol">MPD protocol</A>.</p>
 *
 * @param <T> The PlaylistFile type.
 */
abstract class AbstractPlaylistFile<T extends PlaylistFile> extends AbstractEntry<PlaylistFile> {

    /**
     * The media server response key returned for a Playlist value.
     */
    public static final String RESPONSE_PLAYLIST_FILE = "playlist";

    /**
     * The class log identifier.
     */
    protected static final String TAG = "PlaylistFile";

    private static final Pattern PLAYLIST_FILE_REGEXP = Pattern.compile("^.*/(.+)\\.(\\w+)$");

    /**
     * This constructor is used to create a new PlaylistFile item with a ResponseObject.
     *
     * @param object The prepared ResponseObject.
     */
    AbstractPlaylistFile(@NotNull final ResponseObject object) {
        super(object);
    }

    /**
     * The full path as given by the MPD protocol.
     *
     * @return The full path for this entry.
     */
    @Override
    @NotNull
    public String getFullPath() {
        final String fullPath = getPlaylistFileFullPath();

        if (fullPath == null) {
            throw new InvalidResponseException(pathNotFoundError());
        }

        return fullPath;
    }

    @Override
    public String getName() {
        final String name = super.getName();
        final String result;

        final Matcher matcher = PLAYLIST_FILE_REGEXP.matcher(name);
        if (matcher.matches()) {
            result = matcher.replaceAll("[$2] $1.$2");
        } else {
            result = name;
        }

        return result;
    }

    @Override
    public String toString() {
        return getName();
    }
}
