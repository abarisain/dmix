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

import com.anpmech.mpd.MPDCommand;
import com.anpmech.mpd.Tools;
import com.anpmech.mpd.connection.AbstractCommandResult;
import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.item.Directory;
import com.anpmech.mpd.item.Music;
import com.anpmech.mpd.item.PlaylistFile;

import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * This class contains classes and methods used to process a {@link CommandResult}.
 *
 * <p>This class is immutable, thus, thread-safe.</p>
 */
public class CommandResponse extends ObjectResponse<String> {

    /**
     * The MPD protocol {@code key}:{@code value} delimiter.
     */
    private static final char MPD_KV_DELIMITER = ':';

    /**
     * The class log identifier.
     */
    private static final String TAG = "CommandResponse";

    /**
     * This constructor builds this class from the MPD protocol result.
     *
     * @param connectionResult The result of the connection initiation.
     * @param result           The MPD protocol command result.
     */
    protected CommandResponse(final String connectionResult, final String result) {
        super(connectionResult, result);
    }

    /**
     * This constructor is used for subclassing.
     *
     * @param result The CommandResult to subclass.
     */
    public CommandResponse(final CommandResult result) {
        super(result);
    }

    /**
     * This constructor builds this class from an empty MPD protocol result.
     */
    public CommandResponse() {
        super();
    }

    /**
     * This method returns a iterator, starting at the beginning of the response.
     *
     * @param position The position to begin the iterator at, typically beginning or end.
     * @return A iterator to return the response.
     * @see #getList()
     */
    @Override
    protected ListIterator<String> listIterator(final int position) {
        return new ResultIterator(mResult, position);
    }

    /**
     * Returns the some results from the command response.
     *
     * @return Some results from the command response.
     */
    @Override
    public String toString() {
        return "CommandResponse{" +
                "mResult='" + mResult + '\'' +
                ", mConnectionResult='" + mConnectionResult + '\'' +
                ", mListSize=" + mListSize +
                '}';
    }

    /**
     * This Object provides a generic abstraction instantiation for a result iterator.
     *
     * @param <T> The type of Object to be generated from the {@link CommandResult}.
     */
    protected abstract static class AbstractObjectResultIterator<T>
            extends AbstractResultIterator<T> {

        /**
         * Sole constructor.
         *
         * @param result   The MPD protocol command result.
         * @param position The position relative to the result to initiate the {@link #mPosition}
         *                 to.
         * @throws IllegalArgumentException If the position parameter is less than 0.
         */
        protected AbstractObjectResultIterator(final String result, final int position) {
            super(result, position);
        }

        /**
         * Override this to create the Object using the response block.
         *
         * @param responseBlock The response block to create the Object from.
         * @return The object created from the response block.
         */
        abstract T instantiate(final String responseBlock);

        /**
         * Returns the next object in the iteration.
         *
         * @return the next object.
         * @throws NoSuchElementException If there are no more elements.
         * @see #hasNext
         */
        @Override
        public T next() {
            checkNext();

            final String nextLine = getNextLine();
            setPositionNext();

            return instantiate(nextLine);
        }

        /**
         * Returns the previous object in the iteration.
         *
         * @return the previous object.
         * @throws NoSuchElementException If there are no previous elements.
         * @see #hasPrevious
         */
        @Override
        public T previous() {
            checkPrevious();

            final String previousLine = getPreviousLine();
            setPositionPrevious();

            return instantiate(previousLine);
        }
    }

    /**
     * This class is an abstraction to build classes which use entire response blocks.
     *
     * <p>This class is used when the Iterator will skip no lines of the response. This typically
     * used for Iterators which output Strings.</p>
     *
     * @param <T> The type of Object to be generated from the {@link CommandResult}.
     */
    protected abstract static class FullBlockResultIterator<T>
            extends AbstractObjectResultIterator<T> {

        /**
         * Sole constructor.
         *
         * @param response The MPD protocol command response.
         * @param position The position relative to the response to initiate the
         *                 {@link AbstractCommandResult.AbstractResultIterator#mPosition} to.
         */
        protected FullBlockResultIterator(final String response, final int position) {
            super(response, position);
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

            /**
             * If the position is the final character, it should be a newline, don't allow in the
             * Iterator.
             */
            if (mPosition == mResult.length() && mResult.charAt(mResult.length() - 1) == '\n') {
                position = mResult.length() - 1;
            } else {
                position = mPosition;
            }

            return position;
        }
    }

