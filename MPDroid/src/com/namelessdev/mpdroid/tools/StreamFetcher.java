package com.namelessdev.mpdroid.tools;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class StreamFetcher {
    private static class LazyHolder {
        private static final StreamFetcher instance = new StreamFetcher();
    }

    public static StreamFetcher instance() {
    	return LazyHolder.instance;
    }
    
    private static String parsePlaylist(String data, String key, List<String> handlers) {
    	String[] lines=data.split("(\r\n|\n|\r)");

    	for (String line : lines) {
            if (line.toLowerCase().startsWith(key)) {
                for (String handler : handlers) {
                    String protocol=handler+"://";
                    int index=line.indexOf(protocol);
                    if (index>-1 && index<7) {
                    	return line.replace("\n", "").replace("\r", "").substring(index);
                    }
                }
            }
        }
        return null;
    }

    private static String parseExt3Mu(String data, List<String> handlers) {
    	String[] lines=data.split("(\r\n|\n|\r)");

    	for (String line : lines) {
            for (String handler : handlers) {
                String protocol=handler+"://";
                if (line.startsWith(protocol)) {
                	return line.replace("\n", "").replace("\r", "");
                }
            }
        }

        return null;
    }

    private static String parseAsx(String data, List<String> handlers) {
    	String[] lines=data.split("(\r\n|\n|\r)");

    	for (String line : lines) {
            int ref=line.indexOf("<ref href=");
            if (-1!=ref) {
                for (String handler : handlers) {
                    String protocol=handler+"://";
                    int prot=line.indexOf(protocol);
                    if (-1!=prot) {
                        String[] parts=line.split("\"");
                        for (String part : parts) {
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

    private static String parseXml(String data, List<String> handlers) {
        // XSPF / SPIFF
    	try {
    		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    		XmlPullParser xpp = factory.newPullParser();

    		xpp.setInput(new StringReader(data));
    		int eventType = xpp.getEventType();
    		boolean inLocation=false;

    		while (eventType != XmlPullParser.END_DOCUMENT) {
    			if (XmlPullParser.START_TAG==eventType) {
    				inLocation=xpp.getName().equals("location");
    			} else if(inLocation && XmlPullParser.TEXT==eventType) {
    				String text=xpp.getText();
    				for (String handler : handlers) {
    					if (text.startsWith(handler+"://")) {
    						return text;
    					}
    				}
    			}
    			eventType = xpp.next();
    		}
    	} catch (Exception e) {
    	}

        return null;
    }

    static private String parse(String data, List<String> handlers) {
    	String start=data.substring(0, data.length()<10 ? data.length() : 10).toLowerCase();
        if (data.length()>10 && start.startsWith("[playlist]")) {
            return parsePlaylist(data, "file", handlers);
        } else if (data.length()>7 && (start.startsWith("#EXTM3U") || start.startsWith("http://"))) {
            return parseExt3Mu(data, handlers);
        } else if (data.length()>5 && start.startsWith("<asx ")) {
            return parseAsx(data, handlers);
        } else if (data.length()>11 && start.startsWith("[reference]")) {
            return parsePlaylist(data, "ref", handlers);
        } else if (data.length()>5 && start.startsWith("<?xml")) {
            return parseXml(data, handlers);
        } else if ( (-1==data.indexOf("<html") && -1!=data.indexOf("http:/")) || // flat list?
                (-1!=data.indexOf("#EXTM3U")) ) { // m3u with comments?
        	return parseExt3Mu(data, handlers);
        }

        return null;
    }
    
    private List<String> handlers=new LinkedList<String>();
    
    StreamFetcher() {
    	handlers.add("http");
    	handlers.add("mms");
    	handlers.add("mmsh");
    	handlers.add("mmst");
    	handlers.add("mmsu");
    	handlers.add("rtp");
    	handlers.add("rtsp");
    	handlers.add("rtmp");
    	handlers.add("rtmpt");
    	handlers.add("rtmpts");
    }

    // Add stream name to fragment part of URL sent to MPD. This way, when the
    // playqueue listing is received back from MPD, the name can be determined.
    private static String addName(String url, String name) {
    	String fixed=name.replace(" # ", " ");
    	fixed=fixed.replace("#", "");
    	return url+"#"+fixed;
    }

    public URL get(String url, String name) throws MalformedURLException {
    	String parsed=null;
    	if (url.startsWith("http://")) {
    	    parsed=check(url);
    	    if (null != parsed && parsed.startsWith("http://")) {
    	        // If 'check' returned a http link, then see if this points to the stream
    	        // or if it points to the playlist (which would point to the stream). This
    	        // case is mainly for TuneIn links...
    	        parsed=check(parsed);
    	    }
    	}
    	return new URL(addName(null==parsed ? url : parsed, name));
    }
    
    private String check(String url) {
      	HttpURLConnection connection = null;
    	try {
    		URL u = new URL(url);
    		connection = (HttpURLConnection)u.openConnection();
    		InputStream in = new BufferedInputStream(connection.getInputStream(), 8192);

    		byte buffer[] = new byte[8192];
    		int read = in.read(buffer);
    		if (read < buffer.length) {
    			buffer[read] = '\0';
    		}
    		return parse(new String(buffer), handlers);
    	} catch (IOException e) {
    	} finally {
    		if (null != connection) {
    			connection.disconnect();
    		}
    	}
    	return null;
    }
}
