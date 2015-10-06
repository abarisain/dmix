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


import com.anpmech.mpd.commandresponse.CommandResponse;

/**
 * This is the core of the {@link CommandResponse} classes, abstracted for the Java backend.
 *
 * <p>This class contains the bare results from the connection. Processing required from this
 * result should be done in a subclass.</p>
 *
 * <p>This class is subclassed to process any MPD protocol server responses. This class is
 * immutable, thus, thread-safe.</p>
 */
public class CommandResult extends AbstractCommandResult {

    /**
     * This constructor is used to subclass a CommandResult.
     *
     * @param result The result to subclass.
     */
    protected CommandResult(final CommandResult result) {
        this(result.mConnectionResult, result.mResult, result.mExcludeResponses, result.mListSize);
    }

    /**
     * This constructor is used to create a new core result from the MPD protocol.
     *
     * @param connectionResult The result of the connection initiation.
     * @param result           The MPD protocol command result.
     * @param excludeResponses This is used to manually exclude responses from a split iterator.
     */
    protected CommandResult(final String connectionResult, final String result,
            final int[] excludeResponses) {
        this(connectionResult, result, excludeResponses, 16);
    }

    /**
     * This constructor is used to create a new core result from the MPD protocol.
     *
     * @param connectionResult The result of the connection initiation.
     * @param result           The MPD protocol command result.
     * @param excludeResponses This is used to manually exclude responses from a split iterator.
     * @param listSize         This is the size of this object if it is created as a {@link
     *                         java.util.List}; which is to say how many newlines + 1 which can be
     *                         found in the {@link #mResult} field. This value is simply a helper,
     *                         and, is typically, generated during first iteration.
     */
    private CommandResult(final String connectionResult, final String result,
            final int[] excludeResponses, final int listSize) {
        super(connectionResult, result, excludeResponses, listSize);
    }
}
