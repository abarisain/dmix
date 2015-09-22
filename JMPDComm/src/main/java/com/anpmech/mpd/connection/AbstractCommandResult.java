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

import com.anpmech.mpd.Tools;
import com.anpmech.mpd.commandresponse.CommandResponse;
import com.anpmech.mpd.commandresponse.SplitCommandResponse;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
public class AbstractCommandResult {

    /**
     * The class log identifier.
     */
    private static final String TAG = "AbstractCommandResult";

    /**
     * The result of the connection initiation.
     */
    @Nullable
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
    protected int mListSize;

    /**
     * This constructor is used to create a new core result from the MPD protocol.
     *
     * @param connectionResult The result of the connection initiation.
     * @param result           The MPD protocol command result.
     * @param excludeResponses This is used to manually exclude responses from split
     *                         CommandResponse inclusion.
     * @param listSize         This is the size to initialize this object to.
     */
    protected AbstractCommandResult(@Nullable final String connectionResult, final String result,
            final int[] excludeResponses, final int listSize) {
        super();

        mConnectionResult = connectionResult;
        mResult = result;

        if (excludeResponses == null) {
            mExcludeResponses = null;
        } else {
            //noinspection AssignmentToCollectionOrArrayFieldFromParameter
            mExcludeResponses = excludeResponses;
        }

        mListSize = listSize;
    }

