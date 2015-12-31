/*
 * Copyright (C) 2010-2016 The MPDroid Project
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

import android.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class generates a list of cover image URLs from the
 * <A HREF="https://developer.spotify.com/web-api">Spotify Web API</A>.
 */
public class SpotifyCover extends AbstractWebCover {

    /**
     * This matches a colon character, used for removal from the query.
     */
    private static final Pattern COLON_PATTERN = Pattern.compile(":", Pattern.LITERAL);

    /**
     * The URI host to query AlbumIDs from the
     * <A HREF="https://developer.spotify.com/web-api">Spotify Web API</A>.
     */
    private static final String COVER_QUERY_HOST = "api.spotify.com";

    /**
     * The URI path to query AlbumIDs from the
     * <A HREF="https://developer.spotify.com/web-api">Spotify Web API</A>.
     */
    private static final String COVER_QUERY_PATH = "/v1/search";

    /**
     * This is the JSON key used to retrieve the albums JSON value response.
     */
    private static final String JSON_KEY_ALBUMS = "albums";

    /**
     * This is the JSON key used to retrieve the images JSON value response.
     */
    private static final String JSON_KEY_IMAGES = "images";

    /**
     * This is the JSON key used to retrieve the items JSON value response.
     */
    private static final String JSON_KEY_ITEMS = "items";

    /**
     * This is the JSON key used to get the image URL value.
     */
    private static final String JSON_KEY_URL = "url";

    /**
     * The class log identifier.
     */
    private static final String TAG = "SpotifyCover";

    /**
     * The default constructor.
     */
    public SpotifyCover() {
        super();
    }

    /**
     * This method returns a URL for a cover query for the
     * <A HREF="https://developer.spotify.com/web-api">Spotify Web API</A>.
     *
     * @param albumInfo The {@link AlbumInfo} of the album to query.
     * @return A URI encoded URL.
     * @throws URISyntaxException    Upon syntax error.
     * @throws MalformedURLException Upon incorrect input for the URI to ASCII conversion.
     */
    private static URL getCoverQueryURL(final AlbumInfo albumInfo)
            throws URISyntaxException, MalformedURLException {
        // Spotify album query is not compatible with colons, as they're part of the API.
        final String album = encodeQuery(albumInfo.getAlbumName());
        final String coverAlbumName = COLON_PATTERN.matcher(album).replaceAll("");

        final String artist = encodeQuery(albumInfo.getArtistName());
        final String artistAlbumName = COLON_PATTERN.matcher(artist).replaceAll("");

        final String query = "q=album:" + coverAlbumName + " artist:" + artistAlbumName +
                "&type=album";

        return encodeUrl(COVER_QUERY_HOST, COVER_QUERY_PATH, query);
    }

    /**
     * This method returns the image URL retrieved from an {@link #JSON_KEY_IMAGES} element.
     *
     * @param images The {@link #JSON_KEY_IMAGES} JSON.
     * @param query  The query URL to retrieved for these images.
     * @return The first image URL for this {@link #JSON_KEY_IMAGES}, null if there is no first
     * URL.
     * @throws JSONException Upon JSON parsing error.
     */
    private static String getImageURLFromJSON(final JSONArray images, final URL query)
            throws JSONException {
        String url = null;

        /**
         * We only care about the first image, it will be the highest quality.
         */
        if (images.isNull(0)) {
            Log.e(TAG, "JSON key: " + JSON_KEY_IMAGES + " has no images");
        } else {
            final JSONObject image = images.getJSONObject(0);

            if (image.has(JSON_KEY_URL)) {
                url = image.getString(JSON_KEY_URL);
            } else {
                logError(TAG, JSON_KEY_URL, image, query);
            }
        }

        return url;
    }

    /**
     * This method retrieves the cover URL(s) for this API.
     * <BR><BR>
     * The simplified JSON format used to get the image URLs from the
     * <A HREF="https://developer.spotify.com/web-api">Spotify Web API</A>:
     * <BR><BR>
     * {{@link #JSON_KEY_ALBUMS} : {{@link #JSON_KEY_ITEMS} : [{{@link #JSON_KEY_IMAGES}
     * : [{{@link #JSON_KEY_URL} : "{imageURL}",}]}],},}
     *
     * @param albumInfo The {@link AlbumInfo} used to generate the album cover query.
     * @return A {@link String} array of image URLs.
     * @throws JSONException      Thrown upon error parsing the JSON response.
     * @throws URISyntaxException Thrown upon error parsing the generated {@code AlbumInfo} URI.
     * @throws IOException        Thrown upon error retrieving the cover or cover URL.
     */
    @Override
    public List<String> getCoverUrls(final AlbumInfo albumInfo)
            throws JSONException, URISyntaxException, IOException {
        final List<String> coverUrls = new ArrayList<>();
        final URL query = getCoverQueryURL(albumInfo);
        final JSONObject root = new JSONObject(executeGetRequest(query));

        if (root.has(JSON_KEY_ALBUMS)) {
            final JSONObject albums = root.getJSONObject(JSON_KEY_ALBUMS);

            if (albums.has(JSON_KEY_ITEMS)) {
                final JSONArray items = albums.getJSONArray(JSON_KEY_ITEMS);

                for (int itemCount = 0; itemCount < items.length(); itemCount++) {
                    final JSONObject item = items.getJSONObject(itemCount);

                    if (item.has(JSON_KEY_IMAGES)) {
                        final JSONArray images = item.getJSONArray(JSON_KEY_IMAGES);
                        final String imageUrl = getImageURLFromJSON(images, query);

                        if (imageUrl != null) {
                            coverUrls.add(imageUrl);
                        }
                    } else {
                        logError(TAG, JSON_KEY_IMAGES, item, query);
                    }
                }
            } else {
                logError(TAG, JSON_KEY_ITEMS, albums, query);
            }
        } else {
            logError(TAG, JSON_KEY_ALBUMS, root, query);
        }

        return coverUrls;
    }

    /**
     * The name of this class.
     *
     * @return The name of this class.
     */
    @Override
    public String getName() {
        return TAG;
    }
}
