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

package org.a0z.mpd.exception;

import org.a0z.mpd.Log;

/**
 * Represents an MPD Server error with a stack trace, a detail message and elements from the
 * <A HREF="http://www.musicpd.org/doc/protocol/response_syntax.html">response syntax</A>.
 *
 * @author Felipe Gustavo de Almeida
 */
public class MPDException extends Exception {

    /**
     * The MPD protocol ACK error code given when there is an error with a given argument.
     */
    public static final int ACK_ERROR_ARG = 2;

    /**
     * The MPD protocol ACK error code given when the argument target to a command was not expected
     * to exist, but does.
     */
    public static final int ACK_ERROR_EXIST = 56;

    /**
     * The MPD protocol ACK error code given when sending command queue commands out of order.
     * This is no longer used in the standard implementation.
     */
    public static final int ACK_ERROR_NOT_LIST = 1;

    /**
     * The MPD protocol ACK error code given when the argument target to a command was expected to\
     * exist, but does not.
     */
    public static final int ACK_ERROR_NO_EXIST = 50;

    /**
     * The MPD protocol ACK error code given when the password is incorrect.
     */
    public static final int ACK_ERROR_PASSWORD = 3;

    /**
     * The MPD protocol ACK error code given when permission is denied for a command.
     */
    public static final int ACK_ERROR_PERMISSION = 4;

    /**
     * The MPD protocol ACK error code given when player sync has failed.
     */
    public static final int ACK_ERROR_PLAYER_SYNC = 55;

    /**
     * The MPD protocol ACK error code given when loading a playlist has failed.
     */
    public static final int ACK_ERROR_PLAYLIST_LOAD = 53;

    /**
     * The MPD protocol ACK error code given when a playlist queue has reached it's maximum size.
     */
    public static final int ACK_ERROR_PLAYLIST_MAX = 51;

    /**
     * The MPD protocol ACK error code given when a system error has occurred.
     */
    public static final int ACK_ERROR_SYSTEM = 52;

    /**
     * The MPD protocol ACK error code given when an unknown error has occurred with the command.
     */
    public static final int ACK_ERROR_UNKNOWN = 5;

    /**
     * The MPD protocol ACK error code given when attempting a media server database update when an
     * update is already taking place.
     */
    public static final int ACK_ERROR_UPDATE_ALREADY = 54;

    /**
     * This occurs when there is a connection error or a non-standard error response from the media
     * server.
     */
    private static final int ERROR_UNKNOWN = -1;

    private static final String TAG = "MPDException";

    private static final long serialVersionUID = -5837769913914420046L;

    /** The command sent for this error. */
    public final String mCommand;

    /** The position of the command queue the error occurred. 0 if not a command queue. */
    public final int mCommandQueuePosition;

    /** The error code which caused this error in media server response. */
    public final int mErrorCode;

    /** The message text which caused this error in media server response. */
    public final String mErrorMessage;

    /**
     * Constructs a new {@code Exception} that includes the current stack trace.
     */
    public MPDException() {
        mCommandQueuePosition = 0;
        mErrorCode = ERROR_UNKNOWN;
        mErrorMessage = "Exception thrown, no exception detail message given.";
        mCommand = null;
    }

    /**
     * Constructs a new {@code Exception} with the current stack trace, the specified detail
     * message and parses the media server error string for details given by fields in this
     * exception.
     *
     * @param detailMessage The detail message for this exception.
     */
    public MPDException(final String detailMessage) {
        super(detailMessage);

        mErrorCode = getAckErrorCode(detailMessage);
        mCommandQueuePosition = getAckCommandQueuePosition(detailMessage);
        mCommand = parseString(detailMessage, '{', '}');
        mErrorMessage = parseString(detailMessage, '}', '\0').trim();
    }

