/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2014 The MPDroid Project
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

package org.a0z.mpd.connection;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * Class representing a connection to MPD Server.
 */
public class MPDConnectionMultiSocket extends MPDConnection {

    private static final ThreadLocal<InputStreamReader> INPUT_STREAM = new ThreadLocal<>();

    private static final ThreadLocal<OutputStreamWriter> OUTPUT_STREAM = new ThreadLocal<>();

    private static final ThreadLocal<Socket> SOCKET = new ThreadLocal<>();

    public MPDConnectionMultiSocket(final int readWriteTimeout, final int maxConnection) {
        super(readWriteTimeout, maxConnection);
    }

    @Override
    public InputStreamReader getInputStream() {
        return INPUT_STREAM.get();
    }

    @Override
    public OutputStreamWriter getOutputStream() {
        return OUTPUT_STREAM.get();
    }

    @Override
    protected Socket getSocket() {
        return SOCKET.get();
    }

    @Override
    public void setInputStream(final InputStreamReader inputStream) {
        INPUT_STREAM.set(inputStream);
    }

    @Override
    public void setOutputStream(final OutputStreamWriter outputStream) {
        OUTPUT_STREAM.set(outputStream);
    }

    @Override
    protected void setSocket(final Socket socket) {
        SOCKET.set(socket);
    }
}
