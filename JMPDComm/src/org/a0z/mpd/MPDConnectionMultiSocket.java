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

    MPDConnectionMultiSocket(InetAddress server, int port, int maxConnection, String password,
            int readWriteTimeout) throws MPDServerException {
        super(server, port, readWriteTimeout, maxConnection, password);
    }

    @Override
    public InputStreamReader getInputStream() {
        return this.inputstream.get();
    }

    @Override
    public OutputStreamWriter getOutputStream() {
        return this.outputstream.get();
    }

    @Override
    protected Socket getSocket() {
        return socket.get();
    }

    @Override
    public void setInputStream(InputStreamReader inputStream) {
        this.inputstream.set(inputStream);
    }

    @Override
    public void setOutputStream(OutputStreamWriter outputStream) {
        this.outputstream.set(outputStream);
    }

    @Override
    protected void setSocket(Socket sock) {
        socket.set(sock);
    }
}
