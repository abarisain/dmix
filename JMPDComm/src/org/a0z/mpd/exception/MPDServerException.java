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

import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an MPD Server error.
 * 
 * @author Felipe Gustavo de Almeida
 * @version $Id: MPDServerException.java 2716 2004-11-20 17:37:20Z galmeida $
 */
public class MPDServerException extends MPDException {

    private static final long serialVersionUID = 5986199004785561712L;

    private static final String TAG = "MPDServerException";

    private String ackLine;

    private final static Pattern ACK_CODE_PATTERN = Pattern.compile("[0-9]+");

    public enum ErrorKind {
        PASSWORD,
        UNKOWN
    }

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
    public MPDServerException(String message) {
        super("Server error:" + message);
        this.ackLine = message;
    }

    /**
     * Constructor.
     * 
     * @param message exception message.
     * @param cause cause of this exception.
     */
    public MPDServerException(String message, Throwable cause) {
        super("Server error:" + message, cause);
        this.ackLine = message;
    }

    /**
     * Constructor.
     * 
     * @param cause cause of this exception.
     */
    public MPDServerException(Throwable cause) {
        super(cause);
    }

    /**
     * Get the MPD error that happened (after "ACK");
     *
     * @return MPD's Error message
     */
    public String getAckLine() {
        return ackLine;
    }

    /**
     * Parse the error kind from the ACK like and return it
     * Matches the return code using MPD's ack.h
     * Note : Only used errors are supported for the time being.
     * Other kinds will be recognized as "UNKNOWN"
     * @return The error kind
     */
    public ErrorKind getErrorKind() {
        int errorNumber = 5;
        ErrorKind errorKind = ErrorKind.UNKOWN;
        try {
            final Matcher matcher = ACK_CODE_PATTERN.matcher(getAckLine());
            errorNumber = Integer.parseInt(matcher.group(0));
            switch (errorNumber) {
                case 5:
                    errorKind = ErrorKind.PASSWORD;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while extracting MPDServerException error code. ACK line : "
                    + getAckLine(), e);
        }
        return errorKind;
    }
}
