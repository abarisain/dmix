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

package com.anpmech.mpd.connection;

import com.anpmech.mpd.MPDCommand;
import com.anpmech.mpd.commandresponse.CommandResponse;
import com.anpmech.mpd.commandresponse.SplitCommandResponse;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * This is the core of the {@link CommandResponse} classes.
 *
 * <p>This class is subclassed to process any MPD protocol server responses. This class is
 * immutable, thus, thread-safe.</p>
 */
public class CommandResult {

    /**
     * The class log identifier.
     */
    private static final String TAG = "CommandResult";

    /**
     * The result of the connection initiation.
     */
    protected final String mConnectionResult;

    /**
     * This is used to exclude specific responses when utilizing the
     * {@link SplitCommandResponse}.
     */
    protected final int[] mExcludeResponses;

    /**
     * The MPD protocol command response.
     */
    protected final String mResult;

    /**
     * This is a mutable hint for list size.
     */
    protected int mListSize = 16;

    /**
     * This class is used to subclass a CommandResult.
     *
     * @param result The result to subclass.
     */
    protected CommandResult(final CommandResult result) {
        this(result.mConnectionResult, result.mResult, result.mExcludeResponses);
    }

    /**
     * This constructor is used to create a new core result from the MPD protocol.
     *
     * @param connectionResult The result of the connection initiation.
     * @param result           The MPD protocol command result.
     * @param excludeResponses This is used to manually exclude responses from split
     *                         CommandResponse inclusion.
     */
    protected CommandResult(final String connectionResult, final String result,
            final int[] excludeResponses) {
        super();

        mConnectionResult = connectionResult;
        mResult = result;

        if (excludeResponses == null) {
            mExcludeResponses = null;
        } else {
            //noinspection AssignmentToCollectionOrArrayFieldFromParameter
            mExcludeResponses = excludeResponses;
        }
    }

    /**
     * Converts a Iterator&lt;&gt; to a List&lt;&gt; of the same type if the entry key matches the
     * {@code key} parameter.
     *
     * @param collection The collection to add the entries from the Iterator to.
     * @param iterator   The iterator with entries to add to the collection.
     * @param <T>        The type of the Iterator entry.
     * @return True if the list was modified, false otherwise.
     */
    protected static <T> boolean addAll(final Collection<T> collection,
            final Iterator<? extends T> iterator) {
        final int hash = collection.hashCode();

        while (iterator.hasNext()) {
            collection.add(iterator.next());
        }

        return hash != collection.hashCode();
    }

    /**
     * Returns the first string response from the media server after connection. This method is
     * mainly for debugging.
     *
     * @return A string representation of the connection result.
     * @see #getMPDVersion() Use of this method is preferred.
     */
    public String getConnectionResult() {
        return mConnectionResult;
    }

    /**
     * Processes the {@code CommandResponse} connection response to store the current media server
     * MPD protocol version.
     *
     * @return Returns the MPD version retained from the connection result.
     */
    public int[] getMPDVersion() {
        final int subHeaderLength = (MPDConnection.CMD_RESPONSE_OK + " MPD ").length();
        final String formatResponse = mConnectionResult.substring(subHeaderLength);

        final StringTokenizer stringTokenizer = new StringTokenizer(formatResponse, ".");
        final int[] version = new int[stringTokenizer.countTokens()];
        int i = 0;

        while (stringTokenizer.hasMoreElements()) {
            version[i] = Integer.parseInt(stringTokenizer.nextToken());
            i++;
        }

        return version;
    }

    /**
     * This checks the connection response for validity.
     *
     * @return True if the connection header exists, false otherwise.
     */
    public boolean isHeaderValid() {
        return mConnectionResult != null;
    }

    @Override
    public String toString() {
        return "CommandResult{" +
                "mConnectionResult='" + mConnectionResult + '\'' +
                ", mExcludeResponses=" + Arrays.toString(mExcludeResponses) +
                ", mResult='" + mResult + '\'' +
                ", mListSize=" + mListSize +
                '}';
    }

    /**
     * This class is used to create Iterators to iterate over a MPD command response.
     *
     * @param <T> The type to iterate over.
     */
    protected abstract static class AbstractResultIterator<T> implements ListIterator<T> {

        /**
         * The error given if no more elements remain for this iterator instance.
         */
        public static final String NO_MORE_ELEMENTS_REMAIN = "No more elements remain.";

        /**
         * The error given if trying an operation which this iterator doesn't support.
         */
        public static final String UNSUPPORTED = "Operation unsupported by this iterator.";

        private static final String INDEX_CANNOT_MATCH = "Position and next index can't match.";

        /**
         * The MPD protocol command response.
         */
        protected final String mResult;

        /**
         * The current position of this iterator relative to the response.
         */
        protected int mPosition;

