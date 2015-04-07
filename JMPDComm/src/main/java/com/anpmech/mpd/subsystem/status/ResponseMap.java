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

package com.anpmech.mpd.subsystem.status;

import com.anpmech.mpd.Log;
import com.anpmech.mpd.connection.CommandResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * This is a class which serves as the base for a <A HREF="http://www.musicpd.org/doc/protocol"
 * target="_top">MPD protocol</A> key/value based responses with useful tools for processing and
 * error handling.
 *
 * <p>This class is designed with thread-safety in mind. If the class generated map is used, as
 * long as there is only one map writer at a time, there should be no concurrency issues. If there
 * is going to be more than one writer at a time, external locking, or a the alternative
 * constructor will be required.</p>
 */
class ResponseMap {

    /**
     * This is the value given if there was no float value assigned to the given key in the map.
     */
    protected static final float FLOAT_DEFAULT = Float.NaN;

    /**
     * This is the value given if there was no int value assigned to the given key in the map.
     */
    protected static final int INTEGER_DEFAULT = Integer.MIN_VALUE;

    /**
     * This is the value given if there was no long value assigned to the given key in the map.
     */
    protected static final long LONG_DEFAULT = Long.MIN_VALUE;

    /**
     * This is the value given if there was no String value assigned to the given key in the map.
     */
    protected static final String STRING_DEFAULT = null;

    /**
     * The generic fragment of the error message strings.
     */
    private static final String PARSE_ERROR = "Failed to parse as a ";

    /**
     * The log class identifier.
     */
    private static final String TAG = "ResponseMap";

    /**
     * This Semaphore allows blocking to wait for the map's initial update.
     *
     * <p>This Semaphore is constructed with a lack of permits, denoting no map validity until set
     * otherwise.</p>
     */
    private final Semaphore mMapValidity = new Semaphore(0);

    /**
     * The storage map for the &lt;Key, Value&gt; pairs from the response.
     */
    private final Map<CharSequence, String> mResponseMap;

    /**
     * This constructs the {@link ConcurrentHashMap} backed ResponseMap.
     *
     * <p>This map will always have a {@code loadFactor} of {@code 0.75f} and a {@code
     * concurrencyLevel} of {@code 1}, as reasonably, there will only be one writer at a time.</p>
     *
     * @param defaultEntryCount The initial capacity. The implementation performs internal sizing
     *                          to accommodate this many elements.
     */
    protected ResponseMap(final int defaultEntryCount) {
        super();

        /**
         * It is unlikely that more than one thread would write to the map at one time.
         */
        final int concurrencyLevel = 1;

        /**
         * The ConcurrencyMap default.
         */
        final float loadFactor = 0.75f;

        mResponseMap = new ConcurrentHashMap<>(defaultEntryCount, loadFactor, concurrencyLevel);
    }

    /**
     * This constructs a {@link Map} backed ResponseMap, this is useful for immutable or
     * for an alternative subclass mapping. The map given <b>will</b> be modified if {@link
     * #update(CommandResponse)} is called.
     *
     * @param map The alternate mapping to use for backend storage.
     */
    protected ResponseMap(final Map<CharSequence, String> map) {
        super();

        //noinspection AssignmentToCollectionOrArrayFieldFromParameter
        mResponseMap = map;
    }

    /**
     * This method parses the {@code value} parameter for an primitive {@code float} and returns a
     * default value upon error or {@code null} input.
     *
     * @param value        The value to parse.
     * @param defaultValue The value to return if the value parameter cannot be converted to a
     *                     {@code float}.
     * @return The value as a primitive {@code float}, {@code defaultValue} if the value cannot be
     * converted.
     */
    private static float parseFloat(final String value, final float defaultValue) {
        float result;

        if (value == null || value.isEmpty()) {
            result = defaultValue;
        } else {
            try {
                result = Float.parseFloat(value);
            } catch (final NumberFormatException ignored) {
                Log.error(TAG, PARSE_ERROR + "float: " + value);
                result = defaultValue;
            }
        }

        return result;
    }

