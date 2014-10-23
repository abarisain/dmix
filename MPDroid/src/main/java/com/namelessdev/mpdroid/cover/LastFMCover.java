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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

import java.io.StringReader;

/**
 * Fetch cover from LastFM
 */
public class LastFMCover extends AbstractWebCover {

    private static final String TAG = "LastFMCover";

    private static final String URL = "http://ws.audioscrobbler.com/2.0/";

    private static final String sKey = "7fb78a81b20bee7cb6e8fad4cbcb3694";

    @Override
    public String[] getCoverUrl(final AlbumInfo albumInfo) throws Exception {

        final String response;
        final String request;
        String sizeAttribute = null;
        String imageUrl;
        final XmlPullParserFactory factory;
        final XmlPullParser xpp;
        int eventType;

        try {
            request = URL + "?method=album.getInfo&artist=" + albumInfo.getArtist() + "&album="
                    + albumInfo.getAlbum() + "&api_key=" + sKey;
            response = executeGetRequest(request);

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
