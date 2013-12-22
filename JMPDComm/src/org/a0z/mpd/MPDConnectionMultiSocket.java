package org.a0z.mpd;

import org.a0z.mpd.exception.MPDServerException;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Class representing a connection to MPD Server.
 *
 * @version $Id: MPDConnection.java 2941 2005-02-09 02:34:21Z galmeida $
 */
public class MPDConnectionMultiSocket extends MPDConnection {

    private ThreadLocal<Socket> socket = new ThreadLocal<Socket>();
    private ThreadLocal<InputStreamReader> inputstream = new ThreadLocal<InputStreamReader>();
    private ThreadLocal<OutputStreamWriter> outputstream = new ThreadLocal<OutputStreamWriter>();


    MPDConnectionMultiSocket(InetAddress server, int port, int maxConnection, String password, int readWriteTimeout) throws MPDServerException {
        super(server, port, readWriteTimeout, maxConnection, password);
    }

    @Override
    public OutputStreamWriter getOutputStream() {
        return this.outputstream.get();
    }

    @Override
    public void setOutputStream(OutputStreamWriter outputStream) {
        this.outputstream.set(outputStream);
    }

    @Override
    public InputStreamReader getInputStream() {
        return this.inputstream.get();
    }

    @Override
    public void setInputStream(InputStreamReader inputStream) {
        this.inputstream.set(inputStream);
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
