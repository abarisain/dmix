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

import com.anpmech.mpd.Log;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * This class sends one {@link com.anpmech.mpd.MPDCommand} or {@link com.anpmech.mpd.CommandQueue}
 * string over multiple possible connection resources, then processes and returns the result.
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
    private static final Map<SocketAddress, ThreadLocal<IOSocketSet>> SOCKET_MAP = new HashMap<>();

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
     */
    MultiIOCommandProcessor(final SocketAddress socketAddress,
            final MPDConnectionStatus connectionStatus, final String commandString,
            final int readWriteTimeout) {
        super(connectionStatus, commandString);

        mSocketAddress = socketAddress;
        mReadWriteTimeout = readWriteTimeout;

        if (!SOCKET_MAP.containsKey(mSocketAddress)) {
            SOCKET_MAP.put(mSocketAddress, new ThreadLocal<IOSocketSet>());
        }
    }

    /**
     * This returns a socket.
     *
     * @return A socket corresponding to the {@link SocketAddress} for this connection.
     * @see #resetSocketSet()
     */
    @Override
    IOSocketSet getSocketSet() {
        return SOCKET_MAP.get(mSocketAddress).get();
    }

    /**
     * This method disconnects, closes, removes the old socket, then sets a new socket.
     *
     * @see #getSocketSet()
     */
    @Override
    void resetSocketSet() throws IOException {
        final ThreadLocal<IOSocketSet> threadLocal = SOCKET_MAP.get(mSocketAddress);
        final IOSocketSet socketSet = threadLocal.get();
        threadLocal.remove();

        if (socketSet != null) {
            try {
                socketSet.close();
            } catch (final IOException e) {
                Log.warning(TAG, IOSocketSet.ERROR_FAILED_TO_CLOSE, e);
            }
        }

        threadLocal.set(new IOSocketSet(mSocketAddress, mReadWriteTimeout));
    }

    @Override
    public String toString() {
        return "MultiIOCommandProcessor{" +
                "mSocketAddress=" + mSocketAddress +
                ", mReadWriteTimeout=" + mReadWriteTimeout +
                "} " + super.toString();
    }
}
