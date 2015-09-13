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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * This class represents a base for cover retrievers.
 */
abstract class AbstractWebCover implements ICoverRetriever {

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

    /**
     * The class log identifier.
     */
    private static final String TAG = "AbstractWebCover";

    /**
     * Sole constructor.
     */
    protected AbstractWebCover() {
        super();
    }

    /**
     * This method encodes a query string fragment to explicitly retain it's ampersand.
     *
     * <p>This is required to explicitly show which URI query ampersands require encoding.</p>
     *
     * @param query The query string requiring ampersand encoding.
     * @return The query string with ampersand encoding.
     */
    protected static String encodeQuery(final String query) {
        final String tokenAdded;

        if (query.indexOf('&') == -1) {
            tokenAdded = query;
        } else {
            tokenAdded = AMPERSAND.matcher(query).replaceAll(AMPERSAND_TOKEN);
        }

        return tokenAdded;
    }

    /**
     * This method encodes using {@link URI#toASCIIString()}, and encodes explicit ampersands
     * using {@link #encodeQuery(String)}.
     *
     * @param host  The host for this URL.
     * @param path  The path for this URL.
     * @param query The query for this URL.
     * @return A encoded URL.
     * @throws URISyntaxException    Upon syntax error.
     * @throws MalformedURLException Upon incorrect input for the URI to ASCII conversion.
     */
    protected static URL encodeUrl(final String host, final String path,
            final String query) throws URISyntaxException, MalformedURLException {
        String uri = new URI("https", host, path, query, null).toASCIIString();

        if (uri.contains(AMPERSAND_TOKEN)) {
            uri = COMPILE.matcher(uri).replaceAll("%26");
        }

        return new URL(uri);
    }

    /**
     * This method executes a GET request for a response given in the form of a String.
     *
     * @param request     The GET request URL.
     * @param checkExists If true, the request URL is checked for a valid connection code first and
     *                    a null response will be returned if the URL does not exist.
     * @return The GET request response, or null if {@code checkExists} is {@code true}
     * and the URL gives a valid exists HTTP response code.
     * @throws IOException Upon connection error.
     */
    private static String executeGetRequest(final URL request, final boolean checkExists)
            throws IOException {
        final HttpURLConnection connection = prepareGetConnection(request);
        String response = null;

        try {
            if (!checkExists || CoverManager.doesUrlExist(connection)) {
                response = readInputStream(connection.getInputStream());
            }
        } finally {
            connection.disconnect();
        }

        return response;
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
            final URL url) {
        if (CoverManager.DEBUG) {
            Log.d(tag, "No items of key " + key + " in response " + response + " for url " + url);
        }
    }

    /**
     * This method prepares a GET request connection.
     *
     * @param url The URL to request a GET response from.
     * @return A GET request connection.
     * @throws IOException Upon error opening the connection.
     */
    private static HttpURLConnection prepareGetConnection(final URL url) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setUseCaches(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("GET");
        connection.setDoInput(true);

        return connection;
    }

    /**
     * This method prepares a POST request connection.
     *
     * @param url    The URL to request a POST response from.
     * @param length The length of the POST request.
     * @return A POST request connection.
     * @throws IOException Upon error opening the connection.
     */
    private static HttpURLConnection preparePostConnection(final URL url, final int length)
            throws IOException {
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

    /**
     * This method reads an InputStream to a string.
     *
     * @param content The InputStream to read from.
     * @return A String representation of the InputStream.
     * @throws IOException Upon connection error.
     */
    private static String readInputStream(final InputStream content) throws IOException {
        BufferedReader reader = null;

        /** We have no /idea/ how large the input is going to be. */
        final StringBuilder result = new StringBuilder();
        String line;

        try {
            reader = new BufferedReader(new InputStreamReader(content, "UTF-8"));
            line = reader.readLine();
            do {
                result.append(line);
                line = reader.readLine();
            } while (line != null);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Failed to close the input reader.", e);
                }
            }
        }

        return result.toString();
    }

    /**
     * This method writes out a string to an OutputStream.
     *
     * @param outputStream The destination to write the request to.
     * @param request      The request to write.
     * @throws IOException Upon connection error.
     */
    private static void writeOutputStream(final OutputStream outputStream, final String request)
            throws IOException {
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            writer.write(request);
            writer.flush();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Failed to close the output writer.", e);
                }
            }
        }
    }

    /**
     * This method executes a GET request for a response given in the form of a string, only if
     * the URL gives a valid HTTP response code.
     *
     * @param request The GET request URL.
     * @return The GET request response.
     * @throws IOException Upon connection error.
     * @see #executeGetRequestIfExists(URL)
     */
    protected String executeGetRequest(final URL request) throws IOException {
        return executeGetRequest(request, false);
    }

    /**
     * This method executes a GET request for a response given in the form of a string, only if
     * the URL gives a valid HTTP response code.
     *
     * @param request The GET request URL.
     * @return The GET request response, or an empty string if the URL does not give a valid
     * exists HTTP response code.
     * @throws IOException Upon connection error.
     * @see #executeGetRequest(URL)
     */
    protected String executeGetRequestIfExists(final URL request) throws IOException {
        return executeGetRequest(request, true);
    }

    /**
     * This method executes a POST request for a given request.
     *
     * @param url     The POST request URL.
     * @param request The POST request contents.
     * @return The POST response.
     * @throws IOException Upon connection error.
     */
    protected String executePostRequest(final URL url, final String request)
            throws IOException {
        final HttpURLConnection connection = preparePostConnection(url,
                request.getBytes("UTF-8").length);

        try {
            writeOutputStream(connection.getOutputStream(), request);
            return readInputStream(connection.getInputStream());
        } finally {
            connection.disconnect();
        }
    }

    /**
     * This method returns the source of the cover retriever.
     *
     * @return True if the cover will be retrieved from the local device, false otherwise.
     */
    @Override
    public boolean isCoverLocal() {
        return false;
    }
}
