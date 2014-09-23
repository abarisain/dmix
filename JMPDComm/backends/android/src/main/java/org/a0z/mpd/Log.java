/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2014 The MPDroid Project
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

package org.a0z.mpd;

public final class Log {

    private Log() {
        super();
    }

    /**
     * Sends a debug message to the user.
     *
     * @param tag     Used to identify the source of a log message. It usually
     *                identifies the class or activity where the log call occurs.
     * @param message The message to send to the user.
     */
    public static void debug(final String tag, final String message) {
        android.util.Log.d(tag, message);
    }

    /**
     * Sends a debug message to the user.
     *
     * @param tag     Used to identify the source of a log message. It usually
     *                identifies the class or activity where the log call occurs.
     * @param message The message to send to the user.
     * @param tr      An exception to log.
     */
    public static void debug(final String tag, final String message, final Throwable tr) {
        android.util.Log.d(tag, message, tr);
    }

    /**
     * Sends an error message to the user.
     *
     * @param tag     Used to identify the source of a log message. It usually
     *                identifies the class or activity where the log call occurs.
     * @param message The message to send to the user.
     */
    public static void error(final String tag, final String message) {
        android.util.Log.e(tag, message);
    }

    /**
     * Sends an error message to the user.
     *
     * @param tag     Used to identify the source of a log message. It usually
     *                identifies the class or activity where the log call occurs.
     * @param message The message to send to the user.
     * @param tr      An exception to log.
     */
    public static void error(final String tag, final String message, final Throwable tr) {
        android.util.Log.e(tag, message, tr);
    }

    /**
     * Sends a warning message to the user.
     *
     * @param tag     Used to identify the source of a log message. It usually
     *                identifies the class or activity where the log call occurs.
     * @param message The message to send to the user.
     */
    public static void warning(final String tag, final String message) {
        android.util.Log.w(tag, message);
    }

    /**
     * Sends a warning message to the user.
     *
     * @param tag     Used to identify the source of a log message. It usually
     *                identifies the class or activity where the log call occurs.
     * @param message The message to send to the user.
     * @param tr      An exception to log.
     */
    public static void warning(final String tag, final String message, final Throwable tr) {
        android.util.Log.w(tag, message, tr);
    }
}
