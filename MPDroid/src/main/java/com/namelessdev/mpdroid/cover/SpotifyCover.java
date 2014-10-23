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
import java.util.List;

import static android.text.TextUtils.isEmpty;

/**
 * Fetch cover from Spotify
 */
public class SpotifyCover extends AbstractWebCover {

    private static final String TAG = "SpotifyCover";

    private static List<String> extractAlbumIds(final String response) {
        final JSONObject jsonRoot;
        final JSONArray jsonAlbums;
        JSONObject jsonAlbum;
        String albumId;
        final List<String> albumIds = new ArrayList<>();

        try {
            jsonRoot = new JSONObject(response);
            jsonAlbums = jsonRoot.optJSONArray("albums");
            if (jsonAlbums != null) {
                for (int a = 0; a < jsonAlbums.length(); a++) {
                    jsonAlbum = jsonAlbums.optJSONObject(a);
                    if (jsonAlbum != null) {
                        albumId = jsonAlbum.optString("href");
                        if (albumId != null && !albumId.isEmpty()) {
                            albumId = albumId.replace("spotify:album:", "");
                            albumIds.add(albumId);
                        }
                    }
                }
            }
        } catch (final Exception e) {
            if (CoverManager.DEBUG) {
                Log.e(TAG, "Failed to get cover URL from Spotify.", e);
            }
        }

        return albumIds;
    }

    private static String extractImageUrl(final String response) {

        final JSONObject jsonAlbum;
        final String imageUrl;

        try {
            jsonAlbum = new JSONObject(response);
            imageUrl = jsonAlbum.optString("thumbnail_url");
            return imageUrl;
        } catch (final Exception e)

        {
            Log.e(TAG, "Failed to get cover URL from Spotify.", e);
        }

        return null;
    }

    @Override
    public String[] getCoverUrl(final AlbumInfo albumInfo) throws Exception {

        final String albumResponse;
        final List<String> albumIds;
        String coverResponse;
        String coverUrl;

        try {
            albumResponse = executeGetRequest("http://ws.spotify.com/search/1/album.json?q="
                    + albumInfo.getArtist() + ' ' + albumInfo.getAlbum());
            albumIds = extractAlbumIds(albumResponse);
            for (final String albumId : albumIds) {
                coverResponse = executeGetRequest(
                        "https://embed.spotify.com/oembed/?url=http://open.spotify.com/album/"
                                + albumId);
                coverUrl = extractImageUrl(coverResponse);
                if (!isEmpty(coverUrl)) {
                    coverUrl = coverUrl.replace("/cover/", "/640/");
                    return new String[]{
                            coverUrl
                    };
                }
            }

        } catch (final Exception e) {
            Log.e(TAG, "Failed to get cover URL from Spotify.", e);
        }

        return new String[0];
    }

    @Override
    public String getName() {
        return "SPOTIFY";
    }
}
