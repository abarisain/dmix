package org.a0z.mpd;

import org.a0z.mpd.exception.MPDServerException;

import java.net.InetAddress;
import java.net.Socket;

/**
 * Class representing a connection to MPD Server.
 *
 * @version $Id: MPDConnection.java 2941 2005-02-09 02:34:21Z galmeida $
 */
public class MPDConnectionData extends MPDConnection {

    private ThreadLocal<Socket> socket = new ThreadLocal<Socket>();

    MPDConnectionData(InetAddress server, int port, int maxConnection) throws MPDServerException {
        super(server, port,5000, maxConnection);
    }

    @Override
    protected Socket getSocket() {
        return socket.get();
    }

    @Override
    protected void setSocket(Socket sock) {
        socket.set(sock);
    }
}
