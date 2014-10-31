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

/** This class stores the result for MPDCallable. */
class CommandResult {

    private String mConnectionResult;

    private IOException mIOException;

    private MPDException mLastException;

    private List<String> mResult;

    final String getConnectionResult() {
        return mConnectionResult;
    }

    final IOException getIOException() {
        return mIOException;
    }

    final MPDException getLastException() {
        return mLastException;
    }

    final List<String> getResult() {
        /** No need, we already made the collection immutable on the way in. */
        //noinspection ReturnOfCollectionOrArrayField
        return mResult;
    }

    final boolean isMPDException() {
        final boolean isMPDException;

        if (mLastException == null) {
            isMPDException = false;
        } else {
            isMPDException = true;
        }

        return isMPDException;
    }

    final void setConnectionResult(final String result) {
        mConnectionResult = result;
    }

    final void setException(final IOException ioException) {
        mLastException = null;
        mIOException = ioException;
    }

    final void setException(final MPDException lastException) {
        mIOException = null;
        mLastException = lastException;
    }

    final void setResult(final List<String> result) {
        if (result == null) {
            mResult = null;
        } else {
            mResult = Collections.unmodifiableList(result);
        }
    }
}
