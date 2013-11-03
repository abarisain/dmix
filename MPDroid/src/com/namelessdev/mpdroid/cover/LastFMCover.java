package com.namelessdev.mpdroid.cover;

import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;

/**
 * Fetch cover from LastFM
 */
public class LastFMCover extends AbstractWebCover {

    private static String KEY = "7fb78a81b20bee7cb6e8fad4cbcb3694";
    private static final String URL = "http://ws.audioscrobbler.com/2.0/";

    @Override
    public String[] getCoverUrl(String artist, String album, String path, String filename) throws Exception {

        String response;
        String request;
        String sizeAttribute = null;
        String imageUrl;
        XmlPullParserFactory factory;
        XmlPullParser xpp;
        int eventType;

        try {
            request = URL + "?method=album.getInfo&artist=" + artist + "&album=" + album + "&api_key=" + KEY;
            response = executeGetRequest(request);

            factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            xpp = factory.newPullParser();

            xpp.setInput(new StringReader(response));
            eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equals("image")) {
                        sizeAttribute = xpp.getAttributeValue(null, "size");
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    if (sizeAttribute != null && sizeAttribute.equals("mega")) {
                        imageUrl = xpp.getText();
                        if (imageUrl != null && imageUrl.length() > 0) {
                            return new String[]{imageUrl};
                        }
                    }
                }
                eventType = xpp.next();
            }
        } catch (Exception e)

        {
            Log.e(LastFMCover.class.toString(), "Failed to get cover URL from LastFM :" + e);
        }

        return new String[0];
    }

    @Override

    public String getName() {
        return "LastFM";
    }
}
