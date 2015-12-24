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

package com.anpmech.mpd.connection;

import com.anpmech.mpd.Tools;
import com.anpmech.mpd.commandresponse.CommandResponse;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.StringTokenizer;

/**
 * This is the core of the {@link CommandResponse} classes.
 *
 * <p>This class is subclassed to process any MPD protocol server responses. This class is
 * immutable, thus, thread-safe.</p>
 */
public class AbstractCommandResult {

    /**
     * The class log identifier.
     */
    private static final String TAG = "AbstractCommandResult";

    /**
     * The result of the connection initiation.
     */
    @Nullable
    protected final String mConnectionResult;

    /**
     * The MPD protocol command response.
     */
    protected final String mResult;

    /**
     * This constructor is used to create a new core result from the MPD protocol.
     *
     * @param connectionResult The result of the connection initiation.
     * @param result           The MPD protocol command result.
     */
    protected AbstractCommandResult(@Nullable final String connectionResult, final String result) {
        super();

        mConnectionResult = connectionResult;
        mResult = result;
    }

    /**
     * This constructor is used to create a empty CommandResult.
     */
    protected AbstractCommandResult() {
        this(null, "");
    }

    /**
     * This method checks this result for a specific value.
     *
     * @param value The value to find in the response.
     * @return True if the value is found, false otherwise.
     */
    public boolean contains(@NotNull final CharSequence value) {
        return contains(null, value);
    }

    /**
     * This method checks this result for a specific value.
     *
     * @param key   The key to pair with the value. If null, only the value will be searched for.
     * @param value The value to find in the response.
     * @return True if the value, and key, if applicable, is found, false otherwise.
     */
    public boolean contains(@Nullable final CharSequence key, @NotNull final CharSequence value) {
        final StringBuilder stringBuilder;

        if (key == null) {
            stringBuilder = new StringBuilder(value.length() + 10);
        } else {
            stringBuilder = new StringBuilder(key.length() + value.length() + 10);
            stringBuilder.append(key);
        }
        stringBuilder.append(": ");
        stringBuilder.append(value);
        stringBuilder.append('\n');

        return mResult.contains(stringBuilder);
    }

    /**
     * Compares this instance with the specified object and indicates if they are equal. In order
     * to
     * be equal, {@code o} must represent the same object as this instance using a class-specific
     * comparison. The general contract is that this comparison should be reflexive, symmetric, and
     * transitive. Also, no object reference other than null is equal to null.
     *
     * @param o the object to compare this instance with.
     * @return {@code true} if the specified object is equal to this {@code Object}; {@code false}
     * otherwise.
     * @see #hashCode
     */
    @Override
    public boolean equals(final Object o) {
        Boolean isEqual = null;

        if (this == o) {
            isEqual = Boolean.TRUE;
        } else if (o == null || getClass() != o.getClass()) {
            isEqual = Boolean.FALSE;
        }

        if (isEqual == null || isEqual.equals(Boolean.TRUE)) {
            /** This has to be the same due to the class check above. */
            //noinspection unchecked
            final AbstractCommandResult result = (AbstractCommandResult) o;

            //noinspection ConstantConditions
            if (Tools.isNotEqual(mResult, result.mResult) ||
                    Tools.isNotEqual(mConnectionResult, result.mConnectionResult)) {
                isEqual = Boolean.FALSE;
            }
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual.booleanValue();
    }

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

    /**
     * Processes the {@code CommandResponse} connection response to store the current media server
     * MPD protocol version.
     *
     * @return Returns the MPD version retained from the connection result.
     */
    public int[] getMPDVersion() {
        if (mConnectionResult == null) {
            throw new IllegalStateException("Cannot retrieve version when invalid.");
        }

        final int subHeaderLength = (MPDConnection.CMD_RESPONSE_OK + " MPD ").length();
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

    /**
     * This returns the result from this command.
     *
     * @return The response from the sent command.
     */
    public String getResult() {
        return mResult;
    }

    /**
     * Returns an integer hash code for this object. By contract, any two objects for which {@link
     * #equals} returns {@code true} must return the same hash code value. This means that
     * subclasses of {@code Object} usually override both methods or neither method.
     *
     * <p>Note that hash values must not change over time unless information used in equals
     * comparisons also changes.</p>
     *
     * @return this object's hash code.
     * @see #equals
     */
    @Override
    public int hashCode() {
        int result;

        if (mConnectionResult != null) {
            result = mConnectionResult.hashCode();
        } else {
            result = 0;
        }

        result = 31 * result + mResult.hashCode();

        return result;
    }

    /**
     * Returns true if the result returned nothing other than an OK response.
     *
     * @return True if the result returned OK and nothing more, false otherwise.
     */
    public boolean isEmpty() {
        return mResult.isEmpty();
    }

    /**
     * This checks the connection response for validity.
     *
     * @return True if the connection header exists, false otherwise.
     */
    public boolean isHeaderValid() {
        return mConnectionResult != null;
    }

    @Override
    public String toString() {
        return "CommandResult{" +
                "mConnectionResult='" + mConnectionResult + '\'' +
                ", mResult='" + mResult + '\'' +
                '}';
    }
}
