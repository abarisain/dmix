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

import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * This class is used to create Iterators to iterate over a MPD command response.
 *
 * @param <T> The type to iterate over.
 */
abstract class AbstractResult<T> implements ListIterator<T>, Iterable<T> {

    /**
     * The MPD protocol {@code key}:{@code value} delimiter.
     */
    protected static final char MPD_KV_DELIMITER = ':';

    /**
     * The exception message given when a index matches the current position.
     */
    private static final String INDEX_CANNOT_MATCH = "Position and position index can't match.";

    /**
     * The error given if no more elements remain for this iterator instance.
     */
    private static final String NO_MORE_ELEMENTS_REMAIN = "No more elements remain.";

    /**
     * The exception message given if trying an operation which this iterator doesn't support.
     */
    private static final String UNSUPPORTED = "Operation unsupported by this iterator.";

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
    protected AbstractResult(final String result, final int position) {
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
