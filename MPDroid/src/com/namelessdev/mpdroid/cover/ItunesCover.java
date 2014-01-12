
package com.namelessdev.mpdroid.cover;

import android.util.Log;

import org.a0z.mpd.AlbumInfo;
import org.json.JSONArray;
import org.json.JSONObject;

public class ItunesCover extends AbstractWebCover {
    @Override
    public String[] getCoverUrl(AlbumInfo albumInfo) throws Exception {
        String response;
        JSONObject jsonRootObject;
        JSONArray jsonArray;
        String coverUrl;
        JSONObject jsonObject;

        try {
            response = executeGetRequest("https://itunes.apple.com/search?term="
                    + albumInfo.getAlbum() + " " + albumInfo.getArtist()
                    + "&limit=5&media=music&entity=album");
            jsonRootObject = new JSONObject(response);
            jsonArray = jsonRootObject.getJSONArray("results");
            for (int i = 0; i < jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                coverUrl = jsonObject.getString("artworkUrl100");
                if (coverUrl != null) {
                    // Based on some tests even if the cover art size returned
                    // is 100x100
                    // Bigger versions also exists.
                    return new String[] {
                        coverUrl.replace("100x100", "600x600")
                    };
                }
            }

        } catch (Exception e) {
            Log.e(ItunesCover.class.toString(), "Failed to get cover URL from " + getName());
        }

        return new String[0];
    }

    @Override
    public String getName() {
        return "ITUNES";
    }
}
