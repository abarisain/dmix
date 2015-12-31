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

package com.anpmech.mpd.concurrent;

import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.exception.MPDException;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

abstract class MPDFuture<T> {

    /**
     * The message given when a connection has been lost.
     */
    protected static final String LOST_CONNECTION = "Lost connection.";

    /**
     * The future to be wrapped.
     */
    protected final Future<?> mFuture;

    /**
     * This constructor is used for subclassing this class.
     *
     * @param future The future to be subclassed.
     */
    MPDFuture(final ResultFuture future) {
        super();

        mFuture = future.mFuture;
    }

    /**
     * Creates a {@code FutureTask} that will, upon running, execute the given {@code Callable}.
     *
     * @param future The future to wrap.
     */
    MPDFuture(final Future<?> future) {
        super();

        mFuture = future;
    }

    /**
     * Wraps and throws an exception, based one the instance of the {@code Throwable} instance
     * origin.
     *
     * @param throwable The throwable to throw.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    protected static void throwException(final Throwable throwable)
            throws IOException, MPDException {
        if (throwable instanceof MPDException) {
            throw new MPDException(throwable);
        } else {
            throw new IOException(throwable);
        }
    }

    /**
     * Attempts to cancel execution of this task.
     *
     * <p>This attempt will fail if the task has already completed, has already been cancelled, or
     * could not be cancelled for some other reason. If successful, and this task has not started
     * when {@code cancel} is called, this task should never run.  If the task has already started,
     * then the {@code mayInterruptIfRunning} parameter determines whether the thread executing
     * this task should be interrupted in an attempt to stop the task.</p>
     *
     * <p>After this method returns, subsequent calls to {@link #isDone} will always return
     * {@code true}.  Subsequent calls to {@link #isCancelled} will always return {@code true}
     * if this method returned {@code true}.</p>
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this task should be
     *                              interrupted; otherwise, in-progress tasks are allowed to
     *                              complete
     * @return {@code false} if the task could not be cancelled, typically because it has already
     * completed normally; {@code true} otherwise.
     */
    public boolean cancel(final boolean mayInterruptIfRunning) {
        return mFuture.cancel(mayInterruptIfRunning);
    }

    /**
     * Waits if necessary for the computation to complete, and then retrieves its result.
     *
     * @return The computed result.
     * @throws CancellationException If the computation was cancelled.
     * @throws IOException           Thrown upon a communication error with the server.
     * @throws MPDException          Thrown if an error occurs as a result of command execution.
     */
    public T get() throws IOException, MPDException {
        T result = null;

        try {
            result = instantiate((CommandResult) mFuture.get());
        } catch (final ExecutionException | InterruptedException e) {
            throwException(e.getCause());
        }

        /**
         * For some reason, an exception may not be thrown when going into Airplane Mode.
         */
        if (result == null) {
            throw new IOException(LOST_CONNECTION);
        }

        return result;
    }

    /**
     * Waits if necessary for the computation to complete, and then retrieves its result.
     *
     * @param timeout The maximum time to wait.
     * @param unit    The time unit of the timeout argument.
     * @return The computed result.
     * @throws CancellationException If the computation was cancelled.
     * @throws IOException           Thrown upon a communication error with the server.
     * @throws MPDException          Thrown if an error occurs as a result of command execution.
     * @throws TimeoutException      If the wait timed out.
     */
    public T get(final long timeout, final TimeUnit unit)
            throws IOException, MPDException, TimeoutException {
        T result = null;

        try {
            result = instantiate((CommandResult) mFuture.get(timeout, unit));
        } catch (final ExecutionException | InterruptedException e) {
            throwException(e.getCause());
        }

        /**
         * For some reason, an exception may not be thrown when going into Airplane Mode.
         */
        if (result == null) {
            throw new IOException(LOST_CONNECTION);
        }

        return result;
    }

    /**
     * This method is used to construct the Future object for this result.
     *
     * @param result The result to create the MPDFuture from.
     * @return A MPDFuture subclass.
     */
    abstract T instantiate(final CommandResult result);

    /**
     * Returns {@code true} If this task was cancelled before it completed normally.
     *
     * @return {@code true} If this task was cancelled before it completed.
     */
    public boolean isCancelled() {
        return mFuture.isCancelled();
    }

    /**
     * Returns {@code true} if this task completed.
     *
     * <p>Completion may be due to normal termination, an exception, or cancellation -- in all of
     * these cases, this method will return {@code true}.</p>
     *
     * @return {@code true} If this task has completed.
     */
    public boolean isDone() {
        return mFuture.isDone();
    }

    @Override
    public String toString() {
        return "ResultFuture{" +
                "mFuture=" + mFuture +
                '}';
    }
}
