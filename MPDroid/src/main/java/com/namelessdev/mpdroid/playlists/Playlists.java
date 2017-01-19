/*
 * Copyright (C) 2010-2017 The MPDroid Project
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

package com.namelessdev.mpdroid.playlists;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Playlists {

    private static final String TAG = "Playlists";

    private static final String M3U_EXTENSION = "M3U";

    private static final String PLS_EXTENSION = "PLS";

    interface PlaylistEntryExtractor {
        List<PlaylistEntry> extractEntries(BufferedReader reader) throws IOException;
    }

    public static Playlist create(final String url) {
        if (url == null) {
            return null;
        } else if (url.toUpperCase().endsWith(M3U_EXTENSION)) {
            return new M3UPlaylist(url);
        } else if (url.toUpperCase().endsWith(PLS_EXTENSION)) {
            return new PLSPlaylist(url);
        } else {
            return null;
        }
    }

    static List<PlaylistEntry> extractEntries(final String url,
                                              final PlaylistEntryExtractor extractor) {
        try {
            return new AsyncTask<Void, Void, List<PlaylistEntry>>() {
                @Override
                protected List<PlaylistEntry> doInBackground(final Void... ignore) {
                    try {
                        return loadEntries(url, extractor);
                    } catch (final IOException e) {
                        Log.e(TAG, "Failed to load playlist.", e);
                        return Collections.emptyList();
                    }
                }
            }.execute().get();
        } catch (final InterruptedException | ExecutionException e) {
            Log.e(TAG, "Failed to load playlist.", e);
            return Collections.emptyList();
        }
    }

    private static List<PlaylistEntry> loadEntries(final String url,
                                                   final PlaylistEntryExtractor extractor)
            throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            return extractor.extractEntries(reader);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException ignore) {
                }
            }
        }
    }

}
