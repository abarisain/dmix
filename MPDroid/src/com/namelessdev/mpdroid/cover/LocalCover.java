package com.namelessdev.mpdroid.cover;

import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;
import android.net.Uri;

import com.namelessdev.mpdroid.MPDApplication;


public class LocalCover implements ICoverRetriever {

//	private final static String URL = "%s/%s/%s";
	private final static String URL_PREFIX = "http://";
	private final static String PLACEHOLDER_FILENAME = "%placeholder_filename";
	// Note that having two PLACEHOLDER_FILENAME is on purpose
	private final static String[] FILENAMES = new String[] { "%placeholder_custom", PLACEHOLDER_FILENAME, PLACEHOLDER_FILENAME,
			"cover.jpg", "folder.jpg", "front.jpg", "cover.png", "front.png", "folder.png", };

	private MPDApplication app = null;
	private SharedPreferences settings = null;

	public LocalCover(MPDApplication app, SharedPreferences settings) {
		this.app = app;
		this.settings = settings;
	}
	
	static public String buildCoverUrl(String serverName, String musicPath, String path, String fileName){

		if (musicPath.startsWith(URL_PREFIX)) {
			int hostPortEnd = musicPath.indexOf(URL_PREFIX.length(), '/');
			if (hostPortEnd == -1) {
				hostPortEnd = musicPath.length();
			}
			serverName=musicPath.substring(URL_PREFIX.length(), hostPortEnd);
			musicPath=musicPath.substring(hostPortEnd);
		}
		Uri.Builder b = Uri.parse(URL_PREFIX + serverName).buildUpon();
		if (null != musicPath && musicPath.length() > 0) {
			b.appendPath(musicPath);
		}
		if (null != path && path.length() > 0) {
			b.appendPath(path);
		}
		if (null != fileName && fileName.length() > 0) {
			b.appendPath(fileName);
		}
		Uri uri = b.build();
		return uri.toString();
	}	

	public String[] getCoverUrl(String artist, String album, String path, String filename) throws Exception {
		// load URL parts from settings
		String musicPath = settings.getString("musicPath", "music/");
		FILENAMES[0] = settings.getString("coverFileName", null);
	
		if (musicPath != null) {
			// load server name/ip
			final String serverName = app.oMPDAsyncHelper.getConnectionSettings().sServer;

			String url;
			final List<String> urls = new ArrayList<String>();
			boolean secondFilenamePlaceholder = false;
			for (String lfilename : FILENAMES) {
				if (lfilename == null || (lfilename.startsWith("%") && !lfilename.equals(PLACEHOLDER_FILENAME)))
					continue;
				if (lfilename.equals(PLACEHOLDER_FILENAME)) {
					final int dotIndex = filename.lastIndexOf('.');
					if (dotIndex == -1)
						continue;
					lfilename = filename.substring(0, dotIndex) + (secondFilenamePlaceholder ? ".png" : ".jpg");
					secondFilenamePlaceholder = true;
				}

				url = buildCoverUrl(serverName, musicPath, path, lfilename);
				
				if (!urls.contains(url))
					urls.add(url);
			}
			return urls.toArray(new String[urls.size()]);
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
