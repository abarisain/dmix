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

package com.anpmech.mpd;

import com.anpmech.mpd.exception.InvalidResponseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Common utilities used by JMPDComm classes.
 */
public final class Tools {

    /**
     * This is the value used extract the key from the {@link #splitResponse(String)} return array.
     */
    public static final int KEY = 0;

    /**
     * This is the value used to extract the value from the {@link #splitResponse(String)} return
     * array.
     */
    public static final int VALUE = 1;

    /**
     * The MPD protocol {@code key}:{@code value} delimiter.
     */
    private static final char MPD_KV_DELIMITER = ':';

    /**
     * The generic fragment of the error message strings.
     */
    private static final String PARSE_ERROR = "Failed to parse as a ";

    /**
     * The class log identifier.
     */
    private static final String TAG = "Tools";

    private Tools() {
        super();
    }

    /**
     * Simple integer comparison. The result should be equivalent to Object.compare().
     *
     * @param lhs First value to compare to the second.
     * @param rhs Second value to compare to the first.
     * @return 0 if lhs = rhs, less than 0 if lhs &lt; rhs, and greater than 0 if lhs &gt; rhs.
     */
    public static int compare(final int lhs, final int rhs) {
        final int result;

        if (lhs == rhs) {
            result = 0;
        } else {
            if (lhs < rhs) {
                result = -1;
            } else {
                result = 1;
            }
        }

        return result;
    }

    /**
     * Null-safe equivalent of {@code a.equals(b)}. The result should be equivalent to
     * Object.equals().
     *
     * @param a An object.
     * @param b An object to be compared with a for equality.
     * @return True if the arguments are equal to each other, false otherwise
     */
    public static boolean equals(final Object a, final Object b) {
        if (a == null) {
            return b == null;
        } else {
            return a.equals(b);
        }
    }

    /**
     * This method searches for the next value in a String, beginning at the specified position.
     *
     * <p>The rationale behind this method is to provide a fast find method for key/value
     * searching. One advantage is {@link String#indexOf(String)} is the performance is severely
     * degraded in comparison {@link String#indexOf(int)}. A second advantage is when
     * {@link String#indexOf(String)} is used to search for multiple keys, each one will search
     * the entire String, until exhausted, even if it doesn't exist in the String, this method
     * searches key by key which, may sound slower, but has much better performance.</p>
     *
     * @param result   The result to find the {@code key} in.
     * @param position The position to begin looking for the {@code key}.
     * @param getValue This parameter controls whether to return the key index or the value index
     *                 found. If true, the value index will be returned, the key value, otherwise.
     * @param keys     An array of tokens to look for. This array must be sorted in ascending
     *                 natural order prior to calling this constructor. If this array is empty,
     *                 the first value found, if it exists will be returned. The first key found
     *                 will be the value index returned.
     * @return The index of the first key value found, the index of the first value found if the
     * {@code keys} parameter is empty, or -1 if not found.
     */
    private static int getNextIndex(final String result, final int position, final boolean getValue,
            final String... keys) {
        int index = -1;
        int mpdDelimiterIndex = result.indexOf(MPD_KV_DELIMITER, position);
        int keyIndex;

        while (index == -1 && mpdDelimiterIndex != -1) {
            keyIndex = result.lastIndexOf(MPDCommand.MPD_CMD_NEWLINE, mpdDelimiterIndex) + 1;

            if (keyIndex >= position) {
                final String foundKey = result.substring(keyIndex, mpdDelimiterIndex);
                final boolean foundKeyEqual;

                switch (keys.length) {
                    case 0:
                        foundKeyEqual = true;
                        break;
                    case 1:
                        foundKeyEqual = keys[0].equals(foundKey);
                        break;
                    default:
                        foundKeyEqual = Arrays.binarySearch(keys, foundKey) >= 0;
                        break;
                }

                if (foundKeyEqual) {
                    if (getValue) {
                        index = mpdDelimiterIndex + 2;
                    } else {
                        index = keyIndex;
                    }
                }
            }

            if (index == -1) {
                mpdDelimiterIndex = result.indexOf(MPD_KV_DELIMITER, mpdDelimiterIndex + 1);
            }
        }

        return index;
    }