    /**
     * This method parses the value parameter for an primitive {@code int} and returns a default
     * value upon error or {@code null} input.
     *
     * @param value The value to parse.
     * @return The value as a primitive {@code int}, {@link #INTEGER_DEFAULT} if the value is not an
     * {@code int}.
     */
    protected static int parseInteger(final String value) {
        return parseInteger(value, INTEGER_DEFAULT);
    }

    /**
     * This method parses the value parameter for an primitive int and returns a default value upon
     * error or null input.
     *
     * @param value        The value to parse.
     * @param defaultValue The value to return if the value parameter cannot be converted to an
     *                     int.
     * @return The value as a primitive int, {@code defaultValue} if the value cannot be converted.
     */
    private static int parseInteger(final String value, final int defaultValue) {
        int result;

        if (value == null || value.isEmpty()) {
            result = defaultValue;
        } else {
            try {
                result = Integer.parseInt(value);
            } catch (final NumberFormatException ignored) {
                Log.error(TAG, PARSE_ERROR + "integer: " + value);
                result = defaultValue;
            }
        }

        return result;
    }

    /**
     * This method parses the {@code value} parameter for an primitive {@code long} and returns a
     * default value upon error or {@code null} input.
     *
     * @param value        The value to parse.
     * @param defaultValue The value to return if the value parameter cannot be converted to a
     *                     {@code long}.
     * @return The value as a primitive {@code long}, {@code defaultValue} if the value cannot be
     * converted.
     */
    private static long parseLong(final String value, final long defaultValue) {
        long result;

        if (value == null || value.isEmpty()) {
            result = defaultValue;
        } else {
            try {
                result = Long.parseLong(value);
            } catch (final NumberFormatException ignored) {
                Log.error(TAG, PARSE_ERROR + "long: " + value);
                result = defaultValue;
            }
        }

        return result;
    }

    /**
     * This method parses the value parameter for an primitive {@code long} and returns a default
     * value upon error or null input.
     *
     * @param value The value to parse.
     * @return The value as a primitive {@code long}, {@link #LONG_DEFAULT} if the value is not
     * long.
     */
    protected static long parseLong(final String value) {
        return parseLong(value, LONG_DEFAULT);
    }

    /**
     * Returns a copy of the backend storage for this class.
     *
     * @return A copy of the backend storage for this class.
     */
    protected Map<CharSequence, String> getMap() {
        return new HashMap<>(mResponseMap);
    }

    /**
     * This method retrieves the value to the key parameter.
     *
     * @param key The key to the value to retrieve.
     * @return The value assigned to the key passed in the parameter, {@link #STRING_DEFAULT} if not
     * found.
     */
    protected String getMapValue(final CharSequence key) {
        final String value;

        if (mResponseMap.containsKey(key)) {
            value = mResponseMap.get(key);
        } else {
            value = STRING_DEFAULT;
        }

        return value;
    }

    /**
     * Invalidate this {@code ResponseMap}.
     */
    public void invalidate() {
        mResponseMap.clear();
        mMapValidity.tryAcquire();
    }

    /**
     * Checks the status map for the key and retrieves the value parsed as a MPD protocol boolean.
     *
     * @param key The key value to check for boolean.
     * @return True if the value is "1" (MPD protocol true), false otherwise.
     */
    protected boolean isMapValueTrue(final CharSequence key) {
        return "1".equals(mResponseMap.get(key));
    }

    /**
     * Lets callers know if the subclass Object is valid.
     *
     * @return True if the subclass is valid, false otherwise.
     */
    public boolean isValid() {
        return !mResponseMap.isEmpty();
    }

    /**
     * This method retrieves the value assigned to the key parameter from the map and converts it
     * to a primitive {@code float}.
     *
     * @param key The key to the value to retrieve.
     * @return The value assigned to the key as a {@code float}, or {@link #FLOAT_DEFAULT} if the
     * conversion failed.
     */
    protected float parseMapFloat(final CharSequence key) {
        return parseFloat(getMapValue(key), FLOAT_DEFAULT);
    }

