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

import com.anpmech.mpd.CommandQueue;
import com.anpmech.mpd.Log;
import com.anpmech.mpd.MPDCommand;
import com.anpmech.mpd.Tools;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.subsystem.Reflection;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Class representing a connection to MPD Server.
 */
public abstract class MPDConnection {

    /**
     * Response for each successful command executed in a command list if used with {@code
     * MPD_CMD_START_BULK_OK}.
     */
    public static final String MPD_CMD_BULK_SEP = "list_OK";

    static final String MPD_RESPONSE_OK = "OK";

    private static final int CONNECTION_TIMEOUT = 10000;

    /** The debug flag to enable or disable debug logging output. */
    private static final boolean DEBUG = false;

    /** Default buffer size for the socket. */
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private static final String DEFAULT_HOST = "127.0.0.1";

    private static final int DEFAULT_PORT = 6600;

    private static final String ENVIRONMENT_KEY_HOST = "MPD_HOST";

    private static final String ENVIRONMENT_KEY_PORT = "MPD_PORT";

    private static final int MAX_PORT = 65535;

    /** Maximum number of times to attempt command processing. */
    private static final int MAX_REQUEST_RETRY = 3;

    private static final String MPD_RESPONSE_ERR = "ACK";

    private static final String NO_ENDPOINT_ERROR = "Connection endpoint not yet established.";

    private static final String POOL_THREAD_NAME_PREFIX = "pool";

    /** A set containing all available commands, populated on connection. */
    private final Collection<String> mAvailableCommands = new HashSet<>();

    /** The {@code ExecutorService} used to process commands. */
    private final ThreadPoolExecutor mExecutor;

    /** The lock for this connection. */
    private final Object mLock = new Object();

    /** The command communication timeout. */
    private final int mReadWriteTimeout;

    private final String mTag;

    /** If set to true, this will cancel any processing commands at next opportunity. */
    private boolean mCancelled = false;

    /** User facing connection status. */
    private boolean mIsConnected = false;

    /** Current media server's major/minor/micro version. */
    private int[] mMPDVersion = {0, 0, 0};

    /** Current media server password. */
    private MPDCommand mPassword = null;

    /** The host/port pair used to connect to the media server. */
    private InetSocketAddress mSocketAddress;

