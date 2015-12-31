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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class creates a Directory Item, an abstraction of a filesystem directory in the <A
 * HREF="http://www.musicpd.org/doc/protocol">MPD Protocol</A>, for the Android backend.
 *
 * <p>This class is similar to {@link Listing}, but rather than using the
 * <A HREF="http://www.musicpd.org/doc/protocol/database.html#command_listfiles">{@code
 * listfiles}</A> command, the
 * <A HREF="http://www.musicpd.org/doc/protocol/database.html#command_lsinfo">{@code lsinfo}</A>
 * server command is used. When used with the standard MPD implementation, this command provides
 * much more information about the directory listing. Unlike {@link AbstractListing} this command
 * will only list those recognized by the MPD server implementation.</p>
 */
public class Directory extends AbstractDirectory<Directory> {

    /**
     * This field is used to instantiate this class from a {@link Parcel}.
     */
    public static final Creator<Directory> CREATOR = new DirectoryParcelCreator();

    /**
     * The class log identifier.
     */
    private static final String TAG = "Directory";

    /**
     * This is a convenience string to use as a Intent extra tag.
     */
    public static final String EXTRA = TAG;

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
     * @param lsInfo The lsinfo CommandResult. If null, a {@link #refresh(MPDConnection)}
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

    /**
     * Flatten this object in to a Parcel.
     *
     * @param dest  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     *              May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        super.writeToParcel(dest, flags);

        dest.writeParcelable(mResult, 0);
    }

    /**
     * This class is used to instantiate a Directory Object from a {@code Parcel}.
     */
    private static final class DirectoryParcelCreator implements Parcelable.Creator<Directory> {

        /**
         * Sole constructor.
         */
        private DirectoryParcelCreator() {
            super();
        }

        /**
         * Create a new instance of the Parcelable class, instantiating it
         * from the given Parcel whose data had previously been written by
         * {@link Parcelable#writeToParcel Parcelable.writeToParcel()}.
         *
         * @param source The Parcel to read the object's data from.
         * @return Returns a new instance of the Parcelable class.
         */
        @Override
        public Directory createFromParcel(final Parcel source) {
            final ResponseObject response = source.readParcelable(ResponseObject.LOADER);
            final CommandResult result = source.readParcelable(CommandResult.LOADER);

            return new Directory(response, result);
        }

        /**
         * Create a new array of the Parcelable class.
         *
         * @param size Size of the array.
         * @return Returns an array of the Parcelable class, with every entry initialized to null.
         */
        @Override
        public Directory[] newArray(final int size) {
            return new Directory[size];
        }
    }
}
