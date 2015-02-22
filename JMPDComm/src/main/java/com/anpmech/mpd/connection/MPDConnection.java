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
import com.anpmech.mpd.MPDCommand;
import com.anpmech.mpd.Tools;
import com.anpmech.mpd.concurrent.MPDExecutor;
import com.anpmech.mpd.concurrent.MPDFuture;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.subsystem.Reflection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Class representing a connection to MPD Server.
 */
public abstract class MPDConnection {

    /**
     * The command response for a successful command.
     */
    static final String CMD_RESPONSE_OK = "OK";

    /**
     * The debug flag to enable or disable debug logging output.
     */
    static final boolean DEBUG = false;

    /**
     * The MPD protocol charset used since 0.10.0.
     */
    static final String MPD_PROTOCOL_CHARSET = "UTF-8";

    /**
     * The default MPD host address.
     */
    private static final String DEFAULT_HOST = "127.0.0.1";

    /**
     * The default MPD host address port.
     */
    private static final int DEFAULT_PORT = 6600;

    /**
     * The default environment variable for host address storage.
     */
    private static final String ENVIRONMENT_KEY_HOST = "MPD_HOST";

    /**
     * The default environment variable for port address storage.
     */
    private static final String ENVIRONMENT_KEY_PORT = "MPD_PORT";

    /**
     * The highest available IPv4 port.
     */
    private static final int MAX_PORT = 65535;

    /**
     * Response for each successful command executed in a command list if used with {@code
     * MPD_CMD_START_BULK_OK}.
     */
    private static final String MPD_CMD_BULK_SEP = "list_OK";

    /**
     * The error message given when attempting to send a empty command queue.
     */
    private static final String NO_EMPTY_COMMAND_QUEUE = "Cannot send an empty command queue.";

    /**
     * The error given if a command is sent prior to connection.
     */
    private static final String NO_ENDPOINT_ERROR = "Connection endpoint not yet established.";

    /**
     * This object tracks the status of this connection.
     */
    final MPDConnectionStatus mConnectionStatus;

    /**
     * The command communication timeout.
     */
    final int mReadWriteTimeout;

    /**
     * A set containing all available commands, populated on connection.
     */
    private final Collection<String> mAvailableCommands = new ArrayList<>();

    /**
     * The lock for this connection.
     */
    private final Object mLock = new Object();

    /**
     * The host/port pair used to connect to the media server.
     */
    InetSocketAddress mSocketAddress;

    /**
     * Current media server's major/minor/micro version.
     */
    private int[] mMPDVersion = {0, 0, 0};

    /**
     * The default media server password command.
     */
    private MPDCommand mPassword;

    /**
     * The constructor method. This method does not connect to the server.
     *
     * @param readWriteTimeout The read write timeout for this connection.
     * @param connectionStatus The {@link MPDConnectionStatus} object relating to this connection.
     * @see #connect(InetAddress, int)
     */
    MPDConnection(final int readWriteTimeout, final MPDConnectionStatus connectionStatus) {
        super();

        mReadWriteTimeout = readWriteTimeout;
        mConnectionStatus = connectionStatus;
    }

