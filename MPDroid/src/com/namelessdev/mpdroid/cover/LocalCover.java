package com.namelessdev.mpdroid.cover;

import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;

import com.namelessdev.mpdroid.MPDApplication;


public class LocalCover implements ICoverRetriever {

	private final static String URL = "%s/%s/%s";
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

				url = String.format(URL, new Object[] { musicPath, path.replaceAll(" ", "%20"), lfilename });
				url = musicPath.toLowerCase().startsWith(URL_PREFIX) ? url : (URL_PREFIX + serverName + "/" + url);
				while (url.lastIndexOf("//") != -1) {
					url = url.replace("//", "/");
				}
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
