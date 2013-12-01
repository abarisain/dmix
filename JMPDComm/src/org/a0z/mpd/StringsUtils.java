package org.a0z.mpd;


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

}
