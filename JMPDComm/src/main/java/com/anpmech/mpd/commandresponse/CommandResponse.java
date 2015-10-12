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
import com.anpmech.mpd.Tools;
import com.anpmech.mpd.connection.AbstractCommandResult;
import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.item.Directory;
import com.anpmech.mpd.item.Music;
import com.anpmech.mpd.item.PlaylistFile;

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
 * This class contains classes and methods used to process a {@link CommandResult}.
 *
 * <p>This class is immutable, thus, thread-safe.</p>
 */
public class CommandResponse extends ObjectResponse<String> {

    /**
     * The MPD protocol {@code key}:{@code value} delimiter.
     */
    private static final char MPD_KV_DELIMITER = ':';

    /**
     * The class log identifier.
     */
    private static final String TAG = "CommandResponse";

    /**
     * This constructor builds this class from the MPD protocol result.
     *
     * @param connectionResult The result of the connection initiation.
     * @param result           The MPD protocol command result.
     * @param excludeResults   This is used to manually exclude results from
     *                         {@link SplitCommandResponse} inclusion. Unused for this class.
     */
    protected CommandResponse(final String connectionResult, final String result,
            final int[] excludeResults) {
        super(connectionResult, result, null);
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
     * This constructor builds this class from an empty MPD protocol result.
     */
    public CommandResponse() {
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
        putAll(map, splitListIterator());

        return map;
    }

    /**
     * This method returns a list of {@code key}:{@code value} pairs.
     *
     * <p>An error will be produced if this is called on a non-{@code key}:{@code value} MPD server
     * response.</p>
     *
     * @return A list of {@code key}:{@code value} pairs.
     * @see #splitListIterator()
     */
    public List<Map.Entry<String, String>> getSplitList() {
        final List<Map.Entry<String, String>> list = new ArrayList<>(mListSize);
        addAll(list, splitListIterator());

        return list;
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
     * This method returns a list of values with keys equal to the {@code key} parameter from a
     * {@code key}:{@code value} MPD protocol response.
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
     * This method returns a iterator, starting at the beginning of the response.
     *
     * @param position The position to begin the iterator at, typically beginning or end.
     * @return A iterator to return the response.
     * @see #getList()
     */
    @Override
    protected ListIterator<String> listIterator(final int position) {
        return new ResultIterator(mResult, position);
    }

    /**
     * This method returns a iterator of {@code key}:{@code value} pairs, starting at the end of
     * the list.
     *
     * <p>The expectation of this iterator is to use previous() and previousIndex().</p>
     *
     * @return A iterator to return key/value pairs.
     */
    public ListIterator<Map.Entry<String, String>> reverseSplitListIterator() {
        int lastIndex = mResult.lastIndexOf(MPDCommand.MPD_CMD_NEWLINE);

        /**
         * If the last index is -1, the position will be wrongly calculated.
         */
        if (lastIndex == -1) {
            lastIndex = 0;
        }

        return new ResultSplitIterator(mResult, lastIndex);
    }

    /**
     * This method returns a iterator of {@code key}:{@code value} pairs.
     *
     * @return A iterator to return key/value pairs.
     * @see #getSplitList()
     */
    public ResultSplitIterator splitListIterator() {
        return new ResultSplitIterator(mResult, 0);
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
     * This Object provides a generic abstraction instantiation for a result iterator.
     *
     * @param <T> The type of Object to be generated from the {@link CommandResult}.
     */
    protected abstract static class AbstractObjectResultIterator<T>
            extends AbstractResultIterator<T> {

        /**
         * Sole constructor.
         *
         * @param result   The MPD protocol command result.
         * @param position The position relative to the result to initiate the {@link #mPosition}
         *                 to.
         * @throws IllegalArgumentException If the position parameter is less than 0.
         */
        protected AbstractObjectResultIterator(final String result, final int position) {
            super(result, position);
        }

        /**
         * Override this to create the Object using the response block.
         *
         * @param responseBlock The response block to create the Object from.
         * @return The object created from the response block.
         */
        abstract T instantiate(final String responseBlock);

        /**
         * Returns the next object in the iteration.
         *
         * @return the next object.
         * @throws NoSuchElementException If there are no more elements.
         * @see #hasNext
         */
        @Override
        public T next() {
            checkNext();

            final String nextLine = getNextLine();
            setPositionNext();

            return instantiate(nextLine);
        }

        /**
         * Returns the previous object in the iteration.
         *
         * @return the previous object.
         * @throws NoSuchElementException If there are no previous elements.
         * @see #hasPrevious
         */
        @Override
        public T previous() {
            checkPrevious();

            final String previousLine = getPreviousLine();
            setPositionPrevious();

            return instantiate(previousLine);
        }
    }

    /**
     * This class is an abstraction to build classes which use entire response blocks.
     *
     * <p>This class is used when the Iterator will skip no lines of the response. This typically
     * used for Iterators which output Strings.</p>
     *
     * @param <T> The type of Object to be generated from the {@link CommandResult}.
     */
    protected abstract static class FullBlockResultIterator<T>
            extends AbstractObjectResultIterator<T> {

        /**
         * Sole constructor.
         *
         * @param response The MPD protocol command response.
         * @param position The position relative to the response to initiate the
         *                 {@link AbstractCommandResult.AbstractResultIterator#mPosition} to.
         */
        protected FullBlockResultIterator(final String response, final int position) {
            super(response, position);
        }

        /**
         * Returns the previous MPD result line.
         *
         * @return The previous MPD result line.
         */
        @Override
        protected String getPreviousLine() {
            int index = cachedPreviousIndexBegin();

            /** + 1 to discard the newline. */
            if (index != 0) {
                index += 1;
            }

            return mResult.substring(index, cachedPreviousIndexEnd());
        }

        /**
         * This method returns the index of the next beginning token in relation to the current
         * position.
         *
         * @return The next beginning token in relation to the current position.
         */
        @Override
        protected int nextIndexBegin() {
            return mPosition;
        }

        /**
         * This method returns the index of the next ending token in relation to the current
         * position.
         *
         * @return The next ending token in relation to the current position.
         */
        @Override
        protected int nextIndexEnd() {
            return mResult.indexOf(MPDCommand.MPD_CMD_NEWLINE, cachedNextIndexBegin());
        }

        /**
         * This method returns the index of the prior beginning token in relation to the current
         * position.
         *
         * @return The prior beginning token in relation to the current position.
         */
        @Override
        protected int previousIndexBegin() {
            /** - 2 to discard the newline. */
            int index = mResult.lastIndexOf(MPDCommand.MPD_CMD_NEWLINE, mPosition - 2);

            if (index == -1 && mPosition != 0) {
                index = 0;
            }

            return index;
        }

        /**
         * This method returns the index of the prior ending token in relation to the current
         * position.
         *
         * @return The prior ending token in relation to the current position.
         */
        @Override
        protected int previousIndexEnd() {
            final int position;

            /**
             * If the position is the final character, it should be a newline, don't allow in the
             * Iterator.
             */
            if (mPosition == mResult.length() && mResult.charAt(mResult.length() - 1) == '\n') {
                position = mResult.length() - 1;
            } else {
                position = mPosition;
            }

            return position;
        }
    }

    /**
     * This class is used to create an {@link Iterator} to iterate over a result to find a defined
     * beginning block token and end block token to create an Object.
     *
     * <p>This Iterator should be used for iteration which requires more than one line and ends
     * with a block token rather than a {@code newline}.</p>
     *
     * @param <T> The type of Object to be generated from the {@link CommandResult}.
     */
    protected abstract static class MultiLineResultIterator<T>
            extends PartialBlockResultIterator<T> {

        /**
         * The block tokens to tokenize for the beginning of the iteration for this iterator.
         */
        private final String[] mEndBlockTokens;

        /**
         * This constructor is used when the first token found in a response is used as the
         * beginning and ending delimiter for a result.
         *
         * <p>This is used for MPD protocol results which have one single type of information in
         * the result.</p>
         *
         * @param result   The MPD protocol command result.
         * @param position The position relative to the result to initiate the {@link #mPosition}
         *                 to.
         * @throws IllegalArgumentException If the position parameter is less than 0.
         */
        protected MultiLineResultIterator(final String result, final int position) {
            super(result, position);

            mEndBlockTokens = mBeginBlockTokens;
        }

        /**
         * This constructor is used when either the beginning tokens or the ending tokens must be
         * defined.
         *
         * @param result           The MPD protocol command result.
         * @param position         The position relative to the result to initiate the
         *                         {@link #mPosition} to.
         * @param beginBlockTokens The block tokens to find the beginning of a block. This array
         *                         must be sorted in ascending natural order prior to calling this
         *                         constructor.
         * @param endBlockTokens   The block tokens to find the ending of a block. This array must
         *                         be sorted in ascending natural order prior to calling this
         *                         constructor.
         * @throws IllegalArgumentException If the position parameter is less than 0.
         */
        protected MultiLineResultIterator(final String result, final int position,
                final String[] beginBlockTokens, final String[] endBlockTokens) {
            super(result, position, beginBlockTokens);

            mEndBlockTokens = endBlockTokens;
        }

        /**
         * This method returns the index of the next ending token in relation to the current
         * position.
         *
         * @return The next ending token in relation to the current position.
         */
        @Override
        protected int nextIndexEnd() {
            int end = Tools.getNextKeyIndex(mResult, cachedNextIndexBegin() + 1, mEndBlockTokens);

            /**
             * Retrieve the final element; this only occurs if the beginning was found, so, this
             * terminates at the end of the line.
             */
            if (end == -1 && mPosition != mResult.length()) {
                end = mResult.length();
            }

            return end;
        }

        /**
         * This method returns the index of the prior ending token in relation to the current
         * position.
         *
         * @return The prior ending token in relation to the current position.
         */
        @Override
        protected int previousIndexEnd() {
            final int begin = cachedPreviousIndexBegin();
            int end = Tools.getNextKeyIndex(mResult, begin + 1, mEndBlockTokens);

            if (end == -1 && mPosition == mResult.length()) {
                end = mResult.length();
            }

            return end;
        }
    }

    /**
     * This class is used to create an {@link Iterator} to iterate over a result to find a defined
     * beginning block, ending with a newline, to create an Object.
     *
     * <p>This is an abstraction class to create a block {@link Iterator} which uses a partial
     * result.</p>
     *
     * @param <T> The type of Object to be generated from the {@link CommandResult}.
     */
    private abstract static class PartialBlockResultIterator<T>
            extends AbstractObjectResultIterator<T> {

        /**
         * This is a list of all tokens which begin a new block.
         */
        protected static final String[] ENTRY_BLOCK_TOKENS = {Directory.RESPONSE_DIRECTORY,
                Music.RESPONSE_FILE, PlaylistFile.RESPONSE_PLAYLIST_FILE};

        /**
         * The block tokens to tokenize for the beginning of the iteration for this iterator.
         */
        protected final String[] mBeginBlockTokens;

        /**
         * This constructor is used when the first token found in a response is used as the
         * beginning and ending delimiter for a result.
         *
         * <p>This is used for MPD protocol results which have one single type of information in
         * the result.</p>
         *
         * @param result   The MPD protocol command result.
         * @param position The position relative to the result to initiate the {@link #mPosition}
         *                 to.
         * @throws IllegalArgumentException If the position parameter is less than 0.
         */
        protected PartialBlockResultIterator(final String result, final int position) {
            super(result, position);

            final int index = result.indexOf(MPD_KV_DELIMITER);

            if (index == -1) {
                mBeginBlockTokens = new String[]{};
            } else {
                final String token = result.substring(0, index);

                mBeginBlockTokens = new String[]{token};
            }
        }

        /**
         * This constructor is used when either the beginning tokens or the ending tokens must be
         * defined.
         *
         * @param result           The MPD protocol command result.
         * @param position         The position relative to the result to initiate the
         *                         {@link #mPosition} to.
         * @param beginBlockTokens The block tokens to find the beginning of a block. This array
         *                         must be sorted in ascending natural order prior to calling this
         *                         constructor.
         * @throws IllegalArgumentException If the position parameter is less than 0.
         */
        protected PartialBlockResultIterator(final String result, final int position,
                final String[] beginBlockTokens) {
            super(result, position);

            mBeginBlockTokens = beginBlockTokens;
        }

        /**
         * Returns whether there are more elements to iterate.
         *
         * @return {@code true} If there are more elements, {@code false} otherwise.
         * @see #next
         */
        @Override
        public boolean hasNext() {
            return cachedNextIndexBegin() != -1 && super.hasNext();
        }

        /**
         * Returns whether there are previous elements to iterate.
         *
         * @return {@code true} If there are previous elements, {@code false} otherwise.
         * @see #previous
         */
        @Override
        public boolean hasPrevious() {
            return super.hasPrevious() && cachedPreviousIndexEnd() != -1;
        }

        /**
         * This method returns the index of the next beginning token in relation to the current
         * position.
         *
         * @return The next beginning token in relation to the current position.
         */
        @Override
        protected int nextIndexBegin() {
            return Tools.getNextKeyIndex(mResult, mPosition, mBeginBlockTokens);
        }

        /**
         * This method returns the index of the prior beginning token in relation to the current
         * position.
         *
         * @return The prior beginning token in relation to the current position.
         */
        @Override
        protected int previousIndexBegin() {
            int index = -1;
            int mpdDelimiterIndex = mResult.lastIndexOf(MPD_KV_DELIMITER, mPosition);
            int keyIndex;

            while (index == -1 && mpdDelimiterIndex != -1) {
                keyIndex = mResult.lastIndexOf(MPDCommand.MPD_CMD_NEWLINE, mpdDelimiterIndex) + 1;
                if (keyIndex < mPosition) {
                    final String foundToken = mResult.substring(keyIndex, mpdDelimiterIndex);

                    if (Arrays.binarySearch(mBeginBlockTokens, foundToken) >= 0) {
                        index = keyIndex;
                    }
                }

                if (index == -1) {
                    mpdDelimiterIndex = mResult.lastIndexOf(MPD_KV_DELIMITER,
                            mpdDelimiterIndex - 1);
                }
            }

            return index;
        }

        /**
         * This changes the position assignment to the next response block.
         */
        @Override
        protected void setPositionNext() {
            super.setPositionNext();
            mPosition--;
        }

        /**
         * This changes the position assignment to the previous newline.
         */
        @Override
        protected void setPositionPrevious() {
            super.setPositionPrevious();
            mPosition--;
        }
    }

    /**
     * This class is used to create an {@link Iterator} to iterate over each individual line of a
     * MPD command response.
     */
    public static final class ResultIterator extends FullBlockResultIterator<String> {

        /**
         * Sole constructor.
         *
         * @param result   The MPD protocol command result.
         * @param position The position relative to the result to initiate the
         *                 {@link AbstractCommandResult.AbstractResultIterator#mPosition} to.
         */
        private ResultIterator(final String result, final int position) {
            super(result, position);
        }

        /**
         * Override this to create the Object using the response block.
         *
         * @param responseBlock The response block to create the Object from.
         * @return The object created from the response block.
         */
        @Override
        String instantiate(final String responseBlock) {
            return responseBlock;
        }
    }

    /**
     * This class is used to create an {@link Iterator} to iterate over each {@code key}:{@code
     * value} of an MPD
     * command response.
     *
     * <b>This class requires a {@code key}:{@code value} MPD response.</b>
     */
    public static final class ResultSplitIterator
            extends FullBlockResultIterator<Map.Entry<String, String>> {

        /**
         * Sole constructor.
         *
         * @param response The MPD protocol command response.
         * @param position The position relative to the response to initiate the
         *                 {@link AbstractResultIterator#mPosition} to.
         */
        private ResultSplitIterator(final String response, final int position) {
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

    /**
     * This class is used to create an {@link Iterator} to iterate over a result for a specific
     * line to create an Object.
     *
     * @param <T> The type to create from this response.
     */
    protected abstract static class SingleLineResultIterator<T>
            extends PartialBlockResultIterator<T> {

        /**
         * This constructor is used when either the beginning tokens or the ending tokens must be
         * defined.
         *
         * @param result           The MPD protocol command result.
         * @param position         The position relative to the result to initiate the
         *                         {@link #mPosition} to.
         * @param beginBlockTokens The block tokens to find the beginning of a block. This array
         *                         must be sorted in ascending natural order prior to calling this
         *                         constructor.
         * @throws IllegalArgumentException If the position parameter is less than 0.
         */
        protected SingleLineResultIterator(final String result, final int position,
                final String[] beginBlockTokens) {
            super(result, position, beginBlockTokens);
        }

        /**
         * This constructor is used when the first token found in a response is used as the
         * beginning and ending delimiter for a result.
         *
         * <p>This is used for MPD protocol results which have one single type of information in
         * the result.</p>
         *
         * @param result   The MPD protocol command result.
         * @param position The position relative to the result to initiate the {@link #mPosition}
         *                 to.
         * @throws IllegalArgumentException If the position parameter is less than 0.
         */
        protected SingleLineResultIterator(final String result, final int position) {
            super(result, position);
        }

        /**
         * This method returns the index of the next ending token in relation to the current
         * position.
         *
         * @return The next ending token in relation to the current position.
         */
        @Override
        protected int nextIndexEnd() {
            return mResult.indexOf(MPDCommand.MPD_CMD_NEWLINE, cachedNextIndexBegin());
        }

        /**
         * This method returns the index of the prior ending token in relation to the current
         * position.
         *
         * @return The prior ending token in relation to the current position.
         */
        @Override
        protected int previousIndexEnd() {
            int end = mResult.indexOf(MPDCommand.MPD_CMD_NEWLINE, cachedPreviousIndexBegin());

            if (end == -1 && cachedNextIndexBegin() == 0) {
                end = mResult.length();
            }

            return end;
        }
    }
}
