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

package com.anpmech.mpd.concurrent;

import com.anpmech.mpd.commandresponse.CommandResponse;
import com.anpmech.mpd.exception.MPDException;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This class returns a {@link CommandResponse} in the future.
 */
public class ResponseFuture extends ResultFuture {

    /**
     * The sole constructor.
     *
     * @param future The ResultFuture to wrap with the {@link CommandResponse} future.
     */
    public ResponseFuture(final ResultFuture future) {
        super(future);
    }

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
     *
     * @param timeout The maximum time to wait.
     * @param unit    The time unit of the timeout argument.
     * @return The computed result.
     * @throws CancellationException If the computation was cancelled.
     * @throws IOException           Thrown upon a communication error with the server.
     * @throws MPDException          Thrown if an error occurs as a result of command execution.
     * @throws TimeoutException      If the wait timed out.
     */
    @Override
    public CommandResponse get(final long timeout, final TimeUnit unit)
            throws IOException, MPDException, TimeoutException {
        return new CommandResponse(super.get(timeout, unit));
    }

    /**
     * Waits if necessary for the computation to complete, and then retrieves its result.
     *
     * @return The computed result.
     * @throws CancellationException If the computation was cancelled.
     * @throws IOException           Thrown upon a communication error with the server.
     * @throws MPDException          Thrown if an error occurs as a result of command execution.
     */
    @Override
    public CommandResponse get() throws IOException, MPDException {
        return new CommandResponse(super.get());
    }
}
