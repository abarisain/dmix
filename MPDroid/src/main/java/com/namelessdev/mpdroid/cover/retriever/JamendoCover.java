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

import com.namelessdev.mpdroid.helpers.AlbumInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class generates a list of cover image URLs from the
 * <A HREF="https://developer.jamendo.com/v3.0/albums">Jamendo v3 Albums API</a>.
 */
public class JamendoCover extends AbstractWebCover {

    /**
     * This is the unique identifier for this client.
     */
    private static final String CLIENT_ID = "2f2f532d";

    /**
     * The URI host to query cover images from the
     * <A HREF="https://developer.jamendo.com/v3.0/albums">Jamendo v3 Albums API</a>.
     */
    private static final String COVER_QUERY_HOST = "api.jamendo.com";

    /**
     * The URI path to query cover images from the
     * <A HREF="https://developer.jamendo.com/v3.0/albums">Jamendo v3 Albums API</a>.
     */
    private static final String COVER_QUERY_PATH = "/v3.0/albums";

    /**
     * This is the JSON key used to retrieve the {@link #JSON_KEY_RESULTS_COUNT result count}
     * parent.
     */
    private static final String JSON_KEY_HEADERS = "headers";

    /**
     * This is the JSON key used to get the image URL String.
     */
    private static final String JSON_KEY_IMAGE = "image";

    /**
     * This is the JSON key used to get the results value JSONArray.
     */
    private static final String JSON_KEY_RESULTS = "results";

    /**
     * This is the JSON key used to retrieve the number of resulting cover image URLs.
     */
    private static final String JSON_KEY_RESULTS_COUNT = "results_count";

    /**
     * The class log identifier.
     */
    private static final String TAG = "JamendoCover";

    /**
     * The name for this class.
     */
    public static final String NAME = TAG;

    public JamendoCover() {
    }

    /**
     * This method is used to get the cover count from the results.
     *
     * @param root     The root JSON results.
     * @param queryURL The URL the results JSON was retrieved by.
     * @return The number of covers offered by this response.
     * @throws JSONException Thrown upon error parsing the JSON response.
     */
    private static int getCoverCount(final JSONObject root, final URL queryURL)
            throws JSONException {
        int coverCount = 0;

        if (root.has(JSON_KEY_HEADERS)) {
            final JSONObject headers = root.getJSONObject(JSON_KEY_HEADERS);

            if (headers.has(JSON_KEY_RESULTS_COUNT)) {
                coverCount = headers.getInt(JSON_KEY_RESULTS_COUNT);
            } else {
                logError(TAG, JSON_KEY_RESULTS_COUNT, headers, queryURL);
            }
        } else {
            logError(TAG, JSON_KEY_HEADERS, root, queryURL);
        }

        return coverCount;
    }

    /**
     * This method returns a URL for a cover query for the
     * <A HREF="https://developer.jamendo.com/v3.0/albums">Jamendo v3 Albums API</a>.
     *
     * @param albumInfo The {@link AlbumInfo} of the album to query.
     * @return A URI encoded URL.
     * @throws URISyntaxException    Upon syntax error.
     * @throws MalformedURLException Upon incorrect input for the URI to ASCII conversion.
     */
    private static URL getCoverQueryURL(final AlbumInfo albumInfo)
            throws URISyntaxException, MalformedURLException {
        final String artist = encodeQuery(albumInfo.getArtistName());
        final String album = encodeQuery(albumInfo.getAlbumName());
        final String query = "client_id=" + CLIENT_ID + "&name=" + album
                + "&artist_name=" + artist + "&imagesize=600";

        return encodeUrl(HTTPS_SCHEME, COVER_QUERY_HOST, COVER_QUERY_PATH, query);
    }

    /**
     * This method retrieves the cover URL(s) for this API.
     * <BR><BR>
     * The simplified JSON format used to retrieve the image URLs from the <A
     * HREF="https://developer.jamendo.com/v3.0/albums">Jamendo v3 Albums API</a>.
     *
     * {{@link #JSON_KEY_RESULTS}:[{{@link #JSON_KEY_IMAGE}} : {"imageURL"}]}
     *
     * @param albumInfo The {@link AlbumInfo} used to generate the album cover query.
     * @return A {@link String} array of image URLs.
     * @throws JSONException      Thrown upon error parsing the JSON response.
     * @throws URISyntaxException Thrown upon error parsing the generated {@code AlbumInfo} URI.
     * @throws IOException        Thrown upon error retrieving the cover or cover URL.
     */
    @Override
    public String[] getCoverUrl(final AlbumInfo albumInfo) throws JSONException,
            URISyntaxException, IOException {
        final URL query = getCoverQueryURL(albumInfo);
        final JSONObject root = new JSONObject(executeGetRequest(query));
        final int coverCount = getCoverCount(root, query);
        final List<String> coverUrls;

        if (coverCount > 0) {
            coverUrls = new ArrayList<>(coverCount);
            if (root.has(JSON_KEY_RESULTS)) {
                final JSONArray results = root.getJSONArray(JSON_KEY_RESULTS);

                for (int resultCount = 0; resultCount < results.length(); resultCount++) {
                    final JSONObject result = results.getJSONObject(resultCount);

                    if (result.has(JSON_KEY_IMAGE)) {
                        final String imageURL = result.getString(JSON_KEY_IMAGE);
                        coverUrls.add(imageURL);
                    } else {
                        logError(TAG, JSON_KEY_IMAGE, result, query);
                    }
                }
            } else {
                logError(TAG, JSON_KEY_RESULTS, root, query);
            }
        } else {
            coverUrls = Collections.emptyList();
        }

        return coverUrls.toArray(new String[coverUrls.size()]);
    }

    /**
     * The name of this class.
     *
     * @return The name of this class.
     */
    @Override
    public String getName() {
        return NAME;
    }
}
