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

import static android.text.TextUtils.isEmpty;
import static android.util.Log.e;
import static android.util.Log.w;

import android.content.SharedPreferences;

import org.a0z.mpd.AlbumInfo;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;

public class GracenoteCover extends AbstractWebCover {

    private String clientId;
    public static final String USER_ID = "GRACENOTE_USERID";
    public static final String CUSTOM_CLIENT_ID_KEY = "gracenoteClientId";

    public static boolean isClientIdAvailable(SharedPreferences sharedPreferences) {
        return !isEmpty(sharedPreferences.getString(CUSTOM_CLIENT_ID_KEY, null));
    }

    private String apiUrl;
    private SharedPreferences sharedPreferences;
    private String userId;

    public static final String URL_PREFIX = "web.content.cddbp.net";

    public GracenoteCover(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    private String extractCoverUrl(String response) {

        String coverUrl;
        String elementName = null;
        String attribute = null;

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(new StringReader(response));
            int eventType;
            eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.TEXT) {
                    if (elementName != null && elementName.equals("URL")) {
                        if (attribute != null && attribute.equals("COVERART")) {
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
        } catch (Exception e) {
            e(GracenoteCover.class.getName(),
                    "Cannot extract coverArt URL from Gracenote response : " + e);
        }
        return null;
    }

    private String extractUserID(String response) {

        String userId;
        String elementName = null;

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(new StringReader(response));
            int eventType;
            eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.TEXT) {
                    if (elementName != null && elementName.equals("USER")) {
                        userId = xpp.getText();
                        return userId;
                    }
                } else if (eventType == XmlPullParser.START_TAG) {
                    elementName = xpp.getName();
                }
                eventType = xpp.next();
            }
        } catch (Exception e)

        {
            e(GracenoteCover.class.getName(), "Cannot extract userID from Gracenote response : "
                    + e);
        }

        return null;
    }

    private String getClientIdPrefix() {
        String[] splittedString;
        splittedString = clientId.split("-");
        if (splittedString.length == 2) {
            return splittedString[0];
        } else {
            w(GracenoteCover.class.getSimpleName(),
                    "Invalid GraceNote User ID (must be XXXX-XXXXXXXXXXXXX) : " + clientId);
            return "";
        }
    }

    @Override
    public String[] getCoverUrl(AlbumInfo albumInfo) throws Exception {
        String coverUrl;

        if (userId == null) {
            initializeUserId();
        }

        if (userId == null) {
            return new String[0];
        }
        try {
            coverUrl = getCoverUrl(albumInfo.getArtist(), albumInfo.getAlbum());
            if (coverUrl != null) {
                return new String[] {
                        coverUrl
                };
            }
        } catch (Exception ex) {
            e(GracenoteCover.class.getName(), "GracenoteCover fetch failure : " + ex);
        }
        return new String[0];
    }

    public String getCoverUrl(String artist, String album) {
        // Make sure user doesn't try to register again if they already have a
        // userID in the ctor.
        // Do the register request
        String request = "<QUERIES>\n" +
                "  <LANG>eng</LANG>\n" +
                "  <AUTH>\n" +
                "    <CLIENT>" + clientId + "</CLIENT>\n" +
                "    <USER>" + userId + "</USER>\n" +
                "  </AUTH>\n" +
                "  <QUERY CMD=\"ALBUM_SEARCH\">\n" +
                "    <MODE>SINGLE_BEST_COVER</MODE>\n" +
                "    <TEXT TYPE=\"ARTIST\">" + artist + "</TEXT>\n" +
                "    <TEXT TYPE=\"ALBUM_TITLE\">" + album + "</TEXT>\n" +
                "    <OPTION><PARAMETER>COVER_SIZE</PARAMETER><VALUE>LARGE</VALUE></OPTION>\n" +
                "  </QUERY>\n" +
                "</QUERIES>";

        String response = this.executePostRequest(apiUrl, request);
        return extractCoverUrl(response);
    }

    @Override
    public String getName() {
        return "Gracenote";
    }

    private void initializeUserId() {
        try {

            if (sharedPreferences == null) {
                return;
            }

            String customClientId = sharedPreferences.getString(CUSTOM_CLIENT_ID_KEY, null);
            if (!isEmpty(customClientId)) {
                clientId = customClientId;
                apiUrl = "https://c" + getClientIdPrefix() + ".web.cddbp.net/webapi/xml/1.0/";
            } else {
                return;
            }
            userId = sharedPreferences.getString(USER_ID, null);

            if (userId == null) {
                userId = register();
                if (sharedPreferences != null && userId != null) {
                    SharedPreferences.Editor editor;
                    editor = sharedPreferences.edit();
                    editor.putString(USER_ID, userId);
                    editor.commit();
                }
            }
        } catch (Exception e) {
            e(GracenoteCover.class.getName(),
                    "Gracenote initialisation failure : " + e.getMessage());
            if (sharedPreferences != null) {
                if (sharedPreferences != null && userId != null) {
                    SharedPreferences.Editor editor;
                    editor = sharedPreferences.edit();
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
        return this.register(clientId);
    }

    public String register(String clientID) {
        String request = "<QUERIES><QUERY CMD=\"REGISTER\"><CLIENT>" + clientID
                + "</CLIENT></QUERY></QUERIES>";
        String response = this.executePostRequest(apiUrl, request);
        return extractUserID(response);
    }
}
