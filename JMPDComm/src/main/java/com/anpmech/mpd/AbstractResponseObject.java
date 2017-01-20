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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class represents an abstraction for a response or a name for one object.
 */
public class AbstractResponseObject {

    /**
     * The class log identifier.
     */
    private static final String TAG = "AbstractResponseObject";

    /**
     * The client generated entry name.
     */
    protected final String mName;

    /**
     * The server generated entry response.
     */
    protected final String mResponse;

    /**
     * Sole constructor.
     *
     * @param name     The name for the object.
     * @param response The response for the object.
     */
    AbstractResponseObject(@Nullable final String name, @Nullable final String response) {
        super();

        if (name == null && response == null) {
            throw new IllegalArgumentException(
                    "Name and response cannot be null in the same constructor.");
        }

        if (name != null && response != null) {
            throw new IllegalArgumentException(
                    "Name or response must be null in this constructor.");
        }

        mName = name;
        mResponse = response;
    }

    /**
     * This method returns only the available key value if there is only one value in the response,
     * otherwise, the entire value is returned.
     *
     * @param response The response to find the value for.
     * @return A hashCode of a response if there is a single value and only a hashCode
     * of that value, otherwise {@link Integer#MIN_VALUE}.
     */
    private static String valueResponse(@NotNull final String response) {
        final String valueResponse;
        final int length = response.length();

        /**
         * If the response exists, and includes a singular response line, get the value,
         * otherwise, return the entire response.
         */
        if ((int) response.charAt(length - 1) == (int) MPDCommand.MPD_CMD_NEWLINE) {
            valueResponse = response.substring(response.indexOf(':') + 2, length - 1);
        } else {
            valueResponse = response;
        }

        return valueResponse;
    }

    /**
     * Compares this instance with the specified object and indicates if they
     * are equal. In order to be equal, {@code o} must represent the same object
     * as this instance using a class-specific comparison. The general contract
     * is that this comparison should be reflexive, symmetric, and transitive.
     * Also, no object reference other than null is equal to null.
     *
     * <p>The default implementation returns {@code true} only if {@code this ==
     * o}. See <a href="{@docRoot}reference/java/lang/Object.html#writing_equals">Writing a correct
     * {@code equals} method</a>
     * if you intend implementing your own {@code equals} method.
     *
     * <p>The general contract for the {@code equals} and {@link
     * #hashCode()} methods is that if {@code equals} returns {@code true} for
     * any two objects, then {@code hashCode()} must return the same value for
     * these objects. This means that subclasses of {@code Object} usually
     * override either both methods or neither of them.
     *
     * @param o the object to compare this instance with.
     * @return {@code true} if the specified object is equal to this {@code
     * Object}; {@code false} otherwise.
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
            final AbstractResponseObject entry = (AbstractResponseObject) o;

            final String thisString;
            final String thatString;
            if (mResponse == null) {
                thisString = mName;
            } else {
                thisString = valueResponse(mResponse);
            }

            if (entry.mResponse == null) {
                thatString = entry.mName;
            } else {
                thatString = valueResponse(entry.mResponse);
            }

            /**
             * Neither can be null at this point, one or the other is not null,
             * checked at construction.
             */
            //noinspection ConstantConditions
            if (!thisString.equals(thatString)) {
                isEqual = Boolean.FALSE;
            }
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual;
    }

    /**
     * Returns a key's value from the {@link #mResponse}, if the response is not null. Otherwise
     * the {@link #mName} field is returned, regardless of the {@code key} parameter value.
     *
     * @param key The key to retrieve the value for.
     * @return The value paired to the key, null if not found.
     */
    @Nullable
    public String findValue(@NotNull final String... key) {
        String value = null;

        if (mResponse == null) {
            value = mName;
        } else {
            final int valueIndex = Tools.getNextValueIndex(mResponse, 0, key);

            if (valueIndex != -1) {
                value = getResponseValue(valueIndex);
            }
        }

        return value;
    }

    /**
     * This method returns either the entire name or the entire response, whichever exists.
     *
     * @return The entire name or the entire response, whichever exists.
     */
    public String getResponse() {
        final String response;

        if (mName == null) {
            response = mResponse;
        } else {
            response = mName;
        }

        return response;
    }

    /**
     * This method returns this response key value.
     *
     * @param valueIndex The index of the value.
     * @return The value from the response, at the valueIndex position.
     */
    @NotNull
    private String getResponseValue(final int valueIndex) {
        if (mResponse == null) {
            throw new IllegalStateException("Cannot call this method with a null response.");
        }

        final String value;

        if (mResponse.length() < valueIndex) {
            /** MPD, by protocol, does not require anything after semi-colon. */
            value = "";
        } else {
            final int valueEnd = mResponse.indexOf(MPDCommand.MPD_CMD_NEWLINE, valueIndex);

            if (valueEnd == -1) {
                value = mResponse.substring(valueIndex);
            } else {
                value = mResponse.substring(valueIndex, valueEnd);
            }
        }

        return value;
    }

    /**
     * Returns an integer hash code for this object. By contract, any two
     * objects for which {@link #equals} returns {@code true} must return
     * the same hash code value. This means that subclasses of {@code Object}
     * usually override both methods or neither method.
     *
     * <p>Note that hash values must not change over time unless information used in equals
     * comparisons also changes.</p>
     *
     * @return this object's hash code.
     * @see #equals
     */
    @Override
    public int hashCode() {
        final int hashCode;

        if (mName == null) { /** mResponse is not null. */
            //noinspection ConstantConditions
            hashCode = valueResponse(mResponse).hashCode() + getClass().hashCode();
        } else {
            hashCode = mName.hashCode() + getClass().hashCode();
        }

        return hashCode;
    }

    @Override
    public String toString() {
        return "AbstractResponseObject{" +
                "mName='" + mName + '\'' +
                ", mResponse='" + mResponse + '\'' +
                '}';
    }
}
