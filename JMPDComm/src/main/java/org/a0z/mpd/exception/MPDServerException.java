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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an MPD Server error.
 *
 * @author Felipe Gustavo de Almeida
 */
public class MPDServerException extends MPDException {

    private static final Pattern ACK_CODE_PATTERN = Pattern.compile("([0-9]+)");

    private static final String MPD_RESPONSE_ERR = "ACK";

    private static final String TAG = "MPDServerException";

    private static final long serialVersionUID = 5986199004785561712L;

    /**
     * Constructor.
     */
    public MPDServerException() {
        super();
    }

    /**
     * Constructor.
     *
     * @param message exception message.
     */
    public MPDServerException(final String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message exception message.
     * @param cause   cause of this exception.
     */
    public MPDServerException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor.
     *
     * @param cause cause of this exception.
     */
    public MPDServerException(final Throwable cause) {
        super(cause);
    }

    /**
     * Parse the error kind from the ACK like and return it
     * Matches the return code using MPD's ack.h
     * Note : Only used errors are supported for the time being.
     * Other kinds will be recognized as "UNKNOWN"
     *
     * @return The error kind
     */
    public ErrorKind getErrorKind() {
        final int errorNumber;
        ErrorKind errorKind = ErrorKind.UNKNOWN;
        if (getMessage() != null && getMessage().startsWith(MPD_RESPONSE_ERR)) {
            try {
                final Matcher matcher = ACK_CODE_PATTERN.matcher(getMessage());
                if (matcher.find()) {
                    errorNumber = Integer.parseInt(matcher.group(1));
                    switch (errorNumber) {
                        case 3:
                            errorKind = ErrorKind.PASSWORD;
                    }
                }
            } catch (final Exception e) {
                Log.error(TAG, "Error while extracting MPDServerException error code. ACK line : "
                        + getMessage(), e);
            }
        }
        return errorKind;
    }

    public enum ErrorKind {
        PASSWORD,
        UNKNOWN
    }
}
