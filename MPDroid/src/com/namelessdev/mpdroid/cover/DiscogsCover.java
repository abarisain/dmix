/*
 * Copyright 2014 Arnaud Barisain Monrose (The MPDroid Project)
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

import android.util.Log;

import org.a0z.mpd.AlbumInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Fetch cover from Discogs
 */
public class DiscogsCover extends AbstractWebCover {

    private List<String> extractImageUrls(String releaseJson) {
        JSONObject jsonRootObject;
        JSONArray jsonArray;
        String imageUrl;
        JSONObject jsonObject;
        List<String> imageUrls = new ArrayList<String>();

        try {
            jsonRootObject = new JSONObject(releaseJson);
            if (jsonRootObject.has("images")) {
                jsonArray = jsonRootObject.getJSONArray("images");
                for (int i = 0; i < jsonArray.length(); i++) {
                    jsonObject = jsonArray.getJSONObject(i);
                    if (jsonObject.has("resource_url")) {
                        imageUrl = jsonObject.getString("resource_url");
                        imageUrls.add(imageUrl);
                    }
                }
            }

        } catch (Exception e) {
            Log.e(DeezerCover.class.toString(), "Failed to get release image URLs from Discogs : "
                    + e);
        }
        return imageUrls;
    }

    private List<String> extractReleaseIds(String releaseIdJson) {
        JSONObject jsonRootObject;
        JSONArray jsonArray;
        String releaseId;
        JSONObject jsonObject;
        List<String> releaseIds = new ArrayList<String>();

        try {
            jsonRootObject = new JSONObject(releaseIdJson);
            if (jsonRootObject.has("results")) {
                jsonArray = jsonRootObject.getJSONArray("results");
                for (int i = 0; i < jsonArray.length(); i++) {
                    jsonObject = jsonArray.getJSONObject(i);
                    if (jsonObject.has("id")) {
                        releaseId = jsonObject.getString("id");
                        releaseIds.add(releaseId);
                    }
                }
            }

        } catch (Exception e) {
            Log.e(DeezerCover.class.toString(), "Failed to get release Ids from Discogs : " + e);
        }
        return releaseIds;
    }

    @Override
    public String[] getCoverUrl(AlbumInfo albumInfo) throws Exception {

        String releaseIdResponse;
        List<String> releaseIds;
        List<String> imageUrls = new ArrayList<String>();
        String releaseResponse;

        releaseIdResponse = executeGetRequest("http://api.discogs.com/database/search?type=release&q="
                + albumInfo.getArtist() + " " + albumInfo.getAlbum() + "& per_page = 10");
        releaseIds = extractReleaseIds(releaseIdResponse);
        for (String releaseId : releaseIds) {
            releaseResponse = executeGetRequest("http://api.discogs.com/releases/" + releaseId);
            imageUrls.addAll(extractImageUrls(releaseResponse));

            // Exit if some covers have been found to save some time
            if (!imageUrls.isEmpty()) {
                break;
            }
        }

        return imageUrls.toArray(new String[imageUrls.size()]);

    }

    @Override
    public String getName() {
        return "DISCOGS";
    }
}
