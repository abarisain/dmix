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

package com.anpmech.mpd.commandresponse.iterator;

import java.util.Iterator;
import java.util.Map;

/**
 * This class is used to create an {@link Iterator} to iterate over each {@code key}:{@code
 * value} of an MPD command response.
 *
 * <b>This class requires a {@code key}:{@code value} MPD response.</b>
 */
public class KeyValueIterator extends FullBlockResult<Map.Entry<String, String>> {

    /**
     * Sole constructor.
     *
     * @param response The MPD protocol command response.
     * @param position The position relative to the response to initiate the
     *                 {@link FullBlockResult#mPosition} to.
     */
    public KeyValueIterator(final String response, final int position) {
        super(response, position);
    }

    /**
     * Returns a count of how many objects this {@code Collection} contains.
     *
     * @param result The MPD result to get the size for.
     * @return how many objects this {@code Collection} contains, or {@link Integer#MAX_VALUE}
     * if there are more than {@link Integer#MAX_VALUE} elements in this
     * {@code Collection}.
     */
    public static int size(final String result) {
        return FullBlockResult.count(result);
    }

    /**
     * Override this to create the Object using the response block.
     *
     * @param responseBlock The response block to create the Object from.
     * @return The object created from the response block.
     */
    @Override
    protected Map.Entry<String, String> instantiate(final String responseBlock) {
        return new SimplerImmutableEntry(responseBlock);
    }

    /**
     * This creates a simple map entry for {@code key}:{@code value} entries.
     */
    private static final class SimplerImmutableEntry implements Map.Entry<String, String> {

        /**
         * The index of the delimiter in the {@link #mEntry}.
         */
        private final int mDelimiter;

        /**
         * The {@code key}:{@code value} response line.
         */
        private final String mEntry;

        /**
         * Sole constructor.
         *
         * @param entry The line to make a Map.Entry&lt;CharSequence, String&gt;.
         */
        private SimplerImmutableEntry(final String entry) {
            super();

            mEntry = entry;
            mDelimiter = entry.indexOf(MPD_KV_DELIMITER);

            if (mDelimiter == -1) {
                throw new IllegalArgumentException(
                        "Failed to parse line for delimiter.\n" + "Failed iterator information: "
                                + toString());
            }
        }

        /**
         * Compares this instance with the specified object and indicates if they
         * are equal. In order to be equal, {@code o} must represent the same object
         * as this instance using a class-specific comparison. The general contract
         * is that this comparison should be reflexive, symmetric, and transitive.
         * Also, no object reference other than null is equal to null.
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
                final SimplerImmutableEntry entry = (SimplerImmutableEntry) o;

                /**
                 * A null value would have been an error during construction.
                 */
                //noinspection ConstantConditions
                if (!mEntry.equals(entry.mEntry)) {
                    isEqual = Boolean.FALSE;
                }
            }

            if (isEqual == null) {
                isEqual = Boolean.TRUE;
            }

            return isEqual.booleanValue();
        }

        /**
         * The MPD response key for this entry.
         *
         * @return The key for this entry.
         */
        @Override
        public String getKey() {
            return mEntry.substring(0, mDelimiter);
        }

        /**
         * The MPD response value for this entry.
         *
         * @return The value for this entry.
         */
        @Override
        public String getValue() {
            return mEntry.substring(mDelimiter + 2);
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
            return mEntry.hashCode() + super.hashCode();
        }

        /**
         * Calling this method will throw a {@link UnsupportedOperationException}.
         */
        @Override
        public String setValue(final String object) {
            throw new UnsupportedOperationException("Setting a value not supported.");
        }

        /**
         * This returns the entire entry.
         *
         * @return The entire (unsplit) entry.
         */
        @Override
        public String toString() {
            return mEntry;
        }
    }
}
