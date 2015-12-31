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

package com.anpmech.mpd.commandresponse;

import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.item.Artist;
import com.anpmech.mpd.item.Music;

import java.util.Iterator;
import java.util.ListIterator;

/**
 * This class contains methods used to process {@link Artist} entries from a {@link CommandResult}.
 */
public class ArtistResponse extends ObjectResponse<Artist> {

    /**
     * The class log identifier.
     */
    private static final String TAG = "ArtistResponse";

    /**
     * Sole public constructor.
     *
     * @param response The CommandResponse containing a Artist type MPD response.
     */
    public ArtistResponse(final CommandResult response) {
        super(response);
    }

    /**
     * This constructor builds this class from an empty MPD protocol result.
     */
    public ArtistResponse() {
        super();
    }

    /**
     * This method returns a iterator, starting at the beginning of the response.
     *
     * @return A iterator to return the response.
     * @see #getList()
     */
    @Override
    protected ListIterator<Artist> listIterator(final int position) {
        return new ArtistIterator(mResult, position);
    }

    /**
     * This class instantiates an {@link Iterator} to iterate over {@link Artist} entries.
     */
    private static final class ArtistIterator extends
            CommandResponse.SingleLineResultIterator<Artist> {

        /**
         * These are the block tokens to search for.
         */
        private static final String[] BEGINNING_BLOCK_TOKENS = {Music.RESPONSE_ARTIST,
                Music.RESPONSE_ALBUM_ARTIST};

        /**
         * The class log identifier.
         */
        private static final String TAG = "ArtistIterator";

        /**
         * Sole constructor.
         *
         * @param result   The MPD protocol command result.
         * @param position The position relative to the result to initiate the {@link #mPosition}
         *                 to.
         * @throws IllegalArgumentException if the position parameter is less than 0.
         */
        private ArtistIterator(final String result, final int position) {
            super(result, position, BEGINNING_BLOCK_TOKENS);
        }

        /**
         * This method instantiates the {@link Artist} object with a block from the MPD server
         * response.
         *
         * @param responseBlock The MPD server response to instantiate the Artist entry with.
         * @return The Artist entry.
         */
        @Override
        Artist instantiate(final String responseBlock) {
            return Artist.byResponse(responseBlock);
        }
    }
}
