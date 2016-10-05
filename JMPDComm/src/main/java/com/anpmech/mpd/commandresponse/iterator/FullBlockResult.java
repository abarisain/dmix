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
import com.anpmech.mpd.connection.CommandResult;

/**
 * This class is an abstraction to build classes which use entire response blocks.
 *
 * <p>This class is used when the Iterator will skip no lines of the response. This typically
 * used for Iterators which output Strings.</p>
 *
 * @param <T> The type of Object to be generated from the {@link CommandResult}.
 */
abstract class FullBlockResult<T> extends AbstractObjectResult<T> {

    /**
     * Sole constructor.
     *
     * @param response The MPD protocol command response.
     * @param position The position relative to the response to initiate the
     *                 {@link AbstractResult#mPosition} to.
     */
    protected FullBlockResult(final String response, final int position) {
        super(response, position);
    }

    /**
     * Returns a count of how many objects this {@code Collection} contains.
     *
     * @param result The result to count the number of objects to be iterated over.
     * @return how many objects this {@code Collection} contains, or {@link Integer#MAX_VALUE}
     * if there are more than {@link Integer#MAX_VALUE} elements in this
     * {@code Collection}.
     */
    static int count(final String result) {
        int size = 0;
        int position = result.indexOf(MPDCommand.MPD_CMD_NEWLINE);

        while (position != -1) {
            size++;
            position = result.indexOf(MPDCommand.MPD_CMD_NEWLINE, position + 1);
        }

        return size;
    }

    /**
     * Returns the previous MPD result line.
     *
     * @return The previous MPD result line.
     */
    @Override
    protected String getPreviousLine() {
        int index = cachedPreviousIndexBegin();

        /** + 1 to discard the newline. */
        if (index != 0) {
            index += 1;
        }

        return mResult.substring(index, cachedPreviousIndexEnd());
    }

    /**
     * This method returns the index of the next beginning token in relation to the current
     * position.
     *
     * @return The next beginning token in relation to the current position.
     */
    @Override
    protected int nextIndexBegin() {
        return mPosition;
    }

    /**
     * This method returns the index of the next ending token in relation to the current
     * position.
     *
     * @return The next ending token in relation to the current position.
     */
    @Override
    protected int nextIndexEnd() {
        return mResult.indexOf(MPDCommand.MPD_CMD_NEWLINE, cachedNextIndexBegin());
    }

    /**
     * This method returns the index of the prior beginning token in relation to the current
     * position.
     *
     * @return The prior beginning token in relation to the current position.
     */
    @Override
    protected int previousIndexBegin() {
        /** - 2 to discard the newline. */
        int index = mResult.lastIndexOf(MPDCommand.MPD_CMD_NEWLINE, mPosition - 2);

        if (index == -1 && mPosition != 0) {
            index = 0;
        }

        return index;
    }

    /**
     * This method returns the index of the prior ending token in relation to the current
     * position.
     *
     * @return The prior ending token in relation to the current position.
     */
    @Override
    protected int previousIndexEnd() {
        final int position;
        final int resultLength = mResult.length();

        /**
         * If the position is the final character, it should be a newline, don't allow in the
         * Iterator.
         */
        if (mPosition == resultLength &&
                (int) mResult.charAt(resultLength - 1) == (int) MPDCommand.MPD_CMD_NEWLINE) {
            position = resultLength - 1;
        } else {
            position = mPosition;
        }

        return position;
    }
}
