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

package com.anpmech.mpd.commandresponse;

import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.item.Artist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * This class contains methods used to process {@link Artist} entries from a {@link CommandResult}.
 */
public class KeyValueResponse extends ObjectResponse<Map.Entry<String, String>> {

    /**
     * The MPD protocol {@code key}:{@code value} delimiter.
     */
    private static final char MPD_KV_DELIMITER = ':';

    /**
     * The class log identifier.
     */
    private static final String TAG = "KeyValueResponse";

    /**
     * Sole public constructor.
     *
     * @param response The CommandResponse containing a Artist type MPD response.
     */
    public KeyValueResponse(final CommandResult response) {
        super(response);
    }

    /**
     * This constructor builds this class from an empty MPD protocol result.
     */
    public KeyValueResponse() {
        super();
    }

    /**
     * Converts a Iterator&lt;Map.Entry&lt;T, S&gt;&gt; to a List&lt;String&gt; of values where
     * the {@code key} parameter matches the Map Entry.
     *
     * @param list     The list to add the matching entries to.
     * @param iterator The iterator with Map.Entry&lt;T, S&gt; entries.
     * @param key      The key to match. If {@code null}, all will match.
     * @param <T>      The first Map.Entry type.
     * @param <S>      The second Map.Entry type.
     * @return True if the list was modified, false otherwise.
     */
    private static <T, S> boolean addAllMatching(final Collection<S> list,
            final Iterator<Map.Entry<T, S>> iterator, final T key) {
        final int hash = list.hashCode();

        while (iterator.hasNext()) {
            final Map.Entry<T, S> entry = iterator.next();

            if (key == null || entry.getKey().equals(key)) {
                list.add(entry.getValue());
            }
        }

        return hash != list.hashCode();
    }

    /**
     * Converts a Iterator&lt;Map.Entry&lt;T, S&gt;&gt; to a Map&lt;Map.Entry&lt;T, S&gt;&gt;.
     *
     * @param map      The map to add the entries from the iterator to.
     * @param iterator The iterator with Map.Entry&lt;T, S&gt; entries.
     * @param <T>      The first Map.Entry type.
     * @param <S>      The second Map.Entry type.
     * @return True if the list was modified, false otherwise.
     */
    private static <T, S> boolean putAll(final Map<T, S> map,
            final Iterator<Map.Entry<T, S>> iterator) {
        final int hash = map.hashCode();

        while (iterator.hasNext()) {
            final Map.Entry<T, S> entry = iterator.next();

            map.put(entry.getKey(), entry.getValue());
        }

        return hash != map.hashCode();
    }

    /**
     * This method creates a key value map from this response.
     *
     * @return A {@code Map<Key, Value>}.
     */
    public Map<String, String> getKeyValueMap() {
        final Map<String, String> map = new HashMap<>(mListSize);
        putAll(map, listIterator());

        return map;
    }

    /**
     * This method returns a list of values with keys equal to the {@code key} parameter from a
     * {@code key}:{@code value} MPD protocol response.
     *
     * @param key The key to find matching values for.
     * @return A list of values with keys matching the {@code key} parameter.
     */
    public List<String> getValues(final String key) {
        final List<String> values = new ArrayList<>(mListSize);
        addAllMatching(values, listIterator(), key);

        return values;
    }

    /**
     * This method returns a list of values from a {@code key}:{@code value} pair MPD protocol
     * response.
     *
     * <p>Care should be taken to only call this method with a {@code key}:{@code value} pair MPD
     * protocol response.</p>
     *
     * @return A list of values from a {@code key}:{@code value} pair MPD protocol response.
     */
    public List<String> getValues() {
        return getValues(null);
    }

    /**
     * This method returns a iterator, starting at the beginning of the response.
     *
     * @param position The position to begin the iterator at, typically beginning or end.
     * @return A iterator to return the response.
     * @see #getList()
     */
    @Override
    protected ListIterator<Map.Entry<String, String>> listIterator(final int position) {
        return new KeyValueIterator(mResult, position);
    }

    /**
     * Returns a count of how many objects this {@code Collection} contains.
     *
     * @return how many objects this {@code Collection} contains, or {@link Integer#MAX_VALUE}
     * if there are more than {@link Integer#MAX_VALUE} elements in this
     * {@code Collection}.
     */
    @Override
    public int size() {
        return CommandResponse.FullBlockResultIterator.size(mResult);
    }

    /**
     * This class is used to create an {@link Iterator} to iterate over each {@code key}:{@code
     * value} of an MPD
     * command response.
     *
     * <b>This class requires a {@code key}:{@code value} MPD response.</b>
     */
    private static final class KeyValueIterator
            extends CommandResponse.FullBlockResultIterator<Map.Entry<String, String>> {

        /**
         * Sole constructor.
         *
         * @param response The MPD protocol command response.
         * @param position The position relative to the response to initiate the
         *                 {@link AbstractResultIterator#mPosition} to.
         */
        private KeyValueIterator(final String response, final int position) {
            super(response, position);
        }

        /**
         * Override this to create the Object using the response block.
         *
         * @param responseBlock The response block to create the Object from.
         * @return The object created from the response block.
         */
        @Override
        Map.Entry<String, String> instantiate(final String responseBlock) {
            return new SimplerImmutableEntry(responseBlock);
        }
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
