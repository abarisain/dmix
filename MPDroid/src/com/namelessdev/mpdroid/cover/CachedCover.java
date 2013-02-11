package com.namelessdev.mpdroid.cover;

public class CachedCover implements ICoverRetriever {

	@Override
	public String[] getCoverUrl(String artist, String album, String path) throws Exception {
		// TODO : Implement a configurable local cover art provider
		return null;
	}

	@Override
	public boolean isCoverLocal() {
		return true;
	}

	@Override
	public String getName() {
		return "SD Card Cache";
	}

}
