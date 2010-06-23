package org.a0z.mpd.url;

import java.net.ContentHandler;
import java.net.ContentHandlerFactory;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * MPD implementation of <code>ContentHandlerFactory</code>.
 * @author galmeida
 */
public class MpdContentHandlerFactory implements ContentHandlerFactory {
    private Map map = new HashMap();

    /**
     * Creates a new <code>ContentHandler</code> to read an object from a <code>URLStreamHandler</code>.
     * @param mimetype the MIME type for which a content handler is desired.
     * @return a new ContentHandler to read an object from a URLStreamHandler.
     */
    public ContentHandler createContentHandler(String mimetype) {
        Class handlerClass = (Class) map.get(mimetype);
        ContentHandler handler = null;
        if (handlerClass != null) {
            try {
                handler = (ContentHandler) handlerClass.newInstance();
            } catch (InstantiationException e) {
                //should not happen
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                //should not happen
                e.printStackTrace();
            }
        }
        if (handler == null) {
            handler = new NullContentHandler();
        }
        return handler;
    }

    /**
     * Register a new <code>ContentHandler</code> for a given Mime-Type.
     * @param mimetype Mime-Type.
     * @param handlerClass <code>ContentHandler</code> class.
     */
    public void registerContentHandler(String mimetype, Class handlerClass) {
        map.put(mimetype, handlerClass);
    }

    /**
     * Default <code>ContentHandler</code>, will always throw UnsupportedMimeTypeException.
     * @author galmeida
     */
    public class NullContentHandler extends ContentHandler {
        /**
         * Throws <code>UnsupportedMimeTypeException</code>.
         * @param urlc a <code>URLConnection</code>.
         * @return <code>null</code>
         * @throws UnsupportedMimeTypeException always.
         */
        public Object getContent(URLConnection urlc) throws UnsupportedMimeTypeException {
            throw new UnsupportedMimeTypeException("Unsupported Mime-Type: " + urlc.getContentType());
        }
    }
}