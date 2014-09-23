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

import com.namelessdev.mpdroid.helpers.CoverManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.net.http.AndroidHttpClient;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

@SuppressWarnings("resource")
public abstract class AbstractWebCover implements ICoverRetriever {

    private static final boolean DEBUG = CoverManager.DEBUG;

    private static final String TAG = "AbstractWebCover";

    private static String readInputStream(final InputStream content) {
        final InputStreamReader inputStreamReader = new InputStreamReader(content);
        final BufferedReader reader = new BufferedReader(inputStreamReader);

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
        } catch (final IOException e) {
            if (CoverManager.DEBUG) {
                Log.e(TAG, "Failed to retrieve the with the buffered reader.", e);
            }
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

    protected String executeGetRequest(final String rawRequest) {
        final HttpGet httpGet;
        final String httpRequest;
        final String response;

        httpRequest = rawRequest.replace(" ", "%20");
        if (DEBUG) {
            Log.d(TAG, "HTTP request : " + httpRequest);
        }
        httpGet = new HttpGet(httpRequest);
        response = executeRequest(httpGet);

        if (httpRequest != null && !httpGet.isAborted()) {
            httpGet.abort();
        }
        return response;
    }

    /**
     * Use a connection instead of httpClient to be able to handle redirection
     * Redirection are needed for MusicBrainz web services.
     *
     * @param request The web service request
     * @return The web service response
     */
    protected String executeGetRequestWithConnection(final String request) {

        final URL url = CoverManager.buildURLForConnection(request);
        final HttpURLConnection connection = CoverManager.getHttpConnection(url);
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

    protected String executePostRequest(final String url, final String request) {
        HttpPost httpPost = null;
        String result = null;

        try {
            httpPost = new HttpPost(url);
            if (DEBUG) {
                Log.d(TAG, "Http request : " + request);
            }
            httpPost.setEntity(new StringEntity(request));
            result = executeRequest(httpPost);
        } catch (final UnsupportedEncodingException e) {
            Log.e(TAG, "Cannot build the HTTP POST.", e);
            result = "";
        } finally {
            if (request != null && httpPost != null && !httpPost.isAborted()) {
                httpPost.abort();
            }
        }
        return result;
    }

    String executeRequest(final HttpUriRequest request) {

        final AndroidHttpClient client = prepareRequest();
        final HttpResponse response;
        final StatusLine statusLine;
        final int statusCode;
        final HttpEntity entity;
        InputStream content = null;
        String result = null;

        try {
            response = client.execute(request);
            statusLine = response.getStatusLine();
            statusCode = statusLine.getStatusCode();

            if (CoverManager.doesUrlExist(statusCode)) {
                entity = response.getEntity();
                content = entity.getContent();
                result = readInputStream(content);
            } else if (CoverManager.DEBUG) {
                Log.w(TAG, "Failed to download cover : HTTP status code : " + statusCode);

            }
        } catch (final IOException e) {
            if (CoverManager.DEBUG) {
                Log.e(TAG, "Failed to download cover.", e);
            }
        } catch (final IllegalStateException e) {
            Log.e(TAG, "Illegal state exception when downloading.", e);
        } finally {
            if (content != null) {
                try {
                    content.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Failed to close the content.", e);
                }
            }
            if (client != null) {
                client.close();
            }
        }
        if (DEBUG) {
            Log.d(TAG, "HTTP response: " + result);
        }
        return result;
    }

    @Override
    public boolean isCoverLocal() {
        return false;
    }

    AndroidHttpClient prepareRequest() {
        final int fiveSeconds = 5000;
        final String userAgent = "MPDROID/0.0.0 ( MPDROID@MPDROID.com )";
        final AndroidHttpClient client = AndroidHttpClient.newInstance(userAgent);
        final HttpParams params = client.getParams();
        HttpConnectionParams.setConnectionTimeout(params, fiveSeconds);
        HttpConnectionParams.setSoTimeout(params, fiveSeconds);

        return client;
    }
}
