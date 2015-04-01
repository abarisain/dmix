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
import com.anpmech.mpd.commandresponse.CommandResponse;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Class representing a connection to MPD Server.
 *
 * <p>An instance of this class should only be accessed by one thread at a time.</p>
 */
public class MonoIOMPDConnection extends MPDConnection implements ThreadSafeMonoConnection {

    /**
     * The class log identifier.
     */
    private static final String TAG = "MPDConnectionMonoSocket";

    /**
     * Sole constructor.
     *
     * @param readWriteTimeout The {@link java.net.Socket#setSoTimeout(int)} for this connection.
     */
    public MonoIOMPDConnection(final int readWriteTimeout) {
        super(readWriteTimeout, true);
    }

    /**
     * This method outputs the {@code line} parameter to {@link Log#debug(String, String)} if
     * {@link #DEBUG} is set to true.
     *
     * @param line The {@link String} to output to the log.
     */
    @Override
    void debug(final String line) {
        if (DEBUG) {
            Log.debug(TAG, line);
        }
    }

    /**
     * The method to disconnect from the current connected server and cancel any further command
     * calls until {@link #connect()} is called.
     *
     * @throws IOException Thrown upon disconnection error.
     */
    @Override
    public void disconnect() throws IOException {
        debug("Disconnecting by method call");
        super.disconnect();

        MonoIOCommandProcessor.disconnect(mSocketAddress);
    }

    /**
     * Initiates and returns a mono socket command processor.
     *
     * @param command          The command to send to the mono socket command processor.
     * @param excludeResponses This is used to manually exclude responses from split
     *                         {@link CommandResponse} inclusion.
     * @return A mono socket command processor.
     */
    @Override
    Callable<CommandResult> getCommandProcessor(final String command,
            final int[] excludeResponses) {
        return new MonoIOCommandProcessor(mSocketAddress, mConnectionStatus, command,
                mReadWriteTimeout, excludeResponses);
    }

    /**
     * Gets the {@link MPDConnectionStatus} object for this connection.
     *
     * @return The MPDConnectionStatus object for this connection.
     */
    @Override
    public MPDConnectionStatus getConnectionStatus() {
        return mConnectionStatus;
    }

    /**
     * When used with the {@link ThreadSafeMonoConnection} interface, this returns a thread unsafe
     * version of this instance.
     *
     * @return A thread unsafe version of this instance.
     */
    @Override
    public MonoIOMPDConnection getThreadUnsafeConnection() {
        return this;
    }
}
