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
public class MPDConnectionMonoSocket extends MPDConnection {

    private Socket socket;
    private InputStreamReader inputStream;
    private OutputStreamWriter outputStream;

    MPDConnectionMonoSocket(InetAddress server, int port, String password, int readWriteTimeout) throws MPDServerException {
        super(server, port, password, readWriteTimeout);
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

    public OutputStreamWriter getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStreamWriter outputStream) {
        this.outputStream = outputStream;
    }

    public InputStreamReader getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStreamReader inputStream) {
        this.inputStream = inputStream;
    }
}
