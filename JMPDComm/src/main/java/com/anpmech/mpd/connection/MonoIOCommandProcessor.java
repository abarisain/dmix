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

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * This class sends one {@link com.anpmech.mpd.MPDCommand} or {@link com.anpmech.mpd.CommandQueue}
 * string over a single connection resource, queueing if necessary, then processes and returns the
 * result.
 *
 * <p>Only <b>one</b> thread should access command processing methods of this class, per instance,
 * per SocketAddress.</p>
 */
class MonoIOCommandProcessor extends IOCommandProcessor {

    /**
     * A map of blocking IO socket corresponding to it's SocketAddress.
     *
     * <p>Do not synchronize this map, this class does not have a concurrency expectation.</p>
     */
    private static final Map<SocketAddress, IOSocketSet> SOCKET_MAP = new HashMap<>(1);

    /**
     * The class log identifier.
     */
    private static final String TAG = "MonoIOCommandProcessor";

    /**
     * The connection timeout for if there is nothing read or written to a socket.
     */
    private final int mReadWriteTimeout;

    /**
     * The current socket address.
     */
    private final SocketAddress mSocketAddress;

    /**
     * The constructor for this CommandProcessor.
     *
     * @param socketAddress    The host/port pair used to connect to the media server.
     * @param connectionStatus The status tracker for this connection.
     * @param commandString    The command string to be processed.
     * @param readWriteTimeout The {@link Socket#setSoTimeout(int)} for this connection.
     */
    MonoIOCommandProcessor(final SocketAddress socketAddress,
            final MPDConnectionStatus connectionStatus, final String commandString,
            final int readWriteTimeout) {
        super(connectionStatus, commandString);

        mReadWriteTimeout = readWriteTimeout;
        mSocketAddress = socketAddress;
    }

    /**
     * Iterates through the {@link #SOCKET_MAP}, disconnecting and closing all IOSocketSets.
     */
    public static void disconnect() {
        for (final IOSocketSet socketSet : SOCKET_MAP.values()) {
            disconnect(socketSet);
        }
    }

    /**
     * This method disconnects and closes a IOSocket associated with the {@link SocketAddress}
     * parameter.
     *
     * @param socketAddress The SocketAddress to close the connection to.
     */
    public static void disconnect(final SocketAddress socketAddress) {
        disconnect(SOCKET_MAP.get(socketAddress));
    }

    /**
     * Pops off the stack or creates a new {@link IOSocketSet} then validates and connects if
     * necessary.
     *
     * @return A connected and validated IOSocketSet.
     * @throws IOException Thrown if there was a problem reading from from the media server.
     */
    @Override
    IOSocketSet popSocketSet() throws IOException {
        IOSocketSet socketSet = SOCKET_MAP.get(mSocketAddress);

        if (shouldReconnect(socketSet)) {
            disconnect(socketSet);
            socketSet = new IOSocketSet(mSocketAddress, mReadWriteTimeout);
            innerConnect(socketSet);
        }

        return socketSet;
    }

    /**
     * Pushes a {@link IOSocketSet} back onto the stack for possible later use, if still valid.
     *
     * @param socketSet A connected and validated IOSocketSet.
     */
    @Override
    void pushSocketSet(final IOSocketSet socketSet) {
        SOCKET_MAP.put(mSocketAddress, socketSet);
    }

    @Override
    public String toString() {
        return "MonoIOCommandProcessor{" +
                "mSocketAddress=" + mSocketAddress +
                ", mReadWriteTimeout=" + mReadWriteTimeout +
                "} " + super.toString();
    }
}