    /**
     * This constructor is used to create a empty CommandResult.
     */
    protected AbstractCommandResult() {
        this(null, "", new int[]{}, 0);
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


    public boolean contains(@NotNull final CharSequence value) {
        return contains(null, value);
    }

    /**
     * This method checks this result for a specific value.
     *
     * @param key   The key to pair with the value. If null, only the value will be searched for.
     * @param value The value to find in the response.
     * @return True if the value, and key, if applicable, is found, false otherwise.
     */
    public boolean contains(@Nullable final CharSequence key, @NotNull final CharSequence value) {
        final StringBuilder stringBuilder;

        if (key == null) {
            stringBuilder = new StringBuilder(value.length() + 10);
        } else {
            stringBuilder = new StringBuilder(key.length() + value.length() + 10);
            stringBuilder.append(key);
        }
        stringBuilder.append(": ");
        stringBuilder.append(value);
        stringBuilder.append('\n');

        return mResult.contains(stringBuilder);
    }

    /**
     * Compares this instance with the specified object and indicates if they are equal. In order
     * to
     * be equal, {@code o} must represent the same object as this instance using a class-specific
     * comparison. The general contract is that this comparison should be reflexive, symmetric, and
     * transitive. Also, no object reference other than null is equal to null.
     *
     * @param o the object to compare this instance with.
     * @return {@code true} if the specified object is equal to this {@code Object}; {@code false}
     * otherwise.
     * @see #hashCode
     */
    @Override
    public boolean equals(final Object o) {
        Boolean isEqual = null;

        if (this == o) {
            isEqual = Boolean.TRUE;
        } else if (o == null || getClass() != o.getClass()) {
            isEqual = Boolean.FALSE;
        }

        if (isEqual == null || isEqual.equals(Boolean.TRUE)) {
            /** This has to be the same due to the class check above. */
            //noinspection unchecked
            final AbstractCommandResult result = (AbstractCommandResult) o;

            //noinspection ConstantConditions
            if (Tools.isNotEqual(mResult, result.mResult) ||
                    Tools.isNotEqual(mConnectionResult, result.mConnectionResult)) {
                isEqual = Boolean.FALSE;
            }
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual.booleanValue();
    }

    /**
     * This method checks this result for a specific value.
     *
     * @param value The value to find in the response.
     * @return True if the value is found, false otherwise.
     */

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
        if (mConnectionResult == null) {
            throw new IllegalStateException("Cannot retrieve version when invalid.");
        }

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
     * Returns an integer hash code for this object. By contract, any two objects for which {@link
     * #equals} returns {@code true} must return the same hash code value. This means that
     * subclasses of {@code Object} usually override both methods or neither method.
     *
     * <p>Note that hash values must not change over time unless information used in equals
     * comparisons also changes.</p>
     *
     * @return this object's hash code.
     * @see #equals
     */
    @Override
    public int hashCode() {
        int result;

        if (mConnectionResult != null) {
            result = mConnectionResult.hashCode();
        } else {
            result = 0;
        }

        result = 31 * result + mResult.hashCode();

        return result;
    }

    /**
     * Returns true if the result returned nothing other than an OK response.
     *
     * @return True if the result returned OK and nothing more, false otherwise.
     */
    public boolean isEmpty() {
        return mResult.isEmpty();
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
    protected abstract static class AbstractResultIterator<T> implements ListIterator<T>,
            Iterable<T> {

        /**
         * The error given if no more elements remain for this iterator instance.
         */
        public static final String NO_MORE_ELEMENTS_REMAIN = "No more elements remain.";

        /**
         * The exception message given if trying an operation which this iterator doesn't support.
         */
        public static final String UNSUPPORTED = "Operation unsupported by this iterator.";

        /**
         * The exception message given when a index matches the current position.
         */
        private static final String INDEX_CANNOT_MATCH = "Position and position index can't match.";

        /**
         * The MPD protocol command response.
         */
        protected final String mResult;

        /**
         * The current position of this iterator relative to the response.
         */
        protected int mPosition;

        /**
         * This is the cache for {@link #nextIndexBegin()}, {@link Integer#MIN_VALUE} if invalid.
         */
        private int mNextIndexBeginCache = Integer.MIN_VALUE;

        /**
         * This is the cache for {@link #nextIndexEnd()}, {@link Integer#MIN_VALUE} if invalid.
         */
        private int mNextIndexEndCache = Integer.MIN_VALUE;

        /**
         * This is the cache for {@link #previousIndexBegin()}, {@link Integer#MIN_VALUE} if
         * invalid.
         */
        private int mPreviousIndexBeginCache = Integer.MIN_VALUE;

        /**
         * This is the cache for {@link #previousIndexEnd()}, {@link Integer#MIN_VALUE} if invalid.
         */
        private int mPreviousIndexEndCache = Integer.MIN_VALUE;

        /**
         * Sole constructor.
         *
         * @param result   The MPD protocol command result.
         * @param position The position relative to the result to initiate the {@link #mPosition}
         *                 to.
         * @throws IllegalArgumentException if the position parameter is less than 0.
         */
        protected AbstractResultIterator(final String result, final int position) {
            super();

            if (position < 0) {
                throw new IllegalArgumentException("Position must 0 or greater.");
            }

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
         * This is a locally cached version of {@link #nextIndexBegin()}.
         *
         * @return The results of {@link #nextIndexBegin()} from a cache, or generated if cache is
         * invalid.
         */
        protected int cachedNextIndexBegin() {
            if (mNextIndexBeginCache == Integer.MIN_VALUE) {
                mNextIndexBeginCache = nextIndexBegin();
            }

            return mNextIndexBeginCache;
        }

        /**
         * This method returns a locally cached version of {@link #nextIndexEnd()}.
         *
         * @return The results of {@link #nextIndexEnd()} from a cache, or generated if cache is
         * invalid.
         */
        private int cachedNextIndexEnd() {
            if (mNextIndexEndCache == Integer.MIN_VALUE) {
                mNextIndexEndCache = nextIndexEnd();
            }

            if (mPosition == mNextIndexEndCache) {
                throw new IllegalStateException(INDEX_CANNOT_MATCH);
            }

            return mNextIndexEndCache;
        }

        /**
         * This method returns a locally cached version of {@link #previousIndexBegin()}.
         *
         * @return The results of {@link #previousIndexBegin()} from a cache, or generated if cache
         * is
         * invalid.
         */
        protected int cachedPreviousIndexBegin() {
            if (mPreviousIndexBeginCache == Integer.MIN_VALUE) {
                mPreviousIndexBeginCache = previousIndexBegin();
            }

            if (mPosition == mPreviousIndexBeginCache && mPosition != -1) {
                throw new IllegalStateException(INDEX_CANNOT_MATCH);
            }

            return mPreviousIndexBeginCache;
        }

        /**
         * This method returns a locally cached version of {@link #previousIndexEnd()}.
         *
         * @return The results of {@link #previousIndexEnd()} from a cache, or generated if cache is
         * invalid.
         */
        protected int cachedPreviousIndexEnd() {
            if (mPreviousIndexEndCache == Integer.MIN_VALUE) {
                mPreviousIndexEndCache = previousIndexEnd();
            }

            return mPreviousIndexEndCache;
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
            return mResult.substring(cachedNextIndexBegin(), cachedNextIndexEnd());
        }

        /**
         * Returns the previous MPD result line.
         *
         * @return The previous MPD result line.
         */
        protected String getPreviousLine() {
            return mResult.substring(cachedPreviousIndexBegin(), cachedPreviousIndexEnd());
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
            mNextIndexBeginCache = Integer.MIN_VALUE;
            mNextIndexEndCache = Integer.MIN_VALUE;
            mPreviousIndexBeginCache = Integer.MIN_VALUE;
            mPreviousIndexEndCache = Integer.MIN_VALUE;
        }

        /**
         * Returns an {@link Iterator} for the elements in this object.
         *
         * @return An {@code Iterator} instance.
         */
        @Override
        public Iterator<T> iterator() {
            return this;
        }

        /**
         * Returns the index of the next object in the iteration.
         *
         * <p>This will always be equivalent to {@link #nextIndexEnd()}</p>
         *
         * @return The index of the next object, or the size of the list if the iterator is at the
         * end.
         * @see #next
         */
        @Override
        public int nextIndex() {
            return cachedNextIndexEnd();
        }

        /**
         * This method returns the index of the next beginning token in relation to the current
         * position.
         *
         * @return The next beginning token in relation to the current position.
         */
        protected abstract int nextIndexBegin();

        /**
         * This method returns the index of the next ending token in relation to the current
         * position.
         *
         * @return The next ending token in relation to the current position.
         */
        protected abstract int nextIndexEnd();

        /**
         * Returns the index of the previous object in the iteration.
         *
         * <p>This will always be equivalent to {@link #previousIndexBegin()}</p>
         *
         * @return The index of the previous object, or -1 if the iterator is at the beginning.
         * @see #previous
         */
        @Override
        public int previousIndex() {
            return cachedPreviousIndexBegin();
        }

        /**
         * This method returns the index of the prior beginning token in relation to the current
         * position.
         *
         * @return The prior beginning token in relation to the current position.
         */
        protected abstract int previousIndexBegin();

        /**
         * This method returns the index of the prior ending token in relation to the current
         * position.
         *
         * @return The prior ending token in relation to the current position.
         */
        protected abstract int previousIndexEnd();

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
                    ", mNextIndexBeginCache=" + mNextIndexBeginCache +
                    ", mNextIndexEndCache=" + mNextIndexEndCache +
                    ", mPreviousIndexBeginCache=" + mPreviousIndexBeginCache +
                    ", mPreviousIndexEndCache=" + mPreviousIndexEndCache +
                    '}';
        }
    }
}
