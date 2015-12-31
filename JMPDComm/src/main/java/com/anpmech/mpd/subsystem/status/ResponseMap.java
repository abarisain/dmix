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

package com.anpmech.mpd.subsystem.status;

import com.anpmech.mpd.Tools;
import com.anpmech.mpd.commandresponse.KeyValueResponse;

import java.util.Collections;
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
    private final Map<String, String> mResponseMap;

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
     * #update(KeyValueResponse)} is called.
     *
     * @param map The alternate mapping to use for backend storage.
     */
    protected ResponseMap(final Map<String, String> map) {
        super();

        //noinspection AssignmentToCollectionOrArrayFieldFromParameter
        mResponseMap = Collections.unmodifiableMap(map);
    }

    /**
     * Returns a copy of the backend storage for this class.
     *
     * @return A copy of the backend storage for this class.
     */
    protected Map<String, String> getMap() {
        return new HashMap<>(mResponseMap);
    }

    /**
     * This method retrieves the value to the key parameter.
     *
     * @param key The key to the value to retrieve.
     * @return The value assigned to the key passed in the parameter, {@link #STRING_DEFAULT} if not
     * found.
     */
    protected String getMapValue(final String key) {
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
        mMapValidity.drainPermits();
    }

    /**
     * Checks the status map for the key and retrieves the value parsed as a MPD protocol boolean.
     *
     * @param key The key value to check for boolean.
     * @return True if the value is "1" (MPD protocol true), false otherwise.
     */
    protected boolean isMapValueTrue(final String key) {
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
    protected float parseMapFloat(final String key) {
        return Tools.parseFloat(getMapValue(key));
    }

    /**
     * This method retrieves the value assigned to the {@code key} parameter from the map and
     * converts it to a primitive {@code int}.
     *
     * @param key The key to the value to retrieve.
     * @return The value assigned to the key as a {@code int}, or {@link #INTEGER_DEFAULT} if the
     * conversion failed.
     */
    protected int parseMapInteger(final String key) {
        return Tools.parseInteger(getMapValue(key));
    }

    /**
     * This method retrieves the value assigned to the {@code key} parameter from the map and
     * converts it to a primitive {@code long}.
     *
     * @param key The key to the value to retrieve.
     * @return The value assigned to the key as a {@code long}, or {@link #LONG_DEFAULT} if the
     * conversion failed.
     */
    protected long parseMapLong(final String key) {
        return Tools.parseLong(getMapValue(key));
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
    public void update(final KeyValueResponse commandResponse) {
        final Map<String, String> map = commandResponse.getKeyValueMap();

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
        Tools.waitForValidity(mMapValidity);
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
        return Tools.waitForValidity(mMapValidity, timeout, unit);
    }
}
