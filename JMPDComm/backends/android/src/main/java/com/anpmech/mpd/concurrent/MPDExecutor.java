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

import com.anpmech.mpd.connection.CommandResult;

import android.os.Handler;
import android.os.Looper;

import java.security.PrivilegedAction;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class provides one executor to use for this library, and a helper method to submit a
 * Runnable to the main UI thread in the Android context.
 */
public final class MPDExecutor {

    /**
     * The executor to use to execute {@link Runnable} and {@link Callable} classes off the main
     * thread.
     */
    private static final ScheduledExecutorService EXECUTOR =
            Executors.newScheduledThreadPool(Integer.MAX_VALUE);

    /**
     * The Handler object which uses the main thread looper.
     */
    private static final Handler MAIN_LOOP = new Handler(Looper.getMainLooper());

    /**
     * This is a utility class, no instantiation.
     */
    private MPDExecutor() {
        super();
    }

    /**
     * Submits a runnable for execution in the future with a callback as the contents.
     *
     * @param task  A runnable with a callback as the content.
     * @param delay The time from now to delay execution.
     * @param unit  The {@link TimeUnit} of the delay parameter.
     * @return Returns true if the Runnable was successfully placed in to the message queue.
     * Returns false on failure, usually because the looper processing the message queue is
     * exiting.
     */
    public static MPDFuture schedule(final Runnable task, final long delay, final TimeUnit unit) {
        return new MPDFuture(EXECUTOR.schedule(task, delay, unit));
    }

    /**
     * Submits a value-returning task for execution in the future and returns a Future representing
     * the pending results of the task. The Future's <tt>get</tt> method will return the task's
     * result upon successful completion.
     *
     * <p>If you would like to immediately block waiting for a task, you can use constructions of
     * the form <tt>result = exec.submit(aCallable).get();</tt></p>
     *
     * <p> Note: The {@link Executors} class includes a set of methods that can convert some other
     * common closure-like objects, for example, {@link PrivilegedAction} to {@link Callable} form
     * so they can be submitted.</p>
     *
     * @param task  The task to submit.
     * @param delay The time from now to delay execution.
     * @param unit  The {@link TimeUnit} of the delay parameter.
     * @param <T>   The type used in the parameter will be the type returned.
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException If the task cannot be scheduled for execution.
     * @throws NullPointerException       If the task is null.
     */
    public static <T extends CommandResult> MPDFuture schedule(final Callable<T> task,
            final long delay, final TimeUnit unit) {
        return new MPDFuture(EXECUTOR.schedule(task, delay, unit));
    }

    /**
     * This method shuts down any ExecutorServices running in this class.
     */
    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }

    /**
     * Submits a value-returning task for execution and returns a Future representing the pending
     * results of the task. The Future's <tt>get</tt> method will return the task's result upon
     * successful completion.
     *
     * <p>If you would like to immediately block waiting for a task, you can use constructions of
     * the form <tt>result = exec.submit(aCallable).get();</tt></p>
     *
     * <p> Note: The {@link Executors} class includes a set of methods that can convert some other
     * common closure-like objects, for example, {@link PrivilegedAction} to {@link Callable} form
     * so they can be submitted.</p>
     *
     * @param task the task to submit
     * @param <T>  The type used in the parameter will be the type returned.
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException If the task cannot be scheduled for execution.
     * @throws NullPointerException       If the task is null.
     */
    public static <T extends CommandResult> MPDFuture submit(final Callable<T> task) {
        return new MPDFuture(EXECUTOR.submit(task));
    }

    /**
     * Submits a Runnable task for execution and returns a Future representing that task. The
     * Future's <tt>get</tt> method will return <tt>null</tt> upon <em>successful</em> completion.
     *
     * @param task the task to submit
     * @return A MPDFuture without a get() payload.
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException       if the task is null
     */
    public static MPDFuture submit(final Runnable task) {
        return new MPDFuture(EXECUTOR.submit(task));
    }

    /**
     * Submits a runnable with a callback as the contents.
     *
     * @param task A runnable with a callback as the content.
     * @return Returns true if the Runnable was successfully placed in to the message queue.
     * Returns false on failure, usually because the looper processing the message queue is
     * exiting.
     */
    public static boolean submitCallback(final Runnable task) {
        return MAIN_LOOP.post(task);
    }
}
