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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Fetch cover from LastFM
 */
public class LastFMCover extends AbstractWebCover {

    /**
     * The URI host to query covers from the LastFM API.
     */
    private static final String COVER_QUERY_HOST = "ws.audioscrobbler.com";

    /**
     * The URI path to query covers from the LastFM API.
     */
    private static final String COVER_QUERY_PATH = "/2.0/";

    /**
     * The key used to retrieve the covers for this API.
     */
    private static final String KEY = "7fb78a81b20bee7cb6e8fad4cbcb3694";

    /**
     * The class log identifier.
     */
    private static final String TAG = "LastFMCover";

    /**
     * This method returns a URL for cover query for the LastFM Cover API.
     *
     * @param albumInfo The {@link AlbumInfo} of the album to query.
     * @return A {@link URI} encoded URL.
     * @throws URISyntaxException Upon syntax error.
     */
    private static String getCoverQueryURL(final AlbumInfo albumInfo) throws URISyntaxException {
        final String artist = encodeQuery(albumInfo.getArtistName());
        final String album = encodeQuery(albumInfo.getAlbumName());
        final String query = "method=album.getInfo&artist=" + artist + "&album=" + album +
                "&api_key=" + KEY;

        return encodeUrl(HTTPS_SCHEME, COVER_QUERY_HOST, COVER_QUERY_PATH, query);
    }

    @Override
    public String[] getCoverUrl(final AlbumInfo albumInfo) throws Exception {
        final String response;
        String sizeAttribute = null;
        String imageUrl;
        final XmlPullParserFactory factory;
        final XmlPullParser xpp;
        final String queryURL = getCoverQueryURL(albumInfo);
        int eventType;

        try {
            response = executeGetRequest(queryURL);

            factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            xpp = factory.newPullParser();

            xpp.setInput(new StringReader(response));
            eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("image".equals(xpp.getName())) {
                        sizeAttribute = xpp.getAttributeValue(null, "size");
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    if ("mega".equals(sizeAttribute)) {
                        imageUrl = xpp.getText();
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            return new String[]{
                                    imageUrl
                            };
                        }
                    }
                }
                eventType = xpp.next();
            }
        } catch (final Exception e) {
            if (CoverManager.DEBUG) {
                Log.e(TAG, "Failed to get cover URL from LastFM.", e);
            }
        }

        return new String[0];
    }

    @Override
    public String getName() {
        return "LastFM";
    }
}
