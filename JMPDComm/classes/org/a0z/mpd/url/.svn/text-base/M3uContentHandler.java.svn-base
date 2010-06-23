package org.a0z.mpd.url;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.ContentHandler;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;

/**
 * M3U <code>ContentHandler</code>.
 * @author Felipe Gustavo de Almeida - galmeida
 * @version $Id$
 */
public class M3uContentHandler extends ContentHandler {

    /**
     * Given a connection reads a M3U playlist and retrieves its contents.
     * @param urlc a <code>URLConnection</code>.
     * @return M3U contents as a <code>List</code> of <code>String</code>s.
     * @throws IOException on IO errors.
     */
    public Object getContent(URLConnection urlc) throws IOException {
        List list = new LinkedList();
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(urlc.getInputStream()));

        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            line.trim();
            if ("".equals(line) || line.startsWith("#")) {
                continue;
            }
            list.add(line);
        }
        return list;
    }

}