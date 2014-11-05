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

package org.a0z.mpd.subsystem;

import org.a0z.mpd.connection.MPDConnection;
import org.a0z.mpd.exception.MPDException;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.a0z.mpd.Tools.parseResponse;

/**
 * A class to manage the
 * <A HREF="http://www.musicpd.org/doc/protocol/reflection_commands.html">reflection</A> subsystem
 * of the <A HREF="http://www.musicpd.org/doc/protocol">MPD protocol</A>. This will query various
 * capabilities and configuration for the currently connected media server.
 */
public class Reflection {

    /**
     * Command text required to generate a command to retrieve a list of permitted commands.
     * <BR><BR>
     * <B>Protocol command syntax:</B><BR> {@code commands}
     * <BR><BR>
     * <BR><B>Sample protocol output:</B><BR>
     * {@code commands}<BR>
     * {@code command: add}<BR>
     * ... (removed for clarity)<BR>
     * {@code OK}<BR>
     */
    public static final String CMD_ACTION_COMMANDS = "commands";

    /**
     * Command text required to generate a command to retrieve server configuration options.
     * <BR><BR>
     * <B>This is currently not functional for this library due to lack of socket connection
     * functionality.</B>
     */
    public static final String CMD_ACTION_CONFIG = "config";

    /**
     * Command text required to generate a command to receive a list of supported decoders.
     * <BR><BR>
     * <B>Protocol command syntax:</B> {@code decoders}
     * <BR><BR>
     * <BR><B>Sample protocol output:</B><BR>
     * {@code decoders}<BR>
     * {@code plugin: mad}<BR>
     * {@code suffix: mp3}<BR>
     * {@code suffix: mp2}<BR>
     * {@code mime_type: audio/mpeg}<BR>
     * {@code OK}
     */
    public static final String CMD_ACTION_DECODERS = "decoders";

    /**
     * Command text required to generate a command to receive a list of non-permitted commands.
     * <BR><BR>
     * <B>Protocol command syntax:</B><BR> {@code notcommands}
     * <BR><BR>
     * <BR><B>Sample protocol output:</B><BR>
     * {@code notcommands}<BR>
     * {@code command: config}<BR>
     * {@code command: kill}<BR>
     * {@code OK}<BR>
     */
    public static final String CMD_ACTION_NOT_COMMANDS = "notcommands";

    /**
     * Command text required to generate a command to receive a list of available metadata for
     * {@code Music} objects.
     * <BR><BR>
     * <B>Protocol command syntax:</B> {@code tagtypes}
     * {@code tagtypes}<BR>
     * {@code tagtype: Artist}<BR>
     * {@code tagtype: ArtistSort}<BR>
     * ... (removed for clarity)<BR>
     * {@code OK}<BR>
     */
    public static final String CMD_ACTION_TAG_TYPES = "tagtypes";

    /**
     * Command text required to generate a command to receive a list of URL handlers.
     * <BR><BR>
     * <B>Protocol command syntax:</B><BR> {@code urlhandlers}
     * <BR><BR>
     * <BR><B>Sample protocol output:</B><BR>
     * {@code urlhandlers}<BR>
     * {@code handler: file}<BR>
     * {@code handler: http}<BR>
     * {@code handler: https}<BR>
     * {@code handler: local}<BR>
     * {@code OK}<BR>
     */
    public static final String CMD_ACTION_URL_HANDLERS = "urlhandlers";

    /**
     * A response returned from the {@link #CMD_ACTION_COMMANDS} command.
     */
    public static final String CMD_RESPONSE_COMMANDS = "command";

    /**
     * A response returned from the {@link #CMD_ACTION_DECODERS} command.
     */
    public static final String CMD_RESPONSE_DECODER_MIME_TYPE = "mimetype";

    /**
     * A response returned from the {@link #CMD_ACTION_DECODERS} command.
     */
    public static final String CMD_RESPONSE_DECODER_PLUGIN = "plugin";

    /**
     * A response returned from the {@link #CMD_ACTION_DECODERS} command.
     */
    public static final String CMD_RESPONSE_DECODER_SUFFIX = "suffix";

    /**
     * A response key returned from the {@link #CMD_ACTION_TAG_TYPES} command.
     */
    public static final String CMD_RESPONSE_TAG_TYPES = "tagtype";

    /**
     * A response key returned from the {@link #CMD_ACTION_URL_HANDLERS} command.
     */
    public static final String CMD_RESPONSE_URL_HANDLERS = "handler";

    /**
     * The current connection to the media server used to
     * query the media server for reflection handling.
     */
    private final MPDConnection mConnection;

    /**
     * Constructor for the MPD protocol {@code Reflection} subsystem.
     *
     * @param connection The connection to use to query the media server for reflection handling.
     */
    public Reflection(final MPDConnection connection) {
        super();

        mConnection = connection;
    }

    /**
     * Retrieves and returns a collection of available commands on
     * the current media server with the current permissions.
     *
     * @return A collection of available commands with the
     * current permissions on the connected media server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     * @see org.a0z.mpd.MPDCommand#MPD_CMD_PASSWORD For modifying available commands.
     */
    public Collection<String> getCommands() throws IOException, MPDException {
        return getList(CMD_ACTION_COMMANDS, CMD_RESPONSE_COMMANDS);
    }

    /**
     * Retrieves and returns a list of all available file type
     * suffixes supported by the connected media server.
     *
     * @return A collection of supported file type suffixes.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public Collection<String> getFileSuffixes() throws IOException, MPDException {
        return getList(CMD_ACTION_DECODERS, CMD_RESPONSE_DECODER_SUFFIX);
    }

    /**
     * A generic method to send a {@code command} and retrieve only {@code element} responses.
     *
     * @param command The command text to send.
     * @param element The command response to add to the collection.
     * @return A collection of responses matching {@code element} as a key value.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    private Collection<String> getList(final String command, final String element)
            throws IOException, MPDException {
        final List<String> response = mConnection.sendCommand(command);

        return parseResponse(response, element);
    }

    /**
     * Retrieves and returns a collection of all available
     * mime types supported by the connected media server.
     *
     * @return A list of all available mime types for the connected media server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public Collection<String> getMIMETypes() throws IOException, MPDException {
        return getList(CMD_ACTION_DECODERS, CMD_RESPONSE_DECODER_MIME_TYPE);
    }

    /**
     * Returns a collection of commands explicitly not permitted for use.
     *
     * Retrieves and returns a collection of commands explicitly not permitted
     * to use on the current media server with the current permissions.
     *
     * @return A collection of commands explicitly not permitted for use on the currently connected
     * se
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     * @see org.a0z.mpd.MPDCommand#MPD_CMD_PASSWORD For modifying available commands.
     */
    public Collection<String> getNotCommands() throws IOException, MPDException {
        return getList(CMD_ACTION_NOT_COMMANDS, CMD_RESPONSE_COMMANDS);
    }

    /**
     * Returns a list of available {@code Music} metadata tag types which are available from the
     * currently connected media server.
     *
     * @return A collection of metadata tag types from the current server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public Collection<String> getTagTypes() throws IOException, MPDException {
        return getList(CMD_ACTION_TAG_TYPES, CMD_RESPONSE_TAG_TYPES);
    }

    /**
     * Retrieves and returns a list of URL handlers.
     *
     * @return A collection of available URL handlers for the connected media server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public Collection<String> getURLHandlers() throws IOException, MPDException {
        return getList(CMD_ACTION_URL_HANDLERS, CMD_RESPONSE_URL_HANDLERS);
    }
}
