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
import org.a0z.mpd.Tools;
import org.a0z.mpd.exception.MPDException;
import org.a0z.mpd.subsystem.Reflection;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    static final String MPD_RESPONSE_OK = "OK";

    private static final int CONNECTION_TIMEOUT = 10000;

    /** The debug flag to enable or disable debug logging output. */
    private static final boolean DEBUG = false;

    /** Default buffer size for the socket. */
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    /** Maximum number of times to attempt command processing. */
    private static final int MAX_REQUEST_RETRY = 3;

    private static final String MPD_RESPONSE_ERR = "ACK";

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
            mTag = "MPDConnectionMultiSocket";
            mExecutor.allowCoreThreadTimeOut(true);
        } else {
            mTag = "MPDConnectionMonoSocket";
        }
    }

    /**
     * Sets up connection to host/port pair with MPD password.
     *
     * @param host     The media server host to connect to.
     * @param port     The media server port to connect to.
     * @param password The MPD protocol password to pass upon connection.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown upon an error sending a simple command to the {@code
     *                      host}/{@code port} pair with the {@code password}.
     */
    public final void connect(final InetAddress host, final int port, final String password)
            throws IOException, MPDException {
        innerDisconnect();

        mCancelled = false;
        mPassword = password;
        mSocketAddress = new InetSocketAddress(host, port);

        final MPDCommand mpdCommand = new MPDCommand(Reflection.CMD_ACTION_COMMANDS);
        final CommandResult commandResult = processCommand(mpdCommand);

        synchronized (mAvailableCommands) {
            final Collection<String> response = Tools.
                    parseResponse(commandResult.getResult(), Reflection.CMD_RESPONSE_COMMANDS);
            mAvailableCommands.clear();
            mAvailableCommands.addAll(response);
        }

        if (!commandResult.isHeaderValid()) {
            throw new IOException("Failed initial connection.");
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
        final CommandResult result;

        // Bypass thread pool queue if the thread already comes from the pool to avoid deadlock.
        if (Thread.currentThread().getName().startsWith(POOL_THREAD_NAME_PREFIX)) {
            result = new CommandProcessor(command).call();
        } else {
            try {
                result = mExecutor.submit(new CommandProcessor(command)).get();
                // Spam the log with the largest pool size
                //Log.debug(mTag, "Largest pool size: " + mExecutor.getLargestPoolSize());
            } catch (final ExecutionException | InterruptedException e) {
                throw new IOException(e);
            }
        }

        if (result.getResult() == null) {
            if (result.isIOExceptionLast() == null) {
                /**
                 * This should not occur, and this exception should extend RuntimeException,
                 * BUT a RuntimeException would most likely not help the situation.
                 */
                throw new IOException(
                        "No result, no exception. This is a bug. Please report." + '\n' +
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
    public List<String> sendCommand(final MPDCommand command) throws IOException, MPDException {
        return processCommand(command).getResult();
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
    public List<String> sendCommand(final String command, final String... args)
            throws IOException, MPDException {
        return sendCommand(new MPDCommand(command, args));
    }

    /**
     * Communicates with the server by sending a command and receiving the response and defining
     * non-fatal ACK codes.
     *
     * @param command        The command to be sent to the server.
     * @param nonfatalErrors Errors to consider as non-fatal for this command. These MPD error
     *                       codes with this command will not return any exception.
     * @param args           Arguments to the command to be sent to the server.
     * @return The result from the command sent to the server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<String> sendCommand(final String command, final int[] nonfatalErrors,
            final String... args) throws IOException, MPDException {
        return sendCommand(new MPDCommand(command, nonfatalErrors, args));
    }

    protected abstract void setInputStream(InputStreamReader inputStream);

    protected abstract void setOutputStream(OutputStreamWriter outputStream);

    protected abstract void setSocket(Socket socket);

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
            boolean isCommandSent = false;
            final String baseCommand = mCommand.getCommand();

            while (result.getResult() == null && retryCount < MAX_REQUEST_RETRY && !mCancelled) {
                try {
                    if (getSocket() == null || !getSocket().isConnected() ||
                            getSocket().isClosed()) {
                        result.setConnectionResult(innerConnect());
                    }

                    write();
                    isCommandSent = true;
                    result.setResult(read());
                } catch (final EOFException ex0) {
                    handleFailure(result, ex0);

                    // Do not fail when the IDLE response has not been read (to improve connection
                    // failure robustness). Just send the "changed playlist" result to force the MPD
                    // status to be refreshed.
                    if (MPDCommand.MPD_CMD_IDLE.equals(baseCommand)) {
                        result.setResult(Collections.singletonList(
                                "changed: " + MPDStatusMonitor.IDLE_PLAYLIST));
                    }
                } catch (final IOException e) {
                    handleFailure(result, e);
                } catch (final MPDException ex1) {
                    // Avoid getting in an infinite loop if an error occurred in the password cmd
                    if (ex1.mErrorCode == MPDException.ACK_ERROR_PASSWORD ||
                            ex1.mErrorCode == MPDException.ACK_ERROR_PERMISSION) {
                        result.setException(ex1);
                    } else {
                        handleFailure(result, ex1);
                    }
                }

                /** On successful send of non-retryable command, break out. */
                if (!MPDCommand.isRetryable(baseCommand) && isCommandSent) {
                    break;
                }

                retryCount++;
            }

            if (!mCancelled) {
                if (result.getResult() == null) {
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
         * Used after a server error, sleeps for a small time then tries to reconnect.
         *
         * @param result The {@code CommandResult} which stores the connection failure.
         * @param e      The exception to set.
         */
        private void handleFailure(final CommandResult result, final MPDException e) {
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
                sendCommand(MPDCommand.MPD_CMD_PASSWORD, mPassword);
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

        /**
         * This method is a place to specify if a ACK is not actually an error message we don't
         * consider to be a fatal error.
         *
         * @param message The message to check.
         * @return True if the message indicates a non-fatal error, false otherwise.
         */
        private boolean isNonfatalACK(final String message) {
            final boolean isNonfatalACK;
            final int errorCode = MPDException.getAckErrorCode(message);

            if (mCommand.isErrorNonfatal(errorCode)) {
                isNonfatalACK = true;
                if (DEBUG) {
                    Log.debug(mTag, "Non-fatal ACK emitted, exception suppressed: " + message);
                }
            } else {
                isNonfatalACK = false;
            }

            return isNonfatalACK;
        }

        private void logError(final CommandResult result, final String baseCommand,
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
         * @throws IOException  Thrown if there was a problem reading from from the media
         *                      server.
         * @throws MPDException Thrown if there was a server side error with the command that
         *                      was sent.
         */
        private List<String> read() throws MPDException, IOException {
            final List<String> result = new ArrayList<>();
            final BufferedReader in = new BufferedReader(getInputStream(), DEFAULT_BUFFER_SIZE);

            boolean serverDataRead = false;
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                serverDataRead = true;

                if (line.startsWith(MPD_RESPONSE_OK)) {
                    break;
                }

                if (line.startsWith(MPD_RESPONSE_ERR)) {
                    if (isNonfatalACK(line)) {
                        break;
                    }

                    throw new MPDException(line);
                }
                result.add(line);
            }

            if (!serverDataRead) {
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
        private void write() throws IOException {
            final String cmdString = mCommand.toString();

            // Uncomment for extreme command debugging
            //Log.debug(mTag, "Sending MPDCommand : " + cmdString);
            getOutputStream().write(cmdString);
            getOutputStream().flush();
        }
    }
}
