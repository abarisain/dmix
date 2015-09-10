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
import java.util.Collection;
import java.util.List;

/**
 * Fetch cover from CoverArtArchive by release, using a MusicBrainz ID as an intermediary.
 */
public class CoverArtArchiveCover extends AbstractWebCover {

    /**
     * The URL prefix for CoverArtArchive cover art fetching.
     */
    private static final String COVER_ART_ARCHIVE_URL = "https://coverartarchive.org/release/";

    /**
     * The URI host to query MusicBrainz IDs from the MusicBrainz API.
     */
    private static final String COVER_QUERY_HOST = "musicbrainz.org";

    /**
     * The URI path to query MusicBrainz IDs from the MusicBrainz API.
     */
    private static final String COVER_QUERY_PATH = "/ws/2/release/";

    /**
     * The target JSON key for the MusicBrainz ID values.
     */
    private static final String JSON_KEY_ID = "id";

    /**
     * The target JSON key for the image value.
     */
    private static final String JSON_KEY_IMAGE = "image";

    /**
     * The target JSON key for the images array JSON value.
     */
    private static final String JSON_KEY_IMAGES = "images";

    /**
     * The target JSON key for the releases array JSON value.
     */
    private static final String JSON_KEY_RELEASES = "releases";

    /**
     * The class log identifier.
     */
    private static final String TAG = "CoverArtArchiveCover";

    /**
     * Sole constructor.
     */
    public CoverArtArchiveCover() {
        super();
    }

    /**
     * This is a common method used to parse JSON from MusicBrainz.
     * <BR><BR>
     * The simplified JSON format used to get CoverArtArchive image URLs from the MusicBrainz APIs:
     * <BR><BR>
     * {{@code rootKey} : [{{@code targetKey} : [value to add to the collection]},{...}}
     *
     * @param response  The JSON response to parse.
     * @param url       The URL which the response was retrieved from.
     * @param rootKey   The root JSON key.
     * @param targetKey The array JSON key.
     * @return The resulting value from the {@code targetKey}.
     * @throws JSONException If there was a problem parsing the JSON.
     */
    private static Collection<String> extractFromJson(final String response, final URL url,
            final String rootKey, final String targetKey) throws JSONException {
        final Collection<String> results = new ArrayList<>();
        final JSONObject root = new JSONObject(response);

        if (root.has(rootKey)) {
            final JSONArray array = root.getJSONArray(rootKey);
            final int length = array.length();

            for (int itemCount = 0; itemCount < length; itemCount++) {
                final JSONObject item = array.getJSONObject(itemCount);

                if (item.has(targetKey)) {
                    results.add(item.getString(targetKey));
                } else {
                    logError(TAG, targetKey, root, url);
                }
            }
        } else {
            logError(TAG, rootKey, root, url);
        }

        return results;
    }

    /**
     * This method extracts the AlbumArt Image URLs from the JSON response.
     * <BR><BR>
     * The simplified JSON format used to get CoverArtArchive image URLs from the MusicBrainz APIs:
     * <BR><BR>
     * {{@link #JSON_KEY_IMAGES} : [{{@link #JSON_KEY_IMAGE} : value to add to the
     * collection},{...}]}
     *
     * @param response The JSON response containing AlbumArt Image URLs.
     * @param url      The URL which the response was retrieved from.
     * @return A collection of Image URLs in the JSON response.
     * @throws JSONException If there was a problem parsing the JSON.
     */
    private static Collection<String> extractImageUrls(final String response, final URL url)
            throws JSONException {
        return extractFromJson(response, url, JSON_KEY_IMAGES, JSON_KEY_IMAGE);
    }

    /**
     * This method extracts the MusicBrainz IDs from the JSON response.
     * <BR><BR>
     * The simplified JSON format used to get the release IDs from the MusicBrainz APIs:
     * <BR><BR>
     * {{@link #JSON_KEY_RELEASES} : [{{@link #JSON_KEY_ID} : value to add to the
     * collection},{...}]}
     *
     * @param response The JSON response containing MusicBrainz IDs.
     * @param url      The URL which the response was retrieved from.
     * @return A collection of MusicBrainz IDs.
     * @throws JSONException If there was a problem parsing the JSON.
     */
    private static Iterable<String> extractReleaseIds(final String response, final URL url)
            throws JSONException {
        return extractFromJson(response, url, JSON_KEY_RELEASES, JSON_KEY_ID);
    }

    /**
     * This method returns a URL for a MusicBrainz ID from the MusicBrainz API.
     *
     * @param albumInfo The {@link AlbumInfo} of the album to query.
     * @return A URI encoded URL.
     * @throws URISyntaxException    Upon syntax error.
     * @throws MalformedURLException Upon incorrect input for the URI to ASCII conversion.
     */
    private static URL getCoverQueryURL(final AlbumInfo albumInfo)
            throws URISyntaxException, MalformedURLException {
        final String artistName = encodeQuery(albumInfo.getArtistName());
        final String albumName = encodeQuery(albumInfo.getAlbumName());
        final String query = "query=artist:\"" + artistName + "\" AND release:\""
                + albumName + "\"&fmt=json";

        return encodeUrl(COVER_QUERY_HOST, COVER_QUERY_PATH, query);
    }

    /**
     * This method retrieves the CoverArtArchive image URL(s) for this API.
     *
     * @param albumInfo The {@link AlbumInfo} used to generate the album cover query.
     * @return A {@link String} array of image URLs.
     * @throws JSONException      Thrown upon error parsing the JSON response.
     * @throws URISyntaxException Thrown upon error parsing the generated {@code AlbumInfo} URI.
     * @throws IOException        Thrown upon error retrieving the cover or cover URL.
     */
    @Override
    public List<String> getCoverUrls(final AlbumInfo albumInfo)
            throws JSONException, IOException, URISyntaxException {
        final List<String> coverUrls = new ArrayList<>();
        for (final String release : getReleaseIDs(albumInfo)) {
            final URL request = new URL(COVER_ART_ARCHIVE_URL + release + '/');
            final String response = executeGetRequestIfExists(request);

            if (response != null && !response.isEmpty()) {
                coverUrls.addAll(extractImageUrls(response, request));
            }
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


    /**
     * This method retrieves MusicBrainz release IDs for a specific release.
     *
     * @param albumInfo The album and artist information to get the MusicBrainz ID for.
     * @return A Iterable of release IDs for this album information.
     * @throws JSONException      Thrown upon error parsing the JSON response.
     * @throws URISyntaxException Thrown upon error parsing the generated URI.
     * @throws IOException        Thrown upon communication error retrieving the MusicBrainz IDs.
     */
    private Iterable<String> getReleaseIDs(final AlbumInfo albumInfo)
            throws URISyntaxException, IOException, JSONException {
        final URL query = getCoverQueryURL(albumInfo);
        final String response = executeGetRequest(query);

        return extractReleaseIds(response, query);
    }
}
