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

package com.anpmech.mpd.item;

import com.anpmech.mpd.ResponseObject;
import com.anpmech.mpd.commandresponse.DirectoryResponse;
import com.anpmech.mpd.commandresponse.MusicResponse;
import com.anpmech.mpd.commandresponse.PlaylistFileResponse;
import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.connection.MPDConnection;
import com.anpmech.mpd.exception.MPDException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * This class is the generic base for the Directory items, abstracted for backend.
 *
 * <p>This class is similar to {@link Listing}, but rather than using the <A
 * HREF="http://www.musicpd.org/doc/protocol/database.html#command_listfiles">{@code listfiles}</A>
 * command, the
 * <A HREF="http://www.musicpd.org/doc/protocol/database.html#command_lsinfo">{@code lsinfo}</A>
 * server command is used. When used with the standard MPD implementation, this command provides
 * much more information about the directory listing. Unlike {@link AbstractListing} this command
 * will only list those recognized by the MPD server implementation.</p>
 *
 * <p>This item is returned from methods of the
 * <A HREF="http://www.musicpd.org/doc/protocol/database.html">database</A>
 * subsystem of the <A HREF="http://www.musicpd.org/doc/protocol">MPD protocol</A>.</p>
 *
 * <p>This class is mutable, but should be thread-safe.</p>
 *
 * @param <T> The Directory type.
 */
abstract class AbstractDirectory<T extends AbstractDirectory<T>> extends AbstractDirectoryBase<T> {

    /**
     * This is a command sent to retrieve all MPD recognized files in a directory.
     */
    private static final CharSequence CMD_LSDIR = "lsinfo";

    /**
     * This constructor is used to create a new Directory item with a ResponseObject.
     *
     * @param object The prepared {@link ResponseObject}.
     * @param lsInfo The lsinfo CommandResult. If null, a {@link #refresh(MPDConnection)}
     *               will be required to initialize it.
     */
    protected AbstractDirectory(@NotNull final ResponseObject object,
            @Nullable final CommandResult lsInfo) {
        super(object, lsInfo);
    }

    /**
     * Returns a {@link DirectoryResponse} from this {@link Directory}.
     *
     * @return A {@link DirectoryResponse} of this {@link Directory}.
     */
    @NotNull
    public DirectoryResponse getDirectoryEntries() {
        return new DirectoryResponse(mResult);
    }

    /**
     * Returns a {@link MusicResponse} from this {@link Directory}.
     *
     * @return A {@link MusicResponse} of this {@link Directory}.
     */
    @NotNull
    public MusicResponse getMusicEntries() {
        return new MusicResponse(mResult);
    }

    /**
     * Returns a {@link PlaylistFileResponse} from this {@link Directory}.
     *
     * @return A {@link PlaylistFileResponse} of this {@link Directory}.
     */
    @NotNull
    public PlaylistFileResponse getPlaylistFileEntries() {
        return new PlaylistFileResponse(mResult);
    }

    /**
     * Retrieves a database directory listing MPD recognized entries of this Directory.
     *
     * @param connection A connection to the server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    @Override
    public void refresh(@NotNull final MPDConnection connection) throws IOException, MPDException {
        synchronized (mLock) {
            mResult = connection.submit(CMD_LSDIR, getFullPath()).get();
        }
    }
}
