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

import java.util.NoSuchElementException;

/**
 * This class implements a {@link MultiLineResult} simply for counting iterations with less
 * required garbage collection.
 */
class MultiLineNoop extends MultiLineResult<Void> {

    /**
     * This constructor is used when the first token found in a response is used as the
     * beginning and ending delimiter for a result.
     *
     * <p>This is used for MPD protocol results which have one single type of information
     * in the result.</p>
     *
     * @param result The MPD protocol command result.
     * @throws IllegalArgumentException If the position parameter is less than 0.
     */
    MultiLineNoop(final String result) {
        super(result, 0);
    }

    /**
     * This constructor is used when either the beginning tokens or the ending tokens must
     * be defined.
     *
     * @param result           The MPD protocol command result.
     * @param beginBlockTokens The block tokens to find the beginning of a block. This
     *                         array must be sorted in ascending natural order prior to
     *                         calling this constructor.
     * @param endBlockTokens   The block tokens to find the ending of a block. This array
     *                         must be sorted in ascending natural order prior to calling
     *                         this constructor.
     * @throws IllegalArgumentException If the position parameter is less than 0.
     */
    MultiLineNoop(final String result, final String[] beginBlockTokens,
            final String[] endBlockTokens) {
        super(result, 0, beginBlockTokens, endBlockTokens);
    }

    /**
     * Override this to create the Object using the response block.
     *
     * @param responseBlock The response block to create the Object from.
     * @return The object created from the response block.
     */
    @Override
    protected Void instantiate(final String responseBlock) {
        return null;
    }

    /**
     * Returns the next object in the iteration.
     *
     * @return the next object.
     * @throws NoSuchElementException If there are no more elements.
     * @see #hasNext
     */
    @Override
    public Void next() {
        return voidNext();
    }
}
