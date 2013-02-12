package com.namelessdev.mpdroid.cover;

import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;

import com.namelessdev.mpdroid.MPDApplication;


public class LocalCover implements ICoverRetriever {

	private final static String URL = "%s/%s/%s";
	private final static String URL_PREFIX = "http://";
	private final static String[] FILENAMES = new String[] { "%placeholder_custom", "%placeholder_filename", "cover.jpg", "folder.jpg",
			"front.jpg", "cover.png", "front.png", "folder.png", };

	private MPDApplication app = null;
	private SharedPreferences settings = null;

	public LocalCover(MPDApplication app, SharedPreferences settings) {
		this.app = app;
		this.settings = settings;
	}

	public String[] getCoverUrl(String artist, String album, String path) throws Exception {
		// load URL parts from settings
		String musicPath = settings.getString("musicPath", "music/");
		FILENAMES[0] = settings.getString("coverFileName", null);

		if (musicPath != null) {
			// load server name/ip
			final String serverName = app.oMPDAsyncHelper.getConnectionSettings().sServer;

			String url;
			final List<String> urls = new ArrayList<String>();
			for (String filename : FILENAMES) {
				if (filename == null || filename.startsWith("%"))
					continue;
				url = String.format(URL, new Object[] { musicPath, path.replaceAll(" ", "%20"), filename });
				url = musicPath.toLowerCase().startsWith(URL_PREFIX) ? url : (URL_PREFIX + serverName + "/" + url);
				if (!urls.contains(url))
					urls.add(url);
			}
			return (String[]) urls.toArray();
		} else {
			return null;
		}
	}

	@Override
	public boolean isCoverLocal() {
		return false;
	}

	@Override
	public String getName() {
		return "User's HTTP Server";
	}

}
