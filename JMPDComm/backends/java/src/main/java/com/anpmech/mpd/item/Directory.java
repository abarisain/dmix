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

package com.anpmech.mpd.item;

import com.anpmech.mpd.ResponseObject;
import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.connection.MPDConnection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class creates a Directory Item, an abstraction of a filesystem directory in the <A
 * HREF="http://www.musicpd.org/doc/protocol">MPD Protocol</A>, for the Java backend.
 *
 * <p>This class is similar to {@link Listing}, but rather than using the
 * <A HREF="http://www.musicpd.org/doc/protocol/database.html#command_listfiles">{@code
 * listfiles}</A>
 * command, the
 * <A HREF="http://www.musicpd.org/doc/protocol/database.html#command_lsinfo">{@code lsinfo}</A>
 * server command is used. When used with the standard MPD implementation, this command provides
 * much more information about the directory listing. Unlike {@link AbstractListing} this command
 * will only list those recognized by the MPD server implementation.</p>
 */
public class Directory extends AbstractDirectory<Directory> {

    /**
     * The class log identifier.
     */
    private static final String TAG = "Directory";

    /**
     * The copy constructor for this class.
     *
     * @param entry The {@link Entry} to copy.
     */
    public Directory(@NotNull final Directory entry) {
        super(entry.mResponseObject, entry.mResult);
    }

    /**
     * This constructor is used to create a new Directory item with a ResponseObject.
     *
     * @param object The prepared ResponseObject.
     * @param lsInfo The {@code lsinfo} CommandResult. If null, a {@link #refresh(MPDConnection)}
     *               will be required to regenerate it.
     * @see #byPath(String)
     * @see #byResponse(String)
     */
    private Directory(@NotNull final ResponseObject object, @Nullable final CommandResult lsInfo) {
        super(object, lsInfo);
    }

    /**
     * This method is used to create a new Directory by path.
     *
     * @param path The path of the directory, if null, the {@link #ROOT_DIRECTORY} will be the
     *             path.
     * @return The new Directory.
     */
    public static Directory byPath(@Nullable final String path) {
        final String directory;

        if (path == null) {
            directory = ROOT_DIRECTORY;
        } else {
            directory = path;
        }

        return new Directory(new ResponseObject(directory, null), null);
    }

    /**
     * This method is used to construct a new Directory by server response.
     *
     * @param response The server response.
     * @return The new Directory.
     */
    public static Directory byResponse(@NotNull final String response) {
        return new Directory(new ResponseObject(null, response), null);
    }
}