    /**
     * Constructs a new {@code Exception} with the current stack trace, the specified detail
     * message, the specified cause and parses the media server error string for details given by
     * fields in this exception.
     *
     * @param detailMessage The detail message for this exception.
     * @param throwable     The cause of this exception.
     */
    public MPDException(final String detailMessage, final Throwable throwable) {
        super(detailMessage, throwable);

        mErrorCode = getAckErrorCode(detailMessage);
        mCommandQueuePosition = getAckCommandQueuePosition(detailMessage);
        mCommand = parseString(detailMessage, '{', '}');
        mErrorMessage = parseString(detailMessage, '}', '\0').trim();
    }

    /**
     * Constructs a new {@code Exception} with the current stack trace, the specified cause and
     * parses the media server error string for details given by fields in this exception.
     *
     * @param throwable The cause of this exception.
     */
    public MPDException(final Throwable throwable) {
        super(throwable);

        final String detailMessage = throwable.getMessage();

        mErrorCode = getAckErrorCode(detailMessage);
        mCommandQueuePosition = getAckCommandQueuePosition(detailMessage);
        mCommand = parseString(detailMessage, '{', '}');
        mErrorMessage = parseString(detailMessage, '}', '\0').trim();
    }

    /**
     * This method parses the MPD protocol formatted ACK detail message for the command queue
     * position where the error occurred.
     *
     * @param message The incoming media server ACK message.
     * @return The command queue position where the error occurred, -1 if the message is not
     * a valid media server ACK message.
     */
    private static int getAckCommandQueuePosition(final String message) {
        final String parsed = parseString(message, '@', ']');
        int queuePosition = -1;

        if (parsed != null) {
            try {
                queuePosition = Integer.parseInt(parsed);
            } catch (final NumberFormatException e) {
                Log.error(TAG, "Failed to parse ACK command queue position.", e);
            }
        }

        return queuePosition;
    }

    /**
     * This method parses the MPD protocol formatted ACK detail message for the MPD error code.
     *
     * @param message The incoming media server ACK message.
     * @return The MPD protocol error code, -1 if the message is not a valid media server ACK
     * message.
     */
    public static int getAckErrorCode(final String message) {
        final String parsed = parseString(message, '[', '@');
        int errorCode = ACK_ERROR_UNKNOWN;

        if (parsed != null) {
            try {
                errorCode = Integer.parseInt(parsed);
            } catch (final NumberFormatException e) {
                Log.error(TAG, "Failed to parse ACK error code.", e);
            }
        }

        return errorCode;
    }

    /**
     * This parses {@code message} in between the first found {@code start} parameter and {@code
     * end} parameter. In the index of {@code start} and {@code end} are equal, the index search
     * for the {@code end} parameter will begin after the {@code start} parameter index. If the
     * {@code start} parameter is null, the string will match the first character of the
     * {@code message}, if the {@code end} parameter is null, the string will match to the length
     * of
     * the String. This method is not intended to be an all-purpose string parser, and shouldn't be
     * used for anything beyond simple line parsing.
     *
     * @param message Message to parse.
     * @param start   The first character to begin parsing.
     * @param end     The final character to parse.
     * @return The substring of characters between the {@code start} and {@code end}, null if
     * parsing failed.
     */
    private static String parseString(final String message, final char start, final char end) {
        String result = null;

        if (message != null) {
            final int startIndex;
            final int endIndex;

            if (start == '\0') {
                startIndex = 0;
            } else {
                startIndex = message.indexOf(start) + 1;
            }

            if (end == '\0') {
                endIndex = message.length();
            } else {
                endIndex = message.indexOf(end, startIndex);
            }

            if (startIndex != -1 && endIndex != -1) {
                result = message.substring(startIndex, endIndex);
            }
        }

        return result;
    }

    /**
     * Checks this exception to see if it was a correctly parsed {@code ACK} message.
     *
     * @return True if this exception was caused by an {@code ACK} message, false otherwise.
     */
    public boolean isACKError() {
        return mErrorCode == ERROR_UNKNOWN;
    }
}
