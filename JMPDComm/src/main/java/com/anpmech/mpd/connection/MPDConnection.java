/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2016 The MPDroid Project
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
import com.anpmech.mpd.commandresponse.CommandResponse;
import com.anpmech.mpd.commandresponse.KeyValueResponse;
import com.anpmech.mpd.commandresponse.SeparatedResponse;
import com.anpmech.mpd.concurrent.MPDExecutor;
import com.anpmech.mpd.concurrent.ResultFuture;
import com.anpmech.mpd.concurrent.SeparatedFuture;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.subsystem.Reflection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

/**
 * Class representing a connection to MPD Server.
 */
public abstract class MPDConnection implements MPDConnectionListener {

    /**
     * Response for each successful command executed in a command list if used with {@code
     * MPD_CMD_START_BULK_OK}.
     */
    public static final String MPD_CMD_BULK_SEP = "list_OK";

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
     * The error message given when attempting to send a empty command queue.
     */
    private static final String NO_EMPTY_COMMAND_QUEUE = "Cannot send an empty command queue.";

    /**
     * The error given if a command is sent prior to connection.
     */
    private static final String NO_ENDPOINT_ERROR = "Connection endpoint not yet established.";

    /**
     * The class log identifier.
     */
    private static final String TAG = "MPDConnection";

    /**
     * This object tracks the status of this connection.
     */
    protected final MPDConnectionStatus mConnectionStatus;

    /**
     * The command communication timeout.
     */
    protected final int mReadWriteTimeout;

    /**
     * A set containing all available commands, populated on connection.
     */
    private final Collection<String> mAvailableCommands = new ArrayList<>();

    /**
     * The locking Semaphore for this connection.
     * <BR><BR>
     * This is used to prevent more than one connection configuration from running at the same time
     * in the same instance. This is necessary due to the threaded nature of the connection. This
     * Semaphore is also constructed with fairness to allow FIFO connect() execution.
     */
    private final Semaphore mConnectionLock = new Semaphore(1, true);

    /**
     * The host/port pair used to connect to the media server.
     */
    protected InetSocketAddress mSocketAddress;