    /**
     * This method searches for the next key in a String, beginning at a specified position.
     *
     * <p>The rationale behind this method is to provide a fast find method for key searching.
     * One advantage is {@link String#indexOf(String)} is the performance is severely
     * degraded in comparison {@link String#indexOf(int)}. A second advantage is when using
     * {@link String#indexOf(String)} is used to search for multiple keys, each one will search
     * the entire String, until exhausted, even if it doesn't exist in the String.</p>
     *
     * @param result   The result to find the {@code key} in.
     * @param position The position to begin looking for the {@code key}.
     * @param keys     An array of tokens to look for. This array must be sorted in ascending
     *                 natural order prior to calling this constructor. If this array is empty,
     *                 the first value found, if it exists will be returned. The first key found
     *                 will
     *                 be the key index returned.
     * @return The index of the first key found, the index of the first key found if the {@code
     * keys} parameter is empty, or -1 if not found.
     */
    public static int getNextKeyIndex(final String result, final int position,
            final String... keys) {
        return getNextIndex(result, position, false, keys);
    }

    /**
     * This method searches for the next value in a String, beginning at the specified position.
     *
     * <p>The rationale behind this method is to provide a fast find method for key searching.
     * One advantage is {@link String#indexOf(String)} is the performance is severely
     * degraded in comparison {@link String#indexOf(int)}. A second advantage is when using
     * {@link String#indexOf(String)} is used to search for multiple keys, each one will search
     * the entire String, until exhausted, even if it doesn't exist in the String.</p>
     *
     * @param result   The result to find the {@code key} in.
     * @param position The position to begin looking for the {@code key}.
     * @param keys     An array of tokens to look for. This array must be sorted in ascending
     *                 natural order prior to calling this constructor. If this array is empty,
     *                 the first value found, if it exists will be returned. The first key found
     *                 will be the value index returned.
     * @return The index of the first key value found, the index of the first value found if the
     * {@code keys} parameter is empty, or -1 if not found.
     */
    public static int getNextValueIndex(final String result, final int position,
            final String... keys) {
        return getNextIndex(result, position, true, keys);
    }

    /**
     * Checks the {@link String} for nullity or emptiness.
     *
     * @param toCheck The string to analyze.
     * @return {@code true} if null or empty, {@code false} otherwise.
     */
    public static boolean isEmpty(final String toCheck) {
        return toCheck == null || toCheck.isEmpty();
    }

