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

import com.anpmech.mpd.ResponseObject;
import com.anpmech.mpd.commandresponse.iterator.KeyValueIterator;
import com.anpmech.mpd.connection.CommandResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * This class contains methods used to process {@link Map.Entry} entries from a
 * {@code key}:{@code value} MPD response.
 *
 * <p>This class is immutable, thus, thread-safe.</p>
 */
public class KeyValueResponse extends ObjectResponse<Map.Entry<String, String>> {

    /**
     * The class log identifier.
     */
    private static final String TAG = "KeyValueResponse";

    /**
     * This constructor is used to create {@link Map.Entry} objects from a CommandResult.
     *
     * @param result The CommandResult containing a Map.Entry type MPD result.
     */
    public KeyValueResponse(final CommandResult result) {
        super(result);
    }

    /**
     * This constructor builds this class from an empty MPD protocol result.
     */
    public KeyValueResponse() {
        super();
    }

    /**
     * This constructor is used to create {@link Map.Entry} objects from another compatible
     * {@link ObjectResponse}.
     *
     * @param response The ObjectResponse containing a {@code key}:{@code value} type MPD response.
     */
    public KeyValueResponse(final ObjectResponse<?> response) {
        super(response);
    }

    /**
     * This constructor is used to iterate over responses in a {@link ResponseObject}.
     *
     * @param response The ResponseObject to iterate over.
     */
    public KeyValueResponse(final ResponseObject response) {
        super(response);
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
        final Map<String, String> map = new HashMap<>();
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
        final List<String> values = new ArrayList<>();
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
     */
    @Override
    protected ListIterator<Map.Entry<String, String>> listIterator(final int position) {
        return new KeyValueIterator(mResult, position);
    }

    /**
     * Returns a count of how many objects this {@code Collection} contains.
     *
     * @return how many objects this {@code Collection} contains, or {@link Integer#MAX_VALUE}
     * if there are more than {@link Integer#MAX_VALUE} elements in this {@code Collection}.
     */
    @Override
    public int size() {
        return KeyValueIterator.size(mResult);
    }
}
