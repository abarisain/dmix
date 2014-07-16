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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Class representing a connection to MPD Server.
 */
abstract class MPDConnection {

    private static final String MPD_RESPONSE_ERR = "ACK";

    private static final String TAG = "MPDConnection";

    private static final String POOL_THREAD_NAME_PREFIX = "pool";

    private static final int CONNECTION_TIMEOUT = 10000;

    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private static final String MPD_RESPONSE_OK = "OK";

    private static final String MPD_CMD_START_BULK = "command_list_begin";

    private static final String MPD_CMD_START_BULK_OK = "command_list_ok_begin";

    private static final String MPD_CMD_BULK_SEP = "list_OK";

    private static final String MPD_CMD_END_BULK = "command_list_end";

    private static final int MAX_CONNECT_RETRY = 3;

    private static final int MAX_REQUEST_RETRY = 3;

    private static final Pattern PERIOD_DELIMITER = Pattern.compile("\\.");

    private final List<MPDCommand> mCommandQueue;

    private final ExecutorService mExecutor;

    private final InetAddress mHostAddress;

    private final int mHostPort;

    private final int mMaxThreads;

    private final int mReadWriteTimeout;

    private boolean mIsAlbumGroupingSupported = false;

    private boolean mCancelled = false;

    private int mCommandQueueStringLength;

    private boolean mIsConnected = false;

    private int[] mMPDVersion = null;

    private String mPassword = null;

    MPDConnection(final InetAddress server, final int port, final int readWriteTimeout,
            final int maxConnections, final String password) {
        super();
        mReadWriteTimeout = readWriteTimeout;
        mHostPort = port;
        mHostAddress = server;
        mCommandQueue = new ArrayList<>();
        mCommandQueueStringLength = MPD_CMD_START_BULK_OK.length() + MPD_CMD_END_BULK.length() + 5;
        mMaxThreads = maxConnections;
        mExecutor = Executors.newFixedThreadPool(mMaxThreads);
        mPassword = password;
    }


    MPDConnection(final InetAddress server, final int port, final String password,
            final int readWriteTimeout) {
        this(server, port, readWriteTimeout, 1, password);
    }

