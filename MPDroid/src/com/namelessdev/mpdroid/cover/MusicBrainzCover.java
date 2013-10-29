package com.namelessdev.mpdroid.cover;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fetch cover from MusicBrainz
 */
public class MusicBrainzCover extends AbstractWebCover {

    private static final String COVER_ART_ARCHIVE_URL = "http://coverartarchive.org/release/";

    public String[] getCoverUrl(String artist, String album, String path, String filename) throws Exception {

        List<String> releases;
        List<String> coverUrls = new ArrayList<>();
        String covertArtResponse;

        releases = searchForRelease(artist, album);
        for (String release : releases) {
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

    private List<String> searchForRelease(String artist, String album) {

        String response;

        artist = cleanGetRequest(artist);
        album = cleanGetRequest(album);

        String url = "http://musicbrainz.org/ws/2/release/?query=" + artist + " " + album + "&type=release&limit=5";
        url = url.replace("!", "");
        response = executeGetRequest(url);
        return extractReleaseIds(response);
    }

    private List<String> extractReleaseIds(String response) {

        List<String> releaseList = new ArrayList<>();

        try {

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(new StringReader(response));
            int eventType;
            eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equals("release")) {
                        String id = xpp.getAttributeValue(null, "id");
                        if (id != null) {
                            releaseList.add(id);
                        }
                    }
                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            Log.e(MusicBrainzCover.class.getName(), "Musicbrainz release search failure : " + e);
        }

        return releaseList;

    }

    private String getCoverArtArchiveResponse(String mbid) {

        String request = (COVER_ART_ARCHIVE_URL + mbid);
        return executeGetRequest(request);
    }


    private List<String> extractImageUrls(String covertArchiveResponse) {
        JSONObject jsonRootObject;
        JSONArray jsonArray;
        String coverUrl;
        JSONObject jsonObject;
        List<String> coverUrls = new ArrayList<>();

        if (covertArchiveResponse == null || covertArchiveResponse.isEmpty()) {
            return Collections.EMPTY_LIST;
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
        } catch (Exception e) {
            Log.e(MusicBrainzCover.class.getName(), "No cover in CovertArchive : " + e);
        }

        return coverUrls;

    }

    @Override
    public String getName() {
        return "MUSICBRAINZ";
    }
}