    /**
     * The constructor method. This method does not connect to the server.
     *
     * @param readWriteTimeout The read write timeout for this connection.
     * @param maxConnections   Maximum number of sockets to allow running at one time.
     * @see #connect(InetAddress, int)
     */
    MPDConnection(final int readWriteTimeout, final int maxConnections) {
        super();

        mReadWriteTimeout = readWriteTimeout;
        mExecutor = new ThreadPoolExecutor(1, maxConnections, (long) mReadWriteTimeout,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        mExecutor.prestartCoreThread();
        if (maxConnections > 1) {
            mTag = "MPDConnectionMultiSocket";
            mExecutor.allowCoreThreadTimeOut(true);
        } else {
            mTag = "MPDConnectionMonoSocket";
        }
    }

    private static int getDefaultPort() {
        int port;

        try {
            port = Integer.parseInt(System.getenv(ENVIRONMENT_KEY_PORT));
        } catch (final NumberFormatException ignored) {
            port = DEFAULT_PORT;
        }

        return port;
    }

    /**
     * This method calls standard defaults for the host/port pair and MPD password, if it exists.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown upon an error sending a simple command to the {@code
     *                      host}/{@code port} pair with the {@code password}.
     */
    public void connect() throws IOException, MPDException {
        connect(getDefaultAddress(), getDefaultPort());
    }

    /**
     * Sets up connection to host/port pair.
     * <p/>
     * If a main password is required, it MUST be called prior to calling this method.
     *
     * @param host The media server host to connect to.
     * @param port The media server port to connect to.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown upon an error sending a simple command to the {@code
     *                      host}/{@code port} pair with the {@code password}.
     */
    public final void connect(final InetAddress host, final int port)
            throws IOException, MPDException {
        innerDisconnect();

        if (port < 0 || port > MAX_PORT) {
            throw new MalformedURLException("Port must be an integer between 0 and 65535.");
        }

        mCancelled = false;
        mSocketAddress = new InetSocketAddress(host, port);

        final MPDCommand mpdCommand = MPDCommand.create(Reflection.CMD_ACTION_COMMANDS);
        final CommandResult commandResult = processCommand(mpdCommand);

        if (!commandResult.isHeaderValid()) {
            throw new IOException("Failed initial connection.");
        }

        synchronized (mAvailableCommands) {
            final List<String> response = commandResult.getResponse();
            Tools.parseResponse(response, Reflection.CMD_RESPONSE_COMMANDS);
            mAvailableCommands.clear();
            mAvailableCommands.addAll(response);
        }

        mIsConnected = true;
        mMPDVersion = commandResult.getMPDVersion();
    }

    /**
     * The method to disconnect from the current connected server.
     *
     * @throws IOException Thrown upon disconnection error.
     */
    public void disconnect() throws IOException {
        mCancelled = true;
        innerDisconnect();
    }

    private InetAddress getDefaultAddress() throws UnknownHostException {
        String host = System.getenv(ENVIRONMENT_KEY_HOST);
        if (host == null) {
            host = DEFAULT_HOST;
        }
        final int atIndex = host.indexOf('@');

        if (atIndex == -1) {
            mPassword = null;
        } else {
            setDefaultPassword(host.substring(0, atIndex));
            host = host.substring(atIndex + 1);
        }

        return InetAddress.getByName(host);
    }

    /**
     * The current connected media server host.
     *
     * @return The current connected media server host, null if not connected.
     */
    public InetAddress getHostAddress() {
        if (mSocketAddress == null) {
            throw new IllegalStateException(NO_ENDPOINT_ERROR);
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
            throw new IllegalStateException(NO_ENDPOINT_ERROR);
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
     * @throws IOException Thrown if there is a problem closing the socket.
     */
    private void innerDisconnect() throws IOException {
        mIsConnected = false;
        synchronized (mLock) {
            if (getSocket() != null) {
                getSocket().close();
                setSocket(null);
            }
        }
    }

    /**
     * Checks a list of available commands generated on connection.
     *
     * @param command A MPD protocol command.
     * @return True if the {@code command} is available for use, false otherwise.
     */
    public boolean isCommandAvailable(final String command) {
        return mAvailableCommands.contains(command);
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
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    private CommandResult processCommand(final MPDCommand command)
            throws IOException, MPDException {
        // Bypass thread pool queue if the thread already comes from the pool to avoid deadlock.
        if (Thread.currentThread().getName().startsWith(POOL_THREAD_NAME_PREFIX)) {
            throw new IllegalThreadStateException("Don't call from within the executor.");
        }

        final CommandResult result;
        try {
            result = mExecutor.submit(new CommandProcessor(command)).get();
            // Spam the log with the largest pool size
            //Log.debug(mTag, "Largest pool size: " + mExecutor.getLargestPoolSize());
        } catch (final ExecutionException | InterruptedException e) {
            throw new IOException(e);
        }

        if (result.getResponse() == null) {
            if (result.isIOExceptionLast() == null) {
                final String exceptionString;

                if (mCancelled) {
                    exceptionString = "Connection cancelled, not a bug but exception is required.";
                } else {
                    /**
                     * This should not occur, and this exception should extend RuntimeException,
                     * BUT a RuntimeException would most likely not help the situation.
                     */
                    exceptionString = "No result, no exception. This is a bug. Please report.";
                }

                throw new IOException(
                        exceptionString + '\n' +
                                "Cancelled: " + mCancelled + '\n' +
                                "Command: " + command + '\n' +
                                "Connected: " + mIsConnected + '\n' +
                                "Connection result: " + result.getConnectionResult() + '\n');
            } else if (result.isIOExceptionLast().equals(Boolean.TRUE)) {
                throw result.getIOException();
            } else if (result.isIOExceptionLast().equals(Boolean.FALSE)) {
                throw result.getMPDException();
            }
        }

        return result;
    }

    /**
     * Communicates with the server by sending a command and receiving the response.
     *
     * @param command The command to be sent to the server.
     * @return The result from the command sent to the server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<String> send(final MPDCommand command) throws IOException, MPDException {
        return processCommand(command).getResponse();
    }

    /**
     * Communicates with the server by sending a command and receiving the response.
     *
     * @param command The command to be sent to the server.
     * @param args    Arguments to the command to be sent to the server.
     * @return The result from the command sent to the server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<String> send(final CharSequence command, final CharSequence... args)
            throws IOException, MPDException {
        return send(MPDCommand.create(command, args));
    }

    /**
     * Sends the commands (without separated results) which were {@code add}ed to the queue.
     *
     * @param commandQueue The CommandQueue to send.
     * @return The results of from the media server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<String> send(final CommandQueue commandQueue) throws IOException, MPDException {
        return send(commandQueue, false);
    }

    /**
     * Sends the commands which were {@code add}ed to the queue.
     *
     * @param commandQueue The CommandQueue to send.
     * @return The results of from the media server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    private List<String> send(final CommandQueue commandQueue, final boolean separated)
            throws IOException, MPDException {
        if (commandQueue.isEmpty()) {
            throw new IllegalStateException("Cannot send an empty command queue.");
        }

        final MPDCommand mpdCommand;
        if (separated) {
            mpdCommand = MPDCommand.create(commandQueue.toStringSeparated());
        } else {
            mpdCommand = MPDCommand.create(commandQueue.toString());
        }

        return send(mpdCommand);
    }

    /**
     * Sends the commands (with separated results) which were {@code add}ed to the queue.
     *
     * @param commandQueue The CommandQueue to send.
     * @return The results of from the media server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<List<String>> sendSeparated(final CommandQueue commandQueue)
            throws IOException, MPDException {
        final List<String> response = send(commandQueue, true);
        final Collection<int[]> ranges = Tools.getRanges(response, MPD_CMD_BULK_SEP);
        final List<List<String>> result = new ArrayList<>(ranges.size());

        for (final int[] range : ranges) {
            if (commandQueue.size() == 1) {
                /** If the CommandQueue has a size of 1, it was sent as a command. */
                result.add(response);
            } else {
                /** Remove the bulk separator from the subList. */
                result.add(response.subList(range[0], range[1] - 1));
            }
        }

        return result;
    }

    /**
     * Sets the default password for this connection.
     *
     * @param password The main password for this connection.
     */
    public void setDefaultPassword(final CharSequence password) {
        if (password == null) {
            mPassword = null;
        } else {
            mPassword = MPDCommand.create(MPDCommand.MPD_CMD_PASSWORD, password);
        }
    }

    protected abstract void setInputStream(InputStreamReader inputStream);

    protected abstract void setOutputStream(OutputStreamWriter outputStream);

    protected abstract void setSocket(Socket socket);

    /**
     * This method shuts down any running executors in this connection.
     */
    public Runnable shutdown() {
        return new Runnable() {
            @Override
            public void run() {
                if (!mExecutor.isShutdown() && !mExecutor.isTerminating()) {
                    mExecutor.shutdown();
                }
            }
        };
    }

    /** This class communicates with the server by sending the command and processing the result. */
    private class CommandProcessor implements Callable<CommandResult> {

        /** The command to be processed. */
        private final MPDCommand mCommand;

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
            final CharSequence baseCommand = mCommand.getBaseCommand();

            while (result.getResponse() == null && retryCount < MAX_REQUEST_RETRY && !mCancelled) {
                try {
                    if (getSocket() == null || !getSocket().isConnected() ||
                            getSocket().isClosed()) {
                        result.setConnectionResult(innerConnect());
                    }

                    write(mCommand);
                    result.setResponse(read());
                } catch (final IOException e) {
                    handleFailure(result, e);
                } catch (final MPDException ex1) {
                    result.setException(ex1);
                    break;
                }

                retryCount++;
            }

            if (!mCancelled) {
                if (result.getResponse() == null) {
                    logError(result, baseCommand, retryCount);
                } else {
                    mIsConnected = true;
                }
            }
            return result;
        }

        /**
         * Used after a server error, sleeps for a small time then tries to reconnect.
         *
         * @param result The {@code CommandResult} which stores the connection failure.
         * @param e      The exception to set.
         */
        private void handleFailure(final CommandResult result, final IOException e) {
            if (isFailureHandled(result)) {
                result.setException(e);
            }
        }

        /**
         * This is the low level media server connection method.
         *
         * @return The initial response from the connection.
         * @throws IOException  Thrown upon a communication error with the server.
         * @throws MPDException Thrown if an error occurs as a result of command execution.
         */
        private String innerConnect() throws IOException, MPDException {
            final String line;

            // Always release existing socket if any before creating a new one
            if (getSocket() != null) {
                try {
                    innerDisconnect();
                } catch (final IOException ignored) {
                }
            }

            setSocket(new Socket());
            getSocket().setSoTimeout(mReadWriteTimeout);
            getSocket().connect(mSocketAddress, CONNECTION_TIMEOUT);
            setInputStream(new InputStreamReader(getSocket().getInputStream(), "UTF-8"));
            final BufferedReader in = new BufferedReader(getInputStream(), DEFAULT_BUFFER_SIZE);
            setOutputStream(new OutputStreamWriter(getSocket().getOutputStream(), "UTF-8"));
            line = in.readLine();

            if (line == null) {
                throw new IOException("No response from server.");
            }

            /** Protocol says OK will begin the session, otherwise assume IO error. */
            if (!line.startsWith(MPD_RESPONSE_OK)) {
                throw new IOException("Bogus response from server.");
            }

            if (mPassword != null) {
                write(mPassword);
                read(); /** Ignore the output, unless, it's an exception. */
            }

            return line;
        }

        /**
         * Used after a server error, sleeps for a small time then tries to reconnect.
         *
         * @param result The {@code CommandResult} which stores the connection failure.
         */
        private boolean isFailureHandled(final CommandResult result) {
            boolean failureHandled = false;
            mIsConnected = false;

            try {
                Thread.sleep(500L);
            } catch (final InterruptedException ignored) {
            }

            try {
                innerConnect();
                failureHandled = true;
            } catch (final MPDException me) {
                result.setException(me);
            } catch (final IOException ie) {
                result.setException(ie);
            }

            return failureHandled;
        }

        private void logError(final CommandResult result, final CharSequence baseCommand,
                final int retryCount) {
            final StringBuilder stringBuilder = new StringBuilder(50);

            stringBuilder.append("Command ");
            stringBuilder.append(baseCommand);
            stringBuilder.append(" failed after ");
            stringBuilder.append(retryCount + 1);

            if (retryCount == 0) {
                stringBuilder.append(" attempt.");
            } else {
                stringBuilder.append(" attempts.");
            }

            if (result.isIOExceptionLast() == null) {
                Log.error(mTag, stringBuilder.toString());
            } else if (result.isIOExceptionLast().equals(Boolean.TRUE)) {
                Log.error(mTag, stringBuilder.toString(), result.getIOException());
            } else if (result.isIOExceptionLast().equals(Boolean.FALSE)) {
                Log.error(mTag, stringBuilder.toString(), result.getMPDException());
            }
        }

        /**
         * Read the server response after a {@code write()} to the server.
         *
         * @return A String list of responses.
         * @throws IOException  Thrown if there was a problem reading from from the media server.
         * @throws MPDException Thrown if there was a server side error with the command that was
         *                      sent.
         */
        private List<String> read() throws MPDException, IOException {
            final List<String> result = new ArrayList<>();
            final BufferedReader in = new BufferedReader(getInputStream(), DEFAULT_BUFFER_SIZE);
            boolean validResponse = false;

            for (String line = in.readLine(); line != null; line = in.readLine()) {

                if (line.startsWith(MPD_RESPONSE_OK)) {
                    validResponse = true;
                    break;
                }

                if (line.startsWith(MPD_RESPONSE_ERR)) {
                    throw new MPDException(line);
                }
                result.add(line);
            }

            if (!validResponse) {
                // Close socket if there is no response...
                // Something is wrong (e.g. MPD shutdown..)
                throw new EOFException("Connection lost");
            }
            return result;
        }

        /**
         * Sends the command to the server.
         *
         * @throws IOException Thrown upon error transferring command to media server.
         */
        private void write(final MPDCommand mpdCommand) throws IOException {
            final String cmdString = mpdCommand.getCommand();

            // Uncomment for extreme command debugging
            //Log.debug(mTag, "Sending MPDCommand : " + cmdString);

            getOutputStream().write(cmdString);
            getOutputStream().flush();
        }
    }
}