    private static List<String[]> separatedQueueResults(final Iterable<String> lines) {
        final List<String[]> result = new ArrayList<>();
        final ArrayList<String> lineCache = new ArrayList<>();

        for (final String line : lines) {
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

    final void connect() throws MPDServerException {
        int[] result = null;
        int retry = 0;
        MPDServerException lastException = null;

        while (result == null && retry < MAX_CONNECT_RETRY && !mCancelled) {
            try {
                result = innerConnect();
            } catch (final MPDServerException e) {
                lastException = e;
                try {
                    Thread.sleep(500L);
                } catch (final InterruptedException ignored) {
                }
            } catch (final RuntimeException e) {
                lastException = new MPDServerException(e);
            }
            retry++;
        }

        if (result == null) {
            if (lastException == null) {
                throw new MPDServerException("Connection request cancelled.");
            } else {
                throw new MPDServerException(lastException);
            }
        }
        mIsConnected = true;
        mMPDVersion = result;
    }

    void disconnect() throws MPDConnectionException {
        mCancelled = true;
        innerDisconnect();
    }

    InetAddress getHostAddress() {
        return mHostAddress;
    }

    int getHostPort() {
        return mHostPort;
    }

    protected abstract InputStreamReader getInputStream();

    protected abstract void setInputStream(InputStreamReader inputStream);

    int[] getMPDVersion() {
        return mMPDVersion.clone();
    }

    protected abstract OutputStreamWriter getOutputStream();

    protected abstract void setOutputStream(OutputStreamWriter outputStream);

    protected abstract Socket getSocket();

    protected abstract void setSocket(Socket socket);

    private int[] innerConnect() throws MPDServerException {
        final int[] result;
        final String line;

        // Always release existing socket if any before creating a new one
        if (getSocket() != null) {
            try {
                innerDisconnect();
            } catch (final MPDServerException ignored) {
            }
        }

        try {
            setSocket(new Socket());
            getSocket().setSoTimeout(mReadWriteTimeout);
            getSocket().connect(new InetSocketAddress(mHostAddress, mHostPort), CONNECTION_TIMEOUT);
            final BufferedReader in = new BufferedReader(new InputStreamReader(getSocket()
                    .getInputStream()), DEFAULT_BUFFER_SIZE);
            line = in.readLine();
        } catch (final IOException e) {
            throw new MPDConnectionException(e);
        }

        if (line == null) {
            throw new MPDServerException("No response from server.");
        }

        if (line.startsWith(MPD_RESPONSE_ERR)) {
            throw new MPDServerException(line);
        }

        if (!line.startsWith(MPD_RESPONSE_OK)) {
            throw new MPDServerException("Bogus response from server.");
        }

        final String response = line.substring((MPD_RESPONSE_OK + " MPD ").length());
        final String[] tmp = PERIOD_DELIMITER.split(response);
        result = new int[tmp.length];

        for (int i = 0; i < tmp.length; i++) {
            result[i] = Integer.parseInt(tmp[i]);
        }

        try {
            // Use UTF-8 when needed
            if (result[0] > 0 || result[1] >= 10) {
                setOutputStream(new OutputStreamWriter(getSocket().getOutputStream(), "UTF-8"));
                setInputStream(new InputStreamReader(getSocket().getInputStream(), "UTF-8"));
            } else {
                setOutputStream(new OutputStreamWriter(getSocket().getOutputStream()));
                setInputStream(new InputStreamReader(getSocket().getInputStream()));
            }
        } catch (final IOException e) {
            throw new MPDConnectionException(e);
        }

        // MPD 0.19 supports album grouping
        if (result[0] > 0 || result[1] >= 19) {
            mIsAlbumGroupingSupported = true;
        } else {
            mIsAlbumGroupingSupported = false;
        }

        if (mPassword != null) {
            password(mPassword);
        }

        return result;
    }

    private void innerDisconnect() throws MPDConnectionException {
        mIsConnected = false;
        if (isInnerConnected()) {
            try {
                getSocket().close();
                setSocket(null);
            } catch (final IOException e) {
                throw new MPDConnectionException(e.getMessage(), e);
            }
        }
    }

    private boolean isInnerConnected() {
        return getSocket() != null && getSocket().isConnected() && !getSocket().isClosed();
    }

    boolean isAlbumGroupingSupported() {
        return mIsAlbumGroupingSupported;
    }

    boolean isConnected() {
        return mIsConnected;
    }

    /**
     * Authenticate using password.
     *
     * @param password password.
     * @throws MPDServerException if an error occur while contacting server.
     */
    void password(final String password) throws MPDServerException {
        mPassword = password;
        sendCommand(MPDCommand.MPD_CMD_PASSWORD, password);
    }

    private List<String> processRequest(final MPDCommand command) throws MPDServerException {
        final MPDCommandResult result;
        final List<String> commandResult;

        try {
            // Bypass thread pool queue if the thread already comes from the pool to avoid deadlock.
            if (Thread.currentThread().getName().startsWith(POOL_THREAD_NAME_PREFIX)) {
                result = new MPDCallable(command).call();
            } else {
                result = mExecutor.submit(new MPDCallable(command)).get();
            }
        } catch (final ExecutionException | InterruptedException e) {
            throw new MPDServerException(e);
        }

        commandResult = result.getResult();

        if (commandResult == null) {
            if (mCancelled) {
                throw new MPDConnectionException("The MPD request has been cancelled");
            }

            throw result.getLastException();
        }

        return commandResult;
    }

    void queueCommand(final MPDCommand command) {
        mCommandQueue.add(command);
        mCommandQueueStringLength += command.toString().length();
    }

    void queueCommand(final String command, final String... args) {
        queueCommand(new MPDCommand(command, args));
    }

    List<String> sendAsyncCommand(final MPDCommand command) throws MPDServerException {
        return syncedWriteAsyncRead(command);
    }

    List<String> sendAsyncCommand(final String command, final String... args)
            throws MPDServerException {
        return sendAsyncCommand(new MPDCommand(command, args));
    }

    List<String> sendCommand(final MPDCommand command) throws MPDServerException {
        return sendRawCommand(command);
    }

    List<String> sendCommand(final String command, final String... args)
            throws MPDServerException {
        return sendCommand(new MPDCommand(command, args));
    }

    List<String> sendCommandQueue() throws MPDServerException {
        return sendCommandQueue(false);
    }

    List<String> sendCommandQueue(final boolean withSeparator) throws MPDServerException {
        final StringBuilder commandString = new StringBuilder(mCommandQueueStringLength);

        if (withSeparator) {
            commandString.append(MPD_CMD_START_BULK_OK);
        } else {
            commandString.append(MPD_CMD_START_BULK);
        }
        commandString.append('\n');

        for (final MPDCommand command : mCommandQueue) {
            commandString.append(command);
        }
        commandString.append(MPD_CMD_END_BULK);

        mCommandQueueStringLength = MPD_CMD_START_BULK_OK.length() + MPD_CMD_END_BULK.length() + 5;
        mCommandQueue.clear();

        return sendRawCommand(new MPDCommand(commandString.toString()));
    }

    List<String[]> sendCommandQueueSeparated() throws MPDServerException {
        return separatedQueueResults(sendCommandQueue(true));
    }

    private List<String> sendRawCommand(final MPDCommand command) throws MPDServerException {
        return syncedWriteRead(command);
    }

    private List<String> syncedWriteAsyncRead(final MPDCommand command) throws MPDServerException {
        command.setSynchronous(false);
        return processRequest(command);
    }

    private List<String> syncedWriteRead(final MPDCommand command) throws MPDServerException {
        command.setSynchronous(true);
        return processRequest(command);
    }

    private static class MPDCommandResult {

        private MPDServerException mLastException = null;

        private List<String> mResult = null;

        final MPDServerException getLastException() {
            return mLastException;
        }

        final void setLastException(final MPDServerException lastException) {
            mLastException = lastException;
        }

        final List<String> getResult() {
            /** No need, we already made the collection immutable on the way in. */
            //noinspection ReturnOfCollectionOrArrayField
            return mResult;
        }

        final void setResult(final List<String> result) {
            if (result == null) {
                mResult = null;
            } else {
                mResult = Collections.unmodifiableList(result);
            }
        }
    }

    private class MPDCallable extends MPDCommand implements Callable<MPDCommandResult> {

        private int mRetry = 0;

        MPDCallable(final MPDCommand mpdCommand) {
            super(mpdCommand.command, mpdCommand.args, mpdCommand.isSynchronous());
        }

        @Override
        public final MPDCommandResult call() {
            boolean retryable = true;
            final MPDCommandResult result = new MPDCommandResult();

            while (result.getResult() == null && mRetry < MAX_REQUEST_RETRY && !mCancelled
                    && retryable) {
                try {
                    if (!isInnerConnected()) {
                        innerConnect();
                    }
                    if (isSynchronous()) {
                        result.setResult(innerSyncedWriteRead(this));
                    } else {
                        result.setResult(innerSyncedWriteAsyncRead(this));
                    }
                    // Do not fail when the IDLE response has not been read (to improve connection
                    // failure robustness). Just send the "changed playlist" result to force the MPD
                    // status to be refreshed.
                } catch (final MPDNoResponseException ex0) {
                    mIsConnected = false;
                    setSentToServer(false);
                    handleConnectionFailure(result, ex0);
                    if (command.equals(MPDCommand.MPD_CMD_IDLE)) {
                        result.setResult(Collections.singletonList("changed: playlist"));
                    }
                } catch (final MPDServerException ex1) {
                    mIsConnected = false;
                    // Avoid getting in an infinite loop if an error occurred in the password cmd
                    if (ex1.getErrorKind() == MPDServerException.ErrorKind.PASSWORD) {
                        result.setLastException(new MPDServerException("Wrong password"));
                    } else {
                        handleConnectionFailure(result, ex1);
                    }
                }
                retryable = isRetryable(command) || !isSentToServer();
                mRetry++;
            }

            if (result.getResult() == null) {
                if (mCancelled) {
                    mIsConnected = false;
                    result.setLastException(new MPDConnectionException(
                            "MPD request has been cancelled for disconnection."));
                }
                Log.e(TAG, "MPD command " + command + " failed after " + mRetry + " attempts.",
                        result.getLastException());
            } else {
                mIsConnected = true;
            }
            return result;
        }

        private void handleConnectionFailure(final MPDCommandResult result,
                final MPDServerException ex) {

            result.setLastException(ex);
            try {
                Thread.sleep(500L);
            } catch (final InterruptedException ignored) {
            }

            try {
                innerConnect();
                refreshAllConnections();
            } catch (final MPDServerException e) {
                result.setLastException(e);
            }
        }

        private List<String> innerSyncedWriteAsyncRead(final MPDCommand command)
                throws MPDServerException {
            List<String> result = new ArrayList<>();
            try {
                writeToServer(command);
            } catch (final IOException e) {
                throw new MPDConnectionException(e);
            }
            boolean dataReadFromServer = false;
            while (!dataReadFromServer) {
                try {
                    result = readFromServer();
                    dataReadFromServer = true;
                } catch (final SocketTimeoutException e) {
                    Log.w(TAG, "Socket timeout while reading server response.", e);
                } catch (final IOException e) {
                    throw new MPDConnectionException(e);
                }
            }
            return result;
        }

        private List<String> innerSyncedWriteRead(final MPDCommand command)
                throws MPDServerException {
            List<String> result = new ArrayList<>();
            if (mCancelled) {
                throw new MPDConnectionException("No connection to server");
            }

            // send command
            try {
                writeToServer(command);
            } catch (final IOException e) {
                throw new MPDConnectionException(e);
            }

            try {
                result = readFromServer();
            } catch (final MPDConnectionException e) {
                if (!command.command.equals(MPDCommand.MPD_CMD_CLOSE)) {
                    throw e;
                }
            } catch (final IOException e) {
                throw new MPDConnectionException(e);
            }

            return result;
        }

        private List<String> readFromServer() throws MPDServerException, IOException {
            final List<String> result = new ArrayList<>();
            final BufferedReader in = new BufferedReader(getInputStream(), DEFAULT_BUFFER_SIZE);

            boolean serverDataRead = false;
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                serverDataRead = true;
                if (line.startsWith(MPD_RESPONSE_OK)) {
                    break;
                }
                if (line.startsWith(MPD_RESPONSE_ERR)) {

                    if (line.contains("permission")) {
                        throw new MPDConnectionException("MPD Permission failure : " + line);
                    } else {
                        throw new MPDServerException(line);
                    }
                }
                result.add(line);
            }
            if (!serverDataRead) {
                // Close socket if there is no response...
                // Something is wrong (e.g. MPD shutdown..)
                throw new MPDNoResponseException("Connection lost");
            }
            return result;
        }

        private void refreshAllConnections() {
            new Thread(mPingAllConnections).start();
        }

        private final Runnable mPingAllConnections = new Runnable() {
            @Override
            public void run() {
                final MPDCommand ping = new MPDCommand(MPDCommand.MPD_CMD_PING);

                for (int i = 0; i < mMaxThreads; i++) {
                    try {
                        processRequest(ping);
                    } catch (final MPDServerException e) {
                        Log.w(TAG, "All connection refresh failure.", e);
                    }
                }
            }
        };

        private void writeToServer(final MPDCommand command) throws IOException {
            final String cmdString = command.toString();
            // Uncomment for extreme command debugging
            //Log.v(TAG, "Sending MPDCommand : " + cmdString);
            getOutputStream().write(cmdString);
            getOutputStream().flush();
            command.setSentToServer(true);
        }


    }
}
