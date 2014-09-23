/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.tools;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A {@link Map} that supports multiple values per key.
 */
public class MultiMap<K, V> implements Serializable {

    private static final long serialVersionUID = 6716321360137860110L;

    private final Map<K, List<V>> mInternalMap;

    public MultiMap() {
        super();
        mInternalMap = new HashMap<>();
    }

    /**
     * Recursive method that will append characters to proposedKey until its
     * unique. Used in case there are collisions with generated key values.
     *
     * @param uniqueMap   The haystack.
     * @param proposedKey The proposed needle.
     * @param value       The value which goes with the needle.
     */
    private String addUniqueEntry(final Map<String, V> uniqueMap, final String proposedKey,
            final V value) {
        // not the most efficient algorithm, but should work
        if (uniqueMap.containsKey(proposedKey)) {
            return addUniqueEntry(uniqueMap, String.format("%s%s", proposedKey, "X"), value);
        } else {
            uniqueMap.put(proposedKey, value);
            return proposedKey;
        }
    }

    /**
     * Clears the map.
     */
    public void clear() {
        mInternalMap.clear();
    }

    /**
     * Checks whether the map contains the specified key.
     *
     * @see {@link Map#containsKey(Object)} ()}
     */
    public boolean containsKey(final K key) {
        return mInternalMap.containsKey(key);
    }

    /**
     * Checks whether the map contains the specified value.
     *
     * @see {@link Map#containsValue(Object)} ()}
     */
    public boolean containsValue(final V value) {
        for (final List<V> valueList : mInternalMap.values()) {
            if (valueList.contains(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        final MultiMap<?, ?> other = (MultiMap<?, ?>) o;
        return mInternalMap != null && mInternalMap.equals(other.mInternalMap);
    }

    /**
     * Gets the list of values associated with each key.
     */
    public List<V> get(final K key) {
        return mInternalMap.get(key);
    }

    /**
     * Construct a new map, that contains a unique String key for each value.
     * <p/>
     * Current algorithm will construct unique key by appending a unique
     * position number to key's toString() value
     *
     * @return a {@link Map}
     */
    public Map<String, V> getUniqueMap() {
        final Map<String, V> uniqueMap = new HashMap<>();
        for (final Map.Entry<K, List<V>> entry : mInternalMap.entrySet()) {
            int count = 1;
            for (final V value : entry.getValue()) {
                if (count == 1) {
                    addUniqueEntry(uniqueMap, entry.getKey().toString(), value);
                } else {
                    // append unique number to key for each value
                    addUniqueEntry(uniqueMap, String.format("%s%d", entry.getKey(), count), value);
                }
                count++;
            }
        }
        return uniqueMap;
    }

    @Override
    public int hashCode() {
        return mInternalMap.hashCode();
    }

    /**
     * Check if map is empty.
     *
     * @see {@link Map#isEmpty()}
     */
    public boolean isEmpty() {
        return mInternalMap.isEmpty();
    }

    /**
     */
    public Iterable<K> keySet() {
        return mInternalMap.keySet();
    }

    /**
     * Adds the value to the list associated with a key.
     *
     * @see {@link Map#put(Object, Object)} ()}
     */
    public V put(final K key, final V value) {
        List<V> valueList = mInternalMap.get(key);
        if (valueList == null) {
            valueList = new LinkedList<>();
            mInternalMap.put(key, valueList);
        }
        valueList.add(value);
        return value;
    }

    /**
     * Adds all entries in given {@link Map} to this .
     */
    public void putAll(final Map<? extends K, ? extends V> map) {
        for (final Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Adds all entries in given  to this .
     */
    public void putAll(final MultiMap<K, ? extends V> map) {
        for (final K key : map.keySet()) {
            for (final V value : map.get(key)) {
                put(key, value);
            }
        }
    }

    /**
     * Removes all values associated with the specified key.
     */
    public List<V> remove(final K key) {
        return mInternalMap.remove(key);
    }

    /**
     * Returns the number of keys in the map
     */
    public int size() {
        return mInternalMap.size();
    }

    /**
     * Returns list of all values.
     */
    public List<V> values() {
        final List<V> allValues = new LinkedList<>();
        for (final List<V> valueList : mInternalMap.values()) {
            allValues.addAll(valueList);
        }
        return allValues;
    }
}
