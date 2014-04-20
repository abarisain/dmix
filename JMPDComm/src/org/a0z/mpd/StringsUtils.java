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

import java.security.MessageDigest;

public class StringsUtils {

    /**
     * Convert byte array to hex string.
     * 
     * @param data Target data array.
     * @return Hex string.
     */
    private static String convertToHex(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }

        final StringBuffer buffer = new StringBuffer();
        for (int byteIndex = 0; byteIndex < data.length; byteIndex++) {
            int halfbyte = (data[byteIndex] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buffer.append((char) ('0' + halfbyte));
                else
                    buffer.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[byteIndex] & 0x0F;
            } while (two_halfs++ < 1);
        }

        return buffer.toString();
    }

    public static String getExtension(String path) {
        String[] split = path.split("\\.");
        if (split.length > 1) {
            String ext = split[split.length - 1];
            if (ext.length() <= 4) {
                return ext;
            }
        }
        return "";
    }

    /**
     * Gets the hash value from the specified string.
     * 
     * @param value Target string value to get hash from.
     * @return the hash from string.
     */
    public static final String getHashFromString(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }

        try {
            MessageDigest hashEngine = MessageDigest.getInstance("MD5");
            hashEngine.update(value.getBytes("iso-8859-1"), 0, value.length());
            return convertToHex(hashEngine.digest());
        } catch (Exception e) {
            return null;
        }
    }

    public static String trim(String text) {
        return text == null ? text : text.trim();
    }

}
