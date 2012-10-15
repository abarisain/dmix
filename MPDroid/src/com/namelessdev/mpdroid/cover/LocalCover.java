package com.namelessdev.mpdroid.cover;

import android.content.SharedPreferences;

import com.namelessdev.mpdroid.MPDApplication;


public class LocalCover implements ICoverRetriever {

	private final static String URL = "%s/%s/%s";
	private final static String URL_PREFIX = "http://";

	private MPDApplication app = null;
	private SharedPreferences settings = null;

	public LocalCover(MPDApplication app, SharedPreferences settings) {
		this.app = app;
		this.settings = settings;
	}

	public String getCoverUrl(String artist, String album, String path) throws Exception {
		// load URL parts from settings
		String musicPath = settings.getString("musicPath", null);
		String coverFileName = settings.getString("coverFileName", null);

		if(musicPath != null && coverFileName != null) {
			// load server name/ip
			String serverName = app.oMPDAsyncHelper.getConnectionSettings().sServer;

			String url = String.format(URL, new Object[] {
				musicPath,
				path.replaceAll(" ", "%20"),
				coverFileName
			});

			return (musicPath.toLowerCase().startsWith(URL_PREFIX) ? url : (URL_PREFIX + serverName + "/"));
		}else{
			return null;
		}
	}

}
