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

import static android.text.TextUtils.isEmpty;

import android.util.Log;

import org.a0z.mpd.AlbumInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Fetch cover from Spotify
 */
public class SpotifyCover extends AbstractWebCover {

    private List<String> extractAlbumIds(String response) {
        JSONObject jsonRoot;
        JSONArray jsonAlbums;
        JSONObject jsonAlbum;
        String albumId;
        List<String> albumIds = new ArrayList<String>();

        try {
            jsonRoot = new JSONObject(response);
            jsonAlbums = jsonRoot.optJSONArray("albums");
            if (jsonAlbums != null) {
                for (int a = 0; a < jsonAlbums.length(); a++) {
                    jsonAlbum = jsonAlbums.optJSONObject(a);
                    if (jsonAlbum != null) {
                        albumId = jsonAlbum.optString("href");
                        if (albumId != null && albumId.length() > 0) {
                            albumId = albumId.replace("spotify:album:", "");
                            albumIds.add(albumId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(SpotifyCover.class.toString(), "Failed to get cover URL from Spotify");
        }

        return albumIds;
    }

    private String extractImageUrl(String response) {

        JSONObject jsonAlbum;
        String imageUrl;

        try {
            jsonAlbum = new JSONObject(response);
            imageUrl = jsonAlbum.optString("thumbnail_url");
            return imageUrl;
        } catch (Exception e)

        {
            Log.e(SpotifyCover.class.toString(), "Failed to get cover URL from Spotify");
        }

        return null;
    }

    @Override
    public String[] getCoverUrl(AlbumInfo albumInfo) throws Exception {

        String albumResponse;
        List<String> albumIds;
        String coverResponse;
        String coverUrl;

        try {
            albumResponse = executeGetRequest("http://ws.spotify.com/search/1/album.json?q="
                    + albumInfo.getArtist() + " " + albumInfo.getAlbum());
            albumIds = extractAlbumIds(albumResponse);
            for (String albumId : albumIds) {
                coverResponse = executeGetRequest("https://embed.spotify.com/oembed/?url=http://open.spotify.com/album/"
                        + albumId);
                coverUrl = extractImageUrl(coverResponse);
                if (!isEmpty(coverUrl)) {
                    coverUrl = coverUrl.replace("/cover/", "/640/");
                    return new String[] {
                            coverUrl
                    };
                }
            }

        } catch (Exception e) {
            Log.e(SpotifyCover.class.toString(), "Failed to get cover URL from Spotify");
        }

        return new String[0];
    }

    @Override
    public String getName() {
        return "SPOTIFY";
    }
}
