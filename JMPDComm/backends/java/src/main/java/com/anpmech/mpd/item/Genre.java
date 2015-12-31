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
 * This class creates a Genre Item, a item commonly found in the
 * <A HREF="http://www.musicpd.org/doc/protocol/database.html">Database Subsystem</A> in the
 * <A HREF="http://www.musicpd.org/doc/protocol">MPD Protocol</A>, for the Java backend.
 */
public class Genre extends AbstractGenre {

    /**
     * This is the copy constructor.
     *
     * @param genre The Genre to copy.
     */
    public Genre(@NotNull final Genre genre) {
        super(genre.mResponseObject);
    }

    /**
     * This constructor constructs a Genre from a MPD protocol response.
     *
     * @param response A MPD protocol response.
     */
    public Genre(@NotNull final String response) {
        super(new ResponseObject(null, response));
    }

    /**
     * This object is used to create a new Genre with a ResponseObject.
     *
     * @param object The prepared ResponseObject.
     */
    private Genre(@NotNull final ResponseObject object) {
        super(object);
    }

}
