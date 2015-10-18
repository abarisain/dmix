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
import com.anpmech.mpd.MPDCommand;
import com.anpmech.mpd.commandresponse.CommandResponse;
import com.anpmech.mpd.concurrent.ResultFuture;
import com.anpmech.mpd.exception.MPDException;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * This is the foundation for a {@link Callable} class which sends one {@link MPDCommand} or
 * {@link com.anpmech.mpd.CommandQueue} string over a blocking connection, returning a
 * {@link CommandResult}.
 */
abstract class IOCommandProcessor implements Callable<CommandResult> {

    /**
     * The debug tracker flag.
     */
    private static final boolean DEBUG = false;

    /**
     * Maximum number of times to attempt command processing.
     */
    private static final int MAX_REQUEST_RETRY = 3;

    /**
     * Use the {@link BufferedReader} standard buffer size.
     */
    private static final int READ_BUFFER_SIZE = 8192;

    /**
     * This is the read blocking timeout of the stream, in seconds.
     */
    private static final long STREAM_TIMEOUT = 5L;

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
     * The index of responses that will be excluded from a split response.
     */
    private final int[] mExcludeResponses;

    /**
     * The connection header given only during a new connection.
     */
    private String mHeader;

    /**
     * The constructor for this CommandProcessor.
     *
     * @param connectionStatus The status tracker for this connection.
     * @param commandString    The command string to be processed.
     * @param excludeResponses This is used to manually exclude responses from split
     *                         {@link CommandResponse} inclusion.
     */
    IOCommandProcessor(final MPDConnectionStatus connectionStatus, final String commandString,
            final int[] excludeResponses) {
        super();

        mConnectionStatus = connectionStatus;
        mCommandString = commandString;
        mExcludeResponses = excludeResponses;
    }

    /**
     * Checks the MPD response for validity.
     *
     * @param stringBuilder The StringBuilder response to check.
     * @return True if the command response is valid, false otherwise.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    private static boolean checkResponse(final StringBuilder stringBuilder) throws MPDException {
        /** Remove the newline */
        final int length = stringBuilder.length() - 1;
        boolean isOK = false;

        /** Check for exclusive OK */
        if (length == 2 && MPDConnection.CMD_RESPONSE_OK
                .contentEquals(stringBuilder.subSequence(length - 2, length))) {
            isOK = true;
        } else {
            final int lastNewline = stringBuilder.lastIndexOf("\n", length - 1);

            if (lastNewline == -1) {
                /**
                 * Nothing is better at parsing ACK than the MPDException, itself.
                 */
                final MPDException mpdException = new MPDException(stringBuilder.toString());

                if (mpdException.isACKError()) {
                    throw mpdException;
                }
            } else {
                /** Check for OK suffix with newline. */
                final CharSequence subLine = stringBuilder.subSequence(lastNewline + 1, length);
                final int newLineDifference = length - lastNewline;

                if (newLineDifference == 3 && subLine.equals(MPDConnection.CMD_RESPONSE_OK)) {
                    isOK = true;
                }
            }
        }

