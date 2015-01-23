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

import java.util.Arrays;
import java.util.List;

/**
 * A class representing one MPD protocol command and it's arguments.
 */
public class MPDCommand {

    /**
     * MPD default TCP port.
     */
    public static final int DEFAULT_MPD_PORT = 6600;

    public static final String MPD_CMD_CLEARERROR = "clearerror";

    public static final String MPD_CMD_CLOSE = "close";

    public static final String MPD_CMD_CONSUME = "consume";

    public static final String MPD_CMD_COUNT = "count";

    public static final String MPD_CMD_CROSSFADE = "crossfade";

    public static final String MPD_CMD_FIND = "find";

    /** Added in MPD protocol 0.16.0 */
    public static final String MPD_CMD_FIND_ADD = "findadd";

    public static final String MPD_CMD_GROUP = "group";

    public static final CharSequence MPD_CMD_IDLE = "idle";

    public static final String MPD_CMD_KILL = "kill";

    public static final String MPD_CMD_LISTALL = "listall";

    public static final String MPD_CMD_LISTALLINFO = "listallinfo";

    public static final String MPD_CMD_LISTPLAYLISTS = "listplaylists";

    public static final String MPD_CMD_LIST_TAG = "list";

    public static final String MPD_CMD_LSDIR = "lsinfo";

    public static final char MPD_CMD_NEWLINE = '\n';

    public static final CharSequence MPD_CMD_NEXT = "next";

    public static final String MPD_CMD_OUTPUTDISABLE = "disableoutput";

    public static final String MPD_CMD_OUTPUTENABLE = "enableoutput";

    public static final String MPD_CMD_OUTPUTS = "outputs";

    public static final String MPD_CMD_PASSWORD = "password";

    public static final String MPD_CMD_PAUSE = "pause";

    public static final String MPD_CMD_PING = "ping";

    public static final String MPD_CMD_PLAY = "play";

    public static final CharSequence MPD_CMD_PLAYLIST_ADD = "playlistadd";

    public static final CharSequence MPD_CMD_PLAYLIST_DEL = "playlistdelete";

    public static final String MPD_CMD_PLAYLIST_INFO = "listplaylistinfo";

    public static final CharSequence MPD_CMD_PLAYLIST_MOVE = "playlistmove";

    public static final String MPD_CMD_PLAY_ID = "playid";

    public static final CharSequence MPD_CMD_PREV = "previous";

    public static final String MPD_CMD_RANDOM = "random";

    public static final String MPD_CMD_REFRESH = "update";

    public static final String MPD_CMD_REPEAT = "repeat";

    public static final String MPD_CMD_SEARCH = "search";

    /** Added in MPD protocol 0.17.0. */
    public static final String MPD_CMD_SEARCH_ADD_PLAYLIST = "searchaddpl";

    public static final String MPD_CMD_SEEK = "seek";

    public static final String MPD_CMD_SEEK_ID = "seekid";

    public static final String MPD_CMD_SET_VOLUME = "setvol";

    public static final String MPD_CMD_SINGLE = "single";

    public static final String MPD_CMD_STATISTICS = "stats";

    public static final String MPD_CMD_STATUS = "status";

    public static final String MPD_CMD_STOP = "stop";

    public static final String MPD_SEARCH_FILENAME = "filename";

    private static final int[] EMPTY_INT_ARRAY = new int[0];

    private static final List<CharSequence> NON_RETRYABLE_COMMANDS;

    /** The class log identifier. */
    private static final String TAG = "MPDCommand";

    /** The base protocol command. */
    private final CharSequence mBaseCommand;

    /** The fully formatted protocol command and arguments. */
    private final String mCommand;

    /** Storage for whether this command can be resent in the case of send failure. */
    private final boolean mIsRetryable;

    /** This field stores any {@code ACK} errors to be considered as non-fatal. */
    private final int[] mNonfatalErrors;

    static {
        NON_RETRYABLE_COMMANDS = Arrays.asList(MPD_CMD_PLAYLIST_MOVE, MPD_CMD_NEXT,
                MPD_CMD_PLAYLIST_ADD, MPD_CMD_PLAYLIST_DEL, MPD_CMD_PREV);
    }

