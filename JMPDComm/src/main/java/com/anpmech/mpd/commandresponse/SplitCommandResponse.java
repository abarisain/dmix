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

import com.anpmech.mpd.CommandQueue;
import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.connection.MPDConnection;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class processes a separated command response.
 *
 * <p>This class is immutable, thus, thread-safe.</p>
 */
public class SplitCommandResponse extends CommandResult implements Iterable<CommandResponse> {

    /**
     * The class log identifier.
     */
    private static final String TAG = "SplitCommandResponse";

    /**
     * The sole constructor.
     *
     * @param result The CommandResult to use as the base for this response.
     */
    public SplitCommandResponse(final CommandResult result) {
        super(result);
    }

    /**
     * Returns an {@link Iterator} for the elements in this object.
     *
     * @return An {@code Iterator} instance.
     */
    @Override
    public Iterator<CommandResponse> iterator() {
        return new SeparatedResponseIterator(mConnectionResult, mResult, mExcludeResponses);
    }

    /**
     * This class instantiates an {@link Iterator} to iterate over a {@link CommandQueue} response
     * to create separated {@link CommandResult}s.
     */
    private static class SeparatedResponseIterator implements Iterator<CommandResponse> {

        /**
         * The index of the begin position in the {@link #getNextPositions()} return array.
         */
        private static final int BEGIN_POSITION = 1;

        /**
         * The index of the end position in the {@link #getNextPositions()} return array.
         */
        private static final int END_POSITION = 2;

        /**
         * The length of the bulk separator and newline.
         */
        private static final int OK_LENGTH = MPDConnection.MPD_CMD_BULK_SEP.length() + 1;

        /**
         * The index of the current position index in the {@link #getNextPositions()} return array.
         */
        private static final int POSITION_INDEX = 0;

        /**
         * The connection response, if it exists.
         */
        private final String mConnectionResponse;

        /**
         * This is an array of positions indexes to exclude from the list. This can be used to
         * exclude things from the end user, such as a password response.
         */
        private final int[] mExcludeResults;

        /**
         * This is the response from the outer class.
         */
        private final String mResponse;

        /**
         * This field stores the positions for the next line.
         */
        private int[] mCachedPositions;

        /**
         * This tracks the current iterator character position in the response.
         */
        private int mPosition;

        /**
         * This tracks the current number of positions into the response.
         */
        private int mPositionIndex = -1;

        /**
         * Sole constructor.
         *
         * @param connectionResponse The connection response related to this iterator.
         * @param response           The response to be separated into new CommandResponses.
         * @param excludeResults     A list of responses to skip.
         */
        protected SeparatedResponseIterator(final String connectionResponse,
                final String response, final int[] excludeResults) {
            super();
            mConnectionResponse = connectionResponse;
            mResponse = response;

            if (excludeResults != null) {
                Arrays.sort(excludeResults);
            }

            //noinspection AssignmentToCollectionOrArrayFieldFromParameter
            mExcludeResults = excludeResults;
        }

        /**
         * This retrieves the next position set.
         *
         * @return An int[] with the first index being the {@link #POSITION_INDEX}, the second
         * being the {@link #BEGIN_POSITION} and the final being the {@link #END_POSITION}.
         */
        private int[] getNextPositions() {

            if (mCachedPositions == null) {
                int positionIndex = mPositionIndex;
                int beginPosition;
                int nextPosition = mPosition;

                do {
                    positionIndex++;

                    beginPosition = nextPosition;
                    nextPosition = mResponse.indexOf(MPDConnection.MPD_CMD_BULK_SEP,
                            nextPosition) + OK_LENGTH;
                } while (!shouldIncludeResponse(positionIndex));

                mCachedPositions =
                        new int[]{positionIndex, beginPosition, nextPosition - OK_LENGTH};
            }

            return mCachedPositions;
        }

        /**
         * Returns true if there is at least one more element, false otherwise.
         *
         * @see #next
         */
        @Override
        public boolean hasNext() {
            return getNextPositions()[END_POSITION] != -1 || isSingularCommand();
        }

        /**
         * This method detects whether the input was one command.
         *
         * This is necessary due to {@link CommandQueue#toString(boolean)} outputting a singular
         * MPD protocol command if only one command has been queued at call.
         *
         * @return {@code true} if the initiating {@code CommandQueue} had a size of 1, {@code
         * false} otherwise.
         */
        private boolean isSingularCommand() {
            return mPosition == 0 && !mResponse.contains(MPDConnection.MPD_CMD_BULK_SEP) &&
                    shouldIncludeResponse(0);
        }

        /**
         * Returns the next object and advances the iterator.
         *
         * @return the next object.
         * @throws NoSuchElementException if there are no more elements.
         * @see #hasNext
         */
        @Override
        public CommandResponse next() {
            final int[] pos = getNextPositions();

            if (!hasNext()) {
                throw new NoSuchElementException(AbstractResultIterator.NO_MORE_ELEMENTS_REMAIN);
            }

            final CommandResponse response;
            if (isSingularCommand()) {
                response = new CommandResponse(mConnectionResponse, mResponse, null);

                mPosition = mResponse.length();
            } else {
                final String nextLine = mResponse.substring(pos[BEGIN_POSITION], pos[END_POSITION]);

                mPositionIndex = pos[POSITION_INDEX];
                mPosition = pos[END_POSITION] + OK_LENGTH;

                response = new CommandResponse(mConnectionResponse, nextLine, null);
            }

            mCachedPositions = null;

            return response;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException(AbstractResultIterator.UNSUPPORTED);
        }

        /**
         * This method checks {@link #mExcludeResults} to ensure that the current
         * {@code responsePosition} should be included in iteration.
         *
         * @param resultPosition The current result position.
         * @return True if the current result should be included in iteration, false otherwise.
         */
        private boolean shouldIncludeResponse(final int resultPosition) {
            return mExcludeResults == null ||
                    Arrays.binarySearch(mExcludeResults, resultPosition) < 0;
        }

        @Override
        public String toString() {
            return "SeparatedResponseIterator{" +
                    "mResult=" + mResponse +
                    ", mConnectionResponse='" + mConnectionResponse + '\'' +
                    ", mPosition=" + mPosition +
                    '}';
        }
    }
}
