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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class creates a Album Item, a item commonly found in the
 * <A HREF="http://www.musicpd.org/doc/protocol/database.html">Database Subsystem</A> in the
 * <A HREF="http://www.musicpd.org/doc/protocol">MPD Protocol</A>, for the Android backend.
 */
public class Album extends AbstractAlbum<Album> {

    /**
     * This field is used to instantiate this class from a {@link Parcel}.
     */
    public static final Creator<Album> CREATOR = new AlbumParcelCreator();

    /**
     * This is a convenience string to use as a Intent extra tag.
     */
    public static final String EXTRA = AbstractAlbum.TAG;


    protected Album(final String name, final Artist artist, final boolean hasAlbumArtist,
            final long songCount, final long duration, final long year, final String path) {
        super(name, artist, hasAlbumArtist, songCount, duration, year, path);
    }

    protected Album(final Parcel in) {
        super(in.readString(), /** name */
                Artist.byName(in.readString()), /** artist */
                in.readInt() > 0, /** hasAlbumArtist */
                in.readLong(), /** songCount */
                in.readLong(), /** duration */
                in.readLong(), /** year */
                in.readString()); /** path */
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
        final String artistName;
        final Artist artist = getArtist();
        final int hasAlbumArtist;

        if (artist == null) {
            artistName = "";
        } else {
            artistName = artist.getName();
        }

        if (hasAlbumArtist()) {
            hasAlbumArtist = 1;
        } else {
            hasAlbumArtist = 0;
        }

        dest.writeString(getName());
        dest.writeString(artistName);
        dest.writeInt(hasAlbumArtist);
        dest.writeLong(getSongCount());
        dest.writeLong(getDuration());
        dest.writeLong(getDate());
        dest.writeString(getPath());
    }

    /**
     * This class is used to instantiate a Album from a {@code Parcel}.
     */
    private static final class AlbumParcelCreator implements Parcelable.Creator<Album> {

        /**
         * Sole constructor.
         */
        private AlbumParcelCreator() {
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
        public Album createFromParcel(final Parcel source) {
            return new Album(source);
        }

        /**
         * Create a new array of the Parcelable class.
         *
         * @param size Size of the array.
         * @return Returns an array of the Parcelable class, with every entry initialized to null.
         */
        @Override
        public Album[] newArray(final int size) {
            return new Album[size];
        }
    }
}
