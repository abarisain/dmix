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

import com.anpmech.mpd.ResponseObject;
import com.anpmech.mpd.Tools;
import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.item.PlaylistFile;
import com.anpmech.mpd.item.Stream;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * This class contains methods used to process {@link PlaylistFile} entries from a MPD response.
 *
 * <p>This class is immutable, thus, thread-safe.</p>
 */
public class PlaylistFileResponse extends ObjectResponse<PlaylistFile> {

    /**
     * This is the beginning block token to find for this multi-line response.
     */
    private static final String[] BLOCK_TOKEN = {PlaylistFile.RESPONSE_PLAYLIST_FILE};

    /**
     * The class log identifier.
     */
    private static final String TAG = "PlaylistFileResponse";

    /**
     * This constructor is used to create {@link PlaylistFile} objects from a CommandResult.
     *
     * @param result The CommandResult containing a PlaylistFile type MPD result.
     */
    public PlaylistFileResponse(final CommandResult result) {
        super(result);
    }

    /**
     * This constructor builds this class from an empty MPD protocol result.
     */
    public PlaylistFileResponse() {
        super();
    }

    /**
     * This constructor is used to create {@link PlaylistFile} objects from another compatible
     * {@link ObjectResponse}.
     *
     * @param response The ObjectResponse containing a PlaylistFile type MPD response.
     */
    public PlaylistFileResponse(final ObjectResponse<?> response) {
        super(response);
    }

    /**
     * This constructor is used to iterate over responses in a {@link ResponseObject}.
     *
     * @param response The ResponseObject to iterate over.
     */
    public PlaylistFileResponse(final ResponseObject response) {
        super(response);
    }

    /**
     * This method returns a iterator, starting at the beginning of the response.
     *
     * @return A iterator to return the response.
     */
    @Override
    protected ListIterator<PlaylistFile> listIterator(final int position) {
        return new PlaylistFileIterator(mResult, position);
    }

    /**
     * Returns a count of how many objects this {@code Collection} contains.
     *
     * @return how many objects this {@code Collection} contains, or {@link Integer#MAX_VALUE}
     * if there are more than {@link Integer#MAX_VALUE} elements in this
     * {@code Collection}.
     */
    @Override
    public int size() {
        final Iterator<Void> iterator = new NoopIterator(mResult);

        return CommandResponse.AbstractObjectResultIterator.count(iterator);
    }

    /**
     * This class instantiates an {@link Iterator} to iterate over {@link PlaylistFile} entries.
     */
    private abstract static class AbstractPlaylistFileIterator<T> extends
            CommandResponse.SingleLineResultIterator<T> {

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
        private AbstractPlaylistFileIterator(final String result, final int position) {
            super(result, position, BLOCK_TOKEN);
        }

        /**
         * This method checks to see at the key's value at {@code position} is
         * {@link Stream#PLAYLIST_NAME}.
         *
         * @param position The position of the key of the value to check.
         * @return True if the value of the key at {@code position} matches
         * {@link Stream#PLAYLIST_NAME}, false otherwise.
         */
        private boolean isStreamValue(final int position) {
            int start = position;

            if (start != -1) {
                start += PlaylistFile.RESPONSE_PLAYLIST_FILE.length() + 2;
            }

            return mResult.regionMatches(start, Stream.PLAYLIST_NAME, 0,
                    Stream.PLAYLIST_NAME.length());
        }

        /**
         * This method returns the index of the next beginning token in relation to the current
         * position.
         *
         * @return The next beginning token in relation to the current position.
         */
        @Override
        protected int nextIndexBegin() {
            int index = super.nextIndexBegin();

            if (isStreamValue(index)) {
                index = Tools.getNextKeyIndex(mResult, index + 1, mBeginBlockTokens);
            }

            return index;
        }

        /**
         * This method returns the index of the prior beginning token in relation to the current
         * position.
         *
         * @return The prior beginning token in relation to the current position.
         */
        @Override
        protected int previousIndexBegin() {
            int index = super.previousIndexBegin();

            if (isStreamValue(index)) {
                index = previousIndexBegin(index);
            }

            return index;
        }
    }

    /**
     * This class implements a {@link AbstractPlaylistFileIterator} simply for counting
     * iterations with less required garbage collection.
     */
    private static final class NoopIterator extends AbstractPlaylistFileIterator<Void> {

        /**
         * Sole constructor.
         *
         * @param result The MPD protocol command result.
         */
        private NoopIterator(final String result) {
            super(result, 0);
        }

        /**
         * This method instantiates the {@link PlaylistFile} object with a block from the MPD
         * server response.
         *
         * @param responseBlock The MPD server response to instantiate the Music entry with.
         * @return The PlaylistFile entry.
         */
        @Override
        Void instantiate(final String responseBlock) {
            return null;
        }

        /**
         * Returns the next object in the iteration.
         *
         * @return the next object.
         * @throws NoSuchElementException If there are no more elements.
         * @see #hasNext
         */
        @Override
        public Void next() {
            return voidNext();
        }
    }

    /**
     * This class instantiates an {@link Iterator} to iterate over {@link PlaylistFile} entries.
     */
    private static final class PlaylistFileIterator
            extends AbstractPlaylistFileIterator<PlaylistFile> {

        /**
         * Sole constructor.
         *
         * @param result   The MPD protocol command result.
         * @param position The position relative to the result to initiate the {@link #mPosition}
         *                 to.
         * @throws IllegalArgumentException if the position parameter is less than 0.
         */
        private PlaylistFileIterator(final String result, final int position) {
            super(result, position);
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
