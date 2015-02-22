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

import com.anpmech.mpd.Log;
import com.anpmech.mpd.exception.MPDException;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * This is the foundation for a {@link java.util.concurrent.Callable} class which sends one {@link
 * com.anpmech.mpd.MPDCommand} or {@link com.anpmech.mpd.CommandQueue} string over a blocking
 * connection, returning a {@link com.anpmech.mpd.connection.CommandResult}.
 */
abstract class IOCommandProcessor implements Callable<CommandResult> {

    /**
     * The command response for an unsuccessful command.
     */
    static final String CMD_RESPONSE_ACK = "ACK";

    /**
     * The debug tracker flag.
     */
    private static final boolean DEBUG = false;

    /**
     * Maximum number of times to attempt command processing.
     */
    private static final int MAX_REQUEST_RETRY = 3;

    /**
     * The class log identifier.
     */
    private static final String TAG = "CommandProcessor";

    /**
     * The command to be processed.
     */
    private final String mCommandString;

    /**
     * The connection status associated with this socket.
     */
    private final MPDConnectionStatus mConnectionStatus;

    /**
     * The constructor for this CommandProcessor.
     *
     * @param connectionStatus The status tracker for this connection.
     * @param commandString    The command string to be processed.
     */
    IOCommandProcessor(final MPDConnectionStatus connectionStatus, final String commandString) {
        super();

        mConnectionStatus = connectionStatus;
        mCommandString = commandString;
    }

    /**
     * This method outputs the {@code line} parameter to {@link com.anpmech.mpd.Log#debug(String,
     * String)} if {@link #DEBUG} is set to true.
     *
     * @param line The {@link String} to output to the log.
     */
    protected static void debug(final String line) {
        if (DEBUG) {
            Log.debug(TAG, line);
        }
    }

    /**
     * This is the default class method.
     *
     * @return A {@code CommandResult} from the processed command.
     */
    @Override
    public final CommandResult call() throws IOException, MPDException {
        String header = null;
        CommandResult commandResult = null;

        for (int resendTries = 0; resendTries < MAX_REQUEST_RETRY; resendTries++) {
            checkCancelled();

            try {
                if (shouldReconnect()) {
                    header = innerConnect();
                }
                write();
                commandResult = new CommandResult(header, read());
                break;
            } catch (final IOException e) {
                if (resendTries + 1 == MAX_REQUEST_RETRY || mConnectionStatus.isCancelled()) {
                    mConnectionStatus.statusChangeDisconnected(e.getLocalizedMessage());
                    throw e;
                } else {
                    mConnectionStatus.statusChangeConnecting();
                }
            }
        }

        /**
         * CommandResult should be assigned prior to this conditional.
         */
        if (commandResult == null) {
            throw new IllegalStateException("Command result unassigned: " + toString());
        }

        return commandResult;
    }

    /**
     * Act upon a cancelled connection by throwing a IOException.
     *
     * @throws IOException Thrown if the connection is cancelled.
     */
    private void checkCancelled() throws IOException {
        if (mConnectionStatus.isCancelled()) {
            throw new IOException("Connection cancelled.");
        }
    }

    /**
     * This returns a socket for the current abstraction.
     *
     * @return A IOSocketSet for this connection address.
     * @see #resetSocketSet()
     */
    abstract IOSocketSet getSocketSet();

    /**
     * This is the low level media server connection method.
     *
     * @return The initial response from the connection.
     * @throws IOException Thrown upon a communication error with the server.
     */
    private String innerConnect() throws IOException {
        // Always release existing socket if any before creating a new one
        resetSocketSet();
        final String line = getSocketSet().getReader().readLine();

        if (line == null) {
            throw new IOException("No response from server.");
        }

        /** Protocol says OK will begin the session, otherwise assume IO error. */
        if (!line.startsWith(MPDConnection.CMD_RESPONSE_OK)) {
            throw new IOException("Bogus response from server: " + line);
        }

        checkCancelled();
        mConnectionStatus.statusChangeConnected();

        return line;
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
        final BufferedReader in = getSocketSet().getReader();
        boolean validResponse = false;

        try {
            mConnectionStatus.setBlocked();
            for (String line = in.readLine(); line != null; line = in.readLine()) {

                if (line.startsWith(MPDConnection.CMD_RESPONSE_OK)) {
                    validResponse = true;
                    break;
                }

                if (line.startsWith(CMD_RESPONSE_ACK)) {
                    throw new MPDException(line);
                }
                result.add(line);
            }
        } finally {
            mConnectionStatus.setNotBlocked();
        }

        if (!validResponse) {
            // Close socket if there is no response...
            // Something is wrong (e.g. MPD shutdown..)
            throw new EOFException("Connection lost");
        }
        return result;
    }

    /**
     * This method should close and remove the old socket, and set a new socket.
     *
     * @see #getSocketSet()
     */
    abstract void resetSocketSet() throws IOException;

    /**
     * Returns whether it is necessary to reconnect.
     *
     * @return True if a reconnection is required, false otherwise.
     */
    private boolean shouldReconnect() {
        final boolean shouldReconnect;
        final IOSocketSet socketSet = getSocketSet();

        if (socketSet == null || !socketSet.isValid()) {
            /**
             * If the SocketSet hasn't been generated yet, or is invalid, reconnect.
             */
            shouldReconnect = true;
        } else if (mConnectionStatus.isConnecting() || mConnectionStatus.isBlocked()) {
            /**
             * If we're connecting or the connection is blocked, interrupt through reconnection.
             * Arbitrarily interrupting a blocked connection is probably not the best thing to do,
             * but we're left with relatively few good options in this scenario.
             */
            shouldReconnect = true;
        } else {
            /**
             * Finally, if the connection has been cancelled, we shouldn't reconnect.
             */
            shouldReconnect = !mConnectionStatus.isCancelled();
        }

        return shouldReconnect;
    }

    /**
     * A debug helper method for this class.
     *
     * @return A debug string for this class.
     */
    @Override
    public String toString() {
        return "IOCommandProcessor{" +
                "mCommandString='" + mCommandString + '\'' +
                ", mConnectionStatus=" + mConnectionStatus +
                ", getSocketSet{" + getSocketSet() + " }," +
                '}';
    }

    /**
     * Sends the command to the server.
     *
     * @throws IOException Thrown upon error transferring command to media server.
     */
    private void write() throws IOException {
        final OutputStreamWriter writer = getSocketSet().getWriter();

        try {
            mConnectionStatus.setBlocked();
            writer.write(mCommandString);
            writer.flush();
        } finally {
            mConnectionStatus.setNotBlocked();
        }
    }
}