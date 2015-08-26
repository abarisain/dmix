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

import com.namelessdev.mpdroid.cover.CoverManager;
import com.namelessdev.mpdroid.helpers.AlbumInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

import java.net.URISyntaxException;

/**
 * Fetch cover from Deezer
 */
public class DeezerCover extends AbstractWebCover {

    /**
     * The URI host to query covers from the Deezer API.
     */
    private static final String COVER_QUERY_HOST = "api.deezer.com";

    /**
     * The URI path to query covers from the Deezer API.
     */
    private static final String COVER_QUERY_PATH = "/search/album";

    /**
     * The class log identifier.
     */
    private static final String TAG = "DeezerCover";

    /**
     * This method returns a URL for a cover query for the Deezer API.
     *
     * @param albumInfo The {@link AlbumInfo} of the album to query.
     * @return A URI encoded URL.
     * @throws URISyntaxException Upon syntax error.
     */
    private static String getCoverQueryURL(final AlbumInfo albumInfo) throws URISyntaxException {
        final String album = encodeQuery(albumInfo.getAlbumName());
        final String artist = encodeQuery(albumInfo.getArtistName());
        final String query = "q=" + album + ' ' + artist + "&nb_items=1&output=json";

        return encodeUrl(HTTP_SCHEME, COVER_QUERY_HOST, COVER_QUERY_PATH, query);
    }

    @Override
    public String[] getCoverUrl(final AlbumInfo albumInfo) throws Exception {
        final JSONObject jsonRootObject;
        final JSONArray jsonArray;
        StringBuilder coverUrl = new StringBuilder();
        final String queryURL = getCoverQueryURL(albumInfo);
        final String deezerResponse;
        JSONObject jsonObject;

        try {

            deezerResponse = executeGetRequest(queryURL);
            jsonRootObject = new JSONObject(deezerResponse);
            jsonArray = jsonRootObject.getJSONArray("data");
            for (int i = 0; i < jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                coverUrl.setLength(0);
                coverUrl.append(jsonObject.getString("cover"));
                if (coverUrl.length() != 0) {
                    coverUrl.append("&size=big");
                    return new String[]{
                            coverUrl.toString()
                    };
                }
            }

        } catch (final Exception e) {
            if (CoverManager.DEBUG) {
                Log.e(TAG, "Failed to get cover URL from Deezer", e);
            }
        }

        return new String[0];
    }

    @Override
    public String getName() {
        return "DEEZER";
    }
}
