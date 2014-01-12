
package com.namelessdev.mpdroid.tools;

public class StringUtils {

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

    public static String trim(String text) {
        return text == null ? text : text.trim();
    }

}
