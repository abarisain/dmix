/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

/**
 * Fetch cover from Deezer
 */
public class DeezerCover extends AbstractWebCover {

    private static final String TAG = "DeezerCover";

    @Override
    public String[] getCoverUrl(final AlbumInfo albumInfo) throws Exception {

        final String deezerResponse;
        final JSONObject jsonRootObject;
        final JSONArray jsonArray;
        StringBuilder coverUrl = new StringBuilder();
        JSONObject jsonObject;

        try {

            deezerResponse = executeGetRequest("http://api.deezer.com/search/album?q="
                    + albumInfo.getAlbum() + ' ' + albumInfo.getArtist()
                    + "&nb_items=1&output=json");
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
