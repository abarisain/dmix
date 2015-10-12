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

package com.anpmech.mpd.commandresponse;

import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.item.PlaylistFile;

import java.util.Iterator;
import java.util.ListIterator;

/**
 * This class contains methods used to process {@link PlaylistFile} entries from a {@link
 * CommandResult}.
 */
public class PlaylistFileResponse extends ObjectResponse<PlaylistFile> {

    /**
     * The class log identifier.
     */
    private static final String TAG = "PlaylistFileResponse";

    /**
     * Sole public constructor.
     *
     * @param response The CommandResponse containing a PlaylistFile type or mixed entry MPD
     *                 response.
     */
    public PlaylistFileResponse(final CommandResult response) {
        super(response);
    }

    /**
     * This constructor builds this class from an empty MPD protocol result.
     */
    public PlaylistFileResponse() {
        super();
    }

    /**
     * This method returns a iterator, starting at the beginning of the response.
     *
     * @return A iterator to return the response.
     * @see #getList()
     */
    @Override
    protected ListIterator<PlaylistFile> listIterator(final int position) {
        return new PlaylistFileIterator(mResult, position);
    }

    /**
     * This class instantiates an {@link Iterator} to iterate over {@link PlaylistFile} entries.
     */
    private static final class PlaylistFileIterator extends
            CommandResponse.SingleLineResultIterator<PlaylistFile> {

        /**
         * This is the beginning block token to find for this multi-line response.
         */
        private static final String[] BLOCK_TOKEN = {PlaylistFile.RESPONSE_PLAYLIST_FILE};

        /**
         * The class log identifier.
         */
        private static final String TAG = "PlaylistFileIterator";

        /**
         * Sole constructor.
         *
         * @param result   The MPD protocol command result.
         * @param position The position relative to the result to initiate the {@link #mPosition}
         *                 to.
         * @throws IllegalArgumentException if the position parameter is less than 0.
         */
        private PlaylistFileIterator(final String result, final int position) {
            super(result, position, BLOCK_TOKEN);
        }

        /**
         * This method instantiates the {@link PlaylistFile} object with a block from the MPD
         * server response.
         *
         * @param responseBlock The MPD server response to instantiate the Music entry with.
         * @return The PlaylistFile entry.
         */
        @Override
        PlaylistFile instantiate(final String responseBlock) {
            return PlaylistFile.byResponse(responseBlock);
        }
    }
}