    /**
     * This class is used to create an {@link Iterator} to iterate over a result to find a defined
     * beginning block token and end block token to create an Object.
     *
     * <p>This Iterator should be used for iteration which requires more than one line and ends
     * with a block token rather than a {@code newline}.</p>
     *
     * @param <T> The type of Object to be generated from the {@link CommandResult}.
     */
    protected abstract static class MultiLineResultIterator<T>
            extends PartialBlockResultIterator<T> {

        /**
         * The block tokens to tokenize for the beginning of the iteration for this iterator.
         */
        private final String[] mEndBlockTokens;

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
        protected MultiLineResultIterator(final String result, final int position) {
            super(result, position);

            mEndBlockTokens = mBeginBlockTokens;
        }

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
         * @param endBlockTokens   The block tokens to find the ending of a block. This array must
         *                         be sorted in ascending natural order prior to calling this
         *                         constructor.
         * @throws IllegalArgumentException If the position parameter is less than 0.
         */
        protected MultiLineResultIterator(final String result, final int position,
                final String[] beginBlockTokens, final String[] endBlockTokens) {
            super(result, position, beginBlockTokens);

            mEndBlockTokens = endBlockTokens;
        }

        /**
         * This method returns the index of the next ending token in relation to the current
         * position.
         *
         * @return The next ending token in relation to the current position.
         */
        @Override
        protected int nextIndexEnd() {
            int end = Tools.getNextKeyIndex(mResult, cachedNextIndexBegin() + 1, mEndBlockTokens);

            /**
             * Retrieve the final element; this only occurs if the beginning was found, so, this
             * terminates at the end of the line.
             */
            if (end == -1 && mPosition != mResult.length()) {
                end = mResult.length();
            }

            return end;
        }

        /**
         * This method returns the index of the prior ending token in relation to the current
         * position.
         *
         * @return The prior ending token in relation to the current position.
         */
        @Override
        protected int previousIndexEnd() {
            final int begin = cachedPreviousIndexBegin();
            int end = Tools.getNextKeyIndex(mResult, begin + 1, mEndBlockTokens);

            if (end == -1 && mPosition == mResult.length()) {
                end = mResult.length();
            }

            return end;
        }
    }

    /**
     * This class is used to create an {@link Iterator} to iterate over a result to find a defined
     * beginning block, ending with a newline, to create an Object.
     *
     * <p>This is an abstraction class to create a block {@link Iterator} which uses a partial
     * result.</p>
     *
     * @param <T> The type of Object to be generated from the {@link CommandResult}.
     */
    private abstract static class PartialBlockResultIterator<T>
            extends AbstractObjectResultIterator<T> {

        /**
         * This is a list of all tokens which begin a new block.
         */
        protected static final String[] ENTRY_BLOCK_TOKENS = {Directory.RESPONSE_DIRECTORY,
                Music.RESPONSE_FILE, PlaylistFile.RESPONSE_PLAYLIST_FILE};

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
        protected PartialBlockResultIterator(final String result, final int position) {
            super(result, position);

            final int index = result.indexOf(MPD_KV_DELIMITER);

            if (index == -1) {
                mBeginBlockTokens = new String[]{};
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
         * @param beginBlockTokens The block tokens to find the beginning of a block. This array
         *                         must be sorted in ascending natural order prior to calling this
         *                         constructor.
         * @throws IllegalArgumentException If the position parameter is less than 0.
         */
        protected PartialBlockResultIterator(final String result, final int position,
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
         * @return The prior beginning token in relation to the current position.
         */
        @Override
        protected int previousIndexBegin() {
            int index = -1;
            int mpdDelimiterIndex = mResult.lastIndexOf(MPD_KV_DELIMITER, mPosition);
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
                    mpdDelimiterIndex = mResult.lastIndexOf(MPD_KV_DELIMITER,
                            mpdDelimiterIndex - 1);
                }
            }

            return index;
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
    }

    /**
     * This class is used to create an {@link Iterator} to iterate over each individual line of a
     * MPD command response.
     */
    public static final class ResultIterator extends FullBlockResultIterator<String> {

        /**
         * Sole constructor.
         *
         * @param result   The MPD protocol command result.
         * @param position The position relative to the result to initiate the
         *                 {@link AbstractCommandResult.AbstractResultIterator#mPosition} to.
         */
        private ResultIterator(final String result, final int position) {
            super(result, position);
        }

        /**
         * Override this to create the Object using the response block.
         *
         * @param responseBlock The response block to create the Object from.
         * @return The object created from the response block.
         */
        @Override
        String instantiate(final String responseBlock) {
            return responseBlock;
        }
    }

    /**
     * This class is used to create an {@link Iterator} to iterate over a result for a specific
     * line to create an Object.
     *
     * @param <T> The type to create from this response.
     */
    protected abstract static class SingleLineResultIterator<T>
            extends PartialBlockResultIterator<T> {

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
        protected SingleLineResultIterator(final String result, final int position,
                final String[] beginBlockTokens) {
            super(result, position, beginBlockTokens);
        }

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
        protected SingleLineResultIterator(final String result, final int position) {
            super(result, position);
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
         * This method returns the index of the prior ending token in relation to the current
         * position.
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
    }
}
