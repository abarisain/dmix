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

import org.a0z.mpd.exception.MPDConnectionException;
import org.a0z.mpd.exception.MPDNoResponseException;
import org.a0z.mpd.exception.MPDServerException;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class representing a connection to MPD Server.
 */
public abstract class MPDConnection {

    public static final String TAG = "MPDConnection";

    public static final String MPD_RESPONSE_ERR = "ACK";

    public static final String POOL_THREAD_NAME_PREFIX = "pool";

    private static final int CONNECTION_TIMEOUT = 10000;

    private static final String MPD_RESPONSE_OK = "OK";

    private static final String MPD_CMD_START_BULK = "command_list_begin";

    private static final String MPD_CMD_START_BULK_OK = "command_list_ok_begin";

    private static final String MPD_CMD_BULK_SEP = "list_OK";

    private static final String MPD_CMD_END_BULK = "command_list_end";

    private static final int MAX_CONNECT_RETRY = 3;

    private static final int MAX_REQUEST_RETRY = 3;

    private final InetAddress hostAddress;

    private final int hostPort;

    private final int readWriteTimeout;

    private final ExecutorService executor;

    private final int maxThreads;

    protected boolean cancelled = false;

    private int[] mpdVersion;

    private List<MPDCommand> commandQueue;

    private boolean albumGroupingSupported = false;

    private String password = null;

    MPDConnection(InetAddress server, int port, int readWriteTimeout, int maxConnections,
            String password) throws MPDServerException {
        this.readWriteTimeout = readWriteTimeout;
        hostPort = port;
        hostAddress = server;
        commandQueue = new ArrayList<>();
        maxThreads = maxConnections;
        executor = Executors.newFixedThreadPool(maxThreads);
        this.password = password;
    }


    MPDConnection(InetAddress server, int port, String password, int readWriteTimeout)
            throws MPDServerException {
        this(server, port, readWriteTimeout, 1, password);
    }

    static List<String[]> separatedQueueResults(List<String> lines) {
        List<String[]> result = new ArrayList<>();
        ArrayList<String> lineCache = new ArrayList<>();

        for (String line : lines) {
            if (line.equals(MPD_CMD_BULK_SEP)) { // new part
                if (!lineCache.isEmpty()) {
                    result.add(lineCache.toArray(new String[lineCache.size()]));
                    lineCache.clear();
                }
            } else {
                lineCache.add(line);
            }
        }
        if (!lineCache.isEmpty()) {
            result.add(lineCache.toArray(new String[lineCache.size()]));
        }
        return result;
    }

    protected final int[] connect() throws MPDServerException {

        int[] result = null;
        int retry = 0;
        MPDServerException lastException = null;

        while (result == null && retry < MAX_CONNECT_RETRY && !cancelled) {
            try {
                result = innerConnect();
            } catch (final MPDServerException e1) {
                lastException = e1;
                try {
                    Thread.sleep(500L);
                } catch (final InterruptedException ignored) {
                }
            } catch (final Exception e2) {
                lastException = new MPDServerException(e2);
            }
            retry++;
        }

        if (result != null) {
            mpdVersion = result;
            return result;
        } else {
            if (lastException == null) {
                lastException = new MPDServerException("Connection request canceled");
            }
            throw new MPDServerException(lastException);
        }
    }

    void disconnect() throws MPDServerException {
        this.cancelled = true;
        // executor.shutdown();
        innerDisconnect();
    }

    public InetAddress getHostAddress() {
        return hostAddress;
    }

    public int getHostPort() {
        return hostPort;
    }

    protected abstract InputStreamReader getInputStream();

    protected abstract void setInputStream(InputStreamReader inputStream);

    int[] getMpdVersion() {
        return mpdVersion;
    }

    protected abstract OutputStreamWriter getOutputStream();

    protected abstract void setOutputStream(OutputStreamWriter outputStream);

    protected abstract Socket getSocket();

    protected abstract void setSocket(Socket socket);

    private void handleConnectionFailure(MPDCommandResult result, MPDServerException ex) {
        try {
            result.setLastexception(ex);
            try {
                Thread.sleep(500L);
            } catch (final InterruptedException ignored) {
            }
            innerConnect();
            refreshAllConnections();
        } catch (final MPDServerException e) {
            result.setLastexception(e);
        }
    }

