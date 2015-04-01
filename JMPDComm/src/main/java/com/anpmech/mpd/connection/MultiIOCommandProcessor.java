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

import com.anpmech.mpd.CommandQueue;
import com.anpmech.mpd.MPDCommand;
import com.anpmech.mpd.commandresponse.CommandResponse;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class sends one {@link MPDCommand} or {@link CommandQueue} string over multiple possible
 * connection resources, then processes and returns the result.
 *
 * <p>This class was designed with thread safety in mind.</p>
 */
class MultiIOCommandProcessor extends IOCommandProcessor {

    /**
     * The ThreadLocal pool for the corresponding SocketAddress from which to pull the Socket
     * for the current CommandProcessor thread.
     *
     * <p>This map should not need to be synchronized. ThreadLocal is thread safe, and is
     * generated during construction.</p>
     */
    private static final Map<SocketAddress, Queue<IOSocketSet>> SOCKET_MAP
            = new ConcurrentHashMap<>();

    /**
     * The class log identifier.
     */
    private static final String TAG = "MultiCommandProcessor";

    /**
     * The connection timeout for if there is nothing read or written to a socket.
     */
    private final int mReadWriteTimeout;

    /**
     * The current socket address.
     */
    private final SocketAddress mSocketAddress;

    /**
     * The constructor for this CommandProcessor
     *
     * @param socketAddress    The host/port pair used to connect to the media server.
     * @param connectionStatus The status tracker for this connection.
     * @param commandString    The command string to be processed.
     * @param readWriteTimeout The {@link Socket#setSoTimeout(int)} for this connection.
     * @param excludeResponses This is used to manually exclude responses from split
     *                         {@link CommandResponse} inclusion.
     */
    MultiIOCommandProcessor(final SocketAddress socketAddress,
            final MPDConnectionStatus connectionStatus, final String commandString,
            final int readWriteTimeout, final int[] excludeResponses) {
        super(connectionStatus, commandString, excludeResponses);

        mSocketAddress = socketAddress;
        mReadWriteTimeout = readWriteTimeout;

        if (!SOCKET_MAP.containsKey(mSocketAddress)) {
            SOCKET_MAP.put(mSocketAddress, new ConcurrentLinkedQueue<IOSocketSet>());
        }
    }

    /**
     * This method iterates through a {@code Collection}, disconnecting and closing all
     * {@code IOSocketSet}s.
     *
     * @param queue The collection to iterate over.
     */
    private static void disconnect(final Collection<IOSocketSet> queue) {
        for (final IOSocketSet socketSet : queue) {
            disconnect(socketSet);
        }
        queue.clear();
    }

    /**
     * This method disconnects and closes a IOSocket associated with the {@link SocketAddress}
     * parameter.
     *
     * @param socketAddress The SocketAddress to close the connection to.
     */
    public static void disconnect(final SocketAddress socketAddress) {
        disconnect(SOCKET_MAP.get(socketAddress).poll());
    }

    /**
     * Iterates through the {@link #SOCKET_MAP}, disconnecting and closing all {@code
     * IOSocketSet}s.
     */
    public static void disconnect() {
        for (final Queue<IOSocketSet> queue : SOCKET_MAP.values()) {
            disconnect(queue);
        }
        SOCKET_MAP.clear();
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
        IOSocketSet socketSet = null;

        do {
            if (socketSet == null) {
                socketSet = SOCKET_MAP.get(mSocketAddress).poll();
            } else {
                /** shouldReconnect() is true */
                disconnect(socketSet);
                socketSet = SOCKET_MAP.get(mSocketAddress).poll();
                if (socketSet != null) {
                    innerConnect(socketSet);
                }
            }

            /**
             * No more left in the Queue, time to create a new one!
             */
            if (socketSet == null) {
                socketSet = new IOSocketSet(mSocketAddress, mReadWriteTimeout);
                innerConnect(socketSet);
            }
        } while (shouldReconnect(socketSet));

        return socketSet;
    }

    /**
     * Pushes a {@link IOSocketSet} back onto the stack for possible later use, if still valid.
     *
     * @param socketSet A connected and validated IOSocketSet.
     */
    @Override
    void pushSocketSet(final IOSocketSet socketSet) {
        SOCKET_MAP.get(mSocketAddress).add(socketSet);
    }

    @Override
    public String toString() {
        return "MultiIOCommandProcessor{" +
                "mSocketAddress=" + mSocketAddress +
                ", mReadWriteTimeout=" + mReadWriteTimeout +
                "} " + super.toString();
    }
}
