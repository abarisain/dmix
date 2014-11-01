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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Fetch cover from Discogs
 */
public class DiscogsCover extends AbstractWebCover {

    private static final String TAG = "DiscogsCover";

    private static Collection<String> extractImageUrls(final String releaseJson) {
        final JSONObject jsonRootObject;
        final JSONArray jsonArray;
        String imageUrl;
        JSONObject jsonObject;
        final Collection<String> imageUrls = new ArrayList<>();

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

        } catch (final Exception e) {
            if (CoverManager.DEBUG) {
                Log.e(TAG, "Failed to get release image URLs from Discogs.", e);
            }
        }
        return imageUrls;
    }

    private static List<String> extractReleaseIds(final String releaseIdJson) {
        final JSONObject jsonRootObject;
        final JSONArray jsonArray;
        String releaseId;
        JSONObject jsonObject;
        final List<String> releaseIds = new ArrayList<>();

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

        } catch (final Exception e) {
            if (CoverManager.DEBUG) {
                Log.e(TAG, "Failed to get release Ids from Discogs.", e);
            }
        }
        return releaseIds;
    }

    @Override
    public String[] getCoverUrl(final AlbumInfo albumInfo) throws Exception {

        final String releaseIdResponse;
        final List<String> releaseIds;
        final List<String> imageUrls = new ArrayList<>();
        String releaseResponse;

        releaseIdResponse = executeGetRequest(
                "http://api.discogs.com/database/search?type=release&q="
                        + albumInfo.getArtist() + ' ' + albumInfo.getAlbum() + "& per_page = 10");
        releaseIds = extractReleaseIds(releaseIdResponse);
        for (final String releaseId : releaseIds) {
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
