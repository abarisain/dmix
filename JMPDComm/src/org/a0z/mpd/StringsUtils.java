package org.a0z.mpd;


import java.security.MessageDigest;

public class StringsUtils {

    public static String trim(String text) {
        return text == null ? text : text.trim();
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

    /**
     * Convert byte array to hex string.
     *
     * @param data Target data array.
     * @return Hex string.
     */
    private static final String convertToHex(byte[] data) {
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

}
