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
 * This class creates a Listing Item, an abstraction of a filesystem directory in the <A
 * HREF="http://www.musicpd.org/doc/protocol">MPD Protocol</A>, for the Java backend.
 *
 * <p>This class is similar to {@link Directory}, but rather than using the
 * <A HREF="http://www.musicpd.org/doc/protocol/database.html#command_lsinfo">{@code lsinfo}</A>
 * server command, the
 * <A HREF="http://www.musicpd.org/doc/protocol/database.html#command_listfiles">{@code
 * listfiles}</A>
 * server command is used. When used with the standard MPD implementation, this command provides
 * much less information about the directory entries, but provides files which are not recognized
 * by the MPD server implementation.</p>
 */
public class Listing extends AbstractDirectory<Listing> {

    /**
     * The class log identifier.
     */
    private static final String TAG = "Listing";

    /**
     * The copy constructor for this class.
     *
     * @param entry The {@link Entry} to copy.
     */
    public Listing(@NotNull final Listing entry) {
        super(entry.mResponseObject, entry.mResult);
    }

    /**
     * This constructor is used to create a new Listing item with a ResponseObject.
     *
     * @param object    The prepared ResponseObject.
     * @param listFiles The listfiles CommandResult. If null, a {@link #refresh(MPDConnection)}
     *                  call be required to initialize it.
     * @see #byPath(String)
     * @see #byResponse(String)
     */
    private Listing(@NotNull final ResponseObject object,
            @Nullable final CommandResult listFiles) {
        super(object, listFiles);
    }

    /**
     * This method is used to create a new Listing by path.
     *
     * @param path The path of the directory, if null, the {@link #ROOT_DIRECTORY} will be the
     *             path.
     * @return The new Listing.
     */
    public static Listing byPath(@Nullable final String path) {
        final String listing;

        if (path == null) {
            listing = ROOT_DIRECTORY;
        } else {
            listing = path;
        }

        return new Listing(new ResponseObject(listing, null), null);
    }

    /**
     * This method is used to construct a new Listing by server response.
     *
     * @param response The server response.
     * @return The new Listing.
     */
    public static Listing byResponse(@NotNull final String response) {
        return new Listing(new ResponseObject(null, response), null);
    }
}
