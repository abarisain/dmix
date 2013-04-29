package com.namelessdev.mpdroid.cover;

import de.umass.lastfm.Album;
import de.umass.lastfm.ImageSize;

public class LastFMCover implements ICoverRetriever {

	public String[] getCoverUrl(String artist, String album, String path, String filename) throws Exception {
		String key = "7fb78a81b20bee7cb6e8fad4cbcb3694";
		
		Album albumObj = Album.getInfo(artist, album, key);
		if(albumObj == null) {
			return null;
		}else{
			return new String[] { albumObj.getImageURL(ImageSize.MEGA) };
		}
	}

	@Override
	public boolean isCoverLocal() {
		return false;
	}

	@Override
	public String getName() {
		return "LastFM";
	}
}
