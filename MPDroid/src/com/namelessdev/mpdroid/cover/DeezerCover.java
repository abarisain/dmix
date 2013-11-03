package com.namelessdev.mpdroid.cover;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Fetch cover from Deezer
 */
public class DeezerCover extends AbstractWebCover {

    @Override
    public String[] getCoverUrl(String artist, String album, String path, String filename) throws Exception {

        String deezerResponse;
        JSONObject jsonRootObject;
        JSONArray jsonArray;
        String coverUrl;
        JSONObject jsonObject;

        try {
            deezerResponse = executeGetRequest("http://api.deezer.com/search/album?q=" + album + " " + artist + "&nb_items=1&output=json");
            jsonRootObject = new JSONObject(deezerResponse);
            jsonArray = jsonRootObject.getJSONArray("data");
            for (int i = 0; i < jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                coverUrl = jsonObject.getString("cover");
                if (coverUrl != null) {
                    coverUrl += "&size=big";
                    return new String[]{coverUrl};
                }
            }

        } catch (Exception e) {
            Log.e(DeezerCover.class.toString(), "Failed to get cover URL from Deeze");
        }

        return new String[0];
    }

    @Override
    public String getName() {
        return "DEEZER";
    }
}
