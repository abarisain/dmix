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

package com.namelessdev.mpdroid.tools;

import org.a0z.mpd.item.Stream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;

public class StreamFetcher {

    private static final String TAG = "StreamFetcher";

    private final Collection<String> mHandlers = new LinkedList<>();

    StreamFetcher() {
        super();
        mHandlers.add("http");
        mHandlers.add("mms");
        mHandlers.add("mmsh");
        mHandlers.add("mmst");
        mHandlers.add("mmsu");
        mHandlers.add("rtp");
        mHandlers.add("rtsp");
        mHandlers.add("rtmp");
        mHandlers.add("rtmpt");
        mHandlers.add("rtmpts");
    }

    public static StreamFetcher instance() {
        return LazyHolder.INSTANCE;
    }

    private static String parse(final String data, final Iterable<String> handlers) {
        final String start = data.substring(0, data.length() < 10 ? data.length() : 10)
                .toLowerCase();
        if (data.length() > 10 && start.startsWith("[playlist]")) {
            return parsePlaylist(data, "file", handlers);
        } else if (data.length() > 7
                && (start.startsWith("#EXTM3U") || start.startsWith("http://"))) {
            return parseExt3Mu(data, handlers);
        } else if (data.length() > 5 && start.startsWith("<asx ")) {
            return parseAsx(data, handlers);
        } else if (data.length() > 11 && start.startsWith("[reference]")) {
            return parsePlaylist(data, "ref", handlers);
        } else if (data.length() > 5 && start.startsWith("<?xml")) {
            return parseXml(data, handlers);
        } else if ((!data.contains("<html") && data.contains("http:/")) || // flat
                // list?
                (data.contains("#EXTM3U"))) { // m3u with comments?
            return parseExt3Mu(data, handlers);
        }

        return null;
    }

    private static String parseAsx(final String data, final Iterable<String> handlers) {
        final String[] lines = data.split("(\r\n|\n|\r)");

        for (final String line : lines) {
            final int ref = line.indexOf("<ref href=");
            if (-1 != ref) {
                for (final String handler : handlers) {
                    final String protocol = handler + "://";
                    final int prot = line.indexOf(protocol);
                    if (-1 != prot) {
                        final String[] parts = line.split("\"");
                        for (final String part : parts) {
                            if (part.startsWith(protocol)) {
                                return part;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private static String parseExt3Mu(final String data, final Iterable<String> handlers) {
        final String[] lines = data.split("(\r\n|\n|\r)");

        for (final String line : lines) {
            for (final String handler : handlers) {
                final String protocol = handler + "://";
                if (line.startsWith(protocol)) {
                    return line.replace("\n", "").replace("\r", "");
                }
            }
        }

        return null;
    }

    private static String parsePlaylist(final String data, final String key,
            final Iterable<String> handlers) {
        final String[] lines = data.split("(\r\n|\n|\r)");

        for (final String line : lines) {
            if (line.toLowerCase().startsWith(key)) {
                for (final String handler : handlers) {
                    final String protocol = handler + "://";
                    final int index = line.indexOf(protocol);
                    if (index > -1 && index < 7) {
                        return line.replace("\n", "").replace("\r", "").substring(index);
                    }
                }
            }
        }
        return null;
    }

    private static String parseXml(final String data, final Iterable<String> handlers) {
        // XSPF / SPIFF
        try {
            final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            final XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(new StringReader(data));
            int eventType = xpp.getEventType();
            boolean inLocation = false;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (XmlPullParser.START_TAG == eventType) {
                    inLocation = "location".equals(xpp.getName());
                } else if (inLocation && XmlPullParser.TEXT == eventType) {
                    final String text = xpp.getText();
                    for (final String handler : handlers) {
                        if (text.startsWith(handler + "://")) {
                            return text;
                        }
                    }
                }
                eventType = xpp.next();
            }
        } catch (final Exception e) {
            Log.e(TAG, "Failed to parse an XML stream file.", e);
        }

        return null;
    }

    private String check(final String url) {
        HttpURLConnection connection = null;
        try {
            final URL u = new URL(url);
            connection = (HttpURLConnection) u.openConnection();
            final InputStream in = new BufferedInputStream(connection.getInputStream(), 8192);

            final byte[] buffer = new byte[8192];
            final int read = in.read(buffer);
            if (read < buffer.length) {
                buffer[read] = '\0';
            }
            return parse(new String(buffer), mHandlers);
        } catch (final IOException e) {
            Log.e(TAG, "Failed to check and parse an incoming playlist.", e);
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
        }
        return null;
    }

    public String get(final String url, final String name) throws MalformedURLException {
        String parsed = null;
        if (url.startsWith("http://")) {
            parsed = check(url);
            if (null != parsed && parsed.startsWith("http://")) {
                // If 'check' returned a http link, then see if this points to
                // the stream or if it points to the playlist (which would point
                // to the stream). This case is mainly for TuneIn links...
                final String secondParse = check(parsed);
                if (null != secondParse) {
                    parsed = secondParse;
                }
            }
        }
        return Stream.addStreamName(null == parsed ? url : parsed, name);
    }

    private static class LazyHolder {

        private static final StreamFetcher INSTANCE = new StreamFetcher();
    }
}
