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

package com.anpmech.mpd.connection;

import com.anpmech.mpd.MPDCommand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * This class stores and processes the result for a MPD command response.
 *
 * <p>This class is immutable, thus, thread-safe.</p>
 */
public class CommandResponse implements Iterable<String> {

    /**
     * The class log identifier.
     */
    private static final String TAG = "CommandResponse";

    /**
     * The MPD protocol command response.
     */
    protected final String mResponse;

    /**
     * The result of the connection initiation.
     */
    private final String mConnectionResult;

    /**
     * This is a mutable hint for list size.
     */
    protected int mListSize = 16;

    /**
     * The sole constructor.
     *
     * @param connectionResult The result of the connection initiation.
     * @param response         The MPD protocol command response.
     */
    protected CommandResponse(final String connectionResult, final String response) {
        super();

        mConnectionResult = connectionResult;
        mResponse = response;
    }

    /**
     * Converts a Iterator&lt;&gt; to a List&lt;&gt; of the same type if the entry key matches the
     * {@code key} parameter.
     *
     * @param collection The collection to add the entries from the Iterator to.
     * @param iterator   The iterator with entries to add to the collection.
     * @param <T>        The type of the Iterator entry.
     * @return True if the list was modified, false otherwise.
     */
    private static <T> boolean addAll(final Collection<T> collection,
            final Iterator<? extends T> iterator) {
        final int hash = collection.hashCode();

        while (iterator.hasNext()) {
            collection.add(iterator.next());
        }

        return hash != collection.hashCode();
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
     * Returns the first string response from the media server after connection. This method is
     * mainly for debugging.
     *
     * @return A string representation of the connection result.
     * @see #getMPDVersion() Use of this method is preferred.
     */
    public String getConnectionResult() {
        return mConnectionResult;
    }

    public Map<CharSequence, String> getKeyValueMap() {
        final Map<CharSequence, String> map = new HashMap<>(mListSize);
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
     * Processes the {@code CommandResponse} connection response to store the current media server
     * MPD protocol version.
     *
     * @return Returns the MPD version retained from the connection result.
     */
    public int[] getMPDVersion() {
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
     * This method returns a list of key:value pairs.
     *
     * <p>An error will be produced if this is called on a non-key/value MPD server response.</p>
     *
     * @return A list of key:value pairs.
     * @see #splitListIterator()
     */
    public List<Map.Entry<CharSequence, String>> getSplitList() {
        final List<Map.Entry<CharSequence, String>> list = new ArrayList<>(mListSize);
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
    public List<String> getValues(final CharSequence key) {
        final List<String> values = new ArrayList<>(mListSize);
        addAllMatching(values, splitListIterator(), key);

        return values;
    }

    /**
     * This checks the connection response for validity.
     *
     * @return True if the connection header exists, false otherwise.
     */
    public boolean isHeaderValid() {
        return mConnectionResult != null;
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
        return new ResponseIterator(mResponse, 0);
    }

    /**
     * This method returns a iterator, starting at the end of the response.
     *
     * <p>The expectation of this iterator is to use previous() and previousIndex().</p>
     *
     * @return A iterator to return response, line by line, starting at the end.
     */
    public ListIterator<String> reverseListIterator() {
        return new ResponseIterator(mResponse, mResponse.length());
    }

    /**
     * This method returns a iterator of key:value pairs, starting at the end of the list.
     *
     * <p>The expectation of this iterator is to use previous() and previousIndex().</p>
     *
     * @return A iterator to return key/value pairs.
     */
    public ListIterator<Map.Entry<CharSequence, String>> reverseSplitListIterator() {
        return new ResponseSplitIterator(mResponse, mResponse.length());
    }

    /**
     * This method returns a iterator of key:value pairs.
     *
     * @return A iterator to return key/value pairs.
     * @see #getSplitList()
     */
    public ListIterator<Map.Entry<CharSequence, String>> splitListIterator() {
        return new ResponseSplitIterator(mResponse, 0);
    }

    /**
     * Returns the some results from the command response.
     *
     * @return Some results from the command response.
     */
    @Override
    public String toString() {
        return "CommandResponse{" +
                "mConnectionResult='" + mConnectionResult + '\'' +
                ", mResponse='" + mResponse + '\'' +
                ", mListSize=" + mListSize +
                '}';
    }

    /**
     * This class is used to create Iterators to iterate over a MPD command response.
     *
     * @param <T> The type of the Iterator.
     */
    protected abstract static class AbstractResponseIterator<T> implements ListIterator<T> {

        /**
         * The error given if no more elements remain for this iterator instance.
         */
        private static final String NO_MORE_ELEMENTS_REMAIN = "No more elements remain.";

        /**
         * The error given if trying an operation which this iterator doesn't support.
         */
        private static final String UNSUPPORTED = "Operation unsupported by this iterator.";

        /**
         * The MPD protocol command response.
         */
        private final String mResponse;

        /**
         * The current position of this iterator relative to the response.
         */
        private int mPosition;

        /**
         * Sole constructor.
         *
         * @param response The MPD protocol command response.
         * @param position The position relative to the response to initiate the {@link #mPosition}
         *                 to.
         */
        protected AbstractResponseIterator(final String response, final int position) {
            super();

            mResponse = response;
            mPosition = position;
        }

        /**
         * The add operation is invalid for this iterator.
         *
         * @param object The object to insert.
         */
        @Override
        public void add(final T object) {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }

        /**
         * Checks for next element, if not throws an exception.
         */
        protected void checkNext() {
            if (!hasNext()) {
                throw new NoSuchElementException(NO_MORE_ELEMENTS_REMAIN);
            }
        }

        /**
         * Checks for previous element, if not throws an exception.
         */
        protected void checkPrevious() {
            if (!hasPrevious()) {
                throw new NoSuchElementException(NO_MORE_ELEMENTS_REMAIN);
            }
        }

        /**
         * Returns the next MPD response line.
         *
         * @return The next MPD response line.
         */
        protected String getNextLine() {
            return mResponse.substring(mPosition, nextIndex());
        }

        /**
         * Returns the previous MPD response line.
         *
         * @return The previous MPD response line.
         */
        protected String getPreviousLine() {
            return mResponse.substring(previousIndex(), mPosition);
        }

        /**
         * Returns whether there are more elements to iterate.
         *
         * @return {@code true} If there are more elements, {@code false} otherwise.
         * @see #next
         */
        @Override
        public boolean hasNext() {
            return nextIndex() != -1;
        }

        /**
         * Returns whether there are previous elements to iterate.
         *
         * @return {@code true} If there are previous elements, {@code false} otherwise.
         * @see #previous
         */
        @Override
        public boolean hasPrevious() {
            return previousIndex() != -1;

        }

        /**
         * Returns the index of the next object in the iteration.
         *
         * @return The index of the next object, or the size of the list if the iterator is at the
         * end.
         * @see #next
         */
        @Override
        public int nextIndex() {
            return mResponse.indexOf(MPDCommand.MPD_CMD_NEWLINE, mPosition);
        }

        /**
         * Returns the index of the previous object in the iteration.
         *
         * @return The index of the previous object, or -1 if the iterator is at the beginning.
         * @see #previous
         */
        @Override
        public int previousIndex() {
            return mResponse.lastIndexOf(MPDCommand.MPD_CMD_NEWLINE, mPosition);
        }

        /**
         * This object is immutable and it's contents cannot be removed, this method is not
         * supported.
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }

        /**
         * This object is immutable and it's contents cannot be set, this method is not
         * supported.
         *
         * @param object The object to set.
         */
        @Override
        public void set(final T object) {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }

        /**
         * This changes the position assignment to the next newline.
         */
        protected void setPositionNext() {
            mPosition = nextIndex() + 1;
        }

        /**
         * This changes the position assignment to the previous newline.
         */
        protected void setPositionPrevious() {
            mPosition = previousIndex() + 1;
        }

        @Override
        public String toString() {
            return "AbstractResponseIterator{" +
                    "mResponse='" + mResponse + '\'' +
                    ", mPosition=" + mPosition +
                    '}';
        }
    }

    /**
     * This class instantiates a iterator to iterate over the MPD command response.
     */
    private static class ResponseIterator extends AbstractResponseIterator<String> {

        /**
         * Sole constructor.
         *
         * @param response The MPD protocol command response.
         * @param position The position relative to the response to initiate the
         *                 {@link CommandResponse.AbstractResponseIterator#mPosition} to.
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
     * This class instantiates a iterator to iterate over a key:value MPD command response.
     */
    private static class ResponseSplitIterator
            extends AbstractResponseIterator<Map.Entry<CharSequence, String>> {

        /**
         * Sole constructor.
         *
         * @param response The MPD protocol command response.
         * @param position The position relative to the response to initiate the
         *                 {@link CommandResponse.AbstractResponseIterator#mPosition} to.
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
        public Map.Entry<CharSequence, String> next() {
            checkNext();

            final Map.Entry<CharSequence, String> entry = new SimplerImmutableEntry(getNextLine());

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
        public Map.Entry<CharSequence, String> previous() {
            checkPrevious();

            final Map.Entry<CharSequence, String> entry =
                    new SimplerImmutableEntry(getPreviousLine());
            setPositionPrevious();

            return entry;
        }
    }

    /**
     * This creates a simple map entry for our key/value entries.
     */
    static final class SimplerImmutableEntry implements Map.Entry<CharSequence, String> {

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
        public CharSequence getKey() {
            return mEntry.subSequence(0, mDelimiter);
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

        @Override
        public String toString() {
            return "SimplerImmutableEntry{" +
                    "mEntry='" + mEntry + '\'' +
                    ", mDelimiter=" + mDelimiter +
                    '}';
        }
    }
}
