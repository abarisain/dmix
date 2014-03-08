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

import static android.text.TextUtils.isEmpty;
import static android.util.Log.e;
import static android.util.Log.w;

import android.net.http.AndroidHttpClient;
import android.util.Log;

import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverManager;
import com.namelessdev.mpdroid.tools.StringUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.HttpConnectionParams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class AbstractWebCover implements ICoverRetriever {

    private final String USER_AGENT = "MPDROID/0.0.0 ( MPDROID@MPDROID.com )";
    private final static boolean DEBUG = CoverManager.DEBUG;

    protected AndroidHttpClient client = prepareRequest();

    private void closeHttpClient() {
        if (client != null) {
            client.close();
        }
        client = null;
    }

    protected String executeGetRequest(String request) {
        HttpGet httpGet = null;
        try {
            prepareRequest();
            request = request.replace(" ", "%20");
            if (DEBUG)
                Log.d(getName(), "Http request : " + request);
            httpGet = new HttpGet(request);
            return executeRequest(httpGet);
        } finally {
            if (request != null && !httpGet.isAborted()) {
                httpGet.abort();
            }
        }
    }

    /**
     * Use a connection instead of httpClient to be able to handle redirection
     * Redirection are needed for MusicBrainz web services.
     * 
     * @param request The web service request
     * @return The web service response
     */
    protected String executeGetRequestWithConnection(String request) {

        URL url;
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        int statusCode;
        BufferedReader bis;
        String result;
        String line;
        try {
            request = StringUtils.trim(request);
            if (isEmpty(request)) {
                return null;
            }
            request = request.replace(" ", "%20");
            url = new URL(request);
            connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            statusCode = connection.getResponseCode();
            inputStream = connection.getInputStream();
            if (!CoverManager.urlExists(statusCode)) {
                w(CoverAsyncHelper.class.getName(), "This URL does not exist : Status code : "
                        + statusCode + ", " + request);
                return null;
            }
            bis = new BufferedReader(new InputStreamReader(inputStream));
            line = bis.readLine();
            result = line;
            while ((line = bis.readLine()) != null) {
                result += line;
            }
            return result;
        } catch (Exception e) {
            e(CoverAsyncHelper.class.getSimpleName(), "Failed to execute cover get request :" + e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Nothing to do
                }
            }

        }
    }

    protected String executePostRequest(String url, String request) {
        HttpPost httpPost = null;

        try {
            prepareRequest();
            httpPost = new HttpPost(url);
            if (DEBUG)
                Log.d(getName(), "Http request : " + request);
            httpPost.setEntity(new StringEntity(request));
            return executeRequest(httpPost);
        } catch (UnsupportedEncodingException e) {
            Log.e(getName(), "Cannot build the HTTP POST : " + e);
            return "";
        } finally {
            if (request != null && !httpPost.isAborted()) {
                httpPost.abort();
            }
        }

    }

    protected String executeRequest(HttpRequestBase request) {

        StringBuilder builder = new StringBuilder();
        HttpResponse response;
        StatusLine statusLine;
        int statusCode;
        HttpEntity entity;
        InputStream content;
        BufferedReader reader;
        String line;

        try {
            response = client.execute(request);
            statusLine = response.getStatusLine();
            statusCode = statusLine.getStatusCode();
            entity = response.getEntity();
            content = entity.getContent();

            if(CoverManager.urlExists(statusCode)) {

                reader = new BufferedReader(new InputStreamReader(content));
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            } else {
                Log.w(getName(), "Failed to download cover : HTTP status code : " + statusCode);

            }
        } catch (Exception e) {
            Log.e(getName(), "Failed to download cover :" + e);
        }
        if (DEBUG)
            Log.d(getName(), "Http response : " + builder);
        return builder.toString();
    }

    @Override
    protected void finalize() throws Throwable {
        closeHttpClient();
        super.finalize();
    }

    public boolean isCoverLocal() {
        return false;
    }

    protected AndroidHttpClient prepareRequest() {

        if (client == null) {
            client = AndroidHttpClient.newInstance(USER_AGENT);
            HttpConnectionParams.setConnectionTimeout(client.getParams(), 5000);
            HttpConnectionParams.setSoTimeout(client.getParams(), 5000);
        }
        return client;
    }
}
