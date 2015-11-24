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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class is the generic base for the Artist items, abstracted for backend.
 *
 * @param <T> The Artist type.
 */
abstract class AbstractArtist<T extends AbstractArtist<T>> extends ResponseItem<T> {

    /**
     * The class log identifier.
     */
    protected static final String TAG = AbstractMusic.RESPONSE_ARTIST;

    /**
     * This constructor is used to create a new Artist item with a ResponseObject.
     *
     * @param object The prepared ResponseObject.
     */
    AbstractArtist(@NotNull final ResponseObject object) {
        super(object);
    }

    /**
     * This is the string representation of this Artist.
     *
     * @return A string representation of this Artist.
     */
    @Override
    public String getName() {
        return findValue(AbstractMusic.RESPONSE_ARTIST, AbstractMusic.RESPONSE_ALBUM_ARTIST);
    }

    /**
     * This returns the name of the Artist name, with a appended "the" removed.
     *
     * @return The Artist name, with a appended "the" removed.
     */
    @Override
    @Nullable
    public String sortName() {
        final String name = super.sortName();
        final String result;

        if (name != null && name.regionMatches(true, 0, "the", 0, 4)) {
            result = name.substring(4);
        } else {
            result = name;
        }

        return result;
    }
}
