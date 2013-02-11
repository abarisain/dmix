package com.namelessdev.mpdroid.cover;

public interface ICoverRetriever {

	public String[] getCoverUrl(String artist, String album, String path) throws Exception;

	public boolean isCoverLocal();
}
