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

import com.anpmech.mpd.connection.CommandResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * This class is used to create an Object from a {@link CommandResult}.
 *
 * @param <T> The type of object to create from this {@link CommandResult}.
 */
public abstract class ObjectResponse<T> extends CommandResult implements Iterable<T> {

    /**
     * This constructor builds this class from an empty MPD protocol result.
     */
    protected ObjectResponse() {
        super(EMPTY);
    }

    /**
     * This constructor builds this class from the MPD protocol result.
     *
     * @param connectionResult The result of the connection initiation.
     * @param response         The MPD protocol command response.
     */
    protected ObjectResponse(final String connectionResult, final String response) {
        super(connectionResult, response);
    }

    /**
     * This class is used to subclass a CommandResult.
     *
     * @param result The result to subclass.
     */
    protected ObjectResponse(final CommandResult result) {
        super(result);
    }

    /**
     * This method adds all results from this response to the given list.
     *
     * @param list The list to add all elements of this response to.
     */
    public void addAll(final Collection<T> list) {
        addAll(list, iterator());
    }

    /**
     * This returns a List of the results from this response.
     *
     * @return A List of results from this response.
     * @see #listIterator()
     */
    public List<T> getList() {
        final List<T> list = new ArrayList<>(mListSize);

        addAll(list);

        return list;
    }

    /**
     * Returns an {@link Iterator} for the elements in this object.
     *
     * @return An {@code Iterator} instance.
     */
    @Override
    public Iterator<T> iterator() {
        return listIterator();
    }

    /**
     * This method returns a iterator, starting at the beginning of the response.
     *
     * @return A iterator to return the response, line by line.
     * @see #getList()
     */
    public ListIterator<T> listIterator() {
        return listIterator(0);
    }

    /**
     * This method returns a iterator, starting at the beginning of the response.
     *
     * @param position The position to begin the iterator at, typically beginning or end.
     * @return A iterator to return the response.
     * @see #getList()
     */
    protected abstract ListIterator<T> listIterator(final int position);

    /**
     * This method returns a iterator, starting at the end of the response.
     *
     * @return A list iterator positioned to begin at the end of the response.
     */
    public ListIterator<T> reverseListIterator() {
        return listIterator(mResult.length());
    }
}
