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
import com.anpmech.mpd.commandresponse.EntryResponse;
import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.connection.MPDConnection;
import com.anpmech.mpd.exception.InvalidResponseException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class is the generic base for the {@link Directory} and {@link Listing} items, abstracted
 * for backend.
 *
 * <p>This item is returned from methods of the
 * <A HREF="http://www.musicpd.org/doc/protocol/database.html">database</A>
 * subsystem of the <A HREF="http://www.musicpd.org/doc/protocol">MPD protocol</A>.</p>
 *
 * <p>This class is mutable, but should be thread-safe.</p>
 *
 * @param <T> The AbstractDirectoryBase type.
 */
abstract class AbstractDirectoryBase<T extends AbstractDirectoryBase<T>> extends AbstractEntry<T>
        implements RefreshableItem {

    /**
     * The media server response key returned for a Directory filesystem entry.
     */
    public static final String RESPONSE_DIRECTORY = "directory";

    /**
     * The root directory in the MPD protocol is an empty space.
     */
    public static final String ROOT_DIRECTORY = "";

    /**
     * The class log identifier.
     */
    private static final String TAG = "AbstractDirectoryBase";

    /**
     * This is the synchronization lock for this Object.
     */
    protected final Object mLock = new Object();

    /**
     * This is the CommandResult for a lsinfo/listfiles for this path.
     *
     * <p>This comes uninitialized, and will be populated upon a call to
     * {@link #refresh(MPDConnection)}</p>
     */
    protected CommandResult mResult;

    /**
     * This constructor is used to create a new {@link Directory} or {@link Listing} item with a
     * ResponseObject.
     *
     * @param object The prepared {@link ResponseObject}.
     * @param result The lsinfo/listfiles CommandResult. If null, a {@link #refresh(MPDConnection)}
     *               connection will be required to regenerate it.
     */
    protected AbstractDirectoryBase(@NotNull final ResponseObject object,
            @Nullable final CommandResult result) {
        super(object);

        mResult = result;
    }

    /**
     * This method returns a file name for the given path.
     *
     * @param path The path to retrieve the filename from.
     * @return A filename, if one exists, otherwise, the given parameter.
     */
    @NotNull
    private static String getFilename(@NotNull final String path) {
        final int baseNameIndex = path.lastIndexOf('/');
        final String basename;

        if (baseNameIndex == -1) {
            basename = path;
        } else {
            basename = path.substring(baseNameIndex + 1);
        }

        return basename;
    }

    /**
     * This method returns a parent directory for the given directory.
     *
     * @param path The String to get the path.
     * @return The parent directory for the {@code path} parameter, {@link #ROOT_DIRECTORY} for the
     * root directory, and null if there is no parent (already in the root directory).
     */
    @Nullable
    private static String getParent(@NotNull final String path) {
        final int baseNameIndex = path.lastIndexOf('/');
        final String parentDirectory;

        if (baseNameIndex == -1) {

            /**
             * If the path is empty, we're already at the root directory.
             */
            if (path.isEmpty()) {
                parentDirectory = null;
            } else {
                parentDirectory = ROOT_DIRECTORY;
            }
        } else {
            parentDirectory = path.substring(0, baseNameIndex);
        }

        return parentDirectory;
    }

    /**
     * Check if a given path exists as a subdirectory.
     *
     * @param filename The subdirectory filename.
     * @return True if subdirectory exists, false otherwise.
     */
    public boolean containsPath(final CharSequence filename) {
        return mResult.contains(RESPONSE_DIRECTORY, filename);
    }

    /**
     * Returns a {@link EntryResponse} from this Item.
     *
     * @return A {@link EntryResponse} of this Item.
     */
    @NotNull
    public EntryResponse getEntries() {
        return new EntryResponse(mResult);
    }

    /**
     * Returns the filename for this Directory.
     *
     * @return The filename for this Directory.
     */
    @NotNull
    public String getFilename() {
        final String fullPath = getFullPath();

        return getFilename(fullPath);
    }

    /**
     * Retrieves a full path without the forward slash prefix.
     *
     * @return The full path without the forward slash prefix. A root directory will return empty.
     */
    @Override
    @NotNull
    public String getFullPath() {
        final String fullPath = getDirectoryFullPath();

        if (fullPath == null) {
            throw new InvalidResponseException(pathNotFoundError());
        }

        return fullPath;
    }

    /**
     * Retrieves the name of this Item.
     *
     * @return The name of this Item.
     */
    @Override
    @NotNull
    public String getName() {
        return getFilename();
    }

    /**
     * This method returns a parent directory for the given path.
     *
     * @return The parent directory for the {@code path} parameter, null if there is no parent.
     */
    @Nullable
    public String getParent() {
        return getParent(getFullPath());
    }

    /**
     * This method returns if the internal command response is empty.
     *
     * @return True if the internal command result is empty, false otherwise.
     */
    public boolean isEmpty() {
        return mResult == null;
    }
}