    /**
     * This is a holder for a connection {@link CommandResponse} while waiting for the connected
     * callback to occur.
     */
    private ResultFuture mConnectionResponse;

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
     * @param blockingIO       True if the parent instance has potential to block during IO, false
     *                         otherwise.
     * @see #connect(InetAddress, int)
     */
    MPDConnection(final int readWriteTimeout, final boolean blockingIO) {
        mReadWriteTimeout = readWriteTimeout;

        if (blockingIO) {
            mConnectionStatus = new BlockingConnectionStatus(this);
        } else {
            mConnectionStatus = new NonBlockingConnectionStatus(this);
        }
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
     * This method calls standard defaults for the host/port pair and MPD password, if it exists.
     *
     * <p>If a main password is required, it MUST be called prior to calling this method.</p>
     *
     * <p>All threads calling this method in the same instance with a different host will block
     * until the prior connection threads succeeds or fail. Otherwise, expected behaviour is this
     * method will return immediately and status will be provided to listeners via
     * {@link MPDConnectionStatus#addListener(MPDConnectionListener)}.</p>
     *
     * @throws UnknownHostException Thrown when a hostname can not be resolved.
     */
    public void connect() throws UnknownHostException {
        connect(getDefaultHost(), getDefaultPort());
    }

    /**
     * Resolves a host then sets up connection to host/port pair.
     *
     * <p>If a main password is required, it MUST be called prior to calling this method.</p>
     *
     * <p>All threads calling this method in the same instance with a different host will block
     * until the prior connection threads succeeds or fail. Otherwise, expected behaviour is this
     * method will return immediately and status will be provided to listeners via
     * {@link MPDConnectionStatus#addListener(MPDConnectionListener)}.</p>
     *
     * @param host The media server host to connect to.
     * @param port The media server port to connect to.
     */
    public void connect(final String host, final int port) {
        final Runnable resolveHost = new Runnable() {
            @Override
            public void run() {
                try {
                    connect(InetAddress.getByName(host), port);
                } catch (final UnknownHostException e) {
                    mConnectionStatus.disconnectedCallbackComplete(
                            "Unknown host: " + e.getLocalizedMessage());
                }
            }
        };

        MPDExecutor.submit(resolveHost);
    }

    /**
     * Sets up connection to host/port pair.
     *
     * <p>If a main password is required, it MUST be called prior to calling this method.</p>
     *
     * <p>All threads calling this method in the same instance with a different host will block
     * until the prior connection threads succeeds or fail. Otherwise, expected behaviour is this
     * method will return immediately and status will be provided to listeners via
     * {@link MPDConnectionStatus#addListener(MPDConnectionListener)}.</p>
     *
     * @param host The media server host to connect to.
     * @param port The media server port to connect to.
     */
    public void connect(final InetAddress host, final int port) {
        final InetSocketAddress address = new InetSocketAddress(host, port);

        debug("Acquiring a connection lock.");
        mConnectionLock.acquireUninterruptibly();
        final boolean hostChanged = !address.equals(mSocketAddress);
        debug("hasHostChanged: " + hostChanged + " isCancelled: " + mConnectionStatus
                .isCancelled());
        if (hostChanged || !mConnectionStatus.isConnected() || mConnectionStatus.isCancelled()) {
            debug("Information changed, connecting");
            mConnectionStatus.unsetCancelled();
            mConnectionStatus.statusChangeConnecting();
            mSocketAddress = address;
            mConnectionResponse = submit(Reflection.CMD_ACTION_COMMANDS);
        } else {
            debug("Not reconnecting, already connected with same information");
        }
    }

    /**
     * Called upon connection, prior to other {@link MPDConnectionListener}s.
     *
     * @param zero This will always be 0.
     */
    @SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
    @Override
    public void connectionConnected(final int zero) {
        int commandErrorCode = 0;
        CommandResult result = null;
        KeyValueResponse response = null;

        try {
            result = mConnectionResponse.get();
            response = new KeyValueResponse(result);
        } catch (final MPDException e) {
            commandErrorCode = e.mErrorCode;
            Log.error(TAG, "Exception during connection.", e);
        } catch (final IOException e) {
            /**
             * This should not happen. If it does, it is a programmatic error in the
             * CommandProcessor. MPDConnectionStatus.statusChangeDisconnected() should have been
             * called prior to the ResultFuture task completion (and the result from ResultFuture.get()).
             */
            if (mConnectionStatus.isConnected()) {
                throw new IllegalStateException("IOException thrown as result of successful" +
                        "connection.", e);
            }
        }

        /**
         * This can happen because this callback is called early in the CommandProcessor execution.
         * If not connected, this will be skipped and another MPDConnectionStatus callback will
         * have been called.
         */
        if (result != null && mConnectionStatus.isConnected()) {
            /**
             * Don't worry too much about it if we didn't get a connection header. Sometimes,
             * we'll have been told we disconnected when we had not.
             */
            if (response != null && result.isHeaderValid()) {
                mAvailableCommands.clear();
                mAvailableCommands.addAll(response.getValues());

                mMPDVersion = result.getMPDVersion();
            }

            debug("Releasing connection lock upon successful connection.");
            mConnectionLock.release();

            /**
             * This should be the final call from this method.
             */
            mConnectionStatus.connectedCallbackComplete(commandErrorCode);
        }
    }

    /**
     * Called when connecting, prior to other {@link MPDConnectionListener}s.
     *
     * <p>This implies that we've disconnected. This callback is intended to be transient. Status
     * change from connected to connecting may happen, but if a connection is not established, with
     * a connected callback, the disconnection status callback should be called.</p>
     */
    @Override
    public void connectionConnecting() {
        /**
         * This should be the final call from this method.
         */
        mConnectionStatus.connectingCallbackComplete();
    }

    /**
     * Called upon disconnection, prior to other {@link MPDConnectionListener}s.
     *
     * @param reason The reason given for disconnection.
     */
    @Override
    public void connectionDisconnected(final String reason) {
        debug("Releasing connection lock upon disconnection.");
        mConnectionLock.release();

        /**
         * This should be the final call from this method.
         */
        mConnectionStatus.disconnectedCallbackComplete(reason);
    }

    /**
     * This method outputs the {@code line} parameter to {@link Log#debug(String, String)} if
     * {@link #DEBUG} is set to true.
     *
     * @param line The {@link String} to output to the log.
     */
    abstract void debug(final String line);

    /**
     * The method disconnects and disallows any further connections until next {@link #connect()}
     * call.
     *
     * @throws IOException Thrown upon an error when disconnecting from the server.
     */
    public void disconnect() throws IOException {
        mConnectionStatus.statusChangeCancelled();
    }

    /**
     * This method retrieves a CommandProcessor for the particular extending class.
     *
     * @param command          The command line to be processed.
     * @param excludeResponses This is used to manually exclude responses from split
     *                         {@link CommandResponse} inclusion.
     * @return A command processor, ready for {@link ExecutorService} submission.
     */
    abstract Callable<CommandResult> getCommandProcessor(final String command,
            final int[] excludeResponses);

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
     *
     * @return The given MPD protocol as an integer array.
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
    private ResultFuture processCommand(final CommandQueue commandQueue) {
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
    private ResultFuture processCommand(final MPDCommand mpdCommand) {
        final String commandString;

        if (mPassword == null) {
            commandString = mpdCommand.getCommand();
        } else {
            commandString = new CommandQueue(mPassword, mpdCommand).toString();
        }

        return processCommand(commandString);
    }

    private ResultFuture processCommand(final String command) {
        return processCommand(command, null);
    }

    /**
     * Processes the command by setting up the command processor executor.
     *
     * @param command          The command string to be processed.
     * @param excludeResponses This is used to manually exclude responses from split
     *                         {@link CommandResponse} inclusion.
     * @return The response to the processed command.
     */
    private ResultFuture processCommand(final String command, final int[] excludeResponses) {
        debug("processCommand() command: " + command);

        final Callable<CommandResult> callable = getCommandProcessor(command, excludeResponses);

        return MPDExecutor.submit(callable);
    }

    /**
     * This method sets up the {@link CommandQueue} (and prefixes with the password, if applicable)
     * for ExecutorService processing.
     *
     * @param commandQueue The CommandQueue to process.
     * @return The response to the CommandQueue.
     */
    private ResultFuture processCommandSeparated(final CommandQueue commandQueue) {
        if (commandQueue.isEmpty()) {
            throw new IllegalStateException(NO_EMPTY_COMMAND_QUEUE);
        }

        final int[] excludeResponses;
        if (mPassword == null) {
            excludeResponses = null;
        } else {
            commandQueue.add(0, mPassword);
            excludeResponses = new int[]{0};
        }

        return processCommand(commandQueue.toStringSeparated(), excludeResponses);
    }

    /**
     * Communicates with the server by sending a command and receiving the response.
     *
     * @param command The command to be sent to the server.
     * @return The result from the command sent to the server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     * @deprecated Use {@link #submit(MPDCommand)}
     */
    @Deprecated
    public List<String> send(final MPDCommand command) throws IOException, MPDException {
        return new ArrayList<>(new CommandResponse(submit(command).get()));
    }

    /**
     * Communicates with the server by sending a command and receiving the response.
     *
     * @param command The command to be sent to the server.
     * @param args    Arguments to the command to be sent to the server.
     * @return The result from the command sent to the server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     * @deprecated Use {@link #submit(CharSequence, CharSequence...)}
     */
    @Deprecated
    public List<String> send(final CharSequence command, final CharSequence... args)
            throws IOException, MPDException {
        return new ArrayList<>(new CommandResponse(submit(command, args).get()));
    }

    /**
     * Sends the commands (without separated results) which were {@code add}ed to the queue.
     *
     * @param commandQueue The CommandQueue to send.
     * @return The results of from the media server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     * @deprecated Use {@link #submit(CommandQueue)}
     */
    @Deprecated
    public List<String> send(final CommandQueue commandQueue) throws IOException, MPDException {
        return new ArrayList<>(new CommandResponse(submit(commandQueue).get()));
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
     * @return A {@link ResultFuture} for tracking and response processing.
     */
    public ResultFuture submit(final CharSequence command, final CharSequence... args) {
        return submit(MPDCommand.create(command, args));
    }

    /**
     * Submits the command to the {@link MPDExecutor}.
     *
     * @param command The command to be sent to the server.
     * @return A {@link ResultFuture} for tracking and response processing.
     */
    public ResultFuture submit(final MPDCommand command) {
        return processCommand(command);
    }

    /**
     * Submits this CommandQueue to the {@link MPDExecutor}.
     *
     * @param commandQueue The The CommandQueue to send to the server.
     * @return A {@link ResultFuture} for tracking and response processing.
     */
    public ResultFuture submit(final CommandQueue commandQueue) {
        return processCommand(commandQueue);
    }

    /**
     * Submit a CommandQueue, retrieving the result for each command in a separate
     * {@link CommandResponse}.
     *
     * @param commandQueue The CommandQueue to send.
     * @return A {@link SeparatedFuture}, which will return a {@link SeparatedResponse}.
     */
    public SeparatedFuture submitSeparated(final CommandQueue commandQueue) {
        return new SeparatedFuture(processCommandSeparated(commandQueue));
    }
}