    /**
     * Get default address port from the {@code MPD_HOST} environment variable.
     *
     * @return The InetAddress generated from the {@code MPD_HOST} environment variable, if it
     * exists, null otherwise.
     */
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
     * The method is used to disallow any further command sending until next {@link #connect()}
     * call.
     */
    public void cancel() {
        mConnectionStatus.statusChangeCancelled();
    }

    /**
     * This method calls standard defaults for the host/port pair and MPD password, if it exists.
     *
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown upon an error sending a simple command to the {@code
     *                      host}/{@code port} pair with the {@code password}.
     */
    public void connect() throws IOException, MPDException {
        connect(getDefaultHost(), getDefaultPort());
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
    public void connect(final InetAddress host, final int port) throws IOException, MPDException {
        if (port < 0 || port > MAX_PORT) {
            throw new MalformedURLException("Port must be an integer between 0 and 65535.");
        }

        synchronized (mLock) {
            final InetSocketAddress address = new InetSocketAddress(host, port);
            final boolean hostChanged = !address.equals(mSocketAddress);
            debug("hasHostChanged: " + hostChanged + " isCancelled: " + mConnectionStatus
                    .isCancelled());
            if (hostChanged || !mConnectionStatus.isConnected() ||
                    mConnectionStatus.isCancelled()) {
                debug("Information changed, connecting");
                mConnectionStatus.unsetCancelled();
                mSocketAddress = address;

                final CommandResult commandResult = submit(Reflection.CMD_ACTION_COMMANDS).get();

                /**
                 * Don't worry too much about it if we didn't get a connection header. Sometimes,
                 * we'll have been told we disconnected when we had not.
                 */
                if (commandResult.isHeaderValid()) {
                    final List<String> response = commandResult.getResponse();
                    Tools.parseResponse(response, Reflection.CMD_RESPONSE_COMMANDS);
                    mAvailableCommands.clear();
                    mAvailableCommands.addAll(response);

                    mMPDVersion = commandResult.getMPDVersion();
                }
            } else {
                debug("Not reconnecting, already connected with same information");
            }
        }
    }

    /**
     * This method outputs the {@code line} parameter to {@link com.anpmech.mpd.Log#debug(String,
     * String)} if {@link #DEBUG} is set to true.
     *
     * @param line The {@link String} to output to the log.
     */
    abstract void debug(final String line);

    /**
     * This method retrieves a CommandProcessor for the particular extending class.
     *
     * @param command The command line to be processed.
     * @return A command processor, ready for {@link java.util.concurrent.ExecutorService}
     * submission.
     */
    abstract Callable<CommandResult> getCommandProcessor(final String command);

    /**
     * Get default address from the {@code MPD_HOST} environment variable.
     *
     * @return The InetAddress generated from the {@code MPD_HOST} environment variable, if it
     * exists, null otherwise.
     * @throws UnknownHostException If the address lookup fails.
     */
    private InetAddress getDefaultHost() throws UnknownHostException {
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

    /**
     * The current MPD protocol version.
     */
    public int[] getMPDVersion() {
        return mMPDVersion.clone();
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
     * This method sets up the {@link CommandQueue} (and prefixes with the password, if applicable)
     * for ExecutorService processing.
     *
     * @param commandQueue The CommandQueue to process.
     * @return The response to the CommandQueue.
     */
    private MPDFuture<CommandResult> processCommand(final CommandQueue commandQueue) {
        if (commandQueue.isEmpty()) {
            throw new IllegalStateException(NO_EMPTY_COMMAND_QUEUE);
        }

        if (mPassword != null) {
            commandQueue.add(0, mPassword);
        }

        return processCommand(commandQueue.toString());
    }

    /**
     * This method sets up the {@link MPDCommand} (and prefixes with the password, if applicable)
     * for ExecutorService processing.
     *
     * @param mpdCommand The command to be processed.
     * @return The response from the command.
     */
    private MPDFuture<CommandResult> processCommand(final MPDCommand mpdCommand) {
        final String commandString;

        if (mPassword == null) {
            commandString = mpdCommand.getCommand();
        } else {
            commandString = new CommandQueue(mPassword, mpdCommand).toString();
        }

        return processCommand(commandString);
    }

    /**
     * Processes the command by setting up the command processor executor.
     *
     * @param command The command string to be processed.
     * @return The response to the processed command.
     */
    private MPDFuture<CommandResult> processCommand(final String command) {
        debug("processCommand() command: " + command);

        final Callable<CommandResult> callable = getCommandProcessor(command);

        return MPDExecutor.submit(callable);
    }

    /**
     * This method sets up the {@link CommandQueue} (and prefixes with the password, if applicable)
     * for ExecutorService processing.
     *
     * @param commandQueue The CommandQueue to process.
     * @return The response to the CommandQueue.
     */
    private MPDFuture<CommandResult> processCommandSeparated(final CommandQueue commandQueue) {
        if (commandQueue.isEmpty()) {
            throw new IllegalStateException(NO_EMPTY_COMMAND_QUEUE);
        }

        if (mPassword != null) {
            commandQueue.add(0, mPassword);
        }

        return processCommand(commandQueue.toStringSeparated());
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
        return processCommand(command).get().getResponse();
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
        return submit(command, args).get().getResponse();
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
        return processCommand(commandQueue).get().getResponse();
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
        final List<String> response = processCommandSeparated(commandQueue).get().getResponse();

        /** TODO: Fix to push the future down. */
        if (mPassword != null) {
            /** Remove the password response. */
            response.remove(0);
        }

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

    /**
     * Submits the command and arguments to the {@link MPDExecutor}.
     *
     * @param command The command to be sent to the server.
     * @param args    Arguments to the command to be sent to the server.
     * @return A {@link MPDFuture} for tracking and modification of the submission.
     */
    public MPDFuture<CommandResult> submit(final CharSequence command,
            final CharSequence... args) {
        return submit(MPDCommand.create(command, args));
    }

    /**
     * Submits the command to the {@link MPDExecutor}.
     *
     * @param command The command to be sent to the server.
     * @return A {@link MPDFuture} for tracking and modification of the submission.
     */
    public MPDFuture<CommandResult> submit(final MPDCommand command) {
        return processCommand(command);
    }

    /**
     * Submits this CommandQueue to the {@link MPDExecutor}.
     *
     * @param commandQueue The The CommandQueue to send to the server.
     * @return A {@link MPDFuture} for tracking and response processing.
     */
    public MPDFuture<CommandResult> submit(final CommandQueue commandQueue) {
        return processCommand(commandQueue);
    }
}
