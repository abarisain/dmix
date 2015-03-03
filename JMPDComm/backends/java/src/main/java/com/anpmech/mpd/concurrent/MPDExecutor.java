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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class provides one executor to use for this library.
 */
public final class MPDExecutor {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    /**
     * This is a utility class, no instantiation.
     */
    private MPDExecutor() {
        super();
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
     * <p/>
     * <p/>
     * If you would like to immediately block waiting for a task, you can use constructions of the
     * form <tt>result = exec.submit(aCallable).get();</tt>
     * <p/>
     * <p> Note: The {@link Executors} class includes a set of methods that can convert some other
     * common closure-like objects, for example, {@link java.security.PrivilegedAction} to
     * {@link Callable} form so they can be submitted.
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task
     * @throws java.util.concurrent.RejectedExecutionException if the task cannot be scheduled for
     *                                                         execution
     * @throws NullPointerException                            if the task is null
     */
    public static <T> MPDFuture<T> submit(final Callable<T> task) {
        return new MPDFuture<>(EXECUTOR.submit(task));
    }

    /**
     * Submits a Runnable task for execution and returns a Future representing that task. The
     * Future's <tt>get</tt> method will return <tt>null</tt> upon <em>successful</em> completion.
     *
     * @param task the task to submit
     * @throws java.util.concurrent.RejectedExecutionException if the task cannot be scheduled for
     *                                                         execution
     * @throws NullPointerException                            if the task is null
     */
    public static MPDFuture<?> submit(final Runnable task) {
        return new MPDFuture<>(EXECUTOR.submit(task));
    }

    /**
     * Submits a runnable with a callback as the contents.
     *
     * @param runnable A runnable with a callback as the content.
     */
    public static boolean submitCallback(final Runnable runnable) {
        submit(runnable);
        return true;
    }
}
