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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Fetch cover from MusicBrainz
 */
public class MusicBrainzCover extends AbstractWebCover {

    private static final String COVER_ART_ARCHIVE_URL = "http://coverartarchive.org/release-group/";

    private static final String TAG = "MusicBrainzCover";

    private static Collection<String> extractImageUrls(final String covertArchiveResponse) {
        final JSONObject jsonRootObject;
        final JSONArray jsonArray;
        String coverUrl;
        JSONObject jsonObject;
        final Collection<String> coverUrls = new ArrayList<>();

        if (covertArchiveResponse == null || covertArchiveResponse.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            jsonRootObject = new JSONObject(covertArchiveResponse);
            if (jsonRootObject.has("images")) {

                jsonArray = jsonRootObject.getJSONArray("images");
                for (int i = 0; i < jsonArray.length(); i++) {
                    jsonObject = jsonArray.getJSONObject(i);
                    if (jsonObject.has("image")) {
                        coverUrl = jsonObject.getString("image");
                        if (coverUrl != null) {
                            coverUrls.add(coverUrl);
                        }
                    }
                }
            }
        } catch (final Exception e) {
            Log.e(TAG, "No cover in CovertArchive.", e);
        }

        return coverUrls;

    }

    private static List<String> extractReleaseIds(final String response) {

        final List<String> releaseList = new ArrayList<>();

        try {

            final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            final XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(new StringReader(response));
            int eventType;
            eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("release-group".equals(xpp.getName())) {
                        final String id = xpp.getAttributeValue(null, "id");
                        if (id != null) {
                            releaseList.add(id);
                        }
                    }
                }
                eventType = xpp.next();
            }
        } catch (final Exception e) {
            if (CoverManager.DEBUG) {
                Log.e(TAG, "Musicbrainz release search failure.", e);
            }
        }

        return releaseList;

    }

    private String getCoverArtArchiveResponse(final String mbid) {

        final String request = (COVER_ART_ARCHIVE_URL + mbid + '/');
        return executeGetRequestWithConnection(request);
    }

    @Override
    public String[] getCoverUrl(final AlbumInfo albumInfo) throws Exception {

        final List<String> releases;
        final List<String> coverUrls = new ArrayList<>();
        String covertArtResponse;

        releases = searchForRelease(albumInfo);
        for (final String release : releases) {
            covertArtResponse = getCoverArtArchiveResponse(release);
            if (!covertArtResponse.isEmpty()) {
                coverUrls.addAll(extractImageUrls(covertArtResponse));
            }

            if (!coverUrls.isEmpty()) {
                break;
            }
        }
        return coverUrls.toArray(new String[coverUrls.size()]);

    }

    @Override
    public String getName() {
        return "MUSICBRAINZ";
    }

    private List<String> searchForRelease(final AlbumInfo albumInfo) {

        final String response;

        final String url = "http://musicbrainz.org/ws/2/release-group/?query=" + albumInfo
                .getArtist()
                + ' ' + albumInfo.getAlbum() + "&type=release-group&limit=5";
        response = executeGetRequestWithConnection(url);
        return extractReleaseIds(response);
    }
}
