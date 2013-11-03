package com.namelessdev.mpdroid.tools;


public class StringUtils {

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static String trim(String text) {
        return text == null ? text : text.trim();
    }

}
