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

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.helpers.AlbumInfo;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.StringReader;

import static android.text.TextUtils.isEmpty;

public class GracenoteCover extends AbstractWebCover {

    public static final String CUSTOM_CLIENT_ID_KEY = "gracenoteClientId";

    public static final String URL_PREFIX = "web.content.cddbp.net";

    public static final String USER_ID = "GRACENOTE_USERID";

    private static final SharedPreferences SETTINGS =
            PreferenceManager.getDefaultSharedPreferences(MPDApplication.getInstance());

    private static final String TAG = "GracenoteCover";

    private String mApiUrl;

    private String mClientId;

    private String mUserId;

    private static String extractCoverUrl(final String response) {

        final String coverUrl;
        String elementName = null;
        String attribute = null;

        try {
            final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            final XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(new StringReader(response));
            int eventType;
            eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.TEXT) {
                    if ("URL".equals(elementName)) {
                        if ("COVERART".equals(attribute)) {
                            coverUrl = xpp.getText();
                            return coverUrl;
                        }
                    }
                } else if (eventType == XmlPullParser.START_TAG) {
                    elementName = xpp.getName();
                    attribute = xpp.getAttributeValue(null, "TYPE");
                }
                eventType = xpp.next();
            }
        } catch (final Exception e) {
            Log.e(TAG, "Cannot extract coverArt URL from Gracenote response.", e);
        }
        return null;
    }

    private static String extractUserID(final String response) {

        final String userId;
        String elementName = null;

        try {
            final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            final XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(new StringReader(response));
            int eventType;
            eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.TEXT) {
                    if ("USER".equals(elementName)) {
                        userId = xpp.getText();
                        return userId;
                    }
                } else if (eventType == XmlPullParser.START_TAG) {
                    elementName = xpp.getName();
                }
                eventType = xpp.next();
            }
        } catch (final Exception e)

        {
            Log.e(TAG, "Cannot extract userID from Gracenote response.", e);
        }

        return null;
    }

    public static boolean isClientIdAvailable() {
        return !SETTINGS.getString(CUSTOM_CLIENT_ID_KEY, null).isEmpty();
    }

    private String getClientIdPrefix() {
        final String[] splittedString;
        splittedString = mClientId.split("-");
        if (splittedString.length == 2) {
            return splittedString[0];
        } else {
            Log.w(TAG, "Invalid GraceNote User ID (must be XXXX-XXXXXXXXXXXXX) : " + mClientId);
            return "";
        }
    }

    @Override
    public String[] getCoverUrl(final AlbumInfo albumInfo) throws Exception {
        final String coverUrl;

        if (mUserId == null) {
            initializeUserId();
        }

        if (mUserId == null) {
            return new String[0];
        }
        try {
            coverUrl = getCoverUrl(albumInfo.getArtist(), albumInfo.getAlbum());
            if (coverUrl != null) {
                return new String[]{
                        coverUrl
                };
            }
        } catch (final Exception ex) {
            Log.e(TAG, "GracenoteCover fetch failure.", ex);
        }
        return new String[0];
    }

    public String getCoverUrl(final String artist, final String album) {
        // Make sure user doesn't try to register again if they already have a
        // userID in the ctor.
        // Do the register request
        final String request = "<QUERIES>\n" +
                "  <LANG>eng</LANG>\n" +
                "  <AUTH>\n" +
                "    <CLIENT>" + mClientId + "</CLIENT>\n" +
                "    <USER>" + mUserId + "</USER>\n" +
                "  </AUTH>\n" +
                "  <QUERY CMD=\"ALBUM_SEARCH\">\n" +
                "    <MODE>SINGLE_BEST_COVER</MODE>\n" +
                "    <TEXT TYPE=\"ARTIST\">" + artist + "</TEXT>\n" +
                "    <TEXT TYPE=\"ALBUM_TITLE\">" + album + "</TEXT>\n" +
                "    <OPTION><PARAMETER>COVER_SIZE</PARAMETER><VALUE>LARGE</VALUE></OPTION>\n" +
                "  </QUERY>\n" +
                "</QUERIES>";

        final String response = executePostRequest(mApiUrl, request);
        return extractCoverUrl(response);
    }

    @Override
    public String getName() {
        return "Gracenote";
    }

    private void initializeUserId() {
        try {

            if (SETTINGS == null) {
                return;
            }

            final String customClientId = SETTINGS.getString(CUSTOM_CLIENT_ID_KEY, null);
            if (!isEmpty(customClientId)) {
                mClientId = customClientId;
                mApiUrl = "https://c" + getClientIdPrefix() + ".web.cddbp.net/webapi/xml/1.0/";
            } else {
                return;
            }
            mUserId = SETTINGS.getString(USER_ID, null);

            if (mUserId == null) {
                mUserId = register();
                if (SETTINGS != null && mUserId != null) {
                    final SharedPreferences.Editor editor;
                    editor = SETTINGS.edit();
                    editor.putString(USER_ID, mUserId);
                    editor.commit();
                }
            }
        } catch (final Exception e) {
            Log.e(TAG, "Gracenote initialization failure.", e);
            if (SETTINGS != null) {
                if (SETTINGS != null && mUserId != null) {
                    final SharedPreferences.Editor editor;
                    editor = SETTINGS.edit();
                    editor.remove(USER_ID);
                    editor.commit();
                }
            }
        }

    }

    // Will register your clientID and Tag in order to get a userID. The userID
    // should be stored
    // in a persistent form (filesystem, db, etc) otherwise you will hit your
    // user limit.
    public String register() {
        return register(mClientId);
    }

    public String register(final String clientID) {
        final String request = "<QUERIES><QUERY CMD=\"REGISTER\"><CLIENT>" + clientID
                + "</CLIENT></QUERY></QUERIES>";
        final String response = executePostRequest(mApiUrl, request);
        return extractUserID(response);
    }
}
