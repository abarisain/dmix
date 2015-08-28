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

import android.support.annotation.IntDef;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

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
     * This is a comparator int used to show equal values passed as parameters.
     */
    private static final int EQUAL = 0;

    /**
     * This is a comparator int used to show that the first parameter is greater than the second
     * parameter to a specific method.
     */
    private static final int GREATER_THAN = 1;

    /**
     * This is the extra large defined image size in a image response.
     */
    private static final String IMAGE_SIZE_EXTRA_LARGE = "extralarge";

    /**
     * This is the large defined image size in a image response.
     */
    private static final String IMAGE_SIZE_LARGE = "large";

    /**
     * This is the medium defined image size in a image response.
     */
    private static final String IMAGE_SIZE_MEDIUM = "medium";

    /**
     * This is the largest defined image size in a image response.
     */
    private static final String IMAGE_SIZE_MEGA = "mega";

    /**
     * This is the small defined image size in a image response.
     */
    private static final String IMAGE_SIZE_SMALL = "small";

    /**
     * This is the JSON key used to retrieve the albums JSON value response.
     */
    private static final String JSON_KEY_ALBUM = "album";

    /**
     * This is the JSON key used to retrieve the image JSON value response.
     */
    private static final String JSON_KEY_IMAGE = "image";

    /**
     * This is the JSON key used to retrieve the image size from the image JSON array value.
     */
    private static final String JSON_KEY_IMAGE_URL = "#text";

    /**
     * This is the JSON key used to retrieve the image size JSON value response.
     */
    private static final String JSON_KEY_SIZE = "size";

    /**
     * The key used to retrieve the covers for this API.
     */
    private static final String KEY = "7fb78a81b20bee7cb6e8fad4cbcb3694";

    /**
     * This is a comparator int used to show that the first parameter is less than the second
     * parameter to a specific method.
     */
    private static final int LESS_THAN = -1;

    /**
     * The class log identifier.
     */
    private static final String TAG = "LastFMCover";

    /**
     * The name for this class.
     */
    public static final String NAME = TAG;

    /**
     * The default constructor.
     */
    public LastFMCover() {
        super();
    }

    /**
     * This method is used to compare a value against an extra large image size.
     *
     * @param sizeB The value to compare.
     * @return {@link #EQUAL} if equal, {@link #LESS_THAN} if smaller or {@link #GREATER_THAN} if
     * larger than sizeB to a extra large image size.
     */
    @ComparatorInt
    private static int comparatorExtraLarge(final String sizeB) {
        final int result;

        switch (sizeB) {
            case IMAGE_SIZE_EXTRA_LARGE:
                result = EQUAL;
                break;
            case IMAGE_SIZE_MEGA:
                result = LESS_THAN;
                break;
            default:
                result = GREATER_THAN;
        }

        return result;
    }

    /**
     * This method is used to compare a value against an large image size.
     *
     * @param sizeB The value to compare.
     * @return {@link #EQUAL} if equal, {@link #LESS_THAN} if smaller or {@link #GREATER_THAN} if
     * larger than sizeB to a large image size.
     */
    @ComparatorInt
    private static int comparatorLarge(final String sizeB) {
        final int result;

        switch (sizeB) {
            case IMAGE_SIZE_LARGE:
                result = EQUAL;
                break;
            case IMAGE_SIZE_MEGA:
            case IMAGE_SIZE_EXTRA_LARGE:
                result = LESS_THAN;
                break;
            default:
                result = GREATER_THAN;
                break;
        }

        return result;
    }

    /**
     * This method is used to compare a value against an medium image size.
     *
     * @param sizeB The value to compare.
     * @return {@link #EQUAL} if equal, {@link #LESS_THAN} if smaller or {@link #GREATER_THAN} if
     * larger than sizeB to a medium image size.
     */
    @ComparatorInt
    private static int comparatorMedium(final String sizeB) {
        final int result;

        switch (sizeB) {
            case IMAGE_SIZE_MEGA:
            case IMAGE_SIZE_EXTRA_LARGE:
            case IMAGE_SIZE_LARGE:
                result = LESS_THAN;
                break;
            case IMAGE_SIZE_MEDIUM:
                result = EQUAL;
                break;
            default:
                result = GREATER_THAN;
                break;
        }

        return result;
    }

    /**
     * This method is used to compare a value against an mega image size.
     *
     * @param sizeB The value to compare.
     * @return {@link #EQUAL} if equal, {@link #LESS_THAN} if smaller or {@link #GREATER_THAN} if
     * larger than sizeB to a mega image size.
     */
    @ComparatorInt
    private static int comparatorMega(final String sizeB) {
        final int result;

        switch (sizeB) {
            case IMAGE_SIZE_MEGA:
                result = EQUAL;
                break;
            default:
                result = LESS_THAN;
                break;
        }

        return result;
    }

    /**
     * This method is used to compare a value against an small image size.
     *
     * @param sizeB The value to compare.
     * @return {@link #EQUAL} if equal, {@link #LESS_THAN} if smaller or {@link #GREATER_THAN} if
     * larger than sizeB to a small image size.
     */
    @ComparatorInt
    private static int comparatorSmall(final String sizeB) {
        final int result;

        switch (sizeB) {
            case IMAGE_SIZE_SMALL:
                result = EQUAL;
                break;
            default:
                result = LESS_THAN;
                break;
        }

        return result;
    }

    /**
     * This method returns a URL for cover query for the LastFM Cover API.
     *
     * @param albumInfo The {@link AlbumInfo} of the album to query.
     * @return A {@link URI} encoded URL.
     * @throws URISyntaxException    Upon syntax error.
     * @throws MalformedURLException Upon incorrect input for the URI to ASCII conversion.
     */
    private static URL getCoverQueryURL(final AlbumInfo albumInfo)
            throws URISyntaxException, MalformedURLException {
        final String artist = encodeQuery(albumInfo.getArtistName());
        final String album = encodeQuery(albumInfo.getAlbumName());
        final String query = "method=album.getInfo&artist=" + artist + "&album=" + album +
                "&api_key=" + KEY + "&format=json";

        return encodeUrl(HTTPS_SCHEME, COVER_QUERY_HOST, COVER_QUERY_PATH, query);
    }

    /**
     * This method parses the {@link #JSON_KEY_IMAGE} JSON key value and siblings for an image.
     *
     * @param jsonKeyImage The {@link #JSON_KEY_IMAGE} JSONObject key value.
     * @param query        The query URL used to get the the JSON response.
     * @return The largest image URL available, null if one is not found.
     * @throws JSONException If there is an error parsing the JSON response.
     */
    private static String getImageUrl(final JSONArray jsonKeyImage, final URL query)
            throws JSONException {
        String currentSize = null;
        String imageUrl = null;

        for (int imageCount = 0; imageCount < jsonKeyImage.length(); imageCount++) {
            final JSONObject image = jsonKeyImage.getJSONObject(imageCount);

            if (image.has(JSON_KEY_SIZE)) {
                final String imageSize = image.getString(JSON_KEY_SIZE);
                final int sizeComparator = sizeComparator(imageSize, currentSize);

                if (sizeComparator == GREATER_THAN) {
                    if (image.has(JSON_KEY_IMAGE_URL)) {
                        final String checkUrl = image.getString(JSON_KEY_IMAGE_URL);

                        if (!checkUrl.isEmpty()) {
                            imageUrl = checkUrl;
                            currentSize = imageSize;
                        }
                    } else {
                        logError(TAG, JSON_KEY_IMAGE_URL, image, query);
                    }
                }
            } else {
                logError(TAG, JSON_KEY_SIZE, image, query);
            }
        }

        return imageUrl;
    }

    /**
     * This method compares two LastFM image size strings.
     *
     * @param sizeA The parameter to compare to.
     * @param sizeB The parameter to compare with.
     * @return {@link #EQUAL} if equal, {@link #LESS_THAN} if smaller or {@link #GREATER_THAN} if
     * sizeA in comparison to sizeB.
     */
    @ComparatorInt
    private static int sizeComparator(final String sizeA, final String sizeB) {
        @ComparatorInt
        final int result;

        if (sizeA == null) {
            result = LESS_THAN;
        } else if (sizeB == null) {
            result = GREATER_THAN;
        } else {
            switch (sizeA) {
                case IMAGE_SIZE_MEGA:
                    result = comparatorMega(sizeB);
                    break;
                case IMAGE_SIZE_EXTRA_LARGE:
                    result = comparatorExtraLarge(sizeB);
                    break;
                case IMAGE_SIZE_LARGE:
                    result = comparatorLarge(sizeB);
                    break;
                case IMAGE_SIZE_MEDIUM:
                    result = comparatorMedium(sizeB);
                    break;
                default:
                    result = comparatorSmall(sizeB);
                    break;
            }
        }

        return result;
    }

    @Override
    public String[] getCoverUrl(final AlbumInfo albumInfo)
            throws URISyntaxException, JSONException, IOException {
        final URL query = getCoverQueryURL(albumInfo);
        final String response = executeGetRequest(query);
        final JSONObject root = new JSONObject(response);
        List<String> coverUrls = Collections.emptyList();

        if (root.has(JSON_KEY_ALBUM)) {
            final JSONObject album = root.getJSONObject(JSON_KEY_ALBUM);

            if (album.has(JSON_KEY_IMAGE)) {
                final String url = getImageUrl(album.getJSONArray(JSON_KEY_IMAGE), query);

                if (url == null) {
                    coverUrls = Collections.emptyList();
                } else {
                    coverUrls = Collections.singletonList(url);
                }
            } else {
                logError(TAG, JSON_KEY_IMAGE, album, query);
            }
        } else {
            logError(TAG, JSON_KEY_ALBUM, root, query);
        }

        return coverUrls.toArray(new String[coverUrls.size()]);
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * This is a comparator annotation, used to define comparator values, without the memory use
     * of an enum.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({EQUAL, GREATER_THAN, LESS_THAN})
    private @interface ComparatorInt {

    }
}
