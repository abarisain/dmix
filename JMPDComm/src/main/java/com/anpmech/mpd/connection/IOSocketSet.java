/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2015 The MPDroid Project
 *
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice,this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.anpmech.mpd.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * A set of fields associated with the socket address with which it is constructed.
 *
 * <p>This class is not thread safe and use should be restricted to one thread per instance or with
 * a {@link ThreadLocal} resource.</p>
 */
class IOSocketSet {

    /**
     * The error message given in the case of a failure to close resources.
     */
    static final String ERROR_FAILED_TO_CLOSE = "Failed to close the SocketSet.";

    /**
     * This is the timeout for initiating of a connection.
     */
    private static final int CONNECTION_TIMEOUT = 10000;

    /**
     * The reader associated with {@link #mSocket}.
     */
    private final BufferedReader mReader;

    /**
     * The socket associated with the constructed socket address.
     */
    private final Socket mSocket;

    /**
     * The writer associated with {@link #mSocket}.
     */
    private final OutputStreamWriter mWriter;

    /**
     * The sole constructor. This constructor connects the socket to the address given in the
     * {@code socketAddress} parameter and generates a reader/writer for this socket.
     *
     * @param socketAddress    The socket address to construct the socket for.
     * @param readWriteTimeout The timeout to give to the socket address.
     * @throws IOException Thrown upon a communication error with the server.
     */
    IOSocketSet(final SocketAddress socketAddress, final int readWriteTimeout) throws IOException {
        super();

        /** Do not store the socketAddress as a field, it could prevent a weak map cleanup. */
        mSocket = new Socket();
        mSocket.setSoTimeout(readWriteTimeout);
        mSocket.connect(socketAddress, CONNECTION_TIMEOUT);

        final InputStream inputStream = mSocket.getInputStream();
        final InputStreamReader inputStreamReader =
                new InputStreamReader(inputStream, MPDConnection.MPD_PROTOCOL_CHARSET);
        mReader = new BufferedReader(inputStreamReader);

        final OutputStream outputStream = mSocket.getOutputStream();
        mWriter = new OutputStreamWriter(outputStream, MPDConnection.MPD_PROTOCOL_CHARSET);
    }

    /**
     * Closes all associated resources. This object after this is called.
     *
     * @throws IOException If an error occurs while closing any of the resources.
     */
    public void close() throws IOException {
        /**
         * Close the socket only, the other resources might be blocking.
         */
        mSocket.close();
    }

    /**
     * Gets the {@link BufferedReader} associated with the {@link Socket}.
     *
     * @return A reader associated with the {@link #mSocket}.
     */
    public BufferedReader getReader() {
        return mReader;
    }

    /**
     * Gets the {@link OutputStreamWriter} associated with the {@link Socket}.
     *
     * @return A writer associated with the {@link #mSocket}.
     */
    public OutputStreamWriter getWriter() {
        return mWriter;
    }

    /**
     * Gets the current connected status of the Socket object.
     *
     * <p>This is <b>not</b> a replacement for connection status, and is more of a low level
     * status.</p>
     *
     * @return Returns the socket constructed for the server address.
     * @see Socket
     */
    public boolean isValid() {
        return mSocket.isConnected() && !mSocket.isClosed();
    }

    @Override
    public String toString() {
        final String socketLine = "isBound(): " + mSocket.isBound() +
                " isConnected(): " + mSocket.isConnected() +
                " isClosed(): " + mSocket.isClosed() +
                " isInputShutdown(): " + mSocket.isInputShutdown() +
                " isOutputShutdown(): " + mSocket.isOutputShutdown() +
                " getInetAddress(): " + mSocket.getInetAddress();

        return "IOSocketSet{" +
                "mReader=" + mReader +
                ", mWriter=" + mWriter +
                ", mSocket={" + socketLine + " }," +
                '}';
    }
}
