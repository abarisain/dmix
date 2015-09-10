/*
 * Copyright (C) 2010-2015 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.cover.retriever;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.helpers.AlbumInfo;

import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.text.TextUtils.isEmpty;

public class LocalCover implements ICoverRetriever {

    private static final String TAG = "LocalCover";

    private static final String[] EXT = {
            "jpg", "png", "jpeg",
    };

    private static final String PLACEHOLDER_FILENAME = "%placeholder_filename";

    // Note that having two PLACEHOLDER_FILENAME is on purpose
    private static final String[] FILENAMES = {
            "%placeholder_custom", PLACEHOLDER_FILENAME, "AlbumArt", "cover", "folder", "front",
    };

    private static final String[] SUB_FOLDERS = {
            "", "artwork", "Covers"
    };

    // private final static String URL = "%s/%s/%s";
    private static final String URL_PREFIX = "http://";

    private final MPDApplication mApp = MPDApplication.getInstance();

    private final SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(mApp);

    public static void appendPathString(final Uri.Builder builder, final String baseString) {
        if (baseString != null && !baseString.isEmpty()) {
            final String[] components = baseString.split("/");
            for (final String component : components) {
                builder.appendPath(component);
            }
        }
    }

    public static String buildCoverUrl(String serverName, String musicPath, final String path,
            final String fileName) {

        if (musicPath.startsWith(URL_PREFIX)) {
            int hostPortEnd = musicPath.indexOf(URL_PREFIX.length(), '/');
            if (hostPortEnd == -1) {
                hostPortEnd = musicPath.length();
            }
            serverName = musicPath.substring(URL_PREFIX.length(), hostPortEnd);
            musicPath = musicPath.substring(hostPortEnd);
        }
        final Uri.Builder uriBuilder = Uri.parse(URL_PREFIX + serverName).buildUpon();
        appendPathString(uriBuilder, musicPath);
        appendPathString(uriBuilder, path);
        appendPathString(uriBuilder, fileName);

        final Uri uri = uriBuilder.build();
        return uri.toString();
    }

    @Override
    public List<String> getCoverUrls(final AlbumInfo albumInfo) throws Exception {
        final List<String> coverUrls;

        if (isEmpty(albumInfo.getParentDirectory())) {
            coverUrls = Collections.emptyList();
        } else {
            String lfilename;
            // load URL parts from settings
            final String musicPath = mSettings.getString("musicPath", "music/");
            FILENAMES[0] = mSettings.getString("coverFileName", null);
            final String serverName = mApp.getConnectionSettings().server;

            String url;
            coverUrls = new ArrayList<>();
            for (final String subfolder : SUB_FOLDERS) {
                for (String baseFilename : FILENAMES) {
                    for (final String ext : EXT) {

                        if (baseFilename == null
                                || (baseFilename.startsWith("%") && !baseFilename
                                .equals(PLACEHOLDER_FILENAME))) {
                            continue;
                        }
                        if (baseFilename.equals(PLACEHOLDER_FILENAME)
                                && albumInfo.getFilename() != null) {
                            final int dotIndex = albumInfo.getFilename().lastIndexOf('.');
                            if (dotIndex == -1) {
                                continue;
                            }
                            baseFilename = albumInfo.getFilename().substring(0, dotIndex);
                        }

                        // Add file extension except for the filename coming
                        // from settings
                        if (baseFilename.equals(FILENAMES[0])) {
                            lfilename = baseFilename;
                        } else {
                            lfilename = subfolder + '/' + baseFilename + '.' + ext;
                        }

                        url = buildCoverUrl(serverName, musicPath,
                                albumInfo.getParentDirectory(),
                                lfilename);

                        if (!coverUrls.contains(url)) {
                            coverUrls.add(url);
                        }
                    }
                }
            }
        }

        return coverUrls;
    }


    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public boolean isCoverLocal() {
        return false;
    }

}
