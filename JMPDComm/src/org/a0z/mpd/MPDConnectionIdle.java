package org.a0z.mpd;

import org.a0z.mpd.exception.MPDServerException;

import java.net.InetAddress;
import java.net.Socket;

/**
 * Class representing a connection to MPD Server.
 *
 * @version $Id: MPDConnection.java 2941 2005-02-09 02:34:21Z galmeida $
 */
public class MPDConnectionIdle extends MPDConnection {

    private Socket socket;

    MPDConnectionIdle(InetAddress server, int port, String password) throws MPDServerException {
        super(server, port, password);
        // connect right away and setup streams
        this.connect();
    }

    @Override
    protected Socket getSocket() {
        return socket;
    }

    @Override
    protected void setSocket(Socket socket) {
        this.socket = socket;
    }
}