        return isOK;
    }

    /**
     * This method outputs the {@code line} parameter to {@link Log#debug(String, String)} if
     * {@link #DEBUG} is set to true.
     *
     * @param line The {@link String} to output to the log.
     */
    protected static void debug(final String line) {
        if (DEBUG) {
            Log.debug(TAG, line);
        }
    }

    /**
     * Common code used to disconnect a IOSocketSet.
     *
     * @param socketSet The IOSocketSet to disconnect and close.
     */
    protected static void disconnect(final IOSocketSet socketSet) {
        if (socketSet != null) {
            try {
                socketSet.close();
            } catch (final IOException e) {
                Log.warning(TAG, IOSocketSet.ERROR_FAILED_TO_CLOSE, e);
            }
        }
    }

    /**
     * This is the default class method.
     *
     * @return A {@code CommandResponse} from the processed command.
     */
    @Override
    public final CommandResult call() throws IOException, MPDException {
        CommandResult commandResult = null;

        for (int resendTries = 0; resendTries < MAX_REQUEST_RETRY; resendTries++) {
            try {
                checkCancelled();

                final IOSocketSet socketSet = popSocketSet();
                write(socketSet);
                commandResult = new CommandResult(mHeader, read(socketSet));
                pushSocketSet(socketSet);
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
         * CommandResponse should be assigned prior to this conditional.
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
     * This is the low level media server connection method.
     *
     * @param socketSet The socket set to retrieve the connection header for.
     * @throws IOException Thrown upon a communication error with the server.
     */
    protected void innerConnect(final IOSocketSet socketSet) throws IOException {
        final ResultFuture future = socketSet.startTimeout(STREAM_TIMEOUT, TimeUnit.SECONDS);
        mHeader = socketSet.getReader().readLine();
        future.cancel(true);

        if (mHeader == null) {
            throw new IOException("No response from server.");
        }

        /** Protocol says OK will begin the session, otherwise assume IO error. */
        if (!mHeader.startsWith(MPDConnection.CMD_RESPONSE_OK)) {
            throw new IOException("Bogus response from server: " + mHeader);
        }

        checkCancelled();
        mConnectionStatus.statusChangeConnected();
    }

    /**
     * Pops off the stack or creates a new {@link IOSocketSet} then validates and connects if
     * necessary.
     *
     * @return A connected and validated IOSocketSet.
     * @throws IOException Thrown if there was a problem reading from from the media server.
     */
    abstract IOSocketSet popSocketSet() throws IOException;

    /**
     * Pushes a {@link IOSocketSet} back onto the stack for possible later use, if still valid.
     *
     * @param socketSet A connected and validated IOSocketSet.
     */
    abstract void pushSocketSet(final IOSocketSet socketSet);

    /**
     * Read the server response after a {@code write()} to the server.
     *
     * @param socketSet The socket set used to read the server response.
     * @return A String list of responses.
     * @throws IOException  Thrown if there was a problem reading from from the media server.
     * @throws MPDException Thrown if there was a server side error with the command that was
     *                      sent.
     */
    private String read(final IOSocketSet socketSet) throws MPDException, IOException {
        final BufferedReader in = socketSet.getReader();
        final CharBuffer charBuffer = CharBuffer.allocate(READ_BUFFER_SIZE);
        final StringBuilder stringBuilder = new StringBuilder();
        boolean invalidResponse = true;

        try {
            mConnectionStatus.setBlocked();
            while (invalidResponse) {
                /**
                 * In the next line we block. This block will last until more data is received,
                 * socket timeout or the connection is lost; the former would be atypical.
                 */
                if (in.read(charBuffer) == -1) {
                    throw new EOFException("Connection lost");
                }

                charBuffer.flip();
                stringBuilder.append(charBuffer);

                final int length = stringBuilder.length() - 1;
                /** All responses end with a newline. */
                if (stringBuilder.charAt(length) == MPDCommand.MPD_CMD_NEWLINE &&
                        checkResponse(stringBuilder)) {
                    /** Remove the OK and newline. */
                    stringBuilder.setLength(length - 2);
                    invalidResponse = false;
                }

                charBuffer.clear();
            }
        } finally {
            /** Removing the blocking flag is paramount. */
            mConnectionStatus.setNotBlocked();
        }

        removeExcludedResponses(stringBuilder);
        return stringBuilder.toString();
    }

    private void removeExcludedResponses(final StringBuilder stringBuilder) {
        if (mExcludeResponses != null) {
            final int length = mExcludeResponses.length;
            int excludedPosition = 0;
            int stringPosition = 0;
            int positionsExcluded = 0;

            Arrays.sort(mExcludeResponses);

            while (positionsExcluded < length) {
                final int currentPosition =
                        stringBuilder.indexOf(MPDConnection.MPD_CMD_BULK_SEP, stringPosition);

                if (currentPosition == -1 && length == 1 && mExcludeResponses[0] == 0) {
                    // Not sure why someone would do this, but, for correctness.
                    stringBuilder.setLength(0);
                    positionsExcluded++;
                    excludedPosition++;
                } else {
                    if (Arrays.binarySearch(mExcludeResponses, excludedPosition) >= 0) {
                        final int endPosition = currentPosition +
                                MPDConnection.MPD_CMD_BULK_SEP.length() + 1;
                        stringBuilder.delete(stringPosition, endPosition);
                        positionsExcluded++;
                    }
                    excludedPosition++;
                    stringPosition = currentPosition;
                }
            }
        }
    }

    /**
     * Returns whether it is necessary to reconnect.
     *
     * @param socketSet The socket set to use for checking whether this instance needs to
     *                  reconnect.
     * @return True if a reconnection is required, false otherwise.
     */
    protected boolean shouldReconnect(final IOSocketSet socketSet) {
        final boolean shouldReconnect;

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
            shouldReconnect = mConnectionStatus.isCancelled();
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
                '}';
    }

    /**
     * Sends the command to the server.
     *
     * @param socketSet The socket set used to send the command to the server.
     * @throws IOException Thrown upon error transferring command to media server.
     */
    private void write(final IOSocketSet socketSet) throws IOException {
        final OutputStreamWriter writer = socketSet.getWriter();

        try {
            mConnectionStatus.setBlocked();
            writer.write(mCommandString);
            writer.flush();
        } finally {
            mConnectionStatus.setNotBlocked();
        }
    }
}