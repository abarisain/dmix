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

import java.net.Socket;
import java.util.concurrent.Callable;

/**
 * Class representing multiple blocking IO connections to the server.
 * <p/>
 * This class has a few limitations due to the {@link ThreadLocal} design:
 * <ul>
 * <li>Connection status cannot be depended on, connection status for one thread may not be the
 * same as another, so, connection status for this class is private.</li>
 * <li>This class cannot manually disconnect. Due to this, a {@link Socket} timeout is required
 * during construction of this class.</li>
 * </ul>
 * <p/>
 * Due to these limitations, this class is not recommended to be used alone for connection.
 * <p/>
 * This class was designed with thread safety in mind.
 */
public class MultiIOMPDConnection extends MPDConnection {

    /** The class log identifier. */
    private static final String TAG = "MPDConnectionMultiSocket";

    /**
     * Sole constructor.
     *
     * @param readWriteTimeout The {@link Socket#setSoTimeout(int)} for this connection.
     *                         This class requires disconnection timeout, so this must not equal 0.
     */
    public MultiIOMPDConnection(final int readWriteTimeout) {
        super(readWriteTimeout, new NonBlockingConnectionStatus());

        /**
         * For further explanation, see the class header documentation.
         */
        if (readWriteTimeout == 0) {
            throw new IllegalArgumentException("Cannot instantiate a MultiIOMPDConnection with no"
                    + "timeout.");
        }
    }

    @Override
    void debug(final String line) {
    }

    /**
     * Initiates and returns a mono socket command processor.
     *
     * @param command The command to send to the mono socket command processor.
     * @return A mono socket command processor.
     */
    @Override
    Callable<CommandResponse> getCommandProcessor(final String command) {
        return new MultiIOCommandProcessor(mSocketAddress, mConnectionStatus, command,
                mReadWriteTimeout);
    }
}