    /**
     * This method iterates through a 3 dimensional array to check each two element inner array for
     * equality of it's inner objects with the isNotEqual(object, object) method.
     *
     * @param arrays The 3 dimensional array with objects to check for equality.
     * @return Returns true if an inner array was not equal.
     */
    public static boolean isNotEqual(final Object[][] arrays) {
        boolean result = false;

        for (final Object[] array : arrays) {
            if (isNotEqual(array[0], array[1])) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * Compares inside objects for an Object.equals(object) implementation.
     *
     * @param objectA An object to be compared.
     * @param objectB An object to be compared.
     * @return False if objects are both null or are equal, true otherwise.
     */
    public static boolean isNotEqual(final Object objectA, final Object objectB) {
        final boolean isEqual;

        if (objectA == null) {
            if (objectB == null) {
                isEqual = true;
            } else {
                isEqual = false;
            }
        } else {
            if (objectA.equals(objectB)) {
                isEqual = true;
            } else {
                isEqual = false;
            }
        }

        return !isEqual;
    }

    /**
     * Compares inside int values for an Object.equals(object) implementation.
     *
     * @param arrays A an array of two element arrays to be checked for equality.
     * @return True if all two element arrays are equal, false otherwise.
     */
    public static boolean isNotEqual(final int[][] arrays) {
        boolean result = false;

        for (final int[] array : arrays) {
            if (array[0] != array[1]) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * Compares inside long values for an Object.equals(object) implementation.
     *
     * @param arrays A an array of two element arrays to be checked for equality.
     * @return True if all two element arrays are equal, false otherwise.
     */
    public static boolean isNotEqual(final long[][] arrays) {
        boolean result = false;

        for (final long[] array : arrays) {
            if (array[0] != array[1]) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * This method parses the {@code value} parameter for an primitive {@code float} and returns a
     * default value upon error or {@code null} input.
     *
     * @param value The value to parse.
     * @return The value as a primitive {@code float}, {@code defaultValue} if the value cannot be
     * converted.
     */
    public static float parseFloat(final String value) {
        float result;

        if (value == null || value.isEmpty()) {
            result = Float.MIN_VALUE;
        } else {
            try {
                result = Float.parseFloat(value);
            } catch (final NumberFormatException ignored) {
                Log.error(TAG, PARSE_ERROR + "float: " + value);
                result = Float.MIN_VALUE;
            }
        }

        return result;
    }

    /**
     * This method parses the value parameter for an primitive {@code int} and returns a default
     * value upon error or {@code null} input.
     *
     * @param value The value to parse.
     * @return The value as a primitive {@code int}, {@link Integer#MIN_VALUE} if the value is not
     * an {@code int}.
     */
    public static int parseInteger(final String value) {
        return parseInteger(value, Integer.MIN_VALUE);
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
    public static int parseInteger(final String value, final int defaultValue) {
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
    public static long parseLong(final String value, final long defaultValue) {
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
     * @return The value as a primitive {@code long}, {@link Long#MIN_VALUE} if the value is not
     * long.
     */
    public static long parseLong(final String value) {
        return parseLong(value, Long.MIN_VALUE);
    }

    /**
     * This method converts a list of integers into a list of MPD protocol command argument ranges.
     * This can be a win when there are numbers in sequence which can be converted into numbered
     * ranges and sent in fewer commands. The disadvantage to this is that order has to be
     * sacrificed.
     *
     * @param integers A list of integers to convert to numbered range strings..
     * @return A collection of numbered range strings.
     * @see #sequentialToRange(int...)
     */
    public static List<CharSequence> sequentialToRange(final List<Integer> integers) {
        final ListIterator<Integer> iterator = integers.listIterator(integers.size());
        final List<CharSequence> ranges = new ArrayList<>();
        final StringBuilder stringBuilder = new StringBuilder(10);
        boolean inSequenceRange = false;
        int startRange = -1;

        Collections.sort(integers);

        while (iterator.hasPrevious()) {
            final Integer integer = iterator.previous();
            Integer nextInteger = null;

            /** Avoid out of bounds. */
            if (iterator.hasPrevious()) {
                /** Store the next integer in the iteration. */
                nextInteger = integers.get(iterator.previousIndex());
            }

            /** Specifies whether the next integer can be added to a range. */
            if (nextInteger != null && integer.equals(nextInteger + 1)) {
                if (!inSequenceRange) {
                    startRange = integer;
                }
                inSequenceRange = true;
            } else {
                if (inSequenceRange) {
                    /** Range complete, add it to the store. */
                    stringBuilder.append(integer);
                    stringBuilder.append(MPD_KV_DELIMITER);
                    /**
                     * The start range (the end number) is +1 on the
                     * MPD playlist range per the protocol.
                     */
                    stringBuilder.append(startRange + 1);
                    ranges.add(stringBuilder.toString());
                    stringBuilder.setLength(0);
                } else {
                    /** No range, add it to the store. */
                    ranges.add(integer.toString());
                }
                inSequenceRange = false;
            }
        }

        return ranges;
    }

    /**
     * This method converts a list of integers into a list of MPD protocol command argument ranges.
     * This can be a win when there are numbers in sequence which can be converted into numbered
     * ranges and sent in fewer commands. The disadvantage to this is that order has to be
     * sacrificed.
     *
     * @param integers A list of integers to convert to numbered range strings..
     * @return A collection of numbered range strings.
     * @see #sequentialToRange(List)
     */
    public static List<CharSequence> sequentialToRange(final int... integers) {
        final List<CharSequence> ranges = new ArrayList<>();
        final StringBuilder stringBuilder = new StringBuilder(10);
        boolean inSequenceRange = false;
        int startRange = -1;

        Arrays.sort(integers);

        for (int i = integers.length - 1; i >= 0; i--) {
            final int integer = integers[i];
            final int nextInteger;

            if (i == 0) {
                /** Avoid out of bounds. */
                nextInteger = -1;
            } else {
                /** Store the next integer in the iteration. */
                nextInteger = integers[i - 1];
            }

            /** Specifies whether the next integer can be added to a range. */
            if (nextInteger != -1 && integer == nextInteger + 1) {
                if (!inSequenceRange) {
                    startRange = integer;
                }
                inSequenceRange = true;
            } else {
                if (inSequenceRange) {
                    /** Range complete, add it to the store. */
                    stringBuilder.append(integer);
                    stringBuilder.append(MPD_KV_DELIMITER);
                    /**
                     * The start range (the end number) is +1 on the
                     * MPD playlist range per the protocol.
                     */
                    stringBuilder.append(startRange + 1);
                    ranges.add(stringBuilder.toString());
                    stringBuilder.setLength(0);
                } else {
                    /** No range, add it to the store. */
                    ranges.add(Integer.toString(integer));
                }
                inSequenceRange = false;
            }
        }

        return ranges;
    }

    /**
     * Split the standard MPD protocol response into a three dimensional array consisting of a two
     * element String array key / value pairs.
     *
     * @param list The incoming server response.
     * @return A three dimensional {@code String} array of two element {@code String arrays}.
     */
    public static String[][] splitResponse(final Collection<String> list) {
        final String[][] results = new String[list.size()][];
        int iterator = 0;

        for (final String line : list) {
            results[iterator] = splitResponse(line);
            iterator++;
        }

        return results;
    }

    /**
     * Split the standard MPD protocol response.
     *
     * @param line The MPD response string.
     * @return A string array with two elements, one the key, the second the value.
     */
    public static String[] splitResponse(final String line) {
        final int delimiterIndex = line.indexOf(':');
        final String[] result = new String[2];

        if (delimiterIndex == -1) {
            throw new InvalidResponseException("Failed to parse server response key for line: " +
                    line);
        }

        result[0] = line.substring(0, delimiterIndex);

        /** Skip ': ' */
        result[1] = line.substring(delimiterIndex + 2);

        return result;
    }

    /**
     * This method takes seconds and converts it into HH:MM:SS.
     *
     * @param totalSeconds Seconds to convert to a string.
     * @return Returns time formatted from the {@code totalSeconds} in format HH:MM:SS.
     */
    public static CharSequence timeToString(final long totalSeconds) {
        final long hours = TimeUnit.SECONDS.toHours(totalSeconds);
        long secondCalc = totalSeconds - TimeUnit.HOURS.toSeconds(hours);
        final long minutes = TimeUnit.SECONDS.toMinutes(secondCalc);
        secondCalc -= TimeUnit.MINUTES.toSeconds(minutes);
        final long seconds = TimeUnit.SECONDS.toSeconds(secondCalc);
        final StringBuilder stringBuilder;
        int length;

        if (hours == 0) {
            stringBuilder = new StringBuilder(5);
        } else {
            stringBuilder = new StringBuilder(8);
            stringBuilder.append(hours);

            if (stringBuilder.length() == 1) {
                stringBuilder.insert(0, '0');
            }

            stringBuilder.append(':');
        }

        length = stringBuilder.length();
        stringBuilder.append(minutes);

        if (stringBuilder.length() - length == 1) {
            stringBuilder.insert(length, '0');
        }

        stringBuilder.append(':');
        length = stringBuilder.length();
        stringBuilder.append(seconds);

        if (stringBuilder.length() - length == 1) {
            stringBuilder.insert(length, '0');
        }

        return stringBuilder;
    }

    /**
     * Blocks indefinitely until this object is valid.
     *
     * @param semaphore The semaphore to check for validity.
     * @throws InterruptedException If the current thread is {@link Thread#interrupted()}.
     */
    public static void waitForValidity(final Semaphore semaphore) throws InterruptedException {
        try {
            semaphore.acquire();
        } finally {
            semaphore.release();
        }
    }

    /**
     * Blocks for the given waiting time.
     *
     * @param semaphore The semaphore to check for validity.
     * @param timeout   The time to wait for a valid object.
     * @param unit      The time unit of the {@code timeout} argument.
     * @return {@code true} if a the {@code ResponseMap} was valid by the time of return, false
     * otherwise.
     * @throws InterruptedException If the current thread is {@link Thread#interrupted()}.
     */
    public static boolean waitForValidity(final Semaphore semaphore, final long timeout,
            final TimeUnit unit) throws InterruptedException {
        boolean isValid = false;

        try {
            isValid = semaphore.tryAcquire(timeout, unit);
        } finally {
            if (isValid) {
                semaphore.release();
            }
        }

        return isValid;
    }
}
