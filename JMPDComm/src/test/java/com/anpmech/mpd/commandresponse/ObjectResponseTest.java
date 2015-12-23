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

import com.anpmech.mpd.TestTools;
import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.connection.CommandResultCreator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * This class is a set of generic methods to test {@link ObjectResponse} subclasses.
 *
 * @param <T> The type of {@code Object} the S type iterates over.
 * @param <S> The type of {@link ObjectResponse} to test.
 */
public abstract class ObjectResponseTest<T, S extends ObjectResponse<T>> {

    /**
     * This is the error message given when a lower Iterator bound has been exceeded.
     */
    private static final String LOWER_BOUNDS_ERROR
            = "Iterator allowed to exceed lower iterator bounds.";

    /**
     * This is the error message given when an upper Iterator bound has been exceeded.
     */
    private static final String UPPER_BOUNDS_ERROR
            = "Iterator allowed to exceed upper iterator bounds.";

    /**
     * This field allows the expected exception to be changed from none to a specific one.
     */
    @Rule
    public final ExpectedException mException = ExpectedException.none();

    /**
     * If iterators are going to fail, they often get caught in an infinite loop. This rule ensures
     * failure after 10 seconds.
     */
    @Rule
    public final Timeout mTimeout = new Timeout(10L, TimeUnit.SECONDS);

    /**
     * Sole constructor.
     */
    protected ObjectResponseTest() {
        super();
    }

    /**
     * This method asserts that when ObjectResponse subclass CommandResult is empty, that the
     * subclass also says it's empty.
     */
    @Test
    public void emptyResponseIsEmpty() {
        assertTrue(getEmptyResponse().isEmpty());
    }

    /**
     * This method tests the {@link Iterator} size of a forward iterator with a reverse iterator.
     *
     * @throws IOException Thrown if there is a issue retrieving the result file.
     */
    @Test
    public void forwardReverseConsistencyTest() throws IOException {
        final S response = instantiate(getResult());
        final List<T> list = response.getList();
        final List<T> reverseIterated = TestTools.reverseList(response);

        Collections.reverse(reverseIterated);

        assertEquals(list, reverseIterated);
    }

    /**
     * This returns a empty ObjectResponse for the ObjectResponse subclass.
     *
     * @return A empty ObjectResponse.
     */
    protected abstract S getEmptyResponse();

    /**
     * This returns a path to a test sample file to construct a CommandResult from.
     *
     * @return A path to a test sample file.
     */
    protected abstract String getResponsePath();

    /**
     * This method simply creates a {@link CommandResponse} with the path from {@link
     * #getResponsePath()}.
     *
     * @return A CommandResult.
     * @throws IOException Thrown if there is a issue retrieving the result file.
     */
    protected CommandResult getResult() throws IOException {
        return CommandResultCreator.generate(getResponsePath());
    }

    /**
     * This method instantiates the ObjectResponse type from the {@code CommandResult} parameter.
     *
     * @param result The {@code CommandResult} to create the ObjectResponse type from.
     * @return A ObjectResponse subclass type.
     */
    protected abstract S instantiate(final CommandResult result);

    /**
     * This method ensures the {@link ObjectResponse} subclass throws a {@link
     * NoSuchElementException} upon the lower bounds of the {@link Iterator}.
     *
     * @throws IOException Thrown if there is a issue retrieving the result file.
     */
    @Test
    public void lowerIteratorBounds() throws IOException {
        mException.expect(NoSuchElementException.class);
        mException.reportMissingExceptionWithMessage(LOWER_BOUNDS_ERROR);

        final ListIterator<T> iterator = instantiate(getResult()).listIterator();
        iterator.previous();
    }

    /**
     * This method tests that an empty iterator correctly answers the {@link
     * ListIterator#hasPrevious()} call.
     */
    @Test
    public void lowerIteratorEmptyBounds() {
        final ListIterator<T> iterator = getEmptyResponse().listIterator();

        assertFalse(LOWER_BOUNDS_ERROR, iterator.hasPrevious());
    }

    @Test
    public void lowerIteratorHasBounds() throws IOException {
        final ListIterator<T> iterator = instantiate(getResult()).listIterator();

        assertFalse(LOWER_BOUNDS_ERROR, iterator.hasPrevious());
    }

    /**
     * This method checks the optimized size method to ensure it always equals 0 with an empty
     * response.
     *
     * @throws IOException Thrown if there is a issue retrieving the result file.
     */
    @Test
    public void testEmptySize() throws IOException {
        final String message = "Empty response failed to equal 0";

        assertEquals(message, 0L, (long) getEmptyResponse().size());
    }

    /**
     * This method checks the optimized size() method against a typical iteration.
     *
     * @throws IOException Thrown if there is a issue retrieving the result file.
     */
    @Test
    public void testSize() throws IOException {
        final S response = instantiate(getResult());

        int expectedSize = 0;

        for (final Object object : response) {
            expectedSize++;
        }

        final String message
                = "Optimized size method failed to produce the same result as a typical iteration.";
        assertEquals(message, (long) expectedSize, (long) response.size());
    }

    /**
     * This method tests the upper Iterator bounds are not exceeded on a {@link
     * Iterator#next()}.
     *
     * @throws IOException Thrown if there is a issue retrieving the result file.
     */
    @Test
    public void upperIteratorBounds() throws IOException {
        mException.expect(NoSuchElementException.class);
        mException.reportMissingExceptionWithMessage("Iterator must fail at upper bounds.");

        final ListIterator<T> iterator = instantiate(getResult()).reverseListIterator();

        iterator.next();
    }

    /**
     * This method tests the upper {@link Iterator} bounds on a empty {@link CommandResult}.
     */
    @Test
    public void upperIteratorEmptyBounds() {
        final Iterator<T> iterator = getEmptyResponse().iterator();

        assertFalse(UPPER_BOUNDS_ERROR, iterator.hasNext());
    }

    /**
     * This method tests the upper Iterator bounds are not exceeded on a {@link
     * ObjectResponse#reverseListIterator()}.
     *
     * @throws IOException Thrown if there is a issue retrieving the result file.
     */
    @Test
    public void upperIteratorHasBounds() throws IOException {
        final Iterator<T> iterator = instantiate(getResult()).reverseListIterator();

        assertFalse(UPPER_BOUNDS_ERROR, iterator.hasNext());
    }
}