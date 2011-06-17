package org.pmix.cover;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import com.namelessdev.mpdroid.MPDApplication;

import android.content.SharedPreferences;
import android.util.Log;


public class LocalCover implements ICoverRetriever {

	private final static String URL = "http://%s/%s/%s/%s";

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
				serverName,
				musicPath,
				path.replaceAll(" ", "%20"),
				coverFileName
			});

			return url;
		}else{
			return null;
		}
	}

}
