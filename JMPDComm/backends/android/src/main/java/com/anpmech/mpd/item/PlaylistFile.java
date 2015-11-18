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

import org.jetbrains.annotations.NotNull;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class creates a PlaylistFile Item, an abstraction of a playlist file in the <A
 * HREF="http://www.musicpd.org/doc/protocol/playlist_files.html">Stored Playlists</A> <A
 * HREF="http://www.musicpd.org/doc/protocol">MPD Protocol</A> subsystem, for the Android backend.
 */
public class PlaylistFile extends AbstractPlaylistFile {

    /**
     * This field is used to instantiate this class from a {@link Parcel}.
     */
    public static final Creator<PlaylistFile> CREATOR = new PlaylistFileParcelCreator();

    /**
     * This is a convenience string to use as a Intent extra tag.
     */
    public static final String EXTRA = TAG;

    /**
     * The copy constructor for this class.
     *
     * @param entry The {@link Entry} to copy.
     */
    public PlaylistFile(@NotNull final PlaylistFile entry) {
        super(entry.mResponseObject);
    }

    /**
     * This constructor is used to create a new PlaylistFile item with a ResponseObject.
     *
     * @param object The prepared ResponseObject.
     * @see #byPath(String)
     * @see #byResponse(String)
     */
    private PlaylistFile(@NotNull final ResponseObject object) {
        super(object);
    }

    /**
     * This method is used to create a new PlaylistFile by path.
     *
     * @param path The path of the PlaylistFile.
     * @return The new PlaylistFile.
     */
    public static PlaylistFile byPath(final String path) {
        return new PlaylistFile(new ResponseObject(path, null));
    }

    /**
     * This method is used to construct a new PlaylistFile by server response.
     *
     * @param response The server response.
     * @return The new PlaylistFile.
     */
    public static PlaylistFile byResponse(final String response) {
        return new PlaylistFile(new ResponseObject(null, response));
    }

    /**
     * This class is used to instantiate a PlaylistFile from a {@code Parcel}.
     */
    private static final class PlaylistFileParcelCreator
            implements Parcelable.Creator<PlaylistFile> {

        /**
         * Sole constructor.
         */
        private PlaylistFileParcelCreator() {
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
        public PlaylistFile createFromParcel(final Parcel source) {
            return new PlaylistFile((ResponseObject) source.readParcelable(ResponseObject.LOADER));
        }

        /**
         * Create a new array of the Parcelable class.
         *
         * @param size Size of the array.
         * @return Returns an array of the Parcelable class, with every entry initialized to null.
         */
        @Override
        public PlaylistFile[] newArray(final int size) {
            return new PlaylistFile[size];
        }
    }
}
