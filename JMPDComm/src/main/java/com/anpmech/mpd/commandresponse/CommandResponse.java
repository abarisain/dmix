/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2015 The MPDroid Project
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

import com.anpmech.mpd.MPDCommand;
import com.anpmech.mpd.connection.CommandResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * This class stores and processes the result for a MPD command response.
 *
 * <p>This class is immutable, thus, thread-safe.</p>
 */
public class CommandResponse extends CommandResult implements Iterable<String> {

    /**
     * The class log identifier.
     */
    private static final String TAG = "CommandResponse";

    /**
     * This constructor builds this class from the MPD protocol result.
     *
     * @param connectionResult The result of the connection initiation.
     * @param response         The MPD protocol command response.
     * @param excludeResponses This is used to manually exclude responses from split
     *                         CommandResponse inclusion. Unused for this class.
     */
    protected CommandResponse(final String connectionResult, final String response,
            final int[] excludeResponses) {
        super(connectionResult, response, null);
    }

    /**
     * This constructor is used for subclassing.
     *
     * @param result The CommandResult to subclass.
     */
    public CommandResponse(final CommandResult result) {
        super(result);
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
        putAll(map, splitListIterator());

        return map;
    }

    /**
     * This returns a list of the results from this response.
     *
     * @return A list of results from this response.
     * @see #listIterator()
     */
    public List<String> getList() {
        final List<String> list = new ArrayList<>(mListSize);
        addAll(list, iterator());

        return list;
    }

    /**
     * This method returns a list of key:value pairs.
     *
     * <p>An error will be produced if this is called on a non-key/value MPD server response.</p>
     *
     * @return A list of key:value pairs.
     * @see #splitListIterator()
     */
    public List<Map.Entry<String, String>> getSplitList() {
        final List<Map.Entry<String, String>> list = new ArrayList<>(mListSize);
        addAll(list, splitListIterator());

        return list;
    }

    /**
     * This method returns a list of values from a key:value pair MPD protocol response.
     *
     * <p>Care should be taken to only call this method with a key:value pair MPD protocol
     * response.</p>
     *
     * @return A list of values from a key:value pair MPD protocol response.
     */
    public List<String> getValues() {
        return getValues(null);
    }

    /**
     * This method returns a list of values with keys equal to the {@code key} parameter from a
     * key:value MPD protocol response.
     *
     * @param key The key to find matching values for.
     * @return A list of values with keys matching the {@code key} parameter.
     */
    public List<String> getValues(final String key) {
        final List<String> values = new ArrayList<>(mListSize);
        addAllMatching(values, splitListIterator(), key);

        return values;
    }

    /**
     * Returns an {@link Iterator} for the elements in this object.
     *
     * @return An {@code Iterator} instance.
     */
    @Override
    public Iterator<String> iterator() {
        return listIterator();
    }

    /**
     * This method returns a iterator, starting at the beginning of the response.
     *
     * @return A iterator to return the response, line by line.
     * @see #getList()
     */
    public ListIterator<String> listIterator() {
        return new ResponseIterator(mResult, 0);
    }

    /**
     * This method returns a iterator, starting at the end of the response.
     *
     * <p>The expectation of this iterator is to use previous() and previousIndex().</p>
     *
     * @return A iterator to return response, line by line, starting at the end.
     */
    public ListIterator<String> reverseListIterator() {
        return new ResponseIterator(mResult,
                mResult.lastIndexOf(MPDCommand.MPD_CMD_NEWLINE) - 1);
    }

    /**
     * This method returns a iterator of key:value pairs, starting at the end of the list.
     *
     * <p>The expectation of this iterator is to use previous() and previousIndex().</p>
     *
     * @return A iterator to return key/value pairs.
     */
    public ListIterator<Map.Entry<String, String>> reverseSplitListIterator() {
        return new ResponseSplitIterator(mResult,
                mResult.lastIndexOf(MPDCommand.MPD_CMD_NEWLINE));
    }

    /**
     * This method returns a iterator of key:value pairs.
     *
     * @return A iterator to return key/value pairs.
     * @see #getSplitList()
     */
    public ListIterator<Map.Entry<String, String>> splitListIterator() {
        return new ResponseSplitIterator(mResult, 0);
    }

    /**
     * Returns the some results from the command response.
     *
     * @return Some results from the command response.
     */
    @Override
    public String toString() {
        return "CommandResponse{" +
                "mResult='" + mResult + '\'' +
                ", mConnectionResult='" + mConnectionResult + '\'' +
                ", mListSize=" + mListSize +
                ", mExcludeResponses=" + Arrays.toString(mExcludeResponses) +
                '}';
    }

    /**
     * This class instantiates an {@link Iterator} to iterate over the MPD command response.
     */
    private static class ResponseIterator extends AbstractResultIterator<String> {

        /**
         * Sole constructor.
         *
         * @param response The MPD protocol command response.
         * @param position The position relative to the response to initiate the
         *                 {@link CommandResult.AbstractResultIterator#mPosition} to.
         */
        ResponseIterator(final String response, final int position) {
            super(response, position);
        }

        /**
         * Returns the next object in the iteration.
         *
         * @return the next object.
         * @throws NoSuchElementException If there are no more elements.
         * @see #hasNext
         */
        @Override
        public String next() {
            checkNext();

            final String nextLine = getNextLine();
            setPositionNext();

            return nextLine;
        }

        /**
         * Returns the previous object in the iteration.
         *
         * @return the previous object.
         * @throws NoSuchElementException If there are no previous elements.
         * @see #hasPrevious
         */
        @Override
        public String previous() {
            checkPrevious();

            final String previousLine = getPreviousLine();
            setPositionPrevious();

            return previousLine;
        }
    }

    /**
     * This class instantiates an {@link Iterator} to iterate over a key:value MPD command
     * response.
     */
    private static class ResponseSplitIterator
            extends AbstractResultIterator<Map.Entry<String, String>> {

        /**
         * Sole constructor.
         *
         * @param response The MPD protocol command response.
         * @param position The position relative to the response to initiate the
         *                 {@link AbstractResultIterator#mPosition} to.
         */
        ResponseSplitIterator(final String response, final int position) {
            super(response, position);
        }

        /**
         * Returns the next object in the iteration.
         *
         * @return the next object.
         * @throws NoSuchElementException If there are no more elements.
         * @see #hasNext
         */
        @Override
        public Map.Entry<String, String> next() {
            checkNext();

            final Map.Entry<String, String> entry = new SimplerImmutableEntry(getNextLine());

            setPositionNext();

            return entry;
        }

        /**
         * Returns the previous object in the iteration.
         *
         * @return the previous object.
         * @throws NoSuchElementException If there are no previous elements.
         * @see #hasPrevious
         */
        @Override
        public Map.Entry<String, String> previous() {
            checkPrevious();

            final Map.Entry<String, String> entry = new SimplerImmutableEntry(getPreviousLine());

            setPositionPrevious();

            return entry;
        }
    }

    /**
     * This creates a simple map entry for key:value entries.
     */
    public static final class SimplerImmutableEntry implements Map.Entry<String, String> {

        /**
         * The MPD protocol [KEY]:[VALUE] delimiter.
         */
        private static final char MPD_KV_DELIMITER = ':';

        /**
         * The index of the delimiter in the {@link #mEntry}.
         */
        private final int mDelimiter;

        /**
         * The [KEY]:[VALUE] response line.
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
