package com.namelessdev.mpdroid.cover;

import android.content.SharedPreferences;
import android.net.Uri;
import com.namelessdev.mpdroid.MPDApplication;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;


public class LocalCover implements ICoverRetriever {

    //	private final static String URL = "%s/%s/%s";
    private final static String URL_PREFIX = "http://";
    private final static String PLACEHOLDER_FILENAME = "%placeholder_filename";
    // Note that having two PLACEHOLDER_FILENAME is on purpose
    private final static String[] FILENAMES = new String[]{"%placeholder_custom", PLACEHOLDER_FILENAME,
            "cover", "folder", "front", "cover", "front", "folder",};
    private final static String[] EXT = new String[]{"jpg", "png", "jpeg",};
    private final static String[] SUB_FOLDERS = new String[]{"", "artwork", "Covers"};

    private MPDApplication app = null;
    private SharedPreferences settings = null;

    public LocalCover(MPDApplication app, SharedPreferences settings) {
        this.app = app;
        this.settings = settings;
    }

    static public String buildCoverUrl(String serverName, String musicPath, String path, String fileName) {

        if (musicPath.startsWith(URL_PREFIX)) {
            int hostPortEnd = musicPath.indexOf(URL_PREFIX.length(), '/');
            if (hostPortEnd == -1) {
                hostPortEnd = musicPath.length();
            }
            serverName = musicPath.substring(URL_PREFIX.length(), hostPortEnd);
            musicPath = musicPath.substring(hostPortEnd);
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
        String decodedUrl;
        try {
            decodedUrl = URLDecoder.decode(uri.toString(), "UTF-8");
            return decodedUrl;
        } catch (UnsupportedEncodingException e) {
            return uri.toString();
        }
    }

    public String[] getCoverUrl(String artist, String album, String path, String filename) throws Exception {
        String lfilename;
        // load URL parts from settings
        String musicPath = settings.getString("musicPath", "music/");
        FILENAMES[0] = settings.getString("coverFileName", null);

        if (musicPath != null) {
            // load server name/ip
            final String serverName = app.oMPDAsyncHelper.getConnectionSettings().sServer;

            String url;
            final List<String> urls = new ArrayList<String>();
            for (String subfolder : SUB_FOLDERS) {
                for (String baseFilename : FILENAMES) {
                    for (String ext : EXT) {

                        if (baseFilename == null || (baseFilename.startsWith("%") && !baseFilename.equals(PLACEHOLDER_FILENAME)))
                            continue;
                        if (baseFilename.equals(PLACEHOLDER_FILENAME) && filename != null) {
                            final int dotIndex = filename.lastIndexOf('.');
                            if (dotIndex == -1)
                                continue;
                            baseFilename = filename.substring(0, dotIndex);
                        }

                        // Add file extension except for the filename coming from settings
                        if (!baseFilename.equals(FILENAMES[0])) {
                            lfilename = subfolder + "/" + baseFilename + "." + ext;
                        } else {
                            lfilename = baseFilename;
                        }

                        url = buildCoverUrl(serverName, musicPath, path, lfilename);

                        if (!urls.contains(url))
                            urls.add(url);
                    }
                }
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
