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

import com.namelessdev.mpdroid.cover.CoverManager;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

abstract class AbstractWebCover implements ICoverRetriever {

    /**
     * This is the string used to designate a secure http scheme for the first URI parameter.
     */
    protected static final String HTTPS_SCHEME = "https";

    /**
     * This is the string used to designate a non-secure http scheme for the first URI parameter.
     */
    protected static final String HTTP_SCHEME = "http";

    /**
     * This pattern compiles to match an ampersand.
     */
    private static final Pattern AMPERSAND = Pattern.compile("&");

    /**
     * This string designates a token to replace an explicit query ampersand prior to URI encoding.
     */
    private static final String AMPERSAND_TOKEN = "AMPERSAND-AMPERSAND-AMPERSAND";

    /**
     * This pattern compiles to match the {@link #AMPERSAND_TOKEN}.
     */
    private static final Pattern COMPILE = Pattern.compile(AMPERSAND_TOKEN, Pattern.LITERAL);

    private static final boolean DEBUG = CoverManager.DEBUG;

    private static final String TAG = "AbstractWebCover";

    /**
     * This method encodes a query string fragment to explicitly retain it's ampersand.
     *
     * <p>This is required to explicitly show which URI query ampersands require encoding.</p>
     *
     * @param query The query string requiring ampersand encoding.
     * @return The query string with ampersand encoding.
     */
    protected static String encodeQuery(final String query) {
        final String tokened;

        if (query.indexOf('&') == -1) {
            tokened = query;
        } else {
            tokened = AMPERSAND.matcher(query).replaceAll(AMPERSAND_TOKEN);
        }

        return tokened;
    }

    /**
     * This method encodes using {@link URI#toASCIIString()}, and encodes explicit ampersands
     * using {@link #encodeQuery(String)}.
     *
     * @param scheme The URI scheme, or null for a non-absolute URI.
     * @param host   The host for this URL.
     * @param path   The path for this URL.
     * @param query  The query for this URL.
     * @return A encoded URL.
     * @throws URISyntaxException Upon syntax error.
     */
    protected static String encodeUrl(final String scheme, final String host, final String path,
            final String query) throws URISyntaxException {
        String uri = new URI(scheme, host, path, query, null).toASCIIString();

        if (uri.contains(AMPERSAND_TOKEN)) {
            uri = COMPILE.matcher(uri).replaceAll("%26");
        }

        return uri;
    }

    /**
     * This is used to log an error with parsing a key in the JSON response.
     *
     * @param tag      The log identifier for the class calling this method.
     * @param key      The key parsed.
     * @param response The full response parsed for the key.
     * @param url      The query URL.
     */
    protected static void logError(final String tag, final String key, final Object response,
            final String url) {
        if (CoverManager.DEBUG) {
            Log.d(tag, "No items of key " + key + " in response " + response + " for url " + url);
        }
    }

    /**
     * This method prepares a GET request from a raw string URL.
     *
     * @param rawUrl The raw string URL to request a GET response from.
     * @return A string containing the GET response.
     * @throws IOException If there was an error communicating the request.
     */
    private static HttpURLConnection prepareGetConnection(final String rawUrl) throws IOException {
        final URL url = new URL(rawUrl);
        final HttpURLConnection connection = CoverManager.getHTTPConnection(url);
        connection.setRequestMethod("GET");
        connection.setDoInput(true);

        return connection;
    }

    private static HttpURLConnection preparePostConnection(final String rawUrl, final int length)
            throws IOException {
        final URL url = new URL(rawUrl);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/plain");
        connection.setRequestProperty("Charset", "utf-8");
        connection.setRequestProperty("Content-Length", "" + Integer.toString(length));
        connection.setUseCaches(false);

        return connection;
    }

    private static String readInputStream(final InputStream content)
            throws IOException {
        final InputStreamReader inputStreamReader = new InputStreamReader(content, "UTF-8");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(content, "UTF-8"));

        /** We have no /idea/ how large the input is going to be. */
        //noinspection StringBufferWithoutInitialCapacity
        final StringBuilder result = new StringBuilder();
        String line;

        try {
            line = reader.readLine();
            do {
                result.append(line);
                line = reader.readLine();
            } while (line != null);
        } finally {
            try {
                inputStreamReader.close();
                reader.close();
            } catch (final IOException e) {
                Log.e(TAG, "Failed to close the buffered reader.", e);
            }
        }

        return result.toString();
    }

    protected String executeGetRequest(final String request) throws IOException {
        final HttpURLConnection connection = prepareGetConnection(request);

        return readInputStream(connection.getInputStream());
    }

    /**
     * Use a connection instead of httpClient to be able to handle redirection Redirection are
     * needed for MusicBrainz web services.
     *
     * @param request The web service request
     * @return The web service response
     * @throws IOException Upon connection error during GET request.
     */
    protected String executeGetRequestWithConnection(final String request) throws IOException {
        final URL url = CoverManager.buildURLForConnection(request);
        final HttpURLConnection connection = CoverManager.getHTTPConnection(url);
        String result = null;
        InputStream inputStream = null;

        if (CoverManager.doesUrlExist(connection)) {
            /** TODO: After minSdkVersion="19" use try-with-resources here. */
            try {
                inputStream = connection.getInputStream();
                result = readInputStream(inputStream);
            } catch (final IOException e) {
                Log.e(TAG, "Failed to execute cover get request.", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (final IOException e) {
                    Log.e(TAG, "Failed to close input stream from get request.", e);
                }
            }
        }
        return result;
    }

    protected String executePostRequest(final String url, final String request) throws IOException {
        final HttpURLConnection connection = preparePostConnection(url, request.getBytes().length);
        String response = null;
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(
                    new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
            writer.write(request);
            writer.flush();

            response = readInputStream(connection.getInputStream());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Failed to close buffered writer.", e);
                }
            }
        }

        return response;
    }

    @Override
    public boolean isCoverLocal() {
        return false;
    }
}
