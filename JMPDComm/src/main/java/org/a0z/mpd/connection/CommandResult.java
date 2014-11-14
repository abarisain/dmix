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

package org.a0z.mpd.connection;

import org.a0z.mpd.exception.MPDException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/** This class stores the result for MPDCallable. */
class CommandResult {

    private Boolean isIOExceptionLast = null;

    private String mConnectionResult;

    private IOException mIOException;

    private MPDException mMPDException;

    private List<String> mResult;

    /**
     * Returns the first string response from the media server after connection. This method is
     * mainly for debugging.
     *
     * @return A string representation of the connection result.
     * @see #getMPDVersion() Use of this method is preferred.
     */
    public String getConnectionResult() {
        return mConnectionResult;
    }

    final IOException getIOException() {
        return mIOException;
    }

    final MPDException getMPDException() {
        return mMPDException;
    }

    /**
     * Processes the {@code CommandResult} connection response to store the current media server
     * MPD protocol version.
     *
     * @return Returns the MPD version retained from the connection result.
     */
    public int[] getMPDVersion() {
        final int subHeaderLength = (MPDConnection.MPD_RESPONSE_OK + " MPD ").length();
        final String formatResponse = mConnectionResult.substring(subHeaderLength);

        final StringTokenizer stringTokenizer = new StringTokenizer(formatResponse, ".");
        final int[] version = new int[stringTokenizer.countTokens()];
        int i = 0;

        while (stringTokenizer.hasMoreElements()) {
            version[i] = Integer.parseInt(stringTokenizer.nextToken());
            i++;
        }

        return version;
    }

    final List<String> getResult() {
        /** No need, we already made the collection immutable on the way in. */
        //noinspection ReturnOfCollectionOrArrayField
        return mResult;
    }

    public boolean isHeaderValid() {
        final boolean isHeaderValid;

        if (mConnectionResult == null) {
            isHeaderValid = false;
        } else {
            isHeaderValid = true;
        }

        return isHeaderValid;
    }

    public Boolean isIOExceptionLast() {
        return isIOExceptionLast;
    }

    final void setConnectionResult(final String result) {
        mConnectionResult = result;
    }

    final void setException(final IOException exception) {
        isIOExceptionLast = Boolean.TRUE;
        mIOException = exception;
    }

    final void setException(final MPDException exception) {
        isIOExceptionLast = Boolean.FALSE;
        mMPDException = exception;
    }

    final void setResult(final List<String> result) {
        if (result == null) {
            mResult = null;
        } else {
            mResult = Collections.unmodifiableList(result);
        }
    }
}
