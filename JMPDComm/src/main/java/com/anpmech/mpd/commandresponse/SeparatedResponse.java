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
import com.anpmech.mpd.connection.MPDConnection;

import java.util.Iterator;
import java.util.ListIterator;

/**
 * This class contains methods used to process {@link CommandResult} entries from multiple, and
 * separated, {@link CommandResult}s.
 */
public class SeparatedResponse extends ObjectResponse<CommandResponse> {

    /**
     * The class log identifier.
     */
    private static final String TAG = "SeparatedResponse";

    /**
     * Sole public constructor.
     *
     * @param response The CommandResponse containing a SeparatedCommand type MPD response.
     */
    public SeparatedResponse(final CommandResult response) {
        super(response);
    }

    /**
     * This constructor builds this class from an empty MPD protocol result.
     */
    public SeparatedResponse() {
        super();
    }

    /**
     * This method returns a iterator, starting at the beginning of the response.
     *
     * @return A iterator to return the response.
     * @see #getList()
     */
    @Override
    protected ListIterator<CommandResponse> listIterator(final int position) {
        return new SeparatedIterator(mResult, mConnectionResult, position);
    }

    /**
     * This class instantiates an {@link Iterator} to iterate over {@link CommandResponse} entries.
     */
    private static final class SeparatedIterator extends
            CommandResponse.MultiLineResultIterator<CommandResponse> {

        /**
         * The class log identifier.
         */
        private static final String TAG = "SeparatedIterator";

        /**
         * This is the length of the bulk separator, used heavily by this class.
         */
        private static final int TOKEN_LENGTH = MPDConnection.MPD_CMD_BULK_SEP.length();

        /**
         * The connection result for the CommandResult.
         */
        private final String mConnectionResult;

        /**
         * Sole constructor.
         *
         * @param result           The MPD protocol command result.
         * @param connectionResult The connectionResult from the {@link CommandResult}.
         * @param position         The position relative to the result to initiate the
         *                         {@link #mPosition} to.
         * @throws IllegalArgumentException if the position parameter is less than 0.
         */
        private SeparatedIterator(final String result, final String connectionResult,
                final int position) {
            super(result, position);

            /**
             * This is a hack around the requirement that the position and next index never be
             * identical. In this iterator, the token can be at the 0 position.
             */
            if (position == 0) {
                mPosition = -1;
            }

            mConnectionResult = connectionResult;
        }

        /**
         * This method instantiates the {@link CommandResponse} object with a block from the MPD
         * server response.
         *
         * @param responseBlock The MPD server response to instantiate the SeparatedCommand entry
         *                      with.
         * @return The SeparatedCommand entry.
         */
        @Override
        CommandResponse instantiate(final String responseBlock) {
            return new CommandResponse(mConnectionResult, responseBlock);
        }

        /**
         * This method returns the index of the next beginning token in relation to the current
         * position.
         *
         * @return The next beginning token in relation to the current position.
         */
        @Override
        protected int nextIndexBegin() {
            int position = mPosition;

            /**
             * Don't allow mPosition to == the index.
             */
            if (position == -1) {
                position++;
            }

            /**
             * Remove the MPD_CMD_BULK_SEP from the new result.
             */
            if (position != 0) {
                position += TOKEN_LENGTH + 1;
            }

            /**
             * Don't iterate over the ending MPD_CMD_BULK_SEP.
             */
            if (position == mResult.length()) {
                position = -1;
            }

            return position;
        }

        /**
         * This method returns the index of the next ending token in relation to the current
         * position.
         *
         * @return The next ending token in relation to the current position.
         */
        @Override
        protected int nextIndexEnd() {
            final int resultLength = mResult.length();
            int end = mResult.indexOf(MPDConnection.MPD_CMD_BULK_SEP, mPosition + 1);

            if (end + TOKEN_LENGTH == resultLength) {
                end = -1;
            }

            if (end == -1 && mPosition != resultLength) {
                end = resultLength;
            }

            return end;
        }

        /**
         * This method returns the index of the prior beginning token in relation to the current
         * position.
         *
         * @return The prior beginning token in relation to the current position.
         */
        @Override
        protected int previousIndexBegin() {
            final int position = mPosition - TOKEN_LENGTH - 2;
            int index = mResult.lastIndexOf(MPDConnection.MPD_CMD_BULK_SEP, position);

            if (index == -1 && mPosition != 0) {
                index = 0;
            } else if (index != -1) {
                index += TOKEN_LENGTH + 1;
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
            int index = mPosition - TOKEN_LENGTH;

            if (index < 0) {
                index = -1;
            }

            if (index + TOKEN_LENGTH == mResult.length()) {
                index -= 1;
            }

            return index;
        }
    }
}
