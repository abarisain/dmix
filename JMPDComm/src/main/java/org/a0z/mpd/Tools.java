/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2014 The MPDroid Project
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class Tools {

    public static final int KEY = 0;

    public static final int VALUE = 1;

    private Tools() {
        super();
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

    /**
     * Parse a media server response for one entry type.
     *
     * @param response The media server response.
     * @param type     The entry type in the response to add to the collection.
     * @return A collection of entries of one type in a media server response.
     */
    public static List<String> parseResponse(final Collection<String> response, final String type) {
        final List<String> result = new ArrayList<>(response.size());

        for (final String[] lines : splitResponse(response)) {
            if (lines[KEY].equals(type)) {
                result.add(lines[VALUE]);
            }
        }

        return result;
    }

    /**
     * Parse a media server response for one entry type, then sort the resulting list.
     *
     * @param response        The media server response.
     * @param substring       The entry type in the response to add to the collection.
     * @param sortInsensitive Whether to sort insensitively.
     * @return A sorted collection of entries of one type in a media server response.
     */
    public static List<String> parseResponse(final Collection<String> response,
            final String substring, final boolean sortInsensitive) {
        final List<String> result = parseResponse(response, substring);

        if (sortInsensitive) {
            Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        } else {
            Collections.sort(result);
        }

        return result;
    }

    /**
     * Split the standard MPD protocol response into a three dimensional array consisting of a
     * two element String array key / value pairs.
     *
     * @param list The incoming server response.
     * @return A three dimensional {@code String} array of two element {@code String arrays}.
     */
    public static String[][] splitResponse(final String... list) {
        final String[][] results = new String[list.length][];
        int iterator = 0;

        for (final String line : list) {
            results[iterator] = splitResponse(line);
            iterator++;
        }

        return results;
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
}