    private int[] innerConnect() throws MPDServerException {

        if (getSocket() != null) { // Always release existing socket if any
            // before creating a new one
            try {
                innerDisconnect();
            } catch (final MPDServerException ignored) {
                // ok, don't care about any exception here
            }
        }
        try {
            setSocket(new Socket());
            getSocket().setSoTimeout(readWriteTimeout);
            getSocket().connect(new InetSocketAddress(hostAddress, hostPort), CONNECTION_TIMEOUT);
            BufferedReader in = new BufferedReader(new InputStreamReader(getSocket()
                    .getInputStream()), 1024);
            String line = in.readLine();

            if (line == null) {
                throw new MPDServerException("No response from server");
            } else if (line.startsWith(MPD_RESPONSE_OK)) {
                String[] tmp = line.substring((MPD_RESPONSE_OK + " MPD ").length(), line.length())
                        .split("\\.");
                int[] result = new int[tmp.length];

                for (int i = 0; i < tmp.length; i++) {
                    result[i] = Integer.parseInt(tmp[i]);
                }

                // Use UTF-8 when needed
                if (result[0] > 0 || result[1] >= 10) {
                    setOutputStream(new OutputStreamWriter(getSocket().getOutputStream(), "UTF-8"));
                    setInputStream(new InputStreamReader(getSocket().getInputStream(), "UTF-8"));
                } else {
                    setOutputStream(new OutputStreamWriter(getSocket().getOutputStream()));
                    setInputStream(new InputStreamReader(getSocket().getInputStream()));
                }

                // MPD 0.19 supports album grouping
                if (result[0] > 0 || result[1] >= 19) {
                    albumGroupingSupported = true;
                } else {
                    albumGroupingSupported = false;
                }

                if (password != null) {
                    password(password);
                }
                return result;
            } else if (line.startsWith(MPD_RESPONSE_ERR)) {
                throw new MPDServerException(line);
            } else {
                throw new MPDServerException("Bogus response from server");
            }

        } catch (final IOException e) {
            throw new MPDConnectionException(e);
        }
    }

    void innerDisconnect() throws MPDServerException {
        if (innerIsConnected()) {
            try {
                getSocket().close();
                setSocket(null);
            } catch (final IOException e) {
                throw new MPDConnectionException(e.getMessage(), e);
            }
        }
    }

    public boolean innerIsConnected() {
        return (getSocket() != null && getSocket().isConnected() && !getSocket().isClosed());
    }

    private List<String> innerSyncedWriteAsyncRead(MPDCommand command)
            throws MPDServerException {
        ArrayList<String> result = new ArrayList<>();
        try {
            writeToServer(command);
        } catch (final IOException e) {
            throw new MPDConnectionException(e);
        }
        boolean dataReaded = false;
        while (!dataReaded) {
            try {
                result = readFromServer();
                dataReaded = true;
            } catch (final SocketTimeoutException e) {
                Log.w(TAG, "Socket timeout while reading server response : " + e);
            } catch (final IOException e) {
                throw new MPDConnectionException(e);
            }
        }
        return result;
    }

    private List<String> innerSyncedWriteRead(MPDCommand command)
            throws MPDServerException {
        ArrayList<String> result = new ArrayList<>();
        if (!isConnected()) {
            throw new MPDConnectionException("No connection to server");
        }

        // send command
        try {
            writeToServer(command);
        } catch (final IOException e1) {
            throw new MPDConnectionException(e1);
        }
        try {
            result = readFromServer();
            return result;
        } catch (final MPDConnectionException e) {
            if (command.command.equals(MPDCommand.MPD_CMD_CLOSE)) {
                return result;// we sent close command, so don't care about
            }
            // Exception while wrong to read response
            else {
                throw e;
            }
        } catch (final IOException e) {
            throw new MPDConnectionException(e);
        }
    }

    public boolean isAlbumGroupingSupported() {
        return albumGroupingSupported;
    }

    public boolean isConnected() {
        return !cancelled;
    }

    /**
     * Authenticate using password.
     *
     * @param password password.
     * @throws MPDServerException if an error occur while contacting server.
     */
    protected void password(String password) throws MPDServerException {
        this.password = password;
        sendCommand(MPDCommand.MPD_CMD_PASSWORD, password);
    }

    private List<String> processRequest(MPDCommand command) throws MPDServerException {

        MPDCommandResult result;

        try {
            // Bypass thread pool queue if the thread already comes from the
            // pool
            // to avoid deadlock
            if (Thread.currentThread().getName().startsWith(POOL_THREAD_NAME_PREFIX)) {
                result = new MpdCallable(command).call();
            } else {
                result = executor.submit(new MpdCallable(command)).get();
            }
        } catch (final Exception e) {
            throw new MPDServerException(e);
        }
        if (result.getResult() != null) {
            return result.getResult();
        } else if (cancelled) {
            throw new MPDConnectionException("The MPD request has been canceled");
        } else {
            throw result.getLastexception();
        }
    }

    public void queueCommand(MPDCommand command) {
        commandQueue.add(command);
    }

    public void queueCommand(String command, String... args) {
        queueCommand(new MPDCommand(command, args));
    }

