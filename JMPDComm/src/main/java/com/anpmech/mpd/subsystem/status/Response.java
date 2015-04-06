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

package com.anpmech.mpd.subsystem.status;

import java.util.concurrent.TimeUnit;

/**
 * This is a base interface for {@link com.anpmech.mpd.subsystem.status.ResponseMap} methods.
 */
interface Response {

    /**
     * Lets callers know if the subclass Object is valid.
     *
     * @return True if the subclass is valid, false otherwise.
     */
    boolean isValid();

    /**
     * Retrieves a string representation of the {@link ResponseMap} and this object.
     *
     * @return A string representation of the ResponseMap and this resulting object.
     */
    String toString();

    /**
     * Blocks indefinitely until this object is valid.
     *
     * @throws InterruptedException If the current thread is {@link Thread#interrupted()}.
     */
    void waitForValidity() throws InterruptedException;

    /**
     * Blocks for the given waiting time.
     *
     * @param timeout The time to wait for a valid object.
     * @param unit    The time unit of the {@code timeout} argument.
     * @return {@code true} if a the {@code Response} was valid by the time of return, false
     * otherwise.
     * @throws InterruptedException If the current thread is {@link Thread#interrupted()}.
     */
    boolean waitForValidity(final long timeout, final TimeUnit unit) throws InterruptedException;
}
