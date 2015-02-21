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

package com.anpmech.mpd;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A class to generate a <A HREF="http://www.musicpd.org/doc/protocol/command_lists.html">MPD
 * command list</A>.
 * <p/>
 * This class was not designed for thread safety.
 */
public class CommandQueue extends AbstractList<MPDCommand> {

    /** The initial length of the sum of command queue string lengths. */
    private static final int EMPTY_COMMAND_SIZE;

    /** Command text used to end of any command list. */
    private static final String MPD_CMD_END_BULK = "command_list_end";

    /** Command text used to begin of command list. */
    private static final String MPD_CMD_START_BULK = "command_list_begin";

    /** Command text used to begin a separated command list. */
    private static final String MPD_CMD_START_BULK_OK = "command_list_ok_begin";

    /** The class log identifier. */
    private static final String TAG = "CommandQueue";

    /** The internal command queue storage. */
    private final List<MPDCommand> mCommandQueue;

    /** The length of the command queue. */
    private int mCommandQueueStringLength;

    static {
        EMPTY_COMMAND_SIZE = MPD_CMD_START_BULK_OK.length() + MPD_CMD_END_BULK.length() + 5;
    }

    /**
     * A constructor for the CommandQueue to initialize the backing store with an empty list.
     */
    public CommandQueue() {
        this(0);
    }

    /**
     * A constructor for the CommandQueue to initialize the backing store array capacity with a
     * size given in the capacity parameter.
     *
     * @param capacity The initial capacity of this {@code CommandQueue}.
     */
    public CommandQueue(final int capacity) {
        super();

        mCommandQueue = new ArrayList<>(capacity);
        mCommandQueueStringLength = EMPTY_COMMAND_SIZE;
    }

    /**
     * Initiates a CommandQueue with the commands given in the parameter.
     *
     * @param commands The commands to initiate this CommandQueue with.
     */
    public CommandQueue(final MPDCommand... commands) {
        this(Arrays.asList(commands));
    }

    /**
     * Initiates a CommandQueue with the commands given in the parameter.
     *
     * @param commands The commands to initiate this CommandQueue with.
     */
    public CommandQueue(final Collection<MPDCommand> commands) {
        this(commands.size());

        addAll(commands);
    }

    /**
     * This method, simply, removes all newlines from the end of the StringBuilder, except for one,
     * exclusively, and if one doesn't exist, adds one.
     *
     * @param stringBuilder The string builder to clean the newline for.
     */
    private static void cleanNewline(final StringBuilder stringBuilder) {
        int newline = stringBuilder.length() - 1;
        char lastChar = stringBuilder.charAt(newline);

        if (lastChar == MPDCommand.MPD_CMD_NEWLINE) {
            do {
                newline--;
                lastChar = stringBuilder.charAt(newline);
            } while (lastChar == MPDCommand.MPD_CMD_NEWLINE);

            stringBuilder.setLength(newline + 2);
        } else {
            stringBuilder.append(MPDCommand.MPD_CMD_NEWLINE);
        }
    }

    /**
     * Add a command to the specified position of this command queue.
     *
     * @param location The position of this command queue to add the new command.
     * @param command  The command to add to this command queue.
     */
    @SuppressWarnings("RefusedBequest") // Must not call super for this method.
    @Override
    public void add(final int location, final MPDCommand command) {
        mCommandQueue.add(location, command);
        mCommandQueueStringLength += command.getCommand().length();
    }

    /**
     * Add a one argument command in a loop until the arguments are exhausted.
     *
     * @param command Command to add to the queue.
     * @param args    The args to add each, singularly, to the command queue.
     */
    public void add(final CharSequence command, final Iterable<CharSequence> args) {
        for (final CharSequence arg : args) {
            add(command, arg);
        }
    }

    /**
     * Add a command to a command to the {@code CommandQueue}.
     *
     * @param command Command to add to the queue.
     */
    public void add(final CharSequence command, final CharSequence... args) {
        add(MPDCommand.create(command, args));
    }

    /**
     * Add a command queue to the end of this command queue.
     *
     * @param commandQueue The command queue to add to this one.
     */
    public boolean addAll(final CommandQueue commandQueue) {
        mCommandQueueStringLength += commandQueue.mCommandQueueStringLength;

        return mCommandQueue.addAll(commandQueue.mCommandQueue);
    }

    /**
     * Add a command queue to the specified position of this command queue.
     *
     * @param location     The position of this command queue to add the new command queue.
     * @param commandQueue The command queue to add to this one.
     */
    public boolean addAll(final int location, final CommandQueue commandQueue) {
        mCommandQueueStringLength += commandQueue.mCommandQueueStringLength;

        return mCommandQueue.addAll(location, commandQueue.mCommandQueue);
    }

    /**
     * Removes all elements from this {@code CommandQueue}, leaving it empty.
     *
     * @see List#isEmpty
     * @see List#size
     */
    @Override
    public void clear() {
        super.clear();

        mCommandQueueStringLength = EMPTY_COMMAND_SIZE;
    }

    /**
     * Returns the element at the specified location in this list.
     *
     * @param location The index of the element to return.
     * @return The element at the specified index.
     */
    @Override
    public MPDCommand get(final int location) {
        return mCommandQueue.get(location);
    }

    /**
     * Removes the object at the specified location from this list.
     *
     * @param location the index of the object to remove.
     * @return the removed object.
     */
    @SuppressWarnings("RefusedBequest") // Must not call super for this method.
    @Override
    public MPDCommand remove(final int location) {
        return mCommandQueue.remove(location);
    }

    /**
     * Replaces the element at the specified location in this list with the
     * specified object.
     *
     * @param location the index at which to put the specified object.
     * @param object   the object to add.
     * @return the previous element at the index.
     */
    @SuppressWarnings("RefusedBequest") // Must not call super for this method.
    @Override
    public MPDCommand set(final int location, final MPDCommand object) {
        return mCommandQueue.set(location, object);
    }

    /**
     * Returns the number of elements in this {@code CommandQueue}.
     *
     * @return The number of elements in this {@code CommandQueue}.
     */
    @Override
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
     * @return A string to be parsed by either {@code MPDConnection.send()} or
     * {@code MPDConnection.sendSeparated()}.
     */
    private String toString(final boolean separated) {
        final StringBuilder stringBuilder;

        if (mCommandQueue.size() == 1) {
            stringBuilder = new StringBuilder(mCommandQueue.get(0).getCommand());
        } else {
            stringBuilder = new StringBuilder(mCommandQueueStringLength);
            if (separated) {
                stringBuilder.append(MPD_CMD_START_BULK_OK);
            } else {
                stringBuilder.append(MPD_CMD_START_BULK);
            }
            stringBuilder.append(MPDCommand.MPD_CMD_NEWLINE);

            for (final MPDCommand command : mCommandQueue) {
                stringBuilder.append(command.getCommand());
            }
            stringBuilder.append(MPD_CMD_END_BULK);
        }

        cleanNewline(stringBuilder);

        return stringBuilder.toString();
    }

    /**
     * This generates and returns the CommandQueue string used to get a response with separated
     * results,
     *
     * @return This CommandQueue string used to get a response with separated results,
     */
    public String toStringSeparated() {
        return toString(true);
    }
}