    private ArrayList<String> readFromServer() throws MPDServerException, IOException {
        ArrayList<String> result = new ArrayList<>();
        BufferedReader in = new BufferedReader(getInputStream(), 1024);

        boolean dataReaded = false;
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            dataReaded = true;
            if (line.startsWith(MPD_RESPONSE_OK)) {
                break;
            }
            if (line.startsWith(MPD_RESPONSE_ERR)) {

                if (line.contains("permission")) {
                    throw new MPDConnectionException("MPD Permission failure : "
                            + line);
                } else {
                    throw new MPDServerException(line);
                }
            }
            result.add(line);
        }
        if (!dataReaded) {
            // Close socket if there is no response... Something is wrong
            // (e.g.
            // MPD shutdown..)
            throw new MPDNoResponseException("Connection lost");
        }
        return result;
    }

    private void refreshAllConnections() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < maxThreads; i++) {
                    try {
                        processRequest(new MPDCommand(MPDCommand.MPD_CMD_PING));
                    } catch (final MPDServerException e) {
                        Log.w(TAG, "All connection refresh failure.", e);
                    }
                }
            }
        }).start();
    }

    List<String> sendAsyncCommand(MPDCommand command)
            throws MPDServerException {
        return syncedWriteAsyncRead(command);
    }

    List<String> sendAsyncCommand(String command, String... args)
            throws MPDServerException {
        return sendAsyncCommand(new MPDCommand(command, args));
    }

    public List<String> sendCommand(MPDCommand command) throws MPDServerException {
        return sendRawCommand(command);
    }

    public List<String> sendCommand(String command, String... args) throws MPDServerException {
        return sendCommand(new MPDCommand(command, args));
    }

    public List<String> sendCommandQueue() throws MPDServerException {
        return sendCommandQueue(false);
    }

    List<String> sendCommandQueue(boolean withSeparator) throws MPDServerException {
        String commandstr = (withSeparator ? MPD_CMD_START_BULK_OK : MPD_CMD_START_BULK) + '\n';
        for (MPDCommand command : commandQueue) {
            commandstr += command.toString();
        }
        commandstr += MPD_CMD_END_BULK;
        commandQueue = new ArrayList<>();
        return sendRawCommand(new MPDCommand(commandstr));
    }

    public List<String[]> sendCommandQueueSeparated() throws MPDServerException {
        return separatedQueueResults(sendCommandQueue(true));
    }

    public List<String> sendRawCommand(MPDCommand command) throws MPDServerException {
        return syncedWriteRead(command);
    }

    private List<String> syncedWriteAsyncRead(MPDCommand command) throws MPDServerException {
        command.setSynchronous(false);
        return processRequest(command);
    }

    private List<String> syncedWriteRead(MPDCommand command) throws MPDServerException {
        command.setSynchronous(true);
        return processRequest(command);
    }

    private void writeToServer(MPDCommand command) throws IOException {
        final String cmdString = command.toString();
        // Uncomment for extreme command debugging
        //Log.v(TAG, "Sending MPDCommand : " + cmdString);
        getOutputStream().write(cmdString);
        getOutputStream().flush();
        command.setSentToServer(true);
    }

    class MpdCallable extends MPDCommand implements Callable<MPDCommandResult> {

        private int retry = 0;

        public MpdCallable(MPDCommand mpdCommand) {
            super(mpdCommand.command, mpdCommand.args, mpdCommand.isSynchronous());
        }

        @Override
        public MPDCommandResult call() throws Exception {
            boolean retryable = true;
            MPDCommandResult result = new MPDCommandResult();

            while (result.getResult() == null && retry < MAX_REQUEST_RETRY && !cancelled
                    && retryable) {
                try {
                    if (!innerIsConnected()) {
                        innerConnect();
                    }
                    if (isSynchronous()) {
                        result.setResult(innerSyncedWriteRead(this));
                    } else {
                        result.setResult(innerSyncedWriteAsyncRead(this));
                    }
                    // Do not fail when the IDLE response has not been read (to
                    // improve connection failure robustness)
                    // Just send the "changed playlist" result to force the MPD
                    // status to be refreshed.
                } catch (final MPDNoResponseException ex0) {
                    this.setSentToServer(false);
                    handleConnectionFailure(result, ex0);
                    if (command.equals(MPDCommand.MPD_CMD_IDLE)) {
                        result.setResult(Collections.singletonList("changed: playlist"));
                    }
                } catch (final MPDServerException ex1) {
                    // Avoid getting in an infinite loop if an error occurred in the password cmd
                    if (ex1.getErrorKind() == MPDServerException.ErrorKind.PASSWORD) {
                        result.setLastexception(new MPDServerException(
                                "Wrong password"));
                    } else {
                        handleConnectionFailure(result, ex1);
                    }
                }
                retryable = isRetryable(command) || !this.isSentToServer();
                retry++;
            }

            if (result.getResult() == null) {
                if (cancelled) {
                    result.setLastexception(new MPDConnectionException(
                            "MPD request has been cancelled for disconnection"));
                }
                Log.e(TAG, "MPD command " + command + " failed after " + retry + " attempts : "
                        + result.getLastexception().getMessage());
            }
            return result;
        }
    }

    class MPDCommandResult {

        private MPDServerException lastexception = null;

        private List<String> result = null;

        public MPDServerException getLastexception() {
            return lastexception;
        }

        public void setLastexception(MPDServerException lastexception) {
            this.lastexception = lastexception;
        }

        public List<String> getResult() {
            return result;
        }

        public void setResult(List<String> result) {
            this.result = result;
        }
    }
}
