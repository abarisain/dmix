package com.namelessdev.mpdroid.cover;


import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;

import java.io.*;
import java.text.Normalizer;

public abstract class AbstractWebCover implements ICoverRetriever {

    private final String USER_AGENT = "MPDROID/0.0.0 ( MPDROID@MPDROID.com )";

    protected HttpClient client = null;


    protected String executePostRequest(String url, String request) {
        prepareRequest();
        HttpPost httpPost = new HttpPost(url);
        try {
            httpPost.setEntity(new StringEntity(request));
        } catch (UnsupportedEncodingException e) {
            Log.e(AbstractWebCover.class.toString(), "Cannot build the HTTP POST : " + e);
            return "";
        }
        return executeRequest(httpPost);
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

            if (statusCode == 200) {

                reader = new BufferedReader(new InputStreamReader(content));
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            } else {
                Log.e(AbstractWebCover.class.toString(), "Failed to download cover : HTTP status code : " + statusCode);

            }
        } catch (Exception e) {
            Log.e(AbstractWebCover.class.toString(), "Failed to download cover :" + e);
        } finally {
            {

                if (entity != null) {
                    try {
                        entity.consumeContent();
                    } catch (IOException e) {
                        //Nothing to do
                    }

                }

                try {
                    if (content != null) {
                        content.close();
                    }
                } catch (IOException e) {
                    //Nothing to do
                }

            }
        }
        return builder.toString();
    }

    protected void prepareRequest() {

        if (client == null) {
            client = new DefaultHttpClient();
            HttpProtocolParams.setUserAgent(client.getParams(), USER_AGENT);
        }
    }


    protected String executeGetRequest(String request) {

        prepareRequest();
        request = request.replace(" ", "%20");
        HttpGet httpGet = new HttpGet(request);
        return executeRequest(httpGet);

    }

    protected String cleanGetRequest(String text) {
        String processedtext;

        if (text == null) {
            return text;
        }

        processedtext = text.replaceAll("[^\\w .-]+", " ");
        processedtext = Normalizer.normalize(processedtext, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return processedtext;
    }


    public boolean isCoverLocal() {
        return false;
    }

}
