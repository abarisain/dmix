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

import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.item.Directory;
import com.anpmech.mpd.item.Music;
import com.anpmech.mpd.item.PlaylistFile;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This Object provides a generic abstraction instantiation for a result iterator.
 *
 * @param <T> The type of Object to be generated from the {@link CommandResult}.
 */
abstract class AbstractObjectResult<T> extends AbstractResult<T> {

    /**
     * This is a list of all tokens which begin a new block.
     */
    protected static final String[] ENTRY_BLOCK_TOKENS = {Directory.RESPONSE_DIRECTORY,
            Music.RESPONSE_FILE, PlaylistFile.RESPONSE_PLAYLIST_FILE};

    /**
     * Sole constructor.
     *
     * @param result   The MPD protocol command result.
     * @param position The position relative to the result to initiate the {@link #mPosition} to.
     * @throws IllegalArgumentException If the position parameter is less than 0.
     */
    protected AbstractObjectResult(final String result, final int position) {
        super(result, position);
    }

    /**
     * This method counts a Void iterator for the sum of it's iteration.
     *
     * @param iterator The iterator to count the iterations of.
     * @return The size of the number of items from this Void iterator.
     */
    static int count(final Iterator<Void> iterator) {
        int size = 0;

        while (iterator.hasNext()) {
            size++;
            iterator.next();
        }

        return size;
    }

    /**
     * Override this to create the Object using the response block.
     *
     * @param responseBlock The response block to create the Object from.
     * @return The object created from the response block.
     */
    protected abstract T instantiate(final String responseBlock);

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

    /**
     * This is used to implement a Void iterator.
     *
     * @return null
     */
    Void voidNext() {
        checkNext();

        setPositionNext();

        return null;
    }
}
