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

package com.anpmech.mpd.item;

import com.anpmech.mpd.ResponseObject;

import org.jetbrains.annotations.NotNull;

/**
 * This class creates a Artist Item, a item commonly found in the <A
 * HREF="http://www.musicpd.org/doc/protocol/database.html">Database Subsystem</A> in the <A
 * HREF="http://www.musicpd.org/doc/protocol">MPD Protocol</A>, for the Java backend.
 */
public class Artist extends AbstractArtist<Artist> {

    /**
     * The copy constructor for this class.
     *
     * @param entry The AbstractResponseItem to copy.
     */
    public Artist(@NotNull final Artist entry) {
        super(entry.mResponseObject);
    }

    /**
     * This constructor is used to create a new Artist item with a ResponseObject.
     *
     * @param object The prepared ResponseObject.
     * @see #byName(String)
     * @see #byResponse(String)
     */
    private Artist(@NotNull final ResponseObject object) {
        super(object);
    }

    /**
     * This method is used to create a new Artist by name.
     *
     * @param name The name of the Artist.
     * @return The new Artist.
     */
    public static Artist byName(@NotNull final String name) {
        return new Artist(new ResponseObject(name, null));
    }

    /**
     * This method is used to construct a new PlaylistFile by server response.
     *
     * @param response The server response.
     * @return The new PlaylistFile.
     */
    public static Artist byResponse(@NotNull final String response) {
        return new Artist(new ResponseObject(null, response));
    }
}
