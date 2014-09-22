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

import org.a0z.mpd.Log;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.MPDStatusMonitor;
import org.a0z.mpd.exception.MPDConnectionException;
import org.a0z.mpd.exception.MPDNoResponseException;
import org.a0z.mpd.exception.MPDServerException;

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
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Class representing a connection to MPD Server.
 */
public abstract class MPDConnection {

    private static final int CONNECTION_TIMEOUT = 10000;

    /** Default buffer size for the socket. */
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    /** Maximum number of times to attempt command processing. */
    private static final int MAX_REQUEST_RETRY = 3;

    private static final String MPD_RESPONSE_ERR = "ACK";

    private static final String MPD_RESPONSE_OK = "OK";

    private static final String POOL_THREAD_NAME_PREFIX = "pool";

    private static final String TAG = "MPDConnection";

    /** The {@code ExecutorService} used to process commands. */
    private final ThreadPoolExecutor mExecutor;

    /** The command communication timeout. */
    private final int mReadWriteTimeout;

    /** If set to true, this will cancel any processing commands at next opportunity. */
    private boolean mCancelled = false;

    /** User facing connection status. */
    private boolean mIsConnected = false;

    /** Current media server's major/minor/micro version. */
    private int[] mMPDVersion = null;

    /** Current media server password. */
    private String mPassword = null;

    /** The host/port pair used to connect to the media server. */
    private InetSocketAddress mSocketAddress;

    /**
     * The constructor method. This method does not connect to the server.
     *
     * @param readWriteTimeout The read write timeout for this connection.
     * @param maxConnections   Maximum number of sockets to allow running at one time.
     * @see #connect(java.net.InetAddress, int, String)
     */
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

    /**
     * Sets up connection to host/port pair with MPD password.
     *
     * @param host     The media server host to connect to.
     * @param port     The media server port to connect to.
     * @param password The MPD protocol password to pass upon connection.
     * @throws MPDServerException Thrown upon an error sending a simple command to the {@code
     *                            host}/{@code port} pair with the {@code password}.
     */
    public final void connect(final InetAddress host, final int port, final String password)
            throws MPDServerException {
        innerDisconnect();

        mCancelled = false;
        mPassword = password;
        mSocketAddress = new InetSocketAddress(host, port);

        final MPDCommand mpdCommand = new MPDCommand(MPDCommand.MPD_CMD_PING);
        final String result = processCommand(mpdCommand).getConnectionResult();

        if (result == null) {
            throw new MPDServerException("Failed initial connection.");
        }

        mIsConnected = true;
        setMPDVersion(result);
    }

    /**
     * The method to disconnect from the current connected server.
     *
     * @throws MPDConnectionException Thrown upon disconnection error.
     */
    public void disconnect() throws MPDConnectionException {
        mCancelled = true;
        innerDisconnect();
    }

    /**
     * The current connected media server host.
     *
     * @return The current connected media server host, null if not connected.
     */
    public InetAddress getHostAddress() {
        if (mSocketAddress == null) {
            throw new IllegalStateException("Connection endpoint not yet established.");
        }
        return mSocketAddress.getAddress();
    }

    /**
     * The current connected media server port.
     *
     * @return The current connected media server port, null if not connected.
     */
    public int getHostPort() {
        if (mSocketAddress == null) {
            throw new IllegalStateException("Connection endpoint not yet established.");
        }
        return mSocketAddress.getPort();
    }

    protected abstract InputStreamReader getInputStream();

    /**
     * The current MPD protocol version.
     */
    public int[] getMPDVersion() {
        return mMPDVersion.clone();
    }

    protected abstract OutputStreamWriter getOutputStream();

    protected abstract Socket getSocket();

    /**
     * A low level disconnect method for the socket(s).
     *
     * @throws MPDConnectionException Thrown if there is a problem closing the socket.
     */
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

    /**
     * A user facing connection inquiry method.
     *
     * @return True if socket(s) are connected, false otherwise.
     */
    public boolean isConnected() {
        return mIsConnected;
    }

    /**
     * A low level connection check for the socket(s). Don't rely on this for actual connection
     * status. Use {@code isConnected()} for actual connection status.
     *
     * @return True if the socket is supposed to be connected, false otherwise.
     */
    private boolean isInnerConnected() {
        return getSocket() != null && getSocket().isConnected() && !getSocket().isClosed();
    }

    /**
     * Checks the media server version for support of a feature. This does not check micro version
     * as new features shouldn't be added during stable releases.
     *
     * @param major The major version to inquire for support. (x in x.0.0)
     * @param minor The minor version to inquire for support. (x in 0.x.0)
     * @return Returns true if the protocol version input is supported or not connected, false
     * otherwise.
     */
    public boolean isProtocolVersionSupported(final int major, final int minor) {
        final boolean result;

        if (mMPDVersion == null || mMPDVersion[0] <= major && mMPDVersion[1] < minor) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }

