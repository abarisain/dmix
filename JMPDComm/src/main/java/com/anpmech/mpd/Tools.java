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
import java.util.concurrent.TimeUnit;

/**
 * Common utilities used for processing the MPD protocol and processing.
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
     * Finds the {@code value} associated with a {@code key} in a {@code response}.
     *
     * @param response The response to search for the {@code key} key parameter.
     * @param key      The {@code key} to extract the {@code value} from.
     * @return The value associated with the {@code key} parameter in the {@code response}.
     */
    public static String findValue(final String response, final String key) {
        final int keyIndex = response.indexOf(key + ": ");
        String value = null;

        if (keyIndex != -1) {
            final int valueIndex = keyIndex + key.length() + 2;
            final int valueEndIndex = response.indexOf('\n', valueIndex);

            if (valueEndIndex == -1) {
                /**
                 * The remainder of the response.
                 */
                value = response.substring(valueIndex, response.length());
            } else {
                value = response.substring(valueIndex, valueEndIndex);
            }
        }

        return value;
    }

    /**
     * A simple filename extension extractor.
     *
     * @param filename The filename to extract the extension from.
     * @return The extension extracted from the filename parameter.
     */
    public static String getExtension(final String filename) {
        final int index = filename.lastIndexOf('.');
        final int extLength = filename.length() - index - 1;
        final int extensionShort = 2;
        final int extensionLong = 4;
        String result = null;

        if (extLength >= extensionShort && extLength <= extensionLong) {
            result = filename.substring(index + 1);
        }

        return result;
    }

    /**
     * Gets a beginning and an end range of sub-server responses, based on a parameter key.
     *
     * <p>While this method functions very similarly, this list will <B>end</B> with the key,
     * rather than begin with the key like {@link #getRanges(Collection)}.</p>
     *
     * @param response The server response to parse.
     * @param key      The key to the beginning/end of a sub-list.
     * @return A two int array. The first int is either the beginning of the list, or the one
     * position beyond the found key. The second int is one position before the next key or the end
     * of the list (for {@link List#subList(int, int)} compatibility).
     * @see #getRanges(Collection)
     */
    public static Collection<int[]> getRanges(final Collection<String> response, final String key) {
        final int responseSize = response.size();
        /** Initialize the range after the capacity is known. */
        Collection<int[]> ranges = null;
        int iterator = 0;
        int beginIndex = 0;

        for (final String line : response) {
            final int index = line.indexOf(':');
            final CharSequence formatted;

            if (index == -1) {
                formatted = line;
            } else {
                formatted = line.subSequence(0, index);
            }

            if (key.contentEquals(formatted) && iterator != beginIndex) {
                if (ranges == null) {
                    final int capacity = responseSize / (iterator + 1);
                    ranges = new ArrayList<>(capacity);
                }

                if (beginIndex == 0) {
                    /** The beginning range. */
                    ranges.add(new int[]{beginIndex, iterator + 1});
                } else {
                    ranges.add(new int[]{beginIndex + 1, iterator + 1});

                    if (iterator == responseSize) {
                        break;
                    }
                }
                beginIndex = iterator;
            }

            iterator++;
        }

        if (responseSize == 0) {
            ranges = Collections.emptyList();
        } else if (ranges == null) {
            ranges = Collections.singletonList(new int[]{beginIndex, responseSize});
        }

        return ranges;
    }

    /**
     * Gets a beginning and an end range of sub-server responses.
     *
     * <p>This method, unlike {@link #getRanges(Collection, String)}, parses the response
     * for the first key being repeated and will split end the range with the prior position.</p>
     *
     * @param response The server response to parse.
     * @return A two int array. The first int is the beginning range which matched the key
     * parameter. The second number is one int beyond the end of the range (for
     * {@link List#subList(int, int)} compatibility). If no range is found, an empty list will
     * be returned.
     * @see #getRanges(Collection, String)
     */
    public static Collection<int[]> getRanges(final Collection<String> response) {
        final int responseSize = response.size();
        /** Initialize the range after the capacity is known. */
        Collection<int[]> ranges = null;
        CharSequence key = null;
        int beginIndex = 0;
        int iterator = 0;

        for (final String line : response) {
            final int index = line.indexOf(':');
            final CharSequence formatted;

            if (index == -1) {
                formatted = line;
            } else {
                formatted = line.subSequence(0, index);
            }

            if (iterator == 0) {
                key = formatted;
            }

            if (key.equals(formatted) && iterator != beginIndex) {
                if (ranges == null) {
                    final int capacity = responseSize / (iterator - beginIndex);
                    ranges = new ArrayList<>(capacity);
                }
                ranges.add(new int[]{beginIndex, iterator});
                beginIndex = iterator;
            }

            iterator++;
        }

        if (responseSize == 0) {
            ranges = Collections.emptyList();
        } else if (ranges == null) {
            ranges = Collections.singletonList(new int[]{beginIndex, responseSize});
        } else {
            ranges.add(new int[]{beginIndex, responseSize});
        }

        return ranges;
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
                    stringBuilder.append(':');
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
                    stringBuilder.append(':');
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
}
