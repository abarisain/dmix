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

package org.a0z.mpd;

import org.a0z.mpd.exception.InvalidResponseException;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

public final class Tools {

    public static final int KEY = 0;

    public static final int VALUE = 1;

    private static final String TAG = "Tools";

    private Tools() {
        super();
    }

    /**
     * Simple integer comparison. The result
     * should be equivalent to Object.compare().
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
     * Convert byte array to hex string.
     *
     * @param data Target data array.
     * @return Hex string.
     */
    private static String convertToHex(final byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }

        final StringBuilder buffer = new StringBuilder(data.length);
        for (int byteIndex = 0; byteIndex < data.length; byteIndex++) {
            int halfbyte = (data[byteIndex] >>> 4) & 0x0F;
            int twoHalves = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buffer.append((char) ('0' + halfbyte));
                } else {
                    buffer.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[byteIndex] & 0x0F;
            } while (twoHalves++ < 1);
        }

        return buffer.toString();
    }

    /**
     * Null-safe equivalent of {@code a.equals(b)}. The result
     * should be equivalent to Object.equals().
     */
    public static boolean equals(final Object a, final Object b) {
        if (a == null) {
            return b == null;
        } else {
            return a.equals(b);
        }
    }

    public static String getExtension(final String path) {
        final int index = path.lastIndexOf('.');
        final int extLength = path.length() - index - 1;
        final int extensionShort = 2;
        final int extensionLong = 4;
        String result = null;

        if (extLength >= extensionShort && extLength <= extensionLong) {
            result = path.substring(index + 1);
        }

        return result;
    }

    /**
     * Gets the hash value from the specified string.
     *
     * @param value Target string value to get hash from.
     * @return the hash from string.
     */
    public static String getHashFromString(final String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            final MessageDigest hashEngine = MessageDigest.getInstance("MD5");
            hashEngine.update(value.getBytes("iso-8859-1"), 0, value.length());
            return convertToHex(hashEngine.digest());
        } catch (final Exception e) {
            return null;
        }
    }

    /**
     * Gets a beginning and an end range of to a server response.
     *
     * @param response The server response to parse.
     * @param key      The key to the beginning/end of a sub-list.
     * @return A two int array. The first int is the beginning range which matched the key
     * parameter. The second number is one int beyond the end of the range (List.subList()
     * compatible). If no range is found, an empty list will be returned.
     */
    public static Collection<int[]> getRanges(final Collection<String> response, final String key) {
        /** Initialize the range after the capacity is known. */
        Collection<int[]> ranges = null;
        int iterator = 0;
        int beginIndex = 0;

        for (final String line : response) {
            if (key.equals(line.substring(0, line.indexOf(':'))) && iterator != beginIndex) {
                if (ranges == null) {
                    final int capacity = response.size() / (iterator - beginIndex);
                    ranges = new ArrayList<>(capacity);
                }
                ranges.add(new int[]{beginIndex, iterator});
                beginIndex = iterator;
            }

            iterator++;
        }

        if (ranges == null) {
            if (beginIndex == iterator) {
                ranges = Collections.emptyList();
            } else {
                ranges = Collections.singletonList(new int[]{beginIndex, response.size()});
            }
        } else {
            ranges.add(new int[]{beginIndex, response.size()});
        }

        return ranges;
    }

    /**
     * This method iterates through a 3 dimensional array to check each two element inner array
     * for equality of it's inner objects with the isNotEqual(object, object) method.
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
     * Parse a media server response for specific key values and discard the key.
     *
     * @param response The media server response.
     * @param keys     The entry type in the response to add to the list.
     */
    public static void parseResponse(final List<String> response, final String... keys) {
        String[] lines;

        if (keys.length > 1) {
            Arrays.sort(keys);
        }

        for (final ListIterator<String> iterator = response.listIterator(); iterator.hasNext(); ) {
            lines = splitResponse(iterator.next());

            if (keys.length == 1 && keys[0].equals(lines[KEY]) ||
                    Arrays.binarySearch(keys, lines[KEY]) >= 0) {
                iterator.set(lines[VALUE]);
            } else {
                iterator.remove();
            }
        }

        if (response instanceof ArrayList) {
            ((ArrayList<String>) response).trimToSize();
        }
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
    public static List<String> sequentialToRange(final List<Integer> integers) {
        final ListIterator<Integer> iterator = integers.listIterator(integers.size());
        final List<String> ranges = new ArrayList<>();
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
     * @see #sequentialToRange(java.util.List)
     */
    public static List<String> sequentialToRange(final int... integers) {
        final List<String> ranges = new ArrayList<>();
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
     * Split the standard MPD protocol response into a three dimensional array consisting of a
     * two element String array key / value pairs.
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
     * This method takes seconds and converts it into HH:MM:SS
     *
     * @param totalSeconds Seconds to convert to a string.
     * @return Returns time formatted from the {@code totalSeconds} in format HH:MM:SS.
     */
    public static String timeToString(final long totalSeconds) {
        final long hours = TimeUnit.SECONDS.toHours(totalSeconds);
        final long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) -
                TimeUnit.SECONDS.toHours(totalSeconds) * 60L;
        final long seconds = TimeUnit.SECONDS.toSeconds(totalSeconds) -
                TimeUnit.SECONDS.toMinutes(totalSeconds) * 60L;
        final String result;

        if (hours == 0) {
            result = String.format("%02d:%02d", minutes, seconds);
        } else {
            result = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }

        return result;
    }
}
