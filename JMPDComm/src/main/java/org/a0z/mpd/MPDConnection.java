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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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

    private static final int MAX_REQUEST_RETRY = 3;

    private static final Pattern PERIOD_DELIMITER = Pattern.compile("\\.");

    private final ThreadPoolExecutor mExecutor;

    private final int mReadWriteTimeout;

    private InetSocketAddress mSocketAddress;

    private boolean mCancelled = false;

    private boolean mIsConnected = false;

    private int[] mMPDVersion = null;

    private String mPassword = null;

    MPDConnection(final int readWriteTimeout, final int maxConnections) {
        super();

        mReadWriteTimeout = readWriteTimeout;
        mExecutor = new ThreadPoolExecutor(1, maxConnections, (long) mReadWriteTimeout,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        mExecutor.prestartCoreThread();
        if (maxConnections > 1) {
            mExecutor.allowCoreThreadTimeOut(true);
        }
    }

    final void connect(final InetAddress server, final int port, final String password)
            throws MPDServerException {
        innerDisconnect();

        mCancelled = false;
        mPassword = password;
        mSocketAddress = new InetSocketAddress(server, port);

        final MPDCommand mpdCommand = new MPDCommand(MPDCommand.MPD_CMD_PING);
        final String result = processRequest(mpdCommand).getConnectionResult();

        if (result == null) {
            throw new MPDServerException("Failed initial connection.");
        }

        mIsConnected = true;
        setMPDVersion(result);
    }

    void disconnect() throws MPDConnectionException {
        mCancelled = true;
        innerDisconnect();
    }

    InetAddress getHostAddress() {
        return mSocketAddress.getAddress();
    }

    int getHostPort() {
        return mSocketAddress.getPort();
    }

    protected abstract InputStreamReader getInputStream();

    protected abstract void setInputStream(InputStreamReader inputStream);

    int[] getMPDVersion() {
        return mMPDVersion.clone();
    }

    private void setMPDVersion(final String response) {
        final String formatResponse = response.substring((MPD_RESPONSE_OK + " MPD ").length());
        final String[] tmp = PERIOD_DELIMITER.split(formatResponse);
        final int[] version = new int[tmp.length];

        for (int i = 0; i < tmp.length; i++) {
            version[i] = Integer.parseInt(tmp[i]);
        }

        mMPDVersion = version;
    }

    protected abstract OutputStreamWriter getOutputStream();

    protected abstract void setOutputStream(OutputStreamWriter outputStream);

    protected abstract Socket getSocket();

    protected abstract void setSocket(Socket socket);

    boolean isConnected() {
        return mIsConnected;
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

    boolean isProtocolVersionSupported(final int major, final int minor) {
        final boolean result;

        if (mMPDVersion == null || mMPDVersion[0] <= major && mMPDVersion[1] < minor) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }

    private CommandResult processRequest(final MPDCommand command) throws MPDServerException {
        final CommandResult result;
        final List<String> commandResult;

        // Bypass thread pool queue if the thread already comes from the pool to avoid deadlock.
        if (Thread.currentThread().getName().startsWith(POOL_THREAD_NAME_PREFIX)) {
            result = new MPDCallable(command).call();
        } else {
            try {
                result = mExecutor.submit(new MPDCallable(command)).get();
                // Spam the log with the largest pool size
                //Log.d(TAG, "Largest pool size: " + mExecutor.getLargestPoolSize());
            } catch (final ExecutionException | InterruptedException e) {
                throw new MPDServerException(e);
            }
        }

        commandResult = result.getResult();

        if (commandResult == null) {
            if (mCancelled) {
                throw new MPDConnectionException("The MPD request has been cancelled");
            }

            throw result.getLastException();
        }

        return result;
    }

    List<String> sendCommand(final MPDCommand command) throws MPDServerException {
        return syncedWriteRead(command).getResult();
    }

    List<String> sendCommand(final String command, final String... args) throws MPDServerException {
        return sendCommand(new MPDCommand(command, args));
    }

    private CommandResult syncedWriteRead(final MPDCommand command) throws MPDServerException {
        return processRequest(command);
    }

    private class MPDCallable implements Callable<CommandResult> {

        private final MPDCommand mCallableCommand;

        private boolean mIsCommandSent;

        MPDCallable(final MPDCommand mpdCommand) {
            super();

            mCallableCommand = mpdCommand;
        }

        @Override
        public final CommandResult call() {
            int retryCount = 0;
            final CommandResult result = new CommandResult();

            while (result.getResult() == null && retryCount < MAX_REQUEST_RETRY && !mCancelled) {
                try {
                    if (!isInnerConnected()) {
                        result.setConnectionResult(innerConnect());
                    }

                    result.setResult(innerSyncedWriteRead());
                } catch (final MPDNoResponseException ex0) {
                    mIsConnected = false;
                    mIsCommandSent = false;
                    handleConnectionFailure(result, ex0);
                    // Do not fail when the IDLE response has not been read (to improve connection
                    // failure robustness). Just send the "changed playlist" result to force the MPD
                    // status to be refreshed.
                    if (MPDCommand.MPD_CMD_IDLE.equals(mCallableCommand.mCommand)) {
                        result.setResult(Collections.singletonList(
                                "changed: " + MPDStatusMonitor.IDLE_PLAYLIST));
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

                /** On successful send of non-retryable command, break out. */
                if (!MPDCommand.isRetryable(mCallableCommand.mCommand) &&
                        mIsCommandSent) {
                    break;
                }

                retryCount++;
            }

            if (result.getResult() == null) {
                if (mCancelled) {
                    mIsConnected = false;
                    result.setLastException(new MPDConnectionException(
                            "MPD request has been cancelled for disconnection."));
                }
                Log.e(TAG, "MPD command " + mCallableCommand.mCommand + " failed after " +
                        retryCount + " attempts.", result.getLastException());
            } else {
                mIsConnected = true;
            }
            return result;
        }

        private void handleConnectionFailure(final CommandResult result,
                final MPDServerException ex) {

            result.setLastException(ex);
            try {
                Thread.sleep(500L);
            } catch (final InterruptedException ignored) {
            }

            try {
                innerConnect();
            } catch (final MPDServerException e) {
                result.setLastException(e);
            }
        }

        private String innerConnect() throws MPDServerException {
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
                getSocket().connect(mSocketAddress, CONNECTION_TIMEOUT);
                setInputStream(new InputStreamReader(getSocket().getInputStream(), "UTF-8"));
                final BufferedReader in = new BufferedReader(getInputStream(), DEFAULT_BUFFER_SIZE);
                setOutputStream(new OutputStreamWriter(getSocket().getOutputStream(), "UTF-8"));
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

            if (mPassword != null) {
                sendCommand(MPDCommand.MPD_CMD_PASSWORD, mPassword);
            }

            return line;
        }

        private List<String> innerSyncedWriteRead() throws MPDServerException {
            List<String> result = new ArrayList<>();
            if (mCancelled) {
                throw new MPDConnectionException("No connection to server");
            }

            try {
                writeToServer();
                result = readFromServer();
            } catch (final MPDConnectionException e) {
                if (!mCallableCommand.mCommand.equals(MPDCommand.MPD_CMD_CLOSE)) {
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
                    if (line.contains(MPDCommand.MPD_CMD_PERMISSION)) {
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

        private void writeToServer() throws IOException {
            final String cmdString = mCallableCommand.toString();
            // Uncomment for extreme command debugging
            //Log.v(TAG, "Sending MPDCommand : " + cmdString);
            getOutputStream().write(cmdString);
            getOutputStream().flush();
            mIsCommandSent = true;
        }
    }
}
