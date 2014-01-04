package com.namelessdev.mpdroid.cover;


import android.net.http.AndroidHttpClient;
import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.HttpConnectionParams;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public abstract class AbstractWebCover implements ICoverRetriever {

    private final String USER_AGENT = "MPDROID/0.0.0 ( MPDROID@MPDROID.com )";
    private final static boolean DEBUG = false;

    protected AndroidHttpClient client = prepareRequest();

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

    private void closeHttpClient() {
        if (client != null) {
            client.close();
        }
        client = null;
    }

    protected String executeRequest(HttpRequestBase request) {

        StringBuilder builder = new StringBuilder();
        HttpResponse response;
        StatusLine statusLine;
        int statusCode;
        HttpEntity entity = null;
        InputStream content = null;
        BufferedReader reader;
        String line;

        try {
            response = client.execute(request);
            statusLine = response.getStatusLine();
            statusCode = statusLine.getStatusCode();
            entity = response.getEntity();
            content = entity.getContent();

            if (statusCode == 200 || statusCode == 307 || statusCode == 302) {

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

    protected AndroidHttpClient prepareRequest() {

        if (client == null) {
            client = AndroidHttpClient.newInstance(USER_AGENT);
            HttpConnectionParams.setConnectionTimeout(client.getParams(), 5000);
            HttpConnectionParams.setSoTimeout(client.getParams(), 5000);
        }
        return client;
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

    public boolean isCoverLocal() {
        return false;
    }

    @Override
    protected void finalize() throws Throwable {
        closeHttpClient();
        super.finalize();
    }
}