    /**
     * This method retrieves the value assigned to the {@code key} parameter from the map and
     * converts it to a primitive {@code float}.
     *
     * @param key          The key to the value to retrieve.
     * @param defaultValue The value to return if the value assigned to the key parameter cannot be
     *                     converted to an {@code float}.
     * @return The value assigned to the key as a {@code float}, or {@code defaultValue} if the
     * conversion failed.
     */
    private float parseMapFloat(final CharSequence key, final float defaultValue) {
        return parseFloat(getMapValue(key), defaultValue);
    }

    /**
     * This method retrieves the value assigned to the {@code key} parameter from the map and
     * converts it to a primitive {@code int}.
     *
     * @param key The key to the value to retrieve.
     * @return The value assigned to the key as a {@code int}, or {@link #INTEGER_DEFAULT} if the
     * conversion failed.
     */
    protected int parseMapInteger(final CharSequence key) {
        return parseInteger(getMapValue(key));
    }

    /**
     * This method retrieves the value assigned to the {@code key} parameter from the map and
     * converts it to a primitive {@code int}.
     *
     * @param key          The key to the value to retrieve.
     * @param defaultValue The value to return if the value assigned to the key parameter cannot be
     *                     converted to an {@code int}.
     * @return The value assigned to the key as a {@code int}, or {@code defaultValue} if the
     * conversion failed.
     */
    private int parseMapInteger(final CharSequence key, final int defaultValue) {
        return parseInteger(getMapValue(key), defaultValue);
    }

    /**
     * This method retrieves the value assigned to the {@code key} parameter from the map and
     * converts it to a primitive {@code long}.
     *
     * @param key The key to the value to retrieve.
     * @return The value assigned to the key as a {@code long}, or {@link #LONG_DEFAULT} if the
     * conversion failed.
     */
    protected long parseMapLong(final CharSequence key) {
        return parseLong(getMapValue(key));
    }

    /**
     * This method retrieves the value assigned to the {@code key} parameter from the map and
     * converts it to a primitive {@code long}.
     *
     * @param key          The key to the value to retrieve.
     * @param defaultValue The value to return if the value assigned to the key parameter cannot be
     *                     converted to a {@code long}.
     * @return The value assigned to the key as a {@code long}, or {@code defaultValue} if the
     * conversion failed.
     */
    private long parseMapLong(final CharSequence key, final long defaultValue) {
        return parseLong(getMapValue(key), defaultValue);
    }

    /**
     * Simply returns the map from the media server response for the command.
     *
     * <p>Subclasses should override this method and include it's own line of values.</p>
     *
     * @return The map from the media server response for the command.
     */
    @Override
    public String toString() {
        return "ResponseMap: " + mResponseMap + '\n';
    }

    /**
     * Updates the map with a key/value MPD protocol response.
     *
     * @param commandResponse The response from the server.
     */
    public void update(final CommandResponse commandResponse) {
        final Map<CharSequence, String> map = commandResponse.getKeyValueMap();

        /**
         * Delete entries (by key) which don't exist in the new map then replace. This avoids a
         * potential race between clear() and put[All]().
         */
        mResponseMap.keySet().retainAll(map.keySet());
        mResponseMap.putAll(map);

        if (mMapValidity.availablePermits() == 0) {
            mMapValidity.release();
        }
    }

    /**
     * Blocks indefinitely until this object is valid.
     *
     * @throws InterruptedException If the current thread is {@link Thread#interrupted()}.
     */
    public void waitForValidity() throws InterruptedException {
        try {
            mMapValidity.acquire();
        } finally {
            mMapValidity.release();
        }
    }

    /**
     * Blocks for the given waiting time.
     *
     * @param timeout The time to wait for a valid object.
     * @param unit    The time unit of the {@code timeout} argument.
     * @return {@code true} if a the {@code ResponseMap} was valid by the time of return, false
     * otherwise.
     * @throws InterruptedException If the current thread is {@link Thread#interrupted()}.
     */
    public boolean waitForValidity(final long timeout, final TimeUnit unit)
            throws InterruptedException {
        boolean isValid = false;

        try {
            isValid = mMapValidity.tryAcquire(timeout, unit);
        } finally {
            if (isValid) {
                mMapValidity.release();
            }
        }

        return isValid;
    }
}
