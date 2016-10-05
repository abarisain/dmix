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

import com.anpmech.mpd.subsystem.AudioOutput;

import java.util.Iterator;

/**
 * This class instantiates an {@link Iterator} to iterate over {@link AudioOutput} entries.
 */
public class AudioOutputIterator extends MultiLineResult<AudioOutput> {

    /**
     * Class log identifier.
     */
    private static final String TAG = "AudioOutputIterator";

    /**
     * Sole constructor.
     *
     * @param response The MPD protocol command response.
     * @param position The position relative to the response to initiate the {@link #mPosition} to.
     */
    public AudioOutputIterator(final String response, final int position) {
        /**
         * The initial line of this response is implementation dependent. Use the constructor
         * which will generate the beginning block from the first key.
         */
        super(response, position);
    }

    /**
     * Returns a count of how many objects this {@code Collection} contains.
     *
     * @param result The MPD result to get the size for.
     * @return how many objects this {@code Collection} contains, or {@link Integer#MAX_VALUE}
     * if there are more than {@link Integer#MAX_VALUE} elements in this
     * {@code Collection}.
     */
    public static int size(final String result) {
        return MultiLineResult.count(result);
    }

    /**
     * Override this to create the Object using the response block.
     *
     * @param responseBlock The response block to create the Object from.
     * @return The object created from the response block.
     */
    @Override
    protected AudioOutput instantiate(final String responseBlock) {
        return new AudioOutput(responseBlock);
    }
}