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

package com.namelessdev.mpdroid.cover;

import com.namelessdev.mpdroid.helpers.AlbumInfo;
import com.namelessdev.mpdroid.helpers.CoverManager;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public class ItunesCover extends AbstractWebCover {

    /**
     * The URI host to query covers from the iTunes Search API.
     */
    private static final String COVER_QUERY_HOST = "itunes.apple.com";

    /**
     * The URI path to query covers from the iTunes Search API.
     */
    private static final String COVER_QUERY_PATH = "/search";

    private static final Pattern SMALL_IMAGE_ID = Pattern.compile("100x100", Pattern.LITERAL);

    private static final String TAG = "ItunesCover";

    /**
     * This method returns a URL for cover query for the iTunes Search API.
     *
     * @param albumInfo The {@link AlbumInfo} of the album to query.
     * @return A {@link URI} encoded URL.
     * @throws URISyntaxException Upon syntax error.
     */
    private static String getCoverQueryURL(final AlbumInfo albumInfo) throws URISyntaxException {
        final String artist = encodeQuery(albumInfo.getArtistName());
        final String album = encodeQuery(albumInfo.getAlbumName());
        final String query = "term=" + album + ' ' + artist + "&limit=5&media=music&entity=album";

        return encodeUrl(HTTPS_SCHEME, COVER_QUERY_HOST, COVER_QUERY_PATH, query);
    }

    @Override
    public String[] getCoverUrl(final AlbumInfo albumInfo) throws Exception {
        final JSONObject jsonRootObject;
        final JSONArray jsonArray;
        final String response;
        final String queryURL = getCoverQueryURL(albumInfo);
        String coverUrl;
        JSONObject jsonObject;

        try {
            response = executeGetRequest(queryURL);
            jsonRootObject = new JSONObject(response);
            jsonArray = jsonRootObject.getJSONArray("results");
            for (int i = 0; i < jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                coverUrl = jsonObject.getString("artworkUrl100");
                if (coverUrl != null) {
                    // Based on some tests even if the cover art size returned
                    // is 100x100
                    // Bigger versions also exists.
                    return new String[]{
                            SMALL_IMAGE_ID.matcher(coverUrl).replaceAll("600x600")
                    };
                }
            }

        } catch (final Exception e) {
            if (CoverManager.DEBUG) {
                Log.e(TAG, "Failed to get cover URL from " + getName(), e);
            }
        }

        return new String[0];
    }

    @Override
    public String getName() {
        return "ITUNES";
    }
}