    /**
     * Processes the command by setting up the command processor executor.
     *
     * @param command The command to be processed.
     * @return The response to the processed command.
     * @throws MPDServerException Thrown if there were communication problems, execution problems,
     *                            server side problems with the command or if the executor was
     *                            interrupted.
     */
    private CommandResult processCommand(final MPDCommand command) throws MPDServerException {
        final CommandResult result;
        final List<String> commandResult;

        // Bypass thread pool queue if the thread already comes from the pool to avoid deadlock.
        if (Thread.currentThread().getName().startsWith(POOL_THREAD_NAME_PREFIX)) {
            result = new CommandProcessor(command).call();
        } else {
            try {
                result = mExecutor.submit(new CommandProcessor(command)).get();
                // Spam the log with the largest pool size
                //Log.debug(TAG, "Largest pool size: " + mExecutor.getLargestPoolSize());
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

    /**
     * Communicates with the server by sending a command and receiving the response.
     *
     * @param command The command to be sent to the server.
     * @return The result from the command sent to the server.
     * @throws MPDServerException Thrown if there are errors sending the command to the server.
     */
    public List<String> sendCommand(final MPDCommand command) throws MPDServerException {
        return processCommand(command).getResult();
    }

    /**
     * Communicates with the server by sending a command and receiving the response.
     *
     * @param command The command to be sent to the server.
     * @param args    Arguments to the command to be sent to the server.
     * @return The result from the command sent to the server.
     * @throws MPDServerException Thrown if there are errors sending the command to the server.
     */
    public List<String> sendCommand(final String command, final String... args)
            throws MPDServerException {
        return sendCommand(new MPDCommand(command, args));
    }

    protected abstract void setInputStream(InputStreamReader inputStream);

    /**
     * Processes the {@code CommandResult} connection response to store the current media server
     * MPD protocol version.
     *
     * @param response The {@code CommandResult().getConnectionResponse()}.
     */
    private void setMPDVersion(final String response) {
        final String formatResponse = response.substring((MPD_RESPONSE_OK + " MPD ").length());

        final StringTokenizer stringTokenizer = new StringTokenizer(formatResponse, ".");
        final int[] version = new int[stringTokenizer.countTokens()];
        int i = 0;

        while (stringTokenizer.hasMoreElements()) {
            version[i] = Integer.parseInt(stringTokenizer.nextToken());
            i++;
        }

        mMPDVersion = version;
    }

    protected abstract void setOutputStream(OutputStreamWriter outputStream);

    protected abstract void setSocket(Socket socket);

    /** This class communicates with the server by sending the command and processing the result. */
    private class CommandProcessor implements Callable<CommandResult> {

        /** The command to be processed. */
        private final MPDCommand mCommand;

        /** The status of whether the command has been sent. */
        private boolean mIsCommandSent;

        CommandProcessor(final MPDCommand mpdCommand) {
            super();

            mCommand = mpdCommand;
        }

        /**
         * This is the default class method.
         *
         * @return A {@code CommandResult} from the processed command.
         */
        @Override
        public final CommandResult call() {
            int retryCount = 0;
            final CommandResult result = new CommandResult();

            while (result.getResult() == null && retryCount < MAX_REQUEST_RETRY && !mCancelled) {
                try {
                    if (!isInnerConnected()) {
                        result.setConnectionResult(innerConnect());
                    }

                    result.setResult(communicate());
                } catch (final MPDNoResponseException ex0) {
                    mIsConnected = false;
                    mIsCommandSent = false;
                    handleConnectionFailure(result, ex0);
                    // Do not fail when the IDLE response has not been read (to improve connection
                    // failure robustness). Just send the "changed playlist" result to force the MPD
                    // status to be refreshed.
                    if (MPDCommand.MPD_CMD_IDLE.equals(mCommand.getCommand())) {
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
                if (!MPDCommand.isRetryable(mCommand.getCommand()) &&
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
                Log.error(TAG, "MPD command " + mCommand.getCommand() + " failed after " +
                        retryCount + " attempts.", result.getLastException());
            } else {
                mIsConnected = true;
            }
            return result;
        }

        /**
         * This method processes the command and response from the command.
         *
         * @return A String list of responses to the sent command.
         * @throws MPDServerException Thrown if there is an error communicating with the media
         *                            server.
         */
        private List<String> communicate() throws MPDServerException {
            List<String> result = new ArrayList<>();
            if (mCancelled) {
                throw new MPDConnectionException("No connection to server");
            }

            try {
                write();
                result = read();
            } catch (final MPDConnectionException e) {
                if (!mCommand.getCommand().equals(MPDCommand.MPD_CMD_CLOSE)) {
                    throw e;
                }
            } catch (final IOException e) {
                throw new MPDConnectionException(e);
            }

            return result;
        }

        /**
         * Used after a server error, sleeps for a small time then tries to reconnect.
         *
         * @param result The {@code CommandResult} which stores the connection failure.
         * @param ex     The exception thrown to get to this method.
         */
        private void handleConnectionFailure(final CommandResult result,
                final MPDServerException ex) {

            try {
                Thread.sleep(500L);
            } catch (final InterruptedException ignored) {
            }

            try {
                innerConnect();
                result.setLastException(ex);
            } catch (final MPDServerException e) {
                result.setLastException(e);
            }
        }

        /**
         * This is the low level media server connection method.
         *
         * @return The initial response from the connection.
         * @throws MPDServerException Thrown if there was an error connecting to the media server.
         */
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

        /**
         * Read the server response after a {@code write()} to the server.
         *
         * @return A String list of responses.
         * @throws MPDServerException Thrown if there was a server side error with the command that
         *                            was sent.
         * @throws IOException        Thrown if there was a problem reading from from the media
         *                            server.
         */
        private List<String> read() throws MPDServerException, IOException {
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

        /**
         * Sends the command to the server.
         *
         * @throws IOException Thrown upon error transferring command to media server.
         */
        private void write() throws IOException {
            final String cmdString = mCommand.toString();
            // Uncomment for extreme command debugging
            //Log.debug(TAG, "Sending MPDCommand : " + cmdString);
            getOutputStream().write(cmdString);
            getOutputStream().flush();
            mIsCommandSent = true;
        }
    }
}
