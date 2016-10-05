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

package com.anpmech.mpd.commandresponse.iterator;

import com.anpmech.mpd.MPDCommand;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class is used to create an {@link Iterator} to iterate over a result for a specific line to
 * create an Object.
 *
 * @param <T> The type to create from this response.
 */
abstract class SingleLineResult<T> extends PartialBlockResult<T> {

    /**
     * This constructor is used when either the beginning tokens or the ending tokens must be
     * defined.
     *
     * @param result           The MPD protocol command result.
     * @param position         The position relative to the result to initiate the
     *                         {@link #mPosition} to.
     * @param beginBlockTokens The block tokens to find the beginning of a block. This array
     *                         must be sorted in ascending natural order prior to calling this
     *                         constructor.
     * @throws IllegalArgumentException If the position parameter is less than 0.
     */
    protected SingleLineResult(final String result, final int position,
            final String[] beginBlockTokens) {
        super(result, position, beginBlockTokens);
    }

    /**
     * This constructor is used when the first token found in a response is used as the beginning
     * and ending delimiter for a result.
     *
     * <p>This is used for MPD protocol results which have one single type of information in the
     * result.</p>
     *
     * @param result   The MPD protocol command result.
     * @param position The position relative to the result to initiate the {@link #mPosition} to.
     * @throws IllegalArgumentException If the position parameter is less than 0.
     */
    protected SingleLineResult(final String result, final int position) {
        super(result, position);
    }

    /**
     * Returns a count of how many objects this {@code Collection} contains.
     *
     * @param result           The result to count the number of objects to be iterated over.
     * @param beginBlockTokens The block tokens to find the beginning of a block. This array must
     *                         be sorted in ascending natural order prior to calling this
     *                         constructor.
     * @return how many objects this {@code Collection} contains, or {@link Integer#MAX_VALUE}
     * if there are more than {@link Integer#MAX_VALUE} elements in this {@code Collection}.
     */
    static int count(final String result, final String[] beginBlockTokens) {
        final Iterator<Void> iterator = new Noop(result, 0, beginBlockTokens);

        return AbstractObjectResult.count(iterator);
    }

    /**
     * This method returns the index of the next ending token in relation to the current position.
     *
     * @return The next ending token in relation to the current position.
     */
    @Override
    protected int nextIndexEnd() {
        return mResult.indexOf(MPDCommand.MPD_CMD_NEWLINE, cachedNextIndexBegin());
    }

    /**
     * This method returns the index of the prior ending token in relation to the current position.
     *
     * @return The prior ending token in relation to the current position.
     */
    @Override
    protected int previousIndexEnd() {
        int end = mResult.indexOf(MPDCommand.MPD_CMD_NEWLINE, cachedPreviousIndexBegin());

        if (end == -1 && cachedNextIndexBegin() == 0) {
            end = mResult.length();
        }

        return end;
    }

    /**
     * This class implements a {@link SingleLineResult} simply for counting iterations with less
     * required garbage collection.
     */
    private static final class Noop extends SingleLineResult<Void> {

        /**
         * This constructor is used when either the beginning tokens or the ending tokens must be
         * defined.
         *
         * @param result           The MPD protocol command result.
         * @param position         The position relative to the result to initiate the
         *                         {@link #mPosition} to.
         * @param beginBlockTokens The block tokens to find the beginning of a block. This
         *                         array must be sorted in ascending natural order prior to calling
         *                         this constructor.
         * @throws IllegalArgumentException If the position parameter is less than 0.
         */
        private Noop(final String result, final int position,
                final String[] beginBlockTokens) {
            super(result, position, beginBlockTokens);
        }

        /**
         * Override this to create the Object using the response block.
         *
         * @param responseBlock The response block to create the Object from.
         * @return The object created from the response block.
         */
        @Override
        protected Void instantiate(final String responseBlock) {
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
}
