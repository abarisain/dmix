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
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This is a safety interface includes all thread safe methods for a {@link MonoIOMPDConnection}.
 */
public interface ThreadSafeMonoConnection {

    /**
     * The method is used to disallow any further command sending until next {@link #connect()}
     * call.
     */
    void cancel();

    /**
     * This method calls standard defaults for the host/port pair and MPD password, if it exists.
     *
     * @throws UnknownHostException Thrown when a hostname can not be resolved.
     */
    void connect() throws UnknownHostException;

    /**
     * Sets up connection to host/port pair.
     *
     * <p>If a main password is required, it MUST be called prior to calling this method.</p>
     *
     * @param host The media server host to connect to.
     * @param port The media server port to connect to.
     */
    void connect(final InetAddress host, final int port);

    /**
     * The method to disconnect from the current connected server.
     *
     * @throws IOException Thrown upon disconnection error.
     */
    void disconnect() throws IOException;

    /**
     * Gets the {@link MPDConnectionStatus} object for this connection.
     *
     * @return The MPDConnectionStatus object for this connection.
     */
    MPDConnectionStatus getConnectionStatus();

    /**
     * The current connected media server host.
     *
     * @return The current connected media server host, null if not connected.
     */
    InetAddress getHostAddress();

    /**
     * The current connected media server port.
     *
     * @return The current connected media server port, null if not connected.
     */
    int getHostPort();

    /**
     * The current MPD protocol version.
     *
     * @return The given MPD protocol version as an integer array.
     */
    int[] getMPDVersion();

    /**
     * Returns a thread unsafe version of this interface.
     *
     * @return A thread unsafe version of this interface.
     */
    MonoIOMPDConnection getThreadUnsafeConnection();

    /**
     * Checks a list of available commands generated on connection.
     *
     * @param command A MPD protocol command.
     * @return True if the {@code command} is available for use, false otherwise.
     */
    boolean isCommandAvailable(final String command);

    /**
     * Checks the media server version for support of a feature. This does not check micro version
     * as new features shouldn't be added during stable releases.
     *
     * @param major The major version to inquire for support. (x in x.0.0)
     * @param minor The minor version to inquire for support. (x in 0.x.0)
     * @return Returns true if the protocol version input is supported or not connected, false
     * otherwise.
     */
    boolean isProtocolVersionSupported(final int major, final int minor);

    /**
     * Sets the default password for this connection.
     *
     * @param password The main password for this connection.
     */
    void setDefaultPassword(final CharSequence password);
}
