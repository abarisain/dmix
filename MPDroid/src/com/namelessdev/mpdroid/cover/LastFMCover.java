package com.namelessdev.mpdroid.cover;

import de.umass.lastfm.Album;
import de.umass.lastfm.ImageSize;

public class LastFMCover implements ICoverRetriever {

	public String getCoverUrl(String artist, String album, String path) throws Exception {
		String key = "7fb78a81b20bee7cb6e8fad4cbcb3694";
		
		Album albumObj = Album.getInfo(artist, album, key);
		return albumObj.getImageURL(ImageSize.LARGE);
	}
}
