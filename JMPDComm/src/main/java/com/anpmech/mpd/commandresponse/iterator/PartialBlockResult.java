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
import com.anpmech.mpd.Tools;
import com.anpmech.mpd.connection.CommandResult;

import java.util.Arrays;
import java.util.Iterator;

/**
 * This class is used to create an {@link Iterator} to iterate over a result to find a defined
 * beginning block, ending with a newline, to create an Object.
 *
 * <p>This is an abstraction class to create a block {@link Iterator} which uses a partial
 * result.</p>
 *
 * @param <T> The type of Object to be generated from the {@link CommandResult}.
 */
abstract class PartialBlockResult<T> extends AbstractObjectResult<T> {

    /**
     * This is a zero length array.
     */
    private static final String[] ZERO_LENGTH = {};

    /**
     * The block tokens to tokenize for the beginning of the iteration for this iterator.
     */
    protected final String[] mBeginBlockTokens;

    /**
     * This constructor is used when the first token found in a response is used as the
     * beginning and ending delimiter for a result.
     *
     * <p>This is used for MPD protocol results which have one single type of information in
     * the result.</p>
     *
     * @param result   The MPD protocol command result.
     * @param position The position relative to the result to initiate the {@link #mPosition}
     *                 to.
     * @throws IllegalArgumentException If the position parameter is less than 0.
     */
    protected PartialBlockResult(final String result, final int position) {
        super(result, position);

        final int index = result.indexOf(MPD_KV_DELIMITER);

        if (index == -1) {
            mBeginBlockTokens = ZERO_LENGTH;
        } else {
            final String token = result.substring(0, index);

            mBeginBlockTokens = new String[]{token};
        }
    }

    /**
     * This constructor is used when either the beginning tokens or the ending tokens must be
     * defined.
     *
     * @param result           The MPD protocol command result.
     * @param position         The position relative to the result to initiate the
     *                         {@link #mPosition} to.
     * @param beginBlockTokens The block tokens to find the beginning of a block. This array must
     *                         be sorted in ascending natural order prior to calling this
     *                         constructor.
     * @throws IllegalArgumentException If the position parameter is less than 0.
     */
    protected PartialBlockResult(final String result, final int position,
            final String[] beginBlockTokens) {
        super(result, position);

        mBeginBlockTokens = beginBlockTokens;
    }

    /**
     * Returns whether there are more elements to iterate.
     *
     * @return {@code true} If there are more elements, {@code false} otherwise.
     * @see #next
     */
    @Override
    public boolean hasNext() {
        return cachedNextIndexBegin() != -1 && super.hasNext();
    }

    /**
     * Returns whether there are previous elements to iterate.
     *
     * @return {@code true} If there are previous elements, {@code false} otherwise.
     * @see #previous
     */
    @Override
    public boolean hasPrevious() {
        return super.hasPrevious() && cachedPreviousIndexEnd() != -1;
    }

    /**
     * This method returns the index of the next beginning token in relation to the current
     * position.
     *
     * @return The next beginning token in relation to the current position.
     */
    @Override
    protected int nextIndexBegin() {
        return Tools.getNextKeyIndex(mResult, mPosition, mBeginBlockTokens);
    }

    /**
     * This method returns the index of the prior beginning token in relation to the current
     * position.
     *
     * @param position The position to begin at.
     * @return The prior beginning token in relation to the current position.
     */
    protected int previousIndexBegin(final int position) {
        int index = -1;
        int mpdDelimiterIndex = mResult.lastIndexOf(MPD_KV_DELIMITER, position);
        int keyIndex;

        while (index == -1 && mpdDelimiterIndex != -1) {
            keyIndex = mResult.lastIndexOf(MPDCommand.MPD_CMD_NEWLINE, mpdDelimiterIndex) + 1;
            if (keyIndex < mPosition) {
                final String foundToken = mResult.substring(keyIndex, mpdDelimiterIndex);

                if (Arrays.binarySearch(mBeginBlockTokens, foundToken) >= 0) {
                    index = keyIndex;
                }
            }

            if (index == -1) {
                mpdDelimiterIndex = mResult.lastIndexOf(MPD_KV_DELIMITER, mpdDelimiterIndex - 1);
            }
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
        return previousIndexBegin(mPosition);
    }

    /**
     * This changes the position assignment to the next response block.
     */
    @Override
    protected void setPositionNext() {
        super.setPositionNext();
        mPosition--;
    }

    /**
     * This changes the position assignment to the previous newline.
     */
    @Override
    protected void setPositionPrevious() {
        super.setPositionPrevious();
        mPosition--;
    }

    @Override
    public String toString() {
        return "PartialBlockResult{" +
                "mBeginBlockTokens=" + Arrays.toString(mBeginBlockTokens) +
                "} " + super.toString();
    }
}
