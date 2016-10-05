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

package com.anpmech.mpd;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides a logger abstraction for the Java backend.
 */
public final class Log {

    private Log() {
        super();
    }

    /**
     * Sends a debug message to the user.
     *
     * <p>This maps to a {@link Level#FINE} logger message.</p>
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to send to the user.
     */
    public static void debug(final String tag, final String message) {
        final Logger logger = Logger.getLogger(tag);

        logger.log(Level.FINE, message);
    }

    /**
     * Sends a debug message to the user and log the exception.
     *
     * <p>This maps to a {@link Level#FINE} logger message.</p>
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to send to the user.
     * @param tr      An exception to log.
     */
    public static void debug(final String tag, final String message, final Throwable tr) {
        final Logger logger = Logger.getLogger(tag);

        logger.log(Level.FINE, message, tr);
    }

    /**
     * Sends an error message to the user.
     *
     * <p>This maps to a {@link Level#SEVERE} logger message.</p>
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to send to the user.
     */
    public static void error(final String tag, final String message) {
        final Logger logger = Logger.getLogger(tag);

        logger.log(Level.SEVERE, message);
    }

    /**
     * Sends an error message to the user and log the exception.
     *
     * <p>This maps to a {@link Level#SEVERE} logger message.</p>
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to send to the user.
     * @param tr      An exception to log.
     */
    public static void error(final String tag, final String message, final Throwable tr) {
        final Logger logger = Logger.getLogger(tag);

        logger.log(Level.SEVERE, message, tr);
    }

    /**
     * Sends a info message to the user.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    public static void info(final String tag, final String message) {
        final Logger logger = Logger.getLogger(tag);

        logger.log(Level.INFO, message);
    }

    /**
     * Sends a info message to the user and log the exception.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param tr      An exception to log
     */
    public static void info(final String tag, final String message, final Throwable tr) {
        final Logger logger = Logger.getLogger(tag);

        logger.log(Level.INFO, message, tr);
    }

    /**
     * Sends a verbose message to the user.
     *
     * <p>This maps to a {@link Level#FINER} logger message.</p>
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    public static void verbose(final String tag, final String message) {
        final Logger logger = Logger.getLogger(tag);

        logger.log(Level.FINER, message);
    }

    /**
     * Send a verbose log message and log the exception.
     *
     * <p>This maps to a {@link Level#FINER} logger message.</p>
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param tr      An exception to log
     */
    public static void verbose(final String tag, final String message, final Throwable tr) {
        final Logger logger = Logger.getLogger(tag);

        logger.log(Level.FINER, message, tr);
    }

    /**
     * Sends a warning message to the user.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to send to the user.
     */
    public static void warning(final String tag, final String message) {
        final Logger logger = Logger.getLogger(tag);

        logger.log(Level.WARNING, message);
    }

    /**
     * Sends a warning message to the user and log the exception.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to send to the user.
     * @param tr      An exception to log.
     */
    public static void warning(final String tag, final String message, final Throwable tr) {
        final Logger logger = Logger.getLogger(tag);

        logger.log(Level.WARNING, message, tr);
    }
}