    /**
     * The constructor for a command to be sent to the MPD protocol compatible media server.
     *
     * @param baseCommand        The base protocol command to be sent.
     * @param command            The full command with arguments to to be sent.
     * @param nonfatalErrorCodes Errors to consider as non-fatal for this command. These MPD error
     *                           codes with this command will not return any exception.
     * @see #create(CharSequence, CharSequence...)
     * @see #create(CharSequence, int[], CharSequence...)
     */
    private MPDCommand(final CharSequence baseCommand, final String command,
            final int[] nonfatalErrorCodes) {
        super();

        mBaseCommand = baseCommand;
        mCommand = command;
        mIsRetryable = NON_RETRYABLE_COMMANDS.contains(baseCommand);
        mNonfatalErrors = nonfatalErrorCodes;
    }

    /**
     * Translates a boolean value to the MPD protocol.
     *
     * @param valueToTranslate The boolean to translate.
     * @return The MPD protocol boolean value.
     */
    public static String booleanValue(final boolean valueToTranslate) {
        final String result;

        if (valueToTranslate) {
            result = "1";
        } else {
            result = "0";
        }

        return result;
    }

    /**
     * This creates a command to be sent to the MPD protocol compatible media server, with a
     * parameter to add error codes to consider as non-fatal.
     *
     * @param command            The protocol command for this {@code MPDCommand}.
     * @param nonfatalErrorCodes Errors to consider as non-fatal for this command. These MPD error
     *                           codes with this command will not return any exception.
     * @param args               The arguments for the command argument.
     * @return An object to use to send protocol commands to the server.
     */
    public static MPDCommand create(final CharSequence command, final int[] nonfatalErrorCodes,
            final CharSequence... args) {
        return new MPDCommand(command, getCommand(command, args), nonfatalErrorCodes);
    }

    /**
     * This creates a command to be sent to the MPD protocol compatible media server.
     *
     * @param command The protocol command for this {@code MPDCommand}.
     * @param args    The arguments for the command argument.
     * @return An object to use to send protocol commands to the server.
     */
    public static MPDCommand create(final CharSequence command, final CharSequence... args) {
        return create(command, EMPTY_INT_ARRAY, args);
    }

    /**
     * Builds the command string and arguments into one StringBuilder.
     *
     * @param command The protocol command for this {@code MPDCommand}.
     * @param args    The arguments for the command argument.
     * @return A {@code StringBuilder} used for the object's processing.
     */
    private static String getCommand(final CharSequence command, final CharSequence[] args) {
        final StringBuilder outString = new StringBuilder(command.length() + (args.length << 4));

        outString.append(command);
        for (final CharSequence arg : args) {
            if (arg != null) {
                outString.append(" \"");

                for (int i = 0; i < arg.length(); i++) {
                    final char c = arg.charAt(i);

                    if (c == '"') {
                        outString.append("\\\"");
                    } else {
                        outString.append(c);
                    }
                }

                outString.append('"');
            }
        }
        outString.append(MPD_CMD_NEWLINE);

        return outString.toString();
    }

    /**
     * This method returns the protocol command for this object.
     *
     * @return The protocol command for this object.
     */
    public CharSequence getBaseCommand() {
        return mBaseCommand;
    }

    /**
     * This method returns the protocol command and arguments for this object.
     *
     * @return The protocol command and arguments for this object.
     */
    public String getCommand() {
        return mCommand;
    }

    /**
     * This method is used to check if this command was loaded with a command code, specified by
     * the parameter, which is to be considered as non-fatal.
     *
     * @param errorCodeToCheck The {@code ACK} error code to check.
     * @return True if the {@code ACK} error code was loaded as non-fatal, false otherwise.
     */
    public boolean isErrorNonfatal(final int errorCodeToCheck) {
        boolean result = false;

        for (final int errorCode : mNonfatalErrors) {
            if (errorCode == errorCodeToCheck) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * This returns whether the base command object has been flagged as a retryable command, if
     * not, upon possible failure this command will not be retried.
     *
     * @return True if this command will be retried upon failure, false otherwise.
     */
    public boolean isRetryable() {
        return mIsRetryable;
    }

    /**
     * This returns a debugging string with the results of all internal fields.
     *
     * @return A debugging string with the results of all internal fields.
     */
    @Override
    public String toString() {
        return "MPDCommand{" +
                "mBaseCommand=" + mBaseCommand +
                ", mCommand=" + mCommand +
                ", mIsRetryable=" + mIsRetryable +
                ", mNonfatalErrors=" + Arrays.toString(mNonfatalErrors) +
                '}';
    }
}
