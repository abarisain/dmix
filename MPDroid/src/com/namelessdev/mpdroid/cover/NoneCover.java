package com.namelessdev.mpdroid.cover;

/**
 * Created with IntelliJ IDEA.
 * User: philippe
 * Date: 11/1/12
 * Time: 8:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class NoneCover implements ICoverRetriever {

    public String getCoverUrl(String artist, String album, String path) throws Exception {
        return "";
    }
}