        /**
         * This is the cache for {@link #nextIndex()}, Integer.MIN_VALUE if invalid.
         */
        private int mNextIndexCache = Integer.MIN_VALUE;

        /**
         * This is the cache for {@link #previousIndex()}, Integer.MIN_VALUE if invalid.
         */
        private int mPreviousIndexCache = Integer.MIN_VALUE;

        /**
         * Sole constructor.
         *
         * @param result   The MPD protocol command result.
         * @param position The position relative to the result to initiate the {@link #mPosition}
         *                 to.
         */
        protected AbstractResultIterator(final String result, final int position) {
            super();

            mResult = result;
            mPosition = position;
        }

        /**
         * The add operation is invalid for this iterator.
         *
         * @param object The object to insert.
         */
        @Override
        public void add(final T object) {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }

        /**
         * Checks for next element, if not throws an exception.
         */
        protected void checkNext() {
            if (!hasNext()) {
                throw new NoSuchElementException(NO_MORE_ELEMENTS_REMAIN);
            }
        }

        /**
         * Checks for previous element, if not throws an exception.
         */
        protected void checkPrevious() {
            if (!hasPrevious()) {
                throw new NoSuchElementException(NO_MORE_ELEMENTS_REMAIN);
            }
        }

        /**
         * Returns the next MPD result line.
         *
         * @return The next MPD result line.
         */
        protected String getNextLine() {
            final int index = nextIndex();

            if (mPosition == index) {
                throw new IllegalStateException(INDEX_CANNOT_MATCH);
            }

            return mResult.substring(mPosition, index);
        }

        /**
         * Returns the previous MPD result line.
         *
         * @return The previous MPD result line.
         */
        protected String getPreviousLine() {
            int index = previousIndex();

            if (mPosition == index) {
                throw new IllegalStateException(INDEX_CANNOT_MATCH);
            }

            /** + 1 to discard the newline. */
            if (index != 0) {
                index += 1;
            }

            return mResult.substring(index, mPosition);
        }

        /**
         * Returns whether there are more elements to iterate.
         *
         * @return {@code true} If there are more elements, {@code false} otherwise.
         * @see #next
         */
        @Override
        public boolean hasNext() {
            return nextIndex() != -1;
        }

        /**
         * Returns whether there are previous elements to iterate.
         *
         * @return {@code true} If there are previous elements, {@code false} otherwise.
         * @see #previous
         */
        @Override
        public boolean hasPrevious() {
            return previousIndex() != -1;

        }

        /**
         * This method resets the index cache.
         */
        private void invalidateCache() {
            mNextIndexCache = Integer.MIN_VALUE;
            mPreviousIndexCache = Integer.MIN_VALUE;
        }

        /**
         * Returns the index of the next object in the iteration.
         *
         * @return The index of the next object, or the size of the list if the iterator is at the
         * end.
         * @see #next
         */
        @Override
        public int nextIndex() {
            final int index;

            if (mNextIndexCache == Integer.MIN_VALUE) {
                index = mResult.indexOf(MPDCommand.MPD_CMD_NEWLINE, mPosition);

                mNextIndexCache = index;
            } else {
                index = mNextIndexCache;
            }

            return index;
        }

        /**
         * Returns the index of the previous object in the iteration.
         *
         * @return The index of the previous object, or -1 if the iterator is at the beginning.
         * @see #previous
         */
        @Override
        public int previousIndex() {
            int index;

            if (mPreviousIndexCache == Integer.MIN_VALUE) {
                /** - 1 to discard the newline. */
                index = mResult.lastIndexOf(MPDCommand.MPD_CMD_NEWLINE, mPosition - 1);

                if (index == -1 && mPosition != 0) {
                    index = 0;
                }

                mPreviousIndexCache = index;
            } else {
                index = mPreviousIndexCache;
            }

            return index;
        }

        /**
         * This object is immutable and it's contents cannot be removed, this method is not
         * supported.
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }

        /**
         * This object is immutable and it's contents cannot be set, this method is not
         * supported.
         *
         * @param object The object to set.
         */
        @Override
        public void set(final T object) {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }

        /**
         * This changes the position assignment to the next newline.
         */
        protected void setPositionNext() {
            mPosition = nextIndex() + 1;
            invalidateCache();
        }

        /**
         * This changes the position assignment to the previous newline.
         */
        protected void setPositionPrevious() {
            mPosition = previousIndex();
            invalidateCache();
        }

        @Override
        public String toString() {
            return "AbstractResultIterator{" +
                    "mResult='" + mResult + '\'' +
                    ", mPosition=" + mPosition +
                    ", mNextIndexCache=" + mNextIndexCache +
                    ", mPreviousIndexCache=" + mPreviousIndexCache +
                    '}';
        }
    }
}
