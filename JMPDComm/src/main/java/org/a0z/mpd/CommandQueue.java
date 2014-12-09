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

import org.a0z.mpd.connection.MPDConnection;
import org.a0z.mpd.exception.MPDException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** A class to generate and send a command queue. */
public class CommandQueue implements Iterable<MPDCommand> {

    private static final boolean DEBUG = false;

    private static final String MPD_CMD_BULK_SEP = "list_OK";

    private static final String MPD_CMD_END_BULK = "command_list_end";

    private static final String MPD_CMD_START_BULK = "command_list_begin";

    private static final String MPD_CMD_START_BULK_OK = "command_list_ok_begin";

    private static final String TAG = "CommandQueue";

    private final List<MPDCommand> mCommandQueue;

    private int mCommandQueueStringLength;

    public CommandQueue() {
        super();

        mCommandQueue = new ArrayList<>();
        mCommandQueueStringLength = getStartLength();
    }

    public CommandQueue(final int size) {
        super();

        mCommandQueue = new ArrayList<>(size);
        mCommandQueueStringLength = getStartLength();
    }

    private static int getStartLength() {
        return MPD_CMD_START_BULK_OK.length() + MPD_CMD_END_BULK.length() + 5;
    }

    /**
     * Processes the raw results from a command queue and returns a list of those results.
     *
     * @param lines The raw results of a command queue from the media server.
     * @return A list of results from the command queue.
     */
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

    /**
     * Add a command queue to the end of this command queue.
     *
     * @param commandQueue The command queue to add to this one.
     */
    public void add(final CommandQueue commandQueue) {
        mCommandQueue.addAll(commandQueue.mCommandQueue);
        mCommandQueueStringLength += commandQueue.mCommandQueueStringLength;
    }

    /**
     * Add a command queue to the specified position of this command queue.
     *
     * @param position     The position of this command queue to add the new command queue.
     * @param commandQueue The command queue to add to this one.
     */
    public void add(final int position, final CommandQueue commandQueue) {
        mCommandQueue.addAll(position, commandQueue.mCommandQueue);
        mCommandQueueStringLength += commandQueue.mCommandQueueStringLength;
    }

    /**
     * Add a command to the specified position of this command queue.
     *
     * @param position The position of this command queue to add the new command.
     * @param command  The command to add to this command queue.
     */
    public void add(final int position, final MPDCommand command) {
        mCommandQueue.add(position, command);
        mCommandQueueStringLength += command.toString().length();
    }

    /**
     * Add a command to a command to the {@code CommandQueue}.
     *
     * @param command Command to add to the queue.
     */
    public void add(final MPDCommand command) {
        mCommandQueue.add(command);
        mCommandQueueStringLength += command.toString().length();
    }

    /**
     * Add a command to a command to the {@code CommandQueue}.
     *
     * @param command Command to add to the queue.
     */
    public void add(final String command, final String... args) {
        add(new MPDCommand(command, args));
    }

    /** Clear the command queue. */
    public void clear() {
        mCommandQueueStringLength = getStartLength();
        mCommandQueue.clear();
    }

    public boolean isEmpty() {
        return mCommandQueue.isEmpty();
    }

    /**
     * Returns an {@link java.util.Iterator} for the elements in this object.
     *
     * @return An {@code Iterator} instance.
     */
    @Override
    public Iterator<MPDCommand> iterator() {
        return mCommandQueue.iterator();
    }

    /** Reverse the command queue order, useful for removing playlist entries. */
    public void reverse() {
        Collections.reverse(mCommandQueue);
    }

    /**
     * Sends the commands (without separated results) which were {@code add}ed to the queue.
     *
     * @param mpdConnection The connection to send the queued commands to.
     * @return The results of from the media server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<String> send(final MPDConnection mpdConnection) throws IOException, MPDException {
        return send(mpdConnection, false);
    }

    /**
     * Sends the commands which were {@code add}ed to the queue.
     *
     * @param mpdConnection The connection to send the queued commands to.
     * @return The results of from the media server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    private List<String> send(final MPDConnection mpdConnection, final boolean separated)
            throws IOException, MPDException {
        final MPDCommand mpdCommand;

        if (mCommandQueue.isEmpty()) {
            throw new IllegalStateException("Cannot send an empty command queue.");
        }

        if (mCommandQueue.size() == 1) {
            /** OK, it's not really a command queue. Send it anyhow. */
            mpdCommand = mCommandQueue.get(0);
        } else {
            mpdCommand = new MPDCommand(toString(separated));
        }

        if (DEBUG) {
            Log.debug(TAG, toString(separated));
        }

        return mpdConnection.sendCommand(mpdCommand);
    }

    /**
     * Sends the commands (with separated results) which were {@code add}ed to the queue.
     *
     * @param mpdConnection The connection to send the queued commands to.
     * @return The results of from the media server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public List<String[]> sendSeparated(final MPDConnection mpdConnection)
            throws IOException, MPDException {
        return separatedQueueResults(send(mpdConnection, true));
    }

    public int size() {
        return mCommandQueue.size();
    }

    /**
     * Returns the command queue in {@code String} format.
     *
     * @return The command queue as a {@code String}.
     */
    @Override
    public String toString() {
        return toString(false);
    }

    /**
     * The command queue builder.
     *
     * @param separated Whether the results should be separated.
     * @return A string to be parsed by either {@code send()} or {@code sendSeparated()}.
     */
    private String toString(final boolean separated) {
        final StringBuilder commandString = new StringBuilder(mCommandQueueStringLength);

        if (separated) {
            commandString.append(MPD_CMD_START_BULK_OK);
        } else {
            commandString.append(MPD_CMD_START_BULK);
        }
        commandString.append(MPDCommand.MPD_CMD_NEWLINE);

        for (final MPDCommand command : mCommandQueue) {
            commandString.append(command);
        }
        commandString.append(MPD_CMD_END_BULK);

        return commandString.toString();
    }
}
